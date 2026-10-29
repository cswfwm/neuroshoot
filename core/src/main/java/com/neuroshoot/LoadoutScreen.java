package com.neuroshoot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.neuroshoot.loadout.Attachment;
import com.neuroshoot.loadout.Consumable;
import com.neuroshoot.loadout.GunBuild;
import com.neuroshoot.loadout.Loadout;
import com.neuroshoot.loadout.Weapon;
import com.neuroshoot.ui.Art;
import com.neuroshoot.ui.Button;
import com.neuroshoot.ui.Draw;
import com.neuroshoot.ui.Fonts;
import com.neuroshoot.world.C;

/**
 * 配装界面，布局参考三角洲行动的改枪台：
 *
 * 左栏是枪械属性面板（伤害 / 射程 / 后坐控制 / 操控 / 稳定 / 腰射），
 * 中间是枪械大图与主副武器切换，右侧是按槽位排列的配件列表，
 * 底部是投掷物与药品。所有改装即时反映到左栏数值与总积分。
 */
public final class LoadoutScreen extends ScreenAdapter {
    private static final Color BG = Draw.rgba(14, 17, 22, 1f);
    private static final Color PANEL = Draw.rgba(22, 27, 36, 1f);
    private static final Color LINE = Draw.rgba(48, 58, 74, 1f);
    private static final Color TEXT = Draw.rgba(222, 230, 240, 1f);
    private static final Color DIM = Draw.rgba(140, 154, 174, 1f);
    private static final Color ACCENT = Draw.rgba(96, 200, 160, 1f);

    private final GameRoot game;
    private final MenuScreen.Mode mode;
    private final ShapeRenderer sr = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final Matrix4 proj = new Matrix4();
    private final Loadout loadout;

    private final Button backBtn;
    private final Button startBtn;
    private final Button alliesBtn;
    private final Button mapBtn;
    private final Button primaryBtn;
    private final Button secondaryBtn;

    /** 当前编辑的是主武器还是副武器。 */
    private boolean editingPrimary = true;
    /** 当前打开的配件槽，null 表示没展开。 */
    private Attachment.Slot openSlot;
    /** 枪械列表滚动页。 */
    private int weaponPage;
    private int categoryIndex;

    private float px = -9e3f, py = -9e3f;
    private String toast = "";
    private float toastT;

    public LoadoutScreen(GameRoot game, MenuScreen.Mode mode) {
        this.game = game;
        this.mode = mode;
        this.loadout = game.session.playerLoadout;
        backBtn = new Button("返回", 20f, C.VIEW_H - 54f, 88f, 40f);
        startBtn = new Button(mode == MenuScreen.Mode.AI_VS_AI ? "开始观战" : "进入战场",
                C.VIEW_W - 190f, 22f, 168f, 50f);
        alliesBtn = new Button("队友 3", 20f, 22f, 104f, 50f);
        alliesBtn.fontSize = 18;
        mapBtn = new Button("城镇", 132f, 22f, 104f, 50f);
        mapBtn.fontSize = 18;
        primaryBtn = new Button("主武器", 366f, 566f, 120f, 40f);
        primaryBtn.fontSize = 18;
        secondaryBtn = new Button("副武器", 494f, 566f, 120f, 40f);
        secondaryBtn.fontSize = 18;
    }

    private GunBuild build() {
        return editingPrimary ? loadout.primary : loadout.secondary;
    }

