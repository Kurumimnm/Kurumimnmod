"""
アストラ（星幽の巡界者）：設定画に合わせた超大型モデル（全幅 約 500 ブロック）の生成スクリプト。
アストラ・ヴェイル（tools/astra_veil）とは別のモデル。geo.json・テクスチャ・発光マスク・待機アニメを出力する。

設計（設定画から読み取った形）：
  - 4 本の「腕／羽」：上の 2 枚は外上へ、下の 2 枚は外下へ広がる巨大な膜。1 枚が 100 ブロック四方のフィールドを覆う大きさ。
    膜は肩を中心とした扇形で、傘の骨のような筋・楕円の穴（レース）・真珠色のふちを「扇全体でつながった 1 枚の絵」として描く
  - 頭：前へ突き出すくさび形の立体（上から見ると三角形）。体と同じ紫〜藤色の肌、4 つの光る目、下へ開くあごと牙、後ろへ流れる角
  - 胴：顔の後ろから下へ垂れる穴だらけの格子の柱。まわりにカーテン・触腕・襟の葉
  - 後ろへ長くたなびく尾の膜と帯（横からのシルエットが前後に長い）
  - まわりに漂う欠片と星の粒

単位：1 ピクセル = 1/2 ブロック（レンダラーで 8 倍。ModConstants.ASTRA_RENDER_SCALE）。座標は Bedrock / Blockbench 形式（前は北 -Z）。
原点（エンティティの位置）= 胴の中心の真下。頭は y = 110〜150 ピクセルあたりで、前（-z）へ約 45 ブロック突き出す。
回転の向き：ry 正 = +x の先が前（-z）へ、rz 正 = +x の先が下へ、rx 正 = +z の先が上へ。左右反転は x・ry・rz の符号を反転。
"""
import json, math, os, random, sys, zlib
import numpy as np
from PIL import Image

OUT = sys.argv[1] if len(sys.argv) > 1 else "out"
NAME = "astra"
ANIM_LEN = 16.0   # 待機アニメの長さ（秒）。波の速さ（度/秒）は 22.5 の倍数にするとつなぎ目なくループする
os.makedirs(OUT, exist_ok=True)
rnd = random.Random(41)

bones, bone_index, anims = [], {}, {}
PLATES = ("wing", "lace", "drape", "ribbon", "shard", "frond", "talon")   # 薄い板（輪郭・穴を透明で切り抜く）
T = "query.anim_time"

def bone(name, parent, pivot, rotation=(0, 0, 0)):
    b = {"name": name, "pivot": [round(p, 3) for p in pivot], "cubes": []}
    if parent:
        b["parent"] = parent
    if any(abs(r) > 1e-9 for r in rotation):
        b["rotation"] = [round(r, 2) for r in rotation]
    bones.append(b); bone_index[name] = b
    return name

def cube(bname, origin, size, mat, rotation=None, pivot=None, label=None, mirror=False, art=None, akey=None):
    size = [max(1, int(round(s))) for s in size]
    c = {"origin": [round(o, 3) for o in origin], "size": size, "_mat": mat, "_label": label or bname, "_art": art, "_akey": akey}
    if mirror:
        c["mirror"] = True
    if mat.split("|")[0] in PLATES and size.count(1) == 1:
        c["_thin"] = size.index(1)
    if rotation:
        c["rotation"] = [round(r, 2) for r in rotation]; c["pivot"] = [round(p, 3) for p in pivot]
    bone_index[bname]["cubes"].append(c)
    return c

def wave(phase, amp, speed=22.5, fn="sin"):
    if abs(amp) < 0.05:
        return 0
    return f"math.{fn}({T}*{speed}-{phase:.0f})*{amp:.2f}"

def anim(name, rot=None, pos=None):
    anims[name] = {k: v for k, v in (("rotation", rot), ("position", pos)) if v}

# ---------------------------------------------------------------------------
# 鎖：+x 方向へ節を連ねる（side = 1 / -1 で左右反転）。shape="flat" 水平の板 / "vert" 縦の板・角材
# ---------------------------------------------------------------------------
def chain(prefix, parent, start, segs, side=1, shape="flat", overlap=1, anim_fn=None, deco=None, var=0):
    x, y, z = start
    names = []
    for i, s in enumerate(segs):
        name = bone(f"{prefix}_{i}", parent, (x * side, y, z), (s.get("rx", 0), s.get("ry", 0) * side, s.get("rz", 0) * side))
        L, W, H = s["L"], s["W"], s.get("H", 1)
        ox = x if side > 0 else -x - L
        m = f"{s['mat']}|{i}|{int(i == len(segs) - 1)}|u|{var}"
        if shape == "flat":
            cube(name, (ox, y - H / 2, z - W / 2), (L, H, W), m, mirror=side < 0)
        else:
            cube(name, (ox, y - W / 2, z - H / 2), (L, W, H), m, mirror=side < 0)
        if deco:
            deco(name, i, len(segs), ox, y, z, L, W)
        if anim_fn:
            anim(name, rot=anim_fn(i, side))
        names.append(name); parent = name
        x += L - overlap
    return names

# 下へ垂れる鎖（カーテン・触腕）。板は yaw の向きに面を向ける
def hang(prefix, parent, start, segs, yaw=0.0, anim_fn=None):
    x, y, z = start
    names = []
    for i, s in enumerate(segs):
        name = bone(f"{prefix}_{i}", parent, (x, y, z), (s.get("rx", 0), yaw if i == 0 else 0, s.get("rz", 0)))
        W, L, H = s["W"], s["L"], s.get("H", 1)
        cube(name, (x - W / 2, y - L, z - H / 2), (W, L, H), f"{s['mat']}|{i}|{int(i == len(segs) - 1)}|v|{rnd.randint(0, 1)}")
        if anim_fn:
            anim(name, rot=anim_fn(i))
        names.append(name); parent = name
        y -= L - 1
    return names

def tip_x(name):
    """鎖の最後の節の先端の x（絶対値）。"""
    b = bone_index[name]; c = b["cubes"][0]
    return abs(b["pivot"][0]) + c["size"][0] - 1

# ===========================================================================
bone("root", None, (0, 0, 0))
bone("body", "root", (0, 0, 0))
anim("body", rot=[wave(90, 1.2), wave(0, 1.0), wave(45, 0.8)], pos=[0, wave(0, 10.0), 0])

