package com.neuroshoot.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.neuroshoot.world.C;

import java.util.ArrayList;
import java.util.List;

/**
 * 粒子特效：枪口焰、弹道火花、命中血雾、爆炸烟尘。
 *
 * 全部用 ShapeRenderer 的圆与线段绘制，不需要贴图，成本低且在手机上稳定。
 * 粒子寿命短、数量设上限，避免混战时拖慢帧率。
 */
public final class Effects {
    public enum Kind { MUZZLE, FLAME, SPARK, BLOOD, SMOKE, RING, CASING }

    public static final class P {
        public float x, y, vx, vy;
        public float life, maxLife;
        public float size;
        public Kind kind;
        public Color color;
    }

    private static final int MAX = 420;
    private final List<P> particles = new ArrayList<>();

    /**
     * 枪口焰：朝枪口方向喷射的锥形火焰 + 几粒火星，不再画圆球。
     */
    public void spawnMuzzle(float x, float y, float angle) {
        // 主焰：沿枪口方向快速前冲的短条
        for (int i = 0; i < 4; i++) {
            P p = obtain();
            if (p == null) return;
            float a = angle + (float) (Math.random() - 0.5) * 0.42f;
            float sp = 220f + (float) Math.random() * 260f;
            p.x = x;
            p.y = y;
            p.vx = (float) Math.cos(a) * sp;
            p.vy = (float) Math.sin(a) * sp;
            p.life = p.maxLife = 0.06f + (float) Math.random() * 0.05f;
            p.size = 7f + (float) Math.random() * 5f;
            p.kind = Kind.FLAME;
            p.color = new Color(1f, 0.90f, 0.54f, 1f);
        }
        // 内焰：更亮更短
        P inner = obtain();
        if (inner != null) {
            inner.x = x;
            inner.y = y;
            inner.vx = (float) Math.cos(angle) * 150f;
            inner.vy = (float) Math.sin(angle) * 150f;
            inner.life = inner.maxLife = 0.05f;
            inner.size = 9f;
            inner.kind = Kind.FLAME;
            inner.color = new Color(1f, 0.99f, 0.86f, 1f);
        }
    }

    public void spawnSparks(float x, float y, int count) {
        for (int i = 0; i < count; i++) {
            P p = obtain();
            if (p == null) return;
            float a = (float) (Math.random() * Math.PI * 2);
            float sp = 70f + (float) Math.random() * 210f;
            p.x = x;
            p.y = y;
            p.vx = (float) Math.cos(a) * sp;
            p.vy = (float) Math.sin(a) * sp;
            p.life = p.maxLife = 0.12f + (float) Math.random() * 0.16f;
            p.size = 1.8f + (float) Math.random() * 2.2f;
            p.kind = Kind.SPARK;
            p.color = new Color(1f, 0.78f, 0.34f, 1f);
        }
    }

    public void spawnBlood(float x, float y, float angle, int count) {
        for (int i = 0; i < count; i++) {
            P p = obtain();
            if (p == null) return;
            float a = angle + (float) (Math.random() - 0.5) * 1.5f;
            float sp = 60f + (float) Math.random() * 160f;
            p.x = x;
            p.y = y;
            p.vx = (float) Math.cos(a) * sp;
            p.vy = (float) Math.sin(a) * sp;
            p.life = p.maxLife = 0.18f + (float) Math.random() * 0.2f;
            p.size = 2.2f + (float) Math.random() * 2.6f;
            p.kind = Kind.BLOOD;
            p.color = new Color(0.72f, 0.09f, 0.1f, 1f);
        }
    }

    public void spawnSmoke(float x, float y, int count) {
        for (int i = 0; i < count; i++) {
            P p = obtain();
            if (p == null) return;
            float a = (float) (Math.random() * Math.PI * 2);
            float sp = 20f + (float) Math.random() * 80f;
            p.x = x;
            p.y = y;
            p.vx = (float) Math.cos(a) * sp;
            p.vy = (float) Math.sin(a) * sp;
            p.life = p.maxLife = 0.6f + (float) Math.random() * 0.8f;
            p.size = 10f + (float) Math.random() * 18f;
            p.kind = Kind.SMOKE;
            p.color = new Color(0.66f, 0.68f, 0.72f, 1f);
        }
    }

    /** 抛壳：向侧后方弹出的小弹壳，带旋转与重力感。 */
    public void spawnCasing(float x, float y, float angle) {
        P p = obtain();
        if (p == null) return;
        // 从枪身右侧弹出：垂直于瞄准方向
        float perp = angle + (float) Math.PI / 2f;
        p.x = x;
        p.y = y;
        p.vx = (float) Math.cos(perp) * (60f + (float) Math.random() * 60f);
        p.vy = (float) Math.sin(perp) * (60f + (float) Math.random() * 60f) + 40f;
        p.life = p.maxLife = 0.5f;
        p.size = 5f;
        p.kind = Kind.CASING;
        p.color = new Color(0.86f, 0.70f, 0.34f, 1f);
    }

