"""Terrain v2 prototype (design heights + erosion filter).
1) The designer's intent is written as heights (ranges, massifs, basin, plateaus, gorges) with natural noise.
2) An erosion pass (stream power incision + talus) carves dendritic valleys, gorges and spurs into it.
Region: x in [-4096,4096), z in [-6144,2048), 8 blocks per cell (1024x1024).
"""
import numpy as np, numba as nb, time, sys
from erode import fbm, vnoise, smooth, priority_fill, receivers, stream_power, thermal, N, DX, X0, Z0, X, Z

rng = np.random.default_rng(11)

wx = X + 320 * fbm(X, Z, 1700, 4, 1)
wz = Z + 320 * fbm(X, Z, 1700, 4, 2)

def ridged(x, z, scale, octaves, seed):
    out = np.zeros_like(x); amp = 1.0; tot = 0; prev = 1.0
    for o in range(octaves):
        v = 1 - np.abs(vnoise(x / scale, z / scale, seed + o * 31))
        v = v * v * prev
        out += amp * v; tot += amp; prev = np.clip(v * 1.6, 0, 1)
        amp *= 0.5; scale /= 2.1
    return out / tot

def seg_dist(px, pz, pts):
    best = np.full(px.shape, 1e9)
    for (ax, az), (bx, bz) in zip(pts[:-1], pts[1:]):
        vx, vz = bx - ax, bz - az
        t = np.clip(((px - ax) * vx + (pz - az) * vz) / (vx * vx + vz * vz), 0, 1)
        best = np.minimum(best, np.hypot(px - ax - t * vx, pz - az - t * vz))
    return best

BASE = 72.0
# strongly warped coordinates for the mountain texture (branching ridges and spurs)
mx = wx + 180 * fbm(X, Z, 420, 4, 40)
mz = wz + 180 * fbm(X, Z, 420, 4, 41)
def mountain(pts, peak, width, seed, rough=0.6, sharp=1.2):
    """height above base of a mountain chain along a ridge line (blocks)."""
    d = seg_dist(wx, wz, pts)
    prof = (1 - smooth(d / width)) ** sharp
    rr = 0.6 * ridged(mx, mz, width * 0.75, 6, seed) + 0.4 * ridged(mx, mz, 260, 5, seed + 5)
    return (peak - BASE) * prof * (1 - rough + rough * rr) * (0.9 + 0.1 * prof)

# ---- base: rolling lowland ----
H = BASE + 10 * fbm(wx, wz, 1300, 5, 3) + 8 * ridged(wx, wz, 600, 5, 4)

def add(h):
    global H
    H = np.maximum(H, BASE + h)

# great E-W range (impassable): crest 380-440
add(mountain([(-4800, 1350), (-3000, 1050), (-1500, 1250), (0, 1080), (1600, 1300), (3200, 1050), (4800, 1250)], 500, 1500, 5))
# Mediana massif: two broken arcs + spurs, joined to the great range
ang = np.linspace(np.radians(10), np.radians(170), 10); rad = 660 + 80 * np.sin(ang * 3.0)
arcS = [(float(np.cos(a) * r), float(np.sin(a) * r)) for a, r in zip(ang, rad)]
ang = np.linspace(np.radians(-160), np.radians(-20), 10); rad = 700 + 70 * np.cos(ang * 2.0)
arcN = [(float(np.cos(a) * r), float(np.sin(a) * r)) for a, r in zip(ang, rad)]
add(mountain(arcS, 470, 560, 6))
add(mountain(arcN, 455, 560, 7))
for (sx, sz, ex, ez, pk) in [(-450, -700, -1100, -1550, 330), (500, -720, 1250, -1500, 345),
                             (-760, -250, -1750, -700, 360), (760, -300, 1800, -820, 350)]:
    add(mountain([(sx, sz), (ex, ez)], pk, 460, 8 + abs(int(sx)) % 7))
# basin floor (small tutorial basin, ~700 blocks across)
rb = np.hypot(wx, wz * 1.12)
basin = 1 - smooth((rb - 280) / 300)
H = H * (1 - basin) + (BASE + 22 + 6 * fbm(wx, wz, 200, 3, 9)) * basin
# exit gorge to the north (the basin's river leaves through it)
gx = 110 * np.sin(Z / 360.0) + 40 * fbm(X, Z, 300, 3, 8)
slot = (1 - smooth(np.abs(X - gx) / 42)) * smooth((-Z - 200) / 100) * (1 - smooth((-Z - 1650) / 250))
H = H * (1 - slot) + (BASE + 22 - 12 * smooth((-Z - 200) / 1400)) * slot

