package com.neuroshoot.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/** 圆角按钮：图形与文字分两批画，避免 ShapeRenderer 与 SpriteBatch 嵌套。 */
public final class Button {
    public final Rectangle rect = new Rectangle();
    public String label;
    public boolean enabled = true;
    public boolean toggled;
    public int fontSize = 24;

    public Button(String label, float x, float y, float w, float h) {
        this.label = label;
        rect.set(x, y, w, h);
    }

    public boolean hit(float x, float y) {
        return enabled && rect.contains(x, y);
    }

    public void drawShape(ShapeRenderer sr, boolean hover) {
        Color fill = !enabled ? Draw.rgba(34, 38, 50)
                : toggled ? Draw.rgba(46, 104, 168)
                : hover ? Draw.rgba(48, 84, 132)
                : Draw.rgba(26, 32, 46);
        Draw.roundRect(sr, rect.x, rect.y, rect.width, rect.height, 10f, fill);
        Draw.roundBorder(sr, rect.x, rect.y, rect.width, rect.height, 10f, 2f,
                !enabled ? Draw.rgba(52, 56, 68)
                        : hover || toggled ? Draw.rgba(126, 186, 246)
                        : Draw.rgba(72, 94, 128));
    }

    public void drawLabel(SpriteBatch batch) {
        BitmapFont f = Fonts.of(fontSize);
        Draw.textCentered(batch, f, label, rect.x + rect.width / 2f,
                rect.y + rect.height / 2f + f.getCapHeight() / 2f,
                enabled ? Color.WHITE : Draw.rgba(120, 124, 136));
    }
}
