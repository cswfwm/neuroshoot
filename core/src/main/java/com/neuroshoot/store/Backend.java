package com.neuroshoot.store;

import java.util.List;

/** 存档后端：手机用 Preferences，无头测试用内存实现。 */
public interface Backend {
    String get(String key);

    void put(String key, String value);

    void remove(String key);

    List<String> keys();
}
