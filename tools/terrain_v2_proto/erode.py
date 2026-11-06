"""Terrain v2 prototype: designed uplift map + erosion (stream power + thermal) -> realistic landforms.
Region: x in [-4096,4096), z in [-6144,2048), 8 blocks per cell (1024x1024).
"""
import numpy as np, numba as nb, heapq, time, sys

N = 1024
DX = 8.0
X0, Z0 = -4096.0, -6144.0
xs = X0 + (np.arange(N) + 0.5) * DX
zs = Z0 + (np.arange(N) + 0.5) * DX
X, Z = np.meshgrid(xs, zs)  # [j(z), i(x)]

rng = np.random.default_rng(7)

# ---------------- noise helpers ----------------
def fbm(x, z, scale, octaves=5, seed=0, lac=2.0, gain=0.5):
    """value-noise fbm on arrays (numpy, smooth)."""
    out = np.zeros_like(x); amp = 1.0; tot = 0
    for o in range(octaves):
        out += amp * vnoise(x / scale, z / scale, seed + o * 17)
        tot += amp; amp *= gain; scale /= lac
    return out / tot

def vnoise(x, z, seed):
    xi = np.floor(x).astype(np.int64); zi = np.floor(z).astype(np.int64)
    fx = x - xi; fz = z - zi
    def h(a, b):
        v = (a * 374761393 + b * 668265263 + seed * 1442695041) & 0xFFFFFFFF
        v = (v ^ (v >> 13)) * 1274126177 & 0xFFFFFFFF
        v = v ^ (v >> 16)
        return (v & 0xFFFF) / 32767.5 - 1.0
    u = fx * fx * (3 - 2 * fx); w = fz * fz * (3 - 2 * fz)
    return (h(xi, zi) * (1 - u) * (1 - w) + h(xi + 1, zi) * u * (1 - w)
            + h(xi, zi + 1) * (1 - u) * w + h(xi + 1, zi + 1) * u * w)

def smooth(t):
    t = np.clip(t, 0, 1); return t * t * (3 - 2 * t)

# ---------------- design: uplift map ----------------
# domain warp so nothing is a perfect circle / straight line
wx = X + 300 * fbm(X, Z, 1600, 4, 1)
wz = Z + 300 * fbm(X, Z, 1600, 4, 2)

def ridged(x, z, scale, octaves, seed):
    """ridged multifractal (0..1): sharp crest lines -> natural ridge/spur pattern in the uplift."""
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

def range_uplift(pts, height, width, seed, rough=0.45):
    d = seg_dist(wx, wz, pts)
    prof = 1 - smooth(d / width)
    rr = ridged(wx, wz, width * 0.9, 5, seed)
    return height * prof * (1 - rough + rough * rr) * (0.85 + 0.15 * prof)

# low rolling base (plains / hills): enough texture that rivers wander instead of running straight
U = 0.035 + 0.035 * smooth(0.5 + fbm(wx, wz, 1100, 5, 3)) + 0.02 * ridged(wx, wz, 700, 4, 4)

# 1) Great E-W range (impassable)
U = np.maximum(U, range_uplift([(-4800, 1350), (-3000, 1050), (-1500, 1250), (0, 1080), (1600, 1300), (3200, 1050), (4800, 1250)], 1.0, 1300, 5))
# 2) Mediana massif: broken arcs around a small basin, joined to the great range in the south, spurs to the north
ang = np.linspace(np.radians(205), np.radians(335), 9)
rad = 720 + 90 * np.sin(ang * 3.0)
arcW = [(float(np.cos(a) * r), float(np.sin(a) * r)) for a, r in zip(ang, rad)]
ang = np.linspace(np.radians(-155), np.radians(-25), 9)       # northern side (z negative)
rad = 760 + 80 * np.cos(ang * 2.0)
arcN = [(float(np.cos(a) * r), float(np.sin(a) * r)) for a, r in zip(ang, rad)]
U = np.maximum(U, range_uplift(arcW, 0.95, 520, 6))
U = np.maximum(U, range_uplift(arcN, 0.92, 520, 7))
for (sx, sz, ex, ez, hgt) in [(-500, -650, -1200, -1500, 0.55), (550, -700, 1300, -1450, 0.6), (-700, -300, -1700, -700, 0.6), (700, -350, 1800, -800, 0.6)]:
    U = np.maximum(U, range_uplift([(sx, sz), (ex, ez)], hgt, 420, 8 + int(sx) % 7))
