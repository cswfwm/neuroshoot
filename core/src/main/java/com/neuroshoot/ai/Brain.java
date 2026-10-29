package com.neuroshoot.ai;

import com.neuroshoot.world.C;
import com.neuroshoot.world.GameMap;
import com.neuroshoot.world.Tile;
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.Visibility;
import com.neuroshoot.world.World;

/**
 * 单位控制器。玩家、神经网络、脚本 AI 都实现它，世界每帧调用一次。
 */
public interface Brain {
    void update(Unit self, World world, float dt);

    /** 神经网络驱动。null 表示这是由外部（玩家/联机）输入控制。 */
    final class Holder {
        private Holder() {}
    }

    static Brain player() {
        return (self, world, dt) -> {
        };
    }

    /**
     * 简易脚本 AI：只在「看得见」的目标上反应，会绕着掩体走、会补血、会重装弹。
     * 用作没有模型时的对手，也是评估神经网络强弱的标准线。
     */
    static Brain scripted() {
        return new ScriptedBrain();
    }

    final class ScriptedBrain implements Brain {
        private float retarget;
        private Unit target;
        private float wanderT;
        private float wandAng;
        private final Pathfinder pf = new Pathfinder();
        private int pathIdx;
        private float repathT;
        private float goalX, goalY;
        private boolean hasGoal;
        private float wanderX, wanderY;

        /**
         * 挑一个探索目标。
         *
         * 优先级：比自己楼层更高的楼梯口 > 同楼层的室内空格 > 任意室内空格。
         * 这样 AI 会主动上楼，而不是一直在一层转。
         */
        private float[] pickIndoorSpot(Unit self, World world) {
            float bestUpX = 0f, bestUpY = 0f, bestUpD = Float.MAX_VALUE;
            boolean upFound = false;
            float bestX = 0f, bestY = 0f, bestD = Float.MAX_VALUE;
            boolean found = false;

            for (int ty = 2; ty < C.MAP_H - 2; ty++) {
                for (int tx = 2; tx < C.MAP_W - 2; tx++) {
                    int lvl = world.map.levelAt(tx, ty);
                    if (lvl < 1) continue;
                    if (!Pathfinder.walkable(world.map, tx, ty)) continue;
                    float cx = (tx + 0.5f) * C.TILE;
                    float cy = (ty + 0.5f) * C.TILE;
                    float d = (float) Math.hypot(cx - self.x, cy - self.y);
                    if (d < 80f) continue;

                    boolean stair = world.map.at(tx, ty) == Tile.STAIR;
                    // 到楼层上限时改为找向下的楼梯，避免一直困在顶层
                    boolean wantUp = self.floor < 3;
                    boolean target = wantUp ? lvl > self.floor : lvl < self.floor;
                    if (stair && target && d < bestUpD) {
                        bestUpD = d;
                        bestUpX = cx;
                        bestUpY = cy;
                        upFound = true;
                    }
                    if (lvl == self.floor && d < bestD) {
                        bestD = d;
                        bestX = cx;
                        bestY = cy;
                        found = true;
                    }
                }
            }
            if (upFound) return new float[]{bestUpX, bestUpY};
            if (found) return new float[]{bestX, bestY};
            // 退一步：任何室内点
            for (int ty = 2; ty < C.MAP_H - 2; ty += 2) {
                for (int tx = 2; tx < C.MAP_W - 2; tx += 2) {
                    if (world.map.levelAt(tx, ty) < 1) continue;
                    if (!Pathfinder.walkable(world.map, tx, ty)) continue;
                    float cx = (tx + 0.5f) * C.TILE;
                    float cy = (ty + 0.5f) * C.TILE;
                    float d = (float) Math.hypot(cx - self.x, cy - self.y);
                    if (d < 80f) continue;
                    if (d < bestD) {
                        bestD = d;
                        bestX = cx;
                        bestY = cy;
                        found = true;
                    }
                }
            }
            return found ? new float[]{bestX, bestY} : null;
        }

