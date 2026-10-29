#!/usr/bin/env python3
"""生成全套俯视射击素材。

画风对标 Scratch《MMO联机枪战》：粗黑描边、纯色平涂、高饱和、
俯视视角、物件带柔和投影。全套共用同一套图元与描边规则，
只用配色区分阵营与材质，因此跨类别也属于同一画风家族。

分类输出到 assets/art/<类别>/：
  chars/    俯视角色（8 方向 + 蹲姿 + 死亡）
  guns/     突击步枪 / 狙击枪 / 冲锋枪 / 霰弹枪 / 手枪
  throw/    手雷 / 闪光 / 烟雾 / 燃烧瓶 / 遥控炸药
  meds/     手术包 / 治疗针 / 绷带 / 止痛药
  props/    箱子 / 花盆 / 桶 / 沙袋 / 柱子 / 树 / 灌木
  tiles/    地板 / 墙 / 门（关/开/破）
"""
import pathlib

from PIL import Image, ImageDraw, ImageFilter

ROOT = pathlib.Path(__file__).resolve().parent.parent / "core" / "assets" / "art"
SS = 12
GRID = 32
PAD = 8


class Style:
    def __init__(self):
        self.outline = (24, 22, 30, 255)
        self.ow = 2
        self.shadow = (16, 28, 20, 70)

    @staticmethod
    def light(c, d=24):
        return tuple(min(255, v + d) if i < 3 else v for i, v in enumerate(c))

    @staticmethod
    def dark(c, d=32):
        return tuple(max(0, v - d) if i < 3 else v for i, v in enumerate(c))


S = Style()

# 阵营配色：玩家/队友蓝，敌人红
BLUE = (74, 150, 226, 255)
RED = (214, 76, 74, 255)
GREEN = (86, 168, 84, 255)
WOOD = (226, 200, 152, 255)
WOOD_D = (188, 154, 104, 255)
METAL = (168, 176, 190, 255)
METAL_D = (104, 112, 128, 255)
CONCRETE = (150, 146, 138, 255)


class Canvas:
    def __init__(self, size=32, pad=PAD):
        self.size = size
        self.pad = pad
        self.w = (size + pad * 2) * SS
        self.h = self.w
        self.img = Image.new("RGBA", (self.w, self.h), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)

    def p(self, x, y):
        return ((x + self.pad) * SS, (y + self.pad) * SS)

    def box(self, x0, y0, x1, y1):
        a = self.p(x0, y0)
        b = self.p(x1, y1)
        return [a[0], a[1], b[0], b[1]]

    def ow(self, m=1.0):
        return max(1, int(round(S.ow * SS * m)))

    def poly(self, seq, fill, ow=1.0):
        self.d.polygon([self.p(x, y) for x, y in seq], fill=fill,
                       outline=S.outline, width=self.ow(ow))

    def rr(self, b, r, fill, ow=1.0):
        self.d.rounded_rectangle(self.box(*b), radius=r * SS, fill=fill,
                                 outline=S.outline, width=self.ow(ow))

    def rect(self, b, fill, ow=0.0):
        self.d.rectangle(self.box(*b), fill=fill,
                         outline=S.outline if ow > 0 else None,
                         width=self.ow(ow) if ow > 0 else 0)

    def circ(self, b, fill, ow=1.0):
        self.d.ellipse(self.box(*b), fill=fill, outline=S.outline, width=self.ow(ow))

    def line(self, seq, fill, w):
        self.d.line([self.p(x, y) for x, y in seq], fill=fill,
                    width=max(1, int(w * SS)), joint="curve")

    def shade(self, b, color, offset=(0.0, 0.0), scale=1.0, alpha=255):
        """在形状内部放一块实心椭圆做受光/暗部。

        用实心而非半透明叠加：半透明叠在多个色块上会糊成一片，
        实心内缩椭圆边界干净，缩到游戏尺寸仍然清楚。
        """
        x0, y0, x1, y1 = b
        cx = (x0 + x1) * 0.5 + offset[0]
        cy = (y0 + y1) * 0.5 + offset[1]
        rx = (x1 - x0) * 0.5 * scale
        ry = (y1 - y0) * 0.5 * scale
        self.d.ellipse(self.box(cx - rx, cy - ry, cx + rx, cy + ry),
                       fill=(color[0], color[1], color[2], alpha))

    def gradient_box(self, b, color_top, color_bottom):
        """方块用的线性渐变：从上到下由亮到暗，贴合方形物体，不会出现圆块。"""
        x0, y0, x1, y1 = b
        ax, ay = self.p(x0, y0)
        bx, by = self.p(x1, y1)
        h = max(1, int(by - ay))
        for i in range(h):
            t = i / (h - 1.0)
            r = int(color_top[0] + (color_bottom[0] - color_top[0]) * t)
            g = int(color_top[1] + (color_bottom[1] - color_top[1]) * t)
            bl = int(color_top[2] + (color_bottom[2] - color_top[2]) * t)
            self.d.line([(ax, ay + i), (bx, ay + i)], fill=(r, g, bl, 255))

    def shadow(self, cx, cy, rx, ry, alpha=None):
        layer = Image.new("RGBA", (self.w, self.h), (0, 0, 0, 0))
        d = ImageDraw.Draw(layer)
        col = S.shadow if alpha is None else (S.shadow[0], S.shadow[1], S.shadow[2], alpha)
        d.ellipse(self.box(cx - rx, cy - ry, cx + rx, cy + ry), fill=col)
        self.img.alpha_composite(layer.filter(ImageFilter.GaussianBlur(1.4 * SS)))


