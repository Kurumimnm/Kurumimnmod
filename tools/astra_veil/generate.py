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

OUT = sys.argv[1] if len(sys.argv) > 1 else "out4"
K = 4          # モデルの細かさ（1 = 旧版。4 で 1 ピクセル = 1/8 ブロック。レンダラーの倍率は 8 / K）
NCAP = 13      # 節の番号の上限（色・形の段階）
os.makedirs(OUT, exist_ok=True)
rnd = random.Random(23)

bones, bone_index, anims = [], {}, {}
PLATES = ("fin", "lace", "drape", "ribbon", "shard", "petal")   # 薄い板（ふち・穴をテクスチャの透明で切り抜く）
T = "query.anim_time"

def bone(name, parent, pivot, rotation=(0, 0, 0)):
    b = {"name": name, "pivot": [round(p, 3) for p in pivot], "cubes": []}
    if parent:
        b["parent"] = parent
    if any(abs(r) > 1e-9 for r in rotation):
        b["rotation"] = [round(r, 2) for r in rotation]
    bones.append(b); bone_index[name] = b
    return name

def cube(bname, origin, size, mat, rotation=None, pivot=None, label=None, mirror=False, px=None):
    """px = 拡大後の大きさ（ピクセル）を直接指定する細かい部品用。"""
    size = [max(1, int(round(s))) for s in size]
    c = {"origin": [round(o, 3) for o in origin], "size": size, "_mat": mat, "_label": label or bname}
    if mirror:
        c["mirror"] = True
    if px:
        c["_px"] = list(px)
    elif mat.split("|")[0] in PLATES and size.count(1) == 1:
        c["_thin"] = size.index(1)
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
def chain(prefix, parent, start, segs, side=1, shape="flat", overlap=1, anim_fn=None, deco=None, var=0):
    x, y, z = start
    names = []
    for i, s in enumerate(segs):
        name = bone(f"{prefix}_{i}", parent, (x * side, y, z), (s.get("rx", 0), s.get("ry", 0) * side, s.get("rz", 0) * side))
        L, W, H = s["L"], s["W"], s.get("H", 1)
        ox = x if side > 0 else -x - L
        m = f"{s['mat']}|{min(i, NCAP)}|{int(i == len(segs) - 1)}|u|{var}"
        if shape == "flat":
            cube(name, (ox, y - H / 2, z - W / 2), (L, H, W), m, mirror=side < 0)
        else:
            cube(name, (ox, y - W / 2, z - H / 2), (L, W, H), m, mirror=side < 0)
        if deco:
            deco(name, i, len(segs), ox, y, z, L, W)
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
        cube(name, (x - W / 2, y - L, z - H / 2), (W, L, H), f"{s['mat']}|{min(i, NCAP)}|{int(i == len(segs) - 1)}|v|{rnd.randint(0, 1)}")
        if anim_fn:
            anim(name, rot=anim_fn(i))
        names.append(name)
        parent = name
        y -= L - 1
    return names

