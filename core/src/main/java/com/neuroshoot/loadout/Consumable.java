package com.neuroshoot.loadout;

/** 投掷物与药品定义。 */
public final class Consumable {
    public enum Kind { THROW, MED }

    public final String id;
    public final String name;
    public final Kind kind;
    public final int cost;
    /** 生效延迟（秒），用于手雷引信等。 */
    public final float fuse;

    public Consumable(String id, String name, Kind kind, int cost, float fuse) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.cost = cost;
        this.fuse = fuse;
    }

    public static final Consumable[] THROWS = {
            new Consumable("grenade", "手雷", Kind.THROW, 300, 2.2f),
            new Consumable("flash", "闪光弹", Kind.THROW, 180, 1.4f),
            new Consumable("smoke", "烟雾弹", Kind.THROW, 150, 1.0f),
            new Consumable("molotov", "燃烧瓶", Kind.THROW, 260, 1.8f),
            new Consumable("c4", "遥控炸药", Kind.THROW, 420, 0f),
    };

    public static final Consumable[] MEDS = {
            new Consumable("bandage", "绷带", Kind.MED, 90, 0f),
            new Consumable("syringe", "快速治疗针", Kind.MED, 220, 0f),
            new Consumable("surgery", "手术包", Kind.MED, 320, 0f),
            new Consumable("painkiller", "止痛药", Kind.MED, 150, 0f),
    };

    public static Consumable byId(String id) {
        for (Consumable c : THROWS) if (c.id.equals(id)) return c;
        for (Consumable c : MEDS) if (c.id.equals(id)) return c;
        return null;
    }
}
