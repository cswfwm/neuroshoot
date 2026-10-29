package com.neuroshoot.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.neuroshoot.world.C;
import com.neuroshoot.world.GameMap;
import com.neuroshoot.world.Tile;
import com.neuroshoot.world.Unit;
import com.neuroshoot.world.Visibility;
import com.neuroshoot.world.World;

/**
 * 世界渲染：按摄像机可视范围裁剪绘制地形、道具、角色与弹道。
 *
 * 只负责「画」，不判断可见性；调用方传入一个 unitVisible 回调，
 * 被迷雾遮住的敌人不会出现在这里，因此画面与逻辑完全一致。
 */
public final class WorldView {
    public interface VisibilityTest {
        boolean visible(Unit u);
    }

    private static final Color C_GRASS = new Color(0.36f, 0.55f, 0.32f, 1f);
    private static final Color C_BLOOD = new Color(0.52f, 0.05f, 0.06f, 1f);

    private WorldView() {}

    /** 地面用纯色打底（贴图在上层铺，带细密网格），这里只负责区分材质底色。 */
    public static void drawGround(ShapeRenderer sr, World world, Camera cam) {
        int x0 = Math.max(0, (int) (cam.x / C.TILE) - 1);
        int y0 = Math.max(0, (int) (cam.y / C.TILE) - 1);
        int x1 = Math.min(C.MAP_W - 1, (int) ((cam.x + cam.w) / C.TILE) + 1);
        int y1 = Math.min(C.MAP_H - 1, (int) ((cam.y + cam.h) / C.TILE) + 1);
        for (int ty = y0; ty <= y1; ty++) {
            for (int tx = x0; tx <= x1; tx++) {
                float x = tx * C.TILE;
                float y = ty * C.TILE;
                byte t = world.map.at(tx, ty);
                if (t == Tile.FLOOR) {
                    sr.setColor(0.42f, 0.40f, 0.36f, 1f);
                } else if (t == Tile.STAIR) {
                    sr.setColor(0.50f, 0.44f, 0.36f, 1f);
                } else {
                    sr.setColor(C_GRASS);
                }
                sr.rect(x, y, C.TILE, C.TILE);
            }
        }
    }

    public static void drawBlood(ShapeRenderer sr, World world) {
        for (World.Blood b : world.bloods) {
            float a = Math.min(1f, b.life / C.BLOOD_FADE) * 0.55f;
            sr.setColor(C_BLOOD.r, C_BLOOD.g, C_BLOOD.b, a);
            sr.circle(b.x, b.y, b.radius, 10);
        }
    }