# ===========================================================================
bone("root", None, (0, 0, 0))
bone("body", "root", (0, 0, 0))
anim("body", rot=[wave(90, 1.5), wave(0, 1.5), wave(45, 1.0)], pos=[0, wave(0, 2.0 * K), 0])

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
                             rx=(bank * (1 - 0.6 * q) if i == 0 else 5 * math.sin(i * 0.8 + k * 0.3)), mat=mat))
        start = (8, y0, z0 + (z1 - z0) * q)
        def af(i, sd, k=k):
            return [wave(i * 38 + k * 22 + 90, 2.0 + i * 0.35, 90), 0, wave(i * 40 + k * 18 + layer_phase * 50, (2.2 + i * 0.75) * sd)]
        def deco(name, i, n, ox, y, z, L, W, q=q):
            # 膜の裏から垂れる細い糸（ふさ）と、付け根近くの真珠の粒
            for _ in range(rnd.choice([0, 1, 1, 2]) if i >= 1 else 0):
                ln = int(rnd.uniform(8, 22 + 22 * q))
                cube(name, (ox + rnd.uniform(0.5, L - 0.5), y - 0.5 - ln / K, z + rnd.uniform(-0.45, 0.45) * W), (1, 1, 1), "thread", px=(1, ln, 1))
            if i < 5 and rnd.random() < 0.55:
                sz = rnd.choice([3, 4, 5, 6])
                cube(name, (ox + rnd.uniform(1, L - 2), y + 0.4, z + rnd.uniform(-0.35, 0.35) * W), (1, 1, 1), "pearl", px=(sz, sz, sz))
        rays.append(chain(f"{tag}{k}", "body", start, segs, side=side, shape="flat", overlap=1, anim_fn=af, deco=deco, var=k % 2))
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
    cube(H, (px - w / 2, py, FZ + 5), (w, h, 1), f"petal|0|1|b|{k % 3}", rotation=(rnd.uniform(-15, 5), 0, -ang), pivot=(px, py, FZ + 5.5), label=f"collar_{k}")
# --- 細部：仮面のとげ、細かい真珠、垂れ下がる珠の連なり、あごの糸 ---
for side, t in ((1, "l"), (-1, "r")):
    for j, yy in enumerate(range(-8, 24, 4)):
        bx = 4.5 + (yy + 12) * 0.12
        cube(H, (bx if side > 0 else -bx - 4, yy, FZ + 1), (4, 1, 1), "face", rotation=(0, 0, -28 * side), pivot=(bx * side, yy + 0.5, FZ + 1.5), label=f"barb_{t}{j}")
    for k in range(16):
        sz = rnd.choice([3, 3, 4, 5])
        px_ = rnd.uniform(3, 14); py_ = rnd.uniform(4, 34); pz_ = FZ + rnd.uniform(0, 9)
        cube(H, (px_ if side > 0 else -px_ - sz / K, py_, pz_), (1, 1, 1), "pearl", px=(sz, sz, sz), label=f"seed_{t}{k}")
    for k in range(4):
        bx, by = 5 + k * 2.4, 12 - k * 3.5
        for j in range(9):
            sz = 4 if j % 3 == 0 else 3
            xx = (bx + math.sin(j * 0.7 + k) * 0.8) * side
            cube(H, (xx - sz / K / 2, by - j * 1.6, FZ + 3 + k), (1, 1, 1), "pearl", px=(sz, sz, sz), label=f"beads_{t}{k}_{j}")
for k in range(10):
    ln = int(rnd.uniform(30, 80))
    cube(H, (rnd.uniform(-3.5, 3.5), -16 - ln / K, FZ + rnd.uniform(0.5, 3)), (1, 1, 1), "thread", px=(1, ln, 1), label=f"whisker_{k}")
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
for k in range(64):
    ang = rnd.uniform(0, math.tau)
    rr = rnd.uniform(30, 115)
    px, pz = math.cos(ang) * rr, math.sin(ang) * rr * 0.6
    py = rnd.uniform(-60, 24)
    name = bone(f"shard_{k}", "body", (px, py, pz), (rnd.uniform(-60, 60), rnd.uniform(0, 360), rnd.uniform(-60, 60)))
    w, h = rnd.choice([(4, 8), (5, 10), (6, 12), (3, 6), (7, 14)])
    cube(name, (px - w / 2, py - h / 2, pz), (w, h, 1), f"shard|0|1|v|{k % 4}")
    anim(name, rot=[wave(k * 47, 12, 45), wave(k * 31, 20, 45), wave(k * 53, 10, 90)], pos=[0, wave(k * 41, 2.5 * K, 90), 0])

