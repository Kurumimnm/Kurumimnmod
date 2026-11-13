"""
アストラ・ヴェイル（星幽の巡界者）v2：高密度版のモデル・テクスチャ・アニメーションを生成する。
座標は Bedrock / Blockbench 形式（1px = 1/16 ブロック、前は北 -Z）。小さく作り、レンダラーで拡大する（ModConstants ASTRA_VEIL_RENDER_SCALE）。
"""
import json, math, random, sys, os
import numpy as np
from PIL import Image

OUT = sys.argv[1] if len(sys.argv) > 1 else "out2"
os.makedirs(OUT, exist_ok=True)
rnd = random.Random(11)

bones, bone_index = [], {}
anims = {}   # bone name -> dict(rotation=[...], position=[...])
T = "query.anim_time"

def bone(name, parent, pivot, rotation=(0, 0, 0)):
    b = {"name": name, "pivot": [round(p, 3) for p in pivot], "cubes": []}
    if parent:
        b["parent"] = parent
    if any(abs(r) > 1e-9 for r in rotation):
        b["rotation"] = [round(r, 2) for r in rotation]
    bones.append(b); bone_index[name] = b
    return b

def cube(bname, origin, size, mat, rotation=None, pivot=None, label=None, inflate=None):
    size = [int(round(s)) for s in size]
    c = {"origin": [round(o, 3) for o in origin], "size": size, "_mat": mat, "_label": label or bname}
    if rotation:
        c["rotation"] = [round(r, 2) for r in rotation]; c["pivot"] = [round(p, 3) for p in pivot]
    if inflate:
        c["inflate"] = inflate
    bone_index[bname]["cubes"].append(c)
    return c

def wave(phase, amp, speed=45, fn="sin"):
    """8 秒で割り切れる周期（45°/s = 8 秒、90°/s = 4 秒）の揺れ。"""
    if abs(amp) < 0.05:
        return 0
    return f"math.{fn}({T}*{speed}-{phase:.0f})*{amp:.2f}"

def anim(name, rot=None, pos=None):
    a = {}
    if rot:
        a["rotation"] = rot
    if pos:
        a["position"] = pos
    anims[name] = a

