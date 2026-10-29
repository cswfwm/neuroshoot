package com.neuroshoot.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** 适应度折线图：把数组按最大最小值归一化后画成折线。 */
public final class Chart {
    private Chart() {}

    /** 画一条指定颜色的曲线；clear=true 时先铺底色（多条曲线叠加时只第一条清底）。 */
    public static void drawColored(ShapeRenderer sr, float[] values, int count,
                                   float x, float y, float w, float h,
                                   Color color, boolean clear) {
        if (clear) {
            Draw.rect(sr, x, y, w, h, Draw.rgba(14, 18, 26, 1f));
            Draw.roundBorder(sr, x, y, w, h, 6f, 1f, Draw.rgba(52, 64, 86));
        }
        if (count < 2) return;
        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            min = Math.min(min, values[i]);
            max = Math.max(max, values[i]);
        }
        float span = Math.max(0.001f, max - min);
        float stepX = (w - 12f) / Math.max(1, count - 1);
        for (int i = 1; i < count; i++) {
            float x1 = x + 6f + (i - 1) * stepX;
            float x2 = x + 6f + i * stepX;
            float y1 = y + 8f + (values[i - 1] - min) / span * (h - 16f);
            float y2 = y + 8f + (values[i] - min) / span * (h - 16f);
            Draw.line(sr, x1, y1, x2, y2, 1.8f, color);
        }
    }

    public static void draw(ShapeRenderer sr, float[] values, int count,
                            float x, float y, float w, float h) {
        Draw.rect(sr, x, y, w, h, Draw.rgba(14, 18, 26, 1f));
        Draw.roundBorder(sr, x, y, w, h, 6f, 1f, Draw.rgba(52, 64, 86));
        if (count < 2) return;
        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            min = Math.min(min, values[i]);
            max = Math.max(max, values[i]);
        }
        float span = Math.max(0.001f, max - min);
        float stepX = (w - 12f) / Math.max(1, count - 1);
        for (int i = 1; i < count; i++) {
            float x1 = x + 6f + (i - 1) * stepX;
            float x2 = x + 6f + i * stepX;
            float y1 = y + 8f + (values[i - 1] - min) / span * (h - 16f);
            float y2 = y + 8f + (values[i] - min) / span * (h - 16f);
            Draw.line(sr, x1, y1, x2, y2, 2f, Draw.rgba(110, 200, 255));
        }
        float lx = x + 6f + (count - 1) * stepX;
        float ly = y + 8f + (values[count - 1] - min) / span * (h - 16f);
        Draw.circle(sr, lx, ly, 3.4f, new Color(1f, 0.9f, 0.5f, 1f));
    }
}
