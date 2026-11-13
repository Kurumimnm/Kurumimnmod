"""UV 配置図を出す： python3 uv_doc.py 生成フォルダ 出力.png"""
import json, math, sys
from PIL import Image, ImageDraw, ImageFont
out_dir, dst = sys.argv[1], sys.argv[2]
d = json.load(open(out_dir + '/_slots.json'))
TW, TH = d['tex']; slots = d['slots']
tex = Image.open(out_dir + '/astra.png').convert('RGBA')
F = '/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc'
ft = ImageFont.truetype(F, 22); fs = ImageFont.truetype(F, 12); fl = ImageFont.truetype(F, 15); fx = ImageFont.truetype(F, 9)
W = TW + 760; H = max(TH + 260, 1100)
img = Image.new('RGB', (W, H), (32, 34, 44)); dr = ImageDraw.Draw(img)
dr.text((12, 14), f'astra.png UV配置図（{TW}×{TH} / Box UV / 1マス=1px）', font=ft, fill=(235, 235, 245))
ox, oy = 0, 60
bg = Image.new('RGB', (TW, TH)); px = bg.load()
for y in range(TH):
    for x in range(TW):
        px[x, y] = (58, 60, 72) if (x // 4 + y // 4) % 2 == 0 else (48, 50, 60)
img.paste(bg, (ox, oy)); img.paste(tex, (ox, oy), tex)
GROUPS = [('膜（上・下の腕／羽、尾。1 枚ずつ別の絵）', (110, 160, 255), ('wing',)),
          ('顔の板（闇・仮面・盛り上がり・牙）', (255, 210, 90), ('facep',)),
          ('頭の中身・角・牙・膜の骨', (255, 150, 90), ('skull', 'horn', 'bone')),
          ('胴の格子・カーテン', (240, 200, 255), ('lacebox', 'lace', 'drape')),
          ('葉（襟・羽飾り・膜の裏・欠片）', (150, 230, 160), ('frond', 'shard')),
          ('帯・触腕・糸', (120, 230, 200), ('ribbon', 'thread')),
          ('爪・真珠・宝石', (255, 120, 150), ('talon', 'pearl', 'gem'))]
def group_of(mat):
    k = mat.split('|')[0]
    for g in GROUPS:
        if k in g[2]:
            return g
    return GROUPS[-1]
NAMES = {'up': '上', 'down': '下', 'east': '東', 'north': '北', 'west': '西', 'south': '南'}
listing = {}
for s in slots:
    u, v = s['uv']; w, h, dd = s['size']; g = group_of(s['mat']); col = g[1]
    rects = {'up': (u + dd, v, w, dd), 'down': (u + dd + w, v, w, dd), 'east': (u, v + dd, dd, h),
             'north': (u + dd, v + dd, w, h), 'west': (u + dd + w, v + dd, dd, h), 'south': (u + 2 * dd + w, v + dd, w, h)}
    for fn, (x, y, ww, hh) in rects.items():
        if ww <= 0 or hh <= 0:
            continue
        dr.rectangle([ox + x, oy + y, ox + x + ww - 1, oy + y + hh - 1], outline=col, width=1)
        if ww >= 14 and hh >= 14:
            dr.text((ox + x + 2, oy + y + 1), NAMES[fn], font=fx, fill=col)
    labels = s['labels']
    name = labels[0] if len(labels) == 1 else f"{labels[0]} ほか{len(labels) - 1}種"
    listing.setdefault(g[0], []).append((w * h * dd, f"{name}  UV({u},{v})  {w}×{h}×{dd}"))
lx, ly = TW + 24, 60
for g in GROUPS:
    items = sorted(listing.get(g[0], []), reverse=True)
    dr.rectangle([lx, ly + 5, lx + 12, ly + 17], fill=g[1]); dr.text((lx + 20, ly), f"{g[0]}：{len(items)} 種類", font=fl, fill=g[1]); ly += 24
    for _, t in items[:9]:
        dr.text((lx + 20, ly), t, font=fs, fill=(220, 220, 230)); ly += 16
    if len(items) > 9:
        dr.text((lx + 20, ly), f'…ほか {len(items) - 9} 種類', font=fs, fill=(170, 170, 185)); ly += 16
    ly += 10
notes = ['・Box UV：1 つのキューブは「上・下 / 東・北・西・南」の順に並ぶ（枠の中の文字がその面）。モデルの前（顔）は北（-Z）側',
         '・膜（wing_up / wing_low / tail）は扇全体でつながる絵を短冊ごとに切り分けている。左右の膜は同じ絵を mirror で共有',
         '・膜・葉・帯などの薄い板は上面と下面（または北面と南面）だけ描き、側面は透明。輪郭・穴は透明ピクセルで切り抜き',
         '・顔は板を前後に重ねている：face_void（奥の闇。目と喉の光）→ face_mask（仮面。目・口は穴）→ face_relief（眉・鼻筋・頬骨など）→ face_fang（牙）',
         '・発光は astra_glowmask.png（同じ配置。目・喉・ふち・筋・爪・星の粒）',
         '・1 ピクセル = 1/2 ブロック（レンダラーで 8 倍。ModConstants.ASTRA_RENDER_SCALE）',
         '・Blockbench：GeckoLib Animated Model で astra.geo.json を読み込むと同じ配置になる']
ny = oy + TH + 16
for n in notes:
    dr.text((12, ny), n, font=fs, fill=(200, 200, 215)); ny += 18
img.crop((0, 0, W, max(ly, ny) + 16)).save(dst)
