package com.neuroshoot.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.neuroshoot.world.C;
import com.neuroshoot.world.Unit;

/**
 * 投掷物轮盘：右侧一圈扇区，点击哪个扇区就扔哪一种。
 *
 * 比原来竖排图标更好按——拇指在屏幕右侧自然扫过，六个可能投掷物
 * 按圆周均分，每个扇区都有足够大的命中面积，不会误点。
 */
public final class ThrowWheel {
    private static final float CX = C.VIEW_W - 132f;
    private static final float CY = C.VIEW_H * 0.46f;
    private static final float R_OUTER = 118f;
    private static final float R_INNER = 46f;

    /** 展开状态：平时收成一圈半透明提示，按住时完全展开。 */
    public boolean expanded;
    /** 当前选中的扇区，-1 表示没有。 */
    public int selected = -1;

    private float angleOf(int index, int total) {
        // 从正上方开始顺时针排布，避免被右下角弹药框挡住
        return 90f - 360f * index / (float) total;
    }

    /** 返回点击命中的扇区下标，未命中返回 -1。 */
    public int hitTest(float x, float y, int count) {
        if (count <= 0) return -1;
        float dx = x - CX;
        float dy = y - CY;
        float dist = (float) Math.hypot(dx, dy);
        if (dist < R_INNER || dist > R_OUTER + 18f) return -1;
        float ang = (float) Math.toDegrees(Math.atan2(dy, dx));
        float span = 360f / count;
        float rel = (90f - ang + 360f) % 360f;
        int idx = (int) (rel / span);
        return Math.min(count - 1, Math.max(0, idx));
    }

    public void draw(ShapeRenderer sr, Unit me, boolean pressed) {
        int count = me.throwables.size();
        float alpha = pressed || expanded ? 1f : 0.55f;
        Draw.circle(sr, CX, CY, R_INNER - 6f, Draw.rgba(14, 18, 26, 0.62f * alpha));
        if (count == 0) {
            Draw.ring(sr, CX, CY, R_INNER - 6f, 2f, Draw.rgba(90, 100, 120, 0.5f * alpha));
            return;
        }
        float span = 360f / count;
        for (int i = 0; i < count; i++) {
            float mid = angleOf(i, count);
            boolean sel = i == selected;
            float a0 = mid - span * 0.42f;
            float a1 = mid + span * 0.42f;
            Color c = sel ? Draw.rgba(232, 150, 90, 0.78f * alpha)
                    : Draw.rgba(46, 64, 92, 0.66f * alpha);
            sr.setColor(c);
            // 用若干三角形拼出扇环
            int seg = 8;
            for (int k = 0; k < seg; k++) {
                float t0 = a0 + (a1 - a0) * k / seg;
                float t1 = a0 + (a1 - a0) * (k + 1) / seg;
                float x0 = CX + (float) Math.cos(Math.toRadians(t0)) * R_INNER;
                float y0 = CY + (float) Math.sin(Math.toRadians(t0)) * R_INNER;
                float x1 = CX + (float) Math.cos(Math.toRadians(t1)) * R_INNER;
                float y1 = CY + (float) Math.sin(Math.toRadians(t1)) * R_INNER;
                float x2 = CX + (float) Math.cos(Math.toRadians(t1)) * R_OUTER;
                float y2 = CY + (float) Math.sin(Math.toRadians(t1)) * R_OUTER;
                float x3 = CX + (float) Math.cos(Math.toRadians(t0)) * R_OUTER;
                float y3 = CY + (float) Math.sin(Math.toRadians(t0)) * R_OUTER;
                sr.triangle(x0, y0, x1, y1, x2, y2);
                sr.triangle(x0, y0, x2, y2, x3, y3);
            }
        }
        centerDisc(sr);
        Draw.ring(sr, CX, CY, R_INNER, 2f, Draw.rgba(150, 180, 220, 0.6f * alpha));
        Draw.ring(sr, CX, CY, R_OUTER, 2f, Draw.rgba(150, 180, 220, 0.5f * alpha));
    }

    /** 中心圆盘，数量文字在 drawIcons 里用 SpriteBatch 画。 */
    private void centerDisc(ShapeRenderer sr) {
        Draw.circle(sr, CX, CY, 22f, Draw.rgba(232, 150, 90, 0.26f));
    }

    public void drawIcons(SpriteBatch batch, Unit me, boolean pressed) {
        int count = me.throwables.size();
        if (count == 0) {
            Draw.textCentered(batch, Fonts.of(13), "无投掷物", CX, CY - 6f,
                    Draw.rgba(140, 150, 170));
            return;
        }
        float span = 360f / count;
        float r = (R_INNER + R_OUTER) * 0.5f;
        for (int i = 0; i < count; i++) {
            float mid = angleOf(i, count);
            float x = CX + (float) Math.cos(Math.toRadians(mid)) * r;
            float y = CY + (float) Math.sin(Math.toRadians(mid)) * r;
            float size = i == selected ? 46f : 38f;
            Draw.icon(batch, Art.throwable(me.throwables.get(i)),
                    x - size / 2f, y - size / 2f, size, size);
        }
        Draw.textCentered(batch, Fonts.of(15), String.valueOf(count), CX, CY - 5f, Color.WHITE);
        if (!pressed) {
            Draw.textCentered(batch, Fonts.of(11), "投掷", CX, CY - 22f,
                    Draw.rgba(190, 205, 225, 0.75f));
        }
    }

    public float centerX() {
        return CX;
    }

    public float centerY() {
        return CY;
    }

    public float outer() {
        return R_OUTER;
    }
}
