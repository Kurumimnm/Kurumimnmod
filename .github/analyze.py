
import os, glob, zlib, io, struct, math
import nbtlib, numpy as np
import matplotlib; matplotlib.use("Agg"); import matplotlib.pyplot as plt
os.makedirs("probe_out", exist_ok=True)
REG = glob.glob("run/world/**/region", recursive=True)
print("region dirs", REG)
reg = [r for r in REG if "dimensions/minecraft/overworld" in r.replace("\\","/") or r.endswith("world/region")]
reg = reg[0] if reg else REG[0]
heights = {}; blocks_found = {}; stats = {"chunks": 0, "underground_air": 0, "water_y48_79": 0}
def chunk_nbt(path, lx, lz):
    with open(path, "rb") as f:
        f.seek(4 * (lx + lz * 32)); off = f.read(4)
        o = (off[0] << 16 | off[1] << 8 | off[2]) * 4096
        if o == 0: return None
        f.seek(o); ln = struct.unpack(">I", f.read(4))[0]; comp = f.read(1)[0]
        data = zlib.decompress(f.read(ln - 1))
        return nbtlib.File.parse(io.BytesIO(data))
for path in glob.glob(os.path.join(reg, "r.*.mca")):
    rx, rz = map(int, os.path.basename(path).split(".")[1:3])
    for lz in range(32):
        for lx in range(32):
            c = chunk_nbt(path, lx, lz)
            if c is None: continue
            status = str(c.get("Status", ""))
            if "full" not in status: continue
            cx, cz = rx * 32 + lx, rz * 32 + lz
            hm = c["Heightmaps"]["WORLD_SURFACE"]
            vals = []
            BITS = 10; PER = 64 // BITS   # 高さ 512 → 10 ビット
            for v in hm:
                v = int(v) & ((1 << 64) - 1)
                for k in range(PER): vals.append((v >> (BITS * k)) & ((1 << BITS) - 1))
            for i in range(256):
                heights[(cx * 16 + i % 16, cz * 16 + i // 16)] = vals[i] - 64 - 1
            # 洞窟（地下の空気）と川・池（地表付近の水）の数
            for sec in c.get("sections", []):
                Y = int(sec["Y"]); bs = sec.get("block_states")
                pal = [str(p.get("id", "")) if hasattr(p, "get") else str(p) for p in bs.get("palette", [])] if hasattr(bs, "get") else []
                if -4 <= Y <= 2 and ("minecraft:air" in pal or "minecraft:cave_air" in pal): stats["underground_air"] += 1
                if 3 <= Y <= 4 and "minecraft:water" in pal: stats["water_y48_79"] += 1
            stats["chunks"] += 1
            if -2 <= cx <= 1 and -2 <= cz <= 1:
                secs = c.get("sections", [])
                if not isinstance(secs, list): print("sections type", type(secs)); secs = []
                for sec in secs:
                    bs = sec.get("block_states") if hasattr(sec, "get") else None
                    pal = bs.get("palette", []) if hasattr(bs, "get") else []
                    for p in pal:
                        n = str(p.get("id", "")) if hasattr(p, "get") else str(p)
                        if n in ("minecraft:oak_door", "minecraft:crafting_table", "minecraft:lantern"):
                            blocks_found.setdefault(n, []).append((cx, cz, int(sec["Y"])))
print("chunks with heights:", len(heights) // 256)
print("house blocks:", blocks_found)
print("STATS", stats)
# 南北の断面 (x=8) と 東西の断面 (z=8)
ns = sorted((z, h) for (x, z), h in heights.items() if x == 8)
ew = sorted((x, h) for (x, z), h in heights.items() if z == 8)
fig, ax = plt.subplots(2, 1, figsize=(12, 7))
for a, d, t in [(ax[0], ns, "North-South (x=8). left = north"), (ax[1], ew, "West-East (z=8)")]:
    if d:
        a.plot([p[0] for p in d], [p[1] for p in d], lw=0.8)
    a.axhline(192.33, color="gray", ls="--", lw=0.7); a.set_title(t); a.set_ylim(-64, 448); a.grid(alpha=.3)
plt.tight_layout(); plt.savefig("probe_out/profiles.png")
# 峠のあたりの上空図
xs = range(-400, 401); zs = range(-1000, -699)
img = np.full((len(zs), len(xs)), np.nan)
for j, z in enumerate(zs):
    for i, x in enumerate(xs):
        if (x, z) in heights: img[j, i] = heights[(x, z)]
plt.figure(figsize=(9, 4)); plt.imshow(img, extent=[-400, 400, -699, -1000], cmap="terrain"); plt.colorbar(); plt.title("pass heightmap")
plt.savefig("probe_out/pass.png")
for z in (-1600, -1200, -900, -600, -300, 0, 300, 600, 900, 1200, 1500, 1800):
    print("x=8 z=%d height=%s" % (z, heights.get((8, z))))

print("NS_PROFILE", ";".join("%d:%d" % (z, h) for z, h in ns if z % 8 == 0))
print("EW_PROFILE", ";".join("%d:%d" % (x, h) for x, h in ew if x % 8 == 0))
pas = sorted((x, z, h) for (x, z), h in heights.items() if -400 <= x <= 400 and -1000 <= z <= -700 and x % 16 == 0 and z % 16 == 0)
print("PASS", ";".join("%d,%d:%d" % p for p in pas))
