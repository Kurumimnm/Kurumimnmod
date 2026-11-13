"""
アストラ・ヴェイル（星幽の巡界者）v3：設定画（星幽の巡界者のデザインシート）に合わせたモデル・テクスチャ・アニメーションの生成。

設定画から読み取った形：
  - 全体：水平に大きく広がる「布のような膜」。翼幅 約 100 ブロック、高さは翼幅の 1/3 ほど（サイズ比較の図）。
    地上から見上げると、膜の裏面（穴・ひれの筋）が空を覆う
  - 横から：前後に長く、触腕と帯が後ろへたなびく（横からのシルエット）
  - 顔：上の中央。細長い V 字の銀の仮面（2 本の柱が下で合わさる）、中央の稜線、上に小さな触角、
    まわりに真珠の粒の房と、花びらのような襟（顔部アップ）
  - 胴：顔の下へ縦に垂れる、穴だらけの骨のような格子のカーテン。下で大きな葉が外へ巻く
  - まわりに漂う欠片（穴の開いた葉のかけら）、先に玉の付いた渦巻く帯（その他のディテール）
  - 膜：大きな楕円の穴と太い真珠色のふち（レース）、ひれのように放射する筋

座標は Bedrock / Blockbench 形式（1px = 1/16 ブロック、前は北 -Z）。小さく作り、レンダラーで拡大する。
回転の向き（Bedrock 座標・GeckoLib で確認済み）：
  ry 正 = +x の先が前（-z）へ、rz 正 = +x の先が下へ、rx 正 = +z の先が上へ。左右反転は x・ry・rz の符号を反転
"""
import json, math, random, sys, os
import numpy as np
from PIL import Image

OUT = sys.argv[1] if len(sys.argv) > 1 else "out3"
os.makedirs(OUT, exist_ok=True)
rnd = random.Random(23)

bones, bone_index, anims = [], {}, {}
T = "query.anim_time"

def bone(name, parent, pivot, rotation=(0, 0, 0)):
    b = {"name": name, "pivot": [round(p, 3) for p in pivot], "cubes": []}
    if parent:
        b["parent"] = parent
    if any(abs(r) > 1e-9 for r in rotation):
        b["rotation"] = [round(r, 2) for r in rotation]
    bones.append(b); bone_index[name] = b
    return name

def cube(bname, origin, size, mat, rotation=None, pivot=None, label=None):
    size = [max(1, int(round(s))) for s in size]
    c = {"origin": [round(o, 3) for o in origin], "size": size, "_mat": mat, "_label": label or bname}
    if rotation:
        c["rotation"] = [round(r, 2) for r in rotation]; c["pivot"] = [round(p, 3) for p in pivot]
    bone_index[bname]["cubes"].append(c)

def wave(phase, amp, speed=45, fn="sin"):
    if abs(amp) < 0.05:
        return 0
    return f"math.{fn}({T}*{speed}-{phase:.0f})*{amp:.2f}"

def anim(name, rot=None, pos=None):
    anims[name] = {k: v for k, v in (("rotation", rot), ("position", pos)) if v}

# ---------------------------------------------------------------------------
# 鎖：+x 方向へ節を連ねる。side = 1 / -1 で左右反転。
#   segs: dict(L 長さ, W 横幅, H 厚み, ry, rz, rx, mat)
#   shape="flat" … 水平の板（L × H × W）：翼の膜。見上げると裏面が見える
#   shape="vert" … 縦の板（L × W × H）：帯・カーテン
# ---------------------------------------------------------------------------
def chain(prefix, parent, start, segs, side=1, shape="flat", overlap=1, anim_fn=None):
    x, y, z = start
    names = []
    for i, s in enumerate(segs):
        name = bone(f"{prefix}_{i}", parent, (x * side, y, z), (s.get("rx", 0), s.get("ry", 0) * side, s.get("rz", 0) * side))
        L, W, H = s["L"], s["W"], s.get("H", 1)
        ox = x if side > 0 else -x - L
        if shape == "flat":
            cube(name, (ox, y - H / 2, z - W / 2), (L, H, W), s["mat"])
        else:
            cube(name, (ox, y - W / 2, z - H / 2), (L, W, H), s["mat"])
        if anim_fn:
            anim(name, rot=anim_fn(i, side))
        names.append(name)
        parent = name
        x += L - overlap
    return names