# 星の粒（体のまわりに漂う小さな光。body に直接付ける）
for k in range(110):
    ang = rnd.uniform(0, math.tau); rr = rnd.uniform(12, 120)
    sz = rnd.choice([1, 2, 2, 3])
    cube("body", (math.cos(ang) * rr, rnd.uniform(-70, 34), math.sin(ang) * rr * 0.65), (1, 1, 1), "pearl", px=(sz, sz, sz), label=f"mote_{k}")

# ===========================================================================
# 顔まわり（head とその子）を拡大 → 全体を K 倍に細かくする
# ===========================================================================
HEAD_SCALE = 1.4
def _desc(name):
    out = [name]
    for b in bones:
        if b.get("parent") in out:
            out.append(b["name"])
    return out
_hp = bone_index["head"]["pivot"]
def _sc(p):
    return [_hp[i] + (p[i] - _hp[i]) * HEAD_SCALE for i in range(3)]
for n in _desc("head"):
    b = bone_index[n]
    if n != "head":
        b["pivot"] = _sc(b["pivot"])
    for c in b["cubes"]:
        c["origin"] = _sc(c["origin"]); c["size"] = [max(1, int(round(v * HEAD_SCALE))) for v in c["size"]]
        if "pivot" in c:
            c["pivot"] = _sc(c["pivot"])

for b in bones:
    b["pivot"] = [round(p * K, 2) for p in b["pivot"]]
    for c in b["cubes"]:
        o = [p * K for p in c["origin"]]
        if "_px" in c:
            c["size"] = c["_px"]
        else:
            c["size"] = [v * K for v in c["size"]]
            if "_thin" in c:   # 薄い板は厚み 1 ピクセルのまま（中心を保つ）
                a = c["_thin"]; c["size"][a] = 1; o[a] += (K - 1) / 2
        c["origin"] = [round(p, 2) for p in o]
        if "pivot" in c:
            c["pivot"] = [round(p * K, 2) for p in c["pivot"]]

# ===========================================================================
# Box UV の割り付け（同じ大きさ・同じ材質は UV を共有。左右は mirror で反転）
# ===========================================================================
UNIQUE = ("face", "slit")
def foot(size):
    w, h, d = size
    return 2 * (w + d), d + h
slots = {}
for b in bones:
    for c in b["cubes"]:
        kind = c["_mat"].split("|")[0]
        key = (tuple(c["size"]), c["_mat"]) + ((c["_label"],) if kind in UNIQUE else ())
        s = slots.setdefault(key, {"size": c["size"], "mat": c["_mat"], "labels": [], "thin": c.get("_thin")})
        s["labels"].append(c["_label"]); c["_key"] = key
TEX_W = 2048
order = sorted(slots, key=lambda k: (-foot(slots[k]["size"])[1], -foot(slots[k]["size"])[0]))
u = v = shelf = 0
for k in order:
    fw, fh = foot(slots[k]["size"])
    if u + fw > TEX_W:
        u = 0; v += shelf; shelf = 0
    slots[k]["uv"] = (u, v); u += fw; shelf = max(shelf, fh)
TEX_H = -(-(v + shelf) // 64) * 64
for b in bones:
    for c in b["cubes"]:
        c["uv"] = list(slots[c["_key"]]["uv"])

def face_rects(u, v, size):
    w, h, d = size
    return {"up": (u + d, v, w, d), "down": (u + d + w, v, w, d), "east": (u, v + d, d, h),
            "north": (u + d, v + d, w, h), "west": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h)}

# ===========================================================================
# テクスチャ（numpy で 1 面ずつ描く）
#   板の描画は「長さ方向 = 横（x: 0 = 付け根 → 1 = 先）、幅方向 = 縦（y: -1〜1）」の向きで作り、面の向きに合わせて貼る
# ===========================================================================
tex = np.zeros((TEX_H, TEX_W, 4), np.float32)
glow = np.zeros((TEX_H, TEX_W, 4), np.float32)
def C(s, a=255):
    s = s.lstrip("#"); return np.array([int(s[i:i + 2], 16) for i in (0, 2, 4)] + [a], np.float32)
