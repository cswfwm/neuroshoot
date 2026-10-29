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
import com.neuroshoot.ai.Brain;
import com.neuroshoot.loadout.Consumable;
import com.neuroshoot.loadout.Loadout;
import com.neuroshoot.ui.Art;
import com.neuroshoot.ui.Draw;
import com.neuroshoot.ui.Fonts;
import com.neuroshoot.ui.Hud;
import com.neuroshoot.ui.MiniMap;
import com.neuroshoot.ui.WorldView;
import com.neuroshoot.world.C;
import com.neuroshoot.world.GameMap;
import com.neuroshoot.world.Tile;
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.Visibility;
import com.neuroshoot.world.World;

/**
 * 人机对练主界面。
 *
 * 摄像机跟随本地玩家，视野外的敌人被迷雾遮住、也不参与绘制；
 * 玩家只按自己的视野看世界，其余部分呈现为迷雾。
 */
public final class BattleScreen extends ScreenAdapter {
    private final GameRoot game;
    private final ShapeRenderer sr = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final Matrix4 proj = new Matrix4();
    private final Matrix4 worldProj = new Matrix4();

    private final World world;
    private final WorldView.Camera cam = new WorldView.Camera();
    private final Unit player;
    private boolean[][] explored;
    private boolean[][] fogVisible;

    private final MiniMap miniMap = new MiniMap();
    private final com.neuroshoot.ui.ThrowWheel wheel = new com.neuroshoot.ui.ThrowWheel();
    private boolean wheelPressed;
    private final Hud hud = new Hud();

    private float px = -9e3f, py = -9e3f;
    private final com.neuroshoot.ui.Effects fx = new com.neuroshoot.ui.Effects();
    /** 摇杆活动区：占左下角一大块，中心随手指按下位置漂移。 */
    private static final com.badlogic.gdx.math.Rectangle STICK_ZONE =
            new com.badlogic.gdx.math.Rectangle(0f, 0f, C.VIEW_W * 0.42f, C.VIEW_H * 0.55f);
    private static final float STICK_RADIUS = 120f;
    private static final float STICK_DEAD = 12f;
    // 按钮布局：开火在右下角、弹药框之上；其余按钮沿右侧竖排，互不重叠
    private static final com.badlogic.gdx.math.Rectangle FIRE_BTN =
            new com.badlogic.gdx.math.Rectangle(C.VIEW_W - 196f, 128f, 156f, 156f);
    private static final com.badlogic.gdx.math.Rectangle AIM_BTN =
            new com.badlogic.gdx.math.Rectangle(C.VIEW_W - 340f, 150f, 104f, 104f);
    private static final com.badlogic.gdx.math.Rectangle CROUCH_BTN =
            new com.badlogic.gdx.math.Rectangle(C.VIEW_W - 340f, 268f, 96f, 96f);
    private static final com.badlogic.gdx.math.Rectangle RELOAD_BTN =
            new com.badlogic.gdx.math.Rectangle(C.VIEW_W - 200f, 296f, 96f, 76f);
    private static final com.badlogic.gdx.math.Rectangle MED_BTN =
            new com.badlogic.gdx.math.Rectangle(C.VIEW_W - 460f, 150f, 88f, 88f);
    private static final com.badlogic.gdx.math.Rectangle SWAP_BTN =
            new com.badlogic.gdx.math.Rectangle(C.VIEW_W - 460f, 250f, 88f, 76f);
    /** 触屏虚拟摇杆：按下点为圆心，拖动方向与距离决定移动。 */
    private boolean stickActive;
    private float stickCx, stickCy, stickDx, stickDy;
    private int stickPointer = -1;
    private boolean btnFireDown, btnAimDown, btnCrouchDown, btnReloadDown, btnMedDown;
    private boolean btnSwapDown;
    private float smoothAngle;
    private int aimPointer = -1;
    /** 当前准星在屏幕上的位置，开镜时圈跟着它走。 */
    private boolean hasAimPoint;
    private float aimScreenX = C.VIEW_W / 2f;
    private float aimScreenY = C.VIEW_H / 2f;
    private float aimX, aimY;
    private String toast = "";
    private float toastT;
    private boolean ended;
    private float bootT;
    /** 诊断开关：为 true 时只画地面与雾，用于定位黑屏层级。 */
    private static final boolean DEBUG_GROUND_ONLY = false;

