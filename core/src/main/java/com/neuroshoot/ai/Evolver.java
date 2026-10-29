package com.neuroshoot.ai;

import java.util.Arrays;
import java.util.Random;

/**
 * 进化算法：锦标赛选择 + 均匀交叉 + 高斯变异 + 精英保留。
 *
 * 基因是网络的扁平权重与偏置。族群用同一个进化器维持基因库，
 * 因此存档后再次载入可以继续按原样繁衍。
 */
public final class Evolver {
    private final Random rnd;

    public Evolver(long seed) {
        rnd = new Random(seed);
    }

    public Random random() {
        return rnd;
    }

    public void evolve(float[][] genes, float[] fitness, int elitism,
                       float mutation, float crossRate) {
        int n = genes.length;
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Float.compare(fitness[b], fitness[a]));

        float[][] next = new float[n][];
        for (int k = 0; k < elitism && k < n; k++) next[k] = genes[order[k]].clone();
        for (int k = elitism; k < n; k++) {
            float[] p1 = genes[tournament(fitness)];
            float[] p2 = genes[tournament(fitness)];
            float[] child = rnd.nextFloat() < crossRate ? crossover(p1, p2) : p1.clone();
            mutate(child, mutation);
            next[k] = child;
        }
        System.arraycopy(next, 0, genes, 0, n);
    }

    private int tournament(float[] fitness) {
        int best = rnd.nextInt(fitness.length);
        for (int i = 0; i < 3; i++) {
            int c = rnd.nextInt(fitness.length);
            if (fitness[c] > fitness[best]) best = c;
        }
        return best;
    }

    private float[] crossover(float[] a, float[] b) {
        float[] out = new float[a.length];
        for (int i = 0; i < a.length; i++) out[i] = rnd.nextBoolean() ? a[i] : b[i];
        return out;
    }

    private void mutate(float[] g, float sigma) {
        for (int i = 0; i < g.length; i++) {
            if (rnd.nextFloat() < 0.12f) {
                g[i] += (float) rnd.nextGaussian() * sigma;
                if (rnd.nextFloat() < 0.01f) g[i] += (float) rnd.nextGaussian() * 1.2f;
                if (g[i] > 6f) g[i] = 6f;
                else if (g[i] < -6f) g[i] = -6f;
            }
        }
    }

    public static float[] randomGenes(Random rnd) {
        Net net = Net.ofShape();
        net.randomize(rnd);
        return net.genes();
    }

    /** 以一方的基因为核心扩出变异体，按成绩挑出下一代。 */
    public float[] breed(float[] base, float sigma, float gain) {
        float[] best = base.clone();
        float bestScore = gain;
        for (int i = 0; i < 6; i++) {
            float[] g = base.clone();
            mutate(g, sigma);
            float score = gain - rnd.nextFloat() * 0.5f;
            if (score > bestScore) {
                bestScore = score;
                best = g;
            }
        }
        return best;
    }
}
