package com.neuroshoot.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.neuroshoot.loadout.Consumable;
import com.neuroshoot.world.C;
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * 平视界面：左上状态与队友血条、右下弹药、左侧投掷物与药品栏、
 * 中上击杀提示、受击恐慌时的画面压暗。
 */
public final class Hud {
    private static final Color C_PANEL = Draw.rgba(10, 14, 20, 0.62f);
    private static final Color C_TEXT = Draw.rgba(228, 236, 246, 1f);
    private static final Color C_DIM = Draw.rgba(150, 162, 182, 1f);

    private final List<String> killFeed = new ArrayList<>();
    private final List<Float> killAge = new ArrayList<>();
    private float shakeT;

    public void pushKill(String killer, String victim) {
        killFeed.add(0, killer + "  击杀  " + victim);
        killAge.add(0, 3.6f);
        if (killFeed.size() > 4) {
            killFeed.remove(killFeed.size() - 1);
            killAge.remove(killAge.size() - 1);
        }
    }

    public void explosion(float x, float y, float r) {
        shakeT = 0.22f;
    }

    public void doorBroken(float x, float y) {
        shakeT = 0.12f;
    }

    public void crateBroken(float x, float y) {
    }

    public void tick(float dt) {
        for (int i = killAge.size() - 1; i >= 0; i--) {
            killAge.set(i, killAge.get(i) - dt);
            if (killAge.get(i) <= 0f) {
                killAge.remove(i);
                killFeed.remove(i);
            }
        }
        shakeT = Math.max(0f, shakeT - dt);
    }

    /** 左上：血量、护甲与状态图标。 */
    public void drawStatus(ShapeRenderer sr, Unit me, World world) {
        Draw.panel(sr, 16f, C.VIEW_H - 132f, 392f, 116f, 0.72f);
        // 血条
        float bx = 30f, by = C.VIEW_H - 62f, bw = 240f, bh = 18f;
        Draw.roundRect(sr, bx, by, bw, bh, 5f, Draw.rgba(44, 22, 26, 1f));
        float ratio = Math.max(0f, me.hp / (float) C.HP_MAX);
        Color hc = ratio > 0.55f ? Draw.rgba(96, 208, 128)
                : ratio > 0.25f ? Draw.rgba(240, 196, 78)
                : Draw.rgba(232, 84, 84);
        Draw.roundRect(sr, bx, by, Math.max(4f, bw * ratio), bh, 5f, hc);
        // 护甲条
        Draw.rect(sr, bx, by - 9f, bw, 5f, Draw.rgba(30, 40, 56, 1f));
        Draw.rect(sr, bx, by - 9f, bw * (me.armor / (float) C.ARMOR_MAX), 5f,
                Draw.rgba(110, 170, 240));

        // 伤病状态点
        float sx = bx + bw + 16f;
        statusDot(sr, sx, by + 6f, me.bleeding, Draw.rgba(230, 70, 70), "血");
        statusDot(sr, sx + 34f, by + 6f, me.fractured, Draw.rgba(240, 170, 70), "折");
        statusDot(sr, sx + 68f, by + 6f, me.panicking(), Draw.rgba(220, 120, 220), "慌");
        statusDot(sr, sx + 102f, by + 6f, me.painT > 0f, Draw.rgba(120, 200, 240), "止");

        // 队友
        float ty = C.VIEW_H - 148f;
        int idx = 0;
        for (Unit u : world.units) {
            if (u.team != me.team || u == me) continue;
            float w = 82f;
            float x = 24f + idx * (w + 10f);
            Draw.roundRect(sr, x, ty - 6f, w, 26f, 5f, C_PANEL);
            Draw.roundRect(sr, x + 3f, ty - 2f, (w - 6f) * (u.hp / (float) C.HP_MAX), 8f, 4f,
                    u.alive ? Draw.rgba(96, 200, 150) : Draw.rgba(90, 96, 106));
            idx++;
        }
    }

    private void statusDot(ShapeRenderer sr, float x, float y, boolean on, Color c, String label) {
        Draw.circle(sr, x, y, 11f, on ? c : Draw.rgba(44, 50, 62));
        Draw.ring(sr, x, y, 11f, 1.5f, Draw.rgba(20, 24, 32));
    }