def save(canvas, path, out_size, margin=0.0, fill=False):
    """裁剪到内容后缩放；fill=True 时按宽度铺满（枪械这类扁长物体用）。"""
    bbox = canvas.img.getbbox()
    if bbox is None:
        return
    content = canvas.img.crop(bbox)
    w, h = content.size
    if fill:
        scale = out_size * (1.0 - margin * 2) / w
    else:
        scale = out_size * (1.0 - margin * 2) / max(w, h)
    nw, nh = max(1, round(w * scale)), max(1, round(h * scale))
    content = content.resize((nw, nh), Image.LANCZOS)
    out = Image.new("RGBA", (out_size, max(out_size, nh)), (0, 0, 0, 0))
    out.alpha_composite(content, ((out_size - nw) // 2, (out.height - nh) // 2))
    path.parent.mkdir(parents=True, exist_ok=True)
    out.save(path, optimize=True)


# ---------------------------------------------------------------- 角色
# 俯视：横向肩甲 + 中央头盔 + 朝右面罩 + 前伸双臂，面朝 +x。


def char_stand(c, body, helmet, skin, crouch=False, walk=0, recoil=False, reload=0):
    """按 48 网格绘制、输出 256px 的俯视士兵（朝右）。

    层次从下往上：投影 → 背包 → 躯干护甲 → 双肩 → 双臂 → 手套 → 头盔 → 面罩。
    所有受光都用内缩的形状，绝不溢出到描边外，缩到游戏尺寸仍然干净。
    """
    k = 0.9 if crouch else 1.0
    cx, cy = 24.0, 24.0

    def X(v):
        return cx + (v - 24) * k

    def Y(v):
        return cy + (v - 24) * k

    push = -1.5 if recoil else 0.0
    c.shadow(cx, Y(36), 10 * k, 3.2 * k)

    # 背包
    c.rr((X(10), Y(20 + push), X(17), Y(28 + push)), 2.6 * k, S.dark(body, 66), 0.7)

    # 躯干护甲：圆角矩形 + 竖向渐变
    c.rr((X(14), Y(16 + push), X(31), Y(32 + push)), 6.0 * k, S.dark(body, 42), 1.0)
    c.gradient_box((X(15.6), Y(17.6 + push), X(29.4), Y(30.4 + push)),
                   S.light(body, 26), S.dark(body, 30))
    # 胸口护板
    c.rr((X(20), Y(19 + push), X(27), Y(29 + push)), 2.6 * k, S.light(body, 16), 0)
    # 护板上的三条弹匣纹
    for yy in (21.5, 24.0, 26.5):
        c.line([(X(21), Y(yy + push)), (X(26), Y(yy + push))], S.dark(body, 48), 0.35)
    # 战术背心：两侧弹匣袋
    c.rr((X(16.4), Y(21 + push), X(19.6), Y(24.4 + push)), 0.8, S.dark(body, 58), 0.45)
    c.rr((X(16.4), Y(25.6 + push), X(19.6), Y(29 + push)), 0.8, S.dark(body, 58), 0.45)
    # 腰带
    c.rr((X(15.4), Y(30.2 + push), X(29.4), Y(32.0 + push)), 0.6,
         S.dark(body, 68), 0.4)
    c.circ((X(21), Y(30.6 + push), X(23), Y(32.2 + push)), (198, 168, 92, 255), 0.3)

    # 双肩护甲
    c.circ((X(15), Y(14.5 + push), X(23), Y(22.5 + push)), S.light(body, 20), 0.7)
    c.circ((X(15), Y(25.5 + push), X(23), Y(33.5 + push)), S.light(body, 20), 0.7)
    # 肩章（小队标识）
    c.rr((X(16.4), Y(15.4 + push), X(20.2), Y(17.4 + push)), 0.5,
         (226, 196, 96, 255), 0.35)
    c.rr((X(16.4), Y(30.6 + push), X(20.2), Y(32.6 + push)), 0.5,
         (226, 196, 96, 255), 0.35)

    # 双臂：沿上下两侧水平伸出
    swing = 2.0 if walk == 1 else (-2.0 if walk == 2 else 0.0)
    reach = X(33 if recoil else 31)
    ay1 = Y(17.5 + push - swing * 0.45)
    ay2 = Y(30.5 + push + swing * 0.45)
    c.line([(X(21), Y(16.5 + push)), (reach - 2, ay1)], S.dark(skin, 34), 1.9 * k)
    c.line([(X(21), Y(31.5 + push)), (reach - 2, ay2)], S.dark(skin, 34), 1.9 * k)
    # 手套
    c.circ((reach - 2.2 * k, ay1 - 2.2 * k, reach + 2.2 * k, ay1 + 2.2 * k),
           S.dark(skin, 46), 0.4)
    c.circ((reach - 2.2 * k, ay2 - 2.2 * k, reach + 2.2 * k, ay2 + 2.2 * k),
           S.dark(skin, 46), 0.4)

    # 头盔：球面感靠「内缩一圈亮色 + 更小一圈高光」
    c.circ((X(16), Y(16 + push), X(28), Y(28 + push)), helmet, 1.0)
    c.circ((X(17.6), Y(17.6 + push), X(26.4), Y(26.4 + push)), S.light(helmet, 20), 0)
    c.circ((X(19), Y(19 + push), X(23.4), Y(23.4 + push)), S.light(helmet, 56), 0)
    # 面罩：头盔前缘的深色月牙
    c.rr((X(25.4), Y(18.6 + push), X(28.6), Y(25.4 + push)), 1.6 * k, (20, 22, 32, 255), 0.45)
    # 耳机
    c.circ((X(14.6), Y(17.4 + push), X(17.8), Y(20.6 + push)), (34, 38, 48, 255), 0.45)
    c.circ((X(14.6), Y(23.4 + push), X(17.8), Y(26.6 + push)), (34, 38, 48, 255), 0.45)
    # 头盔顶部导轨与夜视仪支架
    c.rr((X(19.4), Y(17.2 + push), X(24.6), Y(19.0 + push)), 0.5, (48, 52, 64, 255), 0.35)
    c.rr((X(21.2), Y(14.8 + push), X(23.4), Y(17.6 + push)), 0.6, (58, 62, 74, 255), 0.35)

    if recoil:
        c.circ((X(34), Y(20 + push), X(40), Y(26 + push)), (255, 236, 160, 255), 0.35)
        c.circ((X(35), Y(21 + push), X(38), Y(25 + push)), (255, 255, 242, 255), 0)

    # 换弹三帧：1 低头取弹匣 2 拔出旧弹匣 3 推入新弹匣
    if reload == 1:
        # 一只手垂下取弹匣
        c.line([(X(20), Y(30 + push)), (X(14), Y(38 + push))], S.dark(skin, 34), 1.9 * k)
        c.circ((X(11.6), Y(35.6 + push), X(16.4), Y(40.4 + push)), S.dark(skin, 46), 0.4)
        # 弹匣袋
        c.rr((X(12), Y(34), X(18), Y(40)), 1.2, S.dark(body, 52), 0.6)
    elif reload == 2:
        # 旧弹匣掉出
        c.rr((X(28), Y(32), X(33), Y(39)), 1.0, (40, 44, 56, 255), 0.7)
        c.line([(X(20), Y(30 + push)), (X(30), Y(34))], S.dark(skin, 34), 1.9 * k)
    elif reload == 3:
        # 新弹匣推入
        c.rr((X(25), Y(31), X(30), Y(38)), 1.0, (48, 52, 66, 255), 0.7)
        c.line([(X(20), Y(30 + push)), (X(27), Y(33))], S.dark(skin, 34), 1.9 * k)
        c.line([(X(27), Y(33)), (X(31), Y(31))], S.dark(skin, 46), 2.2)


def char_dead(c, body, helmet):
    """倒地：躯干压扁 + 头盔滚到一旁 + 血迹。"""
    c.shadow(24, 34, 15, 5)
    c.circ((6, 18, 40, 36), (110, 26, 30, 110), 0)
    c.rr((8, 20, 32, 34), 6, S.dark(body, 40), 1.0)
    c.circ((30, 16, 44, 30), helmet, 1.0)
    c.circ((33, 19, 41, 27), S.dark(helmet, 60), 0.7)


# ---------------------------------------------------------------- 枪械（俯视，朝 +x）
#
# 俯视枪械的读图要点：机匣是主体、枪管向前、弹匣垂在下方、握把在机匣尾部。
# 统一用「深色金属 + 一条亮色导轨」配色，与角色同属一套描边规则。


def gun_body(c, x0, x1, y0, y1, r=1.6):
    c.rr((x0, y0, x1, y1), r, (58, 62, 74, 255))


def gun_barrel(c, x0, x1, y=15.4, h=1.8):
    c.rr((x0, y, x1, y + h), 0.8, METAL_D, 0.7)
    c.line([(x0, y + 0.4), (x1 - 1, y + 0.4)], (210, 216, 228, 255), 0.5)


def gun_rail(c, x0, x1, y):
    c.line([(x0, y), (x1, y)], (250, 200, 78, 255), 0.9)


def gun_rifle(c, barrel=20, scope=False, drum=False):
    gun_body(c, 3, 12 + barrel, 12.5, 19.5)
    gun_barrel(c, 11 + barrel, 17 + barrel)
    c.rr((1, 10, 8, 22), 1.6, (44, 48, 60, 255), 0.9)          # 枪托
    c.rr((9, 19, 15, 26), 1.8, (40, 44, 56, 255), 0.9)          # 弹匣
    if drum:
        c.circ((6, 19, 20, 31), (46, 50, 62, 255), 0.9)
        c.circ((10, 22, 16, 28), (72, 78, 92, 255), 0.5)
    c.rr((5, 19, 10, 27), 1.6, (36, 40, 52, 255), 0.9)          # 握把
    if scope:
        c.rr((8, 8, 20, 12.5), 1.4, METAL_D, 0.8)
        c.circ((18, 8, 23, 13), (34, 38, 48, 255), 0.7)
    gun_rail(c, 6, 10 + barrel, 16)


def gun_smg(c):
    gun_body(c, 5, 24, 13, 19)
    gun_barrel(c, 23, 29)
    c.rr((1, 11.5, 6, 20.5), 1.4, (44, 48, 60, 255), 0.9)
    c.rr((9, 18.5, 14, 26.5), 1.6, (40, 44, 56, 255), 0.9)
    c.rr((6, 18.5, 10, 25), 1.4, (36, 40, 52, 255), 0.9)
    gun_rail(c, 7, 22, 15.6)


def gun_sniper(c):
    gun_body(c, 4, 27, 13.5, 19)
    gun_barrel(c, 25, 35)
    c.rr((1, 11, 9, 21), 1.6, (42, 46, 58, 255), 0.9)
    c.rr((7, 7.5, 24, 12), 1.8, METAL_D, 0.9)                   # 瞄准镜
    c.circ((22, 7.5, 28, 12), (34, 38, 48, 255), 0.7)
    c.line([(10, 9.8), (21, 9.8)], (250, 236, 180, 255), 0.5)
    c.rr((11, 18.5, 15, 25), 1.4, (36, 40, 52, 255), 0.9)
    c.line([(24, 19), (21, 28)], METAL, 0.9)                    # 两脚架
    c.line([(24, 19), (27, 28)], METAL, 0.9)
    gun_rail(c, 6, 24, 16.2)


def gun_shotgun(c):
    c.rr((3, 12, 19, 20), 1.8, (86, 66, 48, 255), 1.0)          # 木质机匣
    c.rr((17, 12.8, 32, 19.2), 1.6, METAL, 0.9)                 # 双管
    c.line([(17, 16), (32, 16)], (60, 64, 74, 255), 0.7)
    c.rr((1, 10.5, 8, 21.5), 1.6, (70, 52, 38, 255), 0.9)
    c.rr((10, 19, 17, 24.5), 1.8, WOOD, 0.9)                    # 前护木
    gun_rail(c, 10, 20, 15.2)


def gun_pistol(c):
    c.rr((7, 13, 21, 19.5), 1.8, (58, 62, 74, 255), 0.9)
    gun_barrel(c, 20, 26, 15.2, 1.6)
    c.rr((8, 19, 13, 26), 1.6, (36, 40, 52, 255), 0.9)
    gun_rail(c, 9, 19, 15.8)


def gun_generic(c, kind, length=1.0, stock=1, optic=0, mag="std", bullpup=False,
                color=None):
    """参数化枪械：用「枪型 + 长度 + 枪托 + 瞄具 + 弹匣」拼出每把枪的独特外形。

    这样 58 把枪都能有自己的贴图，又共用同一套绘制规则，风格保持一致。
    """
    body_c = color if color else (58, 62, 74, 255)
    # 统一放大 1.5 倍并下移居中，让枪在 256px 贴图里占满、更清晰
    sc = 1.5

    def sx(v):
        return 6 + (v - 6) * sc
    barrel_len = 9 + int(11 * length)
    rear = 3.4 if bullpup else 2.0

    # 机匣
    c.rr((rear, 12.5, 11 + barrel_len * 0.55, 19.5), 1.6, body_c)
    # 枪管
    c.rr((11 + barrel_len * 0.55, 15.2, 14 + barrel_len, 17.6), 0.8, METAL_D, 0.7)
    c.line([(12 + barrel_len * 0.55, 15.6), (13 + barrel_len, 15.6)],
           (212, 218, 230, 255), 0.45)

    # 枪托
    if stock == 1:
        c.rr((0.6, 10.5, rear + 1.2, 21.0), 1.4, S.dark(body_c, 24), 0.85)
    elif stock == 2:
        # 折叠/镂空托
        c.line([(1.2, 12.0), (rear, 15.0)], S.dark(body_c, 20), 1.5)
        c.line([(1.2, 19.0), (rear, 16.0)], S.dark(body_c, 20), 1.5)
        c.line([(1.2, 12.0), (1.2, 19.0)], S.dark(body_c, 20), 1.3)
    elif stock == 3:
        # 无托：只有一小段后盖
        c.rr((0.4, 12.0, rear, 20.0), 1.0, S.dark(body_c, 30), 0.7)

    # 弹匣
    if mag == "drum":
        c.circ((6.5, 18.5, 19.5, 30.5), (44, 48, 60, 255), 0.9)
        c.circ((10, 22, 16, 27), (72, 78, 92, 255), 0.45)
    elif mag == "long":
        c.rr((8.5, 19, 13.5, 29), 1.5, (40, 44, 56, 255), 0.85)
    elif mag == "box":
        c.rr((8.0, 19, 13.0, 26.5), 1.6, (40, 44, 56, 255), 0.85)
    elif mag == "tube":
        c.rr((13.0, 18.6, 12 + barrel_len * 0.75, 20.4), 0.8, METAL_D, 0.7)

    # 握把
    c.rr((5.0, 19.0, 9.2, 27.0), 1.4, (34, 38, 48, 255), 0.85)

    # 瞄具：1 机械 2 红点 3 高倍
    if optic == 1:
        c.rr((9.0, 10.4, 12.2, 12.6), 0.6, METAL_D, 0.5)
    elif optic == 2:
        c.rr((8.6, 9.6, 15.4, 12.6), 1.1, METAL_D, 0.75)
        c.circ((13.4, 9.8, 16.8, 12.4), (30, 34, 44, 255), 0.5)
    elif optic == 3:
        c.rr((7.6, 8.4, 20.0, 12.4), 1.3, METAL_D, 0.8)
        c.circ((18.0, 8.4, 22.6, 12.4), (32, 36, 46, 255), 0.6)
        c.line([(9.5, 10.4), (17.5, 10.4)], (250, 236, 180, 255), 0.42)

    # 导轨装饰
    c.line([(rear + 1.5, 16.4), (9.5 + barrel_len * 0.5, 16.4)],
           (250, 200, 78, 255), 0.8)

    # 枪型专属细节
    if kind == "lmg":
        c.rr((9.0, 7.4, 16.0, 10.0), 0.8, METAL_D, 0.6)
        c.line([(12.0, 17.0), (12.0, 24.0)], METAL, 1.0)
        c.line([(6.0, 24.0), (18.0, 24.0)], METAL, 1.0)
    elif kind == "sniper":
        c.line([(20.0, 17.6), (17.5, 25.5)], METAL, 0.9)
        c.line([(20.0, 17.6), (22.5, 25.5)], METAL, 0.9)
        c.rr((13 + barrel_len, 14.4, 15 + barrel_len, 18.4), 0.6, (40, 44, 56, 255), 0.7)
    elif kind == "shotgun":
        c.rr((11.0, 18.4, 11 + barrel_len * 0.7, 20.6), 0.9, WOOD, 0.85)
    elif kind == "dmr":
        c.rr((11.5, 17.6, 14 + barrel_len * 0.8, 21.0), 1.2, WOOD, 0.8)
    elif kind == "pistol":
        pass


def gun_lmg(c):
    """轻机枪：厚重机匣 + 下方弹鼓 + 提把。"""
    gun_body(c, 3, 26, 12, 20)
    gun_barrel(c, 25, 34)
    c.rr((1, 10, 9, 22), 1.6, (44, 48, 60, 255), 0.9)
    c.circ((8, 18, 24, 33), (40, 44, 56, 255), 0.9)      # 弹鼓
    c.circ((12, 22, 20, 29), (66, 72, 86, 255), 0.5)
    c.rr((5, 18, 10, 26), 1.6, (34, 38, 48, 255), 0.9)
    c.rr((10, 6, 20, 10), 1.4, METAL_D, 0.8)             # 提把
    gun_rail(c, 6, 24, 16)


def gun_dmr(c):
    """精确射手步枪：长枪管 + 中等瞄具。"""
    gun_body(c, 4, 22, 13, 19.5)
    gun_barrel(c, 21, 34)
    c.rr((1, 11, 8, 21.5), 1.6, (44, 48, 60, 255), 0.9)
    c.rr((7, 20, 12, 27), 1.6, (40, 44, 56, 255), 0.9)
    c.rr((8, 8.5, 22, 12.5), 1.5, METAL_D, 0.8)          # 瞄具
    c.circ((20, 8.5, 25, 13), (36, 40, 50, 255), 0.6)
    gun_rail(c, 6, 20, 16)


def gun_double_barrel(c):
    """双管霰弹：两根并排枪管。"""
    c.rr((3, 12, 18, 20), 1.8, (92, 70, 50, 255), 1.0)
    c.rr((16, 13.2, 34, 16.2), 1.2, METAL, 0.85)         # 上管
    c.rr((16, 17.0, 34, 20.0), 1.2, METAL, 0.85)         # 下管
    c.line([(17, 14.7), (33, 14.7)], (210, 216, 228, 255), 0.4)
    c.line([(17, 18.5), (33, 18.5)], (196, 202, 214, 255), 0.4)
    c.rr((1, 10.5, 7, 21.5), 1.6, (70, 52, 38, 255), 0.9)
    c.rr((9, 19.5, 16, 25), 1.8, WOOD, 0.9)
    gun_rail(c, 9, 18, 15.2)


def gun_heavy_pistol(c):
    """重型手枪：更大的机匣与滑套，枪口带制退器。"""
    c.rr((6, 12, 22, 20), 1.8, (54, 58, 70, 255), 0.9)
    c.rr((7, 13.4, 21, 16.4), 1.4, METAL_D, 0.7)          # 滑套
    c.rr((21, 13.8, 28, 18.4), 1.4, METAL, 0.8)
    c.rr((25, 12.6, 29, 19.6), 1.0, (40, 44, 56, 255), 0.7)  # 制退器
    c.rr((7, 20, 13, 27.5), 1.6, (34, 38, 48, 255), 0.9)
    gun_rail(c, 9, 19.5, 16.2)


# ---------------------------------------------------------------- 投掷物


def throw_grenade(c):
    c.shadow(16, 28, 9, 3)
    c.circ((6, 8, 26, 28), (86, 132, 76, 255))
    c.circ((11, 13, 21, 23), S.light(GREEN, 26), 0.6)
    c.rr((12, 3, 20, 10), 2, METAL)
    c.line([(20, 6), (25, 3)], (250, 200, 78, 255), 1)


def throw_flash(c):
    c.shadow(16, 28, 9, 3)
    c.circ((6, 8, 26, 28), METAL)
    c.rr((12, 3, 20, 10), 2, (250, 200, 78, 255))
    for y in (14, 19, 24):
        c.line([(10, y), (22, y)], METAL_D, 0.7)


def throw_smoke(c):
    c.shadow(16, 28, 8, 3)
    c.rr((7, 8, 25, 30), 4, (176, 180, 186, 255))
    c.rr((9, 15, 23, 20), 1, METAL_D, 0.6)
    c.rr((13, 2, 19, 9), 1, METAL)


def throw_molotov(c):
    c.shadow(16, 28, 9, 3)
    c.poly([(8, 13), (20, 9), (26, 12), (24, 28), (10, 30), (7, 22)], (232, 148, 72, 255))
    c.rr((16, 2, 23, 11), 1, METAL)
    c.line([(19, 4), (21, 2)], (250, 120, 60, 255), 1.2)


def throw_c4(c):
    c.shadow(16, 27, 10, 3)
    c.rr((5, 10, 27, 26), 2, (196, 186, 150, 255))
    c.rr((9, 13, 23, 19), 1, (60, 60, 66, 255), 0.6)
    c.circ((17, 19, 22, 24), (220, 70, 70, 255), 0.6)


# ---------------------------------------------------------------- 药品


def med_surgery(c):
    c.shadow(16, 28, 11, 3)
    c.rr((3, 8, 29, 28), 3, (238, 238, 242, 255))
    c.rr((14, 11, 18, 25), 1, (220, 64, 64, 255), 0.7)
    c.rr((9, 16, 23, 20), 1, (220, 64, 64, 255), 0.7)
    c.line([(5, 10), (27, 26)], (200, 200, 208, 255), 0.8)


def med_syringe(c):
    c.shadow(16, 28, 10, 3)
    c.rr((7, 13, 25, 20), 2, (240, 244, 250, 255))
    c.rr((9, 15, 20, 18), 1, (120, 220, 160, 255), 0)
    c.line([(25, 16), (31, 16)], METAL, 1)
    c.rr((3, 12, 8, 21), 1, (90, 96, 110, 255))


def med_bandage(c):
    c.shadow(16, 28, 10, 3)
    c.circ((5, 9, 27, 27), (244, 240, 228, 255))
    c.circ((11, 15, 21, 21), (226, 218, 198, 255), 0.6)
    c.line([(9, 12), (23, 24)], (214, 204, 182, 255), 0.8)


def med_painkiller(c):
    c.shadow(16, 28, 9, 3)
    c.rr((8, 9, 24, 28), 4, (250, 250, 252, 255))
    c.rr((9, 10, 23, 19), 3, (96, 176, 236, 255), 0.7)
    for y in (21, 24):
        c.line([(11, y), (21, y)], (180, 186, 196, 255), 0.8)


# ---------------------------------------------------------------- 道具


def prop_crate(c):
    """木箱：竖向木板 + 对角加固条 + 四角铁皮与螺栓。"""
    c.shadow(16, 29.5, 14.5, 3.6)
    c.rr((3, 4, 29, 29), 2.6, WOOD, 1.0)
    # 木板分条：内侧三条浅色板 + 两条缝
    for x in (9.6, 16.0, 22.4):
        c.line([(x, 5.2), (x, 27.8)], S.dark(WOOD, 26), 0.55)
    c.line([(4.2, 26.6), (27.8, 26.6)], S.light(WOOD, 22), 0.5)
    # 对角加固条
    c.line([(5.6, 6.4), (26.4, 26.6)], WOOD_D, 2.0)
    c.line([(26.4, 6.4), (5.6, 26.6)], WOOD_D, 2.0)
    # 整体受光：竖向渐变，贴合箱体方形轮廓
    c.gradient_box((4.6, 5.0, 27.4, 28.2),
                   S.light(WOOD, 30), S.dark(WOOD, 26))
    # 四角铁皮
    for (bx, by) in ((3.4, 4.4), (23.2, 4.4), (3.4, 22.6), (23.2, 22.6)):
        c.rr((bx, by, bx + 5.4, by + 5.4), 1.0, METAL_D, 0.55)
    # 螺栓
    for (nx, ny) in ((6.0, 7.0), (25.6, 7.0), (6.0, 25.4), (25.6, 25.4)):
        c.circ((nx - 0.7, ny - 0.7, nx + 0.7, ny + 0.7), S.light(METAL, 30), 0.3)


def prop_crate_damaged(c):
    """受损木箱：保留结构，木板开裂并有缺口。"""
    c.shadow(16, 29.5, 14.5, 3.4)
    c.rr((3, 4, 29, 29), 2.6, (210, 184, 138, 255), 1.0)
    for x in (9.6, 16.0, 22.4):
        c.line([(x, 5.2), (x, 27.8)], S.dark(WOOD, 30), 0.55)
    c.line([(5.6, 6.4), (26.4, 26.6)], WOOD_D, 1.9)
    c.line([(26.4, 6.4), (5.6, 26.6)], WOOD_D, 1.6)
    for (bx, by) in ((3.4, 4.4), (23.2, 22.6)):
        c.rr((bx, by, bx + 5.4, by + 5.4), 1.0, METAL_D, 0.55)
    # 裂口
    c.poly([(9, 4), (14.5, 11), (11, 16.5), (17.5, 20), (13, 28.5), (7.5, 22)],
           (140, 112, 72, 255), 0.6)
    c.line([(19.5, 5), (24.5, 12.5)], (108, 82, 54, 255), 0.9)
    c.line([(22, 21), (27, 25)], (108, 82, 54, 255), 0.7)


def prop_crate_broken(c):
    c.shadow(16, 29, 13, 3)
    c.poly([(4, 24), (12, 18), (20, 26), (28, 20), (26, 29), (6, 29)], (150, 120, 78, 255), 0.8)
    c.poly([(7, 26), (10, 14), (16, 22), (13, 28)], WOOD, 0.7)
    c.poly([(19, 27), (24, 15), (29, 24)], WOOD_D, 0.7)


def prop_planter(c):
    c.shadow(16, 29, 12, 3)
    c.rr((6, 14, 26, 29), 2, WOOD)
    c.rr((8, 16, 24, 27), 1, WOOD_D, 0.6)
    c.circ((4, 2, 28, 24), (58, 140, 72, 255))
    c.circ((11, 8, 21, 18), (86, 176, 96, 255), 0.6)


def prop_barrel(c):
    c.shadow(16, 29, 12, 3)
    c.circ((4, 4, 28, 28), (232, 148, 72, 255))
    c.circ((9, 9, 23, 23), (250, 176, 96, 255), 0.6)
    c.circ((13, 13, 19, 19), METAL, 0.7)


def prop_sandbag(c):
    c.shadow(16, 29, 14, 3)
    c.rr((2, 12, 20, 30), 6, (198, 178, 132, 255))
    c.rr((12, 3, 30, 21), 6, (216, 198, 152, 255))


def prop_pillar(c):
    c.shadow(16, 29, 10, 3)
    c.circ((4, 4, 28, 28), CONCRETE)
    c.circ((9, 9, 23, 23), S.light(CONCRETE, 18), 0.7)


def prop_tree(c):
    """树：树干加分层树冠，靠多圈明暗叠加做出体积，边缘有叶簇起伏。"""
    c.shadow(16, 30, 13.5, 3.4)
    # 树干
    c.rr((13.6, 20, 18.4, 30.5), 1.6, (128, 96, 62, 255), 0.9)
    c.line([(15.2, 21), (15.2, 30)], (104, 76, 48, 255), 0.5)
    dark = (44, 116, 58, 255)
    mid = (58, 140, 70, 255)
    light = (86, 176, 92, 255)
    # 外圈叶簇：用多个错位圆形成起伏边缘
    for (cx_, cy_, r_) in ((10.0, 15.0, 8.2), (20.0, 15.4, 8.0), (15.0, 20.0, 8.0),
                           (7.6, 20.0, 6.2), (22.6, 20.2, 6.0), (15.0, 9.2, 7.4),
                           (8.4, 10.4, 5.4), (21.8, 10.6, 5.2)):
        c.circ((cx_ - r_, cy_ - r_, cx_ + r_, cy_ + r_), dark, 0.85)
    # 中层
    for (cx_, cy_, r_) in ((11.0, 16.0, 6.4), (19.4, 16.4, 6.2), (15.2, 21.2, 6.0),
                           (15.0, 11.0, 6.0)):
        c.circ((cx_ - r_, cy_ - r_, cx_ + r_, cy_ + r_), mid, 0)
    # 高光簇
    for (cx_, cy_, r_) in ((12.4, 17.6, 3.4), (18.6, 13.4, 3.0), (15.4, 19.4, 2.6)):
        c.circ((cx_ - r_, cy_ - r_, cx_ + r_, cy_ + r_), light, 0)
    # 整体受光：左上亮、右下暗，树冠更有体积
    c.shade((6.0, 7.0, 26.0, 25.0), (104, 190, 108, 255),
            offset=(-2.4, 2.4), scale=0.72)
    # 叶脉点缀
    for (px_, py_) in ((10.4, 13.2), (19.8, 18.6), (14.2, 8.4), (21.6, 12.0)):
        c.circ((px_ - 0.8, py_ - 0.8, px_ + 0.8, py_ + 0.8), (36, 96, 48, 255), 0)


def prop_bush(c):
    c.shadow(16, 28, 12, 3)
    c.circ((3, 9, 21, 27), (58, 140, 72, 255))
    c.circ((13, 7, 29, 25), (58, 140, 72, 255))
    c.circ((8, 5, 24, 21), (86, 176, 96, 255), 0.6)


# ---------------------------------------------------------------- 图块


def tile_floor(c):
    """室内地砖：四块方砖 + 十字缝 + 细分网格线，铺开后像精细网格地面。"""
    base = (150, 146, 136, 255)
    grout = (108, 104, 96, 255)
    c.rr((0, 0, 32, 32), 0, grout, 0)
    c.gradient_box((0, 0, 32, 32), S.light(grout, 12), S.dark(grout, 10))
    for gy in (0, 16):
        for gx in (0, 16):
            c.rr((gx + 1.0, gy + 1.0, gx + 15.0, gy + 15.0), 0.8, base, 0)
    # 每块砖的受光边与暗边
    for gy in (0, 16):
        for gx in (0, 16):
            c.line([(gx + 1.4, gy + 1.4), (gx + 14.6, gy + 1.4)],
                   S.light(base, 22), 0.4)
            c.line([(gx + 1.4, gy + 14.6), (gx + 14.6, gy + 14.6)],
                   S.dark(base, 26), 0.4)
    # 细分网格：把每块砖再切成 4×4 的小格，铺开后形成精细网格质感
    fine = S.dark(base, 10)
    for k in range(1, 4):
        off = k * 4.0
        c.line([(off, 1.0), (off, 31.0)], fine, 0.18)
        c.line([(17.0 + off, 1.0), (17.0 + off, 31.0)], fine, 0.18)
        c.line([(1.0, off), (31.0, off)], fine, 0.18)
        c.line([(1.0, 17.0 + off), (31.0, 17.0 + off)], fine, 0.18)
    # 细微磨损点，打破单调
    for (px_, py_) in ((5.2, 8.4), (12.6, 4.6), (21.4, 12.2), (26.8, 22.6), (7.4, 25.2)):
        c.circ((px_ - 0.6, py_ - 0.6, px_ + 0.6, py_ + 0.6), S.dark(base, 16), 0)


def tile_floor_wood(c):
    """木地板：长条木板错缝铺设，可无缝平铺。"""
    base = (156, 122, 82, 255)
    c.rr((0, 0, 32, 32), 0, base, 0)
    for i, y in enumerate((0, 8, 16, 24)):
        c.rr((0, y + 0.6, 32, y + 7.4), 0, S.light(base, 12) if i % 2 == 0 else base, 0)
        c.line([(0, y + 0.4), (32, y + 0.4)], S.dark(base, 30), 0.45)
    # 竖缝错位
    for i, y in enumerate((0, 8, 16, 24)):
        x = 10 if i % 2 == 0 else 22
        c.line([(x, y + 0.6), (x, y + 7.4)], S.dark(base, 24), 0.4)
    # 木纹
    for y in (3.2, 11.6, 20.4, 27.8):
        c.line([(1, y), (30, y)], S.dark(base, 12), 0.3)


def tile_wall(c):
    """墙体：整条连续，厚度只占格子中部，上下留出立面高光与阴影。

    主体填满整格宽度，横向平铺时完全无缝，连成一整条；纵向只有中间一条是
    「实墙」，上下两段是墙脚/墙顶的明暗，因此看起来薄、像一堵墙而不是大方块。
    """
    face = CONCRETE
    c.rr((0, 0, 32, 32), 0, S.dark(CONCRETE, 52), 0.0)
    # 墙体主体：中部一条
    c.rect((0, 8.5, 32, 22.5), face)
    # 墙顶受光
    c.rect((0, 22.5, 32, 24.5), S.light(CONCRETE, 34))
    # 墙角阴影
    c.rect((0, 6.5, 32, 8.5), S.dark(CONCRETE, 34))
    # 砖缝：竖向细缝 + 中部一条横向缝，平铺后形成砖墙
    for x in (8.0, 20.0):
        c.line([(x, 8.5), (x, 22.5)], S.dark(CONCRETE, 22), 0.35)
    c.line([(0, 15.5), (32, 15.5)], S.dark(CONCRETE, 18), 0.3)
    # 只在墙体外沿描边，横向平铺时不出现格子横线
    c.line([(0, 25.0), (32, 25.0)], S.outline, 0.55)
    c.line([(0, 6.0), (32, 6.0)], S.outline, 0.55)


def tile_wall_v(c):
    """竖向墙：与横向墙同构，只是把主体转到竖直方向，用于建筑左右两侧。"""
    face = CONCRETE
    c.rr((0, 0, 32, 32), 0, S.dark(CONCRETE, 52), 0.0)
    c.rect((8.5, 0, 22.5, 32), face)
    c.rect((22.5, 0, 24.5, 32), S.light(CONCRETE, 34))
    c.rect((6.5, 0, 8.5, 32), S.dark(CONCRETE, 34))
    for y in (8.0, 20.0):
        c.line([(8.5, y), (22.5, y)], S.dark(CONCRETE, 22), 0.35)
    c.line([(15.5, 0), (15.5, 32)], S.dark(CONCRETE, 18), 0.3)
    c.line([(25.0, 0), (25.0, 32)], S.outline, 0.55)
    c.line([(6.0, 0), (6.0, 32)], S.outline, 0.55)


def tile_stair(c):
    """楼梯：带立体感的台阶 + 两侧扶手，从下往上逐级收窄表示上升。"""
    stone = (156, 140, 116, 255)
    stone_light = S.light(stone, 30)
    stone_dark = S.dark(stone, 40)

    # 底层：楼梯间地面
    c.rr((0, 0, 32, 32), 0, S.dark(stone, 18), 0)
    c.gradient_box((1, 1, 31, 31), S.light(stone, 10), S.dark(stone, 24))

    # 台阶：6 级，每级有受光面与阴影面
    steps = 6
    for i in range(steps):
        y0 = 2.2 + i * (28.0 / steps)
        y1 = y0 + (28.0 / steps) - 0.5
        # 越往上越窄，形成透视
        inset = i * 0.7
        c.rr((3.0 + inset, y0, 29.0 - inset, y1), 0.6, stone, 0.55)
        # 踏面受光
        c.rect((3.6 + inset, y1 - 1.1, 28.4 - inset, y1), stone_light)
        # 踢面阴影
        c.rect((3.6 + inset, y0, 28.4 - inset, y0 + 0.8), stone_dark)

    # 两侧扶手
    c.rr((1.4, 1.4, 3.4, 30.6), 0.8, S.dark(stone, 52), 0.7)
    c.rr((28.6, 1.4, 30.6, 30.6), 0.8, S.dark(stone, 52), 0.7)
    c.line([(2.4, 2.0), (2.4, 30.0)], S.light(stone, 46), 0.5)
    c.line([(29.6, 2.0), (29.6, 30.0)], S.light(stone, 46), 0.5)

    # 向上的箭标：一条折线 + 尖端
    c.line([(16, 25), (16, 12)], S.light(stone, 70), 1.2)
    c.line([(12.5, 15.5), (16, 11.5)], S.light(stone, 70), 1.2)
    c.line([(19.5, 15.5), (16, 11.5)], S.light(stone, 70), 1.2)


def tile_grass(c):
    """室外草地：底色 + 细密网格 + 草叶点缀，避免大面积纯色显得空。"""
    base = (96, 132, 88, 255)
    c.rr((0, 0, 32, 32), 0, base, 0)
    fine = S.dark(base, 12)
    for k in range(1, 4):
        off = k * 8.0
        c.line([(off, 0), (off, 32)], fine, 0.16)
        c.line([(0, off), (32, off)], fine, 0.16)
    # 草叶
    for (gx, gy) in ((6.4, 11.2), (19.8, 7.6), (12.2, 23.4), (25.6, 18.8), (8.8, 28.2)):
        c.line([(gx, gy), (gx + 1.4, gy + 3.0)], S.light(base, 20), 0.5)
        c.line([(gx + 1.6, gy + 0.6), (gx + 2.8, gy + 3.2)], S.light(base, 14), 0.45)


def tile_floor_upper(c):
    c.rr((0, 0, 32, 32), 0, (108, 94, 78, 255), 0)
    c.rr((2, 2, 30, 30), 0, (124, 108, 90, 255), 0)


def tile_door_closed(c):
    c.rr((2, 1, 30, 31), 1, WOOD_D, 0.8)
    c.rr((4, 3, 28, 29), 1, WOOD, 0)
    c.circ((22, 14, 27, 19), (250, 200, 78, 255), 0.7)


def tile_door_open(c):
    c.rr((2, 1, 8, 31), 1, WOOD_D, 0.8)
    c.line([(6, 6), (26, 26)], (150, 120, 80, 255), 1)


def tile_door_half(c):
    c.rr((2, 1, 30, 31), 1, WOOD_D, 0.8)
    c.rr((4, 3, 28, 29), 1, WOOD, 0)
    c.poly([(6, 3), (16, 3), (10, 29), (6, 29)], (120, 94, 62, 255), 0.5)


def tile_door_broken(c):
    c.rr((2, 1, 30, 31), 1, (110, 84, 56, 255), 0.8)
    c.poly([(6, 4), (16, 12), (10, 22), (18, 29), (6, 28)], (60, 44, 30, 255), 0.8)
    c.line([(20, 6), (28, 18)], (60, 44, 30, 255), 0.9)


# ---------------------------------------------------------------- 子弹与弹壳
#
# 子弹朝右绘制，运行时按飞行方向旋转。4 帧用于表现旋转与尾焰的细微变化，
# 弹壳单独一张，命中/抛壳时作为小碎片使用。

def bullet_round(c, frame=0, tracer=False):
    """步枪/手枪弹：铜被甲尖头弹 + 淡淡火药尾迹。"""
    tip = 30.0
    back = 8.0
    half = 3.4
    # 尾迹（随帧轻微抖动）
    wobble = 0.6 if frame % 2 == 0 else -0.4
    c.line([(back - 1, 16 + wobble), (back - 9, 16)], (255, 200, 110, 150), 1.6)
    c.line([(back - 1, 16 - wobble), (back - 7, 16)], (255, 170, 80, 110), 1.1)

    # 弹体：尖锥
    c.poly([(tip, 16), (tip - 8, 16 - half), (back, 16 - half),
            (back, 16 + half), (tip - 8, 16 + half)], (214, 168, 84, 255), 0.8)
    # 弹头尖端高光
    c.poly([(tip - 1, 16), (tip - 8, 16 - half + 0.9), (tip - 8, 16)],
           (250, 226, 160, 255), 0)
    # 被甲分界线
    c.line([(tip - 10, 16 - half), (tip - 10, 16 + half)], (150, 110, 52, 255), 0.45)
    # 弹壳底部与底火
    c.rr((back, 16 - half, back + 3.2, 16 + half), 0.8, (170, 130, 62, 255), 0.6)
    c.circ((back + 0.6, 16 - 1.2, back + 2.4, 16 + 1.2), (120, 92, 44, 255), 0.35)
    # 顶部高光条
    c.line([(back + 2, 16 - half + 1.1), (tip - 9, 16 - half + 1.1)],
           (255, 238, 186, 255), 0.55)

    if tracer:
        # 曳光弹：尾部一团橙色光
        c.circ((back - 6, 15.2, back - 1, 20.2), (255, 156, 60, 190), 0)
        c.circ((back - 4.6, 16.4, back - 2.2, 18.8), (255, 226, 140, 220), 0)


def bullet_slug(c):
    """霰弹弹丸：圆形铅弹，带一点高光。"""
    c.circ((9, 9, 25, 25), (150, 148, 156, 255), 1.0)
    c.circ((12, 12, 20, 20), (196, 196, 204, 255), 0)
    c.circ((13.4, 13.4, 17, 17), (238, 238, 244, 255), 0)


def bullet_casing(c):
    """抛出的弹壳：黄铜圆筒 + 底缘。"""
    c.rr((6, 10, 25, 15), 1.2, (206, 166, 82, 255), 0.9)
    c.rr((22, 9, 26, 16), 1.0, (168, 130, 60, 255), 0.8)
    c.line([(8, 11.2), (21, 11.2)], (246, 214, 140, 255), 0.5)
    c.circ((7.2, 11.4, 9.6, 13.6), (140, 108, 48, 255), 0.3)


# ---------------------------------------------------------------- 输出表


def main():
    # 角色：蓝队(玩家/队友) 与 红队(敌人)，站立/蹲下/死亡
    for team, body in (("blue", BLUE), ("red", RED)):
        helmet = S.light(body, 34)
        skin = (236, 198, 160, 255)
        frames = (
            ("stand", dict(crouch=False, walk=0)),
            ("walk1", dict(crouch=False, walk=1)),
            ("walk2", dict(crouch=False, walk=2)),
            ("fire", dict(crouch=False, walk=0, recoil=True)),
            ("crouch", dict(crouch=True, walk=0)),
            ("crouch_fire", dict(crouch=True, walk=0, recoil=True)),
            ("reload1", dict(crouch=False, walk=0, reload=1)),
            ("reload2", dict(crouch=False, walk=0, reload=2)),
            ("reload3", dict(crouch=False, walk=0, reload=3)),
        )
        for name, kw in frames:
            c = Canvas(48, pad=10)
            char_stand(c, body, helmet, skin, **kw)
            save(c, ROOT / "char" / f"{team}_{name}.png", 256, 0.06)
        c = Canvas(48, pad=10)
        char_dead(c, body, helmet)
        save(c, ROOT / "char" / f"{team}_dead.png", 256, 0.06)

    # 每类枪生成一张原型图，运行时按分类复用，避免 57 张图撑大包体
    # 58 把枪逐把生成独立贴图：按枪型 + 长度 + 枪托 + 瞄具 + 弹匣组合
    # (id, kind, 长度, 枪托, 瞄具, 弹匣, 无托, 机匣色)
    gun_specs = [
        # 手枪（短、无托、机械/红点）
        ("g17", "pistol", 0.24, 3, 1, "box", False, (52, 56, 68)),
        ("qsz92g", "pistol", 0.26, 3, 1, "box", False, (54, 58, 70)),
        ("deagle", "pistol", 0.36, 3, 2, "box", False, (74, 66, 52)),
        ("beretta93r", "pistol", 0.28, 3, 1, "box", False, (52, 56, 68)),
        ("g18", "pistol", 0.24, 3, 1, "long", False, (50, 54, 66)),
        ("r357", "pistol", 0.42, 3, 2, "tube", False, (72, 70, 74)),
        ("m1911", "pistol", 0.3, 3, 1, "box", False, (64, 62, 58)),
        # 霰弹枪（长管、木质、管状弹仓）
        ("m870", "shotgun", 0.72, 1, 1, "tube", False, (74, 58, 44)),
        ("m1014", "shotgun", 0.68, 1, 2, "tube", False, (66, 62, 52)),
        ("s12k", "shotgun", 0.62, 1, 2, "long", False, (60, 60, 66)),
        ("725", "shotgun", 0.78, 1, 1, "tube", False, (86, 66, 48)),
        ("fs12", "shotgun", 0.6, 2, 1, "tube", False, (58, 62, 70)),
        # 冲锋枪（短、折叠托）
        ("uzi", "smg", 0.34, 2, 1, "long", False, (52, 56, 68)),
        ("bizon", "smg", 0.4, 2, 1, "drum", False, (54, 58, 70)),
        ("pp19", "smg", 0.4, 2, 2, "long", False, (54, 58, 70)),
        ("mp5", "smg", 0.38, 2, 2, "long", False, (50, 54, 66)),
        ("smg45", "smg", 0.4, 2, 2, "long", False, (58, 56, 58)),
        ("sr3m", "smg", 0.42, 2, 3, "long", False, (46, 50, 60)),
        ("p90", "smg", 0.42, 3, 2, "long", True, (56, 60, 72)),
        ("mp7", "smg", 0.36, 2, 2, "long", False, (50, 54, 66)),
        ("vector", "smg", 0.38, 2, 2, "long", False, (48, 52, 64)),
        ("qcq171", "smg", 0.38, 2, 2, "long", False, (52, 56, 66)),
        # 突击步枪（长度递增、多样化）
        ("car15", "rifle", 0.62, 1, 1, "box", False, (56, 60, 72)),
        ("aks74u", "rifle", 0.44, 2, 1, "long", False, (62, 58, 50)),
        ("m4a1", "rifle", 0.66, 1, 2, "box", False, (56, 60, 72)),
        ("akm", "rifle", 0.7, 1, 1, "long", False, (74, 60, 46)),
        ("qbz95", "rifle", 0.68, 3, 2, "box", True, (58, 64, 60)),
        ("k416", "rifle", 0.66, 1, 2, "box", False, (52, 58, 68)),
        ("k437", "rifle", 0.6, 2, 2, "box", False, (54, 56, 62)),
        ("ak12", "rifle", 0.7, 1, 2, "long", False, (70, 58, 46)),
        ("ptr32", "rifle", 0.72, 1, 2, "long", False, (66, 56, 44)),
        ("asval", "rifle", 0.6, 2, 3, "long", False, (44, 48, 56)),
        ("qbz191", "rifle", 0.7, 1, 2, "box", False, (56, 62, 58)),
        ("aug", "rifle", 0.66, 3, 3, "box", True, (58, 62, 56)),
        ("sg552", "rifle", 0.58, 2, 2, "box", False, (54, 58, 66)),
        ("m16a4", "rifle", 0.74, 1, 1, "box", False, (54, 58, 70)),
        ("g3", "rifle", 0.78, 1, 3, "box", False, (60, 62, 66)),
        ("m7", "rifle", 0.76, 1, 3, "box", False, (64, 62, 56)),
        ("scarh", "rifle", 0.74, 1, 3, "box", False, (78, 70, 56)),
        ("ash12", "rifle", 0.72, 1, 3, "drum", False, (60, 58, 54)),
        ("kc17", "rifle", 0.62, 2, 2, "box", False, (52, 56, 62)),
        # 狙击（最长、高倍镜、脚架）
        ("sv98", "sniper", 0.92, 1, 3, "box", False, (56, 62, 56)),
        ("r93", "sniper", 0.96, 1, 3, "box", False, (50, 54, 58)),
        ("m700", "sniper", 0.9, 1, 3, "box", False, (62, 58, 52)),
        ("awm", "sniper", 1.0, 1, 3, "box", False, (66, 70, 62)),
        # 射手步枪
        ("sr9", "dmr", 0.8, 1, 3, "box", False, (58, 60, 64)),
        ("mini14", "dmr", 0.72, 1, 2, "box", False, (70, 62, 50)),
        ("m14", "dmr", 0.82, 1, 3, "box", False, (78, 64, 50)),
        ("sks", "dmr", 0.78, 1, 2, "box", False, (76, 64, 48)),
        ("psg1", "dmr", 0.84, 1, 3, "box", False, (54, 58, 62)),
        ("vss", "dmr", 0.66, 1, 3, "long", False, (48, 52, 56)),
        ("svd", "dmr", 0.88, 1, 3, "box", False, (70, 60, 48)),
        ("sr25", "dmr", 0.82, 1, 3, "box", False, (58, 62, 62)),
        ("svch", "dmr", 0.86, 1, 3, "box", False, (60, 62, 58)),
        # 轻机枪（弹鼓/弹链、脚架）
        ("m249", "lmg", 0.76, 1, 2, "drum", False, (60, 62, 66)),
        ("m250", "lmg", 0.8, 1, 3, "drum", False, (64, 62, 56)),
        ("pkm", "lmg", 0.82, 1, 2, "drum", False, (66, 58, 48)),
        ("qjb201", "lmg", 0.74, 1, 2, "drum", False, (58, 62, 58)),
    ]
    for spec in gun_specs:
        gid, kind, length, stock, optic, mag, bull, col = spec
        c = Canvas(32, pad=6)
        gun_generic(c, kind, length, stock, optic, mag, bull, col)
        # 裁到内容后放大填满贴图，避免枪身只占中间一小条
        save(c, ROOT / "guns" / (gid + ".png"), 256, fill=True)
        save(c, ROOT / "guns" / f"{name}.png", 256)

    for name, fn in (("grenade", throw_grenade), ("flash", throw_flash),
                     ("smoke", throw_smoke), ("molotov", throw_molotov),
                     ("c4", throw_c4)):
        c = Canvas(32)
        fn(c)
        save(c, ROOT / "throw" / f"{name}.png", 256)

    for name, fn in (("surgery", med_surgery), ("syringe", med_syringe),
                     ("bandage", med_bandage), ("painkiller", med_painkiller)):
        c = Canvas(32)
        fn(c)
        save(c, ROOT / "meds" / f"{name}.png", 256)

    for name, fn in (("crate", prop_crate), ("crate_damaged", prop_crate_damaged),
                     ("crate_broken", prop_crate_broken), ("planter", prop_planter),
                     ("barrel", prop_barrel), ("sandbag", prop_sandbag),
                     ("pillar", prop_pillar), ("tree", prop_tree),
                     ("bush", prop_bush)):
        c = Canvas(32)
        fn(c)
        save(c, ROOT / "props" / f"{name}.png", 256)

    for name, fn in (("floor", tile_floor), ("floor_wood", tile_floor_wood),
                     ("grass", tile_grass),
                     ("wall", tile_wall), ("wall_v", tile_wall_v),
                     ("stair", tile_stair),
                     ("floor_upper", tile_floor_upper),
                     ("door_closed", tile_door_closed), ("door_half", tile_door_half),
                     ("door_open", tile_door_open), ("door_broken", tile_door_broken)):
        c = Canvas(32, pad=1)
        fn(c)
        save(c, ROOT / "tiles" / f"{name}.png", 256)

    # 子弹帧
    for f in range(4):
        c = Canvas(32, pad=6)
        bullet_round(c, frame=f)
        save(c, ROOT / "bullet" / f"round_{f}.png", 64)
    c = Canvas(32, pad=6)
    bullet_round(c, frame=0, tracer=True)
    save(c, ROOT / "bullet" / "tracer_0.png", 64)
    c = Canvas(32, pad=4)
    bullet_slug(c)
    save(c, ROOT / "bullet" / "slug.png", 32)
    c = Canvas(32, pad=4)
    bullet_casing(c)
    save(c, ROOT / "bullet" / "casing.png", 48)

    files = sorted(ROOT.rglob("*.png"))
    total = sum(p.stat().st_size for p in files)
    print("输出:", ROOT)
    print("文件数:", len(files), "总体积: %.1f KB" % (total / 1024))
    for d in sorted({p.parent.name for p in files}):
        n = len(list((ROOT / d).glob("*.png")))
        sz = sum(p.stat().st_size for p in (ROOT / d).glob("*.png"))
        print(f"  {d:8s} {n:2d} 个  {sz/1024:6.1f} KB")


if __name__ == "__main__":
    main()