# basin: floor kept low
rb = np.hypot(wx * 1.0, wz * 1.15)
U = U * smooth((rb - 300) / 260) + 0.03 * (1 - smooth((rb - 300) / 260))
# 3) Exit gorge through the north arc (antecedent river): low-uplift slot
gx = 120 * np.sin(Z / 380.0) + 40 * fbm(X, Z, 300, 3, 8)
slot = (1 - smooth(np.abs(X - gx) / 55)) * smooth((-Z - 220) / 120) * (1 - smooth((-Z - 1700) / 300))
U = U * (1 - 0.96 * slot)

# 4) Crossable mountains north of Mediana, with a valley between them (route to town 1)
U = np.maximum(U, range_uplift([(-3600, -900), (-2600, -1500), (-1700, -2300), (-1300, -3300)], 0.58, 900, 11))
U = np.maximum(U, range_uplift([(900, -1800), (1900, -2500), (2900, -2300), (3800, -1500)], 0.62, 900, 12))
# a lower ridge across the valley: the river cuts a second gorge, the road can climb over a saddle
U = np.maximum(U, range_uplift([(-1500, -2250), (-300, -2420), (900, -2320), (1700, -2500)], 0.32, 380, 13, rough=0.3))

# 5) West: mesa country (hard cap rock -> escarpments, buttes)
def soft_rect(cx, cz, hx, hz, edge):
    q = np.maximum(np.abs(wx - cx) - hx, np.abs(wz - cz) - hz)
    return 1 - smooth(q / edge)
plateau = soft_rect(-3200, -4300, 800, 1200, 220) * (0.8 + 0.2 * smooth(0.5 + fbm(X, Z, 500, 3, 14)))
# 6) East: high plateau crossed by an antecedent river -> deep layered canyon
canyon = soft_rect(3200, -4700, 900, 1300, 300)
U_PLATEAU = 0.40 * plateau
U_CANYON = 0.50 * canyon

# erodibility: varied rock strength; hard cap on the mesa; layered beds in the canyon
K = 0.6 + 0.8 * smooth(0.5 + fbm(X, Z, 700, 3, 21))
HARD_CAP = plateau > 0.5
LAYERED = canyon > 0.4

# ---------------- erosion kernels ----------------
NB = np.array([(-1, -1), (0, -1), (1, -1), (-1, 0), (1, 0), (-1, 1), (0, 1), (1, 1)])
NBD = np.array([np.hypot(a, b) for a, b in NB])

@nb.njit(cache=True)
def priority_fill(h, fixed, eps):
    n = h.shape[0]
    out = h.copy()
    closed = np.zeros((n, n), np.bool_)
    heap = [(0.0, 0, 0)]
    heap.pop()
    for j in range(n):
        for i in range(n):
            if fixed[j, i]:
                heapq.heappush(heap, (out[j, i], j, i)); closed[j, i] = True
    while len(heap) > 0:
        hv, j, i = heapq.heappop(heap)
        for k in range(8):
            jj = j + (-1, -1, -1, 0, 0, 1, 1, 1)[k]
            ii = i + (-1, 0, 1, -1, 1, -1, 0, 1)[k]
            if jj < 0 or ii < 0 or jj >= n or ii >= n or closed[jj, ii]:
                continue
            closed[jj, ii] = True
            if out[jj, ii] <= hv + eps:
                out[jj, ii] = hv + eps
            heapq.heappush(heap, (out[jj, ii], jj, ii))
    return out

@nb.njit(cache=True)
def receivers(h, fixed, dx):
    n = h.shape[0]
    rec = np.empty(n * n, np.int64); dist = np.ones(n * n)
    dj = (-1, -1, -1, 0, 0, 1, 1, 1); di = (-1, 0, 1, -1, 1, -1, 0, 1)
    for j in range(n):
        for i in range(n):
            idx = j * n + i
            rec[idx] = idx
            if fixed[j, i]:
                continue
            best = 0.0
            for k in range(8):
                jj = j + dj[k]; ii = i + di[k]
                if jj < 0 or ii < 0 or jj >= n or ii >= n:
                    continue
                d = dx * (1.4142135 if (dj[k] != 0 and di[k] != 0) else 1.0)
                s = (h[j, i] - h[jj, ii]) / d
                if s > best:
                    best = s; rec[idx] = jj * n + ii; dist[idx] = d
    return rec, dist

