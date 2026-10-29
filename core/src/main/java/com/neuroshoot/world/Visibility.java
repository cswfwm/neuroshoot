package com.neuroshoot.world;

/**
 * 可见性判定：把「视野半径 + 朝向扇形 + 墙体遮挡」三者合起来。
 *
 * 迷雾渲染按格子采样，所以这里以格子为单位给出是否可见；
 * 视野是正前方的一个半圆：背后完全看不见，因此绕后是有效战术。
 */
public final class Visibility {
    private Visibility() {}

    /** 该格中心是否落在观察者的视野范围内且未被遮挡。 */
    public static boolean tileVisible(GameMap map, Unit u, int tx, int ty) {
        return tileVisible(map, u.x, u.y, u.angle, u.viewRadius(), u.viewFov(), tx, ty);
    }

    public static boolean tileVisible(GameMap map, float ux, float uy, float angle,
                                      float radius, float fov, int tx, int ty) {
        float cx = (tx + 0.5f) * C.TILE;
        float cy = (ty + 0.5f) * C.TILE;
        float dx = cx - ux, dy = cy - uy;
        float dist = (float) Math.hypot(dx, dy);
        if (dist > radius) return false;
        // 半圆判定：只保留朝向两侧各 VIEW_FOV 的范围，背后一律不可见
        float diff = Math.abs(normalize((float) Math.atan2(dy, dx) - angle));
        if (diff > fov) return false;
        return map.lineOfSight(ux, uy, cx, cy);
    }

    /** 单位是否可见（取其中心格）。 */
    public static boolean unitVisible(GameMap map, Unit observer, Unit target) {
        // 不同楼层互相看不见：人在二楼，看不见一楼的敌人
        if (observer.floor != target.floor) return false;
        int tx = (int) (target.x / C.TILE);
        int ty = (int) (target.y / C.TILE);
        float dx = target.x - observer.x, dy = target.y - observer.y;
        float dist = (float) Math.hypot(dx, dy);
        if (dist > observer.viewRadius()) return false;
        float diff = Math.abs(normalize((float) Math.atan2(dy, dx) - observer.angle));
        if (diff > observer.viewFov()) return false;
        // 蹲在掩体后时更难被发现：距离衰减
        if (target.crouch) {
            float hide = 0.72f;
            if (dist > observer.viewRadius() * hide) return false;
        }
        return map.lineOfSight(observer.x, observer.y, target.x, target.y)
                && map.lineOfSight(target.x, target.y, observer.x, observer.y);
    }

    public static float normalize(float a) {
        while (a > Math.PI) a -= (float) (Math.PI * 2);
        while (a < -Math.PI) a += (float) (Math.PI * 2);
        return a;
    }
}
