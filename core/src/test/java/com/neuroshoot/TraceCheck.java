package com.neuroshoot;

import com.neuroshoot.ai.Trainer;
import com.neuroshoot.world.Unit;

/** 临时诊断：逐帧打印某个训练单位的位置与速度，找出瞬移与抽搐的来源。 */
public final class TraceCheck {
    public static void main(String[] args) {
        Trainer t = new Trainer(12, 99L);
        t.paused = false;
        Unit u = t.fighters.get(0);
        float px = u.x, py = u.y;
        int big = 0;
        for (int i = 0; i < 900; i++) {
            int genBefore = t.generation;
            int roundBefore = t.roundsDone;
            t.tick(Trainer.DT);
            u = t.fighters.get(0);
            float d = (float) Math.hypot(u.x - px, u.y - py);
            if (d > 45f) {
                big++;
                System.out.printf("i=%d 跳变 %.1f px  pos=(%.0f,%.0f) v=(%.0f,%.0f) 回合%d 代%d%n",
                        i, d, u.x, u.y, u.vx, u.vy, t.roundsDone, t.generation);
            }
            if (t.generation != genBefore) {
                System.out.printf("i=%d 换代 %d→%d 回合%d%n", i, genBefore, t.generation,
                        t.roundsDone);
            }
            px = u.x;
            py = u.y;
            if (big > 12) break;
        }
        System.out.println("大跳变次数 " + big + " 最终代数 " + t.generation
                + " 回合数 " + t.roundsDone);
    }
}
