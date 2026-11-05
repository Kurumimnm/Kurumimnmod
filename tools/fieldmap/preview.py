"""
フィールドマップ（first_continent.json）を上から見た画像にするスクリプト。
地図 JSON を書き換えたあと、ゲームを起動せずに形を確認するために使う。

使い方（リポジトリのルートで）:
    pip install numpy matplotlib
    python tools/fieldmap/preview.py
→ docs/world/field_map_preview.png ができる

※ 境界のゆがみ（warp）の計算は FieldNoise.java / FieldMap.java と同じ式。どちらかを変えたら両方そろえること。
"""
import json, math, os, sys
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MAP = os.path.join(ROOT, "src/main/resources/data/kurumimnmod/kurumimnmod/field_map/first_continent.json")
OUT = os.path.join(ROOT, "docs/world/field_map_preview.png")
STEP = 48  # 何ブロックごとに色を塗るか（小さいほど細かいが遅い）

M = (1 << 64) - 1
def _hash(ix, iz, salt):
    h = (ix * 0x9E3779B97F4A7C15 + iz * 0xC2B2AE3D27D4EB4F + salt * 0x165667B19E3779F9) & M
    h = ((h ^ (h >> 31)) * 0xBF58476D1CE4E5B9) & M
    h = ((h ^ (h >> 29)) * 0x94D049BB133111EB) & M
    h = h ^ (h >> 32)
    return ((h >> 11) * (2.0 ** -53)) * 2 - 1
def _value(x, z, salt):
    ix, iz = math.floor(x), math.floor(z); fx, fz = x - ix, z - iz
    sx, sz = fx * fx * (3 - 2 * fx), fz * fz * (3 - 2 * fz)
    a, b = _hash(ix, iz, salt), _hash(ix + 1, iz, salt)
    c, d = _hash(ix, iz + 1, salt), _hash(ix + 1, iz + 1, salt)
    top = a + (b - a) * sx; bottom = c + (d - c) * sx
    return top + (bottom - top) * sz
def fbm(x, z, salt):
    return _value(x, z, salt) * 0.65 + _value(x * 2.3, z * 2.3, salt + 7) * 0.35

def main():
    data = json.load(open(MAP, encoding="utf-8"))
    amp, scale = data["warp"]["amplitude"], data["warp"]["scale"]
    seeds = [(a["x"], a["z"], f["number"], a["area"]) for f in data["fields"] for a in f["areas"]]
    xs_all = [s[0] for s in seeds] + [t["x"] for t in data["towns"]]
    zs_all = [s[1] for s in seeds] + [t["z"] for t in data["towns"]]
    pad = 1500
    xs = np.arange(min(xs_all) - pad, max(xs_all) + pad + 1, STEP)
    zs = np.arange(min(zs_all) - pad, max(zs_all) + pad + 1, STEP)
    W = np.zeros((len(zs), len(xs), 2))
    for j, z in enumerate(zs):
        for i, x in enumerate(xs):
            W[j, i] = (x + amp * fbm(x / scale, z / scale, 101), z + amp * fbm(x / scale, z / scale, 202))
    S = np.array([[s[0], s[1]] for s in seeds])
    idx = ((W[:, :, None, 0] - S[None, None, :, 0]) ** 2 + (W[:, :, None, 1] - S[None, None, :, 1]) ** 2).argmin(axis=2)
    field = np.array([s[2] for s in seeds])[idx]
    area = np.array([s[3] for s in seeds])[idx]
    rng = np.random.default_rng(1)
    colors = {f["number"]: rng.uniform(0.35, 0.9, 3) for f in data["fields"]}
    img = np.zeros((len(zs), len(xs), 3))
    for n, c in colors.items():
        img[field == n] = c
    img *= (1 - 0.13 * (area - 1))[:, :, None]  # 深いエリアほど暗く
    edge = (field[1:, 1:] != field[:-1, 1:]) | (field[1:, 1:] != field[1:, :-1])
    img[1:, 1:][edge] = 0.15
    fig, ax = plt.subplots(figsize=(7.5, 7.5 * len(zs) / len(xs)), dpi=110)
    ax.imshow(img, extent=[xs[0], xs[-1], zs[-1], zs[0]], interpolation="nearest")
    for t in data["towns"]:
        ax.plot(t["x"], t["z"], "o", ms=9, mfc="#1d9e75", mec="black")
        ax.text(t["x"], t["z"], "M" if t["number"] == 0 else str(t["number"]), ha="center", va="center", fontsize=6, color="white")
    for f in data["fields"]:
        main_area = [a for a in f["areas"] if a["area"] == 1] or f["areas"]
        cx = np.mean([a["x"] for a in main_area]); cz = np.mean([a["z"] for a in main_area])
        ax.text(cx + 180, cz, str(f["number"]), fontsize=7, bbox=dict(boxstyle="round,pad=.15", fc="white", ec="none", alpha=.75))
    ax.set_xlabel("X (east ->)"); ax.set_ylabel("Z (north = up)")
    ax.set_title("Kurumimnmod field map (darker = deeper area)")
    plt.tight_layout(); plt.savefig(OUT)
    print("saved:", OUT)

if __name__ == "__main__":
    sys.exit(main())