    public void spawnRing(float x, float y, float size) {
        P p = obtain();
        if (p == null) return;
        p.x = x;
        p.y = y;
        p.vx = 0f;
        p.vy = 0f;
        p.life = p.maxLife = 0.34f;
        p.size = size;
        p.kind = Kind.RING;
        p.color = new Color(1f, 0.72f, 0.36f, 1f);
    }

    private P obtain() {
        if (particles.size() >= MAX) return null;
        P p = new P();
        particles.add(p);
        return p;
    }

    public void update(float dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            P p = particles.get(i);
            p.life -= dt;
            if (p.life <= 0f) {
                particles.remove(i);
                continue;
            }
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            if (p.kind == Kind.CASING) {
                p.vy -= 420f * dt;
                p.vx *= (1f - 2.2f * dt);
            } else if (p.kind == Kind.SMOKE) {
                p.vx *= (1f - 1.6f * dt);
                p.vy *= (1f - 1.6f * dt);
            } else {
                p.vx *= (1f - 5f * dt);
                p.vy *= (1f - 5f * dt);
            }
        }
    }

    public void draw(ShapeRenderer sr) {
        for (P p : particles) {
            float t = Math.max(0f, p.life / p.maxLife);
            switch (p.kind) {
                case RING: {
                    float r = p.size * (1.5f - t);
                    sr.setColor(p.color.r, p.color.g, p.color.b, t * 0.65f);
                    Draw.ring(sr, p.x, p.y, r, 3f + 4f * t, new Color(p.color.r, p.color.g,
                            p.color.b, t * 0.6f));
                    break;
                }
                case SPARK: {
                    sr.setColor(p.color.r, p.color.g, p.color.b, t);
                    float len = 5f + 7f * t;
                    float m = (float) Math.hypot(p.vx, p.vy);
                    float nx = m > 1f ? p.vx / m : 1f;
                    float ny = m > 1f ? p.vy / m : 0f;
                    sr.rectLine(p.x, p.y, p.x - nx * len, p.y - ny * len, p.size * t + 0.6f);
                    break;
                }
                case FLAME: {
                    // 沿速度方向拉长的火焰条，越燃越短
                    float m = (float) Math.hypot(p.vx, p.vy);
                    float nx = m > 1f ? p.vx / m : 1f;
                    float ny = m > 1f ? p.vy / m : 0f;
                    float len = p.size * (0.8f + t * 1.6f);
                    sr.setColor(p.color.r, p.color.g, p.color.b, t * 0.95f);
                    float wx = -ny * p.size * 0.35f * t;
                    float wy = nx * p.size * 0.35f * t;
                    sr.triangle(p.x + nx * len, p.y + ny * len,
                            p.x + wx, p.y + wy, p.x - wx, p.y - wy);
                    break;
                }
                case MUZZLE: {
                    sr.setColor(p.color.r, p.color.g, p.color.b, t);
                    sr.circle(p.x, p.y, p.size * (0.4f + t * 0.8f), 10);
                    break;
                }
                case BLOOD: {
                    sr.setColor(p.color.r, p.color.g, p.color.b, t * 0.9f);
                    sr.circle(p.x, p.y, p.size * t, 7);
                    break;
                }
                case CASING: {
                    sr.setColor(p.color.r, p.color.g, p.color.b, Math.min(1f, t * 2f));
                    float w = 4.4f, h = 1.8f;
                    float a = (1f - t) * 26f;
                    float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
                    float x1 = p.x - ca * w / 2 - sa * h / 2, y1 = p.y - sa * w / 2 + ca * h / 2;
                    float x2 = p.x + ca * w / 2 - sa * h / 2, y2 = p.y + sa * w / 2 + ca * h / 2;
                    float x3 = p.x + ca * w / 2 + sa * h / 2, y3 = p.y + sa * w / 2 - ca * h / 2;
                    float x4 = p.x - ca * w / 2 + sa * h / 2, y4 = p.y - sa * w / 2 - ca * h / 2;
                    sr.triangle(x1, y1, x2, y2, x3, y3);
                    sr.triangle(x1, y1, x3, y3, x4, y4);
                    break;
                }
                case SMOKE: {
                    sr.setColor(p.color.r, p.color.g, p.color.b, t * 0.28f);
                    sr.circle(p.x, p.y, p.size * (1.6f - t * 0.6f), 12);
                    break;
                }
                default:
                    break;
            }
        }
    }

    public void clear() {
        particles.clear();
    }
}
