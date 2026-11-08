import os, glob, zlib, io, struct, math, base64
import nbtlib, numpy as np
import matplotlib; matplotlib.use("Agg"); import matplotlib.pyplot as plt
# 現世（平らな地形）とクエスト用ディメンションのリージョン
DIRS = {"ow": "run/world/dimensions/minecraft/overworld/region", "quest": "run/world/dimensions/kurumimnmod/quest/region"}
def chunk_nbt(path, lx, lz):
    with open(path, "rb") as f:
        f.seek(4 * (lx + lz * 32)); off = f.read(4)
        o = (off[0] << 16 | off[1] << 8 | off[2]) * 4096
        if o == 0: return None
        f.seek(o); ln = struct.unpack(">I", f.read(4))[0]; f.read(1)
        return nbtlib.File.parse(io.BytesIO(zlib.decompress(f.read(ln - 1))))
def pname(p):
    if not hasattr(p, "get"): return str(p)
    for k in ("id", "Name", "name", "block", ""):
        if k in p: return str(p[k])
    return "?" + repr(p)[:60]
top = {}
DEBUG = {("ow", 0, 6), ("quest", 0, 4096 + 96 - 16), ("quest", 0, 4096 - 96 + 16), ("quest", 8192, 4096), ("quest", 0, -14), ("quest", 4096, 4096)}
AIRS = ("minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:barrier", "")
for dim, reg in DIRS.items():
    files = glob.glob(os.path.join(reg, "r.*.mca"))
    print("REGION", dim, reg, len(files))
    for path in files:
        rx, rz = map(int, os.path.basename(path).split(".")[1:3])
        for lz in range(32):
            for lx in range(32):
                c = chunk_nbt(path, lx, lz)
                if c is None or "full" not in str(c.get("Status", "")): continue
                cx, cz = rx * 32 + lx, rz * 32 + lz
                secs = {}
                for sec in c.get("sections", []):
                    bs = sec.get("block_states")
                    if not hasattr(bs, "get"): continue
                    pal = [pname(p) for p in bs.get("palette", [])]
                    data = [int(x) & ((1 << 64) - 1) for x in bs.get("data", [])]
                    secs[int(sec["Y"])] = (pal, data)
                def block_at(sy, idx):
                    pal, data = secs[sy]
                    if len(pal) == 1: return pal[0]
                    bits = max(4, math.ceil(math.log2(len(pal)))); per = 64 // bits
                    return pal[(data[idx // per] >> (bits * (idx % per))) & ((1 << bits) - 1)]
                for i in range(256):
                    x, z = i % 16, i // 16
                    found = None
                    for sy in sorted(secs, reverse=True):
                        for ly in range(15, -1, -1):
                            n = block_at(sy, ly * 256 + z * 16 + x)
                            if n not in AIRS:
                                found = (n, sy * 16 + ly); break
                        if found: break
                    wx, wz = cx * 16 + x, cz * 16 + z
                    if found: top[(dim, wx, wz)] = found
                    if (dim, wx, wz) in DEBUG:
                        col = []
                        for yy in range(-64, 320):
                            if (yy >> 4) in secs:
                                n = block_at(yy >> 4, (yy & 15) * 256 + z * 16 + x).replace("minecraft:", "")
                                if n not in ("air",): col.append("%d:%s" % (yy, n))
                        print("COLUMN", dim, wx, wz, " ".join(col[-25:]))
print("columns", len(top))
COL = [("quest_gate", (200, 60, 255)), ("water", (60, 100, 220)), ("glass", (180, 220, 240)), ("lantern", (255, 210, 80)),
       ("calcite", (235, 235, 228)), ("deepslate", (60, 60, 66)), ("blackstone", (40, 36, 44)), ("stone_brick", (130, 130, 130)),
       ("andesite", (150, 152, 150)), ("cobble", (110, 110, 110)), ("stair", (90, 70, 50)), ("planks", (170, 130, 80)),
       ("log", (100, 80, 50)), ("sandstone", (220, 205, 150)), ("tiles", (60, 60, 66)), ("wool", (200, 60, 60)),
       ("path", (160, 140, 90)), ("grass_block", (100, 160, 70)), ("short_grass", (110, 170, 75)), ("tall_grass", (110, 170, 75)),
       ("sand", (220, 210, 160)), ("snow", (240, 240, 245)), ("leaves", (50, 110, 40)), ("fence", (140, 110, 70)),
       ("campfire", (255, 120, 40)), ("slab", (120, 120, 120)), ("stone", (125, 125, 125)), ("dirt", (130, 95, 60)),
       ("flower", (230, 80, 160)), ("dandelion", (240, 220, 40)), ("poppy", (220, 40, 40)), ("bush", (120, 100, 60)),
       ("cactus", (60, 130, 50)), ("terracotta", (170, 90, 60)), ("ice", (160, 190, 240))]
def color(n):
    for k, c in COL:
        if k in n: return c
    return (200, 0, 200)
AREAS = [("start_town", "ow", 0, 0, 90), ("hub", "quest", 0, 0, 62),
         ("field_meadow", "quest", 0, 4096, 100), ("field_mountains", "quest", 4096, 4096, 164),
         ("arena", "quest", 8192, 4096, 52), ("field_dunes", "quest", 12288, 4096, 164), ("field_meadow2", "quest", 16384, 4096, 100)]
os.makedirs("probe_out", exist_ok=True)
for name, dim, cx, cz, r in AREAS:
    img = np.zeros((2 * r, 2 * r, 3), dtype=np.uint8); hs = []; names = {}
    hmap = np.full((2 * r, 2 * r), np.nan)
    for dz in range(-r, r):
        for dx in range(-r, r):
            t = top.get((dim, cx + dx, cz + dz))
            if t:
                img[dz + r, dx + r] = color(t[0]); hs.append(t[1]); hmap[dz + r, dx + r] = t[1]
                names[t[0]] = names.get(t[0], 0) + 1
    # 陰影（高さの差）
    if hs:
        gy, gx = np.gradient(np.nan_to_num(hmap, nan=np.nanmin(hmap)))
        shade = np.clip(1 + (-gx - gy) * 0.08, 0.6, 1.3)
        img = np.clip(img * shade[..., None], 0, 255).astype(np.uint8)
    scale = 3 if r <= 100 else 2
    plt.imsave(f"probe_out/{name}.png", np.kron(img, np.ones((scale, scale, 1), dtype=np.uint8)))
    print("AREA", name, "height range", (min(hs), max(hs)) if hs else None, sorted(names.items(), key=lambda k: -k[1])[:14])
    print("PNG_" + name, base64.b64encode(open(f"probe_out/{name}.png", "rb").read()).decode())
