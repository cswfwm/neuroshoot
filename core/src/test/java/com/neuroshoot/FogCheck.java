package com.neuroshoot;

import com.neuroshoot.world.C;
import com.neuroshoot.world.MapFactory;
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.Visibility;

/** 验证开局视野内可见格数不为 0，否则迷雾会盖住整个屏幕。 */
public final class FogCheck {
    public static void main(String[] args) {
        for (long seed : new long[]{20261027L, 1L, 12345L}) {
            MapFactory f = MapFactory.create(seed);
            for (int s = 0; s < f.spawns.size(); s++) {
                Unit u = new Unit();
                u.x = f.spawns.get(s)[0];
                u.y = f.spawns.get(s)[1];
                u.angle = 0f;
                int cx = (int) (u.x / C.TILE), cy = (int) (u.y / C.TILE);
                int vis = 0;
                for (int ty = 0; ty < C.MAP_H; ty++) {
                    for (int tx = 0; tx < C.MAP_W; tx++) {
                        boolean near = Math.abs(tx - cx) <= 1 && Math.abs(ty - cy) <= 1;
                        if (near || Visibility.tileVisible(f.map, u, tx, ty)) vis++;
                    }
                }
                if (vis < 9) throw new AssertionError("seed " + seed + " 出生点" + s
                        + " 可见格过少: " + vis);
            }
        }
        System.out.println("所有出生点视野可见格数正常（含近身兜底 9 格）");
    }
}
