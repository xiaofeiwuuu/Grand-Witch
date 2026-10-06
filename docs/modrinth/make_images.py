"""
Makes the pictures for the Modrinth page, from the mod's own textures (and a few of the game's item textures, read from a copy of the game's jar
unpacked to MCJAR, which are not kept in this repository). Run from the repository root: python3 docs/modrinth/make_images.py
These are artwork, not screenshots of the game.
"""
import math, os, random
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(ROOT, "..", "..", "src/main/resources/assets/grandwitch/textures")
MCJAR = "/tmp/mcjar/assets/minecraft/textures/item"
OUT = os.path.join(ROOT, "images")
os.makedirs(OUT, exist_ok=True)
random.seed(7)

F_TITLE = "/System/Library/Fonts/Supplemental/Arial Black.ttf"
F_BOLD = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
F_CJK = "/System/Library/Fonts/STHeiti Medium.ttc"


def font(path, size):
    return ImageFont.truetype(path, size)


def load(p):
    return Image.open(p).convert("RGBA")


def big(im, k):
    return im.resize((im.width * k, im.height * k), Image.NEAREST)


def gradient(w, h, top, bottom):
    im = Image.new("RGBA", (w, h))
    d = ImageDraw.Draw(im)
    for y in range(h):
        t = y / (h - 1)
        d.line([(0, y), (w, y)], fill=tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)) + (255,))
    return im


