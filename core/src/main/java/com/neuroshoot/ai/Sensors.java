package com.neuroshoot.ai;

import com.neuroshoot.world.C;
import com.neuroshoot.world.GameMap;
import com.neuroshoot.world.Tile;
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.Visibility;
import com.neuroshoot.world.World;

/**
 * 把战场状态压成 20 个 [-1,1] 输入。
 *
 * 训练与实战共用同一份实现，保证训练时学会的输入含义在对战时完全一致。
 * 敌人信息只在「当前看得见」时给出，逼迫网络学会主动搜寻与利用视野。
 */
public final class Sensors {
    public static final int COUNT = C.INPUTS;

    private Sensors() {}

    public static void fill(Unit self, World world, float[] out) {
        Unit target = null;
        float bestD = Float.MAX_VALUE;
        for (Unit u : world.units) {
            if (!u.alive || u.team == self.team) continue;
            float d = (float) Math.hypot(u.x - self.x, u.y - self.y);
            if (d < bestD) {
                bestD = d;
                target = u;
            }
        }
        boolean seen = target != null && Visibility.unitVisible(world.map, self, target);

        out[0] = self.hp / (float) C.HP_MAX * 2f - 1f;
        out[1] = self.bleeding ? 1f : -1f;
        out[2] = self.fractured ? 1f : -1f;
        out[3] = self.crouch ? 1f : -1f;
        out[4] = self.aim ? 1f : -1f;
        out[5] = self.ammo / (float) Math.max(1, self.ammoMax) * 2f - 1f;
        out[6] = self.fireCd > 0f ? -1f : 1f;
        out[7] = self.panicking() ? 1f : -1f;

        if (target == null) {
            out[8] = 0f;
            out[9] = 0f;
            out[10] = 0f;
            out[11] = 0f;
            out[12] = 0f;
        } else {
            float dx = target.x - self.x, dy = target.y - self.y;
            float dist = Math.max(1f, (float) Math.hypot(dx, dy));
            out[8] = seen ? 1f : -1f;
            out[9] = dx / 600f;
            out[10] = dy / 600f;
            out[11] = 1f - Math.min(1f, dist / 700f);
            out[12] = Visibility.normalize((float) Math.atan2(dy, dx) - self.angle) / 3.14f;
        }

        out[13] = world.map.lineOfSight(self.x, self.y,
                self.x + (float) Math.cos(self.angle) * 220f,
                self.y + (float) Math.sin(self.angle) * 220f) ? 1f : -1f;
        out[14] = 1f - Math.min(1f, wallDist(world.map, self, 1f, 0f) / 220f);
        out[15] = 1f - Math.min(1f, wallDist(world.map, self, -1f, 0f) / 220f);
        out[16] = 1f - Math.min(1f, wallDist(world.map, self, 0f, 1f) / 220f);
        out[17] = 1f - Math.min(1f, wallDist(world.map, self, 0f, -1f) / 220f);
        out[18] = 1f - Math.min(1f, distToCover(world.map, self) / 200f);
        out[19] = 1f;

        for (int i = 0; i < COUNT; i++) {
            if (out[i] > 1f) out[i] = 1f;
            else if (out[i] < -1f || Float.isNaN(out[i])) out[i] = -1f;
        }
    }

    private static float wallDist(GameMap map, Unit u, float dx, float dy) {
        for (float d = 16f; d <= 220f; d += 16f) {
            int tx = (int) ((u.x + dx * d) / C.TILE);
            int ty = (int) ((u.y + dy * d) / C.TILE);
            if (!map.inside(tx, ty) || map.at(tx, ty) == Tile.WALL) return d;
        }
        return 220f;
    }

    private static float distToCover(GameMap map, Unit u) {
        // 只查 4 个正交方向：足够代表「附近有没有掩体」，耗时降到四分之一
        for (float d = 24f; d <= 200f; d += 24f) {
            for (int a = 0; a < 4; a++) {
                double ang = Math.PI * 2 * a / 4;
                int tx = (int) ((u.x + Math.cos(ang) * d) / C.TILE);
                int ty = (int) ((u.y + Math.sin(ang) * d) / C.TILE);
                if (!map.inside(tx, ty)) continue;
                if (map.at(tx, ty) == Tile.WALL
                        || map.destructAt(tx, ty) != GameMap.DESTRUCT_NONE) {
                    return d;
                }
            }
        }
        return 200f;
    }
}
