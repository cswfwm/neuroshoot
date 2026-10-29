package com.neuroshoot.world;

/** 地图数据：地形、层高、门与可破坏物。纯逻辑，不依赖绘图。 */
public final class GameMap {
    public static final byte DESTRUCT_NONE = 0;
    public static final byte DESTRUCT_CRATE = 1;

    public final byte[] tiles = new byte[C.MAP_W * C.MAP_H];
    /** 每格所在楼层，0 为室外，1~3 为建筑楼层。 */
    public final byte[] level = new byte[C.MAP_W * C.MAP_H];
    /** 门的状态：0 关、1 半开、2 全开、3 破损。 */
    public final byte[] doorState = new byte[C.MAP_W * C.MAP_H];
    /** 可破坏物类型。 */
    public final byte[] destruct = new byte[C.MAP_W * C.MAP_H];
    /** 可破坏物血量。 */
    public final byte[] destructHp = new byte[C.MAP_W * C.MAP_H];

    public int idx(int x, int y) {
        return y * C.MAP_W + x;
    }

    public boolean inside(int x, int y) {
        return x >= 0 && y >= 0 && x < C.MAP_W && y < C.MAP_H;
    }

    public byte at(int x, int y) {
        return inside(x, y) ? tiles[idx(x, y)] : Tile.WALL;
    }

    public byte levelAt(int x, int y) {
        return inside(x, y) ? level[idx(x, y)] : 0;
    }

    public boolean solidAt(int x, int y) {
        if (!inside(x, y)) return true;
        int i = idx(x, y);
        if (destruct[i] == DESTRUCT_CRATE && destructHp[i] > 0) return true;
        return Tile.solid(tiles[i]);
    }

    public boolean blocksSightAt(int x, int y) {
        if (!inside(x, y)) return true;
        int i = idx(x, y);
        if (destruct[i] == DESTRUCT_CRATE && destructHp[i] > 0) return true;
        return Tile.blocksSight(tiles[i]);
    }

    public boolean blocksBulletAt(int x, int y) {
        if (!inside(x, y)) return true;
        int i = idx(x, y);
        if (destruct[i] == DESTRUCT_CRATE && destructHp[i] > 0) return true;
        return Tile.solid(tiles[i]);
    }

    public void set(int x, int y, byte t, int lvl) {
        if (!inside(x, y)) return;
        int i = idx(x, y);
        tiles[i] = t;
        level[i] = (byte) lvl;
    }

    public void crate(int x, int y, int lvl, int hp) {
        if (!inside(x, y)) return;
        int i = idx(x, y);
        tiles[i] = Tile.FLOOR;
        level[i] = (byte) lvl;
        destruct[i] = DESTRUCT_CRATE;
        destructHp[i] = (byte) hp;
    }

    public void door(int x, int y, int lvl) {
        if (!inside(x, y)) return;
        int i = idx(x, y);
        tiles[i] = Tile.DOOR;
        level[i] = (byte) lvl;
        doorState[i] = 0;
    }

    public void openDoor(int x, int y) {
        if (!inside(x, y)) return;
        int i = idx(x, y);
        if (tiles[i] != Tile.DOOR) return;
        if (doorState[i] < 2) doorState[i] = 2;
    }

    /** 破门：只有霰弹枪与爆炸能走到这个分支。 */
    public void breakDoor(int x, int y) {
        if (!inside(x, y)) return;
        int i = idx(x, y);
        if (tiles[i] != Tile.DOOR) return;
        doorState[i] = 3;
    }

    public boolean doorPassable(int x, int y) {
        if (!inside(x, y)) return false;
        int i = idx(x, y);
        return tiles[i] == Tile.DOOR && doorState[i] >= 2;
    }

    /** 伤害一个可破坏物，返回是否被打碎。 */
    public boolean damageCrate(int x, int y, int dmg) {
        if (!inside(x, y)) return false;
        int i = idx(x, y);
        if (destruct[i] != DESTRUCT_CRATE || destructHp[i] <= 0) return false;
        destructHp[i] = (byte) Math.max(0, destructHp[i] - dmg);
        return destructHp[i] == 0;
    }

    public byte destructAt(int x, int y) {
        return inside(x, y) ? destruct[idx(x, y)] : DESTRUCT_NONE;
    }

    public int destructHpAt(int x, int y) {
        return inside(x, y) ? destructHp[idx(x, y)] : 0;
    }

    /** 视线检测：沿线段采样格心，遇到挡视线的格子即失败。 */
    /**
     * 视线检测：Bresenham 整数遍历格子，只有跨越边界时才换格。
     *
     * 比「按固定步长采样再转格子坐标」快很多——后者在训练里每帧被调用上千次，
     * 是 100 倍速卡顿的主因。
     */
    public boolean lineOfSight(float x0, float y0, float x1, float y1) {
        int tx = (int) (x0 / C.TILE);
        int ty = (int) (y0 / C.TILE);
        int ex = (int) (x1 / C.TILE);
        int ey = (int) (y1 / C.TILE);
        int dx = Math.abs(ex - tx);
        int dy = Math.abs(ey - ty);
        int sx = tx < ex ? 1 : -1;
        int sy = ty < ey ? 1 : -1;
        int err = dx - dy;
        int guard = dx + dy + 2;
        while (guard-- > 0) {
            if (tx != (int) (x0 / C.TILE) || ty != (int) (y0 / C.TILE)) {
                if (blocksSightAt(tx, ty)) return false;
            }
            if (tx == ex && ty == ey) return true;
            int e2 = err * 2;
            if (e2 > -dy) {
                err -= dy;
                tx += sx;
            }
            if (e2 < dx) {
                err += dx;
                ty += sy;
            }
        }
        return true;
    }

    /** 子弹射线：碰到 solid 或箱子就停，返回命中点比例（0~1）。 */
    public float raycastSolid(float x0, float y0, float x1, float y1) {
        float dx = x1 - x0, dy = y1 - y0;
        float dist = (float) Math.hypot(dx, dy);
        int steps = (int) (dist / (C.TILE * 0.25f)) + 1;
        for (int s = 1; s <= steps; s++) {
            float t = s / (float) steps;
            int tx = (int) ((x0 + dx * t) / C.TILE);
            int ty = (int) ((y0 + dy * t) / C.TILE);
            if (blocksBulletAt(tx, ty)) return (s - 1) / (float) steps;
        }
        return 1f;
    }
}
