package com.neuroshoot;

import com.neuroshoot.ai.Trainer;

/** 模拟 100 倍速：每帧推进 50 步，验证代际推进速度与实际耗时。 */
public final class TurboCheck {
    public static void main(String[] args) {
        Trainer t = new Trainer(16, 20261028L);
        t.paused = false;
        t.speed = 100;
        // 预热
        for (int i = 0; i < 200; i++) t.tick(1f / 30f);
        int gen0 = t.generation();
        int rounds0 = t.totalRounds;
        long t0 = System.nanoTime();
        int frames = 0;
        // 模拟 3 秒真实时间、100 倍速：60fps 下约 180 帧，每帧 50 步
        while (frames < 180) {
            for (int s = 0; s < 50; s++) t.tick(1f / 30f);
            frames++;
        }
        long ms = (System.nanoTime() - t0) / 1_000_000L;
        double simSeconds = frames * 50 / 30.0;
        System.out.printf("真实耗时 %.2f s，推进仿真 %.1f s（约 %.0f 倍）%n",
                ms / 1000.0, simSeconds, simSeconds / (ms / 1000.0));
        System.out.printf("局数 %d → %d，代数 %d → %d%n",
                rounds0, t.totalRounds, gen0, t.generation());
        for (int i = 0; i < t.species.length; i++) {
            System.out.printf("  %s 代%d 最佳%.0f%n", t.species[i].name,
                    t.species[i].generation, t.species[i].bestFitness);
        }
        if (t.totalRounds - rounds0 < 8) throw new AssertionError("推进局数过少");
        if (t.generation() <= gen0) throw new AssertionError("没有产生新一代");
        System.out.println("100 倍速训练：通过");
    }
}