# 下へ垂れる鎖（カーテン・触腕）。板は yaw の向きに面を向ける
def hang(prefix, parent, start, segs, yaw=0.0, anim_fn=None):
    x, y, z = start
    names = []
    for i, s in enumerate(segs):
        rot = (s.get("rx", 0), yaw if i == 0 else 0, s.get("rz", 0))
        name = bone(f"{prefix}_{i}", parent, (x, y, z), rot)
        W, L, H = s["W"], s["L"], s.get("H", 1)
        cube(name, (x - W / 2, y - L, z - H / 2), (W, L, H), s["mat"])
        if anim_fn:
            anim(name, rot=anim_fn(i))
        names.append(name)
        parent = name
        y -= L - 1
    return names

# ===========================================================================
bone("root", None, (0, 0, 0))
bone("body", "root", (0, 0, 0))
anim("body", rot=[wave(90, 1.5), wave(0, 1.5), wave(45, 1.0)], pos=[0, wave(0, 2.0), 0])

# ---------------------------------------------------------------------------
# 翼（布のような膜）：肩から扇形に放射する「ひれ」を並べて 1 枚の膜にする
#   上の膜 11 本、下の膜 7 本。節ごとに波打たせ、前のひれは先が巻き上がり、後ろのひれは長く垂れる
# ---------------------------------------------------------------------------
def wing_sheet(tag, side, n_rays, a0, a1, y0, z0, z1, n_seg, droop0, droop_k, wave_amp, w_base, w_k, mat, layer_phase, bank=-38):
    rays = []
    for k in range(n_rays):
        q = k / (n_rays - 1)
        heading = a0 + (a1 - a0) * q          # ry：正 = 前向き、負 = 後ろ向き
        n = n_seg + int(round(q * 3))         # 後ろのひれほど長い
        segs = []
        r = 6
        for i in range(n):
            L = 13
            r += L - 1
            W = min(30, w_base + r * w_k)
            undul = wave_amp * math.sin(i * 1.05 + k * 0.22 + layer_phase)
            rz = droop0 + droop_k * i + undul
            if k == 0 and i >= n - 4:          # 前のひれ：先が上へ巻き上がる
                rz = -18 - (i - (n - 4)) * 14
            if k >= n_rays - 3 and i >= n - 4:  # 後ろのひれ：先が下へ垂れて巻く
                rz += 10 + (i - (n - 4)) * 9
            segs.append(dict(L=L, W=W, H=1, ry=(heading if i == 0 else rnd.uniform(-1, 1)), rz=rz,
                             rx=(bank * (1 - 0.6 * q) if i == 0 else 5 * math.sin(i * 0.8 + k * 0.3)), mat=f"{mat}{min(i, 11)}"))
        start = (8, y0, z0 + (z1 - z0) * q)
        def af(i, sd, k=k):
            return [wave(i * 38 + k * 22 + 90, 2.0 + i * 0.35, 90), 0, wave(i * 40 + k * 18 + layer_phase * 50, (2.2 + i * 0.75) * sd)]
        rays.append(chain(f"{tag}{k}", "body", start, segs, side=side, shape="flat", overlap=1, anim_fn=af))
    return rays

wings = {}
for side, t in ((1, "l"), (-1, "r")):
    wings[(t, "up")] = wing_sheet(f"wing_{t}", side, 12, 34, -62, 2, -8, 10, 9, -9, 1.7, 7, 8, 0.27, "fin", 0)
    wings[(t, "lo")] = wing_sheet(f"under_{t}", side, 8, 22, -55, -7, -4, 10, 6, 2, 3.2, 6, 7, 0.26, "fin", 1.7, bank=-30)

# 顔の左右から上へ立ち上がり、外へ巻き下りる幕（設定画で頭の上に広がる膜）
for side, t in ((1, "l"), (-1, "r")):
    for k in range(3):
        segs = []
        for i in range(7):
            rz = (-62 + k * 10) if i == 0 else 9 + i * 1.5 + rnd.uniform(-3, 3)
            segs.append(dict(L=11, W=14 - i, H=1, ry=(-25 - k * 20) if i == 0 else rnd.uniform(-4, 4), rz=rz, rx=rnd.uniform(-6, 6), mat="fin"))
        chain(f"cape_{t}{k}", "body", (5, 16, -8 + k * 6), segs, side=side, shape="flat",
              anim_fn=lambda i, sd, k=k: [wave(i * 35 + k * 40, 2 + i * 0.4, 90), 0, wave(i * 38 + k * 30 + 20, (2 + i * 0.6) * sd)])

