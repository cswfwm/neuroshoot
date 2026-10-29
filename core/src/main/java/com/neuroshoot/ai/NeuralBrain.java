package com.neuroshoot.ai;

import com.neuroshoot.world.C;
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.World;

/**
 * 神经网络控制器：把 Net 的输出翻译成移动、瞄准、开火、姿态与用药决策。
 *
 * 输出含义：0 前后移动、1 左右移动、2 转向、3 开火、4 开镜、5 下蹲、
 * 6 换弹/用药、7 投掷。移动用阈值过滤，避免每帧抖动。
 */
public final class NeuralBrain implements Brain {
    private final Net net;
    private final float[] input = new float[C.INPUTS];
    private final float[] output = new float[C.OUTPUTS];
    private float think;
    private float throwCd;
    private final Pathfinder pf = new Pathfinder();
    private int pathIdx;
    private float repathT;
    private float goalX, goalY;
    private boolean hasGoal;
    /** 没有可见目标时，往室内走的探索点。 */
    private float exploreX, exploreY;
    private float exploreT;

    /** 由训练器打开，降低思考频率以换取吞吐。 */
    public static void setTrainingFast(boolean fast) {
        trainingFast = fast;
    }
    /** 思考间隔：从 0.05s 收紧到 0.033s，接近每帧决策。 */
    /** 常规思考间隔（1/30 秒，接近每帧决策）。 */
    private static final float THINK_INTERVAL = 1f / 30f;
    /**
     * 训练时的思考间隔。训练要跑几十倍速，每帧决策会让 CPU 成为瓶颈，
     * 而 1/10 秒的决策对学习效果影响很小，换来的是数倍训练吞吐。
     */
    private static final float THINK_INTERVAL_TRAIN = 1f / 10f;
    private static boolean trainingFast;
    /** 转向角速度上限（rad/s），保证能快速对准目标。 */
    private static final float TURN_SPEED = 11f;

    public NeuralBrain(Net net) {
        this.net = net;
    }

    public Net net() {
        return net;
    }

    public float[] lastOutputs() {
        return output;
    }

    public float[] lastInputs() {
        return input;
    }

    @Override
    public void update(Unit self, World world, float dt) {
        if (!self.alive) return;

        // 每帧都检查「枪口是否已压在敌人身上」：这是零延迟的开火通道，
        // 不受思考间隔限制，所以瞄准到即开火。
        if (alignedOnTarget(self, world)) world.fire(self);

        think -= dt;
        if (think > 0f) return;
        think = trainingFast ? THINK_INTERVAL_TRAIN : THINK_INTERVAL;

        Sensors.fill(self, world, input);
        float[] o = net.forward(input);
        System.arraycopy(o, 0, output, 0, C.OUTPUTS);

        // 转向：提高到约 10 rad/s，保证能在 0.15 秒内转过 90°
        float turn = output[2];
        self.angle += turn * TURN_SPEED * THINK_INTERVAL;
        self.angle = (float) ((self.angle + Math.PI * 3) % (Math.PI * 2) - Math.PI);

        Unit visibleEnemy = visibleEnemy(self, world);
        if (visibleEnemy == null) {
            // 看不见人时按概率决定进屋或在室外游走，避免全部挤在房子里
            float[] goal = world.rnd() < 0.3f ? indoorGoal(self, world) : null;
            if (goal != null) {
                navigateTo(self, world, goal[0], goal[1], dt);
                continueAfterMove(self, world);
                return;
            }
        }

        float fx = (float) Math.cos(self.angle), fy = (float) Math.sin(self.angle);
        float rx = -fy, ry = fx;
        float mvF = output[0], mvR = output[1];
        float sp = self.moveSpeed();
        float vx = (fx * mvF + rx * mvR);
        float vy = (fy * mvF + ry * mvR);
        float len = (float) Math.hypot(vx, vy);
        if (len > 0.28f) {
            self.tx_ = vx / len * sp;
            self.ty_ = vy / len * sp;
            world.dropFootprint(self);
        } else {
            self.tx_ = 0f;
            self.ty_ = 0f;
        }

        self.aim = output[4] > 0.35f;
        self.crouch = output[5] > 0.45f;

        // 开火判定：只要网络想开火就开，另外「枪口已对准敌人」时强制补一发，
        // 这样瞄准精度不再被 0.05 秒的思考间隔吃掉落空。
        if (output[3] > 0.05f) world.fire(self);

        if (output[6] > 0.5f) {
            if (self.ammo <= 0) self.reload();
            else if (self.bleeding && self.meds.contains("bandage")) self.useMed("bandage");
            else if (self.fractured && self.meds.contains("surgery")) self.useMed("surgery");
            else if (self.hp < 55 && self.meds.contains("syringe")) self.useMed("syringe");
        }

        // 投掷：需要网络想扔、手里有、目标在 120~560 的合适距离且看得见，再等冷却
        throwCd = Math.max(0f, throwCd - THINK_INTERVAL);
        if (output[7] > 0.5f && !self.throwables.isEmpty() && throwCd <= 0f) {
            Unit t = nearestEnemy(self, world);
            if (t != null) {
                float d = (float) Math.hypot(t.x - self.x, t.y - self.y);
                boolean los = world.map.lineOfSight(self.x, self.y, t.x, t.y);
                if (d > 120f && d < 560f && los) {
                    float lead = Math.min(0.45f, d / 1200f);
                    world.throwItem(self, self.throwables.get(0),
                            t.x + t.vx * lead, t.y + t.vy * lead);
                    throwCd = 6f;
                }
            }
        }
    }

