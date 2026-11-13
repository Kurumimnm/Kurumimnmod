"""geo.json を GeckoLib と同じ変換で組み立て、テクスチャ付きで簡易レンダリングする（形の確認用）。"""
import json, math, sys, os
import numpy as np
from PIL import Image, ImageDraw

geo_path, tex_path, glow_path, out_path = sys.argv[1:5]
view = sys.argv[5] if len(sys.argv) > 5 else "front"
RENDER_SCALE = float(os.environ.get('RS', '8'))  # ModConstants.ASTRA_VEIL_RENDER_SCALE と合わせる
HOVER = float(sys.argv[6]) if len(sys.argv) > 6 else 52.0  # ModConstants.ASTRA_VEIL_SPAWN_HOVER_HEIGHT

geo = json.load(open(geo_path))["minecraft:geometry"][0]
TW, TH = geo["description"]["texture_width"], geo["description"]["texture_height"]
tex = np.asarray(Image.open(tex_path).convert("RGBA")).astype(np.float32) / 255
glow = np.asarray(Image.open(glow_path).convert("RGBA")).astype(np.float32) / 255

def T(v):
    m = np.eye(4); m[:3, 3] = v; return m
def Rx(a):
    c, s = math.cos(a), math.sin(a); m = np.eye(4); m[1, 1] = c; m[1, 2] = -s; m[2, 1] = s; m[2, 2] = c; return m
def Ry(a):
    c, s = math.cos(a), math.sin(a); m = np.eye(4); m[0, 0] = c; m[0, 2] = s; m[2, 0] = -s; m[2, 2] = c; return m
def Rz(a):
    c, s = math.cos(a), math.sin(a); m = np.eye(4); m[0, 0] = c; m[0, 1] = -s; m[1, 0] = s; m[1, 1] = c; return m
def S(k):
    m = np.eye(4); m[0, 0] = m[1, 1] = m[2, 2] = k; return m

def rot_about(p, r):
    p = np.array([-p[0], p[1], p[2]]) / 16
    rx, ry, rz = [math.radians(v) for v in r]
    return T(p) @ Rz(rz) @ Ry(-ry) @ Rx(-rx) @ T(-p)

bones = {b["name"]: b for b in geo["bones"]}
world = {}
def bone_mat(name):
    if name in world:
        return world[name]
    b = bones[name]
    parent = bone_mat(b["parent"]) if "parent" in b else np.eye(4)
    m = parent @ rot_about(b["pivot"], b.get("rotation", [0, 0, 0]))
    world[name] = m
    return m

