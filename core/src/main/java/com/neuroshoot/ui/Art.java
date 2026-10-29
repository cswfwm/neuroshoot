package com.neuroshoot.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;

import java.util.HashMap;
import java.util.Map;

/** 素材加载与缓存。贴图路径与 assets/art 下的分类目录一一对应。 */
public final class Art {
    private static final Map<String, Texture> CACHE = new HashMap<>();
    private static Texture FALLBACK;
    private static final java.util.List<String> FAILED = new java.util.ArrayList<>();

    private Art() {}

    public static Texture get(String path) {
        Texture t = CACHE.get(path);
        if (t != null) return t;
        try {
            // 只用线性过滤：不下发 mipmap 需求，避免采样到不存在的层级导致整图全黑
            t = new Texture(Gdx.files.internal("art/" + path + ".png"));
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            // 单张图缺失不应该让整屏变黑：记录一次并退回占位图
            com.badlogic.gdx.Gdx.app.error("Art", "加载失败 " + path, e);
            FAILED.add(path);
            t = placeholder();
        }
        CACHE.put(path, t);
        return t;
    }

    /** 缺图时的占位：品红方块，一眼能看出是资源问题而不是渲染问题。 */
    private static Texture placeholder() {
        if (FALLBACK == null) {
            com.badlogic.gdx.graphics.Pixmap pm = new com.badlogic.gdx.graphics.Pixmap(
                    8, 8, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            pm.setColor(1f, 0f, 1f, 1f);
            pm.fill();
            pm.setColor(0f, 0f, 0f, 1f);
            pm.drawRectangle(0, 0, 8, 8);
            FALLBACK = new Texture(pm);
            pm.dispose();
        }
        return FALLBACK;
    }

    // 角色：以「队伍 + 姿态」取图，由渲染层根据速度与开火状态挑帧
    public static Texture charFrame(String team, boolean crouch, boolean firing, int walkFrame) {
        return charFrame(team, crouch, firing, walkFrame, false, 0);
    }

    /**
     * 角色帧选择：换弹优先于开火与走路，因为换弹是玩家最需要读到的动作。
     *
     * @param reloading 是否在换弹
     * @param reloadPhase 换弹进度 0~2，对应三帧：取下弹匣 / 拔出 / 推入
     */
    public static Texture charFrame(String team, boolean crouch, boolean firing, int walkFrame,
                                    boolean reloading, int reloadPhase) {
        String prefix = team.equals("blue") ? "blue" : "red";
        if (reloading) {
            int f = Math.max(1, Math.min(3, reloadPhase));
            return get("char/" + prefix + "_reload" + f);
        }
        if (crouch) return get("char/" + prefix + (firing ? "_crouch_fire" : "_crouch"));
        if (firing) return get("char/" + prefix + "_fire");
        if (walkFrame == 1) return get("char/" + prefix + "_walk1");
        if (walkFrame == 2) return get("char/" + prefix + "_walk2");
        return get("char/" + prefix + "_stand");
    }

    public static Texture dead(String team) {
        return get("char/" + (team.equals("blue") ? "blue" : "red") + "_dead");
    }

    /**
     * 枪械贴图：57 把枪不需要 57 张图，按「分类 + 枪型特征」复用同一套原型贴图。
     * 这样包体只增加十张图，视觉上仍能区分手枪/冲锋枪/步枪/狙击/霰弹/机枪。
     */
    /** 每把枪都有自己的贴图，直接用 id 取，缺失时回退到分类原型图。 */
    public static Texture gun(String weaponId) {
        Texture t = CACHE.get("guns/" + weaponId);
        if (t == null) {
            try {
                t = new Texture(Gdx.files.internal("art/guns/" + weaponId + ".png"));
                t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            } catch (Exception e) {
                t = get("guns/rifle");
            }
            CACHE.put("guns/" + weaponId, t);
        }
        return t;
    }

    /** 子弹帧：4 帧循环表现飞行旋转；曳光弹单独一张。 */
    public static Texture bullet(int frame, boolean tracer) {
        if (tracer) return get("bullet/tracer_0");
        int f = Math.max(0, Math.min(3, frame));
        return get("bullet/round_" + f);
    }

    public static Texture slug() {
        return get("bullet/slug");
    }

    public static Texture casing() {
        return get("bullet/casing");
    }

    public static Texture throwable(String id) {
        return get("throw/" + id);
    }

    public static Texture med(String id) {
        return get("meds/" + id);
    }

    public static Texture crate(int hp) {
        if (hp <= 0) return get("props/crate_broken");
        if (hp < 40) return get("props/crate_damaged");
        return get("props/crate");
    }

    /** 供诊断：已成功加载的贴图数量与失败列表。 */
    public static int loadedCount() {
        return CACHE.size();
    }

    public static java.util.List<String> failures() {
        return new java.util.ArrayList<>(FAILED);
    }

    public static void dispose() {
        for (Texture t : CACHE.values()) t.dispose();
        CACHE.clear();
        Fonts.dispose();
    }
}
