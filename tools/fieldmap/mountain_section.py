"""
山脈の断面図を作るスクリプト（ゲームを起動せずに山の形を確認する用）。
地図 JSON の mountains を読み、MountainSettings.java と同じ計算で高さを出す。

使い方（リポジトリのルートで）:
    pip install numpy matplotlib
    python tools/fieldmap/mountain_section.py
→ docs/world/mountain_section.png ができる（黒＝手前の断面、灰色＝少し奥の断面）

※ 式を変えるときは MountainSettings.java / TerrainSettings.java と両方そろえること。
"""
import json, math, os, sys
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from preview import fbm, MAP, ROOT

OUT = os.path.join(ROOT, "docs/world/mountain_section.png")
data = json.load(open(MAP, encoding="utf-8"))
T, MT = data["terrain"], data["mountains"]

def smooth(t):
    t = min(1.0, max(0.0, t)); return t * t * (3 - 2 * t)

def ground(x, z):
    h = T["base_height"]
    for i, o in enumerate(T.get("noise", [])):
        h += o["amplitude"] * fbm(x / o["scale"], z / o["scale"], 505 + i)
    return h

def position(x, z):
    ja, js = MT["jitter"]["amplitude"], MT["jitter"]["scale"]
    jx = x + ja * fbm(x / js, z / js, 303); jz = z + ja * fbm(x / js, z / js, 404)
    best_p, best_side = 0.0, 1
    for r in MT["ridges"]:
        pts = [(p["x"], p["z"]) for p in r["points"]]
        segs = list(zip(pts, pts[1:])) + ([(pts[-1], pts[0])] if r.get("closed") else [])
        dmin, side = float("inf"), 1
        for a, b in segs:
            vx, vz = b[0] - a[0], b[1] - a[1]; l2 = vx * vx + vz * vz
            t = 0 if l2 == 0 else max(0, min(1, ((jx - a[0]) * vx + (jz - a[1]) * vz) / l2))
            d = math.hypot(jx - (a[0] + vx * t), jz - (a[1] + vz * t))
            if d < dmin:
                dmin = d; side = 1 if vx * (jz - a[1]) - vz * (jx - a[0]) >= 0 else -1
        p = 1 - dmin / r["width"]
        if p > best_p: best_p, best_side = p, side
    return best_p, best_side

def mountain(p, side, x, z):
    if p <= 0: return 0.0
    fw, fr = MT["foot_wall"], MT.get("foot_ratio", 0.02)
    wall = fw["min"] + (fw["max"] - fw["min"]) * smooth(0.5 + 0.8 * fbm(x / 350, z / 350, 808))
    if p < fr: return wall * smooth(p / fr)
    u = side * (1 - p); u0 = MT.get("crest_shift", 0) * fbm(x / 600, z / 600, 707)
    q = (1 - u) / (1 - u0) if u >= u0 else (u + 1) / (1 + u0)
    q = min(1, max(0, (q - fr) / (1 - fr)))
    pk = MT["peak"]
    top = pk["min"] + (pk["max"] - pk["min"]) * smooth(0.5 + 0.9 * fbm(x / pk["scale"], z / pk["scale"], 1212))
    h = wall + (top - wall) * min(1, q / 0.94)
    sh = MT["shelves"]
    w = smooth((fbm(x / sh["scale"], z / sh["scale"], 1414) - (1 - 2 * sh["coverage"])) / 0.25)
    level = wall + (top - wall) * (sh["level"] + 0.15 * fbm(x / 400, z / 400, 1616))
    flat = 0.0
    if w > 0 and level < h < level + sh["band"]:
        h += (level - h) * w; flat = w
    rough = sum(o["amplitude"] * fbm(x / o["scale"], z / o["scale"], 1515 + 2 * i) for i, o in enumerate(MT.get("roughness", [])))
    return h + rough * min(1, q * 3) * (1 - flat)

def height(x, z):
    p, s = position(x, z)
    return min(ground(x, z) + mountain(p, s, x, z), T["max_height"])

def main():
    lines = [  # (タイトル, 断面の向き, 固定する座標, 範囲, 奥へのずらし)
        ("メディアナを囲む山（西側・東西方向の断面）", "x", 0, (-1400, -600), 300),
        ("大陸を東西に横切る山脈（南北方向の断面）", "z", 3000, (600, 1400), 400),
        ("大陸を東西に横切る山脈（別の場所）", "z", -5000, (600, 1400), 400),
    ]
    fig, ax = plt.subplots(len(lines), 1, figsize=(10, 3.4 * len(lines)))
    for a, (title, axis, fixed, rng, depth) in zip(ax, lines):
        s = np.arange(rng[0], rng[1], 1)
        for off, color in ((0, "#111"), (depth, "#999")):
            hs = [height(v, fixed + off) if axis == "x" else height(fixed + off, v) for v in s]
            a.plot(s, hs, color=color, lw=1.6)
        a.set_title(title, fontname="Noto Sans CJK JP" if os.name != "nt" else "Meiryo")
        a.set_ylim(60, 450); a.set_aspect("equal"); a.grid(alpha=.3)
        a.axhline(192.33, color="#5b9bd5", ls="--", lw=.8)
    plt.tight_layout(); plt.savefig(OUT, dpi=90); print("saved:", OUT)

if __name__ == "__main__":
    main()