        @Override
        public void update(Unit self, World world, float dt) {
            if (!self.alive) return;
            if (self.hp < 45 && tryHeal(self)) return;
            if (self.bleeding && self.meds.contains("bandage")) {
                self.useMed("bandage");
            }
            if (self.fractured && self.meds.contains("surgery")) {
                self.useMed("surgery");
            }
            if (self.ammo <= 0) self.reload();

            retarget -= dt;
            if (retarget <= 0f) {
                retarget = 0.25f;
                target = pickTarget(self, world);
            }
            if (target != null && target.alive) {
                float dx = target.x - self.x, dy = target.y - self.y;
                float dist = (float) Math.hypot(dx, dy);
                self.angle = (float) Math.atan2(dy, dx);
                self.crouch = dist > 460f && self.weapon().id.equals("sniper");
                self.aim = dist > 420f;
                boolean visible = Visibility.unitVisible(world.map, self, target);
                if (visible && dist < self.weapon().range * 0.92f) {
                    self.tx_ = 0f;
                    self.ty_ = 0f;
                    // 角度几乎对准就开火，不再等随机机会
                    float diff = Math.abs(com.neuroshoot.world.Visibility.normalize(
                            (float) Math.atan2(dy, dx) - self.angle));
                    float need = (float) Math.atan2(26f, Math.max(1f, dist));
                    if (self.fireCd <= 0f && diff < Math.max(need, 0.05f)) world.fire(self);
                } else {
                    steer(self, world, target.x, target.y, dt);
                }
                // 中距离且看得见才扔，避免贴脸炸到自己
                if (dist > 140f && dist < 520f && visible && !self.throwables.isEmpty()
                        && world.rnd() < 0.01f) {
                    world.throwItem(self, self.throwables.get(0), target.x, target.y);
                }
            } else {
                // 没有目标时优先钻进室内：室内有掩体与更好的视野位置，
                // 也让 AI 学会利用建筑而不是一直空地乱跑。
                // 只在「还没到目标」或「超时」时换目标，避免在门口来回摆
                wanderT -= dt;
                float toGoal = (float) Math.hypot(wanderX - self.x, wanderY - self.y);
                boolean needNew = wanderX == 0f && wanderY == 0f
                        || wanderT <= 0f || toGoal < C.TILE * 1.2f;
                if (needNew) {
                    wanderT = 12f;
                    // 只有 35% 概率选室内点，其余在附近室外游走，
                    // 避免所有 AI 都往房子里挤
                    float[] spot = world.rnd() < 0.35f ? pickIndoorSpot(self, world) : null;
                    if (spot != null) {
                        wanderX = spot[0];
                        wanderY = spot[1];
                    } else {
                        wandAng = world.rnd() * 6.283f;
                        wanderX = self.x + (float) Math.cos(wandAng) * 400f;
                        wanderY = self.y + (float) Math.sin(wandAng) * 400f;
                    }
                }
                if (wanderX != 0f || wanderY != 0f) {
                    steer(self, world, wanderX, wanderY, dt);
                    self.angle = (float) Math.atan2(wanderY - self.y, wanderX - self.x);
                } else {
                    self.tx_ = 0f;
                    self.ty_ = 0f;
                }
                self.crouch = false;
                self.aim = false;
            }
        }

        private boolean tryHeal(Unit self) {
            if (self.meds.contains("syringe")) return self.useMed("syringe");
            if (self.meds.contains("bandage") && self.bleeding) return self.useMed("bandage");
            return false;
        }

        private Unit pickTarget(Unit self, World world) {
            Unit best = null;
            float bestD = Float.MAX_VALUE;
            for (Unit u : world.units) {
                if (!u.alive || u.team == self.team) continue;
                float d = (float) Math.hypot(u.x - self.x, u.y - self.y);
                if (d > self.viewRadius()) continue;
                if (!Visibility.unitVisible(world.map, self, u)) continue;
                if (d < bestD) {
                    bestD = d;
                    best = u;
                }
            }
            return best;
        }