    @Override
    public void render(float delta) {
        pointer();
        handle();

        Gdx.gl.glClearColor(BG.r, BG.g, BG.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        proj.setToOrtho2D(0f, 0f, C.VIEW_W, C.VIEW_H);
        sr.setProjectionMatrix(proj);
        batch.setProjectionMatrix(proj);

        sr.begin(ShapeRenderer.ShapeType.Filled);
        drawPanels(sr);
        backBtn.drawShape(sr, backBtn.hit(px, py));
        startBtn.drawShape(sr, startBtn.hit(px, py));
        alliesBtn.toggled = game.session.allies > 0;
        alliesBtn.drawShape(sr, alliesBtn.hit(px, py));
        mapBtn.drawShape(sr, mapBtn.hit(px, py));
        primaryBtn.toggled = editingPrimary;
        primaryBtn.drawShape(sr, primaryBtn.hit(px, py));
        secondaryBtn.toggled = !editingPrimary;
        secondaryBtn.drawShape(sr, secondaryBtn.hit(px, py));
        sr.end();

        batch.begin();
        drawLeftStats(batch);
        drawCenter(batch);
        drawRightAttachments(batch);
        drawBottom(batch);
        backBtn.drawLabel(batch);
        startBtn.drawLabel(batch);
        alliesBtn.label = game.session.allies > 0 ? "队友 3" : "独狼";
        alliesBtn.drawLabel(batch);
        mapBtn.label = game.session.mapKind == com.neuroshoot.world.MapFactory.Kind.FACTORY
                ? "工厂" : "城镇";
        mapBtn.drawLabel(batch);
        primaryBtn.drawLabel(batch);
        secondaryBtn.drawLabel(batch);
        if (toastT > 0f) {
            Draw.textCentered(batch, Fonts.of(18), toast, C.VIEW_W / 2f, 92f,
                    Draw.rgba(240, 190, 120));
            toastT -= delta;
        }
        batch.end();
    }

    /** 中间面板的分类标签与枪械格子。 */
    private void drawWeaponPicker(ShapeRenderer sr) {
        Weapon.Category[] cats = Weapon.Category.values();
        // 左侧竖排分类
        for (int i = 0; i < cats.length; i++) {
            float y = 556f - i * 62f;
            boolean sel = i == categoryIndex;
            Draw.roundRect(sr, 334f, y - 46f, 96f, 52f, 5f,
                    sel ? Draw.rgba(40, 74, 64) : Draw.rgba(28, 34, 44, 1f));
            Draw.roundBorder(sr, 334f, y - 46f, 96f, 52f, 5f, 1.1f,
                    sel ? ACCENT : LINE);
        }
        // 右侧枪械格：每行 3 个
        Weapon[] list = Weapon.ofCategory(cats[Math.min(categoryIndex, cats.length - 1)]);
        for (int i = 0; i < list.length; i++) {
            int col = i % 3, row = i / 3;
            float x = 440f + col * 116f;
            float y = 512f - row * 62f;
            boolean sel = build().weaponId.equals(list[i].id);
            Draw.roundRect(sr, x, y, 108f, 52f, 5f,
                    sel ? Draw.rgba(46, 88, 74) : Draw.rgba(26, 31, 40, 1f));
            Draw.roundBorder(sr, x, y, 108f, 52f, 5f, 1.1f, sel ? ACCENT : LINE);
        }
    }

    private void drawPanels(ShapeRenderer sr) {
        Draw.roundRect(sr, 14f, 74f, 300f, 520f, 8f, PANEL);       // 左：属性
        Draw.roundRect(sr, 326f, 74f, 470f, 520f, 8f, PANEL);      // 中：枪与改装
        Draw.roundRect(sr, 808f, 74f, 458f, 520f, 8f, PANEL);      // 右：配件
        // 属性条底色
        GunBuild b = build();
        String[] labels = {"伤害", "射程", "后坐控制", "操控速度", "据枪稳定", "腰射精度"};
        int[] vals = {Math.min(100, b.damage() * 2), (int) Math.min(100, b.range() / 15f),
                b.recoilControl(), b.handling(), b.stability(), b.hipAccuracy()};
        for (int i = 0; i < labels.length; i++) {
            float y = 456f - i * 46f;
            Draw.roundRect(sr, 34f, y, 260f, 10f, 4f, Draw.rgba(34, 40, 52, 1f));
            Color c = vals[i] > 66 ? ACCENT : vals[i] > 33 ? Draw.rgba(230, 200, 90)
                    : Draw.rgba(226, 110, 100);
            Draw.roundRect(sr, 34f, y, 260f * vals[i] / 100f, 10f, 4f, c);
        }
        // 配件槽按钮
        Attachment.Slot[] slots = Attachment.Slot.values();
        for (int i = 0; i < slots.length; i++) {
            float y = 500f - i * 78f;
            boolean open = openSlot == slots[i];
            Draw.roundRect(sr, 828f, y - 54f, 418f, 66f, 6f,
                    open ? Draw.rgba(40, 74, 64) : Draw.rgba(30, 36, 47, 1f));
            Draw.roundBorder(sr, 828f, y - 54f, 418f, 66f, 6f, 1.2f,
                    open ? ACCENT : LINE);
        }
        drawWeaponPicker(sr);
        // 左下投掷物 / 药品数量块
        Draw.roundRect(sr, 34f, 96f, 120f, 54f, 6f, Draw.rgba(30, 36, 47, 1f));
        Draw.roundRect(sr, 166f, 96f, 120f, 54f, 6f, Draw.rgba(30, 36, 47, 1f));
    }

    private void drawLeftStats(SpriteBatch batch) {
        GunBuild b = build();
        Draw.text(batch, Fonts.of(20), b.weapon().name, 34f, 574f, TEXT);
        Draw.text(batch, Fonts.of(14), b.weapon().category.label, 34f, 554f, DIM);

        String[] labels = {"伤害", "射程", "后坐控制", "操控速度", "据枪稳定", "腰射精度"};
        int[] vals = {Math.min(100, b.damage() * 2), (int) Math.min(100, b.range() / 15f),
                b.recoilControl(), b.handling(), b.stability(), b.hipAccuracy()};
        for (int i = 0; i < labels.length; i++) {
            float y = 456f - i * 46f;
            Draw.text(batch, Fonts.of(15), labels[i], 34f, y + 26f, DIM);
            Draw.textRight(batch, Fonts.of(16), String.valueOf(vals[i]), 294f, y + 26f, TEXT);
        }
        Draw.text(batch, Fonts.of(15), "弹匣 " + b.magazine() + " 发", 34f, 176f, DIM);
        Draw.text(batch, Fonts.of(15), "换弹 " + String.format("%.1f", b.reloadTime()) + "s",
                34f, 154f, DIM);
        Draw.text(batch, Fonts.of(15), "耗时 " + b.cost() + " 分", 34f, 132f,
                Draw.rgba(240, 200, 120));
        Draw.text(batch, Fonts.of(13), "投掷" + loadout.throws_.size() + "/" + C.THROW_MAX,
                44f, 128f, DIM);
        Draw.text(batch, Fonts.of(13), "药品" + loadout.meds.size() + "/" + C.MED_MAX,
                176f, 128f, DIM);
    }

    private void drawCenter(SpriteBatch batch) {
        GunBuild b = build();
        Weapon.Category[] cats = Weapon.Category.values();
        for (int i = 0; i < cats.length; i++) {
            float y = 556f - i * 62f;
            Draw.textCentered(batch, Fonts.of(14), cats[i].label, 382f, y - 14f,
                    i == categoryIndex ? TEXT : DIM);
        }
        Weapon[] list = Weapon.ofCategory(cats[Math.min(categoryIndex, cats.length - 1)]);
        for (int i = 0; i < list.length; i++) {
            int col = i % 3, row = i / 3;
            float x = 440f + col * 116f;
            float y = 512f - row * 62f;
            Draw.textCentered(batch, Fonts.of(13), shortName(list[i].name), x + 54f, y + 30f,
                    b.weaponId.equals(list[i].id) ? ACCENT : TEXT);
            Draw.textCentered(batch, Fonts.of(11), list[i].cost + "分", x + 54f, y + 13f, Dim2());
        }
        Draw.icon(batch, Art.gun(b.weaponId), 470f, 122f, 210f, 210f);
        Draw.textCentered(batch, Fonts.of(15), editingPrimary ? "主武器" : "副武器",
                330f + 235f, 96f, DIM);
    }

    /** 名字太长时截断，避免格子内溢出。 */
    private String shortName(String n) {
        return n.length() <= 8 ? n : n.substring(0, 8);
    }

    private Color Dim2() {
        return Draw.rgba(110, 122, 140);
    }

    private void drawRightAttachments(SpriteBatch batch) {
        Attachment.Slot[] slots = Attachment.Slot.values();
        for (int i = 0; i < slots.length; i++) {
            float y = 500f - i * 78f;
            Draw.text(batch, Fonts.of(17), slots[i].label, 842f, y - 10f, TEXT);
            String cur = build().attached(slots[i]);
            Attachment a = cur == null ? null : Attachment.byId(cur);
            Draw.text(batch, Fonts.of(14), a == null ? "未安装" : a.name + "  " + a.cost + "分",
                    842f, y - 34f, a == null ? DIM : ACCENT);
            Draw.textRight(batch, Fonts.of(14), "点击更改", 1230f, y - 34f, Dim2());
        }
        if (openSlot != null) {
            Attachment[] list = Attachment.ofSlot(openSlot);
            Draw.text(batch, Fonts.of(15), "← 选一个装到「" + openSlot.label + "」",
                    842f, 128f, Draw.rgba(230, 200, 120));
            for (int i = 0; i < Math.min(list.length, 4); i++) {
                Draw.text(batch, Fonts.of(14), list[i].name + "  " + list[i].cost + "分",
                        842f + (i % 2) * 210f, 106f - (i / 2) * 20f, TEXT);
            }
        }
    }

    private void drawBottom(SpriteBatch batch) {
        Draw.textRight(batch, Fonts.of(18), "积分 " + loadout.spent() + " / " + loadout.points,
                C.VIEW_W - 30f, 52f, loadout.remain() < 0 ? Draw.rgba(232, 96, 88) : ACCENT);
        Draw.text(batch, Fonts.of(15), "主：" + loadout.primary.weapon().name
                + "　副：" + loadout.secondary.weapon().name, 250f, 54f, DIM);
    }

    // ── 交互 ──

    private void handle() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.setScreen(new MenuScreen(game));
            return;
        }
        if (!Gdx.input.justTouched()) return;
        if (backBtn.hit(px, py)) {
            game.setScreen(new MenuScreen(game));
            return;
        }
        if (startBtn.hit(px, py)) {
            if (loadout.remain() < 0) {
                toast = "积分不足，请减少装备";
                toastT = 1.8f;
                return;
            }
            if (mode == MenuScreen.Mode.AI_VS_AI) game.setScreen(new AiVersusScreen(game));
            else game.setScreen(new BattleScreen(game));
            return;
        }
        if (alliesBtn.hit(px, py)) {
            game.session.allies = game.session.allies > 0 ? 0 : 3;
            return;
        }
        if (mapBtn.hit(px, py)) {
            game.session.mapKind = game.session.mapKind
                    == com.neuroshoot.world.MapFactory.Kind.FACTORY
                    ? com.neuroshoot.world.MapFactory.Kind.TOWN
                    : com.neuroshoot.world.MapFactory.Kind.FACTORY;
            return;
        }
        if (primaryBtn.hit(px, py)) {
            editingPrimary = true;
            openSlot = null;
            return;
        }
        if (secondaryBtn.hit(px, py)) {
            editingPrimary = false;
            openSlot = null;
            return;
        }
        // 点配件槽
        Attachment.Slot[] slots = Attachment.Slot.values();
        for (int i = 0; i < slots.length; i++) {
            float y = 500f - i * 78f;
            if (px >= 828f && px <= 1246f && py >= y - 54f && py <= y + 12f) {
                cycleAttachment(slots[i]);
                return;
            }
        }
        // 点分类标签
        Weapon.Category[] cats = Weapon.Category.values();
        for (int i = 0; i < cats.length; i++) {
            float y = 556f - i * 62f;
            if (px >= 334f && px <= 430f && py >= y - 46f && py <= y + 6f) {
                categoryIndex = i;
                weaponPage = 0;
                return;
            }
        }
        // 点枪械格
        Weapon[] list = Weapon.ofCategory(cats[Math.min(categoryIndex, cats.length - 1)]);
        for (int i = 0; i < list.length; i++) {
            int col = i % 3, row = i / 3;
            float x = 440f + col * 116f;
            float y = 512f - row * 62f;
            if (px >= x && px <= x + 108f && py >= y && py <= y + 52f) {
                boolean ok = editingPrimary
                        ? loadout.selectPrimary(list[i].id)
                        : loadout.selectSecondary(list[i].id);
                toast = ok ? "已选 " + list[i].name : "积分不足，换不了 " + list[i].name;
                toastT = 1.5f;
                return;
            }
        }
    }

    /** 循环切换某槽位的配件：未装 → 各配件 → 未装。 */
    private void cycleAttachment(Attachment.Slot slot) {
        Attachment[] list = Attachment.ofSlot(slot);
        GunBuild b = build();
        String cur = b.attached(slot);
        int idx = -1;
        for (int i = 0; i < list.length; i++) if (list[i].id.equals(cur)) idx = i;
        int next = idx + 1;
        int oldCost = b.cost();
        if (next >= list.length) {
            b.attach(slot, null);
        } else {
            b.attach(slot, list[next].id);
        }
        if (loadout.remain() < 0) {
            // 超预算则回滚
            b.attach(slot, cur);
            toast = "积分不足，装不上 " + (next < list.length ? list[next].name : "");
            toastT = 1.6f;
            return;
        }
        toast = (next >= list.length ? "已卸下 " + slot.label
                : "已安装 " + list[next].name);
        toastT = 1.4f;
    }

    /** 循环切换枪械：当前分类内轮换，走完换下一个分类。 */
    private void cycleWeapon() {
        Weapon.Category[] cats = Weapon.Category.values();
        int ci = Math.max(0, Math.min(cats.length - 1, categoryIndex));
        Weapon[] list = Weapon.ofCategory(cats[ci]);
        weaponPage++;
        if (weaponPage >= list.length) {
            weaponPage = 0;
            categoryIndex = (categoryIndex + 1) % cats.length;
            list = Weapon.ofCategory(cats[categoryIndex]);
        }
        String id = list[Math.min(weaponPage, list.length - 1)].id;
        boolean ok = editingPrimary ? loadout.selectPrimary(id) : loadout.selectSecondary(id);
        if (!ok) {
            toast = "积分不足，换不了 " + Weapon.byId(id).name;
            toastT = 1.6f;
        } else {
            toast = "已换为 " + Weapon.byId(id).name + "（" + Weapon.byId(id).category.label + "）";
            toastT = 1.4f;
        }
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
