"""
アストラ・ヴェイル：超大型ボスのモデル生成スクリプト（幅 約 384・高さ 約 256・奥行 約 256 ブロック）。
geo.json・テクスチャ・発光マスク・待機アニメ・リグ（触腕のたなびき用の係数）を出力する。

構造（設定画から読み取った正式な構造）：
  A. 頭部   … 横に大きく広がる傘（head）。前面中央に顔の器官（仮面・くちばし・冠のとげ・琥珀色の発光器官・中央の青い宝玉）
  B. 胴体   … 傘の中央から下へ伸びる首と胴（torso → body_0〜5）。下へ細る房（strand）と、内部で光る器官
  C. 大型触腕 … 左右（lat）・上（up）・後ろ（back）・前（front）・下（down）と、帯状の長い触腕（rib）
  D. 小型触腕 … 傘のふちの細い触腕（rim）、胴の細い触腕（btl）、口腕（oral）、触角（ant）
  E. 発光・装飾 … 琥珀色の器官、青白い光の筋、星雲の模様、まわりに漂う星屑（dust）

単位：設計はブロック。1 ブロック = U ピクセル（U = 2）で geo.json に書き出し、レンダラーで 8 倍する（ModConstants.ASTRA_VEIL_RENDER_SCALE = 16 / U）。
座標：このスクリプトの中は「描画空間」（GeckoLib が描くときの座標。前は -Z、上は +Y、回転は Rz·Ry·Rx）で考え、
      書き出すときに Bedrock / Blockbench 形式（x を反転、回転の x・y を反転）へ変換する。
原点（エンティティの位置）：胴の中心。傘は y = +52〜+98、胴は y = +52〜-72、房の先は y = -128 あたり。
"""
import json, math, os, random, sys, zlib
import numpy as np
from PIL import Image

OUT = sys.argv[1] if len(sys.argv) > 1 else "out"
NAME = "astra_veil"
U = 2                    # 1 ブロックあたりのピクセル数
ANIM_LEN = 48.0          # 待機アニメの長さ（秒）
STEP = 360.0 / ANIM_LEN  # 波の速さ（度/秒）はこの倍数にするとつなぎ目なくループする（7.5）
os.makedirs(OUT, exist_ok=True)
rnd = random.Random(20261114)

# ===========================================================================
# 行列（描画空間）
# ===========================================================================
def Rx(a):
    c, s = math.cos(a), math.sin(a); return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
def Ry(a):
    c, s = math.cos(a), math.sin(a); return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
def Rz(a):
    c, s = math.cos(a), math.sin(a); return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])
def rot3(r):
    rx, ry, rz = [math.radians(v) for v in r]
    return Rz(rz) @ Ry(ry) @ Rx(rx)
def M4(R, t):
    m = np.eye(4); m[:3, :3] = R; m[:3, 3] = t; return m
def about(p, R):
    p = np.asarray(p, float)
    return M4(np.eye(3), p) @ M4(R, (0, 0, 0)) @ M4(np.eye(3), -p)

# ===========================================================================
# ボーン・キューブ・アニメ・リグ
# ===========================================================================
bones, bidx = [], {}
anims = {}      # name -> {"rot": [x, y, z], "pos": [...], "scale": [...]}。各軸は (振幅, 速さ 度/秒, 位相 度) の和
rig = []        # 触腕のたなびき用の係数（Java の AstraVeilAnimationController が読む）
uid = [0]

def bone(name, parent, pivot, rot=(0, 0, 0)):
    """pivot はブロック、rot は度（描画空間）。"""
    p = np.asarray(pivot, float) * U
    b = {"name": name, "parent": parent, "pivot": p, "rot": tuple(rot), "cubes": []}
    Mp = bidx[parent]["M"] if parent else np.eye(4)
    Rp = bidx[parent]["R"] if parent else np.eye(3)
    R = rot3(rot)
    b["M"] = Mp @ about(p, R); b["Rp"] = Rp; b["R"] = Rp @ R
    bones.append(b); bidx[name] = b
    return name

def box(bname, lo, size, mat, art=None, share=None, rot=None, pivot=None, label=None):
    """lo・size はブロック。mat は絵の種類、art は絵のパラメータ。share が同じキューブは同じ UV（絵）を使う。"""
    lo = np.asarray(lo, float) * U
    sz = [max(1, int(round(s * U))) for s in size]
    uid[0] += 1
    c = {"lo": lo, "size": sz, "mat": mat, "art": art or {}, "key": share if share is not None else ("u", uid[0]),
         "label": label or bname, "bone": bname}
    if rot:
        c["rot"] = tuple(rot); c["pivot"] = np.asarray(pivot, float) * U
    bidx[bname]["cubes"].append(c)
    return c

def q(speed):
    """速さをループする値（STEP の倍数）に丸める。"""
    return max(STEP, round(speed / STEP) * STEP)

def add_anim(name, ch, axis, amp, speed, phase):
    if abs(amp) < 1e-4:
        return
    a = anims.setdefault(name, {})
    a.setdefault(ch, [[], [], []])[axis].append((float(amp), float(q(speed)), float(phase % 360)))

def add_rig(name, gain, lag, f_amp, f_freq, f_phase, uncurl=0.0):
    """
    たなびきの係数。g（引きずる向き。描画空間）に対して、ボーンの回転（ラジアン）へ A·g を足すと、節が g の向きへ曲がる。
      節の向き d を g へ寄せる回転は、軸 d×g まわり。これを親の座標へ直し、オイラー角（Z·Y·X の順）の増分へ変換する。
    """
    b = bidx[name]
    rx, ry, rz = [math.radians(v) for v in b["rot"]]
    d = b["R"] @ np.array([1.0, 0, 0])
    cross = np.array([[0, -d[2], d[1]], [d[2], 0, -d[0]], [-d[1], d[0], 0]])       # cross @ g = d × g
    J = np.column_stack([Rz(rz) @ Ry(ry) @ np.array([1.0, 0, 0]), Rz(rz) @ np.array([0, 1.0, 0]), np.array([0, 0, 1.0])])
    if abs(np.linalg.det(J)) < 0.2:      # ジンバルロックに近い節は係数を出さない
        return
    A = gain * np.linalg.inv(J) @ b["Rp"].T @ cross
    rig.append({"n": name, "a": [round(float(v), 4) for v in A.flatten()], "l": round(lag, 3),
                "f": [round(f_amp, 4), round(f_freq, 3), round(f_phase, 3)], "u": round(uncurl, 4)})

# ===========================================================================
# 触腕（鎖）：根元のボーン（向き）→ 節のボーンを +X 方向へ連ねる
# ===========================================================================
TENT = {}   # name -> 情報（プレビュー・寸法の確認用）

def tentacle(name, parent, base, yaw, elev, roll, segs, mat, style="thick", art=None, share=None,
             drag=1.0, lag=1.0, flutter=0.10, ffreq=0.16, wave=(0.5, 2.0), wspeed=22.5, wstep=26.0, group=None):
    """
    base: 付け根（ブロック）。yaw: 水平の向き（0 = +X、90 = 後ろ +Z、-90 = 前）。elev: 仰角。roll: ねじり。
    segs: [dict(L, W, T, bz, by, bx)]（L 長さ、W 幅、T 厚み、bz/by/bx はその節の曲げ（度））
    style: "thick" = 断面が八角形に近い太い触腕（箱 2 個）＋ふちのひれ / "band" = 箱 1 個
    drag: 移動のときに後ろへ流れる量（ラジアン。全節の合計） / lag: 追従の遅れ（0 = 速い〜2 = 遅い）
    flutter: 移動のときに加わる波の大きさ（ラジアン。先端）/ ffreq: その速さ（Hz）
    wave: 待機アニメの波の大きさ（度。根元, 先端で足す量）/ wspeed: 速さ（度/秒）/ wstep: 節ごとの位相のずれ（度）
    """
    base = np.asarray(base, float)
    bone(name, parent, base, (0, -yaw, 0))
    n = len(segs)
    S = sum(s["L"] for s in segs)
    x = 0.0
    par = name
    ph0 = rnd.uniform(0, 360); ph1 = rnd.uniform(0, 360); fph = rnd.uniform(0, math.tau)
    wts = [1.0 - 0.65 * i / n for i in range(n)]              # 根元ほど大きく曲がる（根元から後ろへ流れる）
    wsum = sum(wts)
    names = []
    for i, s in enumerate(segs):
        r = (s.get("bx", 0) + (roll if i == 0 else 0), s.get("by", 0), s.get("bz", 0) + (elev if i == 0 else 0))
        sn = bone(f"{name}_{i}", par, base + (x, 0, 0), r)
        L, W, T = s["L"], s["W"], s["T"]
        ov = min(1.5, L * 0.3) if i else 0.0
        a = dict(art or {}); a.update(base=base * U, S=S * U, i=i, n=n, W=W * U, s0=x * U, tip=(i == n - 1))
        sh = (share, i) if share is not None else None
        if style == "thick":
            box(sn, base + (x - ov, -T * 0.31, -W / 2), (L + ov, T * 0.62, W), mat, a, share=(sh, "a") if sh else None, label=group or name)
            box(sn, base + (x - ov, -T / 2, -W * 0.31), (L + ov, T, W * 0.62), mat, a, share=(sh, "b") if sh else None, label=group or name)
            F = max(1.5, W * 0.32)                   # ふちのひれ（切り抜きのある薄い膜）
            fa = dict(a, F=F * U, zin=(W / 2 - 0.5) * U)
            for sd in (1, -1):
                z0 = W / 2 - 0.5 if sd > 0 else -W / 2 + 0.5 - F
                box(sn, base + (x - ov, -0.25, z0), (L + ov, 0.5, F), "fringe", dict(fa, sd=sd),
                    share=("fr", share if share is not None else name, i, sd), label="fringe")
        else:
            box(sn, base + (x - ov, -T / 2, -W / 2), (L + ov, T, W), mat, a, share=sh, label=group or name)
        t = (i + 1) / n
        # 待機アニメ：根元から先端へ伝わる波（面内 rz と、横 ry）
        add_anim(sn, "rot", 2, wave[0] + wave[1] * t, wspeed, ph0 - i * wstep)
        add_anim(sn, "rot", 1, (wave[0] + wave[1] * t) * 0.6, wspeed * 0.75 + STEP, ph1 - i * wstep * 0.8)
        add_rig(sn, drag * wts[i] / wsum, min(2.0, lag + 0.7 * i / n), flutter * t ** 1.2, ffreq, fph - i * 0.55,
                uncurl=-math.radians(s.get("cz", 0.0)) * 0.85 if drag > 0 else 0.0)
        names.append(sn); par = sn
        x += L
    TENT[name] = dict(names=names, base=base, S=S, group=group or name)
    return names