    public BattleScreen(GameRoot game) {
        this.game = game;
        game.session.newWorld();
        this.world = game.session.world;
        explored = new boolean[C.MAP_W][C.MAP_H];
        fogVisible = new boolean[C.MAP_W][C.MAP_H];

        boolean night = game.session.controls.nightMode;
        for (Unit u0 : world.units) u0.nightMode = night;
        Loadout lo = game.session.playerLoadout.copy();
        player = new Unit();
        player.id = 0;
        player.team = 0;
        player.name = "你";
        player.local = true;
        player.nightMode = night;
        player.nvg = game.session.nvgEquipped;
        player.armorLevel = game.session.armorLevel;
        world.addUnit(player, lo, 0);
        player.nightMode = night;

        int id = 1;
        if (game.session.allies > 0) {
            for (int i = 0; i < game.session.allies; i++) {
                Unit u = new Unit();
                u.id = id++;
                u.team = 0;
                u.ai = true;
                u.name = "队友" + (i + 1);
                world.addUnit(u, Loadout.defaults(pickAiWeapon(i)), 1 + i);
                u.brain = Brain.scripted();
            }
        }
        for (int i = 0; i < 4; i++) {
            Unit u = new Unit();
            u.id = id++;
            u.team = 1;
            u.ai = true;
            u.name = "敌军" + (i + 1);
            world.addUnit(u, Loadout.defaults(pickAiWeapon(i + 2)), 3 + i);
            u.brain = Brain.scripted();
        }

        world.events = new World.Events() {
            @Override
            public void onKill(Unit killer, Unit victim, com.neuroshoot.loadout.Weapon weapon) {
                hud.pushKill(killer == null ? "环境" : killer.name, victim.name);
                // 无论谁杀谁，都把击杀发生处附近的敌人暴露在小地图上
                if (killer != null && killer.team != player.team) miniMap.markEnemy(killer);
                miniMap.markEnemy(victim);
                for (Unit o : world.units) {
                    if (!o.alive || o.team == player.team) continue;
                    if (Math.hypot(o.x - victim.x, o.y - victim.y) < 260f) {
                        miniMap.markEnemy(o);
                    }
                }
            }

            @Override
            public void onHit(Unit target, int damage) {
                fx.spawnBlood(target.x, target.y, 0f, 6);
                fx.spawnSparks(target.x, target.y, 3);
            }

            @Override
            public void onExplosion(float x, float y, float radius) {
                hud.explosion(x, y, radius);
                fx.spawnRing(x, y, radius);
                fx.spawnSmoke(x, y, 12);
                fx.spawnSparks(x, y, 16);
            }

            @Override
            public void onDoorBroken(float x, float y) {
                hud.doorBroken(x, y);
                fx.spawnSparks(x + C.TILE / 2f, y + C.TILE / 2f, 8);
            }

            @Override
            public void onCrateBroken(float x, float y) {
                hud.crateBroken(x, y);
            }
        };
    }

    private String pickAiWeapon(int i) {
        String[] pool = {"rifle", "sniper", "smg", "shotgun", "pistol"};
        return pool[i % pool.length];
    }

