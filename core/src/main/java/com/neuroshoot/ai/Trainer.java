package com.neuroshoot.ai;

import com.neuroshoot.loadout.Loadout;
import com.neuroshoot.world.C;
import com.neuroshoot.world.MapFactory;
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 四族群并行训练器。
 *
 * 每局把四个族群各派 2 名上场（共 8 人混战），一局结束后按各族的倾向分别打分：
 * 击杀型看输出、生存型看活着、躲避型看少受伤、跑图型看覆盖面积。
 * 打到若干局后各族繁衍一代；每隔若干代做一次筛选，把长期垫底的族群清空重来，
 * 并把冠军族群的一半基因注入其它族群，避免四个族群退化成同一种打法。
 */
public final class Trainer {
    public static final float DT = 1f / 30f;
    /** 每局上场人数上限，四个族群平分。 */
    public static final int PER_SPECIES = 2;
    /** 打多少局繁衍一代。 */
    public static final int ROUNDS_PER_GEN = 4;
    /** 多少代做一次淘汰。 */
    public static final int GEN_PER_SCREEN = 5;
    public static final float ROUND_TIME = 24f;

    public final Species[] species = new Species[Bias.ALL.length];
    public World world;
    public final List<Unit> fighters = new ArrayList<>();
    public boolean paused = true;
    public int speed = 3;
    public int totalRounds;
    /** 本代与上一代的击杀、命中统计。 */
    public int genKills;
    public int genHits;
    public int lastGenKills;
    public int lastGenHits;
    /** 最近一次击杀的文字，供界面滚动显示。 */
    public String lastKillText = "";
    public float lastKillAge = 0f;
    public final java.util.List<String> killFeed = new java.util.ArrayList<>();
    public final java.util.List<Float> killAges = new java.util.ArrayList<>();
    public int screenCount;
    /** 最近被淘汰的族群名，供界面提示。 */
    public String lastCulled = "";
    public String lastChampion = "";
    /** 每个族群近若干代的曲线。 */
    public final float[][] curves = new float[Bias.ALL.length][48];
    public int curveLen;

    private final Random rnd;
    private final int population;
    private float roundTime;
    private int roundsInGen;
    private int genSinceScreen;
    private int[] lineup;

    public Trainer(int population, long seed) {
        NeuralBrain.setTrainingFast(true);
        this.population = population;
        rnd = new Random(seed);
        for (int i = 0; i < species.length; i++) {
            species[i] = new Species(Bias.ALL[i], Bias.ALL[i].name + "族", population,
                    seed + i * 7919L);
        }
        startRound();
    }

    /** 从存档恢复四族群；缺失的族群用新基因补上。 */
    public Trainer(int population, long seed, float[][][] genes, int[] gens, float[] best) {
        NeuralBrain.setTrainingFast(true);
        this.population = population;
        rnd = new Random(seed);
        for (int i = 0; i < species.length; i++) {
            float[][] g = genes != null && i < genes.length && genes[i] != null
                    ? genes[i] : null;
            if (g == null || g.length == 0) {
                species[i] = new Species(Bias.ALL[i], Bias.ALL[i].name + "族", population,
                        seed + i * 7919L);
            } else {
                species[i] = new Species(Bias.ALL[i], Bias.ALL[i].name + "族",
                        gens != null ? gens[i] : 1, g, best != null ? best[i] : 0f,
                        seed + i * 7919L);
            }
        }
        startRound();
    }

    public int generation() {
        int g = 0;
        for (Species s : species) g = Math.max(g, s.generation);
        return g;
    }

    public int population() {
        return population;
    }

    private void startRound() {
        MapFactory layout = MapFactory.create(20261027L + totalRounds * 977L);
        world = new World(layout);
        fighters.clear();
        lineup = new int[species.length * PER_SPECIES];

        int idx = 0;
        for (int s = 0; s < species.length; s++) {
            for (int k = 0; k < PER_SPECIES; k++) {
                int pick = rnd.nextInt(species[s].size());
                lineup[idx] = s * 10000 + pick;
                Unit u = new Unit();
                u.id = idx;
                u.team = s;                  // 每个族群一个阵营，互相都是敌人
                u.ai = true;
                u.name = Bias.ALL[s].name + pick;
                world.addUnit(u, Loadout.defaults(pickWeapon(s, pick)), idx);
                u.brain = species[s].brain(pick);
                java.util.Arrays.fill(u.visited, false);
                u.visitedCount = 0;
                u.roundTaken = 0f;
                u.roundDist = 0f;
                u.minEnemyDist = 9999f;
                u.aimOnTarget = 0f;
                fighters.add(u);
                idx++;
            }
        }
        world.ended = false;
        roundTime = 0f;
        attachEvents();
        // 击杀提示跨回合保留，换局时只清掉已过期的条目
    }

    /** 训练场上的击杀与命中事件：累加统计，并推入击杀提示。 */
    private void attachEvents() {
        world.events = new World.Events() {
            @Override
            public void onKill(Unit killer, Unit victim, com.neuroshoot.loadout.Weapon weapon) {
                genKills++;
                String k = killer == null ? "环境" : killer.name;
                String text = k + "  击杀  " + victim.name;
                lastKillText = text;
                lastKillAge = 4.5f;
                killFeed.add(0, text);
                killAges.add(0, 4.5f);
                while (killFeed.size() > 5) {
                    killFeed.remove(killFeed.size() - 1);
                    killAges.remove(killAges.size() - 1);
                }
            }

            @Override
            public void onHit(Unit target, int damage) {
                genHits++;
            }

            @Override
            public void onExplosion(float x, float y, float radius) {
            }

            @Override
            public void onDoorBroken(float x, float y) {
            }

            @Override
            public void onCrateBroken(float x, float y) {
            }
        };
    }