# ---------------------------------------------------------------------------
# 顔（細長い V 字の仮面）
# ---------------------------------------------------------------------------
H = bone("head", "body", (0, 10, -14))
anim("head", rot=[wave(200, 2.0), wave(120, 1.5, 90), wave(40, 1.0, 90)])
FZ = -16  # 仮面の前面
for side, t in ((1, "l"), (-1, "r")):
    # 2 本の柱：上は離れ、下で合わさる
    cube(H, (2.5 if side > 0 else -6.5, -12, FZ), (4, 40, 3), "face", rotation=(0, 0, 7 * side), pivot=(4.5 * side, 28, FZ + 1.5), label=f"mask_bar_{t}")
    # 仮面のふちの張り出し
    cube(H, (5 if side > 0 else -8, 6, FZ + 2), (3, 20, 2), "face", rotation=(0, 12 * side, 14 * side), pivot=(5 * side, 26, FZ + 3), label=f"mask_flange_{t}")
    # 目（小さな光る点）
    cube(H, (1 if side > 0 else -2, 19, FZ - 0.5), (1, 2, 1), "gem", label=f"eye_{t}")
    # 触角（2 本ずつ。外へ反って先に玉）
    for k in range(2):
        segs = [dict(L=6, W=1, H=1, ry=10 + k * 25, rz=-70 + k * 12, mat="face"),
                dict(L=6, W=1, H=1, rz=14, mat="face"), dict(L=5, W=1, H=1, rz=18, mat="face")]
        names = chain(f"antenna_{t}{k}", H, (1.5 + k, 30, FZ + 2), segs, side=side, shape="vert",
                      anim_fn=lambda i, sd, k=k: [wave(i * 30 + k * 60, 3, 90), 0, wave(i * 30 + k * 50, 4 * sd, 90)])
        tip = bone_index[names[-1]]
        tx = abs(tip["pivot"][0]) + 4
        cube(names[-1], (tx - 1 if side > 0 else -tx - 1, 29, FZ + 1), (2, 2, 2), "pearl", label=f"antenna_orb_{t}{k}")
    # 真珠の粒の房（頭の上の左右）
    for k in range(9):
        sz = rnd.choice([2, 2, 3, 3, 4])
        px = rnd.uniform(4, 11); py = rnd.uniform(16, 31); pz = FZ + rnd.uniform(1, 7)
        cube(H, (px if side > 0 else -px - sz, py, pz), (sz, sz, sz), "pearl", label=f"pearl_{t}{k}")
# 中央の稜線（光る継ぎ目）と眉
cube(H, (-0.5, 2, FZ - 1), (1, 26, 1), "slit", label="mask_ridge")
cube(H, (-6, 24, FZ + 1), (12, 5, 4), "face", label="mask_brow")
cube(H, (-4, 28, FZ + 2), (8, 4, 4), "face", label="mask_crest")
cube(H, (-2, -16, FZ + 0.5), (4, 7, 3), "face", label="mask_chin")
# 襟（顔のまわりに放射状に開く花びら。穴の開いた膜）
for k in range(12):
    ang = -165 + k * 30
    if abs(ang) < 20:
        continue
    rad = math.radians(ang)
    px, py = math.sin(rad) * 7, 10 + math.cos(rad) * 12
    w, h = rnd.choice([8, 9, 10]), rnd.choice([18, 20, 22])
    cube(H, (px - w / 2, py, FZ + 5), (w, h, 1), "lace", rotation=(rnd.uniform(-15, 5), 0, -ang), pivot=(px, py, FZ + 5.5), label=f"collar_{k}")
# 顔の下へ伸びる細い柱と宝石
cube(H, (-1, -50, FZ + 2), (2, 36, 2), "face", label="spine")
cube(H, (-1.5, -44, FZ + 1), (3, 4, 1), "gem", label="spine_gem")

# ---------------------------------------------------------------------------
# 胴：穴だらけの骨のような格子が縦に垂れる（中心の塊＋まわりのカーテン）
# ---------------------------------------------------------------------------
for i, (w, h, d, y) in enumerate([(16, 18, 14, -14), (13, 18, 12, -30), (10, 16, 10, -45), (7, 12, 7, -56)]):
    cube("body", (-w / 2, y, -6 - d / 2 + 4), (w, h, d), "lace", label=f"core_{i}")