    @Override
    public void render(float delta) {
        handleInput(delta);
        for (Unit u : world.units) if (u.brain != null && u.alive && u != player) u.brain.update(u, world, delta);
        world.step(delta);
        updateExplored();
        hud.tick(delta);
        miniMap.tick(delta);
        fx.update(delta);
        if (!ended && (world.ended || world.aliveCount(0) == 0 || world.aliveCount(1) == 0)) {
            ended = true;
            game.setScreen(new ResultScreen(game, world));
            return;
        }

        Gdx.gl.glClearColor(0.05f, 0.07f, 0.09f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        bootT += delta;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        updateCamera();
        worldProj.setToOrtho2D(cam.x, cam.y, cam.w, cam.h);
        sr.setProjectionMatrix(worldProj);
        batch.setProjectionMatrix(worldProj);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        WorldView.drawGround(sr, world, cam);
        if (!DEBUG_GROUND_ONLY) {
            WorldView.drawBlood(sr, world);
            WorldView.drawTracers(sr, world);
            fx.draw(sr);
        }
        sr.end();
        if (DEBUG_GROUND_ONLY) {
            return;
        }

        batch.begin();
        WorldView.drawTiles(batch, world, cam);
        WorldView.drawCorpses(batch, world);
        WorldView.drawThrowables(batch, world);
        WorldView.drawUnits(batch, world, cam, this::canSee, player, 1f);
        WorldView.drawBulletBodies(batch, world, world.time);
        batch.end();

        // 迷雾
        buildFog();
        drawFogDirect();

        // 屏幕层
        proj.setToOrtho2D(0f, 0f, C.VIEW_W, C.VIEW_H);
        sr.setProjectionMatrix(proj);
        batch.setProjectionMatrix(proj);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        wheel.draw(sr, player, wheelPressed);
        hud.drawMeds(sr, player);
        hud.drawAmmoBox(sr, player);
        hud.drawStatus(sr, player, world);
        miniMap.draw(sr, game.session.layout, world, player, 0f, 0f);
        if (player.aim) drawScope(sr);
        if (player.panicking()) Hud.blurVignette(sr);
        drawTouchControls(sr);
        sr.end();

        batch.begin();
        hud.drawText(batch, player, world);
        wheel.drawIcons(batch, player, wheelPressed);
        hud.drawMedIcons(batch, player);
        hud.drawAmmoText(batch, player);
        drawTouchLabels(batch);
        miniMap.drawLabels(batch);
        drawCrosshair();
        if (toastT > 0f) {
            Draw.textCentered(batch, Fonts.of(22), toast, C.VIEW_W * 0.5f, 120f, Color.WHITE);
            toastT -= delta;
        }
        // 资源加载异常时给出可见提示，而不是静默黑屏
        java.util.List<String> fails = Art.failures();
        if (!fails.isEmpty()) {
            Draw.text(batch, Fonts.of(14), "贴图缺失 " + fails.size() + " 张：" + fails.get(0),
                    24f, C.VIEW_H - 160f, Draw.rgba(255, 120, 200));
        }
        if (!player.alive) {
            Draw.textCentered(batch, Fonts.of(40), "阵亡　等待复活",
                    C.VIEW_W * 0.5f, C.VIEW_H * 0.5f, Draw.rgba(240, 120, 120));
        }
        batch.end();
    }

    /**
     * 直接按格画迷雾：每格一个半透明矩形，不经过 Pixmap 与纹理上传。
     *
     * 之前用「低分辨率纹理放大」的做法在部分机型上会整片变黑，这里是纯几何绘制，
     * 只依赖 ShapeRenderer，稳定且不挑驱动。
     */
    /**
     * 迷雾：以玩家为圆心的半圆视野，边缘用多层扇形渐变过渡，不再是方格块。
     *
     * 做法是在视野半径内由外向内叠若干层同心扇形，透明度逐层降低，
     * 于是边缘自然变软；扇形之外再统一压一层不透明黑。
     * 墙与箱子在迷雾里仍然以暗色轮廓呈现，保证「看得见那是什么东西」。
     */
    /**
     * 迷雾：先压黑不可见区域，再用「挖洞」的方式露出视野。
     *
     * 顺序很关键——之前是先铺满黑再想叠半透明，结果黑底压不回去就是全黑。
     * 现在改为：整屏铺黑 → 已探索区域用半透明黑 → 视野扇形内用更低透明度的黑
     * 逐层递减，最终中心区域几乎通透，形成柔和的半圆视野。
     */
    /**
     * 迷雾绘制。
     *
     * 关键约束：任何可见区域都不能被黑色叠加覆盖。所以这里不用「多层扇形叠黑」，
     * 而是先铺满黑，再用扇环从外向内逐层减淡——每层透明度严格小于前一层，
     * 且总量不超过 1，保证中心完全通透。
     */
    private void drawFogDirect() {
        sr.setProjectionMatrix(worldProj);
        sr.begin(ShapeRenderer.ShapeType.Filled);

        float px_ = player.x;
        float py_ = player.y;
        float radius = player.viewRadius();
        float fov = C.VIEW_FOV;
        float startDeg = (float) Math.toDegrees(player.angle - fov);
        float sweepDeg = (float) Math.toDegrees(fov * 2f);

        // 视野内：先清空（用完全透明的「不提色」方式，即跳过不画）
        // 未探索与已探索但不可见的地方由下面两步压黑。

        // 1) 未探索：深灰半透明，仍能隐约看出地形走向，但看不到任何单位
        for (int ty = 0; ty < C.MAP_H; ty++) {
            for (int tx = 0; tx < C.MAP_W; tx++) {
                if (explored[tx][ty]) continue;
                float x = tx * C.TILE;
                float y = ty * C.TILE;
                if (x + C.TILE < cam.x || x > cam.x + cam.w
                        || y + C.TILE < cam.y || y > cam.y + cam.h) continue;
                sr.setColor(0.30f, 0.32f, 0.36f, 0.88f);
                sr.rect(x, y, C.TILE, C.TILE);
            }
        }

        // 2) 已探索但当前不可见：重雾，保留地形轮廓
        for (int ty = 0; ty < C.MAP_H; ty++) {
            for (int tx = 0; tx < C.MAP_W; tx++) {
                if (!explored[tx][ty] || fogVisible[tx][ty]) continue;
                float x = tx * C.TILE;
                float y = ty * C.TILE;
                if (x + C.TILE < cam.x || x > cam.x + cam.w
                        || y + C.TILE < cam.y || y > cam.y + cam.h) continue;
                byte t = world.map.at(tx, ty);
                boolean wall = t == Tile.WALL || t == Tile.PILLAR
                        || world.map.destructAt(tx, ty) != GameMap.DESTRUCT_NONE;
                // 半透明灰雾：建筑轮廓要能看清，只是整体压暗、看不到人物
                sr.setColor(0.42f, 0.45f, 0.50f, wall ? 0.46f : 0.58f);
                sr.rect(x, y, C.TILE, C.TILE);
                if (wall) {
                    // 墙体在雾里用更亮的灰提亮，轮廓清楚可辨
                    sr.setColor(0.62f, 0.66f, 0.72f, 0.42f);
                    sr.rect(x + 3f, y + 3f, C.TILE - 6f, C.TILE - 6f);
                }
            }
        }

        // 视野边缘不叠任何逐格色块（那会形成灰色方格）。
        // 视野外的暗部已由第 1、2 步铺好，这里不再补色，保持画面干净。

        // 视野内的墙与箱子不额外叠色：它们本来就有贴图，
        // 再盖一层半透明方块会让整个画面糊成灰色方格。

        sr.end();
    }


    /**
     * 计算每个格子当前是否可见。
     *
     * 除了扇形判定，还把玩家脚下与紧邻一圈强制标为可见：
     * 否则贴墙或朝向背面时会连自己站的地方都被雾盖住，整屏全黑。
     */
    private void buildFog() {
        int cx = (int) (player.x / C.TILE);
        int cy = (int) (player.y / C.TILE);
        for (int ty = 0; ty < C.MAP_H; ty++) {
            for (int tx = 0; tx < C.MAP_W; tx++) {
                boolean near = Math.abs(tx - cx) <= 1 && Math.abs(ty - cy) <= 1;
                boolean vis = near || Visibility.tileVisible(world.map, player, tx, ty);
                fogVisible[tx][ty] = vis;
                if (vis) explored[tx][ty] = true;
            }
        }
    }



    private void updateExplored() {
        int r = (int) (C.VIEW_STAND / C.TILE) + 1;
        int cx = (int) (player.x / C.TILE), cy = (int) (player.y / C.TILE);
        for (int ty = Math.max(0, cy - r); ty <= Math.min(C.MAP_H - 1, cy + r); ty++) {
            for (int tx = Math.max(0, cx - r); tx <= Math.min(C.MAP_W - 1, cx + r); tx++) {
                if (Visibility.tileVisible(world.map, player, tx, ty)) explored[tx][ty] = true;
            }
        }
    }

    private boolean canSee(Unit u) {
        if (u == player) return true;
        if (u.team == player.team) return true;
        return Visibility.unitVisible(world.map, player, u);
    }

    private void updateCamera() {
        cam.w = C.VIEW_W;
        cam.h = C.VIEW_H;
        cam.x = player.x - cam.w / 2f;
        cam.y = player.y - cam.h / 2f;
        cam.x = Math.max(0f, Math.min(C.WORLD_W - cam.w, cam.x));
        cam.y = Math.max(0f, Math.min(C.WORLD_H - cam.h, cam.y));
    }

    /**
     * 激光与腰射辅助线。
     *
     * 装了红点/全息这类一倍镜：红色激光 + 两段虚线（提示腰射散布）。
     * 装高倍镜：绿色激光，不画虚线（开镜时本来就有准星圈）。
     */
    private void drawAimLines(ShapeRenderer sr) {
        if (!player.alive) return;
        if (!game.session.controls.laserSight) return;
        String optic = player.gun().attached(com.neuroshoot.loadout.Attachment.Slot.OPTIC);
        boolean lowPower = optic == null || optic.equals("red_dot") || optic.equals("holo");
        Color laser = lowPower ? Draw.rgba(255, 70, 60) : Draw.rgba(90, 240, 140);
        float len = Math.min(720f, player.weapon().range * 0.5f);
        WorldView.drawLaser(sr, player, len, laser);
        if (lowPower) {
            WorldView.drawHipGuide(sr, player, len * 0.8f, Draw.rgba(255, 120, 100));
        }
    }

    /** 触屏控件：左摇杆 + 右侧按钮组，PC 上也显示，方便触屏设备直接上手。 */
    private void drawTouchControls(ShapeRenderer sr) {
        float baseX = stickPointer >= 0 ? stickCx : STICK_ZONE.x + STICK_RADIUS + 20f;
        float baseY = stickPointer >= 0 ? stickCy : STICK_ZONE.y + STICK_RADIUS + 20f;
        Draw.circle(sr, baseX, baseY, STICK_RADIUS,
                Draw.rgba(16, 22, 32, stickActive ? 0.34f : 0.22f));
        Draw.ring(sr, baseX, baseY, STICK_RADIUS, 3f, Draw.rgba(130, 165, 205, 0.65f));
        Draw.ring(sr, baseX, baseY, STICK_RADIUS * 0.55f, 1.5f,
                Draw.rgba(110, 140, 180, 0.35f));
        float kx = baseX + stickDx * STICK_RADIUS * 0.72f;
        float ky = baseY + stickDy * STICK_RADIUS * 0.72f;
        Draw.circle(sr, kx, ky, 46f, Draw.rgba(150, 205, 250, 0.62f));

        float k = game.session.controls.scale;
        circleBtnScaled(sr, FIRE_BTN, k, btnFireDown, Draw.rgba(232, 96, 88, 0.55f));
        circleBtnScaled(sr, AIM_BTN, k, btnAimDown || player.aim,
                Draw.rgba(110, 170, 240, 0.5f));
        circleBtnScaled(sr, CROUCH_BTN, k, btnCrouchDown || player.crouch,
                Draw.rgba(140, 210, 160, 0.5f));
        circleBtnScaled(sr, RELOAD_BTN, k, btnReloadDown, Draw.rgba(220, 190, 110, 0.5f));
        circleBtnScaled(sr, MED_BTN, k, btnMedDown, Draw.rgba(120, 210, 200, 0.5f));
        circleBtnScaled(sr, SWAP_BTN, k, btnSwapDown, Draw.rgba(200, 170, 240, 0.5f));
    }

    /** 按玩家设定缩放绘制的按钮：中心不变，半径按 scale 放大。 */
    private void circleBtnScaled(ShapeRenderer sr, com.badlogic.gdx.math.Rectangle r,
                                 float k, boolean down, Color c) {
        float cx = r.x + r.width / 2f + game.session.controls.offsetX;
        float cy = r.y + r.height / 2f + game.session.controls.offsetY;
        float rad = Math.min(r.width, r.height) / 2f * k;
        Draw.circle(sr, cx, cy, rad, down ? c : Draw.rgba(18, 24, 34, 0.42f));
        Draw.ring(sr, cx, cy, rad, 2f, Draw.rgba(140, 170, 205, 0.6f));
    }

    private void circleBtn(ShapeRenderer sr, com.badlogic.gdx.math.Rectangle r,
                           boolean down, Color c) {
        float cx = r.x + r.width / 2f;
        float cy = r.y + r.height / 2f;
        float rad = Math.min(r.width, r.height) / 2f;
        Draw.circle(sr, cx, cy, rad, down ? c : Draw.rgba(18, 24, 34, 0.42f));
        Draw.ring(sr, cx, cy, rad, 2f, Draw.rgba(140, 170, 205, 0.6f));
    }

    private void drawScope(ShapeRenderer sr) {
        // 准星跟着瞄准点移动，而不是死钉在屏幕中央
        float cx = hasAimPoint ? aimScreenX : C.VIEW_W / 2f;
        float cy = hasAimPoint ? aimScreenY : C.VIEW_H / 2f;
        cx = Math.max(190f, Math.min(C.VIEW_W - 190f, cx));
        cy = Math.max(190f, Math.min(C.VIEW_H - 190f, cy));
        Draw.ring(sr, cx, cy, 168f, 3f, Draw.rgba(20, 26, 32, 0.85f));
        Draw.ring(sr, cx, cy, 164f, 1.5f, Draw.rgba(180, 220, 255, 0.5f));
        Draw.line(sr, cx - 30f, cy, cx - 12f, cy, 2f, Draw.rgba(230, 245, 255, 0.9f));
        Draw.line(sr, cx + 12f, cy, cx + 30f, cy, 2f, Draw.rgba(230, 245, 255, 0.9f));
        Draw.line(sr, cx, cy - 30f, cx, cy - 12f, 2f, Draw.rgba(230, 245, 255, 0.9f));
        Draw.line(sr, cx, cy + 12f, cx, cy + 30f, 2f, Draw.rgba(230, 245, 255, 0.9f));
        Draw.circle(sr, cx, cy, 2f, Draw.rgba(255, 255, 255, 0.95f));
    }

    private void drawTouchLabels(SpriteBatch batch) {
        Draw.textCentered(batch, Fonts.of(22), "开火",
                FIRE_BTN.x + FIRE_BTN.width / 2f, FIRE_BTN.y + FIRE_BTN.height / 2f - 8f,
                Color.WHITE);
        Draw.textCentered(batch, Fonts.of(16), "开镜",
                AIM_BTN.x + AIM_BTN.width / 2f, AIM_BTN.y + AIM_BTN.height / 2f - 6f,
                Color.WHITE);
        Draw.textCentered(batch, Fonts.of(16), "蹲",
                CROUCH_BTN.x + CROUCH_BTN.width / 2f, CROUCH_BTN.y + CROUCH_BTN.height / 2f - 6f,
                Color.WHITE);
        Draw.textCentered(batch, Fonts.of(15), "换弹",
                RELOAD_BTN.x + RELOAD_BTN.width / 2f, RELOAD_BTN.y + RELOAD_BTN.height / 2f - 5f,
                Color.WHITE);
        Draw.textCentered(batch, Fonts.of(15), "用药",
                MED_BTN.x + MED_BTN.width / 2f, MED_BTN.y + MED_BTN.height / 2f - 5f,
                Color.WHITE);
        Draw.textCentered(batch, Fonts.of(14), "切枪",
                SWAP_BTN.x + SWAP_BTN.width / 2f, SWAP_BTN.y + SWAP_BTN.height / 2f - 5f,
                Color.WHITE);
        float dx0 = stickPointer >= 0 ? stickCx : STICK_ZONE.x + STICK_RADIUS + 20f;
        float dy0 = stickPointer >= 0 ? stickCy : STICK_ZONE.y + STICK_RADIUS + 20f;
        Draw.textCentered(batch, Fonts.of(13), "移动",
                dx0, dy0 - STICK_RADIUS - 18f, Draw.rgba(170, 190, 214));
    }

    private void drawCrosshair() {
        if (player.aim) return;
        float cx = C.VIEW_W / 2f, cy = C.VIEW_H / 2f;
        com.badlogic.gdx.graphics.Texture t = Fonts.pixel();
        float spread = 8f + (player.crouch ? -3f : 0f);
        batch.setColor(1f, 1f, 1f, 0.85f);
        batch.draw(t, cx - 1f, cy + spread, 2f, 10f);
        batch.draw(t, cx - 1f, cy - spread - 10f, 2f, 10f);
        batch.draw(t, cx + spread, cy - 1f, 10f, 2f);
        batch.draw(t, cx - spread - 10f, cy - 1f, 10f, 2f);
        batch.setColor(Color.WHITE);
    }

    private void handleInput(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.setScreen(new MenuScreen(game));
            return;
        }
        if (!player.alive) return;

        Vector3 p = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0f);
        px = p.x * (C.VIEW_W / Gdx.graphics.getWidth());
        py = C.VIEW_H - p.y * (C.VIEW_H / Gdx.graphics.getHeight());

