"""geo.json を GeckoLib と同じ変換で組み立て、テクスチャ付きで簡易レンダリングする（形の確認用）。
使い方: python3 preview.py <geo.json> <texture.png> <glowmask.png> <out.png> [view ...]
  view: front / back / side / top / under / player（地上から見上げ）/ quarter。複数指定すると横に並べる。
  generate.py が出す _pose_*.geo.json（待機・移動・旋回の姿勢）もそのまま描ける。"""
import json, math, sys, os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

geo_path, tex_path, glow_path, out_path = sys.argv[1:5]
views = sys.argv[5:] or ["front"]
RENDER_SCALE = float(os.environ.get("RS", "8"))      # ModConstants.ASTRA_VEIL_RENDER_SCALE
HOVER = float(os.environ.get("HOVER", "150"))        # 地面からエンティティの位置までの高さ（ブロック）
W_IMG, H_IMG = int(os.environ.get("W", "1500")), int(os.environ.get("H", "1100"))

geo = json.load(open(geo_path))["minecraft:geometry"][0]
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
    off = np.array(b.get("_offset", [0, 0, 0])) / 16
    m = parent @ T(off) @ rot_about(b["pivot"], b.get("rotation", [0, 0, 0]))
    world[name] = m
    return m

# 面を集める（モデル空間 → エンティティ：Y 軸 180 度（yRot = 0）→ 拡大 → 浮かぶ高さ）
ent = T([0, HOVER, 0]) @ S(RENDER_SCALE) @ Ry(math.pi)
quads_all = []
for name, b in bones.items():
    M = bone_mat(name)
    for c in b.get("cubes", []):
        ox, oy, oz = c["origin"]; sx, sy, sz = c["size"]
        x0, x1 = -(ox + sx) / 16, -ox / 16
        y0, y1 = oy / 16, (oy + sy) / 16
        z0, z1 = oz / 16, (oz + sz) / 16
        CM = M @ rot_about(c["pivot"], c["rotation"]) if "rotation" in c else M
        u, v = c["uv"]; w, h, d = int(sx), int(sy), int(sz)
        V = {"blb": (x0, y0, z0), "brb": (x0, y0, z1), "tlb": (x0, y1, z0), "trb": (x0, y1, z1),
             "tlf": (x1, y1, z0), "trf": (x1, y1, z1), "blf": (x1, y0, z0), "brf": (x1, y0, z1)}
        quads = {"west": ["trb", "tlb", "blb", "brb"], "east": ["tlf", "trf", "brf", "blf"],
                 "north": ["tlb", "tlf", "blf", "blb"], "south": ["trf", "trb", "brb", "brf"],
                 "up": ["trb", "trf", "tlf", "tlb"], "down": ["blb", "blf", "brf", "brb"]}
        R = {"west": (u + d + w, v + d, d, h), "east": (u, v + d, d, h), "north": (u + d, v + d, w, h),
             "south": (u + 2 * d + w, v + d, w, h), "up": (u + d, v, w, d), "down": (u + d + w, v + d, w, -d)}
        for fn, names in quads.items():
            ru, rv, rw, rh = R[fn]
            if rw == 0 or rh == 0:
                continue
            ya, yb = sorted((rv, rv + rh))
            if tex[ya:yb, ru:ru + rw, 3].max() <= 0.02:
                continue
            uv4 = [(ru + rw, rv), (ru, rv), (ru, rv + rh), (ru + rw, rv + rh)]
            P = np.array([[*V[n], 1] for n in names]).T
            quads_all.append(((ent @ CM @ P)[:3].T, uv4))
ALLW = np.concatenate([q[0] for q in quads_all])
print("bounds(world blocks): x", round(ALLW[:, 0].min(), 1), round(ALLW[:, 0].max(), 1), "y", round(ALLW[:, 1].min(), 1), round(ALLW[:, 1].max(), 1),
      "z", round(ALLW[:, 2].min(), 1), round(ALLW[:, 2].max(), 1), "quads", len(quads_all))

