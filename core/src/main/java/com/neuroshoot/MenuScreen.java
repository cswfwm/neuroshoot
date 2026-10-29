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

/** 开始界面：五个入口。 */
public final class MenuScreen extends ScreenAdapter {
    private final GameRoot game;
    private final ShapeRenderer sr = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final Matrix4 proj = new Matrix4();

    private final Button trainBtn;
    private final Button botBtn;
    private final Button aiBtn;
    private final Button lanBtn;
    private final Button settingsBtn;

    private float px = -9e3f, py = -9e3f;

    /** 更新检查状态：null 未检查，checking 检查中，否则保存结果。 */
    private volatile boolean checking;
    private volatile com.neuroshoot.net.UpdateChecker.Result updateResult;
    private volatile String updateError = "";

    public MenuScreen(GameRoot game) {
        this.game = game;
        float cx = C.VIEW_W * 0.5f;
        float w = 520f;
        trainBtn = new Button("神经训练", cx - w / 2f, 452f, w, 66f);
        botBtn = new Button("人机对练", cx - w / 2f, 368f, w, 66f);
        aiBtn = new Button("AI 对练", cx - w / 2f, 284f, w, 66f);
        lanBtn = new Button("局域网联机", cx - w / 2f, 200f, w, 66f);
        settingsBtn = new Button("设置", cx - w / 2f, 92f, w, 66f);
    }

    /** 进菜单自动查一次更新，后台执行不阻塞画面。 */
    private void autoCheckUpdate() {
        if (checking || updateResult != null) return;
        checking = true;
        new Thread(() -> {
            try {
                updateResult = com.neuroshoot.net.UpdateChecker.check();
            } catch (Exception e) {
                updateError = "检查失败";
            } finally {
                checking = false;
            }
        }, "update-check").start();
    }

    private void manualCheckUpdate() {
        if (checking) return;
        updateResult = null;
        updateError = "";
        autoCheckUpdate();
    }

    @Override
    public void show() {
        autoCheckUpdate();
    }

    @Override
    public void render(float delta) {
        pointer();
        Gdx.gl.glClearColor(0.07f, 0.09f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        proj.setToOrtho2D(0f, 0f, C.VIEW_W, C.VIEW_H);
        sr.setProjectionMatrix(proj);
        batch.setProjectionMatrix(proj);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < 8; i++) {
            float y = 40f + i * 88f;
            Draw.rect(sr, 0f, y, C.VIEW_W, 2f, Draw.rgba(26, 34, 46, 1f));
        }
        trainBtn.drawShape(sr, trainBtn.hit(px, py));
        botBtn.drawShape(sr, botBtn.hit(px, py));
        aiBtn.drawShape(sr, aiBtn.hit(px, py));
        lanBtn.drawShape(sr, lanBtn.hit(px, py));
        settingsBtn.drawShape(sr, settingsBtn.hit(px, py));
        sr.end();

        batch.begin();
        Draw.textCentered(batch, Fonts.of(64), "神经枪战", C.VIEW_W * 0.5f, 646f, Color.WHITE);
        Draw.textCentered(batch, Fonts.of(22), "俯视战术射击 · 战争迷雾 · 神经网络对战",
                C.VIEW_W * 0.5f, 596f, Draw.rgba(120, 176, 226, 1f));
        trainBtn.drawLabel(batch);
        botBtn.drawLabel(batch);
        aiBtn.drawLabel(batch);
        lanBtn.drawLabel(batch);
        settingsBtn.drawLabel(batch);
        Draw.textCentered(batch, Fonts.of(16),
                "五个入口：训练可进化的神经网络，人机对练带三名 AI 队友，AI 对练观战两族对战",
                C.VIEW_W * 0.5f, 62f, Draw.rgba(104, 116, 136, 1f));
        Draw.text(batch, Fonts.of(14), Version.display(), 16f, 22f,
                Draw.rgba(90, 100, 118, 1f));
        DrawUpdateBanner(batch);
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            Gdx.app.exit();
            return;
        }
        if (!Gdx.input.justTouched()) return;
        handleUpdateClick();
        if (trainBtn.hit(px, py)) game.setScreen(new TrainScreen(game));
        else if (botBtn.hit(px, py)) game.setScreen(new LoadoutScreen(game, Mode.BOT));
        else if (aiBtn.hit(px, py)) game.setScreen(new LoadoutScreen(game, Mode.AI_VS_AI));
        else if (lanBtn.hit(px, py)) game.setScreen(new LanScreen(game));
        else if (settingsBtn.hit(px, py)) game.setScreen(new SettingsScreen(game));
    }

    /** 版本状态提示：检查中 / 有新版本 / 已是最新。 */
    private void DrawUpdateBanner(SpriteBatch batch) {
        String text;
        Color color;
        if (checking) {
            text = "正在检查更新…";
            color = Draw.rgba(120, 140, 168, 1f);
        } else if (!updateError.isEmpty()) {
            text = updateError;
            color = Draw.rgba(180, 130, 110, 1f);
        } else if (updateResult != null && updateResult.hasUpdate) {
            text = "发现新版本 " + updateResult.latestTag + "，点击查看";
            color = Draw.rgba(120, 220, 160, 1f);
        } else if (updateResult != null) {
            text = "已是最新版本";
            color = Draw.rgba(110, 124, 144, 1f);
        } else {
            return;
        }
        Draw.textRight(batch, Fonts.of(14), text, C.VIEW_W - 16f, 22f, color);
    }

    /** 点右下角版本区可手动重新检查；有新版本时打开下载页。 */
    private void handleUpdateClick() {
        if (px < C.VIEW_W - 380f || py > 44f) return;
        if (updateResult != null && updateResult.hasUpdate) {
            try {
                Gdx.net.openURI(updateResult.apkUrl.isEmpty()
                        ? "https://github.com/" + Version.REPO + "/releases/latest"
                        : updateResult.apkUrl);
            } catch (Exception ignored) {
                // 打不开浏览器时保持安静
            }
        } else {
            manualCheckUpdate();
        }
    }

    private void pointer() {
        Vector3 p = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        px = p.x * (C.VIEW_W / Gdx.graphics.getWidth());
        py = C.VIEW_H - p.y * (C.VIEW_H / Gdx.graphics.getHeight());
    }

    public enum Mode { BOT, AI_VS_AI, LAN }

    @Override
    public void dispose() {
        sr.dispose();
        batch.dispose();
    }
}
