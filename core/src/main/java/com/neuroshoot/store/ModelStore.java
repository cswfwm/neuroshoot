package com.neuroshoot.store;

import com.neuroshoot.store.ModelCodec.SpeciesData;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型库：每个模型单独占一个键，避免整表拼串触发 Preferences 单值体积限制。
 * 键名 {@code m.<序号>}，索引键记录已占用的序号，族群存档单独占一个键。
 */
public final class ModelStore {
    private static final String KEY_INDEX = "index";
    private static final String KEY_SPECIES = "species";
    private static final String KEY_PREFIX = "m.";

    public static final int MAX_MODELS = 32;

    private final Backend backend;

    public ModelStore(Backend backend) {
        this.backend = backend;
    }

    public List<ModelRecord> loadAll() {
        List<ModelRecord> out = new ArrayList<>();
        for (String id : ids()) {
            ModelRecord r = ModelCodec.decode(backend.get(KEY_PREFIX + id));
            if (r != null) out.add(r);
        }
        out.sort((a, b) -> Float.compare(b.fitness, a.fitness));
        return out;
    }

    public boolean save(ModelRecord record) {
        if (loadAll().size() >= MAX_MODELS) return false;
        int id = nextId();
        backend.put(KEY_PREFIX + id, ModelCodec.encode(record));
        String idx = backend.get(KEY_INDEX);
        backend.put(KEY_INDEX, idx == null || idx.isEmpty() ? String.valueOf(id) : idx + "," + id);
        return true;
    }

    public boolean saveOrReplace(ModelRecord record) {
        for (String id : ids()) {
            ModelRecord r = ModelCodec.decode(backend.get(KEY_PREFIX + id));
            if (r != null && r.name.equals(record.name)) {
                backend.put(KEY_PREFIX + id, ModelCodec.encode(record));
                return true;
            }
        }
        return save(record);
    }

    public void remove(String name) {
        for (String id : ids()) {
            ModelRecord r = ModelCodec.decode(backend.get(KEY_PREFIX + id));
            if (r == null || r.name.equals(name)) {
                backend.remove(KEY_PREFIX + id);
                String idx = backend.get(KEY_INDEX);
                if (idx != null) backend.put(KEY_INDEX, removeToken(idx, id));
            }
        }
    }

    public void saveSpecies(String name, int generation, float best, ModelRecord[] pool) {
        backend.put(KEY_SPECIES, ModelCodec.encodeSpecies(name, generation, best, pool));
    }

    public SpeciesData loadSpecies() {
        return ModelCodec.decodeSpecies(backend.get(KEY_SPECIES));
    }

    public boolean hasSpecies() {
        String raw = backend.get(KEY_SPECIES);
        return raw != null && !raw.isEmpty();
    }

    private List<String> ids() {
        List<String> out = new ArrayList<>();
        String idx = backend.get(KEY_INDEX);
        if (idx == null || idx.isEmpty()) return out;
        for (String s : idx.split(",")) if (!s.isEmpty()) out.add(s);
        return out;
    }

    private int nextId() {
        int max = 0;
        for (String s : ids()) {
            try {
                max = Math.max(max, Integer.parseInt(s));
            } catch (NumberFormatException ignored) {
                // 索引项损坏时跳过
            }
        }
        return max + 1;
    }

    private static String removeToken(String list, String token) {
        StringBuilder sb = new StringBuilder();
        for (String s : list.split(",")) {
            if (s.isEmpty() || s.equals(token)) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(s);
        }
        return sb.toString();
    }
}