def stars(im, n=70):
    d = ImageDraw.Draw(im)
    for _ in range(n):
        x, y = random.randrange(im.width), random.randrange(im.height // 2 + 100)
        s = random.choice((2, 2, 3, 4))
        c = random.choice(((255, 245, 200, 200), (210, 190, 255, 170), (255, 255, 255, 140)))
        d.rectangle([x, y, x + s, y + s], fill=c)


def background(w=1280, h=720):
    im = gradient(w, h, (24, 12, 44), (96, 48, 118))
    stars(im)
    d = ImageDraw.Draw(im)
    d.rectangle([0, h - 70, w, h], fill=(32, 22, 40, 255))          # the ground
    for x in range(0, w, 32):
        d.rectangle([x, h - 70, x + 16, h - 62], fill=(60, 110, 50, 255))
    return im


def text(im, xy, s, f, fill=(255, 255, 255, 255), shadow=True, anchor="la"):
    d = ImageDraw.Draw(im)
    if shadow:
        d.text((xy[0] + 3, xy[1] + 3), s, font=f, fill=(0, 0, 0, 170), anchor=anchor)
    d.text(xy, s, font=f, fill=fill, anchor=anchor)


# ---------------------------------------------------------------------------------------------- the figures
def figure(skin, hat=False):
    """A front view of a skin (the standard 64 by 64 layout), 16 wide and 40 high, a hat over the head if asked."""
    s = load(os.path.join(TEX, "entity", skin))
    out = Image.new("RGBA", (16, 40), (0, 0, 0, 0))

    def part(box, at, over=None):
        out.alpha_composite(s.crop(box), at)
        if over:
            out.alpha_composite(s.crop(over), at)

    oy = 8
    part((4, 20, 8, 32), (4, oy + 20 - 0), (4, 36, 8, 48))            # the right leg (on the left, as seen)
    part((20, 52, 24, 64), (8, oy + 20), (4, 52, 8, 64))
    part((20, 20, 28, 32), (4, oy + 8), (20, 36, 28, 48))             # the body
    part((44, 20, 48, 32), (0, oy + 8), (44, 36, 48, 48))
    part((36, 52, 40, 64), (12, oy + 8), (52, 52, 56, 64))
    part((8, 8, 16, 16), (4, oy), (40, 8, 48, 16))                    # the head
    if hat:
        d = ImageDraw.Draw(out)
        dark, band, gold = (28, 22, 40, 255), (120, 60, 160, 255), (230, 190, 60, 255)
        d.rectangle([1, oy - 1, 14, oy], fill=dark)                   # the brim
        d.rectangle([3, oy - 3, 12, oy - 2], fill=dark)
        d.rectangle([3, oy - 3, 12, oy - 3], fill=band)
        d.rectangle([5, oy - 6, 10, oy - 4], fill=dark)
        d.rectangle([6, oy - 8, 9, oy - 7], fill=dark)
        d.rectangle([7, oy - 10, 8, oy - 9], fill=dark)
        d.rectangle([7, oy - 3, 8, oy - 3], fill=gold)
    return out


def mouse_art(grey=True):
    """A mouse seen from the side, 40 by 22, with the colours of the mouse of the mod."""
    body = (168, 162, 172, 255) if grey else (128, 94, 168, 255)
    light = (214, 208, 216, 255) if grey else (170, 130, 210, 255)
    shade = (122, 116, 128, 255) if grey else (92, 64, 128, 255)
    pink = (232, 150, 160, 255)
    im = Image.new("RGBA", (40, 22), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.ellipse([4, 6, 28, 19], fill=body)                              # the body
    d.ellipse([7, 8, 24, 13], fill=light)
    d.ellipse([22, 8, 35, 18], fill=body)                             # the head
    d.polygon([(34, 14), (39, 17), (34, 18)], fill=body)              # the snout
    d.rectangle([38, 16, 39, 17], fill=pink)                          # the nose
    d.ellipse([22, 2, 29, 9], fill=body)                              # an ear
    d.ellipse([24, 3, 28, 8], fill=pink)
    d.rectangle([30, 11, 31, 12], fill=(10, 10, 14, 255))             # the eye
    d.line([(4, 15), (1, 17), (0, 20)], fill=pink, width=1)           # the tail
    d.line([(0, 20), (2, 21)], fill=pink, width=1)
    d.rectangle([9, 18, 12, 20], fill=shade)                          # the feet
    d.rectangle([22, 18, 25, 20], fill=shade)
    d.line([(35, 15), (39, 13)], fill=(235, 235, 240, 255), width=1)       # whiskers
    d.line([(35, 17), (39, 19)], fill=(235, 235, 240, 255), width=1)
    return im


def potion_icon(color):
    base = load(os.path.join(MCJAR, "potion.png"))
    over = load(os.path.join(MCJAR, "potion_overlay.png"))
    tint = Image.new("RGBA", over.size, color + (255,))
    px = over.load()
    t = tint.load()
    for y in range(over.height):
        for x in range(over.width):
            r, g, b, a = px[x, y]
            t[x, y] = (r * color[0] // 255, g * color[1] // 255, b * color[2] // 255, a)
    out = base.copy()
    out.alpha_composite(tint)
    return out


def tipped_arrow(color):
    base = load(os.path.join(MCJAR, "tipped_arrow_base.png"))
    head = load(os.path.join(MCJAR, "tipped_arrow_head.png"))
    px = head.load()
    for y in range(head.height):
        for x in range(head.width):
            r, g, b, a = px[x, y]
            px[x, y] = (r * color[0] // 255, g * color[1] // 255, b * color[2] // 255, a)
    out = base.copy()
    out.alpha_composite(head)
    return out


MOUSE_COLOR = (184, 154, 168)
AWKWARD = (56, 93, 198)


def paste_center(im, icon, cx, cy):
    im.alpha_composite(icon, (int(cx - icon.width / 2), int(cy - icon.height / 2)))


def arrow_sign(im, cx, cy, size=40):
    d = ImageDraw.Draw(im)
    d.polygon([(cx - size, cy - 10), (cx, cy - 10), (cx, cy - 26), (cx + size, cy), (cx, cy + 26), (cx, cy + 10), (cx - size, cy + 10)], fill=(255, 255, 255, 235))


def plus_sign(im, cx, cy, size=22):
    d = ImageDraw.Draw(im)
    d.rectangle([cx - size, cy - 6, cx + size, cy + 6], fill=(255, 255, 255, 235))
    d.rectangle([cx - 6, cy - size, cx + 6, cy + size], fill=(255, 255, 255, 235))


def panel(im, box, alpha=120):
    ov = Image.new("RGBA", im.size, (0, 0, 0, 0))
    ImageDraw.Draw(ov).rounded_rectangle(box, radius=22, fill=(10, 4, 24, alpha))
    im.alpha_composite(ov)


# ---------------------------------------------------------------------------------------------- the icon
def make_icon():
    n = 64
    im = gradient(n, n, (70, 30, 110), (140, 70, 160)).convert("RGBA")
    d = ImageDraw.Draw(im)
    for _ in range(14):
        x, y = random.randrange(n), random.randrange(n // 2)
        d.point((x, y), fill=(255, 240, 190, 255))
    dark, band, gold = (22, 16, 34, 255), (122, 62, 168, 255), (240, 196, 64, 255)
    # the hat
    d.polygon([(32, 2), (24, 24), (40, 24)], fill=dark)
    d.polygon([(32, 2), (36, 14), (30, 14)], fill=(40, 30, 58, 255))
    d.rectangle([22, 22, 42, 25], fill=band)
    d.rectangle([30, 22, 33, 25], fill=gold)
    d.ellipse([10, 24, 54, 32], fill=dark)
    # the mouse, looking up from under it
    grey, light, pink = (176, 170, 182, 255), (222, 216, 226, 255), (236, 150, 164, 255)
    d.ellipse([12, 30, 26, 44], fill=grey)                          # the ears
    d.ellipse([38, 30, 52, 44], fill=grey)
    d.ellipse([15, 33, 23, 41], fill=pink)
    d.ellipse([41, 33, 49, 41], fill=pink)
    d.ellipse([16, 34, 48, 62], fill=grey)                          # the head
    d.ellipse([22, 42, 42, 60], fill=light)
    d.rectangle([24, 42, 27, 46], fill=(8, 8, 14, 255))             # the eyes
    d.rectangle([37, 42, 40, 46], fill=(8, 8, 14, 255))
    d.rectangle([24, 42, 25, 43], fill=(255, 255, 255, 255))
    d.rectangle([37, 42, 38, 43], fill=(255, 255, 255, 255))
    d.rectangle([30, 52, 33, 55], fill=pink)                        # the nose
    for y in (50, 55):
        d.line([(12, y - 2), (22, y)], fill=(240, 240, 245, 255))
        d.line([(52, y - 2), (42, y)], fill=(240, 240, 245, 255))
    icon = big(im, 8)
    mask = Image.new("L", icon.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, 511, 511], radius=96, fill=255)
    out = Image.new("RGBA", icon.size, (0, 0, 0, 0))
    out.paste(icon, (0, 0), mask)
    out.save(os.path.join(OUT, "icon.png"))


# ---------------------------------------------------------------------------------------------- the pictures
def title_card():
    im = background()
    text(im, (70, 110), "Grand Witch", font(F_TITLE, 118))
    text(im, (74, 262), "大女巫", font(F_CJK, 70), fill=(240, 210, 255, 255))
    text(im, (74, 372), "Eat her treats. Become a mouse.", font(F_BOLD, 40), fill=(255, 235, 170, 255))
    text(im, (74, 424), "Outsmart the witch.", font(F_BOLD, 40), fill=(255, 235, 170, 255))
    text(im, (74, 500), "吃了她的点心，你就变成一只老鼠。", font(F_CJK, 32), fill=(230, 215, 245, 255))
    text(im, (74, 548), "Forge 1.20.1  ·  Early release 0.1.0", font(F_BOLD, 26), fill=(190, 170, 220, 255))
    f = big(figure("grand_witch.png", hat=True), 10)
    im.alpha_composite(f, (1040, 720 - 70 - f.height + 6))
    m = big(mouse_art(), 6)
    im.alpha_composite(m, (760, 720 - 70 - m.height + 4))
    im.convert("RGB").save(os.path.join(OUT, "01_title.png"))


def witches_card():
    im = background()
    text(im, (640, 52), "One witch. Many faces.", font(F_TITLE, 56), anchor="ma")
    text(im, (640, 128), "一个女巫，许多张脸", font(F_CJK, 36), fill=(240, 210, 255, 255), anchor="ma")
    cards = [("grand_witch.png", True, "Her true self", "她的真面目"),
             ("girl_red.png", False, "Gift girl", "送礼少女"),
             ("granny.png", False, "Potion granny", "卖药婆"),
             ("girl_apron.png", False, "Gift girl", "围裙少女"),
             ("nanny.png", False, "Nanny", "保姆")]
    k = 8
    for i, (skin, hat, en, zh) in enumerate(cards):
        cx = 140 + i * 250
        f = big(figure(skin, hat), k)
        panel(im, (cx - 112, 190, cx + 112, 640), 90)
        im.alpha_composite(f, (cx - f.width // 2, 545 - f.height))
        text(im, (cx, 562), en, font(F_BOLD, 28), anchor="ma")
        text(im, (cx, 602), zh, font(F_CJK, 24), fill=(240, 210, 255, 255), anchor="ma")
    im.convert("RGB").save(os.path.join(OUT, "02_witches.png"))


def mouse_card():
    im = background()
    text(im, (60, 40), "Life as a mouse", font(F_TITLE, 64))
    text(im, (60, 124), "变成老鼠之后", font(F_CJK, 36), fill=(240, 210, 255, 255))
    m = big(mouse_art(), 14)
    im.alpha_composite(m, (50, 330))
    lines = [("Small, quick, night vision", "身子小、跑得快、夜视"),
             ("Squeaks by itself — cats come for you", "会自己吱吱叫，猫会来追你"),
             ("Dig cross-shaped tunnels in soft ground", "在软土里挖十字形隧道"),
             ("Hide in mouse holes, nibble chests", "躲进老鼠洞，啃箱子里的食物"),
             ("Dangers glow through walls", "危险会隔着墙发光"),
             ("The witch can reach into a hole…", "女巫会趴下来伸手进洞里抓")]
    y0 = 190
    panel(im, (620, 170, 1230, 640), 110)
    for i, (en, zh) in enumerate(lines):
        y = y0 + i * 72
        text(im, (650, y), en, font(F_BOLD, 26))
        text(im, (650, y + 34), zh, font(F_CJK, 20), fill=(230, 210, 245, 255))
    im.convert("RGB").save(os.path.join(OUT, "03_mouse.png"))


def recipes_card():
    im = background()
    text(im, (640, 36), "Brew it. Shoot it. Cure it.", font(F_TITLE, 52), anchor="ma")
    text(im, (640, 106), "炼药、做箭、解药", font(F_CJK, 32), fill=(240, 210, 255, 255), anchor="ma")
    panel(im, (50, 160, 1230, 640), 90)
    k = 5
    ic = lambda p: big(load(p), k)
    rows = [
        ("Potion of the Mouse   变鼠药水", [big(potion_icon(AWKWARD), k), "+", big(load(os.path.join(TEX, "item", "plague_virus.png")), k), "→", big(potion_icon(MOUSE_COLOR), k)],
         "Awkward potion + Plague Virus"),
        ("Mouse Arrows   变鼠药水箭", [big(load(os.path.join(MCJAR, "arrow.png")), k), "×8 +", big(potion_icon(MOUSE_COLOR), k), "→", big(tipped_arrow(MOUSE_COLOR), k)],
         "8 arrows round a Potion of the Mouse"),
        ("Antidote   解鼠药剂", [big(load(os.path.join(MCJAR, "golden_carrot.png")), k), "+", big(load(os.path.join(MCJAR, "spider_eye.png")), k), "+", big(load(os.path.join(MCJAR, "glass_bottle.png")), k), "→", big(load(os.path.join(TEX, "item", "antidote.png")), k)],
         "Golden carrot + spider eye + bottle"),
    ]
    notes_zh = ["粗制药水 + 鼠疫病毒（野生老鼠掉落）", "8 支箭围着 1 瓶变鼠药水", "金胡萝卜 + 蜘蛛眼 + 玻璃瓶"]
    for r, (title, items, note) in enumerate(rows):
        y = 190 + r * 150
        en_t, zh_t = title.split("   ")
        text(im, (90, y), en_t, font(F_BOLD, 28))
        text(im, (90 + ImageDraw.Draw(im).textlength(en_t, font=font(F_BOLD, 28)) + 24, y + 2), zh_t, font(F_CJK, 24), fill=(240, 210, 255, 255))
        x = 100
        for it in items:
            if isinstance(it, str):
                text(im, (x + 25, y + 90), it, font(F_TITLE, 34), anchor="ma")
                x += 70 if len(it) > 1 else 56
            else:
                im.alpha_composite(it, (x, y + 40))
                x += it.width + 18
        text(im, (700, y + 62), note, font(F_BOLD, 24), fill=(255, 235, 170, 255))
        text(im, (700, y + 98), notes_zh[r], font(F_CJK, 20), fill=(240, 220, 200, 255))
    im.convert("RGB").save(os.path.join(OUT, "04_recipes.png"))


def brooms_card():
    im = background()
    text(im, (640, 50), "Brooms", font(F_TITLE, 70), anchor="ma")
    text(im, (640, 142), "扫把：低空飞行", font(F_CJK, 38), fill=(240, 210, 255, 255), anchor="ma")
    for i, (name, en, zh) in enumerate([("wooden_broom.png", "Wooden broom — up to 3 blocks", "木扫把：最高 3 格"),
                                         ("golden_broom.png", "Golden broom — up to 6 blocks", "金扫把：最高 6 格")]):
        cx = 330 + i * 620
        panel(im, (cx - 280, 200, cx + 280, 590), 100)
        ico = big(load(os.path.join(TEX, "item", name)), 14)
        im.alpha_composite(ico, (cx - ico.width // 2, 225))
        text(im, (cx, 460), en, font(F_BOLD, 26), anchor="ma")
        text(im, (cx, 500), zh, font(F_CJK, 24), fill=(240, 210, 255, 255), anchor="ma")
    text(im, (640, 604), "Hold SPACE to climb · look up or down to steer · the witch rides one too", font(F_BOLD, 22), anchor="ma")
    im.convert("RGB").save(os.path.join(OUT, "05_brooms.png"))


def features_card():
    im = background()
    text(im, (640, 36), "What's inside", font(F_TITLE, 60), anchor="ma")
    text(im, (640, 112), "玩法一览", font(F_CJK, 34), fill=(240, 210, 255, 255), anchor="ma")
    items = [("Witches in disguise swap treats with you", "女巫伪装成村民，用点心跟你换食物"),
             ("Her house: cauldron, diary, a cage of mice", "她的小屋：药锅、日记、鼠笼"),
             ("Spike her food, and she turns into a mouse", "给她的食物加料，她就变成老鼠"),
             ("The potion of the mouse works on any creature", "变鼠药水对任何生物都有效"),
             ("Wild mice, and mouse holes in hillsides", "野生老鼠，土坡里的老鼠洞"),
             ("Cure with the antidote, or milk, or by killing her", "解药、牛奶，或者杀掉她，就能变回来")]
    panel(im, (60, 170, 1220, 650), 100)
    for i, (en, zh) in enumerate(items):
        col, row = i % 2, i // 2
        x, y = 90 + col * 580, 195 + row * 150
        text(im, (x, y), en, font(F_BOLD, 24))
        text(im, (x, y + 40), zh, font(F_CJK, 20), fill=(230, 210, 245, 255))
    im.convert("RGB").save(os.path.join(OUT, "06_features.png"))


if __name__ == "__main__":
    make_icon()
    title_card()
    witches_card()
    mouse_card()
    recipes_card()
    brooms_card()
    features_card()
    print("done")
