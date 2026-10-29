package com.neuroshoot.store;

import com.neuroshoot.ai.Net;
import com.neuroshoot.world.C;

/** 一个已存档的个体：网络基因 + 族群身份 + 族谱。 */
public final class ModelRecord {
    public String name;
    public String species;
    public int generation;
    public float fitness;
    public String lineage;
    public long savedAt;
    public float[] genes;

    public static ModelRecord of(String name, String species, int generation, float fitness,
                                 String lineage, float[] genes) {
        ModelRecord r = new ModelRecord();
        r.name = name;
        r.species = species;
        r.generation = generation;
        r.fitness = fitness;
        r.lineage = lineage;
        r.savedAt = System.currentTimeMillis();
        r.genes = genes.clone();
        return r;
    }

    public Net toNet() {
        if (genes == null || genes.length != expectedGeneCount()) return null;
        return Net.fromGenes(genes);
    }

    public static int expectedGeneCount() {
        return Net.geneCount();
    }

    public String summary() {
        return name + "  " + species + " 第" + generation + "代  评分" + Math.round(fitness);
    }

    public static int layers() {
        return C.LAYERS.length;
    }
}