    /**
     * 枪口是否已经对准某个可见敌人。
     *
     * 判定用「身体半径 / 距离」算出的张角，只要敌人落在这个角度内就打，
     * 因此贴着瞄准线的一瞬间就能开火，而不是等下一次思考。
     */
    private boolean alignedOnTarget(Unit self, World world) {
        if (self.fireCd > 0f || self.ammo <= 0 || self.reloading) return false;
        for (Unit e : world.units) {
            if (!e.alive || e.team == self.team) continue;
            float dx = e.x - self.x, dy = e.y - self.y;
            float d = (float) Math.hypot(dx, dy);
            if (d > self.weapon().range || d < 1f) continue;
            float ang = (float) Math.atan2(dy, dx);
            float diff = Math.abs(com.neuroshoot.world.Visibility.normalize(ang - self.angle));
            // 命中所需的最小张角：目标半径约 17px，只给 1.2 倍余量，
            // 越界的角度不开火，避免「明明没对准也乱喷」。
            float need = (float) Math.atan2(20f, d);
            if (diff > need) continue;
            if (!com.neuroshoot.world.Visibility.unitVisible(world.map, self, e)) continue;
            return true;
        }
        return false;
    }

    /** 当前看得见的最近敌人；没有则返回 null。 */
    private Unit visibleEnemy(Unit self, World world) {
        Unit best = null;
        float bd = Float.MAX_VALUE;
        for (Unit u : world.units) {
            if (!u.alive || u.team == self.team) continue;
            if (!com.neuroshoot.world.Visibility.unitVisible(world.map, self, u)) continue;
            float d = (float) Math.hypot(u.x - self.x, u.y - self.y);
            if (d < bd) {
                bd = d;
                best = u;
            }
        }
        return best;
    }