# ---------------------------------------------------------------------------
# 膜（腕／羽・尾）：肩を中心とした扇形。短冊（ひれ）を扇状に並べ、絵は極座標（肩からの距離 R・角度 θ）で
# 扇全体につながるように描く（paint_wing）。短冊どうしは重ならないように自分の角度の範囲だけを描く。
# ---------------------------------------------------------------------------
WINGS = {}
def build_wing(tag, sides, apex, G, th_back, th_front, n_strips, r0, r_lead, trail, L, shape_fn, mode="wing", ribs=(), seed=1, phase=0):
    spec = dict(th_back=th_back, th_front=th_front, r0=r0, r_lead=r_lead, trail=trail, mode=mode, seed=seed, n=n_strips)
    dth = (th_front - th_back) / n_strips
    out = {}
    for side in sides:
        t = {1: "l", -1: "r", 0: "c"}[side]
        sd = side or 1
        root = bone(f"{tag}_{t}", "body", (apex[0] * sd, apex[1], apex[2]), (G[0], G[1] * sd, G[2] * sd))
        anim(root, rot=[wave(phase, 2.0), wave(phase + 60, 1.2 * sd), wave(phase + 120, 2.5 * sd)])
        strips = []
        for k in range(n_strips):
            q = (k + 0.5) / n_strips                 # 0 = 後ろのふち、1 = 前のふち
            th = th_back + dth * (k + 0.5)
            rmax = max(wing_rmax(spec, qq) for qq in (k / n_strips, q, (k + 1) / n_strips)) * 1.02
            n = max(2, math.ceil((rmax - r0) / (L - 1)))
            segs = []
            for i in range(n):
                r_i = r0 + i * (L - 1)
                W = int(math.ceil((r_i + L) * math.radians(dth))) + 2
                rx, rz = shape_fn(k, q, i, n, sd)
                segs.append(dict(L=L, W=W, rx=rx, rz=rz, ry=(th if i == 0 else 0), mat="wing", r=r_i))
            # 短冊の鎖（絵は左右で共有：ラベルから左右を外して UV をまとめる）
            x, y, z = apex[0] + r0, apex[1], apex[2]
            names = []; parent = root
            for i, s in enumerate(segs):
                name = bone(f"{tag}_{t}{k}_{i}", parent, (x * sd, y, z), (s["rx"], s["ry"] * sd, s["rz"] * sd))
                ox = x if sd > 0 else -x - L
                last = i == n - 1
                cube(name, (ox, y - 0.5, z - s["W"] / 2), (L, 1, s["W"]), "wing", mirror=sd < 0, label=f"{tag}_{k}_{i}",
                     akey=(tag, k, i), art=dict(spec=spec, r=s["r"], th=th, dth=dth, last=last))
                if k in ribs:   # 傘の骨（膜の上に載る骨。先へいくほど細い）
                    th_px = max(1, int(round(ribs[k] * (1 - i / n) ** 0.8)))
                    cube(name, (ox, y + 0.5, z - th_px / 2), (L, th_px, th_px), f"bone|{min(i, 9)}|{int(last)}", mirror=sd < 0, label=f"{tag}_rib")
                wing_deco(tag, name, k, q, i, n, ox, y, z, L, s["W"])
                amp = 1.2 + 0.45 * i
                anim(name, rot=[wave(i * 34 + k * 19 + phase + 90, 0.8 + 0.18 * i, 45), 0, wave(i * 38 + k * 23 + phase, amp * sd)])
                names.append(name); parent = name
                x += L - 1
            strips.append(names)
        out[t] = strips
    WINGS[tag] = out
    return out

def wing_rmax(spec, q):
    """膜の外周（肩からの距離）。wing：前のふちが長く後ろが短い。tail：中央が長い。ふちは波形。"""
    q = np.clip(q, 0, 1)
    if spec["mode"] == "wing":
        base = spec["trail"] + (1 - spec["trail"]) * q ** 0.7
    else:
        base = spec["trail"] + (1 - spec["trail"]) * np.sin(math.pi * q) ** 0.8
    sc = 1 - 0.07 * (0.5 + 0.5 * np.cos(q * spec["n"] * 1.5 * math.tau + spec["seed"])) - 0.03 * np.sin(q * 23 + spec["seed"] * 2)
    return spec["r_lead"] * base * sc

def wing_deco(tag, name, k, q, i, n, ox, y, z, L, W):
    """膜の裏から垂れるもの：細い糸、真珠の粒、後ろのふちと先に下がるぼろぼろの葉。"""
    if i >= 1:
        for _ in range(rnd.choice([0, 0, 1, 1])):
            ln = int(rnd.uniform(6, 20 + 26 * (1 - q)))
            cube(name, (ox + rnd.uniform(1, L - 2), y - 0.5 - ln, z + rnd.uniform(-0.45, 0.45) * W), (1, ln, 1), "thread", label="thread")
    if i < 6 and rnd.random() < 0.5:
        sz = rnd.choice([2, 2, 3, 3, 4])
        cube(name, (ox + rnd.uniform(1, L - 4), y + 0.5, z + rnd.uniform(-0.35, 0.35) * W), (sz, sz, sz), "pearl", label="pearl")
    if i >= 3 and rnd.random() < (0.3 if (q < 0.3 or i >= n - 1) else 0.03):
        w, ln = rnd.choice([(14, 34), (18, 44), (22, 56), (26, 70)])
        px = ox + rnd.uniform(4, L - 4)
        cube(name, (px - w / 2, y - 1 - ln, z), (w, ln, 1), f"frond|0|1|v|{rnd.randint(0, 2)}",
             rotation=(rnd.uniform(-12, 12), rnd.uniform(-70, 70), rnd.uniform(-10, 10)), pivot=(px, y - 1, z + 0.5), label="under_frond")

# 上の膜：外上へ伸び、根元で持ち上がって先へいくほどゆるく垂れる。左は前のふちの先が巻き上がり、右は後ろの先が垂れる（左右で表情を変える）
def up_shape(k, q, i, n, sd):
    if i == 0:
        return 1.5 * math.sin(k * 0.9), 2 * math.sin(k * 0.8 + 1)
    rz = -6 + 1.15 * i + 5 * math.sin(i * 0.9 + k * 0.7)
    if sd > 0 and q > 0.55 and i >= n - 4:
        rz -= 8 * (i - (n - 4) + 1)
    if sd < 0 and i >= n - 3:
        rz += 7 * (i - (n - 3) + 1) * (1.2 - q)
    if q < 0.3 and i >= n - 3:
        rz += 9
    return 4 * math.sin(i * 0.7 + k), rz

# 下の膜（腕）：外下へ伸び、先が下へ巻き込んで鎌のような爪で終わる
def low_shape(k, q, i, n, sd):
    if i == 0:
        return 1.5 * math.sin(k * 1.1 + 2), 2 * math.sin(k * 0.9)
    rz = 1.5 + 0.9 * i + 5 * math.sin(i * 0.8 + k * 0.9 + 1.5)
    if i >= n - 4:
        rz += (6 if sd > 0 else 4) * (i - (n - 4) + 1) * (0.5 + 0.5 * q)
    return 4 * math.sin(i * 0.6 + k * 1.3), rz

# 尾の膜：後ろへ長く流れ、リボンのように波打ちながら垂れる
def tail_shape(k, q, i, n, sd):
    if i == 0:
        return 0, 5 * math.sin(k * 1.3)
    return 5 * math.sin(i * 0.8 + k), 1.2 + 0.7 * i + 7 * math.sin(i * 0.85 + k * 1.1)

