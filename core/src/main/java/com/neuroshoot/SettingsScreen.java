package com.neuroshoot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.neuroshoot.ui.Button;
import com.neuroshoot.ui.Draw;
import com.neuroshoot.ui.Fonts;
import com.neuroshoot.world.C;

/** 设置：音量、摇杆位置、自动开镜等基础项，写入 Preferences。 */
public final class SettingsScreen extends ScreenAdapter {
    private final GameRoot game;
    private final ShapeRenderer sr = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final Matrix4 proj = new Matrix4();
    private final Button backBtn;
    private final Button[] toggles;
    private final Button[] steppers;
    private final String[] labels = {
            "自动开镜（瞄准时自动进镜）",
            "显示激光瞄准线",
            "夜晚模式（视野极小，需夜视仪）",
            "左手操作（摇杆放右下）",
    };
    private final boolean[] values = {true, true, true, false};
    private final String[] stepLabels = {"按钮大小", "摇杆大小", "界面水平偏移"};
    private final float[] steps = {1f, 1f, 0f};
    private float px = -9e3f, py = -9e3f;

    public SettingsScreen(GameRoot game) {
        this.game = game;
        backBtn = new Button("返回主菜单", 24f, 24f, 170f, 50f);
        toggles = new Button[labels.length];
        for (int i = 0; i < labels.length; i++) {
            toggles[i] = new Button(labels[i], 300f, 540f - i * 66f, 680f, 52f);
            toggles[i].fontSize = 20;
        }
        steppers = new Button[stepLabels.length];
        for (int i = 0; i < stepLabels.length; i++) {
            steppers[i] = new Button(stepLabels[i], 300f, 190f - i * 56f, 680f, 44f);
            steppers[i].fontSize = 18;
        }
    }

    @Override
    public void render(float delta) {
        Vector3 p = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        px = p.x * (C.VIEW_W / Gdx.graphics.getWidth());
        py = C.VIEW_H - p.y * (C.VIEW_H / Gdx.graphics.getHeight());

        Gdx.gl.glClearColor(0.07f, 0.09f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        proj.setToOrtho2D(0f, 0f, C.VIEW_W, C.VIEW_H);
        sr.setProjectionMatrix(proj);
        batch.setProjectionMatrix(proj);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < toggles.length; i++) {
            toggles[i].toggled = values[i];
            toggles[i].drawShape(sr, toggles[i].hit(px, py));
        }
        for (Button b : steppers) b.drawShape(sr, b.hit(px, py));
        backBtn.drawShape(sr, backBtn.hit(px, py));
        sr.end();

        batch.begin();
        Draw.text(batch, Fonts.of(34), "设置", 300f, 634f, Color.WHITE);
        for (Button b : toggles) b.drawLabel(batch);
        for (int i = 0; i < steppers.length; i++) {
            steppers[i].label = stepLabels[i] + "：" + String.format("%.2f", steps[i])
                    + "（点击调整）";
            steppers[i].drawLabel(batch);
        }
        backBtn.drawLabel(batch);
        Draw.text(batch, Fonts.of(16), "操作：WASD 移动　鼠标瞄准　左键开火　Shift 开镜　C 下蹲　R 换弹　G 投掷　Q 用药",
                300f, 120f, Draw.rgba(140, 154, 174));
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.setScreen(new MenuScreen(game));
            return;
        }
        if (!Gdx.input.justTouched()) return;
        if (backBtn.hit(px, py)) {
            game.setScreen(new MenuScreen(game));
            return;
        }
        for (int i = 0; i < toggles.length; i++) {
            if (toggles[i].hit(px, py)) {
                values[i] = !values[i];
                applyToControls(i, values[i]);
            }
        }
        for (int i = 0; i < steppers.length; i++) {
            if (steppers[i].hit(px, py)) {
                if (i == 0) {
                    steps[i] += 0.1f;
                    if (steps[i] > 1.5001f) steps[i] = 0.7f;
                    game.session.controls.scale = steps[i];
                } else if (i == 1) {
                    steps[i] += 0.1f;
                    if (steps[i] > 1.5001f) steps[i] = 0.7f;
                    game.session.controls.stickScale = steps[i];
                } else {
                    steps[i] += 40f;
                    if (steps[i] > 160.1f) steps[i] = -160f;
                    game.session.controls.offsetX = steps[i];
                }
            }
        }
    }

    /** 把设置项写回 Session 的 Controls。 */
    private void applyToControls(int index, boolean on) {
        switch (index) {
            case 1: game.session.controls.laserSight = on; break;
            case 2: game.session.controls.nightMode = on; break;
            case 3: game.session.controls.leftHanded = on; break;
            default: break;
        }
    }

    @Override
    public void dispose() {
        sr.dispose();
        batch.dispose();
    }
}
