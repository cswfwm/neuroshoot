package com.neuroshoot.ai;

/**
 * 族群倾向：同一个网络结构，靠不同的适应度权重分化出不同打法。
 *
 * 四类倾向并行训练，每类只按自己关心的指标给分：
 *  - 击杀型 只看输出：击杀、伤害；
 *  - 生存型 只看活着：存活时长、残血逃生；
 *  - 躲避型 只看规避：不受伤、拉开距离、利用掩体；
 *  - 跑图型 只看机动：覆盖过的格子数、走过的路程。
 *
 * 互相对战时，适应度会叠加「击败其他倾向的加分」，于是四种打法被逼着互相克制。
 */
public final class Bias {
    public enum Kind { KILL, SURVIVE, EVADE, EXPLORE }

    public final Kind kind;
    public final String name;
    public final String desc;

    private Bias(Kind kind, String name, String desc) {
        this.kind = kind;
        this.name = name;
        this.desc = desc;
    }

    public static final Bias KILL = new Bias(Kind.KILL, "击杀", "追求击杀与输出");
    public static final Bias SURVIVE = new Bias(Kind.SURVIVE, "生存", "追求活得更久");
    public static final Bias EVADE = new Bias(Kind.EVADE, "躲避", "追求少受伤、远离敌人");
    public static final Bias EXPLORE = new Bias(Kind.EXPLORE, "跑图", "追求覆盖地图、到处游走");

    public static final Bias[] ALL = {KILL, SURVIVE, EVADE, EXPLORE};

    /**
     * 按倾向计算一局的适应度。
     *
     * @param kills    本局击杀
     * @param damage   本局造成伤害
     * @param taken    本局承受伤害
     * @param survived 是否存活到结束
     * @param alive   本局存活秒数
     * @param tiles   本局新覆盖的格子数
     * @param dist    本局移动距离
     * @param minEnemyDist 与敌人保持的最短距离（越大说明越会拉开）
     */
    public float score(float kills, float damage, float taken, boolean survived,
                       float alive, float tiles, float dist, float minEnemyDist,
                       float aimOnTargetTime) {
        switch (kind) {
            case KILL:
                return kills * 6f + damage * 0.05f + aimOnTargetTime * 0.6f;
            case SURVIVE:
                return alive * 1.2f + (survived ? 6f : 0f) - taken * 0.02f;
            case EVADE:
                return -taken * 0.12f + minEnemyDist * 0.02f + (survived ? 3f : 0f)
                        + tiles * 0.05f;
            case EXPLORE:
                return tiles * 0.35f + dist * 0.02f;
            default:
                return 0f;
        }
    }
}