build_wing("wing_up", (1, -1), (26, 150, 16), (-60, -6, -16), -30, 16, 10, 14, 450, 0.62, 38, up_shape, ribs={9: 7, 6: 4, 3: 3, 0: 2}, seed=1, phase=0)
build_wing("wing_low", (1, -1), (24, 60, 6), (34, 14, 8), -16, 20, 8, 14, 410, 0.60, 38, low_shape, ribs={7: 7, 4: 4, 1: 3}, seed=2, phase=140)
build_wing("tail", (0,), (0, 120, 44), (-10, 0, 0), -118, -62, 8, 20, 470, 0.55, 38, tail_shape, mode="tail", ribs={4: 4, 3: 4}, seed=3, phase=250)

# 下の膜の先：鎌のような爪（前の 3 本の短冊の先に 1 枚ずつ。膜の面に沿った三日月の板）
for t, sd in (("l", 1), ("r", -1)):
    for j, k in enumerate((7, 6, 5)):
        tipn = WINGS["wing_low"][t][k][-1]
        b = bone_index[tipn]
        tx = tip_x(tipn) - 10
        Lt, Wt = (96, 40) if j == 0 else ((76, 32) if j == 1 else (58, 26))
        nm = bone(f"talon_{t}{j}", tipn, (tx * sd, b["pivot"][1], b["pivot"][2]), (0, (18 - j * 16) * sd, 14 * sd))
        cube(nm, (tx if sd > 0 else -tx - Lt, b["pivot"][1] - 0.5, b["pivot"][2] - Wt / 2), (Lt, 1, Wt), f"talon|{j}|1|u|0", mirror=sd < 0, label="talon")
        anim(nm, rot=[0, wave(j * 70, 3 * sd, 45), wave(j * 50 + 30, 4 * sd, 45)])

# 膜の先から出て渦を巻く帯（先に玉）
def curl_ribbon(prefix, parent_name, sd, curl, n=12, L=20):
    pb = bone_index[parent_name]
    sx = tip_x(parent_name) - 4
    segs = []
    for i in range(n):
        qq = i / (n - 1)
        segs.append(dict(L=L, W=max(2, 7 - i * 0.45), ry=(rnd.uniform(-25, 25) if i == 0 else 0),
                         rz=(8 if i == 0 else curl * (5 + 36 * qq ** 2)), rx=rnd.uniform(-6, 6), mat="ribbon"))
    names = chain(prefix, parent_name, (sx, pb["pivot"][1], pb["pivot"][2]), segs, side=sd, shape="vert",
                  anim_fn=lambda i, s: [wave(i * 30 + sx, 3, 45), 0, wave(i * 32 + sx * 2, (2 + i * 0.5) * s, 45)])
    tx = tip_x(names[-1]) + 1
    tb = bone_index[names[-1]]
    cube(names[-1], (tx if sd > 0 else -tx - 7, tb["pivot"][1] - 3.5, tb["pivot"][2] - 3.5), (7, 7, 7), "pearl", label="curl_orb")
for t, sd in (("l", 1), ("r", -1)):
    for j, (k, curl) in enumerate(((8, -1), (2, 1))):
        curl_ribbon(f"curl_up_{t}{j}", WINGS["wing_up"][t][k][-1], sd, curl)
    for j, (k, curl) in enumerate(((0, -1),)):
        curl_ribbon(f"curl_low_{t}{j}", WINGS["wing_low"][t][k][-1], sd, curl, n=10)
for j, k in enumerate((0, 3, 4, 7)):
    curl_ribbon(f"curl_tail_{j}", WINGS["tail"]["c"][k][-1], 1, 1 if j % 2 == 0 else -1)

# ---------------------------------------------------------------------------
# 頭：前へ突き出すくさび形（上から見ると三角形。後ろが広く、鼻先へ細る）。体と同じ紫〜藤色の肌。
#   厚さ 8 ピクセルの輪切りを前へ並べて作る。目は 4 つ（左右 2 つずつ、段差の角から前と横を向く）
#   下あご（ボーン jaw）は後ろを軸に下へ開く。口の中は暗い紫に光る
# ---------------------------------------------------------------------------
H = bone("head", "body", (0, 125, -18), (30, 0, 0))          # 少しうつむいて地上を見下ろす
anim("head", rot=[wave(200, 1.6), wave(120, 2.0, 45), wave(40, 0.8, 45)])
NS = 11
def sl(k):
    """k 番目の輪切り（0 = 後ろ）：幅、上の y、口の線の y、後ろ側の z。"""
    return 60 - 5 * k, 143 - 1.5 * k, 121 + 0.3 * k, -20 - 8 * k
J = bone("jaw", H, (0, 121, -24), (4, 0, 0))
anim(J, rot=[wave(0, 2.5, 45), 0, 0])                         # ゆっくり口が開閉する（呼吸）
for k in range(NS):
    w, top, ym, z0 = sl(k)
    cube(H, (-w / 2, ym, z0 - 8), (w, top - ym, 8), f"skin|{k}|0", label="head_upper")
    cap = 3 if k <= 8 else 0                                   # 頭の上の盛り上がり（断面を台形に近づける）
    if cap:
        cw = round(w * 0.6)
        cube(H, (-cw / 2, top, z0 - 8), (cw, cap, 8), f"skin|{k}|0", label="head_cap")
    cube(H, (-1.5, top + cap, z0 - 6), (3, round(3 + (10 - k) * 0.9), 4), "horn|2|1", rotation=(-24, 0, 0), pivot=(0, top + cap, z0 - 4), label="head_spine")
    if k >= 1:
        wj, hj = w - 6, max(3, round(10 - 0.7 * k))
        cube(J, (-wj / 2, ym - 1 - hj, z0 - 8), (wj, hj, 8), f"skin|{k}|1", label="head_jaw")
        if w - 8 >= 2:
            cube(H, (-(w - 8) / 2, ym - 2, z0 - 8), (w - 8, 3, 8), "gullet", label="mouth_inside")
    if k >= 2:   # 牙：上あごから下がる牙（外から見える）と、下あごから上がる牙（口を開くと見える）
        fl = 5 if k < 8 else 7
        for sd in (1, -1):
            x = w / 2 - 2.5
            cube(H, (x if sd > 0 else -x - 2, ym - fl, z0 - 5), (2, fl, 2), "fang", label="fang_upper")
            xj = (w - 6) / 2 - 2
            cube(J, (xj if sd > 0 else -xj - 2, ym - 1, z0 - 8), (2, 4, 2), "fang", label="fang_lower")
w, top, ym, z0 = sl(NS - 1)
for sd in (1, -1):                                             # 鼻先の長い牙
    cube(H, (1.5 if sd > 0 else -3.5, ym - 9, z0 - 8), (2, 9, 2), "fang", label="fang_front")
