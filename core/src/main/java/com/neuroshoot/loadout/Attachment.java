package com.neuroshoot.loadout;

import java.util.ArrayList;
import java.util.List;

/**
 * 改枪系统：五类配件槽，每把枪可装一件同类配件。
 *
 * 配件不改变枪的类别，只在原数值上做乘算或加算，因此可以自由组合出
 * 「高精度低机动」或「高机动低精度」等流派，符合三角洲行动的改装思路。
 */
public final class Attachment {
    /** 槽位类型。 */
    public enum Slot {
        OPTIC("瞄具"),
        MUZZLE("枪口"),
        GRIP("握把"),
        MAG("弹匣"),
        STOCK("枪托");

        public final String label;

        Slot(String label) {
            this.label = label;
        }
    }

    public final String id;
    public final String name;
    public final Slot slot;
    public final int cost;
    /** 各项修正系数：1.0 表示不变。 */
    public final float spreadMul;
    public final float recoilMul;
    public final float rangeMul;
    public final float magMul;
    public final float moveMul;
    public final float reloadMul;

    public Attachment(String id, String name, Slot slot, int cost,
                      float spreadMul, float recoilMul, float rangeMul,
                      float magMul, float moveMul, float reloadMul) {
        this.id = id;
        this.name = name;
        this.slot = slot;
        this.cost = cost;
        this.spreadMul = spreadMul;
        this.recoilMul = recoilMul;
        this.rangeMul = rangeMul;
        this.magMul = magMul;
        this.moveMul = moveMul;
        this.reloadMul = reloadMul;
    }

    public static final Attachment[] ALL = {
            // 瞄具：提升开镜精度，略降移速
            new Attachment("red_dot", "红点瞄具", Slot.OPTIC, 120, 0.92f, 1f, 1f, 1f, 0.99f, 1f),
            new Attachment("holo", "全息瞄具", Slot.OPTIC, 160, 0.88f, 1f, 1.05f, 1f, 0.98f, 1f),
            new Attachment("scope4x", "4 倍镜", Slot.OPTIC, 260, 0.72f, 1f, 1.18f, 1f, 0.95f, 1f),
            new Attachment("scope8x", "8 倍镜", Slot.OPTIC, 380, 0.6f, 1f, 1.3f, 1f, 0.9f, 1f),

            // 枪口：抑制后坐或增强射程
            new Attachment("compensator", "补偿器", Slot.MUZZLE, 180, 1f, 0.78f, 1f, 1f, 1f, 1f),
            new Attachment("suppressor", "消音器", Slot.MUZZLE, 220, 0.95f, 0.88f, 0.95f, 1f, 1f, 1f),
            new Attachment("long_barrel", "加长枪管", Slot.MUZZLE, 240, 0.94f, 1.05f, 1.22f, 1f, 0.98f, 1f),

            // 握把：压后坐，牺牲一点移速
            new Attachment("vertical_grip", "垂直握把", Slot.GRIP, 140, 1f, 0.82f, 1f, 1f, 0.99f, 1f),
            new Attachment("angled_grip", "斜角握把", Slot.GRIP, 140, 0.9f, 0.95f, 1f, 1f, 0.99f, 1f),

            // 弹匣：扩容换换弹时间
            new Attachment("extended_mag", "扩容弹匣", Slot.MAG, 200, 1f, 1f, 1f, 1.35f, 0.99f, 1.15f),
            new Attachment("quick_mag", "快拔弹匣", Slot.MAG, 180, 1f, 1f, 1f, 1f, 1f, 0.7f),
            new Attachment("drum_mag", "弹鼓", Slot.MAG, 320, 1f, 1f, 1f, 1.8f, 0.96f, 1.35f),

            // 枪托：稳定与机动的取舍
            new Attachment("heavy_stock", "重型枪托", Slot.STOCK, 150, 0.88f, 0.85f, 1f, 1f, 0.97f, 1f),
            new Attachment("light_stock", "轻量枪托", Slot.STOCK, 130, 1.05f, 1.08f, 1f, 1f, 1.05f, 1f),
    };

    public static Attachment byId(String id) {
        for (Attachment a : ALL) if (a.id.equals(id)) return a;
        return null;
    }

    public static Attachment[] ofSlot(Slot s) {
        List<Attachment> out = new ArrayList<>();
        for (Attachment a : ALL) if (a.slot == s) out.add(a);
        return out.toArray(new Attachment[0]);
    }

    public static int costOf(List<String> ids) {
        int sum = 0;
        for (String id : ids) {
            Attachment a = byId(id);
            if (a != null) sum += a.cost;
        }
        return sum;
    }
}