for k in range(24):
    th = k / 24 * math.tau
    cx, cz = math.sin(th) * (11 + rnd.uniform(-2, 2)), math.cos(th) * 9 + 2
    yaw = -math.degrees(th)
    n = rnd.choice([5, 6, 6, 7, 8])
    segs = []
    for i in range(n):
        segs.append(dict(W=max(3, 9 - i * 0.6), L=10, H=1, rx=(rnd.uniform(-10, 4) if i else -8), rz=(9 if i % 2 else -9) + rnd.uniform(-5, 5),
                         mat=("lace" if (k + i) % 3 else "drape")))
    hang(f"drape_{k}", "body", (cx, -4 - rnd.uniform(0, 8), cz), segs, yaw=yaw,
         anim_fn=lambda i, k=k: [wave(i * 36 + k * 29, 2 + i * 0.7, 90), 0, wave(i * 33 + k * 23 + 90, 2 + i * 0.6, 45, "cos")])

# 下で外へ巻く大きな葉（穴の開いた葉）
for k in range(9):
    th = (k + 0.5) / 9 * math.tau
    cx, cz = math.sin(th) * 9, math.cos(th) * 7 + 2
    yaw = -math.degrees(th)
    segs = []
    for i in range(7):
        rx = -28 if i == 0 else (-10 - i * 11 if i >= 2 else rnd.uniform(-8, 0))
        segs.append(dict(W=max(4, 13 - i * 1.2), L=10, H=1, rx=rx, rz=rnd.uniform(-6, 6), mat="lace"))
    hang(f"leaf_{k}", "body", (cx, -52, cz), segs, yaw=yaw,
         anim_fn=lambda i, k=k: [wave(i * 40 + k * 37, 2.5 + i * 0.8, 90), 0, wave(i * 30 + k * 41, 2 + i * 0.6, 45, "cos")])

# ---------------------------------------------------------------------------
# 後ろへたなびく帯（横からのシルエット）：体の後ろと翼の付け根から後ろ下へ
# ---------------------------------------------------------------------------
for k in range(10):
    side = 1 if k % 2 == 0 else -1
    j = k // 2
    start = (2 + j * 5, -6 - j * 7, 12)
    n = 13 + rnd.randint(-2, 2)
    segs = []
    for i in range(n):
        segs.append(dict(L=10, W=max(3, 7 - i * 0.3), H=1, ry=(-90 + j * 6) if i == 0 else rnd.uniform(-6, 6),
                         rz=(14 if i == 0 else 4 + 7 * math.sin(i * 0.9 + k)), rx=rnd.uniform(-8, 8), mat="ribbon"))
    chain(f"trail_{k}", "body", start, segs, side=side, shape="vert",
          anim_fn=lambda i, sd, k=k: [wave(i * 36 + k * 30, 3 + i * 0.4, 90), wave(i * 30 + k * 25, 3 * sd), wave(i * 34 + k * 27 + 60, (3 + i * 0.6) * sd, 90)])

# 翼のふちから出て渦を巻く帯（先に玉）
for side, t in ((1, "l"), (-1, "r")):
    for k, (ray, frac, curl) in enumerate([(1, 0.6, 1), (5, 0.9, -1), (9, 1.0, 1)]):
        names = wings[(t, "up")][ray]
        parent = names[min(len(names) - 1, int(len(names) * frac))]
        pb = bone_index[parent]; pc = pb["cubes"][0]
        sx = abs(pb["pivot"][0]) + pc["size"][0] - 1
        n = 12
        segs = []
        for i in range(n):
            q = i / (n - 1)
            segs.append(dict(L=7, W=max(2, 5 - i * 0.25), H=1, ry=(rnd.uniform(-20, 20) if i == 0 else 0),
                             rz=(10 if i == 0 else curl * (6 + 34 * q ** 2)), rx=rnd.uniform(-5, 5), mat="ribbon"))
        names2 = chain(f"curl_{t}{k}", parent, (sx, pb["pivot"][1], pb["pivot"][2]), segs, side=side, shape="vert",
                       anim_fn=lambda i, sd, k=k: [wave(i * 30 + k * 70, 3, 90), 0, wave(i * 32 + k * 60, (2 + i * 0.5) * sd, 90)])
        tb = bone_index[names2[-1]]
        tx = abs(tb["pivot"][0]) + 5
        cube(names2[-1], (tx if side > 0 else -tx - 3, tb["pivot"][1] - 1.5, tb["pivot"][2] - 1.5), (3, 3, 3), "pearl", label=f"curl_orb_{t}{k}")