cube(H, (-1.5, top, z0 - 7), (3, 7, 3), "horn|5|1", rotation=(28, 0, 0), pivot=(0, top, z0 - 5), label="nose_horn")
# 目 4 つと、その上のつり上がった眉
for k, (sx, sy, sz) in ((3, (5, 7, 12)), (6, (4, 5, 9))):
    w, top, ym, z0 = sl(k)
    for sd in (1, -1):
        x = w / 2 - 2
        cube(H, (x if sd > 0 else -x - sx, top - sy - 3, z0 - sz + 1), (sx, sy, sz), "eye", label="eye")
        cube(H, (x - 3 if sd > 0 else -x - sx - 1, top - 3, z0 - sz - 1), (sx + 4, 2, sz + 3), "ridge",
             rotation=(-10, 0, 14 * sd), pivot=(x * sd, top - 2, z0 - sz / 2), label="brow")
w, top, ym, z0 = sl(1)
cube(H, (-3, top + 3, z0 - 7), (6, 2, 6), "gem", label="forehead_gem")
# 首（頭と胴をつなぐ）
cube("body", (-20, 106, -22), (40, 36, 24), "skin|0|0", label="neck")
# 角：後ろへ長く流れる 1 対と、ほおの後ろの短い 1 対
for sd, t in ((1, "l"), (-1, "r")):
    segs = [dict(L=22, W=w_, H=w_, mat="horn") for w_ in (9, 8, 7, 6, 5, 3, 2)]
    for i, s_ in enumerate(segs):
        s_["rz"] = -30 if i == 0 else -3 + i * 1.5; s_["ry"] = -62 if i == 0 else -3
    names = chain(f"horn_{t}", H, (20, 138, -26), segs, side=sd, shape="vert")
    segs = [dict(L=18, W=w_, H=w_, mat="horn") for w_ in (6, 5, 4, 2)]
    for i, s_ in enumerate(segs):
        s_["rz"] = 6 if i == 0 else 5; s_["ry"] = -48 if i == 0 else -6
    chain(f"horn2_{t}", H, (27, 127, -28), segs, side=sd, shape="vert")
# 頭の後ろから立ち上がる羽飾り（葉の扇）
for sd, t in ((1, "l"), (-1, "r")):
    for k, lean in enumerate((10, 30, 50, 72)):
        segs = []
        for i in range(5):
            segs.append(dict(L=30, W=26 - i * 2, ry=(-24 - k * 6) if i == 0 else rnd.uniform(-4, 4),
                             rz=(-(90 - lean)) if i == 0 else 7 + i * 2.5 + rnd.uniform(-3, 3), rx=rnd.uniform(-6, 6), mat="frond"))
        chain(f"crest_{t}{k}", "body", (6 + k * 5, 144 - k * 5, -6 + k * 4), segs, side=sd, shape="vert",
              anim_fn=lambda i, s_, k=k: [wave(i * 35 + k * 40, 1.5 + i * 0.4, 45), 0, wave(i * 38 + k * 30 + 20, (1.5 + i * 0.6) * s_)])
# 首まわりの真珠の粒、下あごから垂れるひげの糸
for sd in (1, -1):
    for k in range(16):
        sz = rnd.choice([2, 3, 3, 4, 5])
        px = rnd.uniform(18, 34); py = rnd.uniform(104, 146); pz = rnd.uniform(-24, 8)
        cube("body", (px if sd > 0 else -px - sz, py, pz), (sz, sz, sz), "pearl", label="pearl")
for k in range(12):
    ln = int(rnd.uniform(20, 70)); zz = rnd.uniform(-96, -34)
    cube(J, (rnd.uniform(-4, 4), 112 - ln, zz), (1, ln, 1), "thread", label="whisker")

# ---------------------------------------------------------------------------
# 胴：顔の後ろから下へ垂れる、穴だらけの格子の柱
# ---------------------------------------------------------------------------
BZ = 16
for i, (w, h, d, y) in enumerate([(64, 58, 60, 104), (52, 60, 50, 46), (40, 64, 40, -16), (28, 60, 28, -74), (14, 56, 14, -128)]):
    cube("body", (-w / 2, y, BZ - d / 2), (w, h, d), "lacebox", label=f"core_{i}")
# 襟：首のまわりに開く大きな葉（2 段。顔の正面は空ける）
for tier, (cy, rad, nleaf, ln) in enumerate(((104, 40, 14, 36), (64, 34, 12, 30))):
    for k in range(nleaf):
        th = (k + 0.5) / nleaf * math.tau
        if math.cos(th) < -0.55:
            continue
        cx, cz = math.sin(th) * rad, BZ + math.cos(th) * rad * 0.9
        segs = []
        for i in range(4):
            segs.append(dict(W=30 - i * 3, L=ln, rx=(-52 + tier * 14) if i == 0 else 10 + rnd.uniform(-4, 6), rz=rnd.uniform(-7, 7), mat="frond"))
        hang(f"collar{tier}_{k}", "body", (cx, cy, cz), segs, yaw=-math.degrees(th),
             anim_fn=lambda i, k=k: [wave(i * 36 + k * 29, 1.5 + i * 0.6, 45), 0, wave(i * 33 + k * 23 + 90, 1.5 + i * 0.5, 22.5, "cos")])
# カーテン：格子の柱のまわりに垂れる（レースと筋の束）
for k in range(26):
    th = k / 26 * math.tau
    cx, cz = math.sin(th) * (27 + rnd.uniform(-4, 4)), BZ + math.cos(th) * 24
    n = rnd.choice([4, 5, 5, 6, 7])
    segs = []
    for i in range(n):
        segs.append(dict(W=max(8, 24 - i * 1.7), L=30, rx=(rnd.uniform(-10, 4) if i else -10), rz=(8 if i % 2 else -8) + rnd.uniform(-5, 5),
                         mat=("lace" if (k + i) % 3 else "drape")))
    hang(f"drape_{k}", "body", (cx, 44 - rnd.uniform(0, 70), cz), segs, yaw=-math.degrees(th),
         anim_fn=lambda i, k=k: [wave(i * 36 + k * 29, 1.5 + i * 0.6, 45), 0, wave(i * 33 + k * 23 + 90, 1.5 + i * 0.5, 22.5, "cos")])
# 下で外へ巻く葉
for k in range(10):
    th = (k + 0.5) / 10 * math.tau
    segs = []
    for i in range(7):
        rx = -28 if i == 0 else (-8 - i * 10 if i >= 2 else rnd.uniform(-8, 0))
        segs.append(dict(W=max(8, 30 - i * 3), L=26, rx=rx, rz=rnd.uniform(-6, 6), mat="lace"))
    hang(f"leaf_{k}", "body", (math.sin(th) * 15, -60, BZ + math.cos(th) * 15), segs, yaw=-math.degrees(th),
         anim_fn=lambda i, k=k: [wave(i * 40 + k * 37, 2 + i * 0.7, 45), 0, wave(i * 30 + k * 41, 1.5 + i * 0.5, 22.5, "cos")])