@nb.njit(cache=True)
def stream_power(hf, rec, dist, order, Keff, dt, m, area_cell):
    n2 = hf.size
    A = np.full(n2, area_cell)
    for t in range(n2 - 1, -1, -1):
        i = order[t]
        r = rec[i]
        if r != i:
            A[r] += A[i]
    for t in range(n2):
        i = order[t]
        r = rec[i]
        if r != i:
            F = Keff[i] * dt * (A[i] ** m) / dist[i]
            hf[i] = (hf[i] + F * hf[r]) / (1.0 + F)
    return A

@nb.njit(cache=True)
def thermal(h, fixed, talus, dx, iters):
    n = h.shape[0]
    for it in range(iters):
        for j in range(1, n - 1):
            for i in range(1, n - 1):
                if fixed[j, i]:
                    continue
                for k in range(4):
                    jj = j + (0, 0, 1, -1)[k]; ii = i + (1, -1, 0, 0)[k]
                    d = h[j, i] - h[jj, ii]
                    lim = talus[j, i] * dx
                    if d > lim:
                        mv = 0.5 * (d - lim)
                        h[j, i] -= mv
                        if not fixed[jj, ii]:
                            h[jj, ii] += mv
    return h

def erode(U, K, iters=260, seed_h=None):
    fixed = np.zeros((N, N), bool)
    fixed[0, :] = True; fixed[:, 0] = True; fixed[:, -1] = True   # north / west / east: base level (drain out)
    fixed[-1, :] = True                                             # south edge: held high (range continues)
    h = 2.0 * rng.random((N, N)) if seed_h is None else seed_h.copy()
    h[0, :] = 0; h[:, 0] = 0; h[:, -1] = 0
    UPL = float(sys.argv[2]) if len(sys.argv) > 2 else 5.0  # blocks of uplift per iteration at U=1
    KC = float(sys.argv[3]) if len(sys.argv) > 3 else 0.1
    t0 = time.time()
    for it in range(iters):
        Ut = U.copy()
        if it > iters * 0.35:   # the plateaus rise after the drainage is established -> rivers cut through them
            Ut = np.maximum(Ut, U_PLATEAU)
            Ut = np.maximum(Ut, U_CANYON)
        h[~fixed] += Ut[~fixed] * UPL
        h[-1, :] = h[-2, :] + 1.0
        h = priority_fill(h, fixed, 0.01)
        hflat = h.ravel().copy()
        rec, dist = receivers(h + rng.normal(0, 0.6, h.shape), fixed, DX)   # stochastic routing -> natural, wandering networks
        order = np.argsort(hflat, kind="stable")
        # layered rock: K oscillates with height (cliff bands / terraces)
        Kl = K.copy()
        Kl[LAYERED] *= 0.25 + 0.75 * (np.sin(h[LAYERED] / 7.0) > 0.4)
        Kl[HARD_CAP] *= np.where(h[HARD_CAP] > 0.75 * h[HARD_CAP].max(), 0.05, 1.0)
        A = stream_power(hflat, rec, dist, order, (Kl * KC).ravel(), 1.0, 0.5, DX * DX)
        h = hflat.reshape(N, N)
        # hillslopes: steep in hard rock, standard elsewhere
        talus = np.full((N, N), np.tan(np.radians(42)))
        talus[HARD_CAP | LAYERED] = np.tan(np.radians(70))
        talus *= 0.85 + 0.3 * smooth(0.5 + vn_cache)
        h = thermal(h, fixed, talus, DX, 2)
        if it % 40 == 0 or it == iters - 1:
            print(f"it {it} max {h.max():.0f} mean {h.mean():.0f} t={time.time()-t0:.0f}s", flush=True)
    return h, A.reshape(N, N)

vn_cache = fbm(X, Z, 200, 3, 31)

if __name__ == "__main__":
    h, A = erode(U, K, int(sys.argv[1]) if len(sys.argv) > 1 else 260)
    np.savez_compressed("/home/claude/proto/eroded.npz", h=h, A=A, U=U)
    print("saved")
