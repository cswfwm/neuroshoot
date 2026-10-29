package com.neuroshoot.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.neuroshoot.world.C;
import com.neuroshoot.world.GameMap;
import com.neuroshoot.world.MapFactory;
import com.neuroshoot.world.Tile;
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.World;

/**
 * 右上角小地图：只画基本地形（地面/墙/门/楼梯），不显示敌人位置，
 * 玩家自己的位置用一个亮点标出。这样小地图只提供空间感，不破坏迷雾的信息差。
 */
public final class MiniMap {
    private static final float SIZE = 168f;
    private static final float MARGIN = 16f;

    private float ox, oy;
    /** 击杀发生时短暂标记敌人位置：{x, y, 剩余秒数}。 */
    private final java.util.List<float[]> marks = new java.util.ArrayList<>();

    /** 记录一次击杀预警，敌人位置会在小地图上闪 6 秒。 */
    public void markEnemy(Unit u) {
        marks.removeIf(m -> m[2] <= 0f);
        marks.add(new float[]{u.x, u.y, 6f});
        if (marks.size() > 8) marks.remove(0);
    }

    public void tick(float dt) {
        for (int i = marks.size() - 1; i >= 0; i--) {
            marks.get(i)[2] -= dt;
            if (marks.get(i)[2] <= 0f) marks.remove(i);
        }
    }

    public void draw(ShapeRenderer sr, MapFactory layout, World world, Unit me,
                     float shakeX, float shakeY) {
        ox = C.VIEW_W - SIZE - MARGIN + shakeX;
        oy = C.VIEW_H - SIZE - MARGIN + shakeY;
        Draw.panel(sr, ox, oy, SIZE, SIZE, 0.72f);
        float sx = (SIZE - 12f) / C.MAP_W;
        float sy = (SIZE - 12f) / C.MAP_H;
        float cel = Math.min(sx, sy);
        float bx = ox + 6f, by = oy + 6f;
        GameMap map = layout.map;
        for (int ty = 0; ty < C.MAP_H; ty++) {
            for (int tx = 0; tx < C.MAP_W; tx++) {
                byte t = map.at(tx, ty);
                Color c;
                if (t == Tile.WALL) c = Draw.rgba(72, 82, 100);
                else if (t == Tile.DOOR) c = Draw.rgba(150, 120, 70);
                else if (t == Tile.PILLAR) c = Draw.rgba(96, 100, 110);
                else if (t == Tile.STAIR) c = Draw.rgba(130, 116, 96);
                else if (t == Tile.FLOOR) c = Draw.rgba(84, 74, 60);
                else c = Draw.rgba(42, 62, 44);
                Draw.rect(sr, bx + tx * cel, by + (C.MAP_H - 1 - ty) * cel, cel, cel, c);
            }
        }
        // 玩家位置
        float px = bx + (me.x / C.TILE) * cel;
        float py = by + (C.MAP_H - (me.y / C.TILE)) * cel;
        Draw.circle(sr, px, py, 3.4f, Draw.rgba(130, 220, 255));
        Draw.ring(sr, px, py, 6f, 1f, Draw.rgba(130, 220, 255, 0.6f));

        // 击杀预警：敌人位置以红点闪烁 6 秒
        for (float[] m : marks) {
            float mx = bx + (m[0] / C.TILE) * cel;
            float my = by + (C.MAP_H - (m[1] / C.TILE)) * cel;
            float life = Math.min(1f, m[2] / 6f);
            float pulse = 0.55f + 0.45f * (float) Math.sin(m[2] * 9f);
            Draw.circle(sr, mx, my, 3.2f, new Color(1f, 0.28f, 0.28f, pulse * life));
            Draw.ring(sr, mx, my, 7f, 1.2f, new Color(1f, 0.4f, 0.4f, 0.45f * life));
        }
    }

    public void drawLabels(SpriteBatch batch) {
        Draw.text(batch, Fonts.of(12), "小地图", ox + 6f, oy + SIZE - 4f,
                Draw.rgba(150, 168, 190));
    }

    public float x() {
        return ox;
    }

    public float y() {
        return oy;
    }

    public static float size() {
        return SIZE;
    }
}
