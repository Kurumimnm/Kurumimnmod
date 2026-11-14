"""UV 配置図を出す（Box UV。どの位置がどの部位・どの面か）。使い方: python3 uv_doc.py <generate.py の出力フォルダ> <out.png>"""
import json, sys
from PIL import Image, ImageDraw, ImageFont

out_dir, dst = sys.argv[1], sys.argv[2]
d = json.load(open(out_dir + "/_slots.json"))
TW, TH = d["tex"]; slots = d["slots"]
tex = Image.open(out_dir + "/astra_veil.png").convert("RGBA")
F = "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"
ft = ImageFont.truetype(F, 34); fs = ImageFont.truetype(F, 20); fl = ImageFont.truetype(F, 24); fx = ImageFont.truetype(F, 11)
# 材質 → (凡例の名前, 枠の色)
GROUPS = {
    "tent": ("触腕（大型：左右・上・後ろ・前・下／小型：傘のふち・胴）", (255, 120, 150)),
    "fringe": ("触腕のふちのひれ（切り抜きのある膜）", (255, 170, 120)),
    "band": ("帯状の長い触腕", (120, 230, 200)),
    "oral": ("口腕（傘の裏から垂れるひだ）", (230, 150, 255)),
    "strand": ("胴の下の房", (120, 200, 255)),
    "dome": ("頭部：傘（上面・側面）", (110, 160, 255)), "under": ("頭部：傘の裏", (80, 120, 220)),
    "lobe": ("頭部：傘のふち・上に重なる葉", (170, 130, 255)), "veil": ("頭部：ふちから垂れる膜（ヴェイル）", (150, 220, 255)),
    "gill": ("頭部：傘の裏のえら", (100, 220, 220)), "ridge": ("頭部：傘の稜線", (200, 200, 255)), "star": ("頭部：頂上の星形の飾り", (255, 230, 150)),
    "mask": ("顔：仮面", (255, 255, 255)), "gold": ("顔：金のふち取り・くちばし", (240, 200, 100)), "spike": ("顔：冠のとげ・眉の翼", (220, 200, 255)),
    "fil": ("触角・あごひげ（細い器官）", (200, 255, 200)),
    "body": ("胴体", (90, 110, 255)), "flute": ("胴体：縦のひだ", (130, 170, 255)),
    "amber": ("発光器官：琥珀色", (255, 190, 70)), "gem": ("発光器官：青い宝玉", (90, 230, 255)), "pale": ("発光器官：淡い黄色", (255, 245, 180)),
    "halo_amber": ("発光器官のまわりの光の殻", (255, 220, 150)), "halo_gem": ("発光器官のまわりの光の殻", (255, 220, 150)), "halo_pale": ("発光器官のまわりの光の殻", (255, 220, 150)),
    "mote": ("星屑", (255, 255, 255)), "mote_c": ("星屑", (255, 255, 255)), "mote_y": ("星屑", (255, 255, 255)),
}
NAMES = {"up": "上", "down": "下", "east": "東", "north": "北", "west": "西", "south": "南"}
LEG_W = 1500
img = Image.new("RGB", (TW + LEG_W, TH + 90), (24, 26, 36)); dr = ImageDraw.Draw(img)
dr.text((16, 18), f"astra_veil.png UV 配置図（{TW}×{TH} / Box UV / 等倍。1 ピクセル = 1/2 ブロック）", font=ft, fill=(235, 235, 245))
ox, oy = 0, 80
bg = Image.new("RGB", (TW, TH), (40, 42, 54)); bd = ImageDraw.Draw(bg)
for y in range(0, TH, 16):
    for x in range(0, TW, 16):
        if (x // 16 + y // 16) % 2 == 0:
            bd.rectangle([x, y, x + 15, y + 15], fill=(50, 52, 66))
bg.paste(tex, (0, 0), tex); img.paste(bg, (ox, oy))
stats = {}
for s in slots:
    u, v = s["uv"]; w, h, dd = s["size"]; mk = s["mat"]
    name, col = GROUPS.get(mk, (mk, (200, 200, 200)))
    rects = {"up": (u + dd, v, w, dd), "down": (u + dd + w, v, w, dd), "east": (u, v + dd, dd, h),
             "north": (u + dd, v + dd, w, h), "west": (u + dd + w, v + dd, dd, h), "south": (u + 2 * dd + w, v + dd, w, h)}
    for fn, (x, y, ww, hh) in rects.items():
        if ww <= 0 or hh <= 0:
            continue
        if ww >= 6 and hh >= 6:
            dr.rectangle([ox + x, oy + y, ox + x + ww - 1, oy + y + hh - 1], outline=col, width=1)
        if ww >= 16 and hh >= 16:
            dr.text((ox + x + 2, oy + y + 1), NAMES[fn], font=fx, fill=col)
    st = stats.setdefault(name, {"col": col, "n": 0, "ex": []})
    st["n"] += 1
    if len(st["ex"]) < 2:
        st["ex"].append(f"{s['labels'][0]} UV({u},{v}) {w}×{h}×{dd}")
lx, ly = TW + 30, 90
dr.text((lx, ly), "部位（枠の色）と UV の例", font=fl, fill=(235, 235, 245)); ly += 44
for name, st in stats.items():
    dr.rectangle([lx, ly + 6, lx + 18, ly + 24], fill=st["col"])
    dr.text((lx + 30, ly), f"{name}（{st['n']} 種類）", font=fl, fill=st["col"]); ly += 32
    for e in st["ex"]:
        dr.text((lx + 30, ly), e, font=fs, fill=(205, 205, 220)); ly += 26
    ly += 8
notes = ["・Box UV：1 つのキューブの展開は 上段［上｜下］、下段［東｜北｜西｜南］（枠の中の文字が面）",
         "・モデルの前（顔）は北（-Z）。左右対称の位置にある触腕は同じ絵（UV）を共有する",
         "・色は 2×2 ピクセル（= 1 ブロック）ごとに決めている（ブロックで組んだような見た目）",
         "・透明ピクセル：ひれ・膜・葉・えらの切り抜き。半透明：触腕・帯・光の殻",
         "・発光：astra_veil_glowmask.png（同じ配置。発光器官・ふちの光の筋・星の粒・胴の内側の光）",
         "・生成：tools/astra_veil/generate.py（手描きで直す場合は Blockbench の GeckoLib Animated Model で geo.json を開く）"]
ly += 10
for n in notes:
    dr.text((lx, ly), n, font=fs, fill=(200, 200, 215)); ly += 28
img.save(dst, optimize=True)
print(img.size)