# 触腕：細く長く垂れる（先に玉が付くものもある）
for k in range(34):
    th = rnd.uniform(0, math.tau); rr = rnd.uniform(4, 24)
    y0 = rnd.uniform(-100, 20)
    n = rnd.randint(4, 7)
    w0 = rnd.choice([4, 5, 6, 8])
    segs = [dict(W=max(2, w0 - i * 0.4), L=26, rx=rnd.uniform(-7, 7) - (6 if i == 0 else 0), rz=6 * math.sin(i * 0.9 + k) + rnd.uniform(-3, 3), mat="ribbon") for i in range(n)]
    names = hang(f"tendril_{k}", "body", (math.sin(th) * rr, y0, BZ + math.cos(th) * rr), segs, yaw=rnd.uniform(0, 360),
                 anim_fn=lambda i, k=k: [wave(i * 36 + k * 31, 1.5 + i * 0.5, 45), 0, wave(i * 30 + k * 17 + 90, 1.5 + i * 0.6, 22.5, "cos")])
    if k % 3 == 0:
        b = bone_index[names[-1]]
        cube(names[-1], (b["pivot"][0] - 3, b["pivot"][1] - 26 - 5, b["pivot"][2] - 3), (6, 6, 6), "pearl", label="tendril_orb")
# 後ろへたなびく帯
for k in range(12):
    sd = 1 if k % 2 == 0 else -1
    j = k // 2
    n = 13 + rnd.randint(-2, 3)
    segs = []
    for i in range(n):
        segs.append(dict(L=30, W=max(5, 15 - i * 0.6), ry=(-90 + j * 5) if i == 0 else rnd.uniform(-6, 6),
                         rz=(10 if i == 0 else 3 + 7 * math.sin(i * 0.9 + k)), rx=rnd.uniform(-8, 8), mat="ribbon"))
    chain(f"trail_{k}", "body", (4 + j * 7, 96 - j * 30, BZ + 26), segs, side=sd, shape="vert",
          anim_fn=lambda i, s_, k=k: [wave(i * 36 + k * 30, 2 + i * 0.3, 45), wave(i * 30 + k * 25, 2 * s_), wave(i * 34 + k * 27 + 60, (2 + i * 0.5) * s_, 45)])

# ---------------------------------------------------------------------------
# まわりに漂う欠片（穴の開いた葉のかけら）と星の粒
# ---------------------------------------------------------------------------
for k in range(64):
    ang = rnd.uniform(0, math.tau); rr = rnd.uniform(70, 500)
    px, pz = math.cos(ang) * rr, math.sin(ang) * rr * 0.65 + 40
    py = rnd.uniform(-200, 300)
    name = bone(f"shard_{k}", "body", (px, py, pz), (rnd.uniform(-60, 60), rnd.uniform(0, 360), rnd.uniform(-60, 60)))
    w, h = rnd.choice([(8, 16), (10, 22), (14, 30), (18, 40), (24, 52)])
    cube(name, (px - w / 2, py - h / 2, pz), (w, h, 1), f"shard|0|1|v|{k % 4}")
    anim(name, rot=[wave(k * 47, 10), wave(k * 31, 16), wave(k * 53, 8, 45)], pos=[0, wave(k * 41, 8, 45), 0])
for k in range(180):
    ang = rnd.uniform(0, math.tau); rr = rnd.uniform(30, 500)
    sz = rnd.choice([1, 2, 2, 3])
    cube("body", (math.cos(ang) * rr, rnd.uniform(-220, 320), math.sin(ang) * rr * 0.7 + 40), (sz, sz, sz), "pearl", label="mote")

# ===========================================================================
# Box UV の割り付け（同じ大きさ・同じ材質は UV を共有。膜と顔は 1 枚ずつ別の絵。左右は mirror で反転）
# ===========================================================================
def foot(size):
    w, h, d = size
    return 2 * (w + d), d + h
slots = {}
for b in bones:
    for c in b["cubes"]:
        key = (tuple(c["size"]), c["_mat"], c["_akey"])
        s = slots.setdefault(key, {"size": c["size"], "mat": c["_mat"], "labels": [], "thin": c.get("_thin"), "art": c["_art"]})
        if c["_label"] not in s["labels"]:
            s["labels"].append(c["_label"])
        c["_key"] = key
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
# テクスチャ
# ===========================================================================
tex = np.zeros((TEX_H, TEX_W, 4), np.float32)
glow = np.zeros((TEX_H, TEX_W, 4), np.float32)
def C(s, a=255):
    s = s.lstrip("#"); return np.array([int(s[i:i + 2], 16) for i in (0, 2, 4)] + [a], np.float32)
DEEP = C("#2c2390", 232); VIOL = C("#5346cc", 212); PERI = C("#7d84ea", 190); PALE = C("#b3bdf9", 168); MIST = C("#dcdeff", 190)
PEARL = C("#f4f1ff", 240); WHITE = C("#ffffff", 250); IRI = C("#aef0ff", 190); ROSE = C("#e6c2ff", 190)
G_RIM = C("#d8efff", 48); G_VEIN = C("#a8e2ff", 90); G_STAR = C("#f2f9ff", 235); G_CYAN = C("#9ff4ff", 255)
BONE_HI = C("#f3f1fb"); BONE = C("#b9b8dc"); BONE_SH = C("#6b6aa6"); BONE_DK = C("#34315f")
VOID = C("#0b0720"); VOID2 = C("#1c1146"); GULLET = C("#8a36ff"); G_EYE = C("#c9fbff", 255); G_VIOLET = C("#b47cff", 230)

def mix(a, b, t):
    t = np.clip(np.asarray(t, np.float32), 0, 1)[..., None]
    return a * (1 - t) + b * t

def hash2(a, b, seed=0.0):
    h = np.sin(np.asarray(a, np.float64) * 127.1 + np.asarray(b, np.float64) * 311.7 + seed * 74.7) * 43758.5453
    return h - np.floor(h)

def vnoise(x, y, seed=0.0):
    """場所で決まる滑らかなノイズ（板をまたいでもつながる）。"""
    x0, y0 = np.floor(x), np.floor(y)
    fx, fy = x - x0, y - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a, b = hash2(x0, y0, seed), hash2(x0 + 1, y0, seed)
    c, d = hash2(x0, y0 + 1, seed), hash2(x0 + 1, y0 + 1, seed)
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy

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

