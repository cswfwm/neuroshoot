package com.neuroshoot.loadout;

import java.util.ArrayList;
import java.util.List;

/**
 * 一套配装：主武器 + 副武器 + 投掷物 + 药品，总价不超过积分。
 *
 * 双持是三角洲的核心设定：主武器负责主战，副武器（通常是手枪）
 * 在主武器换弹或弹尽时顶上，切换键可即时换手。
 */
public final class Loadout {
    /** 主武器。 */
    public GunBuild primary = new GunBuild("m4a1");
    /** 副武器，通常为手枪。 */
    public GunBuild secondary = new GunBuild("g17");

    public final List<String> throws_ = new ArrayList<>();
    public final List<String> meds = new ArrayList<>();
    public int points = com.neuroshoot.world.C.POINTS;

    /** 兼容旧代码：主武器 id。 */
    public String getWeaponId() {
        return primary.weaponId;
    }

    public int spent() {
        int sum = primary.cost() + secondary.cost();
        for (String id : throws_) {
            Consumable c = Consumable.byId(id);
            if (c != null) sum += c.cost;
        }
        for (String id : meds) {
            Consumable c = Consumable.byId(id);
            if (c != null) sum += c.cost;
        }
        return sum;
    }

    public int remain() {
        return points - spent();
    }

    public boolean canAdd(Consumable c) {
        if (c == null || remain() < c.cost) return false;
        if (c.kind == Consumable.Kind.THROW) {
            return throws_.size() < com.neuroshoot.world.C.THROW_MAX;
        }
        return meds.size() < com.neuroshoot.world.C.MED_MAX;
    }

    public boolean add(Consumable c) {
        if (!canAdd(c)) return false;
        if (c.kind == Consumable.Kind.THROW) throws_.add(c.id);
        else meds.add(c.id);
        return true;
    }

    public boolean removeOne(String id) {
        if (throws_.remove(id)) return true;
        return meds.remove(id);
    }

    /** 选主武器：会替换整个改枪配置。 */
    public boolean selectPrimary(String weaponId) {
        return select(weaponId, true);
    }

    public boolean selectSecondary(String weaponId) {
        return select(weaponId, false);
    }

    private boolean select(String weaponId, boolean isPrimary) {
        GunBuild cur = isPrimary ? primary : secondary;
        GunBuild next = GunBuild.preset(weaponId);
        int old = cur.cost();
        if (spent() - old + next.cost() > points) return false;
        if (isPrimary) primary = next;
        else secondary = next;
        return true;
    }

    public int countOf(String id) {
        int n = 0;
        for (String s : throws_) if (s.equals(id)) n++;
        for (String s : meds) if (s.equals(id)) n++;
        return n;
    }

    public Loadout copy() {
        Loadout l = new Loadout();
        l.primary = primary.copy();
        l.secondary = secondary.copy();
        l.throws_.addAll(throws_);
        l.meds.addAll(meds);
        l.points = points;
        return l;
    }

    /** AI 与快速开始用的默认配装。 */
    public static Loadout defaults(String weaponId) {
        Loadout l = new Loadout();
        l.primary = GunBuild.preset(weaponId);
        l.secondary = GunBuild.preset("g17");
        l.add(Consumable.byId("bandage"));
        l.add(Consumable.byId("bandage"));
        l.add(Consumable.byId("syringe"));
        l.add(Consumable.byId("painkiller"));
        l.add(Consumable.byId("grenade"));
        l.add(Consumable.byId("smoke"));
        return l;
    }
}