# エンティティの描画：Y 軸 180 度回転（yRot = 0）→ 10 倍 → 浮遊の高さ
VIEWS = {"front": (0, 0), "below": (18, -14), "side": (90, 0), "player": (25, -38), "under": (0, -80), "top": (0, 80)}
yaw, pitch = VIEWS[view]
ent = T([0, HOVER, 0]) @ S(RENDER_SCALE) @ Ry(math.pi)
cam = Rx(math.radians(pitch)) @ Ry(math.radians(yaw))
faces = []
for name, b in bones.items():
    M = bone_mat(name)
    for c in b.get("cubes", []):
        ox, oy, oz = c["origin"]; sx, sy, sz = c["size"]
        x0, x1 = -(ox + sx) / 16, -ox / 16
        y0, y1 = oy / 16, (oy + sy) / 16
        z0, z1 = oz / 16, (oz + sz) / 16
        CM = M
        if "rotation" in c:
            CM = M @ rot_about(c["pivot"], c["rotation"])
        u, v = c["uv"]
        w, h, d = [int(math.ceil(q)) for q in (sx, sy, sz)]
        R = {"up": (u + d, v, w, d), "down": (u + d + w, v, w, d), "east": (u, v + d, d, h),
             "north": (u + d, v + d, w, h), "west": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h)}
        # 各面：4 頂点（左上・右上・右下・左下の順に UV の角へ対応）
        quads = {
            "north": [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
            "south": [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
            "up": [(x1, y1, z1), (x0, y1, z1), (x0, y1, z0), (x1, y1, z0)],
            "down": [(x1, y0, z0), (x0, y0, z0), (x0, y0, z1), (x1, y0, z1)],
            "east": [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
            "west": [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
        }
        for fn, q in quads.items():
            P = np.array([[*p, 1] for p in q]).T
            W = (cam @ ent @ CM @ P)[:3].T
            faces.append((W, R[fn]))

ALL = np.concatenate([f[0] for f in faces])
W_IMG, H_IMG = 1600, 1100
xmin, xmax = ALL[:, 0].min(), ALL[:, 0].max()
ymin, ymax = min(ALL[:, 1].min(), 0), ALL[:, 1].max()
pad = 40
k = min((W_IMG - 2 * pad) / (xmax - xmin), (H_IMG - 2 * pad) / (ymax - ymin))
cx = (xmin + xmax) / 2
def proj(W):
    return np.stack([W_IMG / 2 + (W[:, 0] - cx) * k, H_IMG - pad - (W[:, 1] - ymin) * k], 1)

# 背景（空のグラデーション）
img = np.zeros((H_IMG, W_IMG, 3), dtype=np.float32)
top, bot = np.array([0.16, 0.14, 0.38]), np.array([0.62, 0.62, 0.86])
for y in range(H_IMG):
    img[y] = top + (bot - top) * (y / H_IMG)
rng = np.random.default_rng(1)
for _ in range(220):
    x, y = rng.integers(0, W_IMG), rng.integers(0, H_IMG // 2)
    img[y, x] = [1, 1, 1]

# 奥から手前へ（カメラは +Z 側から -Z を見ている → z が小さいほど奥）
gbuf = np.zeros((H_IMG, W_IMG, 3), dtype=np.float32)
LIGHT = np.array([0.3, 0.8, 0.5]); LIGHT /= np.linalg.norm(LIGHT)
faces.sort(key=lambda f: f[0][:, 2].mean())
for W, (u, v, w, h) in faces:
    nrm = np.cross(W[1] - W[0], W[3] - W[0]); nn = np.linalg.norm(nrm)
    shade = 0.62 + 0.38 * abs(float(np.dot(nrm / nn, LIGHT))) if nn > 1e-9 else 0.8
    if w <= 0 or h <= 0:
        continue
    P2 = proj(W)
    uvs = np.array([[u, v], [u + w, v], [u + w, v + h], [u, v + h]], dtype=np.float32)
    for tri in ((0, 1, 2), (0, 2, 3)):
        p = P2[list(tri)]; t = uvs[list(tri)]
        x0, y0 = np.floor(p.min(0)).astype(int); x1, y1 = np.ceil(p.max(0)).astype(int)
        x0, y0 = max(x0, 0), max(y0, 0); x1, y1 = min(x1, W_IMG - 1), min(y1, H_IMG - 1)
        if x1 < x0 or y1 < y0:
            continue
        den = (p[1, 1] - p[2, 1]) * (p[0, 0] - p[2, 0]) + (p[2, 0] - p[1, 0]) * (p[0, 1] - p[2, 1])
        if abs(den) < 1e-9:
            continue
        xs, ys = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
        a = ((p[1, 1] - p[2, 1]) * (xs - p[2, 0]) + (p[2, 0] - p[1, 0]) * (ys - p[2, 1])) / den
        bb = ((p[2, 1] - p[0, 1]) * (xs - p[2, 0]) + (p[0, 0] - p[2, 0]) * (ys - p[2, 1])) / den
        c = 1 - a - bb
        inside = (a >= -1e-4) & (bb >= -1e-4) & (c >= -1e-4)
        if not inside.any():
            continue
        uu = a * t[0, 0] + bb * t[1, 0] + c * t[2, 0]
        vv = a * t[0, 1] + bb * t[1, 1] + c * t[2, 1]
        ui = np.clip(uu.astype(int), u, u + w - 1); vi = np.clip(vv.astype(int), v, v + h - 1)
        col = tex[vi, ui]; gl = glow[vi, ui]
        alpha = col[..., 3:4] * inside[..., None]
        region = img[y0:y1 + 1, x0:x1 + 1]
        region[:] = region * (1 - alpha) + col[..., :3] * shade * alpha
        ga = gl[..., 3:4] * inside[..., None]
        region[:] = np.clip(region + gl[..., :3] * ga * 0.35, 0, 1)
        gb = gbuf[y0:y1 + 1, x0:x1 + 1]
        gb[:] = np.maximum(gb, gl[..., :3] * ga)

from PIL import ImageFilter
bloom = np.asarray(Image.fromarray((gbuf * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(6))).astype(np.float32) / 255
img = np.clip(img + bloom * 0.7, 0, 1)
out = Image.fromarray((img * 255).astype(np.uint8))
d = ImageDraw.Draw(out)
# 地面とプレイヤー（大きさの比較用。高さ 1.8 ブロック）
gy = H_IMG - pad - (0 - ymin) * k
d.line([(0, gy), (W_IMG, gy)], fill=(40, 90, 60), width=3)
px = W_IMG / 2 + 25 * k
d.rectangle([px - 0.3 * k, gy - 1.8 * k, px + 0.3 * k, gy], fill=(20, 20, 20))
out.save(out_path)
print("bounds(world blocks): x", round(xmin, 1), round(xmax, 1), "y", round(ALL[:, 1].min(), 1), round(ymax, 1))
