package com.neuroshoot.world;

import com.neuroshoot.loadout.Consumable;
import com.neuroshoot.loadout.GunBuild;
import com.neuroshoot.loadout.Loadout;
import com.neuroshoot.loadout.Weapon;

import java.util.ArrayList;
import java.util.List;

/**
 * 一名作战单位：位置、朝向、姿态、血量、护甲、伤病状态与背包。
 *
 * 伤病是这套玩法的核心：
 *  - 子弹与爆炸会造成流血，流血未止血时持续掉血并留下血迹；
 *  - 爆炸有概率骨折，骨折后移动大幅变慢，只能用手术包治好；
 *  - 受伤会触发短暂恐慌：视野收缩、移动变慢，止痛药期间免疫该惩罚。
 */
public final class Unit {
    public int id;
    public String name = "";
    public int team;
    /** 当前所在楼层：0 室外，1~3 建筑层。踩在楼梯上时会自动切换。 */
    public int floor;
    /** 是否携带夜视仪。 */
    public boolean nvg;
    /** 护甲等级 0~3，影响减伤。 */
    public int armorLevel;
    public boolean ai;
    public boolean local;

    public float x, y;
    public float angle;
    public float vx, vy;
    public int hp = C.HP_MAX;
    public int armor;
    public boolean alive = true;
    public boolean crouch;
    public boolean aim;
    public float fireCd;
    /** 当前手持的是主武器还是副武器。 */
    public boolean usingPrimary = true;
    public int ammo;
    public int ammoMax;
    /** 副武器弹药单独记录，切枪不会丢失。 */
    public int ammoSec;
    public int ammoSecMax;
    /** 主武器闲置时的余弹。 */
    public int primaryAmmo;
    public boolean reloading;
    public float reloadT;
    public float respawnT;

    public final Loadout loadout = new Loadout();
    public final List<String> throwables = new ArrayList<>();
    public final List<String> meds = new ArrayList<>();

    public boolean bleeding;
    public float bleedT;
    public boolean fractured;
    public float panicT;
    public float painT;
    public float aimSway;
    /** 连射累积的后坐力热度，停火后衰减。 */
    public float recoilHeat;
    /** 目标速度，由输入写入；实际速度朝它平滑逼近，才有加速与惯性手感。 */
    public float tx_, ty_;
    public float walkPhase;
    /** 渲染用的平滑坐标：逻辑按固定步长推进，画面按帧间插值，避免快速推进时抖动。 */
    public float rx, ry;
    /** 逻辑角度与渲染角度分开，转向也能插值。 */
    public float rAngle;
    public float muzzleT;
    public float hitFlashT;
    public int kills;
    public int deaths;
    public int damageDealt;
    public float aliveTime;
    /** 一局内的统计，供各类族群按自己的倾向打分。 */
    public float roundTaken;
    public float roundDist;
    public float minEnemyDist = 9999f;
    public float aimOnTarget;
    /** 本局走过的格子用位图记录：比 HashSet 快一个量级，且无装箱开销。 */
    public final boolean[] visited = new boolean[com.neuroshoot.world.C.MAP_W
            * com.neuroshoot.world.C.MAP_H];
    public int visitedCount;
    /** 由谁驱动；本地玩家为 null，由输入直接写 vx/vy/angle。 */
    public com.neuroshoot.ai.Brain brain;

    public void spawn(Loadout lo, float sx, float sy) {
        loadout.primary = lo.primary.copy();
        loadout.secondary = lo.secondary.copy();
        throwables.clear();
        meds.clear();
        throwables.addAll(lo.throws_);
        meds.addAll(lo.meds);
        usingPrimary = true;
        ammoMax = loadout.primary.magazine();
        ammo = ammoMax;
        primaryAmmo = ammoMax;
        ammoSecMax = loadout.secondary.magazine();
        ammoSec = ammoSecMax;
        x = sx;
        y = sy;
        angle = 0f;
        hp = C.HP_MAX;
        floor = 0;
        armor = 0;
        alive = true;
        crouch = false;
        aim = false;
        fireCd = 0f;
        reloading = false;
        reloadT = 0f;
        respawnT = 0f;
        bleeding = false;
        bleedT = 0f;
        fractured = false;
        panicT = 0f;
        painT = 0f;
        walkPhase = 0f;
        tx_ = 0f;
        ty_ = 0f;
        rx = x;
        ry = y;
        rAngle = angle;
        muzzleT = 0f;
        hitFlashT = 0f;
    }

    /** 当前手持的枪的改枪配置。 */
    public GunBuild gun() {
        return usingPrimary ? loadout.primary : loadout.secondary;
    }

    public Weapon weapon() {
        return gun().weapon();
    }

    /** 切换主副武器，换枪有短暂硬直。 */
    public void switchWeapon() {
        if (reloading) return;
        // 先把当前武器的余弹存回它自己的槽位，再装载另一把
        if (usingPrimary) {
            primaryAmmo = ammo;
            usingPrimary = false;
            ammoMax = loadout.secondary.magazine();
            if (ammoSec <= 0) ammoSec = ammoMax;
            ammo = ammoSec;
        } else {
            ammoSec = ammo;
            usingPrimary = true;
            ammoMax = loadout.primary.magazine();
            if (primaryAmmo <= 0) primaryAmmo = ammoMax;
            ammo = primaryAmmo;
        }
        fireCd = Math.max(fireCd, 0.45f);
        reloading = false;
    }

