package com.neuroshoot;

import com.neuroshoot.loadout.Attachment;
import com.neuroshoot.loadout.GunBuild;
import com.neuroshoot.loadout.Loadout;
import com.neuroshoot.world.C;
import com.neuroshoot.world.MapFactory;
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.World;

/** 验证昼夜视野、夜视仪、护甲减伤与瞄具影响。 */
public final class DayNightCheck {
    public static void main(String[] args) {
        Unit u = new Unit();
        u.spawn(Loadout.defaults("m4a1"), 100f, 100f);

        u.nightMode = false; u.nvg = false; u.crouch = false; u.aim = false;
        float day = u.viewRadius();
        float dayFov = u.viewFov();
        u.nightMode = true;
        float night = u.viewRadius();
        u.nvg = true;
        float nightNvg = u.viewRadius();

        System.out.printf("白天视野 %.0f px，视角 %.0f°%n", day, Math.toDegrees(dayFov * 2));
        System.out.printf("夜晚视野 %.0f px，夜视仪后 %.0f px%n", night, nightNvg);
        if (day < 2400) throw new AssertionError("白天视野不足 80 米");
        if (night > 600) throw new AssertionError("夜晚视野没有变小");
        if (nightNvg <= night) throw new AssertionError("夜视仪没提升视野");

        // 瞄具影响腰射精度
        GunBuild b = new GunBuild("m4a1");
        float hip0 = b.spreadHip();
        b.attach(Attachment.Slot.OPTIC, "red_dot");
        float hip1 = b.spreadHip();
        System.out.printf("红点镜: 腰射散布 %.4f → %.4f%n", hip0, hip1);
        if (hip1 >= hip0) throw new AssertionError("红点没提升腰射精度");

        // 护甲减伤
        Unit a1 = new Unit();
        a1.spawn(Loadout.defaults("m4a1"), 100f, 100f);
        a1.armorLevel = 0; a1.armor = 60;
        a1.hurt(50, false, true);
        int d0 = 100 - a1.hp;
        Unit a3 = new Unit();
        a3.spawn(Loadout.defaults("m4a1"), 100f, 100f);
        a3.armorLevel = 3; a3.armor = 60;
        a3.hurt(50, false, true);
        int d3 = 100 - a3.hp;
        System.out.printf("同样 50 伤害: 无甲掉 %d，三级甲掉 %d%n", d0, d3);
        if (d3 >= d0) throw new AssertionError("三级甲没有减伤");
        System.out.println("昼夜/夜视/瞄具/护甲：通过");
    }
}
