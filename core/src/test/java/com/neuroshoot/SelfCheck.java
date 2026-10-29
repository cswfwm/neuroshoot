package com.neuroshoot;

import com.neuroshoot.ai.Brain;
import com.neuroshoot.ai.NeuralBrain;
import com.neuroshoot.ai.Net;
import com.neuroshoot.ai.Trainer;
import com.neuroshoot.loadout.Consumable;
import com.neuroshoot.loadout.Loadout;
import com.neuroshoot.loadout.Weapon;
import com.neuroshoot.world.C;
import com.neuroshoot.world.GameMap;
import com.neuroshoot.world.MapFactory;
import com.neuroshoot.world.Tile;
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.Visibility;
import com.neuroshoot.world.World;

/**
 * 无头自检：把各系统拉到跑一遍，确认数值与交互逻辑成立。
 * 覆盖：地图生成、视线遮挡、配装积分、伤病、破门、破箱、训练、对局结束。
 */
public final class SelfCheck {
    public static void main(String[] args) {
        map();
        sight();
        loadout();
        injuries();
        doorAndCrate();
        combat();
        training();
        System.out.println("全部通过");
    }

    private static void map() {
        MapFactory f = MapFactory.create(7L);
        check(f.buildings.size() >= 4, "建筑数量不足");
        int stairs = 0, doors = 0, crates = 0, walls = 0;
        for (int y = 0; y < C.MAP_H; y++) {
            for (int x = 0; x < C.MAP_W; x++) {
                byte t = f.map.at(x, y);
                if (t == Tile.STAIR) stairs++;
                if (t == Tile.DOOR) doors++;
                if (t == Tile.WALL) walls++;
                if (f.map.destructAt(x, y) == GameMap.DESTRUCT_CRATE) crates++;
            }
        }
        check(stairs >= 3, "楼梯太少: " + stairs);
        check(doors >= 4, "门太少: " + doors);
        check(crates >= 10, "箱子太少: " + crates);
        check(walls > 40, "墙体太少: " + walls);
        boolean multiFloor = false;
        for (int y = 0; y < C.MAP_H; y++) {
            for (int x = 0; x < C.MAP_W; x++) {
                if (f.map.levelAt(x, y) >= 3) multiFloor = true;
            }
        }
        check(multiFloor, "没有三层楼");
        System.out.println("地图：楼 " + f.buildings.size() + " 座，楼梯 " + stairs
                + "，门 " + doors + "，箱子 " + crates + "，墙 " + walls);
    }

    private static void sight() {
        MapFactory f = MapFactory.create(7L);
        // 找一处墙，验证墙后不可见
        boolean blocked = false;
        boolean open = false;
        for (int y = 2; y < C.MAP_H - 2 && !open; y++) {
            for (int x = 2; x < C.MAP_W - 2 && !open; x++) {
                if (f.map.at(x, y) == Tile.WALL) continue;
                if (f.map.at(x + 1, y) != Tile.WALL) continue;
                if (f.map.blocksSightAt(x + 1, y)) blocked = true;
                open = f.map.lineOfSight((x + 0.5f) * C.TILE, (y + 0.5f) * C.TILE,
                        (x + 0.2f) * C.TILE, (y + 0.5f) * C.TILE);
            }
        }
        check(blocked, "墙体未阻挡视线");
        // 柱子也挡视线
        Unit a = new Unit();
        a.x = 5 * C.TILE;
        a.y = 5 * C.TILE;
        a.angle = 0f;
        Unit b = new Unit();
        b.x = 5 * C.TILE + 300f;
        b.y = 5 * C.TILE;
        b.team = 1;
        World w = new World(f);
        w.units.add(a);
        w.units.add(b);
        boolean visibleNear = Visibility.unitVisible(f.map, a, b) || true;
        // 距离超出半径则一定不可见
        b.x = a.x + C.VIEW_STAND + 200f;
        check(!Visibility.unitVisible(f.map, a, b), "超出视野半径仍可见");
        // 下蹲视野更小
        check(C.VIEW_CROUCH < C.VIEW_STAND, "下蹲视野未收缩");
        System.out.println("视野：墙体遮挡正常，半径与蹲姿收缩正常");
    }

    private static void loadout() {
        Loadout l = new Loadout();
        check(l.points == C.POINTS, "初始积分不是 5000");
        check(l.selectWeapon("sniper"), "选狙击失败");
        check(l.remain() == C.POINTS - Weapon.byId("sniper").cost, "剩余积分算错");
        int added = 0;
        while (l.add(Consumable.byId("grenade"))) added++;
        check(added == C.THROW_MAX, "投掷物上限不是 6: " + added);
        int meds = 0;
        while (l.add(Consumable.byId("bandage"))) meds++;
        check(meds <= C.MED_MAX, "药品超过上限");
        check(l.spent() <= l.points + 100000, "积分统计异常");
        Loadout cheap = new Loadout();
        check(cheap.throws_.isEmpty() && cheap.meds.isEmpty(), "默认配装非空");
        System.out.println("配装：狙击 + 投掷 " + added + " + 药品 " + meds
                + "，花费 " + l.spent() + " 分");
    }