def seg_list(length, n, w0, w1, t0, t1, bend=lambda t, i: (0, 0, 0), shrink=0.4, wpow=1.0):
    """長さ length を n 節に分ける。先へいくほど節を短く（shrink）、幅・厚みを細く。"""
    ws = [1 - shrink * (i / max(1, n - 1)) for i in range(n)]
    k = length / sum(ws)
    out = []
    for i in range(n):
        t = i / max(1, n - 1)
        bb = bend(t, i)
        bx, by, bz = bb[:3]
        tw = t ** wpow
        out.append(dict(L=ws[i] * k, W=w0 + (w1 - w0) * tw, T=t0 + (t1 - t0) * tw, bx=bx, by=by, bz=bz, cz=bb[3] if len(bb) > 3 else 0.0))
    return out

def curl(total, ts=0.5, p=1.5, n=20):
    """先端の渦巻き：t > ts の節へ、合計 total 度を先へいくほど強く配る。"""
    ws = [max(0.0, (i / max(1, n - 1) - ts) / (1 - ts)) ** p for i in range(n)]
    sm = sum(ws) or 1
    return [total * w / sm for w in ws]

def end_pos(name, i=-1, along=1.0):
    """鎖の i 番目の節の先端の位置（静止姿勢・モデル空間・ブロック）。"""
    sn = TENT[name]["names"][i]
    b = bidx[sn]; c = b["cubes"][0]
    p = np.array([c["lo"][0] + c["size"][0] * along, b["pivot"][1], b["pivot"][2], 1.0])
    return (b["M"] @ p)[:3] / U

def organ(name, parent, center, size, kind="amber", halo=1.7, pulse=(0.10, 0.06), label=None):
    """発光器官：玉（中身）と、そのまわりの淡い光の殻。ボーンごとに不規則に明滅（大きさが脈打つ）。"""
    c = np.asarray(center, float)
    bone(name, parent, c)
    box(name, c - size / 2, (size, size, size), kind, share=(kind, round(size * U)), label=label or kind)
    hn = bone(name + "_halo", name, c)
    hs = size * halo
    box(hn, c - hs / 2, (hs, hs, hs), "halo_" + kind, share=("halo", kind, round(hs * U)), label="halo")
    p1, p2 = rnd.uniform(0, 360), rnd.uniform(0, 360)
    s1, s2 = rnd.choice([30, 37.5, 45, 52.5]), rnd.choice([67.5, 82.5, 97.5, 112.5])
    for ax in range(3):       # 2 つの周期を重ねて不規則に見せる
        add_anim(name, "scale", ax, pulse[0] * 0.5, s1, p1); add_anim(name, "scale", ax, pulse[1] * 0.5, s2, p2)
        add_anim(hn, "scale", ax, pulse[0] * 2.2, s1, p1 + 20); add_anim(hn, "scale", ax, pulse[1] * 2.2, s2, p2 + 35)
    return name

# ===========================================================================
# 骨組み
# ===========================================================================
ZC = 8.0                                   # 傘・胴の中心の z（顔が前にあるぶん、少し後ろ）
bone("root", None, (0, 0, 0))
bone("core", "root", (0, 0, ZC))           # 全体のゆったりした上下・傾き
add_anim("core", "pos", 1, 2.6 * U, 30, 0); add_anim("core", "pos", 1, 0.9 * U, 52.5, 80)
add_anim("core", "rot", 0, 0.8, 22.5, 40); add_anim("core", "rot", 2, 0.7, 15, 200); add_anim("core", "rot", 1, 0.8, 7.5, 120)

# ---------------------------------------------------------------------------
# A. 頭部（傘）
# ---------------------------------------------------------------------------
HA, HB, HY, HH = 122.0, 80.0, 52.0, 54.0   # 傘の半径（x, z）・ふちの高さ・高さ
bone("head", "core", (0, HY, ZC))
add_anim("head", "pos", 1, 1.2 * U, 37.5, 150)                      # ゆっくり上下
add_anim("head", "rot", 0, 1.0, 22.5, 250); add_anim("head", "rot", 2, 0.8, 30, 20)

def dome_f(y):
    """傘の高さ y での広がり（1 = ふち、0 = 頂上）。"""
    u = np.clip((y - HY) / HH, 0, 1)
    return (1 - u ** 2.3) ** (1 / 2.0)

def ring(bname, y0, h, ax, az, mat, art, thick, n=16, cx=0.0, cz=ZC, label=None, cap=False):
    """楕円の輪切り 1 段：外周に沿って壁の箱を n 個並べる（n 角形）。thick は壁の厚み（内側へ）。cap = True なら中をふさぐ箱も置く。"""
    for k in range(n):
        a0, a1 = math.tau * k / n, math.tau * (k + 1) / n
        v0 = np.array([cx + ax * math.cos(a0), cz + az * math.sin(a0)]); v1 = np.array([cx + ax * math.cos(a1), cz + az * math.sin(a1)])
        mid = (v0 + v1) / 2; dvec = v1 - v0; ln = float(np.linalg.norm(dvec)) + 1.2
        yaw = math.degrees(math.atan2(-dvec[1], dvec[0]))
        nz = np.array([math.sin(math.radians(yaw)), math.cos(math.radians(yaw))])      # 箱の +z が向く先
        outward = float(nz @ (mid - np.array([cx, cz]))) > 0
        z0 = -thick if outward else 0.0
        box(bname, (mid[0] - ln / 2, y0, mid[1] + z0), (ln, h, thick), mat, art, rot=(0, yaw, 0), pivot=(mid[0], y0, mid[1]), label=label or bname)
    if cap:
        box(bname, (cx - ax * 0.72, y0, cz - az * 0.72), (ax * 1.44, h, az * 1.44), mat, art, label=label or bname)

DOME_STEP = 6.0
for j in range(10):
    y0 = HY + j * DOME_STEP
    f = float(dome_f(y0 + DOME_STEP * 0.35)); f2 = float(dome_f(y0 + DOME_STEP * 1.35))
    last = f2 < 0.2 or j == 9
    ring("head", y0, DOME_STEP, HA * f * (1 + 0.02 * math.sin(j * 2.1)), HB * f, "dome", dict(top=j), thick=min(HB * f * 0.6, (f - f2) * HA + 6),
         n=16 if f > 0.5 else 10, cx=1.5 * math.sin(j * 1.3), label="dome", cap=last)
    if last:
        break
# 傘の裏：首へ向かってすぼまる
UND = ((0.86, 4.0), (0.62, 4.0), (0.40, 5.0))
for j, (f, h) in enumerate(UND):
    f2 = UND[j + 1][0] if j + 1 < len(UND) else 0.2
    ring("head", HY - sum(h_ for _, h_ in UND[:j + 1]), h, HA * f, HB * f, "under", dict(), thick=(f - f2) * HA + 5, n=14, label="dome_under")

def rim_pt(phi, f=1.0, y=None):
    """傘のふち（広がり f）の点。phi は度（0 = +X、90 = 後ろ、-90 = 前）。"""
    a = math.radians(phi)
    return np.array([HA * f * math.cos(a), HY if y is None else y, ZC + HB * f * math.sin(a)])

def rim_dir(phi):
    """ふちの外向きの向き（楕円の法線。度）。"""
    a = math.radians(phi)
    return math.degrees(math.atan2(math.sin(a) / HB, math.cos(a) / HA))

# 傘のふちの葉（うろこのように重なり、外へ垂れる。大きさ・長さは不ぞろい）
NL = 34
for k in range(NL):
    phi = -90 + (k + 0.5) * 360 / NL + rnd.uniform(-3, 3)
    if abs((phi + 90 + 180) % 360 - 180) < 13:          # 顔の正面は空ける
        continue
    big = rnd.random() < 0.45
    ln = rnd.uniform(26, 40) if big else rnd.uniform(16, 26)
    w0 = rnd.uniform(24, 32) if big else rnd.uniform(16, 22)
    segs = seg_list(ln, 3, w0, w0 * 0.45, 5.0, 2.0, bend=lambda t, i: (rnd.uniform(-4, 4), rnd.uniform(-5, 5), -14 - rnd.uniform(0, 10)), shrink=0.2)
    tentacle(f"lobe_{k}", "head", rim_pt(phi, 0.93, HY + 3), rim_dir(phi) + rnd.uniform(-8, 8), -8 + rnd.uniform(-6, 6), rnd.uniform(-6, 6), segs,
             "lobe", style="band", drag=0.25, lag=0.2, flutter=0.03, ffreq=0.3, wave=(0.8, 1.6), wspeed=q(30 + (k % 3) * 7.5), wstep=40, group="lobe")
    # 波がふちを一周するように位相をそろえ直す（傘の外套がゆっくり波打つ）
    for i, sn in enumerate(TENT[f"lobe_{k}"]["names"]):
        anims[sn]["rot"][2] = [(1.2 + 1.2 * i, 30.0, (phi * 2 - i * 40) % 360), (0.6, 52.5, (phi * 3 + 90) % 360)]
# 傘の上に重なる葉（2 段目・3 段目）
for tier, (f, cnt, ln, w0) in enumerate(((0.70, 22, 26, 24), (0.42, 13, 22, 20))):
    y = HY + HH * (1 - f ** 2.0) ** (1 / 2.3) + 1.0
    for k in range(cnt):
        phi = -90 + (k + 0.5 * (tier % 2)) * 360 / cnt + rnd.uniform(-5, 5)
        if tier == 0 and abs((phi + 90 + 180) % 360 - 180) < 16:
            continue
        slope = -(18 + 26 * f)
        segs = seg_list(ln * rnd.uniform(0.8, 1.2), 2, w0 * rnd.uniform(0.8, 1.2), w0 * 0.5, 4.0, 2.0, bend=lambda t, i: (0, rnd.uniform(-5, 5), -10), shrink=0.2)
        tentacle(f"scale{tier}_{k}", "head", rim_pt(phi, f, y), rim_dir(phi) + rnd.uniform(-10, 10), slope, rnd.uniform(-8, 8), segs,
                 "lobe", style="band", art=dict(tier=tier + 1), drag=0.12, lag=0.2, flutter=0.02, ffreq=0.3, wave=(0.6, 1.0), wspeed=37.5, wstep=40, group="lobe")