# ---------------------------------------------------------------------------
# まわりに漂う欠片（穴の開いた葉のかけら）
# ---------------------------------------------------------------------------
for k in range(28):
    ang = rnd.uniform(0, math.tau)
    rr = rnd.uniform(30, 115)
    px, pz = math.cos(ang) * rr, math.sin(ang) * rr * 0.6
    py = rnd.uniform(-60, 24)
    name = bone(f"shard_{k}", "body", (px, py, pz), (rnd.uniform(-60, 60), rnd.uniform(0, 360), rnd.uniform(-60, 60)))
    w, h = rnd.choice([(4, 7), (5, 9), (6, 10), (3, 6)])
    cube(name, (px - w / 2, py - h / 2, pz), (w, h, 1), rnd.choice(["lace", "fin", "lace"]))
    anim(name, rot=[wave(k * 47, 12, 45), wave(k * 31, 20, 45), wave(k * 53, 10, 90)], pos=[0, wave(k * 41, 2.5, 90), 0])

# 顔まわり（head とその子）を拡大する（設定画では顔が大きく目立つ）
HEAD_SCALE = 1.4
def _desc(name):
    out = [name]
    for b in bones:
        if b.get("parent") in out:
            out.append(b["name"])
    return out
_hp = bone_index["head"]["pivot"]
def _sc(p):
    return [round(_hp[i] + (p[i] - _hp[i]) * HEAD_SCALE, 3) for i in range(3)]
for n in _desc("head"):
    b = bone_index[n]
    if n != "head":
        b["pivot"] = _sc(b["pivot"])
    for c in b["cubes"]:
        c["origin"] = _sc(c["origin"]); c["size"] = [max(1, int(round(v * HEAD_SCALE))) for v in c["size"]]
        if "pivot" in c:
            c["pivot"] = _sc(c["pivot"])

# ===========================================================================
# Box UV の割り付け
# ===========================================================================
UNIQUE = ("face", "slit")
def foot(size):
    w, h, d = size
    return 2 * (w + d), d + h
slots = {}
for b in bones:
    for c in b["cubes"]:
        key = (tuple(c["size"]), c["_mat"]) + ((c["_label"],) if c["_mat"] in UNIQUE else ())
        s = slots.setdefault(key, {"size": c["size"], "mat": c["_mat"], "labels": []})
        s["labels"].append(c["_label"]); c["_key"] = key
TEX_W = 512
order = sorted(slots, key=lambda k: (-foot(slots[k]["size"])[1], -foot(slots[k]["size"])[0]))
u = v = shelf = 0
for k in order:
    fw, fh = foot(slots[k]["size"])
    if u + fw > TEX_W:
        u = 0; v += shelf; shelf = 0
    slots[k]["uv"] = (u, v); u += fw; shelf = max(shelf, fh)
TEX_H = 1 << (v + shelf - 1).bit_length()
for b in bones:
    for c in b["cubes"]:
        c["uv"] = list(slots[c["_key"]]["uv"])

def face_rects(u, v, size):
    w, h, d = size
    return {"up": (u + d, v, w, d), "down": (u + d + w, v, w, d), "east": (u, v + d, d, h),
            "north": (u + d, v + d, w, h), "west": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h)}

# ===========================================================================
# テクスチャ（設定画の色：淡い藤色〜ペリウィンクル、真珠色のハイライト、奥は深い紫）
# ===========================================================================
tex = np.zeros((TEX_H, TEX_W, 4), np.float32)
glow = np.zeros((TEX_H, TEX_W, 4), np.float32)
def C(s, a=255):
    s = s.lstrip("#"); return np.array([int(s[i:i + 2], 16) for i in (0, 2, 4)] + [a], np.float32)
def L(a, b, t):
    return a + (b - a) * max(0.0, min(1.0, t))
DEEP = C("#3f33a3", 215); VIOL = C("#5b4fd0", 200); PERI = C("#7d82e8", 180); PALE = C("#aab6f6", 165); MIST = C("#d6d8ff", 170)
PEARL = C("#f3f0ff", 235); WHITE = C("#ffffff", 245); IRI = C("#bfefff", 190)
G_RIM = C("#d6eeff", 60); G_VEIN = C("#a5e0ff", 80); G_STAR = C("#f0f8ff", 230); G_CYAN = C("#9ff4ff", 255)

