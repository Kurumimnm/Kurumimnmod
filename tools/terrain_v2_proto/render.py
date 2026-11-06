"""Render the eroded prototype: overview map, perspective views, cross-section."""
import numpy as np, numba as nb, heapq, sys
from PIL import Image, ImageDraw, ImageFont
from scipy import ndimage
sys.path.insert(0, "/home/claude/proto")
from erode import fbm, smooth, N, DX, X0, Z0, X, Z

d = np.load("/home/claude/proto/eroded.npz")
h_raw, A, U = d["h"], d["A"], d["U"]

# ---------------- heights -> Minecraft Y ----------------
SEA = 63
# piecewise: keep low land gentle, let mountains reach near the world top
if "absolute" in d:
    Y = h_raw.copy()
else:
    p_range = np.percentile(h_raw, 99.95)
    Y = SEA + 4 + (h_raw / p_range) * (430 - SEA - 4)
print("Y range", Y.min(), Y.max())

# ---------------- rivers & lakes ----------------
cells = A / (DX * DX)
river = cells > 900
river_w = np.clip(np.sqrt(cells / 900.0) * 1.2, 1, 6)
# Mediana lake: a designed depression in the basin, water level at basin floor
r0 = np.hypot(X, Z * 1.08)
basin_floor = np.median(Y[r0 < 200])
lake = (np.hypot((X + 60) / 170, (Z - 40) / 120) < 1 + 0.2 * fbm(X, Z, 120, 3, 40)) & (Y < basin_floor + 6)
print("basin floor", basin_floor)

# ---------------- climate & materials ----------------
gy, gx = np.gradient(Y, DX)
slope = np.degrees(np.arctan(np.hypot(gx, gy)))
T = 17.0 - (Y - SEA) * 0.062 + 1.5 * fbm(X, Z, 1500, 3, 50)           # snowline ~ Y 330
wet = ndimage.gaussian_filter(river.astype(float), 6) * 4
M = np.clip(0.55 + 0.25 * fbm(X, Z, 1200, 4, 51) + wet, 0, 1)
canyon_zone = d["canyon"] > 0.3 if "canyon" in d else np.zeros_like(Y, bool)
mesa_zone = d["mesa"] > 0.3 if "mesa" in d else np.zeros_like(Y, bool)

def lerp(a, b, t):
    t = np.clip(t, 0, 1)[..., None]
    return a * (1 - t) + b * t

C = {k: np.array(v, float) for k, v in dict(
    grass=(110, 158, 72), grass_dry=(156, 160, 88), forest=(52, 98, 44), conifer=(40, 76, 52),
    alpine=(130, 146, 110), rock=(128, 124, 118), rock_dark=(96, 94, 92), snow=(242, 244, 248),
    red=(178, 98, 58), orange=(206, 142, 82), sand=(214, 196, 150), water=(52, 102, 186), path=(182, 158, 110)).items()}

def base_colors(Y, slope, T, M, X, Z, canyon_zone, mesa_zone):
    n = fbm(X, Z, 60, 3, 60)
    col = lerp(C["grass"], C["grass_dry"], (0.5 - M) * 2.5)
    forest_amt = smooth((M - 0.5) * 5 + 0.3 * n) * (1 - smooth((slope - 30) / 8))
    forest_col = lerp(C["forest"], C["conifer"], (12 - T) / 5)
    col = lerp(col, forest_col, forest_amt)
    col = lerp(col, C["alpine"], (6 - T) / 3)
    rockcol = lerp(C["rock"], C["rock_dark"], 0.5 + n)
    col = lerp(col, rockcol, (slope - 33) / 8)
    # layered red rock in canyon / mesa country
    bands = 0.5 + 0.5 * np.sin(Y / 4.5)
    redrock = lerp(C["red"], C["orange"], bands)
    zone = (canyon_zone | mesa_zone).astype(float)
    col = lerp(col, lerp(lerp(C["grass_dry"], C["orange"], 0.4), redrock, (slope - 20) / 10), zone)
    col = lerp(col, C["snow"], (-T) / 2 + 0.3 * n * (T < 1) - (slope - 50) / 20 * (slope > 50))
    return col

