package com.neuroshoot.ai;

import java.util.Random;

/**
 * 全连接多层感知机，权重扁平存放，便于遗传算法直接交叉与变异。
 * 激活统一 tanh，输出落在 [-1,1]。
 */
public final class Net {
    private final int[] sizes;
    private final float[] weights;
    private final float[] biases;
    private final int weightCount;
    private final int biasCount;
    private final float[][] acts;

    public Net(int... sizes) {
        if (sizes.length < 2) throw new IllegalArgumentException("至少需要输入层和输出层");
        this.sizes = sizes.clone();
        int w = 0, b = 0;
        for (int i = 1; i < sizes.length; i++) {
            w += sizes[i] * sizes[i - 1];
            b += sizes[i];
        }
        weightCount = w;
        biasCount = b;
        weights = new float[w];
        biases = new float[b];
        acts = new float[sizes.length][];
        for (int i = 0; i < sizes.length; i++) acts[i] = new float[sizes[i]];
    }

    public static Net ofShape() {
        return new Net(com.neuroshoot.world.C.LAYERS);
    }

    public static int geneCount() {
        return ofShape().weightCount + ofShape().biasCount;
    }

    public int weightCount() {
        return weightCount;
    }

    public int biasCount() {
        return biasCount;
    }

    public float[] weights() {
        return weights;
    }

    public float[] biases() {
        return biases;
    }

    public int inputCount() {
        return sizes[0];
    }

    public int layerCount() {
        return sizes.length;
    }

    public int sizeOf(int l) {
        return sizes[l];
    }

    public void randomize(Random rnd) {
        int idx = 0;
        for (int l = 1; l < sizes.length; l++) {
            int in = sizes[l - 1];
            float scale = (float) Math.sqrt(2.0 / in);
            for (int n = 0; n < sizes[l]; n++) {
                for (int i = 0; i < in; i++) weights[idx++] = (float) rnd.nextGaussian() * scale;
            }
        }
        for (int i = 0; i < biasCount; i++) biases[i] = 0f;
    }

    public float[] forward(float[] input) {
        float[] cur = acts[0];
        System.arraycopy(input, 0, cur, 0, Math.min(input.length, cur.length));
        int widx = 0, bidx = 0;
        for (int l = 1; l < sizes.length; l++) {
            float[] prev = acts[l - 1];
            float[] out = acts[l];
            int in = sizes[l - 1];
            for (int n = 0; n < sizes[l]; n++) {
                float sum = biases[bidx + n];
                int base = widx + n * in;
                for (int i = 0; i < in; i++) sum += weights[base + i] * prev[i];
                out[n] = (float) Math.tanh(sum);
            }
            widx += sizes[l] * in;
            bidx += sizes[l];
        }
        return acts[sizes.length - 1];
    }

    public float[] activationsOf(int layer) {
        return acts[layer];
    }

    public float[] genes() {
        float[] g = new float[weightCount + biasCount];
        System.arraycopy(weights, 0, g, 0, weightCount);
        System.arraycopy(biases, 0, g, weightCount, biasCount);
        return g;
    }

    public static Net fromGenes(float[] g) {
        Net n = ofShape();
        if (g.length != n.weightCount + n.biasCount) return null;
        System.arraycopy(g, 0, n.weights, 0, n.weightCount);
        System.arraycopy(g, n.weightCount, n.biases, 0, n.biasCount);
        return n;
    }
}