def put(x, y, col, g=None):
    tex[y, x] = col
    if g is not None:
        glow[y, x] = g

def holes(x, y, w, h, count, rmin, rmax, rim=1, oval=1.4):
    for _ in range(count):
        rx_ = rnd.uniform(rmin, rmax) * oval; ry_ = rx_ / oval
        if w < 2 * rx_ + 4 or h < 2 * ry_ + 4:
            continue
        cx = rnd.uniform(x + rx_ + 2, x + w - rx_ - 2); cy = rnd.uniform(y + ry_ + 2, y + h - ry_ - 2)
        for j in range(int(cy - ry_ - rim - 1), int(cy + ry_ + rim + 2)):
            for i in range(int(cx - rx_ - rim - 1), int(cx + rx_ + rim + 2)):
                if not (x <= i < x + w and y <= j < y + h):
                    continue
                d = ((i + 0.5 - cx) / rx_) ** 2 + ((j + 0.5 - cy) / ry_) ** 2
                if d <= 1.0:
                    tex[j, i] = 0; glow[j, i] = 0
                elif d <= (1 + rim / max(1, ry_)) ** 2 + 0.25 and tex[j, i, 3] > 0:
                    put(i, j, PEARL, G_RIM)

def paint_fin(x, y, w, h, big, idx):
    """翼の膜。idx = 根元からの節の番号（0〜11）。根元は濃い藤色、先へいくほど淡いペリウィンクル（節の境目で色が飛ばないように節の番号で決める）。"""
    t0, t1 = idx / 12, (idx + 1) / 12
    for j in range(h):
        for i in range(w):
            t = t0 + (t1 - t0) * (i / max(1, w - 1))
            n = math.sin(i * 0.3 + j * 0.7 + idx) * 0.05 + rnd.uniform(-0.05, 0.05)
            col = L(VIOL, PERI, t * 2 + n) if t < 0.5 else L(PERI, PALE, (t - 0.5) * 2 + n)
            if rnd.random() < 0.04:
                col = L(col, IRI, 0.4)
            put(x + i, y + j, col, G_STAR if rnd.random() < 0.008 else None)
    if not big:
        return
    for j in range(3, h - 1, 7):  # 細いひれの筋
        for i in range(w):
            if rnd.random() < 0.85:
                put(x + i, y + j, L(col, MIST, 0.45), G_VEIN if rnd.random() < 0.4 else None)
    if idx >= 5 and w >= 10 and h >= 10 and rnd.random() < 0.55:
        holes(x, y, w, h, 1, 1.2, 2.2)