col = base_colors(Y, slope, T, M, X, Z, canyon_zone, mesa_zone)

# ---------------- road: A* with grade cost (Mediana -> gorge -> saddle -> town 1) ----------------
@nb.njit(cache=True)
def astar(Y, water, sj, si, tj, ti, dx):
    n = Y.shape[0]
    INF = 1e18
    g = np.full((n, n), INF); prev = np.full((n, n), -1, np.int64)
    g[sj, si] = 0.0
    heap = [(0.0, sj, si)]
    dj = (-1, -1, -1, 0, 0, 1, 1, 1); di = (-1, 0, 1, -1, 1, -1, 0, 1)
    while len(heap) > 0:
        f, j, i = heapq.heappop(heap)
        if j == tj and i == ti:
            break
        for k in range(8):
            jj = j + dj[k]; ii = i + di[k]
            if jj < 0 or ii < 0 or jj >= n or ii >= n:
                continue
            dist = dx * (1.4142 if dj[k] != 0 and di[k] != 0 else 1.0)
            grade = abs(Y[jj, ii] - Y[j, i]) / dist
            c = dist * (1 + 30 * grade * grade + (200.0 if grade > 0.35 else 0.0))
            if water[jj, ii]:
                c += 40 * dist      # bridges are expensive -> cross where narrow
            ng = g[j, i] + c
            if ng < g[jj, ii]:
                g[jj, ii] = ng; prev[jj, ii] = j * n + i
                hh = dx * np.hypot(jj - tj, ii - ti)
                heapq.heappush(heap, (ng + hh, jj, ii))
    path = []
    cur = tj * n + ti
    while cur != -1:
        path.append(cur); cur = prev[cur // n, cur % n]
    return path[::-1]

def cell(x, z):
    return int((z - Z0) / DX), int((x - X0) / DX)

water = river | lake
sj, si = cell(0, 0); tj, ti = cell(0, -3000)
road = astar(Y, water, sj, si, tj, ti, DX)
road_j = np.array([p // N for p in road]); road_i = np.array([p % N for p in road])
print("road cells", len(road))
# road through town 1 to the plateau town (mesa) and the canyon (east) as further examples
road2 = astar(Y, water, tj, ti, *cell(-3000, -4300), DX)
road3 = astar(Y, water, tj, ti, *cell(3000, -5200), DX)

# ---------------- overview map ----------------
def hillshade(Y, dx, az=315, alt=40):
    gy, gx = np.gradient(Y, dx)
    a = np.radians(az); e = np.radians(alt)
    nx, nz, ny = -gx, -gy, np.ones_like(Y)
    l = np.array([np.cos(e) * np.sin(a), np.cos(e) * -np.cos(a), np.sin(e)])
    norm = np.sqrt(nx ** 2 + nz ** 2 + 1)
    return np.clip((nx * l[0] + nz * l[1] + ny * l[2]) / norm, 0, 1)

shade = hillshade(Y, DX)
img = col * (0.35 + 0.8 * shade[..., None])
img = np.where((water & ~lake)[..., None], C["water"] * (0.8 + 0.2 * shade[..., None]), img)
img = np.where(lake[..., None], C["water"] * 0.9, img)
im = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8))
dr = ImageDraw.Draw(im)
for rd in (road, road2, road3):
    pts = [(p % N, p // N) for p in rd]
    dr.line(pts, fill=(250, 226, 150), width=2)
try:
    font = ImageFont.truetype("/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc", 22)
except Exception:
    font = ImageFont.load_default()
def label(x, z, text, colr=(255, 255, 255)):
    j, i = cell(x, z)
    dr.ellipse([i - 5, j - 5, i + 5, j + 5], fill=(255, 255, 255), outline=(0, 0, 0))
    dr.text((i + 9, j - 12), text, fill=colr, font=font, stroke_width=3, stroke_fill=(0, 0, 0))
label(0, 0, "Mediana")
label(0, -3000, "Town 1")
label(-3000, -4300, "mesa town (example)")
label(3000, -5200, "canyon town (example)")
im.save("/home/claude/proto/v2_overview.png")
print("overview saved")

# ---------------- high-res subregion for perspective views ----------------
def subregion(x0, z0, size, up):
    """crop [x0, x0+size) x [z0, z0+size) and upsample to (DX/up) blocks per px, adding detail."""
    j0, i0 = cell(x0, z0); n = int(size / DX)
    pad = 4
    sl = (slice(j0 - pad, j0 + n + pad), slice(i0 - pad, i0 + n + pad))
    Ys = ndimage.zoom(Y[sl], up, order=3)[pad * up:-pad * up, pad * up:-pad * up]
    ws = ndimage.zoom(water[sl].astype(float), up, order=1)[pad * up:-pad * up, pad * up:-pad * up] > 0.5
    lk = ndimage.zoom(lake[sl].astype(float), up, order=1)[pad * up:-pad * up, pad * up:-pad * up] > 0.5
    m = Ys.shape[0]
    bx = x0 + (np.arange(m) + 0.5) * DX / up; bz = z0 + (np.arange(m) + 0.5) * DX / up
    Xs, Zs = np.meshgrid(bx, bz)
    d2 = DX / up
    gy, gx = np.gradient(Ys, d2)
    sl_deg = np.degrees(np.arctan(np.hypot(gx, gy)))
    # detail: rocky roughness on steep ground, gentle undulation on flats
    rough = fbm(Xs, Zs, 24, 4, 70) * (1.0 + 5.0 * smooth((sl_deg - 25) / 20))
    Ys = Ys + rough * 2.0
    Ys[ws] = np.minimum(Ys[ws], ndimage.grey_erosion(Ys, 5)[ws])
    gy, gx = np.gradient(Ys, d2)
    sl_deg = np.degrees(np.arctan(np.hypot(gx, gy)))
    Ts = 17.0 - (Ys - SEA) * 0.062 + 1.5 * fbm(Xs, Zs, 1500, 3, 50)
    wet = ndimage.gaussian_filter(ws.astype(float), 6 * up) * 4
    Ms = np.clip(0.55 + 0.25 * fbm(Xs, Zs, 1200, 4, 51) + wet, 0, 1)
    cz = ndimage.zoom(canyon_zone[sl].astype(float), up, order=1)[pad * up:-pad * up, pad * up:-pad * up] > 0.5
    mz = ndimage.zoom(mesa_zone[sl].astype(float), up, order=1)[pad * up:-pad * up, pad * up:-pad * up] > 0.5
    cs = base_colors(Ys, sl_deg, Ts, Ms, Xs, Zs, cz, mz)
    # trees as dark speckles (forest density)
    tree = (fbm(Xs, Zs, 3, 2, 80) > 0.35) & (cs[..., 1] < 105) & (sl_deg < 32) & (Ts > 3) & ~ws & ~lk
    Ys = np.where(tree, Ys + 6, Ys)
    cs = np.where(tree[..., None], cs * 0.7, cs)
    sh = hillshade(Ys, d2, 300, 35)
    cs = cs * (0.45 + 0.7 * sh[..., None])
    Ys = np.where(lk, basin_floor + 1, Ys)
    cs = np.where((ws | lk)[..., None], C["water"] * (0.75 + 0.25 * sh[..., None]), cs)
    return Ys.astype(np.float32), np.clip(cs, 0, 255).astype(np.float32), x0, z0, d2

@nb.njit(cache=True)
def voxelspace(Hm, Cm, x0, z0, d2, cx, cy, cz, yaw, pitch, W, Hh, fov, far, sky_top, sky_bot, fogc):
    img = np.zeros((Hh, W, 3), np.float32)
    for y in range(Hh):
        t = y / Hh
        for k in range(3):
            img[y, :, k] = sky_top[k] * (1 - t) + sky_bot[k] * t
    m = Hm.shape[0]
    horizon = Hh * 0.5 + pitch
    focal = (W / 2) / np.tan(fov / 2)
    for col in range(W):
        ang = yaw + np.arctan((col - W / 2) / focal)
        dxr = np.sin(ang); dzr = -np.cos(ang)
        corr = np.cos(ang - yaw)
        ybuf = Hh
        dist = 1.0
        while dist < far:
            px = cx + dxr * dist; pz = cz + dzr * dist
            fi = (px - x0) / d2; fj = (pz - z0) / d2
            if fi < 0 or fj < 0 or fi >= m - 1 or fj >= m - 1:
                dist += 1.0 + dist * 0.01
                continue
            i = int(fi); j = int(fj)
            u = fi - i; v = fj - j
            hgt = (Hm[j, i] * (1 - u) * (1 - v) + Hm[j, i + 1] * u * (1 - v) + Hm[j + 1, i] * (1 - u) * v + Hm[j + 1, i + 1] * u * v)
            sy = int((cy - hgt) / (dist * corr) * focal + horizon)
            if sy < ybuf:
                fog = min(1.0, (dist / far) ** 1.6)
                top = max(sy, 0)
                for y in range(top, ybuf):
                    for k in range(3):
                        img[y, col, k] = Cm[j, i, k] * (1 - fog) + fogc[k] * fog
                ybuf = top
                if ybuf <= 0:
                    break
            dist += 0.5 + dist * 0.004
    return img

def view(name, sub, cam, yaw_deg, pitch=0, W=1400, Hh=620, fov=70, far=3200):
    Hm, Cm, x0, z0, d2 = sub
    img = voxelspace(Hm, Cm, x0, z0, d2, cam[0], cam[1], cam[2], np.radians(yaw_deg), pitch, W, Hh,
                     np.radians(fov), far, np.array([118., 160., 222.]), np.array([196., 214., 236.]), np.array([186., 204., 228.]))
    Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)).save(f"/home/claude/proto/{name}.png")
    print("view", name)

if __name__ == "__main__":
    sub = subregion(-2048, -3584, 4096, 4)   # x [-2048,2048), z [-3584,512): Mediana + route to town 1
    Hm = sub[0]
    def ground(x, z):
        i = int((x - sub[2]) / sub[4]); j = int((z - sub[3]) / sub[4]); return float(Hm[j, i])
    # 1) inside Mediana, looking north at the exit gorge
    view("v2_view_mediana_north", sub, (40, ground(40, 280) + 14, 280), 0, pitch=40)
    # 2) from the valley north of the massif, looking back south at the range
    view("v2_view_range_from_north", sub, (-300, ground(-300, -2900) + 60, -2900), 175, pitch=-10, far=4000)
    # 3) inside the exit gorge, looking north (walking between cliffs)
    zc = -560
    xs_ = np.arange(-300, 300, 1.0); gs = np.array([ground(x, zc) for x in xs_])
    low = xs_[gs < gs.min() + 6]; xc = float(low.mean())
    view("v2_view_gorge", sub, (xc, ground(xc, zc) + 12, zc), 0, pitch=70, fov=80, far=2600)
    # 4) east canyon from the rim
    sub2 = subregion(1600, -5900, 2400, 4)
    H2 = sub2[0]
    def ground2(x, z):
        i = int((x - sub2[2]) / sub2[4]); j = int((z - sub2[3]) / sub2[4]); return float(H2[j, i])
    zc = -3600
    rxc = 3150 + 260 * np.sin(zc / 520.0) + 120 * np.sin(zc / 190.0)
    rim = max(ground2(rxc - 60, zc), ground2(rxc + 60, zc))
    if False: view("v2_view_canyon", sub2, (rxc, rim + 30, zc), 0, pitch=110, far=2300)
    # cross-section N-S through Mediana
    prof = Y[:, N // 2]
    from PIL import ImageDraw as D
    W2, H2 = 1400, 360
    pim = Image.new("RGB", (W2, H2), (235, 240, 246)); pd = D.Draw(pim)
    ys = prof[::-1]  # south (z+) on the left? keep north on the left
    pts = [(int(k * W2 / N), int(H2 - (prof[k] - 40) * H2 / 420)) for k in range(N)]
    pd.polygon(pts + [(W2, H2), (0, H2)], fill=(120, 110, 96))
    for yy in (63, 100, 200, 300, 400):
        y = int(H2 - (yy - 40) * H2 / 420); pd.line([(0, y), (W2, y)], fill=(200, 205, 215)); pd.text((4, y - 12), f"Y{yy}", fill=(60, 60, 60), font=font)
    pim.save("/home/claude/proto/v2_section.png")
