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
import com.neuroshoot.ai.Trainer;
import com.neuroshoot.store.ModelRecord;
import com.neuroshoot.ui.Button;
import com.neuroshoot.ui.Chart;
import com.neuroshoot.ui.Draw;
import com.neuroshoot.ui.Fonts;
import com.neuroshoot.ui.WorldView;
import com.neuroshoot.world.C;
import com.neuroshoot.world.Unit;

/**
 * 神经训练界面：按倍速推进混战，实时显示代数、适应度曲线与击杀统计，
 * 可随时把当前最佳个体与整池族群存档，供对战时选用。
 */
public final class TrainScreen extends ScreenAdapter {
    private final GameRoot game;
    /** 单帧最多推进多少个子步（1/30 秒一个步），100 倍速下每帧约 50 步。 */
    private static final int MAX_STEPS_PER_FRAME = 400;
    /** 可选倍率档位。 */
    private static final int[] SPEEDS = {1, 2, 4, 8, 16, 32, 64, 100};

    private static final Color[] BIAS_COLORS = {
            Draw.rgba(240, 110, 100), Draw.rgba(120, 210, 150),
            Draw.rgba(130, 180, 250), Draw.rgba(240, 200, 110),
    };

    private final ShapeRenderer sr = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final Matrix4 proj = new Matrix4();
    private final Matrix4 worldProj = new Matrix4();
    private final WorldView.Camera cam = new WorldView.Camera();

    private final Trainer trainer;
    private final Button backBtn;
    private final Button pauseBtn;
    private final Button speedBtn;
    private final Button saveBtn;

    private float px = -9e3f, py = -9e3f;
    private String toast = "";
    private float toastT;
    private float acc;
    /** 0 全图俯视，>0 跟随对应下标的人物。 */
    private int followIndex = -1;
    private boolean wholeMap = true;
    private final com.badlogic.gdx.math.Rectangle viewBtnRect =
            new com.badlogic.gdx.math.Rectangle(C.VIEW_W - 300f, C.VIEW_H - 58f, 132f, 42f);
    private final com.badlogic.gdx.math.Rectangle followBtnRect =
            new com.badlogic.gdx.math.Rectangle(C.VIEW_W - 458f, C.VIEW_H - 58f, 144f, 42f);

    public TrainScreen(GameRoot game) {
        this.game = game;
        this.trainer = game.session.trainer();
        backBtn = new Button("返回", 20f, C.VIEW_H - 58f, 92f, 42f);
        pauseBtn = new Button("开始", 120f, C.VIEW_H - 58f, 92f, 42f);
        speedBtn = new Button("3×", 220f, C.VIEW_H - 58f, 74f, 42f);
        saveBtn = new Button("保存模型", C.VIEW_W - 150f, C.VIEW_H - 58f, 132f, 42f);
        cam.w = C.VIEW_W * 0.72f;
        cam.h = C.VIEW_H * 0.72f;
    }