# 傘の頂上の星形の飾り（上から見ると十字の星）と、放射状の稜線
for k in range(8):
    ang = k * 45.0
    ln, w = (30, 9) if k % 2 == 0 else (18, 6)
    segs = seg_list(ln, 3, w, 1.5, 3.0, 1.5, bend=lambda t, i: (0, 0, -9), shrink=0.1)
    tentacle(f"star_{k}", "head", (0, HY + HH - 2.5, ZC), ang, -4, 0, segs, "star", style="band", drag=0, lag=0, flutter=0, wave=(0, 0), group="crown_star")
organ("crown_gem", "head", (0, HY + HH + 1.5, ZC), 6.0, "gem", label="crown_gem")
for k in range(12):
    phi = k * 30.0 + 15
    for j in range(1, 10):
        y0 = HY + j * DOME_STEP
        f0, f1 = float(dome_f(y0 - DOME_STEP * 0.65)), float(dome_f(y0 + DOME_STEP * 0.35))
        if f1 < 0.15:
            break
        p = rim_pt(phi, (f0 + f1) / 2, y0)
        ln = max(3.0, (f0 - f1) * math.hypot(HA * math.cos(math.radians(phi)), HB * math.sin(math.radians(phi))) + 3)
        box("head", (p[0] - ln / 2, y0 - 0.5, p[2] - 1.5), (ln, 2.5, 3), "ridge", rot=(0, -rim_dir(phi), 0), pivot=p, share=("ridge", round(ln * U)), label="dome_ridge")

# ふちから垂れる膜（ヴェイル）：切り抜きのある薄い膜が波打つ
NV = 40
for k in range(NV):
    phi = -90 + (k + 0.5) * 360 / NV + rnd.uniform(-2, 2)
    if abs((phi + 90 + 180) % 360 - 180) < 9:
        continue
    drop = rnd.uniform(20, 46) * (1.25 if abs(math.sin(math.radians(phi))) < 0.5 else 1.0)
    w0 = rnd.uniform(15, 21)
    segs = seg_list(drop, 4, w0, w0 * 0.8, 0.5, 0.5, bend=lambda t, i: (0, 0, rnd.uniform(-9, 9)), shrink=0.0)
    tentacle(f"veil_{k}", "head", rim_pt(phi, 0.90, HY - 1), rim_dir(phi), -84 + rnd.uniform(-5, 5), 0, segs, "veil", style="band", art=dict(var=k % 5),
             share=("veil", k % 10), drag=0.9, lag=0.5, flutter=0.10, ffreq=0.32, wave=(1.5, 3.0), wspeed=37.5, wstep=50, group="veil")
    for i, sn in enumerate(TENT[f"veil_{k}"]["names"]):
        anims[sn]["rot"][2] = [(2.0 + 1.3 * i, 37.5, (phi * 3 - i * 50) % 360)]

# 傘の裏のえら（首のまわりに放射状に並ぶ薄い板）
for k in range(30):
    phi = k * 12.0 + 6
    p = rim_pt(phi, 0.44, HY - 6)
    ln = rnd.uniform(20, 30); hh = rnd.uniform(9, 15)
    box("head", (p[0] - ln / 2, p[1] - hh, p[2] - 0.25), (ln, hh, 0.5), "gill", dict(var=k % 4), rot=(0, -rim_dir(phi), 0), pivot=p,
        share=("gill", round(ln), round(hh), k % 4), label="gill")

# ---- 顔（傘の前面中央。少しうつむいて地上を見下ろす） ----
FZ = ZC - HB + 4.0                          # 顔の面の z（傘の前のふちの少し内側）
FS = 1.3                                    # 顔全体の大きさ
FY = 76.0                                   # 顔の中心の高さ
def fp(x, y, z=0.0):
    """顔の設計座標（中心 x = 0、y = 30〜94、z は面から前へマイナス）→ モデルの座標。"""
    return np.array([x * FS, FY + (y - 74) * FS, FZ + z * FS])
bone("face", "head", (0, FY, FZ), (20, 0, 0))
add_anim("face", "rot", 0, 0.8, 30, 100)
# 仮面：上が広く、頬で最も広がり、下はくちばしのように尖る（段々に組む）
def mask_w(y):
    if y >= 72: return 24 + (92 - y) * 0.5
    if y >= 54: return 34 - (72 - y) * 1.15
    return max(2.0, 13.3 - (54 - y) * 0.75)
for j in range(15):
    y1 = 94 - j * 4.0; y0 = y1 - 4.0
    w = mask_w((y0 + y1) / 2)
    dpt = 5.0 + 2.0 * math.sin(math.pi * j / 14)
    box("face", fp(-w / 2, y0, -dpt), (w * FS, 4 * FS, (dpt + 2) * FS), "mask", dict(row=j, w=w * FS * U), label="mask")
    if 2 <= j <= 13:                         # 鼻すじ（金のふち取り）
        box("face", fp(-1.5, y0, -dpt - 1.5), (3 * FS, 4 * FS, 2 * FS), "gold", share=("gold", 3, 4, 2), label="mask_ridge")
box("face", fp(-1, 30, -4), (2 * FS, 5 * FS, 4 * FS), "gold", share=("gold", 2, 5, 4), label="beak_tip")
# 眉の翼（仮面から左右へ伸び、傘に沿って後ろへ流れる）
for sd, t_ in ((1, "a"), (-1, "b")):
    for j, (y, ln, e) in enumerate(((88, 40, 14), (80, 32, 2), (70, 24, -12))):
        segs = seg_list(ln * FS * (1.0 if sd > 0 else 0.92), 5, 4 * FS, 3, (9 - j * 1.5) * FS, 2.5, bend=lambda t, i: (0, -10 * sd if i else 0, 3 if i < 3 else -4), shrink=0.3)
        tentacle(f"brow_{t_}{j}", "face", fp(sd * (mask_w(y) / 2 - 2), y, -2), 0 if sd > 0 else 180, e, 0, segs, "spike", style="band",
                 art=dict(tone=0.55), drag=0, lag=0, flutter=0, wave=(0.2, 0.5), wspeed=30, group="brow")
# 冠のとげ（仮面の上から扇形に立ち上がる。中央が最も高い）
CREST = ((-66, 20), (-48, 26), (-31, 32), (-15, 38), (0, 44), (15, 38), (31, 33), (48, 26), (66, 21))
for k, (al, hgt) in enumerate(CREST):
    segs = seg_list(hgt * FS, 4, 5.0 * FS, 3.0, (8.0 - abs(al) * 0.03) * FS, 1.5, bend=lambda t, i: (0, 0, (-math.copysign(5, al) if al and i else 0)), shrink=0.25)
    tentacle(f"crest_{k}", "face", fp(math.sin(math.radians(al)) * 13, 91 + math.cos(math.radians(al)) * 3, 1), 0, 90 - al, 0, segs, "spike", style="band",
             art=dict(tone=0.0), drag=0, lag=0, flutter=0, wave=(0.15, 0.5), wspeed=q(22.5 + k * 7.5), group="crest")
# 発光器官：琥珀色の器官が仮面の左右に弧を描いて並び、中央には青い宝玉が縦に並ぶ
EYES = ((16.5, 86, 5.5), (21.5, 77, 6.0), (22.0, 67.5, 5.0), (17.5, 59.5, 4.5), (12.5, 52.5, 4.0), (8.0, 46.5, 3.0))
for sd, t_ in ((1, "a"), (-1, "b")):
    for j, (x, y, sz_) in enumerate(EYES):
        s2 = sz_ * FS * (1.0 if sd > 0 else (0.9 if j % 2 else 1.08))            # 左右で少し大きさを変える
        organ(f"eye_{t_}{j}", "face", fp(sd * x, y, -6.5) - (0, 0, s2 * 0.2), s2, "amber", label="amber_organ")
for j, (y, sz_, kind) in enumerate(((66, 8.0, "gem"), (78, 4.5, "gem"), (86.5, 3.5, "gem"), (55, 3.5, "amber"))):
    organ(f"face_gem_{j}", "face", fp(0, y, -8.5) - (0, 0, sz_ * FS * 0.25), sz_ * FS, kind, halo=1.9 if j == 0 else 1.7, label="face_gem")
# あごひげ：仮面の下のふちから垂れる細い器官（先に小さな光の玉）
for k in range(12):
    x = rnd.uniform(-13, 13)
    ln = rnd.uniform(18, 46)
    segs = seg_list(ln, 5, 1.5, 1.0, 1.5, 1.0, bend=lambda t, i: (rnd.uniform(-6, 6), 0, rnd.uniform(-7, 7)), shrink=0.0)
    wb = fp(x, 38 + abs(x) * 1.6, -1)
    tentacle(f"whisker_{k}", "face", wb, 0, -90, 0, segs, "fil", style="band", share=("fil", k % 4),
             drag=0.9, lag=0.3, flutter=0.12, ffreq=0.5, wave=(1.5, 4.0), wspeed=q(45 + (k % 4) * 15), wstep=45, group="whisker")
    # 鎖は回転前は +X へ伸びるので、先端は付け根 + (長さ, 0, 0)
    box(TENT[f"whisker_{k}"]["names"][-1], wb + (ln - 1.25, -1.25, -1.25), (2.5, 2.5, 2.5), "mote", share=("mote", 5, 0), label="whisker_bulb")

# 触角（傘の肩から外・上へ伸び、先に淡い黄色の玉）
for k in range(16):
    phi = -90 + (k + 0.5) * 22.5 + rnd.uniform(-6, 6)
    f = rnd.uniform(0.45, 0.85)
    y = HY + HH * (1 - f ** 2.0) ** (1 / 2.3)
    ln = rnd.uniform(26, 48)
    cz = curl(rnd.choice([-1, 1]) * rnd.uniform(60, 160), ts=0.4, n=8)
    segs = seg_list(ln, 8, 1.6, 1.0, 1.6, 1.0, bend=lambda t, i: (0, rnd.uniform(-5, 5), cz[i] - 4, cz[i]), shrink=0.3)
    tentacle(f"ant_{k}", "head", rim_pt(phi, f, y), rim_dir(phi) + rnd.uniform(-20, 20), rnd.uniform(35, 70), 0, segs, "fil", style="band", share=("fil", k % 4),
             drag=0.8, lag=0.4, flutter=0.10, ffreq=0.45, wave=(0.8, 2.6), wspeed=q(45 + (k % 3) * 15), wstep=40, group="antenna")
    organ(f"ant_bulb_{k}", TENT[f"ant_{k}"]["names"][-1],
          np.array([rim_pt(phi, f, y)[0] + ln + 1.5, rim_pt(phi, f, y)[1], rim_pt(phi, f, y)[2]]), rnd.choice([3.0, 3.5, 4.0]), "pale", label="antenna_bulb")

