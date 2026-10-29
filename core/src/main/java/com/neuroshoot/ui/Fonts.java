package com.neuroshoot.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

import java.util.HashMap;
import java.util.Map;

/** 字体与基础绘图资源。 */
public final class Fonts {
    private static final Map<Integer, BitmapFont> CACHE = new HashMap<>();
    private static final String PATH = "fonts/GameFont.ttf";
    private static String chars;
    private static Texture pixel;
    private static Texture vgrad;

    private Fonts() {}

    public static BitmapFont of(int size) {
        BitmapFont f = CACHE.get(size);
        if (f != null) return f;
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal(PATH));
        FreeTypeFontGenerator.FreeTypeFontParameter p =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = size;
        p.color = Color.WHITE;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        p.characters = characters();
        f = gen.generateFont(p);
        gen.dispose();
        CACHE.put(size, f);
        return f;
    }

    private static String characters() {
        if (chars != null) return chars;
        StringBuilder sb = new StringBuilder(4096);
        for (int c = 0x20; c <= 0x7E; c++) sb.append((char) c);
        for (int c = 0x3000; c <= 0x303F; c++) sb.append((char) c);
        for (int c = 0xFF00; c <= 0xFF65; c++) sb.append((char) c);
        for (int c = 0x4E00; c <= 0x9FA5; c++) sb.append((char) c);
        chars = sb.toString();
        return chars;
    }

    public static Texture pixel() {
        if (pixel == null) {
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(Color.WHITE);
            pm.fill();
            pixel = new Texture(pm);
            pm.dispose();
        }
        return pixel;
    }

    /** 垂直渐变，用于战争迷雾的边缘柔化。 */
    public static Texture verticalGradient() {
        if (vgrad == null) {
            int h = 64;
            Pixmap pm = new Pixmap(1, h, Pixmap.Format.RGBA8888);
            for (int y = 0; y < h; y++) {
                int a = (int) (255 * (1f - y / (float) (h - 1)));
                pm.setColor(0f, 0f, 0f, a / 255f);
                pm.drawPixel(0, y);
            }
            vgrad = new Texture(pm);
            pm.dispose();
        }
        return vgrad;
    }

    public static void dispose() {
        for (BitmapFont f : CACHE.values()) f.dispose();
        CACHE.clear();
        if (pixel != null) {
            pixel.dispose();
            pixel = null;
        }
        if (vgrad != null) {
            vgrad.dispose();
            vgrad = null;
        }
    }
}
