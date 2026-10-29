package com.neuroshoot.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 一个族群：固定倾向 + 一池个体基因 + 自己的进化器。
 *
 * 四个族群共享同一张地图、同一局对战场，但各按自己的倾向打分，
 * 因此会分化出「爱杀」「爱苟」「爱躲」「爱跑」四种打法。
 */
public final class Species {
    public final Bias bias;
    public final float[][] genes;
    public final float[] fitness;
    public int generation;
    public float bestFitness;
    public float avgFitness;
    public float lastGenAvg;
    /** 最近若干代的平均分，用于判断是否该被淘汰。 */
    public final float[] recentAvg = new float[5];
    public int recentCount;
    public int eliminations;
    public final String name;

    private final Evolver evolver;

    public Species(Bias bias, String name, int population, long seed) {
        this.bias = bias;
        this.name = name;
        genes = new float[population][];
        fitness = new float[population];
        evolver = new Evolver(seed);
        Random rnd = evolver.random();
        for (int i = 0; i < population; i++) genes[i] = Evolver.randomGenes(rnd);
    }

    public Species(Bias bias, String name, int generation, float[][] genes, float best, long seed) {
        this.bias = bias;
        this.name = name;
        this.generation = generation;
        this.bestFitness = best;
        this.genes = new float[genes.length][];
        for (int i = 0; i < genes.length; i++) this.genes[i] = genes[i].clone();
        fitness = new float[this.genes.length];
        evolver = new Evolver(seed);
    }

    public int size() {
        return genes.length;
    }

    public NeuralBrain brain(int i) {
        return new NeuralBrain(Net.fromGenes(genes[i]));
    }

    public float[] bestGenes() {
        int idx = 0;
        for (int i = 1; i < genes.length; i++) if (fitness[i] > fitness[idx]) idx = i;
        return genes[idx];
    }

    public void evolve() {
        float sum = 0f, best = Float.NEGATIVE_INFINITY;
        for (float f : fitness) {
            sum += f;
            if (f > best) best = f;
        }
        avgFitness = sum / fitness.length;
        lastGenAvg = avgFitness;
        bestFitness = Math.max(bestFitness, best);
        recentAvg[recentCount % recentAvg.length] = avgFitness;
        recentCount++;
        evolver.evolve(genes, fitness, 2, 0.3f, 0.7f);
        java.util.Arrays.fill(fitness, 0f);
        generation++;
    }

    /** 用另一个族群的基因替换一半个体，模拟杂交注入。 */
    public void absorb(float[][] donorGenes) {
        int half = genes.length / 2;
        for (int i = 0; i < half && i < donorGenes.length; i++) {
            genes[genes.length - 1 - i] = donorGenes[i].clone();
        }
    }

    /** 长期垫底时清空重来，保留倾向但换一批新基因。 */
    public void reset(Random rnd) {
        for (int i = 0; i < genes.length; i++) genes[i] = Evolver.randomGenes(rnd);
        java.util.Arrays.fill(fitness, 0f);
        generation = 0;
        bestFitness = 0f;
        avgFitness = 0f;
        recentCount = 0;
        eliminations++;
    }

    public float recentMean() {
        int n = Math.min(recentCount, recentAvg.length);
        if (n == 0) return 0f;
        float s = 0f;
        for (int i = 0; i < n; i++) s += recentAvg[i];
        return s / n;
    }

    public List<float[]> snapshot() {
        List<float[]> out = new ArrayList<>();
        for (float[] g : genes) out.add(g.clone());
        return out;
    }
}
