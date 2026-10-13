# Gera o ícone do app (calendário + guarda-sol) e os recursos Android.
# Já executado: os PNGs estão em android-overrides/res. Só rode de novo se quiser mudar o desenho.
from PIL import Image, ImageDraw
import os

S = 2048           # desenho em 2x para suavizar as bordas
K = S / 1024

def r(v): return int(round(v * K))

def foreground():
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    # sombra suave do cartão
    sh = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle([r(262), r(298), r(762), r(758)], r(64), fill=(0, 30, 35, 70))
    from PIL import ImageFilter
    im.alpha_composite(sh.filter(ImageFilter.GaussianBlur(r(14))))
    d = ImageDraw.Draw(im)
    # cartão do calendário
    d.rounded_rectangle([r(262), r(282), r(762), r(442)], r(64), fill="#F4B13D", corners=(True, True, False, False))
    d.rounded_rectangle([r(262), r(412), r(762), r(742)], r(64), fill="#FFFFFF", corners=(False, False, True, True))
    # argolas
    for x in (372, 652):
        d.rounded_rectangle([r(x - 18), r(250), r(x + 18), r(326)], r(18), fill="#0A4E56")
    # guarda-sol
    cx, cy, rad = 512, 600, 128
    cores = ["#E0483C", "#0B6B75", "#E0483C", "#0B6B75"]
    for i, c in enumerate(cores):
        d.pieslice([r(cx - rad), r(cy - rad), r(cx + rad), r(cy + rad)], 180 + i * 45, 225 + i * 45, fill=c)
    d.line([r(cx), r(cy), r(cx), r(706)], fill="#0A4E56", width=r(12))
    d.ellipse([r(cx - 78), r(698), r(cx + 78), r(726)], fill="#F4D9A0")
    return im

def background():
    im = Image.new("RGB", (S, S))
    px = im.load()
    top, bot = (14, 135, 145), (8, 79, 88)
    for y in range(S):
        t = y / (S - 1)
        c = tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3))
        for x in range(S):
            px[x, y] = c
    return im.convert("RGBA")

fg, bg = foreground(), background()
full = bg.copy(); full.alpha_composite(fg)

res = "android-overrides/res"
dens = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
for name, f in dens.items():
    d = f"{res}/mipmap-{name}"; os.makedirs(d, exist_ok=True)
    size_i, size_a = int(48 * f), int(108 * f)
    # ícone legado (quadrado com cantos) e redondo
    leg = full.resize((size_i, size_i), Image.LANCZOS)
    mask = Image.new("L", (S, S), 0); ImageDraw.Draw(mask).rounded_rectangle([0, 0, S - 1, S - 1], r(180), fill=255)
    sq = full.copy(); sq.putalpha(mask)
    sq.resize((size_i, size_i), Image.LANCZOS).save(f"{d}/ic_launcher.png")
    cm = Image.new("L", (S, S), 0); ImageDraw.Draw(cm).ellipse([0, 0, S - 1, S - 1], fill=255)
    rd = full.copy(); rd.putalpha(cm)
    rd.resize((size_i, size_i), Image.LANCZOS).save(f"{d}/ic_launcher_round.png")
    fg.resize((size_a, size_a), Image.LANCZOS).save(f"{d}/ic_launcher_foreground.png")
    bg.convert("RGB").resize((size_a, size_a), Image.LANCZOS).save(f"{d}/ic_launcher_bg.png")

os.makedirs(f"{res}/mipmap-anydpi-v26", exist_ok=True)
xml = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/ic_launcher_bg"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
</adaptive-icon>
"""
for n in ("ic_launcher.xml", "ic_launcher_round.xml"):
    open(f"{res}/mipmap-anydpi-v26/{n}", "w").write(xml)

# tela de abertura (splash): fundo petróleo com o ícone no centro
splash = {"drawable": (480, 320), "drawable-land-hdpi": (800, 480), "drawable-land-mdpi": (480, 320),
          "drawable-land-xhdpi": (1280, 720), "drawable-land-xxhdpi": (1600, 960), "drawable-land-xxxhdpi": (1920, 1280),
          "drawable-port-hdpi": (480, 800), "drawable-port-mdpi": (320, 480), "drawable-port-xhdpi": (720, 1280),
          "drawable-port-xxhdpi": (960, 1600), "drawable-port-xxxhdpi": (1280, 1920)}
for folder, (w, h) in splash.items():
    os.makedirs(f"{res}/{folder}", exist_ok=True)
    base = bg.resize((w, h), Image.LANCZOS)
    s = int(min(w, h) * 0.46)
    ic = fg.resize((s, s), Image.LANCZOS)
    base.alpha_composite(ic, ((w - s) // 2, (h - s) // 2))
    base.convert("RGB").save(f"{res}/{folder}/splash.png")

# pré-visualização grande
full.resize((512, 512), Image.LANCZOS).save("tools/icone-previa.png")
print("ok")
