package com.neuroshoot;

import com.neuroshoot.world.C;
import com.neuroshoot.world.MapFactory;
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.Visibility;

/** 检查玩家出生点周围有多少格可见；若为 0，迷雾就会盖满全屏。 */
public final class VisCheck {
    public static void main(String[] args) {
        MapFactory f = MapFactory.create(20261027L);
        for (int s = 0; s < f.spawns.size(); s++) {
            Unit u = new Unit();
            u.x = f.spawns.get(s)[0];
            u.y = f.spawns.get(s)[1];
            u.angle = 0f;
            int vis = 0, total = 0;
            int r = (int) (C.VIEW_STAND / C.TILE) + 1;
            int cx = (int) (u.x / C.TILE), cy = (int) (u.y / C.TILE);
            for (int ty = Math.max(0, cy - r); ty <= Math.min(C.MAP_H - 1, cy + r); ty++) {
                for (int tx = Math.max(0, cx - r); tx <= Math.min(C.MAP_W - 1, cx + r); tx++) {
                    total++;
                    if (Visibility.tileVisible(f.map, u, tx, ty)) vis++;
                }
            }
            System.out.printf("出生点%d (%.0f,%.0f) 可见 %d / %d 格，所在格实心=%b%n",
                    s, u.x, u.y, vis, total,
                    f.map.solidAt((int) (u.x / C.TILE), (int) (u.y / C.TILE)));
        }
    }
}