    public void drawText(SpriteBatch batch, Unit me, World world) {
        BitmapFont f16 = Fonts.of(16);
        BitmapFont f20 = Fonts.of(20);
        BitmapFont f14 = Fonts.of(14);
        Draw.text(batch, f20, me.name, 30f, C.VIEW_H - 22f, C_TEXT);
        Draw.text(batch, f16, me.hp + " HP", 300f, C.VIEW_H - 48f, C_TEXT);
        Draw.text(batch, f14, "护甲 " + me.armor, 300f, C.VIEW_H - 70f, C_DIM);

        // 队友血条上的名字
        int idx = 0;
        for (Unit u : world.units) {
            if (u.team != me.team || u == me) continue;
            float w = 82f;
            float x = 24f + idx * (w + 10f);
            Draw.text(batch, Fonts.of(12), u.name, x + 4f, C.VIEW_H - 116f,
                    u.alive ? C_TEXT : C_DIM);
            idx++;
        }

        // 击杀提示
        float ky = C.VIEW_H - 170f;
        for (int i = 0; i < killFeed.size(); i++) {
            float a = Math.min(1f, killAge.get(i) / 1.2f);
            Draw.textCentered(batch, f16, killFeed.get(i), C.VIEW_W * 0.5f, ky - i * 24f,
                    new Color(1f, 0.92f, 0.85f, a));
        }
    }

    /** 右下：弹药。 */
    public void drawAmmoBox(ShapeRenderer sr, Unit me) {
        float w = 250f, h = 86f;
        float x = C.VIEW_W - w - 18f, y = 18f;
        Draw.panel(sr, x, y, w, h, 0.72f);
        float ratio = me.ammo / (float) Math.max(1, me.ammoMax);
        Draw.rect(sr, x + 14f, y + 16f, w - 28f, 6f, Draw.rgba(36, 42, 56, 1f));
        Draw.rect(sr, x + 14f, y + 16f, (w - 28f) * ratio, 6f,
                ratio > 0.3f ? Draw.rgba(120, 200, 240) : Draw.rgba(240, 120, 90));
    }

    public void drawAmmoText(SpriteBatch batch, Unit me) {
        float w = 250f, x = C.VIEW_W - w - 18f, y = 18f;
        Draw.textRight(batch, Fonts.of(34), String.valueOf(me.ammo), x + w - 16f, y + 62f,
                C_TEXT);
        Draw.text(batch, Fonts.of(16), "/ " + me.ammoMax, x + 14f, y + 40f, C_DIM);
        Draw.text(batch, Fonts.of(16), me.weapon().name, x + 14f, y + 66f, C_TEXT);
        if (me.reloading) {
            Draw.textRight(batch, Fonts.of(16), "换弹中", x + w - 16f, y + 30f,
                    Draw.rgba(240, 200, 120));
        }
    }

    /** 左侧投掷物栏（枪械栏左边）。 */
    public void drawGrenades(ShapeRenderer sr, Unit me) {
        float x = 18f, y = 200f;
        for (int i = 0; i < C.THROW_MAX; i++) {
            float by = y + i * 52f;
            Draw.roundRect(sr, x, by, 46f, 46f, 8f, Draw.rgba(14, 18, 26, 0.7f));
            Draw.roundBorder(sr, x, by, 46f, 46f, 8f, 1.5f, Draw.rgba(72, 90, 118, 0.8f));
        }
    }

    public void drawGrenadeIcons(SpriteBatch batch, Unit me) {
        float x = 18f, y = 200f;
        for (int i = 0; i < Math.min(C.THROW_MAX, me.throwables.size()); i++) {
            String id = me.throwables.get(i);
            Draw.icon(batch, Art.throwable(id), x + 5f, y + i * 52f + 5f, 36f, 36f);
        }
    }

    /** 药品栏（投掷物下方）。 */
    public void drawMeds(ShapeRenderer sr, Unit me) {
        float x = 18f, y = 60f;
        for (int i = 0; i < C.MED_MAX; i++) {
            float by = y + i * 18f;
            Draw.roundRect(sr, x, by, 150f, 16f, 4f, Draw.rgba(14, 18, 26, 0.66f));
        }
    }

    public void drawMedIcons(SpriteBatch batch, Unit me) {
        float x = 22f, y = 62f;
        for (int i = 0; i < Math.min(C.MED_MAX, me.meds.size()); i++) {
            String id = me.meds.get(i);
            Draw.icon(batch, Art.med(id), x + i * 19f, y, 15f, 15f);
        }
    }

    /** 恐慌时画面四周压暗并向内收，模拟视野模糊。 */
    public static void blurVignette(ShapeRenderer sr) {
        float w = C.VIEW_W, h = C.VIEW_H;
        for (int i = 0; i < 6; i++) {
            float a = 0.06f + i * 0.03f;
            float t = 14f + i * 12f;
            Draw.rect(sr, 0f, 0f, w, t, Draw.rgba(30, 10, 20, a));
            Draw.rect(sr, 0f, h - t, w, t, Draw.rgba(30, 10, 20, a));
            Draw.rect(sr, 0f, 0f, t, h, Draw.rgba(30, 10, 20, a));
            Draw.rect(sr, w - t, 0f, t, h, Draw.rgba(30, 10, 20, a));
        }
    }
}
