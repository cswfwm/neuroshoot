package com.neuroshoot.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * 战争迷雾：用一张低分辨率遮罩贴图表示「哪些地方看得见」。
 *
 * 每格算一次可见性（半径 + 朝向扇形 + 墙体遮挡），写入像素的 alpha，
 * 再线性放大盖到画面上。低分辨率纹理边缘本来就有插值过渡，
 * 所以放大后天然带柔和边缘，不需要逐像素算模糊。
 */
public final class FogOfWar {
    private static final int CELL = 8;
    private Pixmap pix;
    private Texture tex;
    private int w, h;
    private boolean healthy = true;

    public FogOfWar(int mapW, int mapH) {
        w = mapW * CELL;
        h = mapH * CELL;
        pix = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        // 关闭混合：迷雾层要求「直接覆盖」，否则半透明叠加会累积成不透明黑
        pix.setBlending(Pixmap.Blending.None);
        tex = new Texture(pix);
        // 迷雾贴图尺寸是「格子数×8」，不是 2 的幂；GLES2 老机型上非 2 幂 +
        // Linear 过滤常被驱动处理成黑块，因此默认用 Nearest，宁可边缘硬一点。
        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    public void begin() {
        pix.setBlending(Pixmap.Blending.None);
        pix.setColor(0f, 0f, 0f, 0f);
        pix.fill();
    }

    /** 标记一个格子的可见程度：alpha 越大越暗（看不见）。 */
    public void mark(int tx, int ty, boolean visible, float fade) {
        if (visible) return;
        int px0 = tx * CELL;
        int py0 = h - (ty + 1) * CELL;
        float a = Math.min(1f, Math.max(0f, 1f - fade));
        pix.setColor(0f, 0f, 0f, a);
        pix.fillRectangle(px0, py0, CELL, CELL);
    }

    /** 已探索过的区域用半透明迷雾，未探索区域全黑。 */
    public void markExplored(int tx, int ty, boolean explored) {
        if (explored) return;
        int px0 = tx * CELL;
        int py0 = h - (ty + 1) * CELL;
        pix.setColor(0f, 0f, 0f, 1f);
        pix.fillRectangle(px0, py0, CELL, CELL);
    }

    public void end() {
        try {
            tex.draw(pix, 0, 0);
        } catch (Exception e) {
            // 驱动不接受这次上传时，宁可不画迷雾，也不要整屏变黑
            healthy = false;
            com.badlogic.gdx.Gdx.app.error("Fog", "迷雾纹理上传失败", e);
        }
    }

    public void draw(SpriteBatch batch, float x, float y, float w, float h) {
        if (!healthy) return;
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(tex, x, y, w, h);
    }

    /** 纹理上传异常时标记为不健康，调用方直接跳过迷雾层。 */
    public boolean isHealthy() {
        return healthy;
    }

    public void drawTinted(SpriteBatch batch, float x, float y, float w, float h, Color c) {
        batch.setColor(c);
        batch.draw(tex, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    public void dispose() {
        tex.dispose();
        pix.dispose();
    }

    /** 黑色遮罩矩形，用于读条/过场压暗。 */
    public static void dim(ShapeRenderer sr, float x, float y, float w, float h, float alpha) {
        sr.setColor(0f, 0f, 0f, alpha);
        sr.rect(x, y, w, h);
    }

    public static int cell() {
        return CELL;
    }

    public static int deviceWidth() {
        return Gdx.graphics.getWidth();
    }
}
