package com.neuroshoot.store;

import java.util.ArrayList;
import java.util.List;

/**
 * 存档编解码：单模型一行，族群存档一整池。
 * 解析失败一律返回 null，由调用方跳过，避免一条坏记录拖垮模型库。
 */
public final class ModelCodec {
    private static final char FIELD = '\u0002';
    private static final char ITEM = '\u0003';
    private static final String PIPE_ESC = "\u0004";
    private static final String VERSION = "v1";
    private static final String SPECIES_VERSION = "sp1";

    private ModelCodec() {}

    public static String encode(ModelRecord r) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append(VERSION).append('|')
                .append(esc(r.name)).append('|')
                .append(esc(r.species)).append('|')
                .append(r.generation).append('|')
                .append(r.fitness).append('|')
                .append(r.savedAt).append('|')
                .append(esc(r.lineage)).append('|');
        appendGenes(sb, r.genes);
        return sb.toString();
    }

    public static ModelRecord decode(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String[] p = raw.split("\\|", -1);
        if (p.length != 8 || !VERSION.equals(p[0])) return null;
        try {
            ModelRecord r = new ModelRecord();
            r.name = unesc(p[1]);
            r.species = unesc(p[2]);
            r.generation = Integer.parseInt(p[3]);
            r.fitness = Float.parseFloat(p[4]);
            r.savedAt = Long.parseLong(p[5]);
            r.lineage = unesc(p[6]);
            r.genes = parseGenes(p[7]);
            return r.genes == null ? null : r;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String encodeSpecies(String name, int generation, float best, ModelRecord[] pool) {
        StringBuilder sb = new StringBuilder(pool.length * 4200);
        sb.append(SPECIES_VERSION).append('|').append(esc(name)).append('|')
                .append(generation).append('|').append(best).append('|')
                .append(pool.length).append('|');
        for (ModelRecord r : pool) {
            sb.append(esc(r.name)).append(FIELD).append(r.fitness).append(FIELD);
            appendGenes(sb, r.genes);
            sb.append(ITEM);
        }
        return sb.toString();
    }

    public static SpeciesData decodeSpecies(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String[] head = raw.split("\\|", 6);
        if (head.length != 6 || !SPECIES_VERSION.equals(head[0])) return null;
        try {
            String name = unesc(head[1]);
            int gen = Integer.parseInt(head[2]);
            float best = Float.parseFloat(head[3]);
            int count = Integer.parseInt(head[4]);
            String body = head[5];
            List<ModelRecord> list = new ArrayList<>(count);
            int idx = 0;
            for (int i = 0; i < count; i++) {
                int end = body.indexOf(ITEM, idx);
                if (end < 0) return null;
                String[] pair = body.substring(idx, end).split(String.valueOf(FIELD), -1);
                idx = end + 1;
                if (pair.length != 3) return null;
                ModelRecord r = new ModelRecord();
                r.name = unesc(pair[0]);
                r.species = name;
                r.generation = gen;
                r.fitness = Float.parseFloat(pair[1]);
                r.genes = parseGenes(pair[2]);
                if (r.genes == null) return null;
                list.add(r);
            }
            return new SpeciesData(name, gen, best, list.toArray(new ModelRecord[0]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static float[] parseGenes(String s) {
        if (s.isEmpty()) return new float[0];
        String[] p = s.split(",");
        float[] g = new float[p.length];
        try {
            for (int i = 0; i < p.length; i++) g[i] = Float.parseFloat(p[i]);
        } catch (NumberFormatException e) {
            return null;
        }
        return g;
    }

    private static void appendGenes(StringBuilder sb, float[] g) {
        for (int i = 0; i < g.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(g[i]);
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace(FIELD, ' ').replace(ITEM, ' ').replace("|", PIPE_ESC);
    }

    private static String unesc(String s) {
        return s.replace(PIPE_ESC, "|");
    }

    public static final class SpeciesData {
        public final String name;
        public final int generation;
        public final float bestFitness;
        public final ModelRecord[] members;

        SpeciesData(String name, int generation, float bestFitness, ModelRecord[] members) {
            this.name = name;
            this.generation = generation;
            this.bestFitness = bestFitness;
            this.members = members;
        }
    }
}
