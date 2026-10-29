package com.neuroshoot;

import com.neuroshoot.ai.Bias;
import com.neuroshoot.ai.Trainer;
import com.neuroshoot.loadout.Consumable;
import com.neuroshoot.loadout.Loadout;
import com.neuroshoot.world.C;
import com.neuroshoot.world.MapFactory;
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.World;

/** 验证四族群训练、投掷物飞行与落点。 */
public final class FourCheck {
    public static void main(String[] args) {
        System.out.println("族群数: " + Bias.ALL.length + " → "
                + Bias.KILL.name + "/" + Bias.SURVIVE.name + "/"
                + Bias.EVADE.name + "/" + Bias.EXPLORE.name);
        Trainer t = new Trainer(12, 20261028L);
        t.paused = false;
        int guard = 0;
        while (t.generation() < 7 && guard++ < 800000) t.tick(Trainer.DT);
        System.out.println("跑完 " + t.totalRounds + " 局，第 " + t.generation() + " 代，筛选 "
                + t.screenCount + " 次");
        for (int i = 0; i < t.species.length; i++) {
            System.out.printf("  %s 代%d 最佳%.1f 平均%.1f 淘汰%d次%n",
                    t.species[i].name, t.species[i].generation,
                    t.species[i].bestFitness, t.species[i].avgFitness,
                    t.species[i].eliminations);
        }
        if (t.screenCount == 0) throw new AssertionError("没有发生筛选");

        // 投掷物飞行检查
        MapFactory f = MapFactory.create(7L);
        World w = new World(f);
        Unit u = new Unit();
        u.id = 0;
        u.team = 0;
        w.addUnit(u, Loadout.defaults("rifle"), 0);
        u.throwables.clear();
        u.throwables.add("grenade");
        float sx = u.x, sy = u.y;
        w.throwItem(u, "grenade", u.x + 300f, u.y);
        float ox = w.throwables.get(0).x;
        if (Math.abs(ox - sx) < 5f) throw new AssertionError("投掷物没有出手");
        float maxD = 0f;
        for (int i = 0; i < 200; i++) {
            w.step(1f / 60f);
            if (w.throwables.isEmpty()) break;
            World.Throwable t2 = w.throwables.get(0);
            maxD = Math.max(maxD, (float) Math.hypot(t2.x - sx, t2.y - sy));
        }
        System.out.printf("投掷物飞出 %.0f px（出手点偏移 %.0f px）%n", maxD, Math.abs(ox - sx));
        if (maxD < 80f) throw new AssertionError("投掷物几乎没飞出去: " + maxD);
        System.out.println("四族群与投掷逻辑全部通过");
    }
}