    public static void drawTiles(SpriteBatch batch, World world, Camera cam) {
        int x0 = Math.max(0, (int) (cam.x / C.TILE) - 1);
        int y0 = Math.max(0, (int) (cam.y / C.TILE) - 1);
        int x1 = Math.min(C.MAP_W - 1, (int) ((cam.x + cam.w) / C.TILE) + 1);
        int y1 = Math.min(C.MAP_H - 1, (int) ((cam.y + cam.h) / C.TILE) + 1);
        for (int ty = y0; ty <= y1; ty++) {
            for (int tx = x0; tx <= x1; tx++) {
                float x = tx * C.TILE;
                float y = ty * C.TILE;
                byte t = world.map.at(tx, ty);
                int i = world.map.idx(tx, ty);
                if (world.map.destructAt(tx, ty) == GameMap.DESTRUCT_CRATE) {
                    int hp = world.map.destructHpAt(tx, ty);
                    Texture ct = Art.crate(hp);
                    batch.draw(ct, x, y, C.TILE, C.TILE, 0, 0, ct.getWidth(), ct.getHeight(),
                            false, false);
                    continue;
                }
                switch (t) {
                    case Tile.GRASS:
                        batch.draw(Art.get("tiles/grass"), x, y, C.TILE, C.TILE);
                        break;
                    case Tile.FLOOR:
                        batch.draw(Art.get(world.map.levelAt(tx, ty) >= 2
                                ? "tiles/floor_upper" : "tiles/floor"),
                                x, y, C.TILE, C.TILE);
                        break;
                    case Tile.STAIR:
                        batch.draw(Art.get("tiles/stair"), x, y, C.TILE, C.TILE);
                        break;
                    case Tile.WALL:
                        // 按墙体走向选贴图：横向墙连成一条，竖向墙转 90°，避免竖墙长成横板
                        batch.draw(Art.get(wallTexture(world.map, tx, ty)),
                                x, y, C.TILE, C.TILE);
                        break;
                    case Tile.DOOR: {
                        int st = world.map.doorState[i];
                        String path = st >= 3 ? "tiles/door_broken"
                                : st >= 2 ? "tiles/door_open" : "tiles/door_closed";
                        batch.draw(Art.get(path), x, y, C.TILE, C.TILE);
                        break;
                    }
                    case Tile.PILLAR:
                        batch.draw(Art.get("props/pillar"), x, y, C.TILE, C.TILE);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * 判断该墙格该用横版还是竖版贴图。
     *
     * 左右两边是实心墙、上下是空地 → 说明这是一段竖直的墙。
     * 反之用横版；十字或孤立格默认横版。
     */
    private static String wallTexture(GameMap map, int tx, int ty) {
        boolean left = isWallLike(map, tx - 1, ty);
        boolean right = isWallLike(map, tx + 1, ty);
        boolean up = isWallLike(map, tx, ty + 1);
        boolean down = isWallLike(map, tx, ty - 1);
        int horizontal = (left ? 1 : 0) + (right ? 1 : 0);
        int vertical = (up ? 1 : 0) + (down ? 1 : 0);
        return vertical > horizontal ? "tiles/wall_v" : "tiles/wall";
    }

    private static boolean isWallLike(GameMap map, int tx, int ty) {
        if (!map.inside(tx, ty)) return true;
        byte t = map.at(tx, ty);
        return t == Tile.WALL || t == Tile.PILLAR || t == Tile.DOOR
                || map.destructAt(tx, ty) != GameMap.DESTRUCT_NONE;
    }

    public static void drawUnits(SpriteBatch batch, World world, Camera cam,
                                 VisibilityTest visible, Unit local, float alpha) {
        for (Unit u : world.units) {
            if (!u.alive) continue;
            boolean isLocal = u == local;
            boolean teamVisible = local != null && u.team == local.team;
            if (!isLocal && !teamVisible && !visible.visible(u)) continue;

            // 换弹进度映射到三帧：0~33% 取下、33~66% 拔出、66~100% 推入
            int reloadPhase = 0;
            if (u.reloading) {
                float prog = 1f - u.reloadT / Math.max(0.01f, u.gun().reloadTime());
                reloadPhase = prog < 0.34f ? 1 : prog < 0.68f ? 2 : 3;
            }
            Texture tex = Art.charFrame(u.team == 0 ? "blue" : "red", u.crouch,
                    u.muzzleT > 0f, walkFrame(u), u.reloading, reloadPhase);
            float size = u.crouch ? C.TILE * 0.86f : C.TILE;
            float cx = u.rx + (u.x - u.rx) * alpha;
            float cy = u.ry + (u.y - u.ry) * alpha;
            float ang = u.rAngle + Visibility.normalize(u.angle - u.rAngle) * alpha;

            // 贴图默认朝右，这里按朝向旋转
            batch.setColor(Color.WHITE);
            batch.draw(tex, cx - size / 2f, cy - size / 2f,
                    size / 2f, size / 2f, size, size, 1f, 1f,
                    (float) Math.toDegrees(ang), 0, 0, tex.getWidth(), tex.getHeight(),
                    false, false);

            // 枪械叠在双手位置
            Texture gun = Art.gun(u.weapon().id);
            float gs = C.TILE * 0.92f;
            batch.draw(gun, cx - gs / 2f, cy - gs / 2f,
                    gs / 2f, gs / 2f, gs, gs, 1f, 1f,
                    (float) Math.toDegrees(ang), 0, 0,
                    gun.getWidth(), gun.getHeight(), false, false);

            if (u.hitFlashT > 0f) {
                // 受击时叠一层白色闪光，而不是把颜色清成透明
                batch.setColor(1f, 1f, 1f, Math.min(0.85f, u.hitFlashT * 2.6f));
                batch.draw(tex, cx - size / 2f, cy - size / 2f, size, size);
                batch.setColor(Color.WHITE);
            }
        }
        batch.setColor(Color.WHITE);
    }

    private static int walkFrame(Unit u) {
        if (u.vx == 0f && u.vy == 0f) return 0;
        int f = ((int) (u.walkPhase)) % 2;
        return f + 1;
    }

    public static void drawCorpses(SpriteBatch batch, World world) {
        for (Unit u : world.units) {
            if (u.alive) continue;
            Texture tex = Art.dead(u.team == 0 ? "blue" : "red");
            float s = C.TILE;
            batch.draw(tex, u.x - s / 2f, u.y - s / 2f, s, s);
        }
    }

    /**
     * 子弹：画成「尖头弹丸 + 渐隐拖尾」而不是圆球。
     * 弹丸沿速度方向拉长，尾部叠两段更暗更细的线段，读起来有速度感。
     */
    /**
     * 弹道光轨：必须由调用方在 sr.begin(...) 内调用。
     *
     * 之前这个方法同时画光轨和弹体贴图，导致在 batch.begin 里调 sr，
     * 两个渲染器状态互相破坏 → 进战场直接闪退。现在按渲染器拆成两个方法。
     */
    public static void drawTracers(ShapeRenderer sr, World world) {
        for (World.Bullet b : world.bullets) {
            float m = (float) Math.hypot(b.vx, b.vy);
            if (m < 0.001f) continue;
            float nx = b.vx / m, ny = b.vy / m;
            float fullLen = (float) Math.hypot(b.x - b.ox, b.y - b.oy);
            float trailLen = Math.min(fullLen, b.fromShotgun ? 260f : 900f);

            sr.setColor(1f, 0.74f, 0.34f, 0.10f);
            sr.rectLine(b.x - nx * trailLen, b.y - ny * trailLen, b.x, b.y, 5f);
            sr.setColor(1f, 0.84f, 0.48f, 0.26f);
            sr.rectLine(b.x - nx * trailLen * 0.86f, b.y - ny * trailLen * 0.86f,
                    b.x, b.y, 2.4f);
            sr.setColor(1f, 0.96f, 0.82f, 0.72f);
            sr.rectLine(b.x - nx * trailLen * 0.72f, b.y - ny * trailLen * 0.72f,
                    b.x, b.y, 1.1f);
            float head = Math.min(trailLen, 46f);
            sr.setColor(1f, 1f, 0.95f, 0.9f);
            sr.rectLine(b.x - nx * head, b.y - ny * head, b.x, b.y, 1.8f);
            if (fullLen > trailLen * 1.05f) {
                sr.setColor(1f, 0.86f, 0.56f, 0.16f);
                sr.rectLine(b.ox, b.oy, b.x - nx * trailLen, b.y - ny * trailLen, 0.9f);
            }
        }
    }

    /** 弹体：必须由调用方在 batch.begin() 内调用。 */
    public static void drawBulletBodies(SpriteBatch batch, World world, float time) {
        int frame = (int) (time * 16f) % 4;
        for (World.Bullet b : world.bullets) {
            float m = (float) Math.hypot(b.vx, b.vy);
            if (m < 0.001f) continue;
            float ang = (float) Math.toDegrees(Math.atan2(b.vy, b.vx));
            if (b.fromShotgun) {
                float s = 14f;
                com.badlogic.gdx.graphics.Texture t = Art.slug();
                batch.draw(t, b.x - s / 2f, b.y - s / 2f, s / 2f, s / 2f, s, s,
                        1f, 1f, ang, 0, 0, t.getWidth(), t.getHeight(), false, false);
            } else {
                float s = 26f;
                com.badlogic.gdx.graphics.Texture t = Art.bullet(frame, b.damage > 30);
                batch.draw(t, b.x - s / 2f, b.y - s / 2f, s / 2f, s / 2f, s, s,
                        1f, 1f, ang, 0, 0, t.getWidth(), t.getHeight(), false, false);
            }
        }
    }

    /**
     * 激光瞄准线：从枪口射出一条与瞄准方向一致的细线，末端带光点。
     *
     * 红点/全息镜配红色激光，高倍镜配绿色，用来直观提示当前指向。
     */
    public static void drawLaser(ShapeRenderer sr, Unit u, float length, Color color) {
        if (!u.alive) return;
        float mx = u.x + (float) Math.cos(u.angle) * 26f;
        float my = u.y + (float) Math.sin(u.angle) * 26f;
        float ex = u.x + (float) Math.cos(u.angle) * length;
        float ey = u.y + (float) Math.sin(u.angle) * length;

        // 光晕
        sr.setColor(color.r, color.g, color.b, 0.12f);
        sr.rectLine(mx, my, ex, ey, 5f);
        // 主线
        sr.setColor(color.r, color.g, color.b, 0.55f);
        sr.rectLine(mx, my, ex, ey, 1.6f);
        // 末端光点
        sr.setColor(color.r, color.g, color.b, 0.85f);
        sr.circle(ex, ey, 3.2f, 10);
    }

    /**
     * 腰射辅助线：由两段虚线组成的直线，提示腰射散布范围。
     * 装了红点/全息这类一倍镜时显示，帮助腰射时判断准度。
     */
    public static void drawHipGuide(ShapeRenderer sr, Unit u, float length, Color color) {
        if (!u.alive) return;
        float mx = u.x + (float) Math.cos(u.angle) * 26f;
        float my = u.y + (float) Math.sin(u.angle) * 26f;
        float dx = (float) Math.cos(u.angle);
        float dy = (float) Math.sin(u.angle);
        int segs = 12;
        for (int i = 0; i < segs; i++) {
            if (i % 2 == 1) continue;   // 隔段画 → 虚线
            float t0 = i / (float) segs;
            float t1 = (i + 1) / (float) segs;
            sr.setColor(color.r, color.g, color.b, 0.30f * (1f - t0 * 0.6f));
            sr.rectLine(mx + dx * length * t0, my + dy * length * t0,
                    mx + dx * length * t1, my + dy * length * t1, 1.4f);
        }
    }

    public static void drawThrowables(SpriteBatch batch, World world) {
        for (World.Throwable t : world.throwables) {
            Texture tex = Art.throwable(t.kind);
            float s = C.TILE * 0.7f;
            batch.draw(tex, t.x - s / 2f, t.y - s / 2f, s, s);
        }
    }

    /** 摄像机：世界坐标视口。 */
    public static final class Camera {
        public float x, y, w, h;
    }
}
