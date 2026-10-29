package com.neuroshoot.loadout;

import java.util.ArrayList;
import java.util.List;

/**
 * 改枪配置：一把枪 + 各槽位的配件。
 *
 * 数值用「基础值 + 配件乘算」计算，界面上以六项面板呈现，
 * 与三角洲改枪台一致：伤害、射程、后坐力控制、操控速度、据枪稳定性、腰射精度。
 */
public final class GunBuild {
    public final String weaponId;
    /** 槽位 -> 配件 id。 */
    public final java.util.Map<Attachment.Slot, String> slots =
            new java.util.LinkedHashMap<>();

    public GunBuild(String weaponId) {
        this.weaponId = weaponId;
    }

    public Weapon weapon() {
        return Weapon.byId(weaponId);
    }

    public int cost() {
        int sum = weapon().cost;
        for (String id : slots.values()) {
            Attachment a = Attachment.byId(id);
            if (a != null) sum += a.cost;
        }
        return sum;
    }

    /** 装机件；传 null 表示卸下。 */
    public void attach(Attachment.Slot slot, String attachmentId) {
        if (attachmentId == null) slots.remove(slot);
        else slots.put(slot, attachmentId);
    }

    public String attached(Attachment.Slot slot) {
        return slots.get(slot);
    }

    private float mul(java.util.function.Function<Attachment, Float> f) {
        float m = 1f;
        for (String id : slots.values()) {
            Attachment a = Attachment.byId(id);
            if (a != null) m *= f.apply(a);
        }
        return m;
    }

    // ── 六项面板数值（供界面显示与逻辑使用）──

    public int damage() {
        return weapon().damage;
    }

    public float range() {
        return weapon().range * mul(a -> a.rangeMul);
    }

    public float spreadHip() {
        return weapon().spreadHip * mul(a -> a.spreadMul);
    }

    public float spreadAim() {
        return weapon().spreadAim * mul(a -> a.spreadMul);
    }

    public float recoil() {
        return weapon().recoil * mul(a -> a.recoilMul);
    }

    public int magazine() {
        return Math.max(1, Math.round(weapon().magazine * mul(a -> a.magMul)));
    }

    public float moveMul() {
        return weapon().moveMul * mul(a -> a.moveMul);
    }

    public float reloadTime() {
        return weapon().reloadTime * mul(a -> a.reloadMul);
    }

    public float fireInterval() {
        return weapon().fireInterval;
    }

    public int pellets() {
        return weapon().pellets();
    }

    public boolean breaksDoor() {
        return weapon().breaksDoor;
    }

    /** 后坐力控制评分（0~100，越大越稳）。 */
    public int recoilControl() {
        float base = 60f;
        float r = weapon().recoil;
        float score = base - r * 8f + (1f - recoil() / Math.max(0.01f, r)) * 40f;
        return clamp((int) score);
    }

    /** 操控速度评分（0~100，越大越灵活）。 */
    public int handling() {
        float score = 50f + (weapon().moveMul - 1f) * 200f
                + (1.1f - weapon().fireInterval) * 60f
                + (1f - moveMul() / Math.max(0.01f, weapon().moveMul)) * 30f;
        return clamp((int) score);
    }

    /** 据枪稳定性（0~100）。 */
    public int stability() {
        float quiet = 1f - spreadAim() / Math.max(0.001f, weapon().spreadAim) * 0.5f;
        float score = 45f + quiet * 45f + (1f - recoil() / Math.max(0.01f, weapon().recoil)) * 25f;
        return clamp((int) score);
    }

    /** 腰射精度（0~100）。 */
    public int hipAccuracy() {
        float score = 100f - spreadHip() / Math.max(0.001f, weapon().spreadHip) * 30f
                - weapon().spreadHip * 260f;
        return clamp((int) score);
    }

    private static int clamp(int v) {
        return Math.max(1, Math.min(100, v));
    }

    public GunBuild copy() {
        GunBuild b = new GunBuild(weaponId);
        b.slots.putAll(slots);
        return b;
    }

    /** 推荐配装：给 AI 用，按类别选一套均衡配件。 */
    public static GunBuild preset(String weaponId) {
        GunBuild b = new GunBuild(weaponId);
        Weapon w = b.weapon();
        switch (w.category) {
            case SNIPER:
                b.attach(Attachment.Slot.OPTIC, "scope8x");
                b.attach(Attachment.Slot.MUZZLE, "long_barrel");
                b.attach(Attachment.Slot.STOCK, "heavy_stock");
                break;
            case SHOTGUN:
                b.attach(Attachment.Slot.MUZZLE, "compensator");
                b.attach(Attachment.Slot.GRIP, "vertical_grip");
                b.attach(Attachment.Slot.MAG, "extended_mag");
                break;
            case SMG:
                b.attach(Attachment.Slot.OPTIC, "red_dot");
                b.attach(Attachment.Slot.MUZZLE, "suppressor");
                b.attach(Attachment.Slot.STOCK, "light_stock");
                break;
            case LMG:
                b.attach(Attachment.Slot.MUZZLE, "compensator");
                b.attach(Attachment.Slot.GRIP, "vertical_grip");
                b.attach(Attachment.Slot.MAG, "drum_mag");
                break;
            default:
                b.attach(Attachment.Slot.OPTIC, "holo");
                b.attach(Attachment.Slot.GRIP, "angled_grip");
                b.attach(Attachment.Slot.MAG, "extended_mag");
                break;
        }
        return b;
    }

    public List<String> attachmentIds() {
        return new ArrayList<>(slots.values());
    }
}
