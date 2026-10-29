package com.neuroshoot.ai;

import com.neuroshoot.world.C;
import com.neuroshoot.world.GameMap;
import com.neuroshoot.world.Tile;

/**
 * 网格寻路：BFS（广度优先）在 64px 格子上找最短路。
 *
 * 为什么用 BFS 而不是 A*：地图只有 46×40 格，整图搜索的开销可以忽略；
 * 而 8 方向带斜向夹角的 A* 需要额外的一致性处理才不会重复入堆，
 * BFS 每格只入队一次、天然最短路，代码简单且没有启发式失配的问题。
 *
 * 门在寻路里视为可通行，AI 走到门口会自动推开，因此能真正进屋。
 * 为了减少斜向贴墙的抖动，允许「先直走再斜走」，等价于 8 方向的距离。
 */
public final class Pathfinder {
    private static final int W = C.MAP_W;
    private static final int H = C.MAP_H;
    private static final int SIZE = W * H;

    /** 每个格子的前驱，用于回溯路径；-2 表示未访问。 */
    private final int[] prev = new int[SIZE];
    private final int[] queue = new int[SIZE];

    /** 结果路径（格子索引），由 findPath 填充。path[0] 是起点。 */
    public final int[] path = new int[SIZE];
    public int pathLen;

    private static final int[] DX = {1, -1, 0, 0, 1, 1, -1, -1};
    private static final int[] DY = {0, 0, 1, -1, 1, -1, 1, -1};
    /** 前 4 个是正交方向，后 4 个是斜向。BFS 先展开正交，路径更贴直线。 */
    private static final int ORTHO = 4;

    public boolean findPath(GameMap map, int sx, int sy, int tx, int ty) {
        pathLen = 0;
        if (!map.inside(sx, sy) || !map.inside(tx, ty)) return false;
        int start = sy * W + sx;
        int goal = ty * W + tx;
        if (start == goal) {
            path[0] = start;
            pathLen = 1;
            return true;
        }
        if (!walkable(map, tx, ty)) return false;

        java.util.Arrays.fill(prev, -2);
        int head = 0, tail = 0;
        queue[tail++] = start;
        prev[start] = -1;

        while (head < tail) {
            int cur = queue[head++];
            if (cur == goal) {
                rebuild(goal);
                return true;
            }
            int cx = cur % W, cy = cur / W;
            for (int d = 0; d < 8; d++) {
                int nx = cx + DX[d];
                int ny = cy + DY[d];
                if (!map.inside(nx, ny)) continue;
                int ni = ny * W + nx;
                if (prev[ni] != -2) continue;
                if (!walkable(map, nx, ny)) continue;
                // 斜向不允许穿过两个实心格的夹角，否则会「贴墙角瞬移」
                if (d >= ORTHO && (!walkable(map, cx + DX[d], cy)
                        || !walkable(map, cx, cy + DY[d]))) continue;
                prev[ni] = cur;
                queue[tail++] = ni;
            }
        }
        return false;
    }

    /** 可通行：地面/草地/楼梯，以及门（关着的门也走，到门口会自动开）。 */
    public static boolean walkable(GameMap map, int x, int y) {
        if (!map.inside(x, y)) return false;
        if (map.destructAt(x, y) != GameMap.DESTRUCT_NONE && map.destructHpAt(x, y) > 0) {
            return false;
        }
        byte t = map.at(x, y);
        if (t == Tile.DOOR) return true;
        return !Tile.solid(t);
    }

    private void rebuild(int goal) {
        pathLen = 0;
        int cur = goal;
        while (cur >= 0 && pathLen < path.length) {
            path[pathLen++] = cur;
            cur = prev[cur];
        }
        for (int i = 0, j = pathLen - 1; i < j; i++, j--) {
            int t = path[i];
            path[i] = path[j];
            path[j] = t;
        }
    }

    /** 从起点到终点的格子数，找不到返回 -1。供自检与调试使用。 */
    public int distance(GameMap map, int sx, int sy, int tx, int ty) {
        return findPath(map, sx, sy, tx, ty) ? pathLen - 1 : -1;
    }
}