        // 键盘移动
        float mx = 0f, my = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) mx -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) mx += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) my += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) my -= 1f;

        // 触屏：左下摇杆优先
        handleTouch(delta);
        if (stickActive) {
            mx = stickDx;
            my = stickDy;
        }

        // 瞄准：只取空白区拖动点或鼠标位置，摇杆与按钮的触点不参与转向
        boolean hasAim = false;
        float ax0 = 0f, ay0 = 0f;
        if (!Gdx.input.isTouched() || aimPointer >= 0) {
            if (aimPointer >= 0) {
                ax0 = Gdx.input.getX(aimPointer) * (C.VIEW_W / Gdx.graphics.getWidth());
                ay0 = C.VIEW_H - Gdx.input.getY(aimPointer)
                        * (C.VIEW_H / Gdx.graphics.getHeight());
                hasAim = true;
            } else {
                ax0 = px;
                ay0 = py;
                hasAim = true;
            }
        }
        // 朝向优先级：开火/开镜时锁定瞄准方向；否则朝移动方向转身，枪口随之转动
        boolean aiming = player.aim
                || Gdx.input.isButtonPressed(Input.Buttons.LEFT) || btnFireDown;
        float targetAngle = player.angle;
        if (aiming) {
            if (hasAim) {
                float worldMx = cam.x + ax0;
                float worldMy = cam.y + ay0;
                targetAngle = (float) Math.atan2(worldMy - player.y, worldMx - player.x);
                lastAim = targetAngle;
                lastAimValid = true;
                hasAimPoint = true;
                aimScreenX = ax0;
                aimScreenY = ay0;
            } else if (lastAimValid) {
                targetAngle = lastAim;
            }
        } else {
            // 移动时人物朝移动方向（枪口也朝同一个方向）
            float mlen = (float) Math.hypot(mx, my);
            if (mlen > 0.15f) {
                targetAngle = (float) Math.atan2(my, mx);
                lastAim = targetAngle;
                lastAimValid = true;
            } else if (lastAimValid) {
                targetAngle = lastAim;
            }
        }
        float da = com.neuroshoot.world.Visibility.normalize(targetAngle - smoothAngle);
        smoothAngle += da * Math.min(1f, delta * 18f);
        player.angle = smoothAngle;

        float sp = player.moveSpeed();
        float len = (float) Math.hypot(mx, my);
        if (len > 0.01f) {
            player.tx_ = mx / len * Math.min(1f, len) * sp;
            player.ty_ = my / len * Math.min(1f, len) * sp;
            world.dropFootprint(player);
        } else {
            player.tx_ = 0f;
            player.ty_ = 0f;
        }

        player.crouch = Gdx.input.isKeyPressed(Input.Keys.C)
                || Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || btnCrouchDown;
        player.aim = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)
                || Gdx.input.isButtonPressed(Input.Buttons.RIGHT) || btnAimDown;

        boolean fire = Gdx.input.isKeyPressed(Input.Keys.SPACE) || btnFireDown
                || (!Gdx.input.isTouched() && Gdx.input.isButtonPressed(Input.Buttons.LEFT));
        if (fire && !player.aim) legalHipFire();
        if (fire) {
            boolean wasReady = player.fireCd <= 0f && player.ammo > 0 && !player.reloading;
            world.fire(player);
            if (wasReady) {
                float mxp = player.x + (float) Math.cos(player.angle) * 30f;
                float myp = player.y + (float) Math.sin(player.angle) * 30f;
                fx.spawnMuzzle(mxp, myp, player.angle);
                fx.spawnSparks(mxp, myp, 4);
                fx.spawnCasing(player.x, player.y, player.angle);
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R) || btnReloadDown) player.reload();
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            if (!player.usingPrimary) player.switchWeapon();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            if (player.usingPrimary) player.switchWeapon();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.X) || btnSwapDown) player.switchWeapon();
        btnSwapDown = false;
        btnReloadDown = false;
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) tryDoor();
        if (Gdx.input.isKeyJustPressed(Input.Keys.G)) useThrowable(0);
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q) || btnMedDown) useMed();
        btnMedDown = false;
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            toast = "投掷物 " + player.throwables.size() + "　药品 " + player.meds.size();
            toastT = 1.5f;
        }
    }

    private float lastAim;
    private boolean lastAimValid;

    /** 腰射：允许开火，但精度低，这里只用于触发火花等表现。 */
    private void legalHipFire() {
    }

    /** 处理触屏：左下摇杆 + 右下按钮组。 */
    /**
     * 触屏输入。
     *
     * 摇杆采用「浮动中心」：手指落在左下活动区时，以按下点为圆心，拖动即偏转，
     * 死区 12px 避免手抖；松开后重新按下会以新位置为圆心，比固定摇杆好推。
     * 右侧按钮各自独立命中，按哪个只触发哪个，不会连带开火。
     */
    private void handleTouch(float delta) {
        btnFireDown = false;
        btnAimDown = false;
        btnCrouchDown = false;
        btnReloadDown = false;
        btnMedDown = false;
        wheelPressed = false;
        boolean stickHeld = false;
        aimPointer = -1;

        for (int i = 0; i < 8; i++) {
            if (!Gdx.input.isTouched(i)) continue;
            float tx = Gdx.input.getX(i) * (C.VIEW_W / Gdx.graphics.getWidth());
            float ty = C.VIEW_H - Gdx.input.getY(i) * (C.VIEW_H / Gdx.graphics.getHeight());

            if (STICK_ZONE.contains(tx, ty)) {
                if (stickPointer < 0) {
                    stickPointer = i;
                    stickCx = tx;
                    stickCy = ty;
                }
                if (stickPointer == i) {
                    stickHeld = true;
                    float dx = tx - stickCx;
                    float dy = ty - stickCy;
                    float m = (float) Math.hypot(dx, dy);
                    if (m > STICK_RADIUS) {
                        dx = dx / m * STICK_RADIUS;
                        dy = dy / m * STICK_RADIUS;
                        m = STICK_RADIUS;
                    }
                    stickDx = dx / STICK_RADIUS;
                    stickDy = dy / STICK_RADIUS;
                    stickActive = m > STICK_DEAD;
                }
                continue;
            }

            if (FIRE_BTN.contains(tx, ty) && ty > 120f) {
                btnFireDown = true;
            } else if (AIM_BTN.contains(tx, ty)) {
                btnAimDown = true;
            } else if (CROUCH_BTN.contains(tx, ty)) {
                btnCrouchDown = true;
            } else if (RELOAD_BTN.contains(tx, ty)) {
                btnReloadDown = true;
            } else if (MED_BTN.contains(tx, ty)) {
                btnMedDown = true;
            } else if (SWAP_BTN.contains(tx, ty)) {
                btnSwapDown = true;
            } else {
                int hit = wheel.hitTest(tx, ty, player.throwables.size());
                if (hit >= 0) {
                    wheelPressed = true;
                    wheel.expanded = true;
                    wheel.selected = hit;
                    playThrow(hit);
                } else {
                    wheelPressed = false;
                    aimPointer = i;
                }
            }
        }
        if (!stickHeld) {
            stickActive = false;
            stickDx = 0f;
            stickDy = 0f;
            if (stickPointer >= 0 && !Gdx.input.isTouched(stickPointer)) stickPointer = -1;
        }
    }

    /** 站在门旁按 E 开门。破门只能靠霰弹与爆炸。 */
    private void tryDoor() {
        int tx = (int) (player.x / C.TILE);
        int ty = (int) (player.y / C.TILE);
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            int nx = tx + d[0], ny = ty + d[1];
            if (world.map.at(nx, ny) == Tile.DOOR && !world.map.doorPassable(nx, ny)) {
                world.map.openDoor(nx, ny);
                toast = "已开门";
                toastT = 1.2f;
                return;
            }
        }
    }

    /** 轮盘点了第几个就扔第几个。 */
    private void playThrow(int index) {
        if (index < 0 || index >= player.throwables.size()) return;
        String id = player.throwables.get(index);
        float tx = lastAimValid ? player.x + (float) Math.cos(lastAim) * 320f
                : player.x + (float) Math.cos(player.angle) * 320f;
        float ty = lastAimValid ? player.y + (float) Math.sin(lastAim) * 320f
                : player.y + (float) Math.sin(player.angle) * 320f;
        world.throwItem(player, id, tx, ty);
        wheel.selected = -1;
        toast = "已投出 " + id;
        toastT = 1.0f;
    }

    private void useThrowable(int index) {
        if (player.throwables.isEmpty()) return;
        String id = player.throwables.get(0);
        float tx = cam.x + px, ty = cam.y + py;
        world.throwItem(player, id, tx, ty);
    }

    private void useMed() {
        if (player.bleeding && player.meds.contains("bandage")) {
            player.useMed("bandage");
            toast = "已止血";
        } else if (player.fractured && player.meds.contains("surgery")) {
            player.useMed("surgery");
            toast = "骨折已处理";
        } else if (player.hp < 70 && player.meds.contains("syringe")) {
            player.useMed("syringe");
            toast = "已注射治疗针";
        } else if (player.meds.contains("painkiller")) {
            player.useMed("painkiller");
            toast = "止痛药生效";
        }
        toastT = 1.4f;
    }

    @Override
    public void dispose() {
        sr.dispose();
        batch.dispose();
    }
}