    @Override
    public void render(float delta) {
        pointer();
        // 按倍率换算本帧要推进的仿真时长：倍率越高，一帧里跑的子步越多。
        // 上限 SPEED_CAP 防止掉帧时越积越多（掉帧雪崩）。
        // 按倍率换算本帧要推进的仿真时长。上限 MAX_STEPS_PER_FRAME 防止掉帧雪崩：
        // 一旦某帧超过预算，就丢弃积压而不是继续追，宁可慢一点也不要卡死。
        float real = Math.min(0.05f, delta);
        acc += real * trainer.speed;
        int steps = 0;
        while (acc >= Trainer.DT && steps < MAX_STEPS_PER_FRAME) {
            trainer.tick(Trainer.DT);
            acc -= Trainer.DT;
            steps++;
        }
        if (acc > Trainer.DT * 2f) acc = 0f;
        updateCamera(delta);

        Gdx.gl.glClearColor(0.05f, 0.07f, 0.09f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        worldProj.setToOrtho2D(cam.x, cam.y, cam.w, cam.h);
        sr.setProjectionMatrix(worldProj);
        batch.setProjectionMatrix(worldProj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        WorldView.drawGround(sr, trainer.world, cam);
        WorldView.drawBlood(sr, trainer.world);
        sr.end();
        batch.begin();
        WorldView.drawTiles(batch, trainer.world, cam);
        WorldView.drawCorpses(batch, trainer.world);
        WorldView.drawUnits(batch, trainer.world, cam, u -> true, null, 1f);
        batch.end();

        proj.setToOrtho2D(0f, 0f, C.VIEW_W, C.VIEW_H);
        sr.setProjectionMatrix(proj);
        batch.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        Draw.panel(sr, 16f, C.VIEW_H - 250f, 340f, 180f, 0.75f);
        Draw.rect(sr, 32f, C.VIEW_H - 940f + 800f, 0f, 0f, Color.CLEAR);
        int cl = Math.min(trainer.curveLen, trainer.curves[0].length);
        for (int i = 0; i < trainer.curves.length; i++) {
            Chart.drawColored(sr, trainer.curves[i], cl, 32f, 120f, 300f, 100f,
                    BIAS_COLORS[i], i == 0);
        }
        backBtn.drawShape(sr, backBtn.hit(px, py));
        pauseBtn.toggled = !trainer.paused;
        pauseBtn.drawShape(sr, pauseBtn.hit(px, py));
        speedBtn.drawShape(sr, speedBtn.hit(px, py));
        saveBtn.drawShape(sr, saveBtn.hit(px, py));
        Draw.roundRect(sr, followBtnRect.x, followBtnRect.y, followBtnRect.width,
                followBtnRect.height, 8f,
                wholeMap ? Draw.rgba(26, 32, 46) : Draw.rgba(46, 104, 168));
        Draw.roundRect(sr, viewBtnRect.x, viewBtnRect.y, viewBtnRect.width,
                viewBtnRect.height, 8f,
                wholeMap ? Draw.rgba(46, 104, 168) : Draw.rgba(26, 32, 46));
        sr.end();

        batch.begin();
        Draw.text(batch, Fonts.of(22), "神经训练（击杀/生存/躲避/跑图）", 20f, C.VIEW_H - 22f, Color.WHITE);
        Draw.text(batch, Fonts.of(18), "四族群混战　第 " + trainer.generation() + " 代",
                32f, C.VIEW_H - 226f, Color.WHITE);
        for (int i = 0; i < trainer.species.length; i++) {
            com.neuroshoot.ai.Species sp = trainer.species[i];
            float y = C.VIEW_H - 208f + i * 24f;
            Draw.text(batch, Fonts.of(16), sp.name + " " + sp.bias.desc, 32f, y,
                    BIAS_COLORS[i]);
            Draw.text(batch, Fonts.of(16), "代" + sp.generation + " 最佳"
                    + String.format("%.0f", sp.bestFitness), 210f, y,
                    Draw.rgba(190, 200, 216));
        }
        Draw.text(batch, Fonts.of(15), "对局 " + trainer.totalRounds + "　筛选 "
                + trainer.screenCount + " 次", 32f, C.VIEW_H - 108f,
                Draw.rgba(150, 166, 190));
        Draw.text(batch, Fonts.of(17), "本代击杀 " + trainer.genKills + "　命中 "
                + trainer.genHits, 32f, C.VIEW_H - 84f, Draw.rgba(240, 200, 120));
        Draw.text(batch, Fonts.of(14), "上代击杀 " + trainer.lastGenKills,
                220f, C.VIEW_H - 84f, Draw.rgba(160, 172, 192));
        if (!trainer.lastCulled.isEmpty()) {
            Draw.text(batch, Fonts.of(15), "淘汰重组：" + trainer.lastCulled,
                    32f, C.VIEW_H - 88f, Draw.rgba(240, 140, 120));
        }
        if (!trainer.lastChampion.isEmpty()) {
            Draw.text(batch, Fonts.of(15), "当前冠军：" + trainer.lastChampion,
                    32f, C.VIEW_H - 68f, Draw.rgba(140, 220, 170));
        }
        Draw.text(batch, Fonts.of(15), "适应度曲线（近 64 代）", 32f, 238f,
                Draw.rgba(150, 168, 190));

        // 击杀提示：滚动显示最近几次击杀
        for (int i = 0; i < trainer.killFeed.size(); i++) {
            float age = trainer.killAges.get(i);
            float a = Math.min(1f, age / 1.0f);
            Draw.textCentered(batch, Fonts.of(18), trainer.killFeed.get(i),
                    C.VIEW_W * 0.5f, C.VIEW_H - 60f - i * 26f,
                    new Color(1f, 0.94f, 0.86f, a));
        }
        backBtn.drawLabel(batch);
        pauseBtn.label = trainer.paused ? "开始" : "暂停";
        pauseBtn.drawLabel(batch);
        speedBtn.label = trainer.speed + "×";
        speedBtn.drawLabel(batch);
        saveBtn.drawLabel(batch);
        Draw.textCentered(batch, Fonts.of(18), "全图俯视",
                viewBtnRect.x + viewBtnRect.width / 2f,
                viewBtnRect.y + viewBtnRect.height / 2f - 6f, Color.WHITE);
        Draw.textCentered(batch, Fonts.of(18),
                wholeMap ? "跟随某人" : "跟随 " + followName(),
                followBtnRect.x + followBtnRect.width / 2f,
                followBtnRect.y + followBtnRect.height / 2f - 6f, Color.WHITE);
        if (toastT > 0f) {
            Draw.textCentered(batch, Fonts.of(20), toast, C.VIEW_W / 2f, 90f,
                    Draw.rgba(140, 240, 170));
            toastT -= delta;
        }
        batch.end();

        handle();
    }

    private void updateCamera(float delta) {
        if (wholeMap) {
            float zx = C.WORLD_W / C.VIEW_W;
            float zy = C.WORLD_H / C.VIEW_H;
            float scale = Math.max(zx, zy);
            cam.w = C.VIEW_W * scale;
            cam.h = C.VIEW_H * scale;
            cam.x = (C.WORLD_W - cam.w) / 2f;
            cam.y = (C.WORLD_H - cam.h) / 2f;
            return;
        }
        int idx = Math.max(0, Math.min(followIndex, trainer.fighters.size() - 1));
        Unit target = trainer.fighters.get(idx);
        cam.w = C.VIEW_W * 0.62f;
        cam.h = C.VIEW_H * 0.62f;
        float tx = target.rx - cam.w / 2f;
        float ty = target.ry - cam.h / 2f;
        cam.x += (tx - cam.x) * Math.min(1f, delta * 8f);
        cam.y += (ty - cam.y) * Math.min(1f, delta * 8f);
        cam.x = Math.max(0f, Math.min(C.WORLD_W - cam.w, cam.x));
        cam.y = Math.max(0f, Math.min(C.WORLD_H - cam.h, cam.y));
    }

    private void followCamera(float delta) {
        float sx = 0f, sy = 0f;
        int n = 0;
        for (Unit u : trainer.fighters) {
            if (!u.alive) continue;
            sx += u.x;
            sy += u.y;
            n++;
        }
        if (n == 0) return;
        float tx = sx / n - cam.w / 2f;
        float ty = sy / n - cam.h / 2f;
        cam.x += (tx - cam.x) * Math.min(1f, delta * 1.8f);
        cam.y += (ty - cam.y) * Math.min(1f, delta * 1.8f);
        cam.x = Math.max(0f, Math.min(C.WORLD_W - cam.w, cam.x));
        cam.y = Math.max(0f, Math.min(C.WORLD_H - cam.h, cam.y));
    }

    private void handle() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.setScreen(new MenuScreen(game));
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) trainer.paused = !trainer.paused;
        if (!Gdx.input.justTouched()) return;
        if (backBtn.hit(px, py)) {
            game.setScreen(new MenuScreen(game));
        } else if (pauseBtn.hit(px, py)) {
            trainer.paused = !trainer.paused;
        } else if (speedBtn.hit(px, py)) {
            trainer.speed = nextSpeed(trainer.speed);
        } else if (saveBtn.hit(px, py)) {
            save();
        } else if (viewBtnRect.contains(px, py)) {
            wholeMap = true;
            followIndex = -1;
        } else if (followBtnRect.contains(px, py)) {
            wholeMap = false;
            followIndex = (followIndex + 1) % Math.max(1, trainer.fighters.size());
        }
    }