DEEP = C("#3a2f9c", 225); VIOL = C("#5a4ed0", 205); PERI = C("#7d84ea", 185); PALE = C("#aebaf8", 165); MIST = C("#d9dbff", 190)
PEARL = C("#f4f1ff", 240); WHITE = C("#ffffff", 250); IRI = C("#b4f0ff", 190); ROSE = C("#e3c4ff", 190)
G_RIM = C("#d8efff", 70); G_VEIN = C("#a8e2ff", 85); G_STAR = C("#f2f9ff", 235); G_CYAN = C("#9ff4ff", 255)
STEEL_HI = C("#eef0fc"); STEEL = C("#a3a9d0"); STEEL_D = C("#4d5184")

def mix(a, b, t):
    t = np.clip(np.asarray(t, np.float32), 0, 1)[..., None]
    return a * (1 - t) + b * t

def smooth(h, w, scale, rs):
    gh, gw = int(h / scale) + 2, int(w / scale) + 2
    g = rs.random((gh, gw)).astype(np.float32)
    ys, xs = np.arange(h) / scale, np.arange(w) / scale
    y0, x0 = ys.astype(int), xs.astype(int)
    fy, fx = (ys - y0)[:, None], (xs - x0)[None, :]
    return (g[y0][:, x0] * (1 - fx) * (1 - fy) + g[y0][:, x0 + 1] * fx * (1 - fy)
            + g[y0 + 1][:, x0] * (1 - fx) * fy + g[y0 + 1][:, x0 + 1] * fx * fy)

def erode(m, r):
    for _ in range(r):
        p = np.pad(m, 1, mode="edge")
        m = p[1:-1, 1:-1] & p[:-2, 1:-1] & p[2:, 1:-1] & p[1:-1, :-2] & p[1:-1, 2:]
    return m

def grid(Lp, Wp):
    X = (np.arange(Lp) + 0.5)[None, :] / Lp * np.ones((Wp, 1), np.float32)
    Y = ((np.arange(Wp) + 0.5)[:, None] / Wp * 2 - 1) * np.ones((1, Lp), np.float32)
    return X, Y, X * Lp, Y * Wp / 2

def punch(mask, PX, PY, rs, count, r0, r1, Lp, Wp, margin=0.12):
    for _ in range(count):
        ra = rs.uniform(r0, r1); rb = ra * rs.uniform(0.55, 0.85)
        if Lp < 2 * ra + 6 or Wp < 2 * rb + 6:
            continue
        cx = rs.uniform(ra + 3, Lp - ra - 3); cy = rs.uniform(-Wp / 2 * (1 - margin) + rb, Wp / 2 * (1 - margin) - rb)
        mask &= ~((((PX - cx) / ra) ** 2 + ((PY - cy) / rb) ** 2) < 1)
    return mask

def finish(col, gl, mask, rs, rim_w=2, rim_col=PEARL, rim_glow=G_RIM, sparkle=0.004):
    rim = mask & ~erode(mask, rim_w)
    col[rim] = rim_col; gl[rim] = rim_glow
    sp = (rs.random(mask.shape) < sparkle) & mask
    col[sp] = WHITE; gl[sp] = G_STAR
    col[~mask] = 0; gl[~mask] = 0
    return col, gl