# ---- 膜（扇全体でつながる絵） ----
def paint_wing(Lp, Wp, art):
    sp = art["spec"]; seed = sp["seed"]
    thb, thf = math.radians(sp["th_back"]), math.radians(sp["th_front"])
    span = thf - thb
    R = art["r"] + (np.arange(Lp) + 0.5)[None, :] * np.ones((Wp, 1))
    t = ((np.arange(Wp) + 0.5)[:, None] - Wp / 2) * np.ones((1, Lp))     # 前（-z）向きが正
    TH = math.radians(art["th"]) + t / R
    q = (TH - thb) / span
    Rm = wing_rmax(sp, q)
    rho = R / Rm
    arc = R * span                                                       # その半径での扇の弧の長さ
    q_lo = 0.012 + 0.016 * np.sin(R / 7.0 + seed) + 0.01 * np.sin(R / 2.9)   # 後ろのふち（細かい波形）
    inside = (np.abs(t) <= R * math.radians(art["dth"]) / 2 + 0.5) & (rho < 1) & (q > q_lo) & (q < 1.0)
    if not art["last"]:
        inside &= (np.arange(Lp) < Lp - 1)[None, :]
    # 穴の多さ：外周と後ろのふちへ向かって増える（根元と前のふちは詰まっている）
    def holeyness(rho_, q_):
        return np.clip(np.maximum((rho_ - 0.42) / 0.5, (0.42 - q_) / 0.42 * np.clip(rho_ * 2.5, 0, 1)), 0, 1)
    g = holeyness(rho, q)
    cs = 22.0
    j0 = np.floor(R / cs)
    dmin = np.full(R.shape, 9.0)
    for dj in (-1, 0, 1):
        j = j0 + dj
        nj = np.maximum(2, np.round((j + 0.5) * span))          # その輪の穴の数
        m0 = np.floor(q * nj)
        for dm in (-1, 0, 1):
            m = m0 + dm
            u1, u2, u3, u4, u5 = (hash2(j, m, seed + s_) for s_ in (0.0, 1.3, 2.6, 3.9, 5.2))
            Rc = (j + 0.5 + 0.3 * (u1 - 0.5)) * cs
            qc = (m + 0.5 + 0.3 * (u2 - 0.5)) / nj
            gc = holeyness(Rc / wing_rmax(sp, qc), qc) ** 0.8
            a = np.maximum(cs * (0.10 + 0.36 * u3) * gc, 0.01)
            b_ = np.maximum(cs * (0.10 + 0.36 * u4) * gc, 0.01)
            d = np.sqrt(((R - Rc) / a) ** 2 + ((q - qc) * arc / b_) ** 2)
            d = np.where((gc < 0.1) | (u5 < 0.22) | (np.minimum(a, b_) < 1.6), 9.0, d)
            d_rim = np.where(d < 9, d - 1.8 / np.minimum(a, b_), 9.0)   # ふちの太さ 約 1.8 ピクセル
            dmin = np.minimum(dmin, np.where(d < 1, 0.0, np.where(d_rim < 1, 1.0, 9.0)))
    hole = dmin == 0.0
    hole_rim = dmin == 1.0
    # 色：根元は深い藍、外へ向かって紫 → 青紫 → 淡い藤色
    n1 = vnoise(R / 34.0, q * 9.0, seed) - 0.5
    tt = np.clip(0.26 + 0.72 * np.maximum(rho ** 1.2, g * 0.9) + n1 * 0.4, 0, 1)
    col = np.where((tt < 0.33)[..., None], mix(DEEP, VIOL, tt * 3),
                   np.where((tt < 0.66)[..., None], mix(VIOL, PERI, (tt - 0.33) * 3), mix(PERI, PALE, (tt - 0.66) * 3)))
    col = mix(col, IRI, (0.5 + 0.5 * np.sin(R / 31.0 + q * 7 + seed * 2)) ** 3 * 0.30)       # 虹色のつや
    col = mix(col, ROSE, (0.5 + 0.5 * np.sin(R / 47.0 - q * 9 + seed)) ** 4 * 0.24)
    col = mix(col, DEEP, np.clip(0.55 - rho * 3.2, 0, 1) * 0.8)
    gl = np.zeros_like(col)
    # 傘の骨（太い筋）と、そのあいだの細い筋・同心の弧（くもの巣のような網目）
    NR = 5
    qr = (np.floor(q * NR) + 0.5) / NR
    d_rib = np.abs(q - qr) * arc
    rib = d_rib < (1.3 - 0.6 * rho)
    col[rib] = mix(col, MIST, 0.75)[rib]; gl[rib] = G_VEIN
    qf = (np.floor(q * NR * 4) + 0.5) / (NR * 4)
    fine = (np.abs(q - qf) * arc < 0.5) & (rho > 0.12)
    col[fine] = mix(col, MIST, 0.30)[fine]
    ring = (np.mod(R + 6 * np.sin(q * NR * math.tau), 26.0) < 1.0) & (rho > 0.1)
    col[ring] = mix(col, MIST, 0.28)[ring]
    node = ring & (np.abs(q - qf) * arc < 1.2)
    col[node] = PEARL; gl[node] = G_STAR * np.array([1, 1, 1, 0.55], np.float32)
    col[..., 3] = 236 - 60 * g
    # 真珠色のふち（外周・後ろのふち・穴のまわり）
    edge = ((Rm - R) < 2.6) | ((q - q_lo) * arc < 2.6)
    col[hole_rim | edge] = PEARL; gl[hole_rim | edge] = G_RIM
    star = (hash2(np.floor(R), np.floor(t) + art["th"] * 31, seed) < 0.004)
    col[star] = WHITE; gl[star] = G_STAR
    mask = inside & ~hole
    col[~mask] = 0; gl[~mask] = 0
    return col.astype(np.float32), gl.astype(np.float32)

def paint_lace(Lp, Wp, i, tip, rs, full=False):
    """骨のような格子：大きな楕円の穴が並び、太い真珠色のふちだけが残る。"""
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
    col = mix(PERI, VIOL, 0.1 + 0.7 * smooth(Wp, Lp, 8, rs)); col[..., 3] = 232
    gl = np.zeros_like(col)
    inner = mask & ~erode(mask, 4)
    col[inner] = mix(col, PALE, 0.5)[inner]
    return finish(col, gl, mask, rs, rim_w=1, sparkle=0.004)