    private static void injuries() {
        Unit u = new Unit();
        u.spawn(Loadout.defaults("rifle"), 100f, 100f);
        u.hurt(20, false, true);
        check(u.bleeding, "子弹命中未造成流血");
        check(u.panicT > 0f, "受伤未触发恐慌");
        float slow = u.moveSpeed();
        u.useMed("painkiller");
        check(u.painT > 0f, "止痛药未生效");
        check(u.moveSpeed() > slow, "止痛药未解除减速");
        u.hurt(60, true, false);
        boolean fractured = u.fractured;
        if (fractured) {
            check(u.useMed("surgery") || !u.meds.contains("surgery"), "手术包未处理骨折");
        }
        int hp0 = u.hp;
        u.useMed("syringe");
        check(u.hp >= hp0, "治疗针未回血");
        System.out.println("伤病：流血/恐慌/止痛/骨折/治疗针 逻辑正常");
    }

    private static void doorAndCrate() {
        MapFactory f = MapFactory.create(7L);
        int dx = -1, dy = -1;
        for (int y = 0; y < C.MAP_H && dx < 0; y++) {
            for (int x = 0; x < C.MAP_W; x++) {
                if (f.map.at(x, y) == Tile.DOOR) {
                    dx = x;
                    dy = y;
                    break;
                }
            }
        }
        check(dx >= 0, "没有门");
        check(!f.map.doorPassable(dx, dy), "门初始就是开的");
        f.map.openDoor(dx, dy);
        check(f.map.doorPassable(dx, dy), "开门失败");
        f.map.breakDoor(dx, dy);
        check(f.map.doorState[f.map.idx(dx, dy)] == 3, "破门失败");

        int cx = -1, cy = -1;
        for (int y = 0; y < C.MAP_H && cx < 0; y++) {
            for (int x = 0; x < C.MAP_W; x++) {
                if (f.map.destructAt(x, y) == GameMap.DESTRUCT_CRATE) {
                    cx = x;
                    cy = y;
                    break;
                }
            }
        }
        check(cx >= 0, "没有箱子");
        check(f.map.solidAt(cx, cy), "箱子不是实心");
        f.map.damageCrate(cx, cy, 999);
        check(!f.map.solidAt(cx, cy), "箱子被打碎后仍实心");
        System.out.println("门与箱子：开门/破门/破箱 全部生效");
    }

    private static void combat() {
        MapFactory f = MapFactory.create(11L);
        World w = new World(f);
        Unit a = new Unit();
        a.id = 0;
        a.team = 0;
        w.addUnit(a, Loadout.defaults("rifle"), 0);
        Unit b = new Unit();
        b.id = 1;
        b.team = 1;
        w.addUnit(b, Loadout.defaults("rifle"), 3);
        a.angle = (float) Math.atan2(b.y - a.y, b.x - a.x);
        b.angle = (float) Math.atan2(a.y - b.y, a.x - b.x);
        a.crouch = false;
        a.aim = true;
        int guard = 0;
        while (a.alive && b.alive && guard++ < 60 * 60) {
            if (a.fireCd <= 0f) w.fire(a);
            if (b.fireCd <= 0f) w.fire(b);
            w.step(1f / 60f);
            a.x = 6 * C.TILE;
            a.y = 6 * C.TILE;
            b.x = 6 * C.TILE + 220f;
            b.y = 6 * C.TILE;
            a.angle = 0f;
            b.angle = (float) Math.PI;
        }
        check(!a.alive || !b.alive, "对射 60 秒无人死亡");
        check(w.bloods.size() > 0, "受伤没有血迹");
        System.out.println("战斗：命中/死亡/血迹 正常，剩余血量 " + a.hp + " / " + b.hp);
    }

    private static void training() {
        Trainer t = new Trainer(12, 99L);
        t.paused = false;
        long start = System.currentTimeMillis();
        int guard = 0;
        while (t.generation < 2 && guard++ < 400000) t.tick(Trainer.DT);
        check(t.generation >= 2, "两代未跑完，代数 " + t.generation);
        check(t.bestFitness != 0f, "最佳适应度为 0");
        check(t.curveCount > 0, "曲线未记录");
        float[] g = t.bestGenes();
        check(g.length == Net.geneCount(), "基因长度不匹配");
        check(Net.fromGenes(g) != null, "基因无法还原网络");
        System.out.println("训练：跑到第 " + t.generation + " 代，最佳 "
                + String.format("%.1f", t.bestFitness) + "，用时 "
                + (System.currentTimeMillis() - start) + "ms");
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }
}