# 上へ伸びる大型触腕（傘の肩から立ち上がり、先が渦を巻く）
UPS = ((12, 0.84, 48, 92, 26, 1), (48, 0.78, 58, 98, 26, -1), (96, 0.70, 54, 90, 23, 1))
for sd, t_ in ((1, "a"), (-1, "b")):
    for j, (phi, f, e, ln, w0, cs) in enumerate(UPS):
        ph = phi if sd > 0 else 180 - phi
        n = 16
        cz = curl(cs * rnd.uniform(250, 380), ts=0.48, n=n)
        mb = rnd.uniform(0, 6)
        segs = seg_list(ln * rnd.uniform(0.94, 1.06), n, w0, 4.0, w0 * 0.45, 2.5,
                        bend=lambda t, i: (rnd.uniform(-3, 3), rnd.uniform(-3, 3), cz[i] - 2.2 + 3.0 * math.sin(t * 5 + mb), cz[i]), shrink=0.5)
        y = HY + HH * (1 - f ** 2.0) ** (1 / 2.3) - 2
        tentacle(f"up_{t_}{j}", "head", rim_pt(ph, f, y), rim_dir(ph) + rnd.uniform(-8, 8), e + rnd.uniform(-5, 5), rnd.uniform(-25, 25), segs, "tent", art=dict(hue=0.15 + 0.1 * j),
                 share=("up", j), drag=0.85, lag=1.0, flutter=0.09, ffreq=0.15, wave=(0.4, 1.6), wspeed=q(15 + j * 7.5 + (7.5 if sd < 0 else 0)), wstep=24, group="tentacle_up")

# ---------------------------------------------------------------------------
# B. 胴体（首 → 胴 → 下へ細る房）
# ---------------------------------------------------------------------------
bone("torso", "core", (0, HY, ZC))          # 胴全体（頭より少し遅れて向きを変える）
BODY_Y = (52, 30, 8, -14, -36, -56, -74)    # 胴の節の境目（上から）
BR = ((52, 25), (40, 31), (26, 38), (8, 43), (-8, 42), (-26, 37), (-44, 29), (-60, 20), (-74, 12))   # (y, 半径 x)
def body_r(y):
    for (ya, ra), (yb, rb) in zip(BR, BR[1:]):
        if ya >= y >= yb:
            return ra + (rb - ra) * (ya - y) / (ya - yb)
    return BR[-1][1] if y < BR[-1][0] else BR[0][1]
BODY = []
par = "torso"
for i in range(6):
    BODY.append(bone(f"body_{i}", par, (0, BODY_Y[i], ZC))); par = BODY[-1]
    # 波が上から下へ伝わる（ゆるやかな波動）
    add_anim(BODY[-1], "rot", 0, 0.5 + 0.25 * i, 22.5, 60 - i * 45); add_anim(BODY[-1], "rot", 2, 0.5 + 0.25 * i, 15, 200 - i * 50)
    if i:
        add_rig(BODY[-1], 0.55 * (i / 5) ** 1.2 / 2.6, 0.8 + 0.15 * i, 0.012 * i, 0.11, -i * 0.7)
    # 呼吸：胴がゆっくりふくらんで縮む（上の節ほど大きく）
    br = 0.022 * (1.0 if i in (1, 2) else 0.5)
    for ax in (0, 2):
        add_anim(BODY[-1], "scale", ax, br, 45, 90 - i * 25)
def body_bone(y):
    for i in range(6):
        if y >= BODY_Y[i + 1]:
            return BODY[i]
    return BODY[5]
BL = 6.0
y = 52.0
while y > -74:
    y0 = y - BL
    r = body_r(y0 + BL / 2)
    ring(body_bone(y0 + BL / 2), y0, BL, r, r * 0.86, "body", dict(), thick=min(r * 0.8, 9.0), n=12, label="body", cap=(y0 <= -72 or y == 52.0))
    y = y0
# 胴の表面を流れ落ちる縦のひだ（長さ・太さは不ぞろい。先は淡く光る）
for k in range(34):
    ang = rnd.uniform(0, 360)
    ytop = rnd.uniform(-58, 44)
    ln = rnd.uniform(16, 42); w = rnd.uniform(4, 8)
    a = math.radians(ang)
    r = body_r(ytop - ln * 0.3) + 0.6
    p = np.array([r * math.cos(a), ytop, ZC + r * 0.86 * math.sin(a)])
    box(body_bone(ytop - ln / 2), (p[0] - 1.5, ytop - ln, p[2] - w / 2), (3, ln, w), "flute", dict(ln=ln * U), rot=(0, -ang, -rnd.uniform(2, 7)), pivot=p,
        share=("flute", round(ln), round(w), k % 3), label="body_flute")
# 胴の内部で光る器官：前面に縦に並ぶ青い器官と、まわりの琥珀色の器官
for j, (y, s) in enumerate(((34, 7.0), (18, 10.0), (0, 8.0), (-18, 6.0), (-34, 4.5))):
    organ(f"core_organ_{j}", body_bone(y), (0, y, ZC - body_r(y) * 0.86 - 0.5), s, "gem", halo=2.0, pulse=(0.14, 0.09), label="core_organ")
for k in range(14):
    ang = rnd.uniform(0, 360); y = rnd.uniform(-50, 40); a = math.radians(ang)
    r = body_r(y) + 0.3
    organ(f"body_organ_{k}", body_bone(y), (r * math.cos(a) * 0.96, y, ZC + r * 0.86 * math.sin(a) * 0.96), rnd.choice([3.0, 3.5, 4.5, 5.0]),
          rnd.choice(["amber", "gem", "pale"]), halo=1.9, pulse=(0.16, 0.10), label="body_organ")
# 下へ細る房（つららのような器官。中央ほど長い）
for k in range(18):
    ang = rnd.uniform(0, 360); rr = rnd.uniform(0, 17) if k else 0
    a = math.radians(ang)
    ln = (38 - rr * 0.9) * rnd.uniform(0.8, 1.05) if k else 36
    y0 = -66 - rnd.uniform(0, 6) + rr * 0.6
    w0 = rnd.uniform(7, 11) if k else 12
    n = 7
    segs = seg_list(ln, n, w0, 1.5, w0, 1.5, bend=lambda t, i: (rnd.uniform(-2.5, 2.5), 0, rnd.uniform(-2.5, 2.5)), shrink=0.1, wpow=0.8)
    tentacle(f"strand_{k}", BODY[5], (rr * math.cos(a), y0, ZC + rr * math.sin(a)), rnd.uniform(0, 360), -90 + rnd.uniform(-5, 5), 0, segs, "strand", style="band",
             share=("strand", k % 6) if k else None, drag=0.7, lag=1.2, flutter=0.05, ffreq=0.2, wave=(0.3, 1.2), wspeed=q(22.5 + (k % 4) * 7.5), wstep=30, group="strand")
# 口腕（傘の裏から垂れる、ひだの多い器官）
for k in range(12):
    phi = k * 30.0 + rnd.uniform(-8, 8)
    f = rnd.uniform(0.30, 0.42)
    ln = rnd.uniform(56, 92)
    n = 11
    zig = rnd.uniform(0, 6)
    segs = seg_list(ln, n, rnd.uniform(13, 17), 4.0, 4.0, 1.5, bend=lambda t, i: (rnd.uniform(-8, 8), 0, 11 * math.sin(i * 1.7 + zig) + rnd.uniform(-4, 4)), shrink=0.3)
    tentacle(f"oral_{k}", "torso", rim_pt(phi, f, HY - 12), rim_dir(phi) + rnd.uniform(-30, 30), -80 + rnd.uniform(-6, 6), rnd.uniform(-30, 30), segs, "oral",
             share=("oral", k % 4), drag=0.9, lag=0.8, flutter=0.10, ffreq=0.24, wave=(0.8, 2.6), wspeed=q(30 + (k % 3) * 7.5), wstep=36, group="oral_arm")

# ---------------------------------------------------------------------------
# C. 大型触腕（傘の裏の付け根「mantle」から伸びる）
# ---------------------------------------------------------------------------
bone("mantle", "torso", (0, HY - 4, ZC))
add_anim("mantle", "rot", 1, 0.8, 15, 300)
def mant_pt(phi, f=0.60, y=HY - 5):
    return rim_pt(phi, f, y)

def large(name, phi, f, y, yaw_off, elev, roll, ln, w0, curl_deg, ts, sag, share, hue, n=22, drag=1.3, lag=1.0, flut=0.10, group="tentacle", wsp=22.5, w1=4.0):
    cz = curl(curl_deg, ts=ts, n=n)
    mb = rnd.uniform(0, 6); my = rnd.uniform(0, 6)
    segs = seg_list(ln, n, w0, w1, w0 * 0.46, 2.4,
                    bend=lambda t, i: (rnd.uniform(-2.5, 2.5), 3.0 * math.sin(t * 6 + my), cz[i] + sag * (1 - t) + 2.5 * math.sin(t * 7 + mb), cz[i]), shrink=0.5)
    return tentacle(name, "mantle", mant_pt(phi, f, y), rim_dir(phi) + yaw_off, elev, roll, segs, "tent", art=dict(hue=hue), share=share,
                    drag=drag, lag=lag, flutter=flut, ffreq=rnd.uniform(0.12, 0.19), wave=(0.35, 1.5), wspeed=q(wsp), wstep=22, group=group)

