package com.neuroshoot.world;

import com.neuroshoot.loadout.Consumable;
import com.neuroshoot.loadout.Loadout;
import com.neuroshoot.loadout.Weapon;

import java.util.ArrayList;
import java.util.List;

/**
 * 一局对战的世界：地图、单位、子弹、投掷物、血迹与击杀记录。
 *
 * 全部为纯逻辑，不依赖绘图；界面与 AI 都通过这里的 API 推进与查询，
 * 这样同一份模拟可以服务单机、人机、AI 对 AI 与联机主机。
 */
public final class World {
    public interface Events {
        void onKill(Unit killer, Unit victim, Weapon weapon);
        void onHit(Unit target, int damage);
        void onExplosion(float x, float y, float radius);
        void onDoorBroken(float x, float y);
        void onCrateBroken(float x, float y);
    }

    public static final class Bullet {
        public float x, y, vx, vy;
        public float life;
        public int owner;
        public int team;
        public int damage;
        public boolean fromShotgun;
        public boolean alive = true;
        public float px, py;
        /** 发射原点，用于绘制一条从枪口到弹头的笔直弹道线。 */
        public float ox, oy;
    }

    public static final class Throwable {
        public float x, y, vx, vy;
        public float fuse;
        public String kind;
        public int owner;
        public int team;
        public boolean alive = true;
        public boolean exploded;
        public float spin;
    }

    public static final class Blood {
        public float x, y;
        public float radius;
        public float life;
        public boolean footprint;
    }

    public final MapFactory layout;
    public final GameMap map;
    public final List<Unit> units = new ArrayList<>();
    public final List<Bullet> bullets = new ArrayList<>();
    public final List<Throwable> throwables = new ArrayList<>();
    public final List<Blood> bloods = new ArrayList<>();
    public float time;
    public boolean ended;
    public Events events;

    private long rng = 12345L;

    public World(MapFactory layout) {
        this.layout = layout;
        this.map = layout.map;
    }

    public void addUnit(Unit u, Loadout lo, int spawnIndex) {
        float[] s = layout.spawns.get(spawnIndex % layout.spawns.size());
        u.spawn(lo, s[0], s[1]);
        units.add(u);
    }

    public float rnd() {
        rng = rng * 6364136223846793005L + 1442695040888963407L;
        return ((rng >>> 40) & 0xFFFFFF) / (float) 0xFFFFFF;
    }

    public void step(float dt) {
        if (ended) return;
        time += dt;
        for (Unit u : units) {
            u.rx = u.x;
            u.ry = u.y;
            u.rAngle = u.angle;
            u.tick(dt, this);
            if (u.alive) {
                moveUnit(u, dt);
                updateFloor(u);
            }
        }
        stepBullets(dt);
        stepThrowables(dt);
        stepBlood(dt);
        if (time > C.MATCH_TIME) ended = true;
    }

    /** 平滑逼近目标速度：起步有加速、松手有滑行，避免一按就满速的僵硬感。 */
    private void approachVelocity(Unit u, float dt) {
        float accel = 2400f;
        float decel = 3200f;
        boolean wantMove = u.tx_ != 0f || u.ty_ != 0f;
        float dvx = u.tx_ - u.vx;
        float dvy = u.ty_ - u.vy;
        float a = wantMove ? accel : decel;
        u.vx += clamp(dvx, a * dt);
        u.vy += clamp(dvy, a * dt);
    }

    private static float clamp(float v, float limit) {
        if (v > limit) return limit;
        if (v < -limit) return -limit;
        return v;
    }

    /**
     * 楼层切换：站在楼梯格上就升到该格标注的楼层，离开楼梯则回到该格楼层或 0。
     *
     * 这样楼梯就是真正的「上下楼通道」：踩上去换层，站在楼板上就在楼上，
     * 不同楼层的单位互相看不见也打不到。
     */
    private void updateFloor(Unit u) {
        int tx = (int) (u.x / C.TILE);
        int ty = (int) (u.y / C.TILE);
        byte t = map.at(tx, ty);
        if (t == Tile.STAIR) {
            int lvl = map.levelAt(tx, ty);
            if (lvl > 0) u.floor = lvl;
        } else if (map.levelAt(tx, ty) > 0) {
            // 站在楼板上
            u.floor = map.levelAt(tx, ty);
        } else {
            u.floor = 0;
        }
    }