    /**
     * 选一个室内目标点：优先建筑内部的空格，离自己越近越好。
     *
     * 找不到室内点时退化为地图中央，避免原地不动。
     * 目标每 3 秒重选一次，防止在两个房间之间来回摆。
     */
    private float[] indoorGoal(Unit self, World world) {
        exploreT -= 0.033f;
        if (exploreT > 0f && (exploreX != 0f || exploreY != 0f)) {
            return new float[]{exploreX, exploreY};
        }
        exploreT = 3f;
        float bestX = 0f, bestY = 0f;
        float bestD = Float.MAX_VALUE;
        float stairX = 0f, stairY = 0f, stairD = Float.MAX_VALUE;
        boolean found = false, stairFound = false;

        for (int ty = 2; ty < C.MAP_H - 2; ty++) {
            for (int tx = 2; tx < C.MAP_W - 2; tx++) {
                int lvl = world.map.levelAt(tx, ty);
                if (lvl < 1) continue;
                if (!Pathfinder.walkable(world.map, tx, ty)) continue;
                float cx = (tx + 0.5f) * C.TILE;
                float cy = (ty + 0.5f) * C.TILE;
                float d = (float) Math.hypot(cx - self.x, cy - self.y);
                if (d < 60f) continue;

                // 优先找「能换层」的楼梯口，让网络也学会上下楼
                boolean stair = world.map.at(tx, ty) == com.neuroshoot.world.Tile.STAIR;
                if (stair && lvl != self.floor && d < stairD) {
                    stairD = d;
                    stairX = cx;
                    stairY = cy;
                    stairFound = true;
                }
                if (lvl == Math.max(1, self.floor) && d < bestD) {
                    bestD = d;
                    bestX = cx;
                    bestY = cy;
                    found = true;
                }
            }
        }
        if (stairFound && (found ? stairD < bestD * 2.2f : true)) {
            exploreX = stairX;
            exploreY = stairY;
            return new float[]{stairX, stairY};
        }
        if (!found) return null;
        exploreX = bestX;
        exploreY = bestY;
        return new float[]{bestX, bestY};
    }

    /** 沿 A* 路径朝目标走，途经关着的门会自动推开。 */
    private void navigateTo(Unit self, World world, float tx, float ty, float dt) {
        int sx = (int) (self.x / C.TILE);
        int sy = (int) (self.y / C.TILE);
        int gx = (int) (tx / C.TILE);
        int gy = (int) (ty / C.TILE);

        repathT -= dt;
        boolean moved = Math.hypot(tx - goalX, ty - goalY) > C.TILE * 1.5f;
        if (repathT <= 0f || !hasGoal || moved) {
            repathT = 0.8f;
            goalX = tx;
            goalY = ty;
            hasGoal = true;
            pathIdx = 0;
            pathIdx = pf.findPath(world.map, sx, sy, gx, gy) ? 1 : -1;
        }

        float wx = tx, wy = ty;
        if (pathIdx > 0 && pathIdx < pf.pathLen) {
            int cell = pf.path[pathIdx];
            wx = (cell % C.MAP_W + 0.5f) * C.TILE;
            wy = (cell / C.MAP_W + 0.5f) * C.TILE;
            if (Math.hypot(wx - self.x, wy - self.y) < C.TILE * 0.55f) pathIdx++;
        }

        // 途经的门：推开
        float dx = wx - self.x, dy = wy - self.y;
        float len = Math.max(1f, (float) Math.hypot(dx, dy));
        int ax = (int) ((self.x + dx / len * C.TILE * 1.2f) / C.TILE);
        int ay = (int) ((self.y + dy / len * C.TILE * 1.2f) / C.TILE);
        if (world.map.at(ax, ay) == com.neuroshoot.world.Tile.DOOR
                && !world.map.doorPassable(ax, ay)) {
            world.map.openDoor(ax, ay);
        }

        // 朝路径点走，并让身体转向前进方向
        float sp = self.moveSpeed();
        self.tx_ = dx / len * sp;
        self.ty_ = dy / len * sp;
        self.angle = (float) Math.atan2(dy, dx);
        self.crouch = false;
        self.aim = false;
        world.dropFootprint(self);
    }

    /** 移动后处理非移动类动作（换弹/用药），避免探索时忘记补给。 */
    private void continueAfterMove(Unit self, World world) {
        if (self.ammo <= 0) self.reload();
        else if (self.bleeding && self.meds.contains("bandage")) self.useMed("bandage");
        else if (self.fractured && self.meds.contains("surgery")) self.useMed("surgery");
        else if (self.hp < 55 && self.meds.contains("syringe")) self.useMed("syringe");
    }

    private Unit nearestEnemy(Unit self, World world) {
        Unit best = null;
        float bd = Float.MAX_VALUE;
        for (Unit u : world.units) {
            if (!u.alive || u.team == self.team) continue;
            float d = (float) Math.hypot(u.x - self.x, u.y - self.y);
            if (d < bd) {
                bd = d;
                best = u;
            }
        }
        return best;
    }
}
