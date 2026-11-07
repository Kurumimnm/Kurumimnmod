
import os, glob, zlib, io, struct, math, base64
import nbtlib, numpy as np
import matplotlib; matplotlib.use("Agg"); import matplotlib.pyplot as plt
REG = [r for r in glob.glob("run/world/**/region", recursive=True) if "overworld" in r.replace("\\","/") or r.endswith("world/region")]
reg = REG[0]
def chunk_nbt(path, lx, lz):
    with open(path, "rb") as f:
        f.seek(4 * (lx + lz * 32)); off = f.read(4)
        o = (off[0] << 16 | off[1] << 8 | off[2]) * 4096
        if o == 0: return None
        f.seek(o); ln = struct.unpack(">I", f.read(4))[0]; f.read(1)
        return nbtlib.File.parse(io.BytesIO(zlib.decompress(f.read(ln - 1))))
SEEN = set()
def pname(p):
    if not hasattr(p, "get"): return str(p)
    for k in ("id", "Name", "name", "block", ""):
        if k in p: return str(p[k])
    key = tuple(sorted(p.keys()))
    if key not in SEEN:
        SEEN.add(key); print("PALETTE_KEYS", key, repr(p)[:200])
    return "?"
top = {}
SECTION_HITS = {}
DEBUG = {(20, 20), (-30, 25), (20, -5990), (50, -6050), (40, -9040), (8, -3030)}
for path in glob.glob(os.path.join(reg, "r.*.mca")):
    rx, rz = map(int, os.path.basename(path).split(".")[1:3])
    for lz in range(32):
        for lx in range(32):
            c = chunk_nbt(path, lx, lz)
            if c is None or "full" not in str(c.get("Status", "")): continue
            cx, cz = rx * 32 + lx, rz * 32 + lz
            vals = []
            for v in c["Heightmaps"]["WORLD_SURFACE"]:
                v = int(v) & ((1 << 64) - 1)
                for k in range(6): vals.append((v >> (10 * k)) & 1023)
            secs = {}
            for sec in c.get("sections", []):
                bs = sec.get("block_states")
                if hasattr(bs, "get"):
                    for p in bs.get("palette", []):
                        n = pname(p)
                        if any(k in n for k in ("_ore", "andesite", "granite", "diorite", "tuff", "gravel", "lava", "amethyst", "spawner", "pumpkin", "poppy", "dandelion", "mushroom", "dirt", "clay", "water")):
                            SECTION_HITS[n] = SECTION_HITS.get(n, 0) + 1
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
            AIRS = ("minecraft:air", "minecraft:cave_air", "minecraft:void_air", "")
            for i in range(256):
                x, z = i % 16, i // 16
                found = None
                for sy in sorted(secs, reverse=True):
                    for ly in range(15, -1, -1):
                        n = block_at(sy, ly * 256 + z * 16 + x)
                        if n not in AIRS:
                            found = (n, sy * 16 + ly); break
                    if found: break
                if found: top[(cx * 16 + x, cz * 16 + z)] = found
                wx, wz = cx * 16 + x, cz * 16 + z
                if (wx, wz) in DEBUG:
                    col = []
                    for yy in range(60, 100):
                        if (yy >> 4) in secs: col.append("%d:%s" % (yy, block_at(yy >> 4, (yy & 15) * 256 + z * 16 + x).replace("minecraft:", "")))
                    print("COLUMN", wx, wz, " ".join(col))
print("columns", len(top))
print("SECTIONS_WITH", sorted(SECTION_HITS.items(), key=lambda k: -k[1]))
COL = [("water", (60, 100, 220)), ("glass", (180, 220, 240)), ("lantern", (255, 210, 80)), ("glowstone", (255, 230, 120)),
       ("quartz", (240, 236, 228)), ("deepslate", (60, 60, 66)), ("stone_brick", (130, 130, 130)), ("andesite", (150, 152, 150)),
       ("cobble", (110, 110, 110)), ("stair", (90, 70, 50)), ("planks", (170, 130, 80)), ("log", (100, 80, 50)),
       ("sandstone", (220, 205, 150)), ("mud", (140, 110, 90)), ("tiles", (60, 60, 66)), ("wool", (200, 60, 60)),
       ("farmland", (120, 80, 50)), ("wheat", (200, 190, 80)), ("path", (160, 140, 90)), ("grass", (100, 160, 70)),
       ("sand", (220, 210, 160)), ("snow", (240, 240, 245)), ("leaves", (50, 110, 40)), ("fence", (140, 110, 70)),
       ("hay", (210, 180, 60)), ("target", (230, 220, 210)), ("campfire", (255, 120, 40)), ("slab", (120, 120, 120))]
def color(n):
    for k, c in COL:
        if k in n: return c
    return (200, 0, 200)
AREAS = [("mediana", 0, 0, 250), ("gorge", 0, -972, 126), ("town1", 0, -3000, 90)]
for name, cx, cz, r in AREAS:
    img = np.zeros((2 * r, 2 * r, 3), dtype=np.uint8); hs = []
    for dz in range(-r, r):
        for dx in range(-r, r):
            t = top.get((cx + dx, cz + dz))
            if t: img[dz + r, dx + r] = color(t[0]); hs.append(t[1])
    plt.imsave(f"/tmp/{name}.png", np.kron(img, np.ones((3, 3, 1), dtype=np.uint8)))
    names = {}
    for dz in range(-r, r):
        for dx in range(-r, r):
            t = top.get((cx + dx, cz + dz))
            if t: names[t[0]] = names.get(t[0], 0) + 1
    print("TOWN", name, "height range", (min(hs), max(hs)) if hs else None, sorted(names.items(), key=lambda k: -k[1])[:12])
    print("PNG_" + name, base64.b64encode(open(f"/tmp/{name}.png", "rb").read()).decode())

# ---- 地形 v2 の計算結果（道の端・街1のまわり） ----
import gzip, struct, glob
for f in glob.glob("run/world/data/kurumimnmod/terrain/*.bin.gz"):
    d = gzip.open(f).read()
    magic, fmt, seed, hsh, n = struct.unpack(">iiqii", d[:24])
    off = 24 + n * n * 9
    count = struct.unpack(">i", d[off:off + 4])[0]; off += 4
    print("BAKE", f, "n", n, "roads", count)
    for r in range(count):
        ln = struct.unpack(">i", d[off:off + 4])[0]; off += 4
        pts = struct.unpack(">%df" % ln, d[off:off + 4 * ln]); off += 4 * ln
        xy = list(zip(pts[0::2], pts[1::2]))
        near = [(round(x), round(z)) for x, z in xy if abs(x) < 250 and abs(z + 3000) < 250]
        print("ROAD", r, "start", xy[0], "end", xy[-1], "points", len(xy), "near_town1", near[::4])