    private void moveUnit(Unit u, float dt) {
        approachVelocity(u, dt);
        float nx = u.x + u.vx * dt;
        float ny = u.y + u.vy * dt;
        float r = C.RADIUS;
        if (!blocked(nx, u.y, r)) u.x = nx;
        if (!blocked(u.x, ny, r)) u.y = ny;
        u.x = Math.max(C.TILE, Math.min(C.WORLD_W - C.TILE, u.x));
        u.y = Math.max(C.TILE, Math.min(C.WORLD_H - C.TILE, u.y));
        if (u.vx != 0f || u.vy != 0f) u.walkPhase += dt * 6.5f;
    }

    private boolean blocked(float cx, float cy, float r) {
        int x0 = (int) ((cx - r) / C.TILE), x1 = (int) ((cx + r) / C.TILE);
        int y0 = (int) ((cy - r) / C.TILE), y1 = (int) ((cy + r) / C.TILE);
        for (int ty = y0; ty <= y1; ty++) {
            for (int tx = x0; tx <= x1; tx++) {
                // 通行判定：门只要开了就能过，不能只看 Tile.solid
                boolean passable = !map.solidAt(tx, ty)
                        || (map.at(tx, ty) == Tile.DOOR && map.doorPassable(tx, ty));
                if (passable) continue;
                float bx = tx * C.TILE, by = ty * C.TILE;
                float nx = Math.max(bx, Math.min(cx, bx + C.TILE));
                float ny = Math.max(by, Math.min(cy, by + C.TILE));
                if ((nx - cx) * (nx - cx) + (ny - cy) * (ny - cy) < r * r) return true;
            }
        }
        return false;
    }

    /** 开火：霰弹一次多颗弹丸，其余单发；精度按腰射/开镜取不同扩散。 */
    public void fire(Unit u) {
        if (!u.alive || u.fireCd > 0f || u.reloading) return;
        com.neuroshoot.loadout.GunBuild g = u.gun();
        if (u.ammo <= 0) {
            // 主武器打空且副武器还有弹时自动切副武器
            if (u.usingPrimary && u.ammoSec > 0) {
                u.switchWeapon();
                return;
            }
            u.reload();
            return;
        }
        u.ammo--;
        u.fireCd = g.fireInterval();
        shotsByUnit.merge(u.id, 1, Integer::sum);
        u.muzzleT = 0.07f;
        // 腰射与开镜用改枪后的散布；连射时后坐力累加
        float spread = u.aim ? g.spreadAim() : g.spreadHip();
        spread *= 1f + Math.min(1.6f, u.recoilHeat * g.recoil() * 0.35f);
        u.recoilHeat = Math.min(6f, u.recoilHeat + 1f);
        int pellets = g.pellets();
        for (int i = 0; i < pellets; i++) {
            float a = u.angle + (rnd() - 0.5f) * 2f * spread * (pellets > 1 ? 2.2f : 1f);
            Bullet b = new Bullet();
            b.x = u.x + (float) Math.cos(u.angle) * 22f;
            b.y = u.y + (float) Math.sin(u.angle) * 22f;
            b.px = b.x;
            b.py = b.y;
            b.ox = b.x;
            b.oy = b.y;
            b.vx = (float) Math.cos(a) * C.BULLET_SPEED;
            b.vy = (float) Math.sin(a) * C.BULLET_SPEED;
            b.life = g.range() / C.BULLET_SPEED;
            b.owner = u.id;
            b.team = u.team;
            b.damage = g.damage();
            b.fromShotgun = pellets > 1;
            bullets.add(b);
        }
    }

