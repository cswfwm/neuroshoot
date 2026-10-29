package com.neuroshoot.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** 绘图辅助：矩形、圆角矩形、圆、线、文字。 */
public final class Draw {
    private static GlyphLayout layout;

    private Draw() {}

    public static Color rgba(int r, int g, int b) {
        return new Color(r / 255f, g / 255f, b / 255f, 1f);
    }

    public static Color rgba(int r, int g, int b, float a) {
        return new Color(r / 255f, g / 255f, b / 255f, a);
    }

    public static void rect(ShapeRenderer sr, float x, float y, float w, float h, Color c) {
        sr.setColor(c);
        sr.rect(x, y, w, h);
    }

    public static void roundRect(ShapeRenderer sr, float x, float y, float w, float h,
                                 float r, Color c) {
        sr.setColor(c);
        sr.rect(x + r, y, w - 2 * r, h);
        sr.rect(x, y + r, w, h - 2 * r);
        sr.circle(x + r, y + r, r, 12);
        sr.circle(x + w - r, y + r, r, 12);
        sr.circle(x + r, y + h - r, r, 12);
        sr.circle(x + w - r, y + h - r, r, 12);
    }

    public static void roundBorder(ShapeRenderer sr, float x, float y, float w, float h,
                                   float r, float t, Color c) {
        sr.setColor(c);
        sr.rectLine(x + r, y, x + w - r, y, t);
        sr.rectLine(x + r, y + h, x + w - r, y + h, t);
        sr.rectLine(x, y + r, x, y + h - r, t);
        sr.rectLine(x + w, y + r, x + w, y + h - r, t);
        arc(sr, x + r, y + r, r, 180, 90, t);
        arc(sr, x + w - r, y + r, r, 270, 90, t);
        arc(sr, x + r, y + h - r, r, 90, 90, t);
        arc(sr, x + w - r, y + h - r, r, 0, 90, t);
    }

    private static void arc(ShapeRenderer sr, float cx, float cy, float r,
                            float start, float deg, float t) {
        int seg = 8;
        float prevX = 0f, prevY = 0f;
        for (int i = 0; i <= seg; i++) {
            double a = Math.toRadians(start + deg * i / (double) seg);
            float px = cx + (float) Math.cos(a) * r;
            float py = cy + (float) Math.sin(a) * r;
            if (i > 0) sr.rectLine(prevX, prevY, px, py, t);
            prevX = px;
            prevY = py;
        }
    }

    public static void line(ShapeRenderer sr, float x1, float y1, float x2, float y2,
                            float w, Color c) {
        sr.setColor(c);
        sr.rectLine(x1, y1, x2, y2, w);
    }

    public static void circle(ShapeRenderer sr, float x, float y, float r, Color c) {
        sr.setColor(c);
        sr.circle(x, y, r, 20);
    }

    public static void ring(ShapeRenderer sr, float x, float y, float r, float w, Color c) {
        sr.setColor(c);
        int seg = 32;
        float px = x + r, py = y;
        for (int i = 1; i <= seg; i++) {
            double a = Math.PI * 2 * i / seg;
            float nx = x + (float) Math.cos(a) * r;
            float ny = y + (float) Math.sin(a) * r;
            sr.rectLine(px, py, nx, ny, w);
            px = nx;
            py = ny;
        }
    }

    public static void text(SpriteBatch batch, BitmapFont f, String s, float x, float y, Color c) {
        f.setColor(c);
        f.draw(batch, s, x, y);
    }

    public static void textCentered(SpriteBatch batch, BitmapFont f, String s,
                                    float cx, float y, Color c) {
        f.setColor(c);
        f.draw(batch, s, cx - width(f, s) / 2f, y);
    }

    public static void textRight(SpriteBatch batch, BitmapFont f, String s,
                                 float right, float y, Color c) {
        f.setColor(c);
        f.draw(batch, s, right - width(f, s), y);
    }

    public static float width(BitmapFont f, String s) {
        if (layout == null) layout = new GlyphLayout();
        layout.setText(f, s);
        return layout.width;
    }

    public static void icon(SpriteBatch batch, Texture t, float x, float y, float w, float h) {
        batch.setColor(Color.WHITE);
        batch.draw(t, x, y, w, h);
    }

    public static void panel(ShapeRenderer sr, float x, float y, float w, float h, float alpha) {
        roundRect(sr, x, y, w, h, 10f, rgba(12, 16, 24, alpha));
        roundBorder(sr, x, y, w, h, 10f, 1.5f, rgba(70, 88, 116, 0.9f));
    }
}
