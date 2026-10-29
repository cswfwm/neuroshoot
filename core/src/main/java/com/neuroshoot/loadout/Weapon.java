package com.neuroshoot.loadout;

/**
 * 枪械定义：分类 + 基础数值 + 改枪参数。
 *
 * 分类完全对齐三角洲行动：手枪 7 把、霰弹枪 5 把、冲锋枪 10 把、
 * 突击步枪 19 把、狙击步枪 4 把、精确射手步枪 9 把、轻机枪 4 把。
 */
public final class Weapon {
    /** 枪械分类，界面按类分组，也用于 AI 选枪。 */
    public enum Category {
        ASSAULT("突击步枪"),
        SMG("冲锋枪"),
        SHOTGUN("霰弹枪"),
        SNIPER("狙击步枪"),
        DMR("射手步枪"),
        LMG("轻机枪"),
        PISTOL("手枪");

        public final String label;

        Category(String label) {
            this.label = label;
        }
    }

    public final String id;
    public final String name;
    public final Category category;
    /** 口径，仅用于界面显示与同类感。 */
    public final String caliber;

    public final int damage;
    public final float fireInterval;
    public final int magazine;
    public final float spreadHip;
    public final float spreadAim;
    public final float range;
    public final int cost;
    public final boolean breaksDoor;
    public final float moveMul;
    /** 换弹时长（秒）。 */
    public final float reloadTime;
    /** 后坐力：每发导致准星上跳的角度，影响连射精度。 */
    public final float recoil;