def paint_lace(x, y, w, h, big):
    """骨のような格子：淡い真珠色に、大きな楕円の穴と太いふち（その他のディテール 1 枚目）。"""
    for j in range(h):
        for i in range(w):
            t = j / max(1, h - 1)
            col = L(MIST, PERI, t * 0.6 + rnd.random() * 0.25)
            put(x + i, y + j, col, G_STAR if rnd.random() < 0.01 else None)
    if big:
        holes(x, y, w, h, max(1, (w * h) // 30), 1.0, 2.4, rim=1)
        for i in range(w):
            put(x + i, y, PEARL, G_RIM)

def paint_drape(x, y, w, h, big):
    for j in range(h):
        for i in range(w):
            col = L(DEEP, PERI, (i % 3) / 3 * 0.5 + rnd.random() * 0.3)
            put(x + i, y + j, col)
    if big:
        for i in range(0, w, 3):
            for j in range(h):
                put(x + i, y + j, L(PERI, MIST, 0.6), G_VEIN if j % 5 == 0 else None)
        holes(x, y, w, h, max(1, (w * h) // 45), 0.8, 1.6)

def paint_ribbon(x, y, w, h, big):
    for j in range(h):
        for i in range(w):
            edge = j == 0 or j == h - 1
            col = PEARL if edge else L(PERI, PALE, rnd.random())
            put(x + i, y + j, col, G_RIM if edge else (G_STAR if rnd.random() < 0.02 else None))

def paint_solid(x, y, w, h, a, b, gcol=None, gp=0.0):
    for j in range(h):
        for i in range(w):
            put(x + i, y + j, L(a, b, j / max(1, h - 1) * 0.7 + rnd.random() * 0.3), gcol if (gcol is not None and rnd.random() < gp) else None)

STEEL_HI = C("#e9ecfb"); STEEL = C("#9aa0c8"); STEEL_D = C("#4c5080")
def paint_face(x, y, w, h, fname, label):
    paint_solid(x, y, w, h, STEEL_HI, STEEL)
    if fname in ("north", "east", "west") and h >= 8:
        for j in range(h):  # 縦の彫り込み
            if w >= 3:
                put(x + w // 2, y + j, STEEL_D)
        for j in range(2, h, 5):
            for i in range(w):
                put(x + i, y + j, L(STEEL, STEEL_D, 0.4))
        put(x, y, WHITE); put(x + w - 1, y, WHITE)

for k, s in slots.items():
    u0, v0 = s["uv"]
    rects = face_rects(u0, v0, s["size"])
    areas = sorted(rects.items(), key=lambda kv: -kv[1][2] * kv[1][3])
    bigfaces = {areas[0][0], areas[1][0]}
    lab = s["labels"][0]
    for fname, (x, y, w, h) in rects.items():
        if w <= 0 or h <= 0:
            continue
        m = s["mat"]; big = fname in bigfaces
        if m.startswith("fin") and m[3:].isdigit():
            paint_fin(x, y, w, h, big, int(m[3:]))
        elif m == "fin":
            paint_fin(x, y, w, h, big, 6)
        elif m == "lace":
            paint_lace(x, y, w, h, big)
        elif m == "drape":
            paint_drape(x, y, w, h, big)
        elif m == "ribbon":
            paint_ribbon(x, y, w, h, big)
        elif m == "face":
            paint_face(x, y, w, h, fname, lab)
        elif m == "slit":
            for j in range(h):
                for i in range(w):
                    put(x + i, y + j, C("#c8f7ff"), G_CYAN * np.array([1, 1, 1, 0.7]))
        elif m == "pearl":
            paint_solid(x, y, w, h, C("#ffffff"), C("#c6c4f4"), C("#eef6ff", 140), 0.5)
            put(x, y, WHITE, G_STAR)
        elif m == "gem":
            paint_solid(x, y, w, h, C("#e8feff"), C("#45b8f0"), G_CYAN, 1.0)

Image.fromarray(np.clip(tex, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, "astra_veil.png"))
Image.fromarray(np.clip(glow, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, "astra_veil_glowmask.png"))

# ===========================================================================
def clean(b):
    nb = {"name": b["name"]}
    if "parent" in b: nb["parent"] = b["parent"]
    nb["pivot"] = b["pivot"]
    if "rotation" in b: nb["rotation"] = b["rotation"]
    if b["cubes"]:
        nb["cubes"] = []
        for c in b["cubes"]:
            nc = {"origin": c["origin"], "size": c["size"]}
            if "rotation" in c: nc["pivot"] = c["pivot"]; nc["rotation"] = c["rotation"]
            nc["uv"] = c["uv"]; nb["cubes"].append(nc)
    return nb
geo = {"format_version": "1.12.0", "minecraft:geometry": [{
    "description": {"identifier": "geometry.astra_veil", "texture_width": TEX_W, "texture_height": TEX_H,
                    "visible_bounds_width": 16, "visible_bounds_height": 12, "visible_bounds_offset": [0, 0, 0]},
    "bones": [clean(b) for b in bones]}]}
json.dump(geo, open(os.path.join(OUT, "astra_veil.geo.json"), "w"), indent="\t", ensure_ascii=False)
ab = {}
for b in bones:
    a = anims.get(b["name"])
    if not a:
        continue
    o = {k: [x if isinstance(x, str) else 0 for x in v] for k, v in a.items() if any(isinstance(x, str) for x in v)}
    if o:
        ab[b["name"]] = o
json.dump({"format_version": "1.8.0", "animations": {"animation.astra_veil.idle": {"loop": True, "animation_length": 8.0, "bones": ab}}},
          open(os.path.join(OUT, "astra_veil.animation.json"), "w"), indent="\t", ensure_ascii=False)
json.dump({"slots": {str(k): v for k, v in slots.items()}, "tex": [TEX_W, TEX_H]}, open(os.path.join(OUT, "_slots.json"), "w"), ensure_ascii=False, default=str)
print("tex", TEX_W, TEX_H, "slots", len(slots), "bones", len(bones), "cubes", sum(len(b["cubes"]) for b in bones))