def paint_drape(Lp, Wp, i, tip, rs):
    """縦に垂れる筋の束：細い筋がうねりながら並び、横糸と珠でつながる。"""
    X, Y, PX, PY = grid(Lp, Wp)
    mask = np.zeros(X.shape, bool); bead = np.zeros(X.shape, bool)
    ns = max(2, Wp // 7)
    links = [rs.uniform(3, Lp - 3) for _ in range(max(1, Lp // 14))]
    for k in range(ns):
        yc = (((k + 0.5) / ns * 2 - 1) * 0.86 + 0.07 * np.sin((i + X) * math.tau * 1.5 + k * 1.7)) * Wp / 2
        end = rs.uniform(0.55, 1.0) if tip else 2.0
        mask |= (np.abs(PY - yc) < 1.3) & (X < end)
        for lx in links:
            if rs.random() < 0.6:
                bead |= ((PX - lx) ** 2 + (PY - yc[0, int(min(Lp - 1, lx))]) ** 2) < 5.5
    for lx in links:
        mask |= (np.abs(PX - lx) < 0.8) & (np.abs(Y) < 0.86)
    mask |= bead
    col = mix(DEEP, PERI, 0.2 + 0.7 * smooth(Wp, Lp, 6, rs)); col[..., 3] = 232
    gl = np.zeros_like(col)
    col[bead] = PEARL; gl[bead] = G_RIM * np.array([1, 1, 1, 2.0], np.float32)
    return finish(col, gl, mask, rs, rim_w=1, rim_col=mix(PERI, PEARL, np.float32(0.6)), sparkle=0.004)

def paint_ribbon(Lp, Wp, i, tip, rs):
    """たなびく帯：波打つふち、中央の光る芯、ひし形の飾り。先は細く尖る。"""
    X, Y, PX, PY = grid(Lp, Wp)
    env = 0.92 - 0.2 * (0.5 + 0.5 * np.sin((i + X) * math.tau))
    if tip:
        env = env * np.clip(1 - X, 0, 1) ** 0.6
    mask = np.abs(Y) < env if Wp > 3 else np.ones(X.shape, bool) & ((X < 0.97) | (not tip))
    col = mix(PERI, PALE, smooth(Wp, Lp, 5, rs)); col[..., 3] = 205
    col = mix(col, IRI, (0.5 + 0.5 * np.sin((i + X) * 4)) ** 3 * 0.35)
    gl = np.zeros_like(col)
    core = np.abs(PY) < 0.8
    if Wp >= 6:
        col[core] = PEARL; gl[core] = G_VEIN
    if Wp >= 6:
        dia = (np.abs(((PX + 7) % 14) - 7) + np.abs(PY)) < 2.6
        col[dia] = WHITE; gl[dia] = G_STAR * np.array([1, 1, 1, 0.5], np.float32)
    return finish(col, gl, mask, rs, rim_w=1)

def paint_frond(Lp, Wp, i, tip, rs, n=None):
    """葉：尖った輪郭、中央の葉脈と側脈、楕円の穴。鎖のときは節をまたいで 1 枚の葉の形になる（n = 節の数）。"""
    X, Y, PX, PY = grid(Lp, Wp)
    if n:
        tt = (i + X) / n
        env = np.clip(np.sin(math.pi * np.clip(tt, 0, 1) ** 0.75) ** 0.6 * 1.15 + 0.25 * (1 - tt), 0, 1) * 0.95
        if tip:
            env = env * np.clip(1 - X ** 2, 0, 1) ** 0.6
    else:
        tt = X
        env = np.sin(math.pi * np.clip(X, 0, 1) ** 0.75) ** 0.75 * 0.95
    env = env - 0.06 * (0.5 + 0.5 * np.sin((i + X) * math.tau * 3.5))
    mask = np.abs(Y) < env
    mask = punch(mask, PX, PY, rs, 2 + (Lp * Wp) // 260, 1.8, 5.0, Lp, Wp, margin=0.4)
    col = mix(VIOL, MIST, tt * 0.75 + smooth(Wp, Lp, 6, rs) * 0.3); col[..., 3] = 218
    col = mix(col, IRI, (0.5 + 0.5 * np.sin(PX / 9.0 + PY / 5.0 + i)) ** 3 * 0.25)
    gl = np.zeros_like(col)
    mid = np.abs(PY) < 0.9
    side = (((PX - np.abs(PY) * 1.1) % 7) < 0.9) & ~mid
    col[side] = mix(col, PEARL, 0.4)[side]
    col[mid] = PEARL; gl[mid] = G_VEIN
    return finish(col, gl, mask, rs, sparkle=0.008)

def paint_talon(Lp, Wp, rs):
    """鎌のような爪：付け根が太く、前へ反りながら尖る三日月。"""
    X, Y, PX, PY = grid(Lp, Wp)
    yc = -0.62 + 1.45 * X ** 1.7
    half = 0.36 * np.clip(1 - X, 0, 1) ** 0.75 + 0.015
    mask = (np.abs(Y - yc) < half)
    col = mix(PEARL, PALE, np.clip((Y - yc) / np.maximum(half, 0.02) * 0.5 + 0.5, 0, 1) * 0.8); col[..., 3] = 245
    col = mix(col, IRI, (0.5 + 0.5 * np.sin(X * 14)) ** 4 * 0.3)
    gl = np.zeros_like(col); gl[...] = G_RIM * np.array([1, 1, 1, 1.4], np.float32)
    edge = (Y - yc) < -half * 0.55            # 刃（内側のふち）が強く光る
    col[edge] = WHITE; gl[edge] = G_STAR
    return finish(col, gl, mask, rs, rim_w=1, rim_col=WHITE, rim_glow=G_STAR, sparkle=0.01)

def paint_bone(w, h, rs, i=0, dark=0.0):
    """骨・角：節のある象牙色。長い辺に沿って輪の筋が入る。"""
    yy = (np.arange(h)[:, None] / max(1, h - 1)) * np.ones((1, w), np.float32)
    xx = (np.arange(w)[None, :] / max(1, w - 1)) * np.ones((h, 1), np.float32)
    along, across = (xx * w, yy) if w >= h else (yy * h, xx)
    col = mix(BONE_HI, BONE, across * 0.7 + smooth(h, w, 4, rs) * 0.3)
    col = mix(col, BONE_SH, np.clip(dark + 0.08 * i, 0, 0.7))
    ringm = (np.mod(along + 2, 7) < 1)
    col[ringm] = mix(col, BONE_DK, 0.2)[ringm]
    col[..., 3] = 255
    gl = np.zeros_like(col)
    return col, gl

def paint_skull(w, h, rs):
    """頭の中身：暗い骨。ひびと小さな穴。"""
    col = mix(BONE, BONE_DK, 0.25 + 0.55 * smooth(h, w, 7, rs)); col[..., 3] = 255
    crack = smooth(h, w, 5, rs)
    col[np.abs(crack - 0.5) < 0.02] = BONE_DK
    col[0, :] = mix(col[0, :], BONE_HI, np.float32(0.5))
    return col, np.zeros_like(col)

def paint_orb(w, h, rs, a, b, gcol, garef):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    r = np.sqrt(((xx + 0.5) / w * 2 - 1) ** 2 + ((yy + 0.5) / h * 2 - 1) ** 2)
    hl = np.sqrt(((xx + 0.5) / w * 2 - 0.6) ** 2 + ((yy + 0.5) / h * 2 - 0.6) ** 2)
    col = mix(a, b, r * 0.9); col = mix(col, WHITE, np.clip(1 - hl * 2.2, 0, 1))
    gl = np.zeros_like(col); gl[...] = gcol; gl[..., 3] = garef * np.clip(1.1 - r, 0, 1)
    if min(w, h) >= 3:
        out = r > 1.08
        col[out] = 0; gl[out] = 0
    return col, gl

# ---- 頭の肌・目・口の中・牙 ----
def paint_skin(w, h, rs, k, pale, fname):
    """体と同じ色の肌：後ろは深い紫、鼻先へ向かって青紫。側面は下へいくほど淡く、裏は真珠色。細かいうろこ。"""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    t = k / 10.0
    top = mix(mix(DEEP, VIOL, 0.35 + t), PERI, np.clip(t * 1.1 - 0.15, 0, 1) + smooth(h, w, 5, rs) * 0.25)
    if pale or fname == "down":
        col = mix(PALE, MIST, smooth(h, w, 5, rs))
    elif fname == "up":
        col = top
    else:
        col = mix(top, PALE, (yy / max(1, h - 1)) ** 1.5 * 0.75)
    scale = (np.mod(xx + (np.floor(yy / 3) % 2) * 2, 4) < 1) | (np.mod(yy, 3) < 1)
    col = np.where(scale[..., None], mix(col, DEEP, 0.16), col)
    col = mix(col, IRI, smooth(h, w, 9, rs) ** 3 * 0.35)
    col[..., 3] = 255
    return col.astype(np.float32), np.zeros((h, w, 4), np.float32)

def paint_eye(w, h, rs):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    r = np.sqrt(((xx + 0.5) / w * 2 - 1) ** 2 + ((yy + 0.5) / h * 2 - 1) ** 2)
    col = mix(WHITE, C("#3fd0ff"), r * 0.85); col[..., 3] = 255
    gl = np.zeros_like(col); gl[...] = G_CYAN
    if w >= 3 and h >= 3:
        slit = np.abs(xx - (w - 1) / 2) < 0.6          # 縦に細い瞳
        col[slit] = VOID; gl[slit] = 0
    return col, gl

def blit(rect, col, gl):
    x, y, w, h = rect
    tex[y:y + h, x:x + w] = col; glow[y:y + h, x:x + w] = gl

for key, s in slots.items():
    rs = np.random.default_rng(zlib.crc32(repr(key).encode()))
    parts = s["mat"].split("|"); kind = parts[0]
    i = int(parts[1]) if len(parts) > 1 else 0
    tip = len(parts) > 2 and parts[2] == "1"
    ori = parts[3] if len(parts) > 3 else "u"
    w, h, d = s["size"]
    R = face_rects(*s["uv"], s["size"])
    thin = s["thin"]
    if kind == "wing":
        col, gl = paint_wing(w, d, s["art"])
        blit(R["up"], col, gl)
        blit(R["down"], mix(col, DEEP, np.float32(0.14)) * (col[..., 3:4] > 0), gl)
        continue
    if kind in PLATES and thin is not None:
        def P(Lp, Wp):
            if kind == "lace": return paint_lace(Lp, Wp, i, tip, rs)
            if kind == "drape": return paint_drape(Lp, Wp, i, tip, rs)
            if kind == "ribbon": return paint_ribbon(Lp, Wp, i, tip, rs)
            if kind == "talon": return paint_talon(Lp, Wp, rs)
            if kind == "frond" and ori != "v": return paint_frond(Lp, Wp, i, tip, rs, n=5)
            if kind == "frond" and len(parts) > 4 and thin == 2 and tip and i == 0: return paint_frond(Lp, Wp, 0, True, rs)
            if kind == "frond": return paint_frond(Lp, Wp, i, tip, rs, n=4)
            return paint_frond(Lp, Wp, 0, True, rs)
        if thin == 1:
            col, gl = P(w, d)
            blit(R["up"], col, gl)
            blit(R["down"], mix(col, DEEP, np.float32(0.12)) * (col[..., 3:4] > 0), gl)
        elif thin == 2:
            if ori == "u":
                col, gl = P(w, h)
            else:
                col, gl = P(h, w); col, gl = col.transpose(1, 0, 2), gl.transpose(1, 0, 2)
            blit(R["north"], col, gl); blit(R["south"], col[:, ::-1], gl[:, ::-1])
        continue
    for fname, rect in R.items():
        x, y, fw, fh = rect
        if fw <= 0 or fh <= 0:
            continue
        if kind == "lacebox":
            col, gl = paint_lace(fw, fh, 0, False, rs, full=True)
        elif kind == "skin":
            col, gl = paint_skin(fw, fh, rs, i, tip, fname)
        elif kind == "eye":
            col, gl = paint_eye(fw, fh, rs)
        elif kind == "gullet":
            col = mix(VOID2, GULLET, smooth(fh, fw, 3, rs) * 0.8); col[..., 3] = 255
            gl = np.zeros_like(col); gl[...] = G_VIOLET * np.array([1, 1, 1, 0.6], np.float32)
        elif kind == "ridge":
            col = mix(PEARL, PALE, smooth(fh, fw, 3, rs)); col[..., 3] = 255; gl = np.zeros_like(col)
        elif kind == "fang":
            col = mix(WHITE, BONE, (np.arange(fh)[:, None] / max(1, fh - 1)) * np.ones((1, fw), np.float32) * 0.5); col[..., 3] = 255
            gl = np.zeros_like(col); gl[...] = G_RIM
        elif kind in ("bone", "horn"):
            col, gl = paint_bone(fw, fh, rs, i=i, dark=(0.0 if kind == "bone" else 0.12))
            if kind == "horn":
                col = mix(col, PALE, np.float32(0.35)); col[..., 3] = 255
            if tip and kind == "horn":
                col = mix(col, WHITE, np.float32(0.35)); col[..., 3] = 255
        elif kind == "skull":
            col, gl = paint_skull(fw, fh, rs)
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

Image.fromarray(np.clip(tex, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, f"{NAME}.png"), optimize=True)
Image.fromarray(np.clip(glow, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, f"{NAME}_glowmask.png"), optimize=True)

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
    "description": {"identifier": f"geometry.{NAME}", "texture_width": TEX_W, "texture_height": TEX_H,
                    "visible_bounds_width": 64, "visible_bounds_height": 48, "visible_bounds_offset": [0, 0, 0]},
    "bones": [clean(b) for b in bones]}]}
json.dump(geo, open(os.path.join(OUT, f"{NAME}.geo.json"), "w"), separators=(",", ":"), ensure_ascii=False)
ab = {}
for b in bones:
    a = anims.get(b["name"])
    if not a:
        continue
    o = {k: [x if isinstance(x, str) else 0 for x in v] for k, v in a.items() if any(isinstance(x, str) for x in v)}
    if o:
        ab[b["name"]] = o
json.dump({"format_version": "1.8.0", "animations": {f"animation.{NAME}.idle": {"loop": True, "animation_length": ANIM_LEN, "bones": ab}}},
          open(os.path.join(OUT, f"{NAME}.animation.json"), "w"), separators=(",", ":"), ensure_ascii=False)
json.dump({"slots": [{"uv": v_["uv"], "size": v_["size"], "mat": v_["mat"], "labels": v_["labels"]} for v_ in slots.values()], "tex": [TEX_W, TEX_H]},
          open(os.path.join(OUT, "_slots.json"), "w"), ensure_ascii=False)
print("tex", TEX_W, TEX_H, "slots", len(slots), "bones", len(bones), "cubes", sum(len(b["cubes"]) for b in bones))
