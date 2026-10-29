package com.neuroshoot.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 内存后端，供无头自检使用。 */
public final class MemoryBackend implements Backend {
    private final Map<String, String> map = new HashMap<>();

    @Override
    public String get(String key) {
        return map.get(key);
    }

    @Override
    public void put(String key, String value) {
        map.put(key, value);
    }

    @Override
    public void remove(String key) {
        map.remove(key);
    }

    @Override
    public List<String> keys() {
        return new ArrayList<>(map.keySet());
    }
}
