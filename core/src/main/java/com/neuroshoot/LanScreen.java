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
import com.neuroshoot.net.Discovery;
import com.neuroshoot.net.GameClient;
import com.neuroshoot.net.GameHost;
import com.neuroshoot.ui.Button;
import com.neuroshoot.ui.Draw;
import com.neuroshoot.ui.Fonts;
import com.neuroshoot.world.C;

import java.util.ArrayList;
import java.util.List;

/**
 * 局域网联机：开房间（主机）或搜索/手动输入 IP 加入。
 *
 * 广播在部分安卓 ROM 上会被限制，所以同时提供手填 IP 的备用路径。
 */
public final class LanScreen extends ScreenAdapter {
    private final GameRoot game;
    private final ShapeRenderer sr = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final Matrix4 proj = new Matrix4();

    private final Button backBtn;
    private final Button hostBtn;
    private final Button scanBtn;
    private final Button joinBtn;

    private final List<Discovery.Room> rooms = new ArrayList<>();
    private final List<float[]> roomRects = new ArrayList<>();
    private int selected = -1;
    private String manualIp = "";
    private boolean editingIp;
    private String status = "";
    private GameHost host;
    private GameClient client;
    private Discovery.Responder responder;
    private float px = -9e3f, py = -9e3f;

    public LanScreen(GameRoot game) {
        this.game = game;
        backBtn = new Button("返回", 24f, C.VIEW_H - 62f, 96f, 44f);
        hostBtn = new Button("创建房间", C.VIEW_W - 300f, C.VIEW_H - 62f, 130f, 44f);
        scanBtn = new Button("搜索房间", C.VIEW_W - 160f, C.VIEW_H - 62f, 130f, 44f);
        joinBtn = new Button("加入对战", C.VIEW_W - 170f, 30f, 150f, 50f);
    }

    @Override
    public void render(float delta) {
        pointer();
        handleKeys();

        Gdx.gl.glClearColor(0.07f, 0.09f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        proj.setToOrtho2D(0f, 0f, C.VIEW_W, C.VIEW_H);
        sr.setProjectionMatrix(proj);
        batch.setProjectionMatrix(proj);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        Draw.panel(sr, 40f, 120f, C.VIEW_W - 80f, 470f, 0.75f);
        Draw.panel(sr, 40f, 34f, 520f, 62f, 0.75f);
        roomRects.clear();
        for (int i = 0; i < rooms.size() && i < 6; i++) {
            float y = 520f - i * 62f;
            roomRects.add(new float[]{60f, y, C.VIEW_W - 120f, 52f});
            Draw.roundRect(sr, 60f, y, C.VIEW_W - 120f, 52f, 8f,
                    i == selected ? Draw.rgba(46, 104, 168) : Draw.rgba(20, 26, 38));
        }
        backBtn.drawShape(sr, backBtn.hit(px, py));
        hostBtn.drawShape(sr, hostBtn.hit(px, py));
        scanBtn.drawShape(sr, scanBtn.hit(px, py));
        joinBtn.drawShape(sr, joinBtn.hit(px, py));
        sr.end();

        batch.begin();
        Draw.text(batch, Fonts.of(28), "局域网联机", 140f, C.VIEW_H - 26f, Color.WHITE);
        if (rooms.isEmpty()) {
            Draw.textCentered(batch, Fonts.of(20),
                    "还没有搜到房间。点「创建房间」当主机，或「搜索房间」/下面手填 IP",
                    C.VIEW_W / 2f, 380f, Draw.rgba(140, 154, 174));
        }
        for (int i = 0; i < rooms.size() && i < 6; i++) {
            Draw.text(batch, Fonts.of(20), rooms.get(i).label(), 78f, 520f - i * 62f + 34f,
                    Color.WHITE);
        }
        Draw.text(batch, Fonts.of(16), "主机 IP：" , 58f, 62f, Draw.rgba(150, 168, 190));
        Draw.text(batch, Fonts.of(18), manualIp.isEmpty() ? "点这里输入" : manualIp,
                160f, 62f, manualIp.isEmpty() ? Draw.rgba(110, 124, 146) : Color.WHITE);
        backBtn.drawLabel(batch);
        hostBtn.drawLabel(batch);
        scanBtn.drawLabel(batch);
        joinBtn.drawLabel(batch);
        if (!status.isEmpty()) {
            Draw.text(batch, Fonts.of(16), status, 590f, 62f, Draw.rgba(140, 220, 170));
        }
        batch.end();

        handleTouch();
    }

    private void handleKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            cleanup();
            game.setScreen(new MenuScreen(game));
            return;
        }
        if (!editingIp) return;
        for (int k = Input.Keys.NUM_0; k <= Input.Keys.NUM_9; k++) {
            if (Gdx.input.isKeyJustPressed(k)) manualIp += (char) ('0' + (k - Input.Keys.NUM_0));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.PERIOD)) manualIp += ".";
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && manualIp.length() > 0) {
            manualIp = manualIp.substring(0, manualIp.length() - 1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) editingIp = false;
    }

    private void handleTouch() {
        if (!Gdx.input.justTouched()) return;
        if (backBtn.hit(px, py)) {
            cleanup();
            game.setScreen(new MenuScreen(game));
            return;
        }
        if (hostBtn.hit(px, py)) {
            startHost();
            return;
        }
        if (scanBtn.hit(px, py)) {
            rooms.clear();
            rooms.addAll(Discovery.scan());
            selected = rooms.isEmpty() ? -1 : 0;
            status = rooms.isEmpty() ? "未发现房间，可手填 IP 加入" : "发现 " + rooms.size() + " 个房间";
            return;
        }
        if (px >= 40f && px <= 560f && py >= 34f && py <= 96f) {
            editingIp = true;
            status = "输入主机 IP，回车确认";
            return;
        }
        for (int i = 0; i < roomRects.size(); i++) {
            float[] r = roomRects.get(i);
            if (px >= r[0] && px <= r[0] + r[2] && py >= r[1] && py <= r[1] + r[3]) {
                selected = i;
                manualIp = rooms.get(i).host;
                return;
            }
        }
        if (joinBtn.hit(px, py)) {
            join();
        }
    }

    private void startHost() {
        host = new GameHost();
        if (!host.start()) {
            status = "端口被占用，无法开房";
            return;
        }
        responder = new Discovery.Responder("战术房");
        Thread t = new Thread(responder, "neuroshoot-responder");
        t.setDaemon(true);
        t.start();
        status = "房间已开，等待其他玩家（端口 " + GameHost.PORT + "）";
        game.setScreen(new BattleScreen(game));
    }

    private void join() {
        String ip = manualIp.isEmpty()
                ? (selected >= 0 && selected < rooms.size() ? rooms.get(selected).host : "")
                : manualIp;
        if (ip.isEmpty()) {
            status = "请先搜索房间或输入主机 IP";
            return;
        }
        client = new GameClient();
        if (client.connect(ip)) {
            status = "已连接 " + ip;
            game.setScreen(new BattleScreen(game));
        } else {
            status = "连接失败：" + ip;
        }
    }

    private void cleanup() {
        if (host != null) host.stop();
        if (client != null) client.stop();
        if (responder != null) responder.stop();
    }

    private void pointer() {
        Vector3 p = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        px = p.x * (C.VIEW_W / Gdx.graphics.getWidth());
        py = C.VIEW_H - p.y * (C.VIEW_H / Gdx.graphics.getHeight());
    }

    @Override
    public void dispose() {
        cleanup();
        sr.dispose();
        batch.dispose();
    }
}
