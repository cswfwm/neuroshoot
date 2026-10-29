package com.neuroshoot;

import com.neuroshoot.ai.Brain;
import com.neuroshoot.loadout.Loadout;
import com.neuroshoot.world.C;
import com.neuroshoot.world.MapFactory;
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.Visibility;
import com.neuroshoot.world.World;

/** 验证 AI 能上楼，且楼层之间互相看不见。 */
public final class FloorCheck {
    public static void main(String[] args) {
        MapFactory f = MapFactory.create(20261028L);
        World w = new World(f);
        Unit u = new Unit();
        u.id = 0; u.team = 0; u.ai = true;
        w.addUnit(u, Loadout.defaults("rifle"), 0);
        Unit e = new Unit();
        e.id = 1; e.team = 1; e.ai = true;
        w.addUnit(e, Loadout.defaults("rifle"), 3);
        Brain b = Brain.scripted();

        int[] floorFrames = new int[4];
        int maxFloor = 0;
        for (int i = 0; i < 60 * 120; i++) {
            b.update(u, w, 1f / 60f);
            w.step(1f / 60f);
            int fl = Math.min(3, Math.max(0, u.floor));
            floorFrames[fl]++;
            maxFloor = Math.max(maxFloor, u.floor);
        }
        System.out.printf("楼层分布: 0层%.0fs 1层%.0fs 2层%.0fs 3层%.0fs，最高到达 %d 层%n",
                floorFrames[0] / 60f, floorFrames[1] / 60f,
                floorFrames[2] / 60f, floorFrames[3] / 60f, maxFloor);
        if (maxFloor < 2) throw new AssertionError("AI 没有上到二层，最高 " + maxFloor);

        // 楼层隔离：同位置不同楼层不可见
        Unit a = new Unit();
        a.x = u.x; a.y = u.y; a.floor = 1; a.team = 0;
        Unit c = new Unit();
        c.x = u.x; c.y = u.y; c.floor = 2; c.team = 1;
        if (Visibility.unitVisible(f.map, a, c)) {
            throw new AssertionError("不同楼层仍然可见");
        }
        System.out.println("楼层隔离：不同层互相不可见，通过");
        System.out.println("AI 上楼：通过");
    }
}
