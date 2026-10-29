package com.neuroshoot;

import com.neuroshoot.ai.Trainer;

/** 测单步耗时，判断 100 倍速是否可持续（60fps 下每帧预算 16.6ms）。 */
public final class SpeedCheck {
    public static void main(String[] args) {
        Trainer t = new Trainer(16, 7L);
        t.paused = false;
        for (int i = 0; i < 300; i++) t.tick(1f / 30f);  // 预热
        int n = 3000;
        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) t.tick(1f / 30f);
        long ms = (System.nanoTime() - t0) / 1_000_000L;
        double perStep = ms / (double) n;
        System.out.printf("单步 %.3f ms，100 倍速每帧约 50 步 → %.1f ms%n",
                perStep, perStep * 50);
        System.out.printf("跑完 %d 局，第 %d 代%n", t.totalRounds, t.generation());
        if (perStep * 50 > 16.6) {
            System.out.println("警告：100 倍速会掉到 30fps 以下，需要降低仿真成本");
        } else {
            System.out.println("100 倍速可维持 60fps");
        }
    }
}