    /** 是否夜晚模式：由世界设置。 */
    public boolean nightMode;

    public float viewRadius() {
        float base;
        if (nightMode) {
            base = C.NIGHT_VIEW;
            if (nvg) base = C.DAY_VIEW * C.NVG_BOOST;
        } else {
            base = C.DAY_VIEW;
        }
        if (crouch) base *= 0.62f;
        else if (aim) base *= 1.18f;
        return base;
    }

    /** 视野半角（弧度）。 */
    public float viewFov() {
        float f = nightMode ? C.NIGHT_FOV : C.DAY_FOV;
        return aim ? f * 0.78f : f;
    }

    public float moveSpeed() {
        float s = C.MOVE_SPEED;
        if (crouch) s = C.CROUCH_SPEED;
        if (aim) s = Math.min(s, C.AIM_SPEED);
        s *= weapon().moveMul;
        if (fractured) s *= 0.55f;
        if (panicT > 0f && painT <= 0f) s *= C.PANIC_SPEED_MUL;
        return s;
    }

    public boolean panicking() {
        return panicT > 0f && painT <= 0f;
    }

    /** 受伤入口：返回是否致死。 */
    public boolean hurt(int dmg, boolean explosive, boolean fromBullet) {
        if (!alive) return false;
        int applied = dmg;
        // 护甲按等级减伤：1 级 20%、2 级 35%、3 级 50%，同时消耗护甲值
        if (armorLevel > 0) {
            float reduce = armorLevel >= 3 ? 0.5f : armorLevel == 2 ? 0.35f : 0.2f;
            int absorbed = (int) (dmg * reduce);
            absorbed = Math.min(absorbed, armor > 0 ? armor : absorbed);
            armor = Math.max(0, armor - absorbed / 2);
            applied = dmg - absorbed;
        }
        hp -= applied;
        roundTaken += applied;
        hitFlashT = 0.28f;
        if (painT <= 0f) panicT = C.PANIC_TIME;
        if (fromBullet || explosive) bleeding = true;
        if (explosive && Math.random() < 0.45) fractured = true;
        if (hp <= 0) {
            hp = 0;
            alive = false;
            deaths++;
            return true;
        }
        return false;
    }

    public void heal(int amount) {
        hp = Math.min(C.HP_MAX, hp + amount);
    }

    /** 使用一件药品，返回是否用掉。 */
    public boolean useMed(String id) {
        if (!meds.contains(id)) return false;
        switch (id) {
            case "bandage":
                if (!bleeding) return false;
                bleeding = false;
                bleedT = 0f;
                heal(18);
                break;
            case "syringe":
                if (hp >= C.HP_MAX) return false;
                heal(45);
                break;
            case "surgery":
                if (!fractured) return false;
                fractured = false;
                heal(30);
                break;
            case "painkiller":
                painT = C.PAINKILLER_TIME;
                panicT = 0f;
                break;
            default:
                return false;
        }
        meds.remove(id);
        return true;
    }

    public void takeThrowable(String id) {
        if (!throwables.contains(id)) return;
        throwables.remove(id);
    }

    public Consumable firstThrowable() {
        return throwables.isEmpty() ? null : Consumable.byId(throwables.get(0));
    }

    /** 每帧的伤病推进。 */
    public void tick(float dt, World world) {
        if (!alive) {
            if (respawnT > 0f) respawnT -= dt;
            return;
        }
        aliveTime += dt;
        int vx = (int) (x / C.TILE);
        int vy = (int) (y / C.TILE);
        if (vx >= 0 && vy >= 0 && vx < C.MAP_W && vy < C.MAP_H) {
            int vi = vy * C.MAP_W + vx;
            if (!visited[vi]) {
                visited[vi] = true;
                visitedCount++;
            }
        }
        fireCd = Math.max(0f, fireCd - dt);
        panicT = Math.max(0f, panicT - dt);
        painT = Math.max(0f, painT - dt);
        hitFlashT = Math.max(0f, hitFlashT - dt);
        muzzleT = Math.max(0f, muzzleT - dt);
        aimSway += dt;
        recoilHeat = Math.max(0f, recoilHeat - dt * 3.2f);

        if (reloading) {
            reloadT -= dt;
            if (reloadT <= 0f) {
                reloading = false;
                ammo = ammoMax;
                if (usingPrimary) primaryAmmo = ammoMax;
                else ammoSec = ammoSecMax;
            }
        }

        if (bleeding) {
            bleedT += dt;
            if (bleedT >= C.BLEED_INTERVAL) {
                bleedT = 0f;
                hp -= 2;
                if (hp <= 0) {
                    hp = 0;
                    alive = false;
                    deaths++;
                    world.onDeath(this, -1, false);
                    return;
                }
            }
            world.dropBlood(this, 0.5f);
        }
    }

    public void reload() {
        if (reloading || ammo == ammoMax) return;
        reloading = true;
        reloadT = gun().reloadTime();
    }
}