# 左右へ広がる触腕（片側 6 本）：上のものは持ち上がって内へ巻き、中ほどは遠くまで伸びて垂れ、下のものは外へ垂れて鉤のように巻く
#        (phi, 仰角, 長さ, 幅, 渦, 渦の始まり, たるみ)
LATS = ((-46, 26, 118, 28, 330, 0.50, -1.0), (-24, 12, 136, 34, -300, 0.56, -0.6), (-4, 2, 142, 36, 280, 0.60, -1.2),
        (18, -10, 134, 34, -340, 0.55, -0.8), (40, -24, 120, 30, -260, 0.52, -0.4), (60, -40, 106, 26, 300, 0.50, 0.6))
for sd, t_ in ((1, "a"), (-1, "b")):
    for j, (phi, e, ln, w0, cd, ts, sag) in enumerate(LATS):
        ph = phi if sd > 0 else 180 - phi
        k = rnd.uniform(0.92, 1.08)                       # 左右で長さ・渦・角度を少し変える（左右対称にしすぎない）
        cdd = cd * rnd.uniform(0.8, 1.2) * (-1 if (sd < 0 and j in (1, 4)) else 1)
        large(f"lat_{t_}{j}", ph + rnd.uniform(-5, 5), 0.66, HY - 4 - j * 1.5, rnd.uniform(-7, 7), e + rnd.uniform(-5, 5), rnd.uniform(-22, 22),
              ln * k, w0, cdd, ts, sag, ("lat", j), 0.1 * j, n=20, wsp=15 + (j % 3) * 7.5 + (7.5 if sd < 0 else 0), group="tentacle_side")
# 後ろへ伸びる触腕（8 本）
BACKS = ((70, 12, 180, 28, 520), (82, -4, 200, 32, -560), (96, 4, 208, 32, 540), (108, -14, 188, 28, -500),
         (76, -30, 180, 26, 480), (104, -34, 190, 26, -520), (88, 26, 168, 24, 540), (94, -52, 180, 24, 500))
for j, (phi, e, ln, w0, cd) in enumerate(BACKS):
    large(f"back_{j}", phi + rnd.uniform(-4, 4), 0.72, HY - 6 - (j % 3) * 4, rnd.uniform(-10, 10), e, rnd.uniform(-30, 30), ln, w0, cd * 1.5, 0.18, -0.8,
          None, 0.3 + 0.05 * j, n=26, wsp=15 + (j % 4) * 7.5, group="tentacle_back")
# 前へ伸びる触腕（4 本。顔を隠さないよう、傘の下から外・下へ）
FRONTS = ((-52, -40, 66, 22, -280), (-128, -36, 62, 22, 300), (-66, -62, 64, 19, 240), (-114, -66, 66, 19, -220))
for j, (phi, e, ln, w0, cd) in enumerate(FRONTS):
    large(f"front_{j}", phi, 0.70, HY - 8, rnd.uniform(-8, 8), e, rnd.uniform(-25, 25), ln, w0, cd, 0.5, 0.4, None, 0.2 + 0.1 * j, n=14, wsp=22.5 + j * 7.5, group="tentacle_front")
# 下へ垂れる触腕（6 本。先が鉤のように巻き上がる）
for j in range(5):
    phi = j * 72.0 + 40 + rnd.uniform(-10, 10)
    large(f"down_{j}", phi, 0.52, HY - 9, rnd.uniform(-20, 20), -66 + rnd.uniform(-8, 8), rnd.uniform(-40, 40), rnd.uniform(168, 196), rnd.uniform(22, 27),
          rnd.choice([-1, 1]) * rnd.uniform(440, 540), 0.42, 0.0, None, 0.5 + 0.06 * j, n=24, wsp=15 + (j % 3) * 7.5, group="tentacle_down")
# 帯状の長い触腕（薄く長い帯。10 本。ゆるく波打ちながら流れる）
for j in range(10):
    phi = -18 + j * 24.0 + rnd.uniform(-8, 8)            # 左右〜後ろに付ける（真正面に伸びる帯は、移動しても後ろへ流れにくいため）
    ln = rnd.uniform(300, 380)
    n = 40
    mb, my = rnd.uniform(0, 6), rnd.uniform(0, 6)
    cz = curl(rnd.choice([-1, 1]) * rnd.uniform(720, 900), ts=0.26, p=1.2, n=n)
    zz = [20 * math.sin(i * 1.05 + mb) for i in range(n)]          # 静止時はひだのように折りたたまれ、移動すると伸びて流れる
    segs = seg_list(ln, n, rnd.uniform(7, 10), 2.5, 1.5, 1.0,
                    bend=lambda t, i: (rnd.uniform(-5, 5), 5 * math.sin(t * 9 + my), cz[i] + zz[i] - 1.0, cz[i] + zz[i] * 0.85), shrink=0.2)
    e = rnd.uniform(-82, -60)
    tentacle(f"rib_{j}", "mantle", mant_pt(phi, 0.78, HY - 3), rim_dir(phi) + rnd.uniform(-15, 15), e, rnd.uniform(-50, 50), segs, "band", style="band", art=dict(var=j % 3),
             drag=1.1, lag=1.3, flutter=0.15, ffreq=rnd.uniform(0.16, 0.24), wave=(0.6, 2.4), wspeed=q(15 + (j % 4) * 7.5), wstep=30, group="ribbon")

# ---------------------------------------------------------------------------
# D. 小型触腕
# ---------------------------------------------------------------------------
# 傘のふちの細い触腕（短いものは細かく揺れる）
for k in range(48):
    phi = rnd.uniform(0, 360)
    if abs((phi + 90 + 180) % 360 - 180) < 12:
        continue
    ln = rnd.uniform(24, 64)
    n = 7
    cz = curl(rnd.choice([-1, 1]) * rnd.uniform(40, 200), ts=0.45, n=n)
    w0 = rnd.uniform(2.5, 4.5)
    segs = seg_list(ln, n, w0, 1.0, w0 * 0.6, 1.0, bend=lambda t, i: (rnd.uniform(-5, 5), rnd.uniform(-6, 6), cz[i] + rnd.uniform(-5, 5), cz[i]), shrink=0.3)
    tentacle(f"rim_{k}", "head", rim_pt(phi, rnd.uniform(0.86, 0.98), HY - 1), rim_dir(phi) + rnd.uniform(-25, 25), rnd.uniform(-78, -30), rnd.uniform(-40, 40), segs,
             "tent", style="band", art=dict(hue=rnd.uniform(0.3, 0.9), small=1), share=("rim", k % 8), drag=1.0, lag=0.3, flutter=0.13, ffreq=rnd.uniform(0.4, 0.65),
             wave=(1.2, 3.6), wspeed=q(rnd.choice([45, 52.5, 60, 75, 90])), wstep=42, group="tentacle_small_rim")
# 胴のまわりの細い触腕
for k in range(40):
    ang = rnd.uniform(0, 360); y = rnd.uniform(-66, 30); a = math.radians(ang)
    r = body_r(y) * 0.92
    ln = rnd.uniform(30, 84)
    n = 8
    cz = curl(rnd.choice([-1, 1]) * rnd.uniform(40, 220), ts=0.5, n=n)
    w0 = rnd.uniform(2.0, 4.0)
    segs = seg_list(ln, n, w0, 1.0, w0 * 0.6, 1.0, bend=lambda t, i: (rnd.uniform(-5, 5), rnd.uniform(-6, 6), cz[i] + rnd.uniform(-5, 5) - 3, cz[i]), shrink=0.3)
    tentacle(f"btl_{k}", body_bone(y), (r * math.cos(a), y, ZC + r * 0.86 * math.sin(a)), ang + rnd.uniform(-25, 25), rnd.uniform(-70, -15), rnd.uniform(-40, 40), segs,
             "tent", style="band", art=dict(hue=rnd.uniform(0.3, 0.9), small=1), share=("btl", k % 8), drag=1.0, lag=0.4, flutter=0.12, ffreq=rnd.uniform(0.35, 0.6),
             wave=(1.0, 3.2), wspeed=q(rnd.choice([37.5, 45, 52.5, 60, 75])), wstep=40, group="tentacle_small_body")

# ---------------------------------------------------------------------------
# E. まわりに漂う星屑（12 の群れがそれぞれ別の周期でゆっくり回り、上下する）
# ---------------------------------------------------------------------------
for gi in range(12):
    gn = bone(f"dust_{gi}", "core", (0, rnd.uniform(-20, 60), ZC))
    add_anim(gn, "rot", 1, rnd.uniform(4, 9), q(7.5 * rnd.choice([1, 2, 3])), rnd.uniform(0, 360))
    add_anim(gn, "pos", 1, rnd.uniform(1.5, 4) * U, q(rnd.choice([15, 22.5, 30, 37.5])), rnd.uniform(0, 360))
    for _ in range(34):
        ang = rnd.uniform(0, math.tau); rr = rnd.uniform(40, 180)
        p = (math.cos(ang) * rr, rnd.uniform(-98, 140), ZC + math.sin(ang) * rr * 0.62)
        s = rnd.choice([0.5, 1.0, 1.0, 1.5, 2.0])
        box(gn, p, (s, s, s), rnd.choice(["mote", "mote", "mote_c", "mote_y"]), share=("mote", round(s * U), rnd.randint(0, 2)), label="stardust")

# ===========================================================================
# Box UV の割り付け
# ===========================================================================
def foot(size):
    w, h, d = size
    return 2 * (w + d), d + h
slots = {}
for b in bones:
    for c in b["cubes"]:
        key = (tuple(c["size"]), c["mat"], c["key"])
        s = slots.setdefault(key, {"size": c["size"], "mat": c["mat"], "labels": [], "cube": c})
        if c["label"] not in s["labels"]:
            s["labels"].append(c["label"])
        c["slot"] = key
TEX_W = 4096
order = sorted(slots, key=lambda k: (-foot(slots[k]["size"])[1], -foot(slots[k]["size"])[0]))
u = v = shelf = 0
for k in order:
    fw, fh = foot(slots[k]["size"])
    if u + fw > TEX_W:
        u = 0; v += shelf; shelf = 0
    slots[k]["uv"] = (u, v); u += fw; shelf = max(shelf, fh)