def paint_fin(Lp, Wp, i, tip, rs):
    """翼の膜：ひれの筋・細かい葉脈・虹色のつや・楕円の穴・波打つふち。先の節は二股の尾のように細る。"""
    X, Y, PX, PY = grid(Lp, Wp)
    t = (i + X) / (NCAP + 1)
    env = 0.94 - 0.06 * (0.5 + 0.5 * np.sin((i + X) * math.tau * 2)) - 0.03 * smooth(Wp, Lp, 9, rs)
    if tip:
        env = env * np.clip(1 - X ** 2.4, 0, 1) ** 0.55
    mask = np.abs(Y) < env
    if tip:
        mask &= ~((np.abs(Y) < (X - 0.55) * 0.9) & (X > 0.55))
    if i >= 3:
        mask = punch(mask, PX, PY, rs, 1 + (Lp * Wp) // 3000, 3.5, 9.0, Lp, Wp)
    mask = punch(mask, PX, PY, rs, (Lp * Wp) // 900, 1.2, 2.2, Lp, Wp)
    n = smooth(Wp, Lp, 14, rs) * 0.18 - 0.09
    col = np.where((t < 0.5)[..., None], mix(VIOL, PERI, t * 2 + n), mix(PERI, PALE, (t - 0.5) * 2 + n))
    col = mix(col, DEEP, np.clip(0.55 - t * 3.5, 0, 1))
    gl = np.zeros_like(col)
    sheen = (0.5 + 0.5 * np.sin(X * 5 + Y * 3 + i * 1.3 + rs.uniform(0, 6))) ** 3
    col = mix(col, IRI, sheen * 0.30)
    col = mix(col, ROSE, ((0.5 + 0.5 * np.sin(X * 3 - Y * 4 + i)) ** 4) * 0.22)
    nr = max(3, Wp // 12)
    near = np.full(X.shape, 99.0, np.float32)
    for k in range(nr):   # ひれの筋
        yc = ((k + 0.5) / nr * 2 - 1) * 0.88 * Wp / 2 + 1.4 * np.sin((i + X) * 5 + k * 1.9)
        d = np.abs(PY - yc); near = np.minimum(near, d)
        rib = d < 0.75
        col[rib] = mix(col, MIST, 0.7)[rib]; gl[rib] = G_VEIN
    veinlet = (((PX + near * 1.3) % 8) < 1.0) & (near > 1.2)   # 葉脈（筋から斜めに出る）
    col[veinlet] = mix(col, MIST, 0.28)[veinlet]
    col[..., 3] = 228 - 50 * np.clip(t, 0, 1)
    return finish(col, gl, mask, rs)

def paint_lace(Lp, Wp, i, tip, rs, full=False):
    """骨のような格子：大きな楕円の穴がびっしり並び、太い真珠色のふちだけが残る。"""
    X, Y, PX, PY = grid(Lp, Wp)
    if full:
        mask = np.ones(X.shape, bool)
    else:
        env = 0.95 - 0.07 * (0.5 + 0.5 * np.sin((i + X) * math.tau * 1.5 + rs.uniform(0, 6)))
        if tip:
            env = env * np.clip(1 - X ** 1.8, 0, 1) ** 0.7
        mask = np.abs(Y) < env
    cell = rs.uniform(9, 13)
    for gx in range(int(Lp / cell) + 1):
        for gy in range(int(Wp / cell) + 1):
            if rs.random() < 0.15:
                continue
            cx = (gx + 0.5 + rs.uniform(-0.2, 0.2)) * cell + (cell / 2 if gy % 2 else 0)
            cy = (gy + 0.5 + rs.uniform(-0.2, 0.2)) * cell - Wp / 2
            ra, rb = cell * rs.uniform(0.28, 0.42), cell * rs.uniform(0.22, 0.36)
            if rs.random() < 0.5:
                ra, rb = rb, ra
            mask &= ~((((PX - cx) / ra) ** 2 + ((PY - cy) / rb) ** 2) < 1)
    col = mix(MIST, PERI, 0.15 + 0.5 * smooth(Wp, Lp, 8, rs)); col[..., 3] = 225
    gl = np.zeros_like(col)
    inner = mask & ~erode(mask, 4)
    col[inner] = mix(col, PEARL, 0.45)[inner]
    return finish(col, gl, mask, rs, rim_w=2, sparkle=0.006)

def paint_drape(Lp, Wp, i, tip, rs):
    """縦に垂れる筋の束：細い筋がうねりながら並び、ところどころ横糸と珠でつながる。先はばらばらの長さで終わる。"""
    X, Y, PX, PY = grid(Lp, Wp)
    mask = np.zeros(X.shape, bool); bead = np.zeros(X.shape, bool)
    ns = max(2, Wp // 7)
    links = [rs.uniform(3, Lp - 3) for _ in range(max(1, Lp // 16))]
    for k in range(ns):
        yc = (((k + 0.5) / ns * 2 - 1) * 0.86 + 0.07 * np.sin((i + X) * math.tau * 1.5 + k * 1.7)) * Wp / 2
        end = rs.uniform(0.55, 1.0) if tip else 2.0
        mask |= (np.abs(PY - yc) < 1.4) & (X < end)
        for lx in links:
            if rs.random() < 0.6:
                bead |= ((PX - lx) ** 2 + (PY - yc[:, int(min(Lp - 1, lx))][:, None] * 0 - yc[0, int(min(Lp - 1, lx))]) ** 2) < 6.5
    for lx in links:
        mask |= (np.abs(PX - lx) < 0.8) & (np.abs(Y) < 0.86)
    mask |= bead
    col = mix(DEEP, PERI, 0.2 + 0.7 * smooth(Wp, Lp, 6, rs)); col[..., 3] = 230
    gl = np.zeros_like(col)
    col[bead] = PEARL; gl[bead] = G_RIM * np.array([1, 1, 1, 2.0], np.float32)
    return finish(col, gl, mask, rs, rim_w=1, rim_col=mix(PERI, PEARL, np.float32(0.6)), sparkle=0.004)

def paint_ribbon(Lp, Wp, i, tip, rs):
    """たなびく帯：波打つふち、中央の光る芯、等間隔のひし形の飾り。先へいくほど細く尖る。"""
    X, Y, PX, PY = grid(Lp, Wp)
    env = 0.92 - 0.2 * (0.5 + 0.5 * np.sin((i + X) * math.tau))
    if tip:
        env = env * np.clip(1 - X, 0, 1) ** 0.6
    mask = np.abs(Y) < env
    col = mix(PERI, PALE, smooth(Wp, Lp, 5, rs)); col[..., 3] = 200
    col = mix(col, IRI, (0.5 + 0.5 * np.sin((i + X) * 4)) ** 3 * 0.35)
    gl = np.zeros_like(col)
    core = np.abs(PY) < 0.8
    col[core] = PEARL; gl[core] = G_VEIN
    dia = (np.abs(((PX + 6) % 12) - 6) + np.abs(PY)) < 2.6
    col[dia] = WHITE; gl[dia] = G_STAR * np.array([1, 1, 1, 0.5], np.float32)
    return finish(col, gl, mask, rs, rim_w=1)

def paint_leaf(Lp, Wp, rs, petal=False):
    """葉の形の欠片・襟の花びら：尖った葉の輪郭、中央の葉脈と側脈、楕円の穴。"""
    X, Y, PX, PY = grid(Lp, Wp)
    env = np.sin(math.pi * np.clip(X, 0, 1) ** (0.6 if petal else 0.8)) ** 0.75 * 0.95
    env = env - 0.05 * (0.5 + 0.5 * np.sin(X * math.tau * 3.5))
    mask = np.abs(Y) < env
    mask = punch(mask, PX, PY, rs, 2 + (Lp * Wp) // 500, 2.0, 5.5, Lp, Wp, margin=0.45)
    col = mix(PERI if not petal else VIOL, MIST, X * 0.8 + smooth(Wp, Lp, 6, rs) * 0.3); col[..., 3] = 215
    gl = np.zeros_like(col)
    mid = np.abs(PY) < 0.8
    side = (((PX - np.abs(PY) * 1.1) % 7) < 0.9) & ~mid
    col[side] = mix(col, PEARL, 0.4)[side]
    col[mid] = PEARL; gl[mid] = G_VEIN
    return finish(col, gl, mask, rs, sparkle=0.008)

def paint_steel(w, h, rs, engrave=True):
    """仮面の金属：縦のグラデーション、面取りのハイライト、中央の溝、山形の彫り、鋲。"""
    yy = (np.arange(h)[:, None] / max(1, h - 1)) * np.ones((1, w), np.float32)
    xx = np.arange(w)[None, :] * np.ones((h, 1), np.float32)
    col = mix(STEEL_HI, STEEL, yy * 0.6 + smooth(h, w, 5, rs) * 0.35)
    if engrave and w >= 8 and h >= 16:
        cx = (w - 1) / 2
        d = np.abs(xx - cx)
        chev = ((np.arange(h)[:, None] + d * 0.9) % 10) < 1.2
        col[chev & (d > 1.5) & (d < w / 2 - 2)] = mix(STEEL, STEEL_D, np.float32(0.55))
        col[(d < 1.2) * np.ones((h, w), bool)] = STEEL_D
        col[((d >= 1.2) & (d < 2.2)) * np.ones((h, w), bool)] = WHITE
        riv = (((np.arange(h)[:, None] % 10) == 5) & (np.abs(d - (w / 2 - 2.5)) < 1))
        col[riv] = WHITE
    col[0, :] = WHITE; col[:, 0] = mix(col[:, 0], WHITE, np.float32(0.6))
    col[-1, :] = mix(col[-1, :], STEEL_D, np.float32(0.6)); col[:, -1] = mix(col[:, -1], STEEL_D, np.float32(0.5))
    col[..., 3] = 255
    return col, np.zeros_like(col)

def paint_orb(w, h, rs, a, b, gcol, garef):
    """真珠・宝石：球のような陰影。面の角を透明にして丸く見せる。"""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    r = np.sqrt(((xx + 0.5) / w * 2 - 1) ** 2 + ((yy + 0.5) / h * 2 - 1) ** 2)
    hl = np.sqrt(((xx + 0.5) / w * 2 - 0.6) ** 2 + ((yy + 0.5) / h * 2 - 0.6) ** 2)
    col = mix(a, b, r * 0.9); col = mix(col, WHITE, np.clip(1 - hl * 2.2, 0, 1))
    gl = np.zeros_like(col); gl[...] = gcol; gl[..., 3] = garef * np.clip(1.1 - r, 0, 1)
    if min(w, h) >= 3:
        out = r > 1.08
        col[out] = 0; gl[out] = 0
    return col, gl

def blit(rect, col, gl):
    x, y, w, h = rect
    tex[y:y + h, x:x + w] = col; glow[y:y + h, x:x + w] = gl

import zlib
for key, s in slots.items():
    rs = np.random.default_rng(zlib.crc32(repr(key).encode()))
    parts = s["mat"].split("|"); kind = parts[0]
    i = int(parts[1]) if len(parts) > 1 else 0
    tip = len(parts) > 2 and parts[2] == "1"
    ori = parts[3] if len(parts) > 3 else "u"
    w, h, d = s["size"]
    R = face_rects(*s["uv"], s["size"])
    thin = s["thin"]
    if kind in PLATES and thin is not None:
        def P(Lp, Wp):
            if kind == "fin": return paint_fin(Lp, Wp, i, tip, rs)
            if kind == "lace": return paint_lace(Lp, Wp, i, tip, rs)
            if kind == "drape": return paint_drape(Lp, Wp, i, tip, rs)
            if kind == "ribbon": return paint_ribbon(Lp, Wp, i, tip, rs)
            return paint_leaf(Lp, Wp, rs, petal=(kind == "petal"))
        if thin == 1:      # 水平の板：上面と下面（どちらも u = x、v の上 = 後ろ）
            col, gl = P(w, d)
            blit(R["up"], col, gl)
            blit(R["down"], mix(col, DEEP, np.float32(0.12)) * (col[..., 3:4] > 0), gl)
        elif thin == 2:    # 縦の板：北面と南面（南は左右反転）
            if ori == "u":
                col, gl = P(w, h)
            else:
                col, gl = P(h, w); col, gl = col.transpose(1, 0, 2), gl.transpose(1, 0, 2)
                if ori == "b":
                    col, gl = col[::-1], gl[::-1]
            blit(R["north"], col, gl); blit(R["south"], col[:, ::-1], gl[:, ::-1])
        continue
    for fname, rect in R.items():
        x, y, fw, fh = rect
        if fw <= 0 or fh <= 0:
            continue
        if kind == "lace":
            col, gl = paint_lace(fw, fh, 0, False, rs, full=True)
        elif kind == "face":
            col, gl = paint_steel(fw, fh, rs)
        elif kind == "slit":
            col = np.zeros((fh, fw, 4), np.float32); col[...] = C("#c8f7ff"); gl = np.zeros_like(col); gl[...] = G_CYAN * np.array([1, 1, 1, 0.7], np.float32)
        elif kind == "pearl":
            col, gl = paint_orb(fw, fh, rs, C("#ffffff"), C("#b9b6f0"), C("#eef6ff"), 150)
        elif kind == "gem":
            col, gl = paint_orb(fw, fh, rs, C("#e8feff"), C("#3fb2ee"), G_CYAN, 255)
        elif kind == "thread":
            col = np.zeros((fh, fw, 4), np.float32); col[...] = mix(PEARL, PALE, np.float32(0.3)); col[..., 3] = 220
            gl = np.zeros_like(col)
            if fh > 4:
                gl[::5, :] = G_STAR * np.array([1, 1, 1, 0.5], np.float32)
                gl[-2:, :] = G_STAR; col[-2:, :] = WHITE
        else:
            col = np.zeros((fh, fw, 4), np.float32); col[...] = PERI; gl = np.zeros_like(col)
        blit(rect, col, gl)

Image.fromarray(np.clip(tex, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, "astra_veil.png"), optimize=True)
Image.fromarray(np.clip(glow, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, "astra_veil_glowmask.png"), optimize=True)

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
            nc["uv"] = c["uv"]
            if c.get("mirror"): nc["mirror"] = True
            nb["cubes"].append(nc)
    return nb
geo = {"format_version": "1.12.0", "minecraft:geometry": [{
    "description": {"identifier": "geometry.astra_veil", "texture_width": TEX_W, "texture_height": TEX_H,
                    "visible_bounds_width": 64, "visible_bounds_height": 48, "visible_bounds_offset": [0, 0, 0]},
    "bones": [clean(b) for b in bones]}]}
json.dump(geo, open(os.path.join(OUT, "astra_veil.geo.json"), "w"), separators=(",", ":"), ensure_ascii=False)
ab = {}
for b in bones:
    a = anims.get(b["name"])
    if not a:
        continue
    o = {k: [x if isinstance(x, str) else 0 for x in v] for k, v in a.items() if any(isinstance(x, str) for x in v)}
    if o:
        ab[b["name"]] = o
json.dump({"format_version": "1.8.0", "animations": {"animation.astra_veil.idle": {"loop": True, "animation_length": 8.0, "bones": ab}}},
          open(os.path.join(OUT, "astra_veil.animation.json"), "w"), separators=(",", ":"), ensure_ascii=False)
json.dump({"slots": {str(k): {kk: vv for kk, vv in v.items()} for k, v in slots.items()}, "tex": [TEX_W, TEX_H]}, open(os.path.join(OUT, "_slots.json"), "w"), ensure_ascii=False, default=str)
print("tex", TEX_W, TEX_H, "used_h", v + shelf, "slots", len(slots), "bones", len(bones), "cubes", sum(len(b["cubes"]) for b in bones))
