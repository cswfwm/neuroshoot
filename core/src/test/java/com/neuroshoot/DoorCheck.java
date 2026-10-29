package com.neuroshoot;

import com.neuroshoot.ai.Brain;
import com.neuroshoot.ai.NeuralBrain;
import com.neuroshoot.ai.Net;
import com.neuroshoot.ai.Evolver;
import com.neuroshoot.ai.Pathfinder;
import com.neuroshoot.loadout.Loadout;
import com.neuroshoot.world.C;
import com.neuroshoot.world.MapFactory;
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.World;

/** 验证寻路能穿过关闭的门进入建筑内部。 */
public final class DoorCheck {
    public static void main(String[] args) {
        MapFactory f = MapFactory.create(20261028L);
        Pathfinder pf = new Pathfinder();
        // 找一个室内格与室外格
        int inX = -1, inY = -1, outX = -1, outY = -1;
        for (int y = 2; y < C.MAP_H - 2 && inX < 0; y++) {
            for (int x = 2; x < C.MAP_W - 2; x++) {
                if (f.map.levelAt(x, y) >= 1 && Pathfinder.walkable(f.map, x, y)) {
                    inX = x; inY = y; break;
                }
            }
        }
        for (int y = 2; y < C.MAP_H - 2 && outX < 0; y++) {
            for (int x = 2; x < C.MAP_W - 2; x++) {
                if (f.map.levelAt(x, y) == 0 && Pathfinder.walkable(f.map, x, y)) {
                    outX = x; outY = y; break;
                }
            }
        }
        System.out.println("室内格(" + inX + "," + inY + ") 室外格(" + outX + "," + outY + ")");
        boolean ok = pf.findPath(f.map, outX, outY, inX, inY);
        System.out.println("寻路结果: " + ok + "，路径长度 " + pf.pathLen);
        if (!ok) throw new AssertionError("找不到进入建筑的路");

        // 实际跑一个脚本 AI，看它能否真的进屋
        World w = new World(f);
        Unit u = new Unit();
        u.id = 0; u.team = 0; u.ai = true;
        u.x = (outX + 0.5f) * C.TILE;
        u.y = (outY + 0.5f) * C.TILE;
        w.units.add(u);
        u.spawn(Loadout.defaults("rifle"), u.x, u.y);
        Brain b = Brain.scripted();
        // 放一个敌人在室内，促使 AI 进去
        Unit e = new Unit();
        e.id = 1; e.team = 1; e.ai = true;
        e.spawn(Loadout.defaults("rifle"), (inX + 0.5f) * C.TILE, (inY + 0.5f) * C.TILE);
        w.units.add(e);

        int indoorFrames = 0;
        for (int i = 0; i < 60 * 90; i++) {
            b.update(u, w, 1f / 60f);
            w.step(1f / 60f);
            if (f.map.levelAt((int) (u.x / C.TILE), (int) (u.y / C.TILE)) >= 1) indoorFrames++;
        }
        float indoorSec = indoorFrames / 60f;
        System.out.printf("90 秒内处于室内的时间: %.1f 秒%n", indoorSec);
        if (indoorSec < 5f) throw new AssertionError("AI 没能进入建筑内部");
        System.out.println("AI 进屋：通过");
    }
}
