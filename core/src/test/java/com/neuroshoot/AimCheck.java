package com.neuroshoot;

import com.neuroshoot.ai.Brain;
import com.neuroshoot.ai.NeuralBrain;
import com.neuroshoot.ai.Net;
import com.neuroshoot.ai.Evolver;
import com.neuroshoot.loadout.Loadout;
import com.neuroshoot.world.C;
import com.neuroshoot.world.MapFactory;
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.World;

/** 对比：AI 在完全对准时是否开火，以及命中率。 */
public final class AimCheck {
    public static void main(String[] args) {
        int shots = 0, hits = 0;
        for (int trial = 0; trial < 30; trial++) {
            MapFactory f = MapFactory.create(7L + trial);
            World w = new World(f);
            Unit a = new Unit();
            a.id = 0; a.team = 0;
            w.addUnit(a, Loadout.defaults("rifle"), 0);
            Unit b = new Unit();
            b.id = 1; b.team = 1;
            w.addUnit(b, Loadout.defaults("rifle"), 3);
            // 摆在空旷处正对彼此
            a.x = 10 * C.TILE; a.y = 10 * C.TILE; a.angle = 0f;
            b.x = 10 * C.TILE + 300f; b.y = 10 * C.TILE; b.angle = (float) Math.PI;
            NeuralBrain brainA = new NeuralBrain(Net.fromGenes(
                    Evolver.randomGenes(new java.util.Random(trial))));
            Brain brainB = Brain.scripted();
            int s0 = 0, h0 = 0;
            for (int i = 0; i < 240; i++) {
                brainA.update(a, w, Trainer_dt());
                brainB.update(b, w, Trainer_dt());
                // 强制瞄准对手，检验「对准就开火」
                a.angle = (float) Math.atan2(b.y - a.y, b.x - a.x);
                b.angle = (float) Math.atan2(a.y - b.y, a.x - b.x);
                w.step(Trainer_dt());
                a.x = 10 * C.TILE; a.y = 10 * C.TILE;
                b.x = 10 * C.TILE + 300f; b.y = 10 * C.TILE;
            }
            shots += w.totalShotsOf(0);
            hits += w.totalHitsOf(0);
        }
        System.out.printf("AI 开火 %d 发，命中 %d 发，命中率 %.0f%%%n",
                shots, hits, shots == 0 ? 0 : hits * 100f / shots);
        if (shots == 0) throw new AssertionError("AI 完全没开火");
        if (hits * 100f / shots < 40) throw new AssertionError("命中率过低");
        System.out.println("瞄准即开火：通过");
    }

    private static float Trainer_dt() {
        return 1f / 30f;
    }
}
