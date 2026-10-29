package com.neuroshoot;

import com.neuroshoot.loadout.Loadout;
import com.neuroshoot.world.MapFactory;
import com.neuroshoot.world.World;

/** 跨界面共享的对局设置：配装、队伍规模、地图种子。 */
public final class Session {
    public Loadout playerLoadout = Loadout.defaults("rifle");
    /** 队友数量 0 或 3；敌方固定 4。 */
    public int allies = 3;
    public boolean alliesAreAi = true;
    public long seed = 20261027L;
    /** 选中的地图：城镇或工厂。 */
    public MapFactory.Kind mapKind = MapFactory.Kind.TOWN;
    public MapFactory layout;
    public World world;

    private com.neuroshoot.ai.Trainer trainer;

    /** 四族群训练器跨界面保留，返回主菜单再进来不会丢进度。 */
    public com.neuroshoot.ai.Trainer trainer() {
        if (trainer == null) {
            trainer = new com.neuroshoot.ai.Trainer(16, System.nanoTime());
        }
        return trainer;
    }

    public void resetTrainer() {
        trainer = null;
    }

    public com.neuroshoot.store.ModelStore store;
    /** 玩家自定义操作设置。 */
    public final Controls controls = new Controls();
    /** 是否携带夜视仪与护甲等级（配装界面设定）。 */
    public boolean nvgEquipped = true;
    public int armorLevel = 1;

    public void newWorld() {
        layout = MapFactory.create(seed, mapKind);
        world = new World(layout);
    }
}
