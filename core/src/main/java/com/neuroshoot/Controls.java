package com.neuroshoot;

/**
 * 玩家自定义设置：按钮大小、位置偏移、昼夜模式。
 *
 * 存在 Session 里跨界面共享；BattleScreen 的虚拟按键布局完全从这里读取，
 * 因此玩家可以自己把按钮放大、挪到顺手的位置。
 */
public final class Controls {
    /** 按钮缩放：0.7 ~ 1.5。 */
    public float scale = 1f;
    /** 摇杆缩放。 */
    public float stickScale = 1f;
    /** 整体 UI 平移（像素），用于适配不同握持习惯。 */
    public float offsetX;
    public float offsetY;
    /** 是否左手布局（摇杆放右下）。 */
    public boolean leftHanded;
    /** 始终显示激光瞄准线。 */
    public boolean laserSight = true;
    /** 是否夜晚模式。 */
    public boolean nightMode = true;

    public float btn(float base) {
        return base * scale;
    }

    public float stick(float base) {
        return base * stickScale;
    }

    public Controls copy() {
        Controls c = new Controls();
        c.scale = scale;
        c.stickScale = stickScale;
        c.offsetX = offsetX;
        c.offsetY = offsetY;
        c.leftHanded = leftHanded;
        c.laserSight = laserSight;
        c.nightMode = nightMode;
        return c;
    }
}