TEX_H = -(-(v + shelf) // 64) * 64
area = {}
for k_ in slots:
    fw_, fh_ = foot(slots[k_]["size"]); area[slots[k_]["mat"]] = area.get(slots[k_]["mat"], 0) + fw_ * fh_
print("area(Mpx):", {m: round(a / 1e6, 2) for m, a in sorted(area.items(), key=lambda x: -x[1])})
assert TEX_H <= 4096, f"テクスチャが大きすぎる: {TEX_H}"

# ===========================================================================
# テクスチャ：各面のピクセル → キューブの中の位置 Q（回転前の座標）と、静止姿勢での位置 P（モデル空間）から色を決める
# ===========================================================================
tex = np.zeros((TEX_H, TEX_W, 4), np.float32)
glow = np.zeros((TEX_H, TEX_W, 4), np.float32)
def C(s, a=255):
    s = s.lstrip("#"); return np.array([int(s[i:i + 2], 16) for i in (0, 2, 4)] + [a], np.float32)
INDIGO = C("#161058"); NAVY = C("#1c2a9c"); ROYAL = C("#2b47d6"); VIOLET = C("#5a36d8"); PURPLE = C("#8a5cf2"); MAGENTA = C("#a66bf5")
AZURE = C("#2f8cff"); CYAN = C("#3fd4ff"); TURQ = C("#33e2cf"); LAV = C("#b9a8ff"); SILVER = C("#dfe4ff"); WHITE = C("#ffffff")
AMBER = C("#ffb43c"); AMBER_D = C("#c8741a"); CREAM = C("#fff0b4"); GOLD = C("#e8c46a"); GOLD_D = C("#a8843a")
G_CYAN = C("#8fe9ff"); G_WHITE = C("#f2f8ff"); G_AMBER = C("#ffc65a"); G_PALE = C("#fff3b8"); G_VIO = C("#b99cff")

def mix(a, b, t):
    t = np.clip(np.asarray(t, np.float32), 0, 1)
    if t.ndim:
        t = t[..., None]
    return a * (1 - t) + b * t
def ramp(cols, t):
    """色の並びを t（0〜1）でなめらかにたどる。"""
    t = np.clip(t, 0, 0.9999) * (len(cols) - 1)
    i = np.floor(t).astype(int); f = (t - i)[..., None]
    arr = np.array(cols)
    return arr[i] * (1 - f) + arr[i + 1] * f
def hash3(x, y, z, seed=0.0):
    h = np.sin(np.asarray(x, np.float64) * 127.1 + np.asarray(y, np.float64) * 311.7 + np.asarray(z, np.float64) * 74.7 + seed * 19.19) * 43758.5453
    return h - np.floor(h)
def noise3(p, scale, seed=0.0):
    p = p / scale
    i = np.floor(p); f = p - i; f = f * f * (3 - 2 * f)
    out = 0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (f[..., 0] if dx else 1 - f[..., 0]) * (f[..., 1] if dy else 1 - f[..., 1]) * (f[..., 2] if dz else 1 - f[..., 2])
                out = out + w * hash3(i[..., 0] + dx, i[..., 1] + dy, i[..., 2] + dz, seed)
    return out
def fbm(p, scale, seed=0.0):
    return (noise3(p, scale, seed) * 0.55 + noise3(p, scale / 2.1, seed + 3) * 0.3 + noise3(p, scale / 4.3, seed + 7) * 0.15)

NEB = [INDIGO, NAVY, VIOLET, ROYAL, PURPLE, AZURE, MAGENTA]
def nebula(P, bias=0.0, seed=0.0):
    """星雲の地色：場所（モデル空間）で決まるので、キューブをまたいでも模様がつながる。"""
    n = fbm(P, 110.0, seed)
    n2 = fbm(P + 500, 46.0, seed + 11)
    col = ramp(NEB, np.clip((n - 0.22) * 1.55 + bias, 0, 1))
    wisp = np.clip((n2 - 0.60) * 4.0, 0, 1)                   # 淡く光る雲
    col = mix(col, mix(CYAN, LAV, noise3(P, 70.0, seed + 5)), wisp * 0.55)
    return col, wisp
def stars(Pc, dens, seed=0.0):
    h = hash3(Pc[..., 0], Pc[..., 1], Pc[..., 2], seed)
    return h < dens, h < dens * 0.25

def face_grids(size):
    """各面のピクセル → キューブ内の位置（0〜size。描画空間）。戻り値：{面: (rect, X, Y, Z)}"""
    w, h, d = size
    out = {}
    def g(n_u, n_v):
        return np.meshgrid(np.arange(n_u) + 0.5, np.arange(n_v) + 0.5)
    uu, vv = g(w, d); out["up"] = ((d, 0, w, d), w - uu, np.full_like(uu, h), d - vv)
    out["down"] = ((d + w, 0, w, d), w - uu, np.zeros_like(uu), d - vv)
    uu, vv = g(w, h); out["north"] = ((d, d, w, h), w - uu, h - vv, np.zeros_like(uu))
    out["south"] = ((2 * d + w, d, w, h), uu, h - vv, np.full_like(uu, d))
    uu, vv = g(d, h); out["west"] = ((d + w, d, d, h), np.zeros_like(uu), h - vv, uu)
    out["east"] = ((0, d, d, h), np.full_like(uu, w), h - vv, d - uu)
    return out

CELL = 2.0    # 色の 1 マス（ピクセル）= 1 ブロック。ブロックで組んだような見た目にする
def paint(mat, art, c, fname, X, Y, Z):
    lo = c["lo"]; w, h, d = c["size"]
    Q = np.stack([lo[0] + X, lo[1] + Y, lo[2] + Z], -1)                     # 回転前の座標（ピクセル）
    Qc = np.stack([lo[0] + (np.floor(X / CELL) + 0.5) * CELL, lo[1] + (np.floor(Y / CELL) + 0.5) * CELL, lo[2] + (np.floor(Z / CELL) + 0.5) * CELL], -1)
    M = bidx[c["bone"]]["M"]
    if "rot" in c:
        M = M @ about(c["pivot"], rot3(c["rot"]))
    P = Qc @ M[:3, :3].T + M[:3, 3]                                          # 静止姿勢での位置（マスの中心）
    cellid = np.floor(P / CELL)
    jit = (hash3(cellid[..., 0], cellid[..., 1], cellid[..., 2], 1.7) - 0.5)   # マスごとの明るさのむら
    gl = np.zeros(Q.shape[:2] + (4,), np.float32)
    st, st_big = stars(cellid, 0.012, 3.1)

    def finish(col, alpha=255, star=True, shade=0.10):
        col = np.array(col, np.float32)
        col[..., :3] = np.clip(col[..., :3] * (1 + jit[..., None] * shade * 2), 0, 255)
        if np.ndim(alpha) == 0:
            col[..., 3] = alpha
        else:
            col[..., 3] = alpha
        if star:
            col[st] = mix(col[st], WHITE, 0.85); gl[st] = G_WHITE * np.array([1, 1, 1, 0.8], np.float32)
        return col, gl

    if mat in ("tent", "band", "oral", "strand", "lobe", "veil", "fringe", "fil", "spike", "star"):
        b0 = art["base"]; S = art["S"]; W = art["W"]
        s = Q[..., 0] - b0[0]; sc = Qc[..., 0] - b0[0]
        t = np.clip(sc / S, 0, 1)
        az = Q[..., 2] - b0[2]; ay = Q[..., 1] - b0[1]
        a = np.clip(np.abs(az) / max(1.0, W / 2), 0, 1.3)                    # 幅方向（0 = 中心、1 = ふち）
    if mat == "tent":
        neb, wisp = nebula(P, seed=art.get("hue", 0) * 3)
        hue = art.get("hue", 0.0)
        grad = ramp([INDIGO, VIOLET, mix(ROYAL, PURPLE, hue), ROYAL, mix(ROYAL, AZURE, 0.6), AZURE, CYAN], np.clip(t ** 1.25 * 0.97 + hue * 0.08, 0, 1))
        col = mix(grad, neb, 0.5)
        col = mix(col, INDIGO, np.clip(0.5 - a, 0, 1) * 0.35)                # 中心は深い色
        col = mix(col, mix(AZURE, LAV, 0.5), np.clip((a - 0.62) / 0.3, 0, 1) * 0.45)          # ふちへ向かって淡く
        edge = (a > 0.88)
        col[edge] = mix(col[edge], mix(CYAN, SILVER, 0.5), 0.7)
        gl[edge & (np.mod(s, 14) < 9)] = G_CYAN * np.array([1, 1, 1, 0.42], np.float32)     # ふちの光る破線
        if not art.get("small"):
            spot = (np.abs(az) < 2.0) & (np.mod(s + 6, 22) < 4)              # 中心に並ぶ光の斑点
            col[spot] = mix(col[spot], CYAN, 0.8); gl[spot] = G_CYAN * np.array([1, 1, 1, 0.6], np.float32)
            dia = (np.abs(np.mod(s, 26) - 13) + np.abs(np.abs(az) - W * 0.27)) < 2.6      # 結晶の模様（ひし形）
            col[dia] = mix(col[dia], LAV, 0.6); gl[dia] = G_WHITE * np.array([1, 1, 1, 0.2], np.float32)
        gl[wisp > 0.5] = np.maximum(gl[wisp > 0.5], G_CYAN * np.array([1, 1, 1, 0.22], np.float32))
        tipg = np.clip((t - 0.9) / 0.1, 0, 1)
        col = mix(col, SILVER, tipg * 0.6)
        return finish(col, 226 if not art.get("small") else 236)
    if mat == "fringe":
        F = art["F"]; vv_ = (np.abs(az) - art["zin"]) / F                    # 0 = 触腕側、1 = 外のふち
        cut = 0.62 + 0.30 * np.sin(s / 7.0 + art["i"] * 1.3) + 0.12 * np.sin(s / 2.6)
        mask = (vv_ < cut) & ((np.mod(np.floor(s / 2) + np.floor(np.abs(az) / 2), 5) > 0) | (vv_ < 0.25))   # ふちは波形、中は細かい穴
        col = mix(mix(ROYAL, AZURE, t), CYAN, np.clip(vv_ / np.maximum(cut, 0.1), 0, 1) ** 2 * 0.9)
        rim = mask & (vv_ > cut - 0.16)
        col, _ = finish(col, 205, star=False)
        gl[rim] = G_CYAN * np.array([1, 1, 1, 0.45], np.float32)
        col[~mask] = 0; gl[~mask] = 0
        if fname not in ("up", "down"):
            col[...] = 0; gl[...] = 0
        return col, gl
    if mat == "band":
        var = art.get("var", 0)
        col = ramp([[PURPLE, LAV, AZURE, CYAN, WHITE], [ROYAL, AZURE, CYAN, TURQ, WHITE], [VIOLET, MAGENTA, LAV, CYAN, WHITE]][var], t)
        neb, _ = nebula(P)
        col = mix(col, neb, 0.25)
        core = np.abs(az) < 1.2
        col[core] = mix(col[core], WHITE, 0.7); gl[core] = G_CYAN * np.array([1, 1, 1, 0.7], np.float32)
        dia = (np.abs(np.mod(s, 20) - 10) + np.abs(az)) < 3.4
        col[dia] = WHITE; gl[dia] = G_WHITE * np.array([1, 1, 1, 0.8], np.float32)
        edge = a > 0.8
        col[edge] = mix(col[edge], SILVER, 0.6); gl[edge] = G_CYAN * np.array([1, 1, 1, 0.35], np.float32)
        mask = a < (0.96 - 0.16 * (0.5 + 0.5 * np.sin(s / 6.0)))               # 波打つふち
        col, _ = finish(col, 214)
        col[~mask] = 0; gl[~mask] = 0
        return col, gl
    if mat == "oral":
        neb, wisp = nebula(P, bias=0.2)
        col = mix(ramp([PURPLE, MAGENTA, LAV, AZURE, CYAN], t), neb, 0.35)
        ruff = 0.5 + 0.5 * np.sin(s / 3.2 + a * 5)
        col = mix(col, SILVER, np.clip((a - 0.55) / 0.4, 0, 1) * (0.35 + 0.5 * ruff))
        gl[(a > 0.86) & (ruff > 0.5)] = G_VIO * np.array([1, 1, 1, 0.6], np.float32)
        return finish(col, 232)
    if mat == "strand":
        neb, wisp = nebula(P, bias=-0.1)
        col = mix(neb, ramp([NAVY, ROYAL, AZURE, CYAN, WHITE], t), 0.35 + 0.6 * t)
        streak = hash3(np.floor(P[..., 0] / 2), 0, np.floor(P[..., 2] / 2), 5.0) > 0.7
        col[streak] = mix(col[streak], SILVER, 0.35)
        gl[...] = G_CYAN * np.array([1, 1, 1, 0.0], np.float32)
        gl[..., :3] = G_CYAN[:3]; gl[..., 3] = 255 * np.clip((t - 0.55) / 0.45, 0, 1) ** 1.5 * 0.85
        return finish(col, 248)
    if mat == "lobe":
        neb, wisp = nebula(P, bias=0.12 * art.get("tier", 0))
        col = mix(neb, ramp([VIOLET, PURPLE, AZURE, CYAN], t), 0.25 + 0.45 * t)
        rimm = (a > 0.82) | ((t > 0.86) & art["tip"])
        col[rimm] = mix(col[rimm], SILVER, 0.7); gl[rimm] = G_CYAN * np.array([1, 1, 1, 0.3], np.float32)
        vein = (np.abs(az) < 1.1) | (np.mod(s + np.abs(az) * 0.9, 12) < 1.2)
        col[vein] = mix(col[vein], LAV, 0.45)
        env = np.sin(math.pi * np.clip((s / S) * 0.5 + 0.5, 0, 1)) ** 0.5       # 先へ向かって丸く細る葉の形
        mask = a < np.maximum(env, 0.2) * (1.0 - 0.07 * np.sin(s / 3.0))
        col, _ = finish(col, 248)
        col[~mask] = 0; gl[~mask] = 0
        return col, gl
    if mat == "veil":
        var = art.get("var", 0)
        col = ramp([PURPLE, LAV, AZURE, CYAN, SILVER], np.clip(t * 0.9 + 0.1 * np.sin(az / 5.0 + var), 0, 1))
        thread = np.mod(az + 100 + 1.5 * np.sin(s / 9.0 + var), 6) < 2.2             # 縦の筋（すき間は透ける）
        cross = np.mod(s + 3 * np.sin(az / 4.0), 15) < 1.6                          # 横糸
        hemline = 0.80 + 0.14 * np.sin(az / 4.5 + var * 1.7) + 0.05 * np.sin(az / 1.7)
        mask = (thread | cross | (t > hemline - 0.07)) & (t < hemline) & (a < 0.98)
        col, _ = finish(col, 200, star=False)
        hem = mask & (t > hemline - 0.07)
        col[hem] = SILVER; gl[hem] = G_CYAN * np.array([1, 1, 1, 0.8], np.float32)
        bead = cross & thread & mask
        gl[bead] = G_WHITE * np.array([1, 1, 1, 0.55], np.float32)
        col[~mask] = 0; gl[~mask] = 0
        if fname not in ("up", "down"):
            col[...] = 0; gl[...] = 0
        return col, gl
    if mat == "fil":
        col = mix(LAV, SILVER, t)
        gl[np.mod(s, 12) < 2] = G_WHITE * np.array([1, 1, 1, 0.7], np.float32)
        return finish(col, 240, star=False)
    if mat == "spike":
        tone = art.get("tone", 0)
        col = ramp([mix(LAV, PURPLE, 0.35 + tone), mix(LAV, PURPLE, tone), mix(SILVER, LAV, tone), WHITE], t)
        ring_ = np.mod(s, 9) < 1.2
        col[ring_] = mix(col[ring_], PURPLE, 0.25)
        gl[..., :3] = G_WHITE[:3]; gl[..., 3] = 255 * np.clip((t - 0.6) / 0.4, 0, 1) * 0.6
        return finish(col, 255, star=False, shade=0.05)
    if mat == "star":
        col = ramp([SILVER, WHITE, GOLD], np.clip(t * 1.1, 0, 1))
        edge = a > 0.7
        col[edge] = mix(col[edge], GOLD, 0.7)
        gl[np.abs(az) < 1.2] = G_PALE * np.array([1, 1, 1, 0.6], np.float32)
        return finish(col, 255, star=False, shade=0.04)
    # ---- 傘・胴（星雲の地色） ----
    cx, cz_ = 0.0, ZC * U
    ang = np.arctan2(P[..., 2] - cz_, P[..., 0] - cx)
    if mat in ("dome", "under"):
        neb, wisp = nebula(P, bias=0.08)
        hh_ = np.clip((P[..., 1] - HY * U) / (HH * U), 0, 1)
        col = mix(neb, PURPLE, hh_ * 0.28)
        if mat == "under":
            col = mix(col, INDIGO, 0.45)
            rib = np.mod(np.degrees(ang) + 360, 12) < 1.6
            col[rib] = mix(col[rib], CYAN, 0.5); gl[rib] = G_CYAN * np.array([1, 1, 1, 0.4], np.float32)
        else:
            if fname == "up":                                           # 段の上面：ふちを明るく（段々が見えるように）
                col = mix(col, LAV, 0.12)
            terr = (Y < 2.2) & (fname not in ("up", "down"))             # 段の下のふちの影
            col[terr] = mix(col[terr], INDIGO, 0.35)
            topl = (Y > h - 2.2) & (fname not in ("up", "down"))
            col[topl] = mix(col[topl], LAV, 0.3)
            vein = np.abs(np.mod(np.degrees(ang) + 7.5 + 4 * noise3(P, 60.0, 9), 15) - 7.5) < 0.5
            col[vein] = mix(col[vein], CYAN, 0.45); gl[vein] = G_CYAN * np.array([1, 1, 1, 0.3], np.float32)
        gl[wisp > 0.45] = np.maximum(gl[wisp > 0.45], G_CYAN * np.array([1, 1, 1, 0.26], np.float32))
        return finish(col, 255)
    if mat == "body":
        neb, wisp = nebula(P, bias=-0.06, seed=2.0)
        low = np.clip((-P[..., 1] / U - 10) / 60, 0, 1)                   # 下へいくほど青白く
        col = mix(neb, ramp([ROYAL, AZURE, CYAN], low), low * 0.45)
        fl = 0.5 + 0.5 * np.cos(ang * 16 + 1.2 * noise3(P, 50.0, 4))      # 縦のひだ
        col = mix(col, INDIGO, (1 - fl) ** 2 * 0.5)
        col = mix(col, LAV, fl ** 6 * 0.25)
        vein = (fl > 0.985) & (noise3(P, 40.0, 8) > 0.45)
        col[vein] = mix(col[vein], CYAN, 0.7); gl[vein] = G_CYAN * np.array([1, 1, 1, 0.55], np.float32)
        inner = np.clip((fbm(P, 60.0, 21) - 0.56) * 5, 0, 1)             # 内側から透ける光
        col = mix(col, mix(CYAN, WHITE, 0.3), inner * 0.5)
        gl[..., :3] = np.where(gl[..., 3:4] > 0, gl[..., :3], G_CYAN[:3]); gl[..., 3] = np.maximum(gl[..., 3], 255 * inner * 0.5)
        return finish(col, 255)
    if mat == "flute":
        neb, _ = nebula(P, seed=2.0)
        tt = np.clip(1 - Y / max(1, h), 0, 1)
        col = mix(mix(neb, VIOLET, 0.3), ramp([AZURE, CYAN, WHITE], tt), tt ** 1.5 * 0.8)
        gl[..., :3] = G_CYAN[:3]; gl[..., 3] = 255 * np.clip((tt - 0.7) / 0.3, 0, 1) * 0.8
        return finish(col, 250)
    if mat == "ridge":
        col = mix(LAV, SILVER, 0.5 + jit)
        gl[...] = G_CYAN * np.array([1, 1, 1, 0.25], np.float32)
        return finish(col, 255, star=False)
    if mat == "gill":
        col = mix(PURPLE, CYAN, np.clip(1 - Y / max(1, h), 0, 1))
        line = np.mod(X + art.get("var", 0), 4) < 1.4
        mask = line | (Y > h - 2) | (np.mod(Y, 6) < 1)
        col, _ = finish(col, 215, star=False)
        gl[mask & (Y < 3)] = G_CYAN * np.array([1, 1, 1, 0.8], np.float32)
        col[~mask] = 0
        if fname not in ("north", "south"):
            col[...] = 0; gl[...] = 0
        return col, gl
    # ---- 仮面・金 ----
    if mat == "mask":
        sh = (Y / max(1, h)) * 0.25 + np.abs(X - w / 2) / max(1, w) * 0.8
        col = mix(WHITE, mix(SILVER, LAV, 0.5), np.clip(sh, 0, 1))
        groove = (np.mod(np.abs(X - w / 2) + (art["row"] * 8 + Y) * 0.6, 9) < 1.1)       # V 字の彫り
        col[groove] = mix(col[groove], PURPLE, 0.35)
        edge = (np.abs(X - w / 2) > w / 2 - 2.2) & (fname in ("north", "south"))
        col[edge] = GOLD
        if fname in ("east", "west"):
            col = mix(col, GOLD, 0.6)
        return finish(col, 255, star=False, shade=0.03)
    if mat == "gold":
        col = mix(GOLD, CREAM, 0.3 + jit); gl[...] = G_PALE * np.array([1, 1, 1, 0.3], np.float32)
        return finish(col, 255, star=False, shade=0.04)
    # ---- 発光器官 ----
    if mat in ("amber", "gem", "pale", "mote", "mote_c", "mote_y") or mat.startswith("halo_"):
        fw_, fh_ = X.shape[1], X.shape[0]
        uu = (np.arange(fw_) + 0.5) / fw_ * 2 - 1; vv2 = (np.arange(fh_) + 0.5) / fh_ * 2 - 1
        r = np.sqrt(uu[None, :] ** 2 + vv2[:, None] ** 2)
        if mat.startswith("halo_"):
            base = {"amber": G_AMBER, "gem": G_CYAN, "pale": G_PALE}[mat[5:]]
            col = np.zeros(X.shape + (4,), np.float32); col[...] = base
            col[..., 3] = 62 * np.clip(1.15 - r, 0, 1)
            gl[...] = base; gl[..., 3] = 120 * np.clip(1.1 - r, 0, 1)
            return col, gl
        if mat == "amber":
            col = ramp([CREAM, AMBER, AMBER, AMBER_D], np.clip(r * 0.8, 0, 1)); gcol = G_AMBER
            slit = (np.abs(uu)[None, :] < 0.16) & (np.abs(vv2)[:, None] < 0.62)            # 縦に細い瞳孔のような芯
            col[slit] = mix(col[slit], C("#5a2a08"), 0.75)
        elif mat == "gem":
            col = ramp([WHITE, C("#b8f6ff"), CYAN, AZURE, ROYAL], np.clip(r * 0.85, 0, 1)); gcol = G_CYAN
        elif mat == "pale":
            col = ramp([WHITE, CREAM, C("#ffe38a")], np.clip(r * 0.8, 0, 1)); gcol = G_PALE
        else:
            gcol = {"mote": G_WHITE, "mote_c": G_CYAN, "mote_y": G_PALE}[mat]
            col = np.zeros(X.shape + (4,), np.float32); col[...] = mix(WHITE, gcol, 0.4)
        col = np.array(col, np.float32); col[..., 3] = 255
        gl[...] = gcol; gl[..., 3] = 255 * np.clip(1.25 - r * 0.6, 0, 1)
        if mat in ("amber", "gem", "pale") and min(fw_, fh_) >= 6:
            hl = np.sqrt((uu[None, :] + 0.4) ** 2 + (vv2[:, None] + 0.4) ** 2) < 0.28      # つやの点
            col[hl] = WHITE
            if mat == "amber":
                gl[slit] = gl[slit] * np.array([1, 1, 1, 0.25], np.float32)
        return col, gl
    col = np.zeros(X.shape + (4,), np.float32); col[...] = PURPLE
    return col, gl

for key, s in slots.items():
    c = s["cube"]
    u0, v0 = s["uv"]
    for fname, (rect, X, Y, Z) in face_grids(c["size"]).items():
        rx_, ry_, rw, rh = rect
        if rw <= 0 or rh <= 0:
            continue
        col, gl = paint(c["mat"], c["art"], c, fname, X, Y, Z)
        tex[v0 + ry_:v0 + ry_ + rh, u0 + rx_:u0 + rx_ + rw] = col
        glow[v0 + ry_:v0 + ry_ + rh, u0 + rx_:u0 + rx_ + rw] = gl
glow[tex[..., 3] <= 0] = 0
Image.fromarray(np.clip(tex, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, f"{NAME}.png"), optimize=True)
Image.fromarray(np.clip(glow, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, f"{NAME}_glowmask.png"), optimize=True)

# ===========================================================================
# 書き出し（描画空間 → Bedrock 形式：x を反転、回転の x・y を反転）
# ===========================================================================
def r3(v):
    return [round(float(x), 3) for x in v]
def export_geo(path, pose=None):
    """pose: {ボーン名: (回転の増分 度（描画空間）, 位置の増分 ピクセル)}。プレビュー用に姿勢を付けた geo を出せる。"""
    out = []
    for b in bones:
        nb = {"name": b["name"]}
        if b["parent"]:
            nb["parent"] = b["parent"]
        p = b["pivot"]; r = np.array(b["rot"], float)
        dp = np.zeros(3)
        if pose and b["name"] in pose:
            r = r + pose[b["name"]][0]; dp = pose[b["name"]][1]
        nb["pivot"] = r3((-p[0], p[1], p[2]))
        if np.abs(r).max() > 1e-6:
            nb["rotation"] = [round(float(-r[0]), 2), round(float(-r[1]), 2), round(float(r[2]), 2)]
        if np.abs(dp).max() > 1e-6:
            nb["_offset"] = r3(dp)
        if b["cubes"]:
            nb["cubes"] = []
            for c in b["cubes"]:
                lo = c["lo"]; sz = c["size"]
                nc = {"origin": r3((-(lo[0] + sz[0]), lo[1], lo[2])), "size": sz}
                if "rot" in c:
                    cp = c["pivot"]; cr = c["rot"]
                    nc["pivot"] = r3((-cp[0], cp[1], cp[2])); nc["rotation"] = [round(-cr[0], 2), round(-cr[1], 2), round(cr[2], 2)]
                nc["uv"] = list(slots[c["slot"]]["uv"])
                nb["cubes"].append(nc)
        out.append(nb)
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{NAME}", "texture_width": TEX_W, "texture_height": TEX_H,
                        "visible_bounds_width": 64, "visible_bounds_height": 48, "visible_bounds_offset": [0, 0, 0]},
        "bones": out}]}
    json.dump(geo, open(path, "w"), separators=(",", ":"), ensure_ascii=False)