# crossable mountains north of Mediana with a valley between them
add(mountain([(-3600, -900), (-2600, -1500), (-1750, -2300), (-1350, -3300)], 270, 950, 11))
add(mountain([(900, -1800), (1900, -2550), (2900, -2300), (3800, -1500)], 285, 950, 12))
# a lower ridge across the valley (the river cuts through it, the road climbs a saddle)
add(mountain([(-1500, -2250), (-300, -2420), (900, -2320), (1700, -2500)], 150, 420, 13, rough=0.35))
cut = (1 - smooth(np.abs(X - (60 + 90 * np.sin(Z / 300))) / 26)) * (1 - smooth(np.abs(Z + 2400) / 500))
H = H * (1 - cut) + (BASE + 8) * cut

# west: mesa country with a hard cap rock
def soft_rect(cx, cz, hx, hz, edge, warp=1.0):
    """organic blob: superellipse warped by noise (no straight edges)."""
    px = wx + 220 * fbm(X, Z, 520, 4, 60); pz = wz + 220 * fbm(X, Z, 520, 4, 61)
    q = (np.abs((px - cx) / hx) ** 3 + np.abs((pz - cz) / hz) ** 3) ** (1 / 3) - 1
    q = q * min(hx, hz) + 90 * fbm(X, Z, 160, 3, 62)
    return 1 - smooth(q / edge)
mesa = soft_rect(-3200, -4300, 800, 1200, 70)
mesa_top = BASE + 110
H = np.maximum(H, H * (1 - mesa) + (mesa_top + 6 * fbm(X, Z, 300, 3, 14)) * mesa)
# east: a high layered plateau with an antecedent river crossing it (initial groove -> canyon)
canyon = soft_rect(3200, -4600, 950, 1350, 120)
plat_top = BASE + 140
H = np.maximum(H, H * (1 - canyon) + (plat_top + 5 * fbm(X, Z, 300, 3, 15)) * canyon)
rx = 3150 + 260 * np.sin(Z / 520.0) + 120 * np.sin(Z / 190.0)
groove = (1 - smooth(np.abs(X - rx) / 30)) * canyon
H = H - 40 * groove

# fine natural irregularity everywhere (so water finds natural paths)
H += (H - BASE) * 0.06 * fbm(X, Z, 140, 4, 90) + 1.5 * fbm(X, Z, 50, 3, 91)

DESIGN = H.copy()
GORGE = (slot > 0.05) | (cut > 0.05) | (groove > 0.05)
HARD_CAP = mesa > 0.4
LAYERED = canyon > 0.3

def erode(H, iters, KC):
    fixed = np.zeros((N, N), bool)
    fixed[0, :] = True; fixed[:, 0] = True; fixed[:, -1] = True; fixed[-1, :] = True
    h = H.copy()
    K = 0.6 + 0.8 * smooth(0.5 + fbm(X, Z, 700, 3, 21))
    t0 = time.time()
    for it in range(iters):
        hf = priority_fill(h, fixed, 0.02)
        lake = hf - h
        rec, dist = receivers(hf + rng.normal(0, 0.03, h.shape), fixed, DX)
        order = np.argsort(hf.ravel(), kind="stable")
        Kl = K.copy()
        Kl[LAYERED] *= 0.2 + 0.8 * (np.sin(hf[LAYERED] / 6.0) > 0.3)          # alternating hard / soft beds
        Kl[HARD_CAP] *= np.where(hf[HARD_CAP] > mesa_top - 12, 0.04, 1.0)     # cap rock
        flat = hf.ravel().copy()
        A = stream_power(flat, rec, dist, order, (Kl * KC).ravel(), 1.0, 0.5, DX * DX)
        h = h - np.maximum(0, hf - flat.reshape(N, N))   # erosion depth applied to the real surface (lakes stay lakes)
        talus = np.full((N, N), np.tan(np.radians(40)))
        talus[HARD_CAP | LAYERED] = np.tan(np.radians(72))
        talus[DESIGN > 260] = np.tan(np.radians(52))
        talus[GORGE] = np.tan(np.radians(80))
        h = thermal(h, fixed, talus, DX, 2)
        h = np.minimum(h, 440.0)
        if it % 30 == 0 or it == iters - 1:
            print(f"it {it} max {h.max():.0f} min {h.min():.0f} t={time.time()-t0:.0f}s", flush=True)
    return h, A.reshape(N, N)

if __name__ == "__main__":
    iters = int(sys.argv[1]); KC = float(sys.argv[2])
    H = np.minimum(H, 440.0)
    h, A = erode(H, iters, KC)
    np.savez_compressed("/home/claude/proto/eroded.npz", h=h - 0, A=A, U=DESIGN, absolute=1, mesa=mesa, canyon=canyon)
    print("saved")