    private void stepBullets(float dt) {
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            if (!b.alive) {
                bullets.remove(i);
                continue;
            }
            b.px = b.x;
            b.py = b.y;
            b.x += b.vx * dt;
            b.y += b.vy * dt;
            b.life -= dt;
            if (b.life <= 0f) {
                b.alive = false;
                bullets.remove(i);
                continue;
            }
            int tx = (int) (b.x / C.TILE);
            int ty = (int) (b.y / C.TILE);
            if (map.blocksBulletAt(tx, ty)) {
                if (map.destructAt(tx, ty) == GameMap.DESTRUCT_CRATE) {
                    if (map.damageCrate(tx, ty, b.damage)) {
                        if (events != null) events.onCrateBroken(tx * C.TILE, ty * C.TILE);
                    }
                } else if (map.at(tx, ty) == Tile.DOOR) {
                    com.neuroshoot.loadout.Weapon w = com.neuroshoot.loadout.Weapon.byId(weaponIdOf(b.owner));
                    if (w.breaksDoor) {
                        map.breakDoor(tx, ty);
                        if (events != null) events.onDoorBroken(tx * C.TILE, ty * C.TILE);
                    }
                }
                bullets.remove(i);
                continue;
            }
            Unit hit = null;
            Unit shooter = unit(b.owner);
            for (Unit u : units) {
                if (!u.alive || u.id == b.owner) continue;
                // 隔层打不到：楼层不同则子弹穿过
                if (shooter != null && u.floor != shooter.floor) continue;
                float dx = u.x - b.x, dy = u.y - b.y;
                if (dx * dx + dy * dy < (C.RADIUS + 4f) * (C.RADIUS + 4f)) {
                    hit = u;
                    break;
                }
            }
            if (hit != null) {
                hitsByUnit.merge(b.owner, 1, Integer::sum);
                boolean dead = hit.hurt(b.damage, false, true);
                dropBlood(hit, 1f);
                if (events != null) events.onHit(hit, b.damage);
                if (dead) onDeath(hit, b.owner, true);
                bullets.remove(i);
            }
        }
    }

    private String weaponIdOf(int unitId) {
        for (Unit u : units) if (u.id == unitId) return u.weapon().id;
        return "m4a1";
    }

    /** 投掷：从身体外沿出手，飞行距离按目标点距离换算，飞不到就落在途中。 */
    public void throwItem(Unit u, String kind, float tx, float ty) {
        if (!u.throwables.contains(kind)) return;
        Consumable c = Consumable.byId(kind);
        u.takeThrowable(kind);

        float dx = tx - u.x, dy = ty - u.y;
        float d = Math.max(1f, (float) Math.hypot(dx, dy));
        float nx = dx / d, ny = dy / d;

        Throwable t = new Throwable();
        // 从身体外沿出生，避免出生点就在墙/箱子里被立刻判定为碰撞
        t.x = u.x + nx * (C.RADIUS + 6f);
        t.y = u.y + ny * (C.RADIUS + 6f);
        // 目标距离越远，初速越大，但仍受上限约束，于是有「扔近了/扔远了」的手感
        float speed = Math.min(700f, 220f + d * 1.1f);
        t.vx = nx * speed;
        t.vy = ny * speed;
        t.fuse = c == null ? 2f : c.fuse;
        t.kind = kind;
        t.owner = u.id;
        t.team = u.team;
        t.spin = 0f;
        throwables.add(t);
    }

    private void stepThrowables(float dt) {
        for (int i = throwables.size() - 1; i >= 0; i--) {
            Throwable t = throwables.get(i);
            if (!t.alive) {
                throwables.remove(i);
                continue;
            }
            float nx = t.x + t.vx * dt;
            float ny = t.y + t.vy * dt;
            // 撞墙时沿墙滑行而不是立刻停住，否则丢出去的手雷会原地不动
            if (map.blocksBulletAt((int) (nx / C.TILE), (int) (t.y / C.TILE))) {
                t.vx *= -0.35f;
            } else {
                t.x = nx;
            }
            if (map.blocksBulletAt((int) (t.x / C.TILE), (int) (ny / C.TILE))) {
                t.vy *= -0.35f;
            } else {
                t.y = ny;
            }
            t.vx *= (1f - 1.5f * dt);
            t.vy *= (1f - 1.5f * dt);
            t.spin += dt * 6f;
            t.fuse -= dt;
            if (t.fuse <= 0f) {
                explode(t);
                throwables.remove(i);
            }
        }
    }

    private void explode(Throwable t) {
        float radius = t.kind.equals("c4") ? 260f : t.kind.equals("grenade") ? 210f : 150f;
        if (t.kind.equals("smoke") || t.kind.equals("flash")) {
            radius = 0f;
        }
        if (events != null) events.onExplosion(t.x, t.y, Math.max(radius, 90f));
        if (t.kind.equals("smoke")) return;
        if (t.kind.equals("flash")) {
            for (Unit u : units) {
                if (!u.alive) continue;
                float d = (float) Math.hypot(u.x - t.x, u.y - t.y);
                if (d < 320f) u.panicT = Math.max(u.panicT, 2.6f);
            }
            return;
        }
        // 爆炸破门与破箱
        int r = (int) (radius / C.TILE) + 1;
        int cx = (int) (t.x / C.TILE), cy = (int) (t.y / C.TILE);
        for (int ty = cy - r; ty <= cy + r; ty++) {
            for (int tx = cx - r; tx <= cx + r; tx++) {
                if (!map.inside(tx, ty)) continue;
                float d = (float) Math.hypot((tx + 0.5f) * C.TILE - t.x, (ty + 0.5f) * C.TILE - t.y);
                if (d > radius) continue;
                if (map.at(tx, ty) == Tile.DOOR) {
                    map.breakDoor(tx, ty);
                    if (events != null) events.onDoorBroken(tx * C.TILE, ty * C.TILE);
                } else if (map.destructAt(tx, ty) == GameMap.DESTRUCT_CRATE) {
                    map.damageCrate(tx, ty, 999);
                    if (events != null) events.onCrateBroken(tx * C.TILE, ty * C.TILE);
                }
            }
        }
        Unit thrower = unit(t.owner);
        for (Unit u : units) {
            if (!u.alive) continue;
            if (thrower != null && u.floor != thrower.floor) continue;
            float d = (float) Math.hypot(u.x - t.x, u.y - t.y);
            if (d > radius) continue;
            boolean los = map.lineOfSight(t.x, t.y, u.x, u.y);
            if (!los) continue;
            float falloff = 1f - (d / radius) * 0.65f;
            int dmg = (int) ((t.kind.equals("c4") ? 105 : 78) * falloff);
            boolean dead = u.hurt(dmg, true, false);
            dropBlood(u, 2f);
            if (dead) onDeath(u, t.owner, false);
        }
    }

    public void onDeath(Unit victim, int killerId, boolean bullet) {
        Unit killer = null;
        for (Unit u : units) if (u.id == killerId) killer = u;
        if (killer != null && killer != victim) killer.kills++;
        victim.respawnT = 6f;
        dropBlood(victim, 3f);
        if (events != null) {
            events.onKill(killer, victim, victim.weapon());
        }
    }

    public void dropBlood(Unit u, float amount) {
        if (bloods.size() > 260) bloods.remove(0);
        Blood b = new Blood();
        b.x = u.x + (rnd() - 0.5f) * 26f;
        b.y = u.y + (rnd() - 0.5f) * 26f;
        b.radius = 6f + rnd() * 7f * amount;
        b.life = C.BLOOD_FADE;
        b.footprint = false;
        bloods.add(b);
    }

    /** 走路时在身后留下血脚印（仅流血状态）。 */
    public void dropFootprint(Unit u) {
        if (!u.bleeding) return;
        if (bloods.size() > 300) bloods.remove(0);
        Blood b = new Blood();
        b.x = u.x - (float) Math.cos(u.angle) * 10f + (rnd() - 0.5f) * 6f;
        b.y = u.y - (float) Math.sin(u.angle) * 10f + (rnd() - 0.5f) * 6f;
        b.radius = 4.5f;
        b.life = C.BLOOD_FADE;
        b.footprint = true;
        bloods.add(b);
    }

    private void stepBlood(float dt) {
        for (int i = bloods.size() - 1; i >= 0; i--) {
            Blood b = bloods.get(i);
            b.life -= dt;
            if (b.life <= 0f) bloods.remove(i);
        }
    }

    public Unit unit(int id) {
        for (Unit u : units) if (u.id == id) return u;
        return null;
    }

    public List<Unit> teamOf(int team) {
        List<Unit> out = new ArrayList<>();
        for (Unit u : units) if (u.team == team) out.add(u);
        return out;
    }

    /** 按单位统计的开火/命中，供评估脚本与自检使用。 */
    public final java.util.Map<Integer, Integer> shotsByUnit = new java.util.HashMap<>();
    public final java.util.Map<Integer, Integer> hitsByUnit = new java.util.HashMap<>();

    public int totalShotsOf(int unitId) {
        return shotsByUnit.getOrDefault(unitId, 0);
    }

    public int totalHitsOf(int unitId) {
        return hitsByUnit.getOrDefault(unitId, 0);
    }

    public int aliveCount(int team) {
        int n = 0;
        for (Unit u : units) if (u.team == team && u.alive) n++;
        return n;
    }
}