export_geo(os.path.join(OUT, f"{NAME}.geo.json"))

T = "query.anim_time"
def expr(terms, base=0.0, sign=1.0):
    """Molang の式にする。負の振幅は位相を 180 度ずらして正の数だけで書く（「*-0.5」のような書き方を避ける）。"""
    parts = [] if base == 0 else [f"{base:g}"]
    for amp, speed, phase in terms:
        amp = amp * sign
        if amp < 0:
            amp, phase = -amp, (phase + 180) % 360
        if amp >= 0.0005:
            parts.append(f"math.sin({T}*{speed:g}-{phase:.0f})*{amp:.3f}")
    if not parts:
        return 0
    return "+".join(parts) if len(parts) > 1 or parts[0].startswith("math") else float(parts[0])
ab = {}
for name, a in anims.items():
    o = {}
    if "rot" in a:      # アニメの回転は Bedrock 形式（x・y の符号が描画空間と逆）
        o["rotation"] = [expr(a["rot"][0], sign=-1), expr(a["rot"][1], sign=-1), expr(a["rot"][2])]
    if "pos" in a:
        o["position"] = [expr(a["pos"][0], sign=-1), expr(a["pos"][1]), expr(a["pos"][2])]
    if "scale" in a:
        o["scale"] = [expr(a["scale"][i], base=1.0) or 1 for i in range(3)]
    ab[name] = o
