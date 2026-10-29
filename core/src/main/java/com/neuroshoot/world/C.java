package com.neuroshoot.world;

/** 全局参数：视口、地图、物理、视野与积分。 */
public final class C {
    public static final float VIEW_W = 1280f;
    public static final float VIEW_H = 720f;

    /** 地图以格子为单位，每格 64 像素。 */
    public static final float TILE = 64f;
    public static final int MAP_W = 46;
    public static final int MAP_H = 40;
    public static final float WORLD_W = MAP_W * TILE;
    public static final float WORLD_H = MAP_H * TILE;

    public static final float MOVE_SPEED = 210f;
    public static final float CROUCH_SPEED = 96f;
    public static final float AIM_SPEED = 108f;
    public static final float PANIC_SPEED_MUL = 0.62f;
    public static final float RADIUS = 17f;

    /** 视野：站立/下蹲/开镜。迷雾半径随姿态变化。 */
    public static final float VIEW_STAND = 430f;
    public static final float VIEW_CROUCH = 250f;
    public static final float VIEW_AIM = 560f;
    /** 朝向扇形的半角，超出扇形只保留很短的贴身视野。 */
    /** 视野半角：π/2 即正前方 180° 的半圆，背后完全看不见。 */
    public static final float VIEW_FOV = (float) (Math.PI / 2.0);

    // ── 昼夜模式 ──
    // 白天：视野 80 米（按 32px≈1m 折算为 2560px），视角 100°
    // 夜晚：视野极小（与旧版一致约 13 米），视角 132°，需要夜视仪补偿
    public static final float DAY_VIEW = 2560f;
    public static final float DAY_FOV = (float) Math.toRadians(50.0);   // 半角 → 全角 100°
    public static final float NIGHT_VIEW = 430f;
    public static final float NIGHT_FOV = (float) Math.toRadians(66.0); // 半角 → 全角 132°

    /** 夜视仪在夜晚把视野放大到白天的 55%。 */
    public static final float NVG_BOOST = 0.55f;
    /** 已废弃的背后视野，保留常量以兼容旧引用，值为 0 表示不启用。 */
    public static final float VIEW_BACK = 0f;

    public static final int HP_MAX = 100;
    public static final int ARMOR_MAX = 60;
    public static final float MATCH_TIME = 300f;

    public static final float BULLET_SPEED = 1150f;
    public static final float BLEED_INTERVAL = 1.5f;
    public static final float BLOOD_FADE = 26f;
    public static final float PAINKILLER_TIME = 12f;
    public static final float PANIC_TIME = 2.2f;
    public static final int THROW_MAX = 6;
    public static final int MED_MAX = 7;
    public static final int POINTS = 5000;

    /** 神经网络输入：自身状态 + 最近敌人 + 视线 + 弹药 + 附近掩体。 */
    public static final int[] LAYERS = {20, 24, 8};
    public static final int INPUTS = 20;
    public static final int OUTPUTS = 8;

    private C() {}
}
