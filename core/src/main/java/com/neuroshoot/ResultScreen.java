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
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.World;

import java.util.ArrayList;
import java.util.List;

/** 结算：分队伍列出每人的击杀、伤害、死亡，按击杀降序。 */
public final class ResultScreen extends ScreenAdapter {
    private final GameRoot game;
    private final ShapeRenderer sr = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final Matrix4 proj = new Matrix4();
    private final Button againBtn;
    private final Button menuBtn;
    private final List<Unit> blue = new ArrayList<>();
    private final List<Unit> red = new ArrayList<>();
    private final boolean blueWin;
    private float px = -9e3f, py = -9e3f;

    public ResultScreen(GameRoot game, World world) {
        this.game = game;
        for (Unit u : world.units) {
            if (u.team == 0) blue.add(u);
            else red.add(u);
        }
        blue.sort((a, b) -> Integer.compare(b.kills, a.kills));
        red.sort((a, b) -> Integer.compare(b.kills, a.kills));
        int bk = 0, rk = 0;
        for (Unit u : blue) bk += u.kills;
        for (Unit u : red) rk += u.kills;
        blueWin = bk >= rk;
        againBtn = new Button("再来一局", C.VIEW_W / 2f - 210f, 44f, 190f, 56f);
        menuBtn = new Button("返回主菜单", C.VIEW_W / 2f + 20f, 44f, 190f, 56f);
    }

    @Override
    public void render(float delta) {
        Vector3 p = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        px = p.x * (C.VIEW_W / Gdx.graphics.getWidth());
        py = C.VIEW_H - p.y * (C.VIEW_H / Gdx.graphics.getHeight());

        Gdx.gl.glClearColor(0.06f, 0.08f, 0.11f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        proj.setToOrtho2D(0f, 0f, C.VIEW_W, C.VIEW_H);
        sr.setProjectionMatrix(proj);
        batch.setProjectionMatrix(proj);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        Draw.panel(sr, 60f, 130f, 520f, 480f, 0.8f);
        Draw.panel(sr, C.VIEW_W - 580f, 130f, 520f, 480f, 0.8f);
        againBtn.drawShape(sr, againBtn.hit(px, py));
        menuBtn.drawShape(sr, menuBtn.hit(px, py));
        sr.end();

        batch.begin();
        Draw.textCentered(batch, Fonts.of(44), blueWin ? "战斗胜利" : "战斗失败",
                C.VIEW_W / 2f, 660f,
                blueWin ? Draw.rgba(120, 220, 160) : Draw.rgba(230, 110, 110));
        Draw.text(batch, Fonts.of(24), "蓝队", 90f, 580f, Draw.rgba(120, 200, 255));
        Draw.text(batch, Fonts.of(24), "红队", C.VIEW_W - 550f, 580f,
                Draw.rgba(240, 120, 120));
        drawTeam(batch, blue, 90f, 90f);
        drawTeam(batch, red, C.VIEW_W - 550f, 90f);
        againBtn.drawLabel(batch);
        menuBtn.drawLabel(batch);
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.setScreen(new MenuScreen(game));
            return;
        }
        if (!Gdx.input.justTouched()) return;
        if (againBtn.hit(px, py)) game.setScreen(new LoadoutScreen(game, MenuScreen.Mode.BOT));
        else if (menuBtn.hit(px, py)) game.setScreen(new MenuScreen(game));
    }

    private void drawTeam(SpriteBatch batch, List<Unit> team, float x, float top) {
        float y = top + 400f;
        Draw.text(batch, Fonts.of(15), "名字", x + 12f, y, Draw.rgba(150, 168, 190));
        Draw.text(batch, Fonts.of(15), "击杀", x + 210f, y, Draw.rgba(150, 168, 190));
        Draw.text(batch, Fonts.of(15), "伤害", x + 290f, y, Draw.rgba(150, 168, 190));
        Draw.text(batch, Fonts.of(15), "阵亡", x + 380f, y, Draw.rgba(150, 168, 190));
        y -= 8f;
        for (Unit u : team) {
            y -= 46f;
            Draw.text(batch, Fonts.of(20), u.name, x + 12f, y, Color.WHITE);
            Draw.text(batch, Fonts.of(20), String.valueOf(u.kills), x + 218f, y,
                    Draw.rgba(240, 210, 120));
            Draw.text(batch, Fonts.of(20), String.valueOf((int) u.damageDealt), x + 290f, y,
                    Draw.rgba(200, 214, 230));
            Draw.text(batch, Fonts.of(20), String.valueOf(u.deaths), x + 388f, y,
                    Draw.rgba(200, 140, 140));
        }
    }

    @Override
    public void dispose() {
        sr.dispose();
        batch.dispose();
    }
}
