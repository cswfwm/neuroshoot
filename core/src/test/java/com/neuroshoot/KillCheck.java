package com.neuroshoot;

import com.neuroshoot.ai.Trainer;

/** 验证训练中击杀会进入统计与提示。 */
public final class KillCheck {
    public static void main(String[] args) {
        Trainer t = new Trainer(12, 20261028L);
        t.paused = false;
        int guard = 0;
        int maxKills = 0;
        int feedSeen = 0;
        while (guard++ < 60000) {
            t.tick(1f / 30f);
            maxKills = Math.max(maxKills, t.genKills);
            if (!t.killFeed.isEmpty()) feedSeen = Math.max(feedSeen, t.killFeed.size());
            if (t.totalRounds >= 6) break;
        }
        System.out.println("跑完 " + t.totalRounds + " 局，本代击杀 " + t.genKills
                + "，峰值 " + maxKills + "，提示条数峰值 " + feedSeen
                + "，最近提示: " + t.lastKillText);
        if (maxKills == 0) throw new AssertionError("击杀数始终为 0");
        if (feedSeen == 0) throw new AssertionError("击杀提示从未出现");
        System.out.println("击杀统计与提示：通过");
    }
}
