package com.neuroshoot;

import com.neuroshoot.loadout.Loadout;
import com.neuroshoot.world.C;
import com.neuroshoot.world.MapFactory;
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.World;

/** 验证子弹记录了发射原点，弹道线长度随飞行增长。 */
public final class TracerCheck {
    public static void main(String[] args) {
        MapFactory f = MapFactory.create(7L);
        World w = new World(f);
        Unit u = new Unit();
        u.id = 0; u.team = 0;
        w.addUnit(u, Loadout.defaults("m4a1"), 0);
        u.x = 10 * C.TILE; u.y = 10 * C.TILE; u.angle = 0f;
        u.fireCd = 0f;
        w.fire(u);
        if (w.bullets.isEmpty()) throw new AssertionError("没有生成子弹");
        World.Bullet b = w.bullets.get(0);
        if (b.ox == 0f && b.oy == 0f) throw new AssertionError("没有记录发射原点");
        float ox = b.ox, oy = b.oy;
        // 只推进 1 帧，保证子弹还在飞行中
        w.step(1f / 120f);
        if (!w.bullets.isEmpty()) {
            b = w.bullets.get(0);
            float len = (float) Math.hypot(b.x - b.ox, b.y - b.oy);
            System.out.printf("发射原点 (%.0f,%.0f) → 当前 (%.0f,%.0f)，弹道长度 %.0f px%n",
                    ox, oy, b.x, b.y, len);
            if (len < 5f) throw new AssertionError("弹道没有延伸: " + len);
            // 方向应笔直：origin→bullet 与速度方向一致
            float ang1 = (float) Math.atan2(b.y - b.oy, b.x - b.ox);
            float ang2 = (float) Math.atan2(b.vy, b.vx);
            float diff = Math.abs(com.neuroshoot.world.Visibility.normalize(ang1 - ang2));
            if (diff > 0.01f) throw new AssertionError("弹道不笔直，偏差 " + diff);
            System.out.println("弹道笔直：通过");
        }
        System.out.println("弹道线：通过");
    }
}