    /** 保存四个族群各自的最佳个体，并把整池基因存成族群存档供下次续训。 */
    private void save() {
        int saved = 0;
        for (int i = 0; i < trainer.species.length; i++) {
            com.neuroshoot.ai.Species sp = trainer.species[i];
            String name = sp.name + "-G" + sp.generation;
            ModelRecord r = ModelRecord.of(name, sp.name, sp.generation, sp.bestFitness,
                    sp.bias.desc, sp.bestGenes());
            if (game.store.saveOrReplace(r)) saved++;
        }
        // 族群存档只保留综合最佳的那个族群，避免键值膨胀
        int champion = 0;
        for (int i = 1; i < trainer.species.length; i++) {
            if (trainer.species[i].bestFitness > trainer.species[champion].bestFitness) {
                champion = i;
            }
        }
        com.neuroshoot.ai.Species best = trainer.species[champion];
        ModelRecord[] pool = new ModelRecord[best.genes.length];
        for (int i = 0; i < pool.length; i++) {
            pool[i] = ModelRecord.of(best.name + "-" + i, best.name, best.generation,
                    best.fitness[i], "", best.genes[i]);
        }
        game.store.saveSpecies(best.name, best.generation, best.bestFitness, pool);
        toast = "已保存 " + saved + " 个族群模型";
        toastT = 2.2f;
    }

    /** 按档位表循环取下一个倍率。 */
    private static int nextSpeed(int cur) {
        for (int i = 0; i < SPEEDS.length; i++) {
            if (SPEEDS[i] > cur) return SPEEDS[i];
        }
        return SPEEDS[0];
    }

    private String followName() {
        if (followIndex < 0 || followIndex >= trainer.fighters.size()) return "某人";
        return trainer.fighters.get(followIndex).name;
    }

    private void pointer() {
        Vector3 p = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        px = p.x * (C.VIEW_W / Gdx.graphics.getWidth());
        py = C.VIEW_H - p.y * (C.VIEW_H / Gdx.graphics.getHeight());
    }

    @Override
    public void dispose() {
        sr.dispose();
        batch.dispose();
    }
}