json.dump({"format_version": "1.8.0", "animations": {f"animation.{NAME}.idle": {"loop": True, "animation_length": ANIM_LEN, "bones": ab}}},
          open(os.path.join(OUT, f"{NAME}.animation.json"), "w"), separators=(",", ":"), ensure_ascii=False)
json.dump({"comment": "generated by tools/astra_veil/generate.py. a = 3x3 (row-major) rotation gain for the drag vector, l = lag (0 fast .. 2 slow), f = flutter [amplitude rad, frequency Hz, phase rad]",
           "bones": rig}, open(os.path.join(OUT, f"{NAME}.rig.json"), "w"), separators=(",", ":"))
json.dump({"slots": [{"uv": s["uv"], "size": s["size"], "mat": s["mat"], "labels": s["labels"]} for s in slots.values()], "tex": [TEX_W, TEX_H]},
          open(os.path.join(OUT, "_slots.json"), "w"), ensure_ascii=False)

# ===========================================================================
# プレビュー用の姿勢（待機アニメの時刻 t、引きずる向き g、移動の強さ m を Java と同じ式で当てはめる）
# ===========================================================================
def make_pose(t, g=(0, 0, 0), m=0.0):
    pose = {}
    for name, a in anims.items():
        dr = np.zeros(3); dp = np.zeros(3)
        for ax in range(3):
            for amp, speed, phase in a.get("rot", [[], [], []])[ax]:
                dr[ax] += amp * math.sin(math.radians(t * speed - phase))
            for amp, speed, phase in a.get("pos", [[], [], []])[ax]:
                dp[ax] += amp * math.sin(math.radians(t * speed - phase))
        pose[name] = (dr, dp)
    g = np.asarray(g, float)
    for r in rig:
        d = np.degrees(np.array(r["a"]).reshape(3, 3) @ g)
        fa, ff, fp = r["f"]
        d[2] += math.degrees(m * fa * math.sin(math.tau * ff * t + fp) + m * r["u"])
        d[1] += math.degrees(m * fa * 0.6 * math.sin(math.tau * ff * t + fp + 1.3))
        dr, dp = pose.get(r["n"], (np.zeros(3), np.zeros(3)))
        pose[r["n"]] = (dr + d, dp)
    return pose
for pname, (t, g, m) in {"idle_a": (0.0, (0, 0, 0), 0), "idle_b": (7.0, (0, 0, 0), 0), "move": (3.0, (0, 0, 1.0), 1.0),
                         "turn": (3.0, (0.6, 0, 0.7), 0.8), "climb": (3.0, (0, -0.7, 0.5), 0.6)}.items():
    export_geo(os.path.join(OUT, f"_pose_{pname}.geo.json"), make_pose(t, g, m))

ncube = sum(len(b["cubes"]) for b in bones)
print("tex", TEX_W, TEX_H, "slots", len(slots), "bones", len(bones), "cubes", ncube, "rig", len(rig), "anim bones", len(anims))
