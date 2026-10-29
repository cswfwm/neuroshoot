package com.neuroshoot.store;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import java.util.ArrayList;
import java.util.List;

/** Preferences 后端：安卓上每个键存进 XML，重启后仍在。 */
public final class PrefsBackend implements Backend {
    private final Preferences prefs;

    public PrefsBackend() {
        prefs = Gdx.app.getPreferences("neuroshoot");
    }

    @Override
    public String get(String key) {
        return prefs.getString(key, null);
    }

    @Override
    public void put(String key, String value) {
        prefs.putString(key, value);
        prefs.flush();
    }

    @Override
    public void remove(String key) {
        prefs.remove(key);
        prefs.flush();
    }

    @Override
    public List<String> keys() {
        return new ArrayList<>(prefs.get().keySet());
    }
}