        /**
         * 朝目标移动：先 A* 算路径，再沿路径逐格走。
         *
         * 门在寻路里视为可通行，走到门口会自动推开，
         * 所以 AI 能真正绕进房子里，而不是在墙外反复蹭。
         */
        private void steer(Unit self, World world, float tx, float ty, float dt) {
            int sx = (int) (self.x / C.TILE);
            int sy = (int) (self.y / C.TILE);
            int gx = (int) (tx / C.TILE);
            int gy = (int) (ty / C.TILE);

            repathT -= dt;
            boolean goalMoved = Math.hypot(tx - goalX, ty - goalY) > C.TILE * 1.5f;
            if (repathT <= 0f || !hasGoal || goalMoved) {
                repathT = 0.7f;
                goalX = tx;
                goalY = ty;
                hasGoal = true;
                pathIdx = 0;
                if (pf.findPath(world.map, sx, sy, gx, gy)) {
                    pathIdx = 1;   // path[0] 是当前格
                } else {
                    pathIdx = -1;  // 找不到路，退回直线朝向
                }
            }

            float wx = tx, wy = ty;
            if (pathIdx > 0 && pathIdx < pf.pathLen) {
                int cell = pf.path[pathIdx];
                wx = (cell % C.MAP_W + 0.5f) * C.TILE;
                wy = (cell / C.MAP_W + 0.5f) * C.TILE;
                // 走到当前路径点附近就推进下一格
                if (Math.hypot(wx - self.x, wy - self.y) < C.TILE * 0.55f) {
                    pathIdx++;
                }
            }
            openDoorOnTheWay(self, world, wx, wy);

            float dx = wx - self.x, dy = wy - self.y;
            float len = Math.max(1f, (float) Math.hypot(dx, dy));
            float sp = self.moveSpeed();
            float ax = dx / len * sp, ay = dy / len * sp;

            // 贴墙时沿切向滑一下，避免原地抖动
            if (hitsWall(world.map, self, ax, ay)) {
                float tx2 = -ay, ty2 = ax;
                if (!hitsWall(world.map, self, tx2, ty2)) {
                    ax = tx2;
                    ay = ty2;
                } else {
                    ax = -tx2;
                    ay = -ty2;
                }
            }
            self.tx_ = ax;
            self.ty_ = ay;
            if (ax != 0f || ay != 0f) world.dropFootprint(self);
        }

        /**
         * 推开附近关着的门。
         *
         * 扫描自身 3 格半径内的所有门并打开：AI 常常停在门前半个身位，
         * 只检测正前方一格会漏掉，导致永远进不了房子。
         */
        private void openDoorOnTheWay(Unit self, World world, float wx, float wy) {
            int cx = (int) (self.x / C.TILE);
            int cy = (int) (self.y / C.TILE);
            for (int dy = -3; dy <= 3; dy++) {
                for (int dx = -3; dx <= 3; dx++) {
                    int ax = cx + dx, ay = cy + dy;
                    if (world.map.at(ax, ay) != com.neuroshoot.world.Tile.DOOR) continue;
                    if (world.map.doorPassable(ax, ay)) continue;
                    // 只开朝目标方向那一侧的门，避免把背面的门也开掉
                    float dcx = (ax + 0.5f) * C.TILE - self.x;
                    float dcy = (ay + 0.5f) * C.TILE - self.y;
                    float gx = wx - self.x, gy = wy - self.y;
                    if (dcx * gx + dcy * gy < 0f) continue;
                    world.map.openDoor(ax, ay);
                }
            }
        }

        /** 前方是否被挡。开着的门不算墙，否则 AI 会在门口反复侧滑进不去。 */
        private boolean hitsWall(GameMap map, Unit u, float vx, float vy) {
            float nx = u.x + vx * 0.09f, ny = u.y + vy * 0.09f;
            int tx = (int) (nx / C.TILE), ty = (int) (ny / C.TILE);
            if (map.at(tx, ty) == Tile.DOOR) return !map.doorPassable(tx, ty);
            if (map.at(tx, ty) == Tile.STAIR) return false;
            return map.solidAt(tx, ty);
        }
    }
}