# 視点：カメラは +Z 側から -Z を見る。エンティティは yRot = 0（南 +Z 向き）なので、front = 正面から
VIEWS = {"front": (0, 0), "back": (180, 0), "side": (90, 0), "player": (20, -40), "under": (0, -89), "top": (0, 89), "quarter": (35, -18)}
def render(view):
    yaw, pitch = VIEWS[view]
    cam = Rx(math.radians(pitch))[:3, :3] @ Ry(math.radians(yaw))[:3, :3]
    faces = [(W @ cam.T, uv4) for W, uv4 in quads_all]
    ALL = np.concatenate([f[0] for f in faces])
    xmin, xmax = ALL[:, 0].min(), ALL[:, 0].max(); ymin, ymax = ALL[:, 1].min(), ALL[:, 1].max()
    pad = 30
    k = min((W_IMG - 2 * pad) / (xmax - xmin), (H_IMG - 2 * pad) / (ymax - ymin))
    cx, cy = (xmin + xmax) / 2, (ymin + ymax) / 2
    img = np.zeros((H_IMG, W_IMG, 3), np.float32)
    top, bot = np.array([0.03, 0.03, 0.12]), np.array([0.10, 0.09, 0.26])
    img[:] = top + (bot - top) * (np.arange(H_IMG)[:, None, None] / H_IMG)
    gbuf = np.zeros((H_IMG, W_IMG, 3), np.float32)
    LIGHT = np.array([0.3, 0.8, 0.5]); LIGHT /= np.linalg.norm(LIGHT)
    faces.sort(key=lambda f: f[0][:, 2].mean())
    for W, uv4 in faces:
        nrm = np.cross(W[1] - W[0], W[3] - W[0]); nn = np.linalg.norm(nrm)
        if nn < 1e-9 or nrm[2] <= 0:          # 裏向きの面は描かない（ゲームと同じ）
            continue
        shade = 0.66 + 0.34 * abs(float(np.dot(nrm / nn, LIGHT)))
        P2 = np.stack([W_IMG / 2 + (W[:, 0] - cx) * k, H_IMG / 2 - (W[:, 1] - cy) * k], 1)
        uvs = np.array(uv4, np.float32)
        umin, vmin = uvs.min(0).astype(int); umax, vmax = uvs.max(0).astype(int)
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
            uu = a * t[0, 0] + bb * t[1, 0] + c * t[2, 0]; vv = a * t[0, 1] + bb * t[1, 1] + c * t[2, 1]
            ui = np.clip(np.floor(uu).astype(int), umin, umax - 1); vi = np.clip(np.floor(vv).astype(int), vmin, vmax - 1)
            col = tex[vi, ui]; gl = glow[vi, ui]
            alpha = col[..., 3:4] * inside[..., None]
            region = img[y0:y1 + 1, x0:x1 + 1]
            region[:] = region * (1 - alpha) + col[..., :3] * shade * alpha
            ga = gl[..., 3:4] * inside[..., None]
            region[:] = np.clip(region + gl[..., :3] * ga * 0.45, 0, 1)
            gb = gbuf[y0:y1 + 1, x0:x1 + 1]
            gb[:] = np.maximum(gb * (1 - alpha * 0.85), gl[..., :3] * ga)
    bloom = np.asarray(Image.fromarray((gbuf * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(4))).astype(np.float32) / 255
    img = np.clip(img + bloom * 0.6, 0, 1)
    out = Image.fromarray((img * 255).astype(np.uint8))
    d = ImageDraw.Draw(out)
    d.text((10, 8), f"{view}  {xmax - xmin:.0f} x {ymax - ymin:.0f} blocks", fill=(230, 230, 255))
    # 物差し：16 ブロック（1 チャンク）ごとの目盛りと、プレイヤー（1.8 ブロック）
    for i in range(int((xmax - xmin) // 16) + 1):
        xx = W_IMG / 2 + (xmin + i * 16 - cx) * k
        d.line([(xx, H_IMG - 12), (xx, H_IMG - (6 if i % 4 else 2))], fill=(200, 200, 230))
    d.rectangle([W_IMG - 20 - 0.6 * k, H_IMG - 20 - 1.8 * k, W_IMG - 20, H_IMG - 20], fill=(255, 255, 255))
    return out
imgs = [render(v) for v in views]
sheet = Image.new("RGB", (W_IMG * len(imgs), H_IMG))
for i, im in enumerate(imgs):
    sheet.paste(im, (i * W_IMG, 0))
sheet.save(out_path)
