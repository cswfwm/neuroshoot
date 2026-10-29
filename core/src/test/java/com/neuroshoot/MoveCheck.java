package com.neuroshoot;

import com.neuroshoot.ai.Trainer;
import com.neuroshoot.world.Unit;

/** 检查单帧位移是否平滑：不应出现非法小跳变（除换局重生）。 */
public final class MoveCheck {
    public static void main(String[] args) {
        Trainer t = new Trainer(12, 99L);
        t.paused = false;
        int[] hist = new int[6];
        float maxD = 0f;
        Unit prev = null;
        float px = 0, py = 0;
        for (int i = 0; i < 1200; i++) {
            int roundsBefore = t.roundsDone;
            t.tick(Trainer.DT);
            Unit u = t.fighters.get(0);
            if (t.roundsDone != roundsBefore) { px = u.x; py = u.y; continue; }
            float d = (float) Math.hypot(u.x - px, u.y - py);
            px = u.x; py = u.y;
            if (d < 1) hist[0]++;
            else if (d < 4) hist[1]++;
            else if (d < 8) hist[2]++;
            else if (d < 14) hist[3]++;
            else if (d < 20) hist[4]++;
            else hist[5]++;
            maxD = Math.max(maxD, d);
        }
        System.out.printf("单帧位移分布 <1:%d 1-4:%d 4-8:%d 8-14:%d 14-20:%d >20:%d 最大%.1fpx%n",
                hist[0], hist[1], hist[2], hist[3], hist[4], hist[5], maxD);
        System.out.printf("速度上限 %.0f px/s，单帧(1/30s)理论最大 %.1f px%n",
                com.neuroshoot.world.C.MOVE_SPEED, com.neuroshoot.world.C.MOVE_SPEED / 30f);
    }
}