    /** 不同倾向配不同枪，让打法差异更明显；个体之间也有轮换。 */
    private String pickWeapon(int speciesIdx, int pick) {
        String[][] pool = {
                {"rifle", "dmr", "lmg"},          // 击杀族：中远距离压制
                {"shotgun", "double_barrel", "rifle"},  // 生存族：近战自保
                {"smg", "pistol", "heavy_pistol"},      // 躲避族：轻便灵活
                {"sniper", "dmr", "rifle"},       // 跑图族：远距离点名
        };
        String[] set = pool[Math.max(0, Math.min(pool.length - 1, speciesIdx))];
        return set[pick % set.length];
    }

    /** 推进一帧，返回是否刚完成一次演化或筛选。 */
    public boolean tick(float dt) {
        if (paused) return false;
        for (Unit u : fighters) {
            if (!u.alive || u.brain == null) continue;
            u.brain.update(u, world, dt);
        }
        world.step(dt);
        roundTime += dt;
        lastKillAge = Math.max(0f, lastKillAge - dt);
        for (int i = killAges.size() - 1; i >= 0; i--) {
            killAges.set(i, killAges.get(i) - dt);
            if (killAges.get(i) <= 0f) {
                killAges.remove(i);
                killFeed.remove(i);
            }
        }
        trackMetrics(dt);

        boolean changed = false;
        if (roundTime > ROUND_TIME || allOneTeamOut()) {
            scoreRound();
            totalRounds++;
            roundsInGen++;
            if (roundsInGen >= ROUNDS_PER_GEN) {
                roundsInGen = 0;
                endGeneration();
                changed = true;
            }
            startRound();
        }
        return changed;
    }

    /** 记录「与敌人最近距离」和「瞄准对准敌人的时长」，供躲避型与击杀型打分。 */
    private void trackMetrics(float dt) {
        for (Unit u : fighters) {
            if (!u.alive) continue;
            float best = 9999f;
            Unit near = null;
            for (Unit o : fighters) {
                if (!o.alive || o.team == u.team) continue;
                float d = (float) Math.hypot(o.x - u.x, o.y - u.y);
                if (d < best) {
                    best = d;
                    near = o;
                }
            }
            if (best < u.minEnemyDist) u.minEnemyDist = best;
            if (near != null) {
                float ang = (float) Math.atan2(near.y - u.y, near.x - u.x);
                float diff = Math.abs(com.neuroshoot.world.Visibility.normalize(ang - u.angle));
                if (diff < 0.22f) u.aimOnTarget += dt;
            }
        }
    }

    private boolean allOneTeamOut() {
        int aliveTeams = 0;
        for (int s = 0; s < species.length; s++) {
            for (Unit u : fighters) {
                if (u.team == s && u.alive) {
                    aliveTeams++;
                    break;
                }
            }
        }
        return aliveTeams <= 1;
    }

    private void scoreRound() {
        for (int i = 0; i < fighters.size(); i++) {
            Unit u = fighters.get(i);
            int who = lineup[i] % 10000;
            int sp = lineup[i] / 10000;
            float f = species[sp].bias.score(
                    u.kills, u.damageDealt, u.roundTaken, u.alive, u.aliveTime,
                    u.visitedCount, u.roundDist, u.minEnemyDist, u.aimOnTarget);
            species[sp].fitness[who] += f;
        }
    }

    private void endGeneration() {
        lastGenKills = genKills;
        lastGenHits = genHits;
        genKills = 0;
        genHits = 0;
        for (Species s : species) s.evolve();
        genSinceScreen++;
        for (int i = 0; i < species.length; i++) {
            curves[i][curveLen % curves[i].length] = species[i].bestFitness;
        }
        curveLen++;
        if (genSinceScreen >= GEN_PER_SCREEN) {
            genSinceScreen = 0;
            screen();
        }
    }

    /**
     * 筛选：把最近表现最差的族群清空重来，并让冠军族群向其余族群注入一半基因。
     *
     * 这样四个族群始终在「淘汰 + 杂交」的压力下互相追赶，不会全部收敛成同一种打法。
     */
    private void screen() {
        screenCount++;
        int worst = 0;
        int champion = 0;
        for (int i = 1; i < species.length; i++) {
            if (species[i].recentMean() < species[worst].recentMean()) worst = i;
            if (species[i].recentMean() > species[champion].recentMean()) champion = i;
        }
        // 只在差距足够明显时才淘汰，避免随机波动误杀
        if (worst != champion
                && species[champion].recentMean() > species[worst].recentMean() * 1.35f + 1f) {
            lastCulled = species[worst].name;
            species[worst].reset(rnd);
        } else {
            lastCulled = "";
        }
        lastChampion = species[champion].name;
        float[][] donor = species[champion].snapshot().toArray(new float[0][]);
        for (int i = 0; i < species.length; i++) {
            if (i == champion) continue;
            species[i].absorb(donor);
        }
    }

    public float[] bestGenesOf(int idx) {
        return species[idx].bestGenes();
    }

    public float[][][] allGenes() {
        float[][][] out = new float[species.length][][];
        for (int i = 0; i < species.length; i++) {
            out[i] = species[i].genes;
        }
        return out;
    }

    public int[] generations() {
        int[] g = new int[species.length];
        for (int i = 0; i < species.length; i++) g[i] = species[i].generation;
        return g;
    }

    public float[] bests() {
        float[] b = new float[species.length];
        for (int i = 0; i < species.length; i++) b[i] = species[i].bestFitness;
        return b;
    }
}
