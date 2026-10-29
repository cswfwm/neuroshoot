package com.neuroshoot;

import com.neuroshoot.loadout.Attachment;
import com.neuroshoot.loadout.GunBuild;
import com.neuroshoot.loadout.Loadout;
import com.neuroshoot.loadout.Weapon;
import com.neuroshoot.world.C;
import com.neuroshoot.world.MapFactory;
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.World;

/** 验证 57 把枪、分类、改枪、双持与切枪。 */
public final class ArsenalCheck {
    public static void main(String[] args) {
        int total = Weapon.ALL.length;
        System.out.println("枪械总数: " + total);
        for (Weapon.Category c : Weapon.Category.values()) {
            System.out.printf("  %s: %d 把%n", c.label, Weapon.countOf(c));
        }
        if (total < 50) throw new AssertionError("枪械数量不足: " + total);

        // 改枪：装 4 倍镜后射程与精度应变化
        GunBuild b = new GunBuild("m4a1");
        int dmg0 = b.damage();
        float range0 = b.range();
        float hip0 = b.spreadHip();
        int mag0 = b.magazine();
        b.attach(Attachment.Slot.OPTIC, "scope4x");
        b.attach(Attachment.Slot.MAG, "extended_mag");
        if (b.range() <= range0) throw new AssertionError("4倍镜没提升射程");
        if (b.spreadHip() >= hip0) throw new AssertionError("瞄具没改善散布");
        if (b.magazine() <= mag0) throw new AssertionError("扩容弹匣没增加弹容");
        System.out.printf("改枪: 射程 %.0f→%.0f, 弹匣 %d→%d, 花费 %d 分%n",
                range0, b.range(), mag0, b.magazine(), b.cost());

        // 六项面板合理
        System.out.printf("面板: 伤害%d 射程%d 后坐%d 操控%d 稳定%d 腰射%d%n",
                b.damage(), (int) b.range(), b.recoilControl(), b.handling(),
                b.stability(), b.hipAccuracy());
        if (b.recoilControl() < 1 || b.recoilControl() > 100) throw new AssertionError("后坐评分越界");
        if (b.handling() < 1 || b.handling() > 100) throw new AssertionError("操控评分越界");

        // 双持与切枪
        MapFactory f = MapFactory.create(7L);
        World w = new World(f);
        Unit u = new Unit();
        u.id = 0; u.team = 0;
        w.addUnit(u, Loadout.defaults("akm"), 0);
        if (u.weapon().id.equals("g17")) throw new AssertionError("初始不是主武器");
        u.switchWeapon();
        if (!u.weapon().id.equals("g17")) throw new AssertionError("切副武器失败: " + u.weapon().id);
        u.switchWeapon();
        if (!u.weapon().id.equals("akm")) throw new AssertionError("切回主武器失败");
        System.out.println("双持: 主 " + u.loadout.primary.weaponId + " / 副 "
                + u.loadout.secondary.weaponId + "，切枪正常");

        // 打空自动切枪（清掉切枪硬直）
        u.ammo = 0;
        u.fireCd = 0f;
        u.reloading = false;
        w.fire(u);
        System.out.println("主武器空弹时自动切到: " + u.weapon().id);
        if (u.usingPrimary) throw new AssertionError("空弹没自动切副武器");
        System.out.println("全枪械/改枪/双持：通过");
    }
}