    public Weapon(String id, String name, Category category, String caliber,
                  int damage, float fireInterval, int magazine,
                  float spreadHip, float spreadAim, float range, int cost,
                  boolean breaksDoor, float moveMul, float reloadTime, float recoil) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.caliber = caliber;
        this.damage = damage;
        this.fireInterval = fireInterval;
        this.magazine = magazine;
        this.spreadHip = spreadHip;
        this.spreadAim = spreadAim;
        this.range = range;
        this.cost = cost;
        this.breaksDoor = breaksDoor;
        this.moveMul = moveMul;
        this.reloadTime = reloadTime;
        this.recoil = recoil;
    }

    /** 全部枪械，按分类排列。数值取向参考三角洲的枪械分工。 */
    public static final Weapon[] ALL = {
            // ── 手枪 7 把 ──
            new Weapon("g17", "G17", Category.PISTOL, "9×19mm", 16, 0.22f, 100,
                    0.09f, 0.04f, 750f, 400, false, 1.06f, 1.4f, 1.0f),
            new Weapon("qsz92g", "QSZ-92G", Category.PISTOL, "9×19mm", 18, 0.24f, 100,
                    0.085f, 0.038f, 780f, 420, false, 1.05f, 1.45f, 1.1f),
            new Weapon("deagle", "沙漠之鹰", Category.PISTOL, ".50 AE", 42, 0.5f, 84,
                    0.1f, 0.03f, 800f, 700, false, 1.02f, 1.8f, 3.0f),
            new Weapon("beretta93r", "93R", Category.PISTOL, "9×19mm", 20, 0.28f, 96,
                    0.095f, 0.042f, 760f, 480, false, 1.04f, 1.5f, 1.3f),
            new Weapon("g18", "G18", Category.PISTOL, "9×19mm", 12, 0.09f, 100,
                    0.13f, 0.07f, 700f, 520, false, 1.08f, 1.3f, 0.7f),
            new Weapon("r357", ".357 左轮", Category.PISTOL, ".357 Mag", 34, 0.42f, 84,
                    0.095f, 0.028f, 820f, 620, false, 1.03f, 1.7f, 2.6f),
            new Weapon("m1911", "M1911", Category.PISTOL, ".45 ACP", 28, 0.3f, 88,
                    0.088f, 0.032f, 780f, 500, false, 1.04f, 1.5f, 1.6f),

            // ── 霰弹枪 5 把 ──
            new Weapon("m870", "M870", Category.SHOTGUN, "12 号", 26, 0.75f, 100,
                    0.16f, 0.09f, 420f, 1100, true, 0.95f, 2.6f, 3.2f),
            new Weapon("m1014", "M1014", Category.SHOTGUN, "12 号", 24, 0.5f, 100,
                    0.15f, 0.085f, 440f, 1250, true, 0.94f, 2.4f, 2.8f),
            new Weapon("s12k", "S12K", Category.SHOTGUN, "12 号", 22, 0.38f, 100,
                    0.15f, 0.08f, 460f, 1300, true, 0.93f, 2.2f, 2.4f),
            new Weapon("725", "725 双管", Category.SHOTGUN, "12 号", 38, 0.9f, 40,
                    0.2f, 0.12f, 380f, 800, true, 0.97f, 2.8f, 4.0f),
            new Weapon("fs12", "FS12", Category.SHOTGUN, "12 号", 23, 0.55f, 100,
                    0.155f, 0.088f, 430f, 1050, true, 0.98f, 2.5f, 2.6f),

            // ── 冲锋枪 10 把 ──
            new Weapon("uzi", "UZI", Category.SMG, "9×19mm", 10, 0.075f, 400,
                    0.12f, 0.055f, 680f, 750, false, 1.07f, 1.5f, 0.5f),
            new Weapon("bizon", "野牛", Category.SMG, "9×19mm", 11, 0.08f, 400,
                    0.115f, 0.052f, 700f, 800, false, 1.06f, 1.5f, 0.55f),
            new Weapon("pp19", "勇士", Category.SMG, "9×19mm", 13, 0.082f, 400,
                    0.11f, 0.048f, 720f, 850, false, 1.05f, 1.55f, 0.6f),
            new Weapon("mp5", "MP5", Category.SMG, "9×19mm", 11, 0.072f, 400,
                    0.11f, 0.048f, 700f, 900, false, 1.06f, 1.5f, 0.5f),
            new Weapon("smg45", "SMG-45", Category.SMG, ".45 ACP", 16, 0.1f, 400,
                    0.115f, 0.05f, 700f, 920, false, 1.03f, 1.6f, 0.9f),
            new Weapon("sr3m", "SR-3M", Category.SMG, "9×39mm", 20, 0.09f, 400,
                    0.13f, 0.06f, 620f, 1000, false, 1.0f, 1.7f, 1.4f),
            new Weapon("p90", "P90", Category.SMG, "5.7×28mm", 12, 0.08f, 400,
                    0.1f, 0.042f, 720f, 950, false, 1.05f, 1.6f, 0.6f),
            new Weapon("mp7", "MP7", Category.SMG, "4.6×30mm", 10, 0.07f, 400,
                    0.12f, 0.055f, 680f, 930, false, 1.07f, 1.5f, 0.45f),
            new Weapon("vector", "Vector", Category.SMG, ".45 ACP", 9, 0.055f, 400,
                    0.12f, 0.05f, 660f, 980, false, 1.08f, 1.5f, 0.4f),
            new Weapon("qcq171", "QCQ-171", Category.SMG, "9×19mm", 12, 0.078f, 400,
                    0.108f, 0.046f, 710f, 880, false, 1.06f, 1.55f, 0.55f),

            // ── 突击步枪 19 把 ──
            new Weapon("car15", "CAR-15", Category.ASSAULT, "5.56×45mm", 17, 0.11f, 180,
                    0.08f, 0.026f, 880f, 900, false, 1.02f, 1.7f, 0.85f),
            new Weapon("aks74u", "AKS-74U", Category.ASSAULT, "5.45×39mm", 19, 0.105f, 180,
                    0.085f, 0.028f, 850f, 950, false, 1.03f, 1.7f, 1.0f),
            new Weapon("m4a1", "M4A1", Category.ASSAULT, "5.56×45mm", 18, 0.105f, 180,
                    0.075f, 0.022f, 900f, 1200, false, 1.0f, 1.7f, 0.9f),
            new Weapon("akm", "AKM", Category.ASSAULT, "7.62×39mm", 24, 0.12f, 180,
                    0.09f, 0.03f, 880f, 1180, false, 0.99f, 1.8f, 1.5f),
            new Weapon("qbz95", "QBZ-95-1", Category.ASSAULT, "5.8×42mm", 20, 0.11f, 180,
                    0.08f, 0.024f, 900f, 1220, false, 1.0f, 1.75f, 1.1f),
            new Weapon("k416", "K416", Category.ASSAULT, "5.56×45mm", 19, 0.098f, 180,
                    0.078f, 0.023f, 900f, 1300, false, 1.0f, 1.7f, 0.95f),
            new Weapon("k437", "K437", Category.ASSAULT, ".300 BLK", 22, 0.105f, 180,
                    0.082f, 0.025f, 860f, 1280, false, 1.0f, 1.75f, 1.2f),
            new Weapon("ak12", "AK-12", Category.ASSAULT, "5.45×39mm", 22, 0.12f, 180,
                    0.09f, 0.03f, 880f, 1250, false, 0.98f, 1.8f, 1.5f),
            new Weapon("ptr32", "PTR-32", Category.ASSAULT, "7.62×39mm", 25, 0.125f, 180,
                    0.095f, 0.032f, 870f, 1240, false, 0.97f, 1.85f, 1.7f),
            new Weapon("asval", "AS Val", Category.ASSAULT, "9×39mm", 21, 0.09f, 180,
                    0.12f, 0.055f, 640f, 1350, false, 0.99f, 1.8f, 1.6f),
            new Weapon("qbz191", "腾龙 QBZ-191", Category.ASSAULT, "5.8×42mm", 22, 0.108f, 180,
                    0.082f, 0.025f, 910f, 1380, false, 0.99f, 1.75f, 1.25f),
            new Weapon("aug", "AUG A3", Category.ASSAULT, "5.56×45mm", 19, 0.1f, 180,
                    0.07f, 0.02f, 920f, 1280, false, 1.0f, 1.7f, 0.8f),
            new Weapon("sg552", "SG552", Category.ASSAULT, "5.56×45mm", 15, 0.095f, 180,
                    0.075f, 0.022f, 880f, 1000, false, 1.02f, 1.65f, 0.6f),
            new Weapon("m16a4", "M16A4", Category.ASSAULT, "5.56×45mm", 20, 0.115f, 180,
                    0.07f, 0.018f, 950f, 1150, false, 1.0f, 1.75f, 0.9f),
            new Weapon("g3", "G3", Category.ASSAULT, "7.62×51mm", 30, 0.16f, 150,
                    0.085f, 0.022f, 1050f, 1320, false, 0.95f, 1.95f, 2.2f),
            new Weapon("m7", "M7", Category.ASSAULT, "6.8×51mm", 32, 0.13f, 150,
                    0.09f, 0.024f, 1000f, 1550, false, 0.94f, 2.0f, 2.4f),
            new Weapon("scarh", "SCAR-H", Category.ASSAULT, "7.62×51mm", 28, 0.135f, 150,
                    0.085f, 0.026f, 950f, 1350, false, 0.95f, 1.9f, 1.9f),
            new Weapon("ash12", "ASh-12", Category.ASSAULT, "12.7×55mm", 48, 0.28f, 120,
                    0.14f, 0.06f, 620f, 1600, false, 0.9f, 2.3f, 4.2f),
            new Weapon("kc17", "KC17", Category.ASSAULT, "5.45×39mm", 21, 0.1f, 180,
                    0.08f, 0.022f, 930f, 1400, false, 0.99f, 1.75f, 1.2f),

            // ── 狙击步枪 4 把 ──
            new Weapon("sv98", "SV-98", Category.SNIPER, "7.62×54R", 74, 1.05f, 200,
                    0.095f, 0.0035f, 1450f, 1700, false, 0.85f, 2.8f, 4.2f),
            new Weapon("r93", "R93", Category.SNIPER, "7.62×51mm", 80, 1.1f, 200,
                    0.09f, 0.003f, 1480f, 1750, false, 0.85f, 2.9f, 4.4f),
            new Weapon("m700", "M700", Category.SNIPER, "7.62×51mm", 72, 0.95f, 200,
                    0.1f, 0.004f, 1500f, 1600, false, 0.86f, 2.6f, 3.8f),
            new Weapon("awm", "AWM", Category.SNIPER, ".338 LM", 96, 1.25f, 200,
                    0.1f, 0.003f, 1550f, 1900, false, 0.82f, 3.0f, 5.0f),

            // ── 精确射手步枪 9 把 ──
            new Weapon("sr9", "SR9", Category.DMR, "7.62×51mm", 26, 0.3f, 150,
                    0.07f, 0.02f, 1100f, 900, false, 0.96f, 2.0f, 1.6f),
            new Weapon("mini14", "Mini-14", Category.DMR, "5.56×45mm", 24, 0.28f, 150,
                    0.075f, 0.022f, 1050f, 1000, false, 0.97f, 1.95f, 1.4f),
            new Weapon("m14", "M14", Category.DMR, "7.62×51mm", 34, 0.26f, 150,
                    0.085f, 0.028f, 1120f, 1450, false, 0.92f, 2.1f, 2.6f),
            new Weapon("sks", "SKS", Category.DMR, "7.62×39mm", 30, 0.3f, 150,
                    0.06f, 0.014f, 1150f, 1050, false, 0.95f, 2.0f, 1.9f),
            new Weapon("psg1", "PSG-1", Category.DMR, "7.62×51mm", 36, 0.32f, 150,
                    0.055f, 0.01f, 1250f, 1550, false, 0.9f, 2.2f, 2.2f),
            new Weapon("vss", "VSS", Category.DMR, "9×39mm", 32, 0.25f, 150,
                    0.1f, 0.035f, 900f, 1400, false, 0.94f, 2.0f, 1.8f),
            new Weapon("svd", "SVD", Category.DMR, "7.62×54R", 38, 0.35f, 150,
                    0.075f, 0.014f, 1300f, 1500, false, 0.9f, 2.15f, 2.4f),
            new Weapon("sr25", "SR-25", Category.DMR, "7.62×51mm", 34, 0.32f, 150,
                    0.055f, 0.012f, 1200f, 1400, false, 0.93f, 2.0f, 2.0f),
            new Weapon("svch", "SVCH", Category.DMR, "7.62×54R", 40, 0.34f, 150,
                    0.08f, 0.016f, 1280f, 1650, false, 0.9f, 2.2f, 2.8f),

            // ── 轻机枪 4 把 ──
            new Weapon("m249", "M249", Category.LMG, "5.56×45mm", 16, 0.085f, 300,
                    0.13f, 0.06f, 820f, 1550, false, 0.8f, 3.4f, 1.6f),
            new Weapon("m250", "M250", Category.LMG, "6.8×51mm", 22, 0.1f, 300,
                    0.14f, 0.065f, 860f, 1700, false, 0.78f, 3.6f, 2.0f),
            new Weapon("pkm", "PKM", Category.LMG, "7.62×54R", 20, 0.1f, 300,
                    0.14f, 0.065f, 860f, 1600, false, 0.78f, 3.6f, 2.0f),
            new Weapon("qjb201", "QJB-201", Category.LMG, "5.8×42mm", 18, 0.09f, 300,
                    0.13f, 0.058f, 840f, 1500, false, 0.81f, 3.4f, 1.7f),
    };

    public static Weapon byId(String id) {
        for (Weapon w : ALL) if (w.id.equals(id)) return w;
        return ALL[0];
    }

    public static Weapon[] ofCategory(Category c) {
        int n = 0;
        for (Weapon w : ALL) if (w.category == c) n++;
        Weapon[] out = new Weapon[n];
        int i = 0;
        for (Weapon w : ALL) if (w.category == c) out[i++] = w;
        return out;
    }

    /** 霰弹类一次打出多颗弹丸。 */
    public int pellets() {
        if ("725".equals(id)) return 10;
        if (category == Category.SHOTGUN) return 8;
        return 1;
    }

    public boolean isShotgun() {
        return category == Category.SHOTGUN;
    }

    /** 各分类的枪械数量，供界面显示。 */
    public static int countOf(Category c) {
        return ofCategory(c).length;
    }
}
