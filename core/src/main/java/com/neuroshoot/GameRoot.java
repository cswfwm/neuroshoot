package com.neuroshoot;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.neuroshoot.store.ModelStore;
import com.neuroshoot.store.PrefsBackend;

/** 游戏入口。所有界面共用 1280x720 逻辑坐标。 */
public class GameRoot extends Game {
    public ModelStore store;
    public Session session;

    @Override
    public void create() {
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        store = new ModelStore(new PrefsBackend());
        session = new Session();
        session.store = store;
        setScreen(new MenuScreen(this));
    }
}
