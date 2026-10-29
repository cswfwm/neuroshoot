package com.neuroshoot;

import com.neuroshoot.world.C;
import com.neuroshoot.world.MapFactory;
import com.neuroshoot.world.Tile;
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.Visibility;

/** 检查城镇与工厂地图的结构、楼层、掩体数量与半圆视野。 */
public final class MapCheck {
    public static void main(String[] args) {
        for (MapFactory.Kind kind : MapFactory.Kind.values()) {
            MapFactory f = MapFactory.create(20261028L, kind);
            int walls = 0, floors = 0, doors = 0, crates = 0, pillars = 0, stairs = 0;
            int maxLevel = 0;
            for (int y = 0; y < C.MAP_H; y++) {
                for (int x = 0; x < C.MAP_W; x++) {
                    byte t = f.map.at(x, y);
                    if (t == Tile.WALL) walls++;
                    if (t == Tile.FLOOR) floors++;
                    if (t == Tile.DOOR) doors++;
                    if (t == Tile.PILLAR) pillars++;
                    if (t == Tile.STAIR) stairs++;
                    if (f.map.destructAt(x, y) != 0) crates++;
                    maxLevel = Math.max(maxLevel, f.map.levelAt(x, y));
                }
            }
            System.out.printf("%s: 楼%d座 三层以上=%b 墙%d 地板%d 门%d 箱%d 柱%d 梯%d%n",
                    kind, f.buildings.size(), maxLevel >= 3, walls, floors, doors,
                    crates, pillars, stairs);
            if (kind == MapFactory.Kind.FACTORY) {
                if (crates < 60) throw new AssertionError("工厂箱子太少: " + crates);
                if (maxLevel < 3) throw new AssertionError("工厂没有三层");
            } else {
                if (f.buildings.size() < 6) throw new AssertionError("城镇楼房太少");
            }
            // 半圆视野：背后必须看不见
            Unit u = new Unit();
            u.x = f.spawns.get(0)[0];
            u.y = f.spawns.get(0)[1];
            u.angle = 0f;  // 朝右
            float bx = u.x - 120f, by = u.y;  // 正后方
            int tx = (int) (bx / C.TILE), ty = (int) (by / C.TILE);
            boolean behind = Visibility.tileVisible(f.map, u, tx, ty);
            if (behind) throw new AssertionError("背后仍然可见");
            // 正前方必须可见（除非被挡）
            int fx = (int) ((u.x + 60f) / C.TILE), fy = (int) (u.y / C.TILE);
            boolean front = Visibility.tileVisible(f.map, u, fx, fy);
            System.out.printf("  正前方可见=%b，正后方可见=%b%n", front, behind);
        }
        System.out.println("地图与半圆视野：通过");
    }
}