# ---------------------------------------------------------------------------
# 横へ伸びる鎖（幕・帯）。side = 1（+x）/ -1（-x）
#   segs: [(w, h, d, rz, ry, rx)]  rz 正 = 先が垂れる（左右対称に side を掛ける）
# ---------------------------------------------------------------------------
def x_chain(prefix, parent, start, segs, mat, side, yalign="top", overlap=2, feathers=None, anim_amp=(2, 1.2),
            anim_speed=45, phase0=0, extra=None):
    x, y, z = start
    names = []
    for i, (w, h, d, rz, ry, rx) in enumerate(segs):
        name = f"{prefix}_{i}"
        bone(name, parent, (x * side, y, z), (rx, ry * side, rz * side))
        if yalign == "top":
            oy = y - h + 2
        else:
            oy = y - h / 2
        ox = x if side > 0 else -x - w
        cube(name, (ox, oy, z - d / 2), (w, h, d), mat)
        if feathers:
            fw, fh, n = feathers
            for k in range(n):
                fx = x + 2 + k * max(1, (w - 4) // max(1, n))
                fname = f"{name}_f{k}"
                bone(fname, name, (fx * side, oy + 3, z - d / 2 - 0.5), (10 + rnd.uniform(-4, 4), 0, (-28 - k * 14 + rnd.uniform(-5, 5)) * side))
                fww = max(3, int(fw * (1 - i * 0.07)))
                cube(fname, (fx if side > 0 else -fx - fww, oy + 3 - fh, z - d / 2 - 1), (fww, fh, 1), "feather")
                anim(fname, rot=[wave(phase0 + i * 30 + k * 50 + 40, 3 + i * 0.4, 90), 0, wave(phase0 + i * 30 + k * 40, (2 + i * 0.3) * side, 90)])
        if extra:
            extra(name, i, x, y, z, w, h)
        a0, a1 = anim_amp
        anim(name, rot=[wave(phase0 + i * 32 + 60, a1 + i * 0.4, anim_speed * 2), 0, wave(phase0 + i * 30, (a0 + i * 0.9) * side, anim_speed)])
        names.append(name)
        parent = name
        x += w - overlap
    return names

# ---------------------------------------------------------------------------
# 下へ伸びる鎖（触腕）
# ---------------------------------------------------------------------------
def y_chain(prefix, parent, start, segs, mat, phase0=0, amp=1.0):
    x, y, z = start
    for i, (w, d, L, rx, rz) in enumerate(segs):
        name = f"{prefix}_{i}"
        bone(name, parent, (x, y, z), (rx, 0, rz))
        cube(name, (x - w / 2, y - L, z - d / 2), (w, L, d), mat)
        anim(name, rot=[wave(phase0 + i * 34, (1.5 + i * 0.9) * amp, 90), 0, wave(phase0 + i * 30 + 90, (1.2 + i * 0.8) * amp, 45, "cos")])
        parent = name
        y -= L - 1

# ===========================================================================
# 骨格
# ===========================================================================
bone("root", None, (0, 0, 0))
bone("body", "root", (0, 8, 0))
anim("body", rot=[wave(90, 1.2), wave(0, 1.6), 0], pos=[0, wave(0, 1.5), 0])

# ---------------------------------------------------------------------------
# 顔（縦長の銀白の仮面＋冠）
# ---------------------------------------------------------------------------
bone("head", "body", (0, 14, -2))
anim("head", rot=[wave(200, 1.8), wave(120, 1.2, 90), wave(40, 0.8, 90)])
H = "head"
cube(H, (-5, 0, -7), (10, 36, 5), "face", label="face_spine")
cube(H, (-1, 1, -8), (2, 32, 1), "slit", label="face_slit")
for side in (1, -1):
    t = "l" if side > 0 else "r"
    # 縦の稜線
    cube(H, (3.5 if side > 0 else -5.5, 2, -8), (2, 30, 1), "face", label=f"ridge_{t}")
    # 頬（斜めに張り出す板を 2 段）
    cube(H, (5 if side > 0 else -11, 10, -5), (6, 16, 2), "crown", rotation=(0, 10 * side, 16 * side), pivot=(5 * side, 26, -4), label=f"cheek_{t}")
    cube(H, (9 if side > 0 else -15, 4, -3), (6, 14, 2), "crown", rotation=(0, 18 * side, 28 * side), pivot=(9 * side, 18, -2), label=f"cheek2_{t}")
    # 下あごの牙のような板
    cube(H, (2 if side > 0 else -5, -12, -6), (3, 14, 2), "face", rotation=(0, 0, -10 * side), pivot=(3 * side, 2, -5), label=f"mandible_{t}")
    # 目（レンズ）
    for i, (lx, ly) in enumerate([(3, 27), (6, 29), (8.5, 26), (5, 23)]):
        cube(H, (lx - 1 if side > 0 else -lx - 1, ly - 1, -8.5), (2, 2, 1), "lens", label=f"lens_{t}{i}")
# 眉・冠（上へ広がる段）
for i, (w, y, d) in enumerate([(14, 30, 6), (18, 34, 6), (22, 37, 5), (16, 40, 4)]):
    cube(H, (-w / 2, y, -6 + i * 0.5), (w, 4, d), "crown", label=f"crown_{i}")
# 冠の枝角（左右に放射状、2 段）
for side in (1, -1):
    t = "l" if side > 0 else "r"
    for i in range(7):
        ang = 6 + i * 11
        h = 12 - i * 0.8
        px = (1.5 + i * 1.6) * side
        cube(H, (px - 0.5, 42, -3), (1, h, 1), "crown", rotation=(rnd.uniform(-8, 8), 0, -ang * side), pivot=(px, 42, -2.5), label=f"spike_{t}{i}")
        if i % 2 == 0:
            cube(H, (px - 0.5, 42 + h * 0.5, -3), (1, h * 0.45, 1), "crown", rotation=(0, 0, -(ang + 28) * side), pivot=(px, 42 + h * 0.5, -2.5), label=f"twig_{t}{i}")
# 冠の上の宝玉の房
for i, (ox, oy) in enumerate([(0, 46), (-2.5, 44), (2.5, 44), (-1.2, 49), (1.2, 49), (0, 53)]):
    cube(H, (ox - 1, oy, -4), (2, 2, 2), "lens", label=f"orb_{i}")
cube(H, (-0.5, 44, -3.5), (1, 16, 1), "antenna", label="horn")
# 胸の核と、下へ伸びる節のある柱
cube(H, (-2.5, 4, -9), (5, 6, 2), "core", label="core")
for i, w in enumerate([8, 7, 6, 5, 4, 3]):
    y = -12 - i * 7
    cube(H, (-w / 2, y, -5 + i * 0.3), (w, 6, 4), "face", label=f"column_{i}")
    cube(H, (-1, y + 2, -6 + i * 0.3), (2, 2, 1), "core", label=f"column_gem_{i}")

# ---------------------------------------------------------------------------
# 胴（顔の後ろ〜下の幕の重なり）と、顔のまわりの珊瑚のような房
# ---------------------------------------------------------------------------
cube("body", (-9, -16, -2), (18, 26, 8), "membrane", label="torso")
cube("body", (-12, -30, -1), (24, 16, 6), "membrane", label="torso_low")
cube("body", (-8, -40, 0), (16, 12, 5), "membrane", label="torso_tail")
for k in range(12):
    side = 1 if k % 2 == 0 else -1
    j = k // 2
    sx = (6 + j * 0.8) * side
    sy = 34 - j * 7.5
    base = (35 + j * 12) * side
    segs = []
    for i in range(5):
        segs.append((5 - i, 1, 5, rnd.uniform(-10, 10), -base if i == 0 else -(22 + i * 8) * side))
    # 横向きの短い巻きひげ（y_chain を横に倒して使う）
    y_chain(f"coral_{k}", "body", (sx, sy, -1.5 - (k % 3)), segs, "coral", phase0=k * 40, amp=0.7)

# ---------------------------------------------------------------------------
# 背中の扇（頭の後ろに放射状に立つ幕）
# ---------------------------------------------------------------------------
for k in range(9):
    ang = -72 + k * 18
    segs = []
    for i, (w, L) in enumerate([(7, 16), (6, 14), (5, 12), (4, 10), (3, 8)]):
        segs.append((w, 1, L, rnd.uniform(-5, 5) - 4, (180 + ang) if i == 0 else -ang * 0.18 + rnd.uniform(-4, 4)))
    y_chain(f"fan_{k}", "body", (ang * 0.06, 40, 5 + abs(ang) * 0.02), segs, "feather", phase0=k * 30, amp=0.8)

# ---------------------------------------------------------------------------
# 翼（3 層の幕。それぞれの節の下に羽のようなひだが重なる）
# ---------------------------------------------------------------------------
WING_A = [(26, 28, 2, -22, -4, 0), (25, 27, 2, -6, -4, 0), (24, 26, 2, -2, -4, 0), (22, 25, 2, 2, -3, 0), (20, 23, 2, 5, -3, 0),
          (18, 21, 2, 9, -3, 0), (15, 18, 2, 13, -2, 0), (12, 15, 2, 19, -2, 0), (9, 12, 2, 27, 0, 0), (6, 9, 2, 38, 0, 0)]
WING_B = [(24, 24, 2, 0, -2, 0), (22, 23, 2, 3, -2, 0), (20, 22, 2, 5, -2, 0), (18, 20, 2, 8, -2, 0),
          (16, 18, 2, 11, -2, 0), (13, 15, 2, 15, 0, 0), (10, 12, 2, 22, 0, 0), (7, 9, 2, 30, 0, 0)]
WING_C = [(20, 22, 2, 30, 4, 0), (18, 20, 2, 6, 2, 0), (16, 18, 2, 7, 2, 0), (14, 16, 2, 10, 0, 0),
          (11, 13, 2, 14, 0, 0), (8, 10, 2, 20, 0, 0), (6, 8, 2, 28, 0, 0)]
wing_names = {}
for side, t in ((1, "l"), (-1, "r")):
    wing_names[(t, "a")] = x_chain(f"wing_a{t}", "body", (6, 46, 2), WING_A, "membrane", side, overlap=3, feathers=(8, 12, 2), anim_amp=(2.5, 1.5), phase0=0)
    wing_names[(t, "b")] = x_chain(f"wing_b{t}", "body", (8, 34, -0.5), WING_B, "membrane", side, overlap=3, feathers=(7, 11, 2), anim_amp=(2.8, 1.5), phase0=50)
    wing_names[(t, "c")] = x_chain(f"wing_c{t}", "body", (7, 20, -3), WING_C, "membrane", side, overlap=3, feathers=(6, 10, 1), anim_amp=(3, 1.8), phase0=100)

# ---------------------------------------------------------------------------
# 渦を巻く長い帯（翼の先・ふちから出て、外へ流れてくるりと巻く）
# ---------------------------------------------------------------------------
TENDRILS = [  # (親の層, 節, 付け根の x 割合, 初めの向き rz, 巻きの強さ, 巻く向き, 本数の節)
    ("a", 1, 0.5, -38, 26, -1, 15),
    ("a", 4, 0.8, -18, 30, 1, 15),
    ("a", 7, 0.9, -6, 34, 1, 14),
    ("a", 9, 1.0, 8, 38, 1, 12),
    ("b", 5, 0.9, 16, 30, -1, 14),
    ("c", 4, 0.9, 30, 32, 1, 13),
]
for side, t in ((1, "l"), (-1, "r")):
    for k, (layer, si, fx, rz0, curl, cdir, n) in enumerate(TENDRILS):
        parent = wing_names[(t, layer)][si]
        pb = bone_index[parent]
        pc = pb["cubes"][0]
        px = abs(pb["pivot"][0]) + pc["size"][0] * fx
        py = pc["origin"][1] + pc["size"][1] - 4
        pz = pb["pivot"][2] - 1.5
        segs = []
        for i in range(n):
            w = max(3, 9 - i * 0.35)
            h = max(2, 6 - i * 0.3)
            q = i / (n - 1)
            rz = rz0 if i == 0 else cdir * curl * (0.08 + q ** 2.2) + rnd.uniform(-2, 2)
            segs.append((w, h, 1, rz, rnd.uniform(-5, 5), rnd.uniform(-4, 4)))
        x_chain(f"tendril_{t}{k}", parent, (px, py, pz), segs, "ribbon", side, yalign="mid", overlap=1,
                anim_amp=(1.5, 2.0), anim_speed=45, phase0=k * 55)

# ---------------------------------------------------------------------------
# 下の幕（腰から左右斜め下へ 2 段）
# ---------------------------------------------------------------------------
for side, t in ((1, "l"), (-1, "r")):
    x_chain(f"skirt_a{t}", "body", (8, 4, -2), [(18, 24, 2, 40, 8, 0), (15, 21, 2, 14, 4, 0), (12, 17, 2, 12, 0, 0), (9, 13, 2, 12, 0, 0), (6, 9, 2, 14, 0, 0)],
            "membrane", side, feathers=(7, 12, 2), anim_amp=(3, 2), phase0=150)
    x_chain(f"skirt_b{t}", "body", (6, -14, 1), [(16, 22, 2, 58, -6, 0), (13, 19, 2, 16, -4, 0), (10, 15, 2, 12, 0, 0), (7, 11, 2, 12, 0, 0)],
            "membrane", side, feathers=(6, 10, 1), anim_amp=(3, 2), phase0=200)

# ---------------------------------------------------------------------------
# 触腕（外側：平たい葉のような帯 16 本、内側：細い糸 12 本）
# ---------------------------------------------------------------------------
tent = 0
for k in range(16):
    ang = k / 16 * math.tau
    tx, tz = math.cos(ang) * 11, math.sin(ang) * 4
    n = rnd.choice([7, 8, 8, 9])
    spread = tx * 1.8
    curl = rnd.choice([-1, 1]) * rnd.uniform(5, 9)
    segs = []
    for i in range(n):
        w = max(2, 8 - i * 0.7)
        rz = -spread if i == 0 else (curl * (i - n + 5) * 0.9 * (1 if tx > 0 else -1) if i >= n - 5 else -spread * 0.1 + rnd.uniform(-3, 3))
        rx = (4 + tz * 2) if i == 0 else rnd.uniform(-5, 5)
        segs.append((w, 1, 9, rx, rz))
    y_chain(f"tentacle_{k}", "body", (tx, -36, tz), segs, "flat", phase0=k * 37)
for k in range(8):
    ang = (k + 0.5) / 8 * math.tau
    tx, tz = math.cos(ang) * 7, math.sin(ang) * 3
    n = rnd.choice([7, 8, 9])
    segs = []
    for i in range(n):
        w = max(1, 3 - i // 4)
        rz = (-tx * 1.4) if i == 0 else rnd.uniform(-5, 5)
        segs.append((w, w, 10, rnd.uniform(-5, 5), rz))
    y_chain(f"strand_{k}", "body", (tx, -40, tz), segs, "tentacle", phase0=k * 41 + 20, amp=1.3)

# ===========================================================================
# Box UV の割り付け（同じ大きさ・同じ材質は UV を共有）
# ===========================================================================
UNIQUE = ("face", "core", "slit")
def foot(size):
    w, h, d = size
    return 2 * (w + d), d + h

slots = {}
for b in bones:
    for c in b["cubes"]:
        key = (tuple(c["size"]), c["_mat"]) + ((c["_label"],) if c["_mat"] in UNIQUE else ())
        s = slots.setdefault(key, {"size": c["size"], "mat": c["_mat"], "labels": []})
        s["labels"].append(c["_label"] if c["_label"] != b["name"] else b["name"])
        c["_key"] = key

TEX_W = 512
order = sorted(slots.keys(), key=lambda k: (-foot(slots[k]["size"])[1], -foot(slots[k]["size"])[0]))
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
# テクスチャ
# ===========================================================================
tex = np.zeros((TEX_H, TEX_W, 4), np.float32)
glow = np.zeros((TEX_H, TEX_W, 4), np.float32)
def C(s, a=255):
    s = s.lstrip("#"); return np.array([int(s[i:i + 2], 16) for i in (0, 2, 4)] + [a], np.float32)
def L(a, b, t):
    return a + (b - a) * max(0.0, min(1.0, t))

DEEP = C("#3b2a9e", 200); VIOLET = C("#5d4be0", 180); SKY = C("#8fb2ff", 160); MIST = C("#c9d4ff", 150)
PEARL = C("#e4e2ff", 225); WHITE = C("#ffffff", 250)
G_RIM = C("#b9e4ff", 70); G_VEIN = C("#8fd2ff", 90); G_STAR = C("#e6f6ff", 220); G_CYAN = C("#8ff3ff", 255)

def put(x, y, col, g=None):
    tex[y, x] = col
    if g is not None:
        glow[y, x] = g

def paint_membrane(x, y, w, h, fname, lace=1.0):
    big = w >= 6 and h >= 6
    for j in range(h):
        for i in range(w):
            t = j / max(1, h - 1)
            col = L(DEEP, VIOLET, t * 1.6) if t < 0.5 else L(VIOLET, SKY, (t - 0.5) * 2)
            col = L(col, MIST, rnd.random() * 0.35)
            # 細かい鱗の模様
            if (i + (j // 2) * 2) % 4 == 0 and (j % 2 == 0):
                col = L(col, MIST, 0.45)
            put(x + i, y + j, col)
            if rnd.random() < 0.03:
                put(x + i, y + j, WHITE, G_STAR)
    if not big:
        for i in range(w):
            put(x + i, y + h - 1, PEARL, G_RIM)
            put(x + i, y, PEARL)
        return
    if fname not in ("north", "south"):
        for j in range(h):
            put(x, y + j, PEARL, G_RIM)
        return
    # 放射状の葉脈
    for vline in range(max(2, w // 5)):
        sx = x + 1 + (vline * (w - 2)) // max(1, w // 5)
        drift = rnd.uniform(-0.25, 0.25)
        for j in range(h - 2):
            xx = int(round(sx + drift * j + math.sin(j * 0.5 + vline) * 0.8))
            if x < xx < x + w - 1:
                put(xx, y + j, L(PEARL, SKY, 0.3), G_VEIN)
    # レースの穴（大小）
    for _ in range(int(max(1, (w * h) // 55) * lace)):
        r = rnd.choice([1, 1, 1, 2, 2, 3])
        if w <= 4 + 2 * r or h <= 6 + 2 * r:
            continue
        hx = rnd.randint(x + 2 + r, x + w - 3 - r); hy = rnd.randint(y + 3 + r, y + h - 4 - r)
        for j in range(-r - 1, r + 2):
            for i in range(-r - 1, r + 2):
                d2 = i * i + j * j
                if d2 <= r * r:
                    tex[hy + j, hx + i] = 0; glow[hy + j, hx + i] = 0
                elif d2 <= (r + 1) ** 2 + 1:
                    put(hx + i, hy + j, PEARL, G_RIM * np.array([1, 1, 1, 0.6]))
    # 下のふち：真珠色＋不規則な欠け
    for i in range(w):
        cut = 0 if i in (0, w - 1) else rnd.choice([0, 1, 1, 2, 3])
        for jj in range(cut):
            tex[y + h - 1 - jj, x + i] = 0; glow[y + h - 1 - jj, x + i] = 0
        put(x + i, y + h - 1 - cut, WHITE, G_RIM)
        if h - 2 - cut > 0:
            put(x + i, y + h - 2 - cut, PEARL)

def paint_feather(x, y, w, h, fname):
    for j in range(h):
        for i in range(w):
            t = j / max(1, h - 1)
            col = L(VIOLET, SKY, t)
            if (i - j // 2) % 3 == 0:
                col = L(col, MIST, 0.55)
            put(x + i, y + j, L(col, MIST, rnd.random() * 0.25))
    if fname in ("north", "south"):
        cx = x + w // 2
        for j in range(h):
            put(cx, y + j, PEARL, G_VEIN)
        # 先をとがらせる（下の角を透明に）
        for j in range(h):
            k = max(0, (j - (h - w // 2 - 1)))
            for i in range(k):
                if x + i < cx:
                    tex[y + j, x + i] = 0; glow[y + j, x + i] = 0
                if x + w - 1 - i > cx:
                    tex[y + j, x + w - 1 - i] = 0; glow[y + j, x + w - 1 - i] = 0
        for i in range(w):
            if tex[y + h - 1, x + i, 3] > 0:
                put(x + i, y + h - 1, WHITE, G_RIM)

def paint_ribbon(x, y, w, h, fname):
    for j in range(h):
        for i in range(w):
            edge = j == 0 or j == h - 1
            col = PEARL if edge else L(VIOLET, SKY, rnd.random() * 0.6 + 0.2)
            if not edge and rnd.random() < 0.15:
                col = MIST
            put(x + i, y + j, col, G_RIM if j == h - 1 else (G_STAR if rnd.random() < 0.03 else None))
    if fname in ("north", "south") and w >= 4 and h >= 3:
        for i in range(0, w, 3):
            tex[y + h // 2, x + i] = 0

def paint_flat(x, y, w, h, fname):
    for j in range(h):
        for i in range(w):
            t = j / max(1, h - 1)
            col = L(VIOLET, MIST, t * 0.8 + rnd.random() * 0.2)
            put(x + i, y + j, col, G_STAR if rnd.random() < 0.03 else None)
    if fname in ("north", "south") and w >= 3:
        for j in range(h):
            put(x, y + j, PEARL, G_RIM); put(x + w - 1, y + j, PEARL, G_RIM)
            if w >= 5 and j % 4 == 1:
                put(x + w // 2, y + j, PEARL, G_VEIN)
        put(x + w // 2, y + h - 1, WHITE, G_STAR)

def paint_strand(x, y, w, h, fname):
    for j in range(h):
        for i in range(w):
            put(x + i, y + j, L(SKY, PEARL, (j % 4) / 4 * 0.6 + 0.2), G_RIM if j % 4 == 0 else None)

SIL_A = C("#f2f4fc"); SIL_B = C("#a8b0d8"); SIL_D = C("#5a5f96")
CR_A = C("#d8d4fb"); CR_B = C("#6b64c9")
def paint_solid(x, y, w, h, a, b, gcol=None, gp=0.0, rim=None):
    for j in range(h):
        for i in range(w):
            put(x + i, y + j, L(a, b, j / max(1, h - 1) * 0.75 + rnd.random() * 0.25), gcol if (gcol is not None and rnd.random() < gp) else None)
    if rim is not None:
        for i in range(w):
            put(x + i, y, rim); put(x + i, y + h - 1, rim)
        for j in range(h):
            put(x, y + j, rim); put(x + w - 1, y + j, rim)

def paint_face(x, y, w, h, fname, label):
    paint_solid(x, y, w, h, SIL_A, SIL_B, rim=C("#ffffff"))
    if fname != "north":
        return
    # 彫り込みの横帯
    for j in range(3, h - 1, 4):
        for i in range(1, w - 1):
            put(x + i, y + j, L(SIL_B, SIL_D, 0.5))
    if label == "face_spine":
        cx = x + w // 2
        for j in range(1, h - 1):
            put(cx - 1, y + j, SIL_D); put(cx, y + j, SIL_D)
        # 左右対称の紋様
        for j in range(2, h - 2, 6):
            for i in (1, 2):
                put(cx - 2 - i, y + j + i, C("#7d84bd")); put(cx + 1 + i, y + j + i, C("#7d84bd"))

for k, s in slots.items():
    u0, v0 = s["uv"]
    lab = s["labels"][0]
    for fname, (x, y, w, h) in face_rects(u0, v0, s["size"]).items():
        if w <= 0 or h <= 0:
            continue
        m = s["mat"]
        if m == "membrane":
            paint_membrane(x, y, w, h, fname)
        elif m == "feather":
            paint_feather(x, y, w, h, fname)
        elif m == "ribbon":
            paint_ribbon(x, y, w, h, fname)
        elif m in ("flat", "coral"):
            paint_flat(x, y, w, h, fname)
        elif m == "tentacle":
            paint_strand(x, y, w, h, fname)
        elif m == "face":
            paint_face(x, y, w, h, fname, lab)
        elif m == "slit":
            for j in range(h):
                for i in range(w):
                    lit = (j % 3 != 0) and j > h * 0.15
                    put(x + i, y + j, C("#7ff0ff") if lit else C("#2b2d66"), G_CYAN if lit else None)
        elif m == "crown":
            paint_solid(x, y, w, h, CR_A, CR_B, C("#bff6ff", 170), 0.08, rim=C("#f4f2ff"))
        elif m == "core":
            paint_solid(x, y, w, h, C("#dffcff"), C("#36a6ec"), G_CYAN, 1.0)
        elif m == "lens":
            paint_solid(x, y, w, h, C("#f2feff"), C("#64d2f0"), C("#d6fcff"), 1.0)
        elif m == "antenna":
            paint_solid(x, y, w, h, C("#f8f9ff"), C("#b2bbe6"), C("#dff8ff", 230), 0.6)

Image.fromarray(np.clip(tex, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, "astra_veil.png"))
Image.fromarray(np.clip(glow, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, "astra_veil_glowmask.png"))

# ===========================================================================
# 書き出し
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
            if "inflate" in c: nc["inflate"] = c["inflate"]
            nc["uv"] = c["uv"]; nb["cubes"].append(nc)
    return nb

geo = {"format_version": "1.12.0", "minecraft:geometry": [{
    "description": {"identifier": "geometry.astra_veil", "texture_width": TEX_W, "texture_height": TEX_H,
                    "visible_bounds_width": 16, "visible_bounds_height": 14, "visible_bounds_offset": [0, 0, 0]},
    "bones": [clean(b) for b in bones]}]}
json.dump(geo, open(os.path.join(OUT, "astra_veil.geo.json"), "w"), indent="\t", ensure_ascii=False)

def tidy(v):
    return v if isinstance(v, str) else 0
anim_bones = {}
for b in bones:
    a = anims.get(b["name"])
    if not a:
        continue
    out = {}
    for k in ("rotation", "position"):
        if k in a and any(isinstance(x, str) for x in a[k]):
            out[k] = [tidy(x) for x in a[k]]
    if out:
        anim_bones[b["name"]] = out
json.dump({"format_version": "1.8.0", "animations": {"animation.astra_veil.idle": {"loop": True, "animation_length": 8.0, "bones": anim_bones}}},
          open(os.path.join(OUT, "astra_veil.animation.json"), "w"), indent="\t", ensure_ascii=False)
json.dump({"slots": {str(k): v for k, v in slots.items()}, "tex": [TEX_W, TEX_H]}, open(os.path.join(OUT, "_slots.json"), "w"), ensure_ascii=False, default=str)
print("tex", TEX_W, TEX_H, "slots", len(slots), "bones", len(bones), "cubes", sum(len(b["cubes"]) for b in bones), "animated", len(anim_bones))
