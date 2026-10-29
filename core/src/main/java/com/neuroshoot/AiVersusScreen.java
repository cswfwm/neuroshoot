package com.neuroshoot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.neuroshoot.ai.Brain;
import com.neuroshoot.loadout.Loadout;
import com.neuroshoot.ui.Button;
import com.neuroshoot.ui.Draw;
import com.neuroshoot.ui.Fonts;
import com.neuroshoot.ui.WorldView;
import com.neuroshoot.world.C;
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.World;

/**
 * AI 对练：4 名 AI 队友对 4 名 AI 敌人，纯观战。
 *
 * 摄像机自动跟拍交战最激烈的位置，便于观察两套策略的差异。
 */
public final class AiVersusScreen extends ScreenAdapter {
    private final GameRoot game;
    private final ShapeRenderer sr = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final Matrix4 proj = new Matrix4();
    private final Matrix4 worldProj = new Matrix4();
    private final World world;
    private final WorldView.Camera cam = new WorldView.Camera();
    private final Button backBtn;
    private float zoom = 1f;

    public AiVersusScreen(GameRoot game) {
        this.game = game;
        game.session.newWorld();
        world = game.session.world;
        String[] pool = {"rifle", "sniper", "smg", "shotgun"};
        for (int i = 0; i < 4; i++) {
            Unit u = new Unit();
            u.id = i;
            u.team = 0;
            u.ai = true;
            u.name = "蓝队" + (i + 1);
            world.addUnit(u, Loadout.defaults(pool[i]), i);
            u.brain = Brain.scripted();
        }
        for (int i = 0; i < 4; i++) {
            Unit u = new Unit();
            u.id = 4 + i;
            u.team = 1;
            u.ai = true;
            u.name = "红队" + (i + 1);
            world.addUnit(u, Loadout.defaults(pool[(i + 2) % 4]), 3 + i);
            u.brain = Brain.scripted();
        }
        backBtn = new Button("返回", 20f, C.VIEW_H - 58f, 92f, 42f);
        cam.w = C.VIEW_W;
        cam.h = C.VIEW_H;
    }

    @Override
    public void render(float delta) {
        for (Unit u : world.units) if (u.brain != null && u.alive) u.brain.update(u, world, delta);
        world.step(delta);
        followAction(delta);

        Gdx.gl.glClearColor(0.05f, 0.07f, 0.09f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        worldProj.setToOrtho2D(cam.x, cam.y, cam.w, cam.h);
        sr.setProjectionMatrix(worldProj);
        batch.setProjectionMatrix(worldProj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        WorldView.drawGround(sr, world, cam);
        WorldView.drawBlood(sr, world);
        sr.end();
        batch.begin();
        WorldView.drawTiles(batch, world, cam);
        WorldView.drawCorpses(batch, world);
        WorldView.drawThrowables(batch, world);
        WorldView.drawUnits(batch, world, cam, u -> true, null, 1f);
        batch.end();

        proj.setToOrtho2D(0f, 0f, C.VIEW_W, C.VIEW_H);
        sr.setProjectionMatrix(proj);
        batch.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        Draw.panel(sr, 16f, C.VIEW_H - 116f, 300f, 96f, 0.72f);
        backBtn.drawShape(sr, false);
        sr.end();
        batch.begin();
        Draw.text(batch, Fonts.of(20), "AI 对练　蓝队 vs 红队", 30f, C.VIEW_H - 32f, Color.WHITE);
        Draw.text(batch, Fonts.of(18), "蓝队存活 " + world.aliveCount(0)
                + "　红队存活 " + world.aliveCount(1), 30f, C.VIEW_H - 60f,
                Draw.rgba(170, 200, 230));
        Draw.text(batch, Fonts.of(16), "击杀　蓝 " + kills(0) + " : " + kills(1) + " 红",
                30f, C.VIEW_H - 86f, Draw.rgba(150, 170, 195));
        backBtn.label = "返回";
        backBtn.drawLabel(batch);
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.setScreen(new MenuScreen(game));
            return;
        }
        if (Gdx.input.justTouched()) {
            float mx = Gdx.input.getX() * (C.VIEW_W / Gdx.graphics.getWidth());
            float my = C.VIEW_H - Gdx.input.getY() * (C.VIEW_H / Gdx.graphics.getHeight());
            if (backBtn.hit(mx, my)) game.setScreen(new MenuScreen(game));
        }
    }

    private int kills(int team) {
        int n = 0;
        for (Unit u : world.units) if (u.team == team) n += u.kills;
        return n;
    }

    private void followAction(float delta) {
        float sx = 0f, sy = 0f;
        int n = 0;
        for (Unit u : world.units) {
            if (!u.alive) continue;
            sx += u.x;
            sy += u.y;
            n++;
        }
        if (n == 0) return;
        float tx = sx / n - cam.w / 2f;
        float ty = sy / n - cam.h / 2f;
        cam.x += (tx - cam.x) * Math.min(1f, delta * 2.2f);
        cam.y += (ty - cam.y) * Math.min(1f, delta * 2.2f);
        cam.x = Math.max(0f, Math.min(C.WORLD_W - cam.w, cam.x));
        cam.y = Math.max(0f, Math.min(C.WORLD_H - cam.h, cam.y));
    }

    @Override
    public void dispose() {
        sr.dispose();
        batch.dispose();
    }
}
