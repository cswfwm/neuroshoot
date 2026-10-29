package com.neuroshoot.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 生成一张城镇地图：中央三层主楼、两座两层侧楼、若干散落掩体与植被。
 *
 * 建筑内部在地图上划分为独立楼层（level 1~3），楼梯口连通上下层；
 * 站在楼梯上能从上层看到下层，反之亦然，这是绕后偷袭的主要通道。
 */
public final class MapFactory {
    /** 建筑矩形，用于绘制楼板与楼层判定。 */
    public static final class Building {
        public final int x, y, w, h, floors;

        Building(int x, int y, int w, int h, int floors) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.floors = floors;
        }

        public boolean contains(int tx, int ty) {
            return tx >= x && ty >= y && tx < x + w && ty < y + h;
        }
    }

    public final GameMap map = new GameMap();
    public final List<Building> buildings = new ArrayList<>();
    /** 出生点：左上蓝队、右下红队。 */
    public final List<float[]> spawns = new ArrayList<>();

    /** 可选地图。 */
    public enum Kind { TOWN, FACTORY }

    public static MapFactory create(long seed) {
        return create(seed, Kind.TOWN);
    }

    public static MapFactory create(long seed, Kind kind) {
        MapFactory f = new MapFactory();
        if (kind == Kind.FACTORY) f.generateFactory(new Random(seed));
        else f.generate(new Random(seed));
        return f;
    }

    /** 全室内的工厂：三层厂房、大量箱子与花盆作为掩体。 */
    private void generateFactory(Random rnd) {
        for (int y = 0; y < C.MAP_H; y++) {
            for (int x = 0; x < C.MAP_W; x++) {
                boolean border = x < 2 || y < 2 || x >= C.MAP_W - 2 || y >= C.MAP_H - 2;
                map.set(x, y, border ? Tile.WALL : Tile.FLOOR, 1);
            }
        }

        // 三条主厂房：两条纵向、一条横向办公区，都做到三层
        addBuilding(4, 4, 11, 13, 3, 1);
        addBuilding(31, 4, 11, 13, 3, 2);
        addBuilding(17, 18, 12, 9, 2, 3);
        addBuilding(4, 30, 11, 7, 2, 4);
        addBuilding(31, 30, 11, 7, 2, 5);
        addBuilding(18, 3, 10, 7, 2, 6);

        // 中央通道的隔墙，制造巷道
        for (int y = 3; y < C.MAP_H - 3; y++) {
            if (y % 9 < 2) continue;
            map.set(16, y, Tile.WALL, 1);
            map.set(29, y, Tile.WALL, 1);
        }
        for (int y = 3; y < C.MAP_H - 3; y++) {
            if (y % 7 == 0) map.door(16, y, 1);
            if (y % 8 == 0) map.door(29, y, 1);
        }

        // 工厂特征：大量箱子堆成生产线，夹杂花盆
        for (int i = 0; i < 90; i++) {
            int x = 3 + rnd.nextInt(C.MAP_W - 6);
            int y = 3 + rnd.nextInt(C.MAP_H - 6);
            if (!map.inside(x, y)) continue;
            byte t = map.at(x, y);
            if (t != Tile.FLOOR) continue;
            if (map.destructAt(x, y) != GameMap.DESTRUCT_NONE) continue;
            map.crate(x, y, 1, 40);
        }
        // 柱子
        for (int i = 0; i < 16; i++) {
            int x = 4 + rnd.nextInt(C.MAP_W - 8);
            int y = 4 + rnd.nextInt(C.MAP_H - 8);
            if (map.at(x, y) == Tile.FLOOR && map.destructAt(x, y) == GameMap.DESTRUCT_NONE) {
                map.set(x, y, Tile.PILLAR, 1);
            }
        }

        spawns.add(new float[]{3.5f * C.TILE, 3.5f * C.TILE});
        spawns.add(new float[]{3.5f * C.TILE, 20.5f * C.TILE});
        spawns.add(new float[]{3.5f * C.TILE, 36.5f * C.TILE});
        spawns.add(new float[]{(C.MAP_W - 3.5f) * C.TILE, 35.5f * C.TILE});
        spawns.add(new float[]{(C.MAP_W - 3.5f) * C.TILE, 20.5f * C.TILE});
        spawns.add(new float[]{(C.MAP_W - 3.5f) * C.TILE, 3.5f * C.TILE});
        spawns.add(new float[]{(C.MAP_W * 0.5f) * C.TILE, 36.5f * C.TILE});
    }

    private void generate(Random rnd) {
        for (int y = 0; y < C.MAP_H; y++) {
            for (int x = 0; x < C.MAP_W; x++) {
                boolean border = x < 2 || y < 2 || x >= C.MAP_W - 2 || y >= C.MAP_H - 2;
                map.set(x, y, border ? Tile.WALL : Tile.GRASS, 0);
            }
        }

        // 主楼三层，四座侧楼两到三层，整体像一片街区
        addBuilding(17, 12, 12, 10, 3, 1);
        addBuilding(4, 24, 9, 10, 2, 2);
        addBuilding(33, 24, 9, 10, 3, 3);
        addBuilding(33, 5, 9, 6, 2, 4);
        addBuilding(4, 5, 8, 8, 2, 5);
        addBuilding(18, 27, 8, 8, 2, 6);

        // 建筑之间的巷道掩体
        scatterCrates(rnd, 6, 18, 22, 22, 0, 10);
        scatterCrates(rnd, 6, 6, 30, 10, 0, 8);
        scatterCrates(rnd, 14, 26, 28, 34, 0, 10);
        scatterCrates(rnd, 20, 3, 30, 9, 0, 6);

        // 柱子：小体型但挡视线，制造「柱子后面看不见」
        for (int i = 0; i < 10; i++) {
            int x = 4 + rnd.nextInt(C.MAP_W - 8);
            int y = 4 + rnd.nextInt(C.MAP_H - 8);
            if (map.at(x, y) == Tile.GRASS && map.levelAt(x, y) == 0) {
                map.set(x, y, Tile.PILLAR, 0);
            }
        }

        spawns.add(new float[]{4.5f * C.TILE, 4.5f * C.TILE});
        spawns.add(new float[]{3.5f * C.TILE, 20.5f * C.TILE});
        spawns.add(new float[]{4.5f * C.TILE, 36.5f * C.TILE});
        spawns.add(new float[]{(C.MAP_W - 4.5f) * C.TILE, 35.5f * C.TILE});
        spawns.add(new float[]{(C.MAP_W - 4.5f) * C.TILE, 20.5f * C.TILE});
        spawns.add(new float[]{(C.MAP_W - 4.5f) * C.TILE, 4.5f * C.TILE});
        spawns.add(new float[]{(C.MAP_W * 0.5f) * C.TILE, 35.5f * C.TILE});
    }

    private void addBuilding(int x, int y, int w, int h, int floors, int id) {
        buildings.add(new Building(x, y, w, h, floors));
        for (int ty = y; ty < y + h; ty++) {
            for (int tx = x; tx < x + w; tx++) {
                boolean edgeX = tx == x || tx == x + w - 1;
                boolean edgeY = ty == y || ty == y + h - 1;
                if (edgeX || edgeY) {
                    map.set(tx, ty, Tile.WALL, 1);
                } else {
                    map.set(tx, ty, Tile.FLOOR, 1);
                }
            }
        }
        // 两个门：南侧与北侧各一，位于建筑中段
        int mx = x + w / 2;
        map.set(mx, y + h - 1, Tile.FLOOR, 1);
        map.door(mx, y + h - 1, 1);
        map.set(x, y + h / 2, Tile.FLOOR, 1);
        map.door(x, y + h / 2, 1);

        // 室内隔墙，把一层分成两间
        int midY = y + h / 2;
        for (int tx = x + 1; tx < x + w - 1; tx++) {
            if (tx == x + w / 2) continue;
            map.set(tx, midY, Tile.WALL, 1);
        }
        map.set(x + w / 2, midY, Tile.DOOR, 1);
        map.doorState[map.idx(x + w / 2, midY)] = 0;

        // 楼梯：靠西南角，逐层上升
        for (int f = 1; f <= floors; f++) {
            int sx = x + 1 + (f - 1);
            int sy = y + 1;
            if (map.inside(sx, sy)) map.set(sx, sy, Tile.STAIR, f);
        }
        // 二层/三层楼板：建筑内部与楼层对应，非楼梯格标为对应层
        for (int f = 2; f <= floors; f++) {
            for (int ty = y + 1; ty < y + h - 1; ty++) {
                for (int tx = x + 1; tx < x + w - 1; tx++) {
                    int i = map.idx(tx, ty);
                    if (map.tiles[i] == Tile.STAIR) continue;
                    if (map.level[i] < f) map.level[i] = (byte) f;
                }
            }
        }
        // 室内掩体
        map.crate(x + 2, y + 3, 1, 40);
        map.crate(x + w - 3, y + 3, 1, 40);
        map.crate(x + 2, y + h - 3, 1, 40);
    }

    private void scatterCrates(Random rnd, int x0, int y0, int x1, int y1, int lvl, int count) {
        for (int i = 0; i < count; i++) {
            int x = x0 + rnd.nextInt(Math.max(1, x1 - x0));
            int y = y0 + rnd.nextInt(Math.max(1, y1 - y0));
            if (!map.inside(x, y)) continue;
            if (map.at(x, y) != Tile.GRASS && map.at(x, y) != Tile.FLOOR) continue;
            if (map.destructAt(x, y) != GameMap.DESTRUCT_NONE) continue;
            map.crate(x, y, lvl, 40);
        }
    }
}
