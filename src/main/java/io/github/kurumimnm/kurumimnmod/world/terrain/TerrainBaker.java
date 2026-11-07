package io.github.kurumimnm.kurumimnmod.world.terrain;

import io.github.kurumimnm.kurumimnmod.world.terrain.TerrainDesign.Landform;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleBinaryOperator;
import org.slf4j.Logger;

/**
 * 地形 v2 の計算（ワールド作成時に 1 回だけ）。
 * <ol>
 *   <li><b>設計の高さ</b>：設計図の部品（山並み・盆地・峡谷・湖）を、シード値で少しずつ形を変えながら重ねる</li>
 *   <li><b>侵食</b>：雨水の流れを計算し、水が集まる所ほど地面を削る（川の谷・峡谷・支尾根ができる）。
 *       急すぎる斜面は崩す（岩の固さで角度を変える）</li>
 *   <li><b>水</b>：窪みにたまる水を湖、水がたくさん集まる所を川にし、水面の高さを決める</li>
 *   <li><b>道</b>：坂がゆるく、川を渡らない道筋を探す（経路探索）</li>
 * </ol>
 * 結果は {@link TerrainBake}。範囲の縁は今までの地形（v1）に合わせてあるので、境目は出ない。
 */
public final class TerrainBaker {

    private TerrainBaker() {}

    /** 細かい決まり（設計図で変えるほどではないもの）。 */
    static final class ModLimits {
        /** これより小さい窪み（マス数）は湖にせず埋める。 */
        static final int LAKE_MIN_CELLS = 60;
        /** これより浅い窪み（ブロック）は湖にせず埋める。 */
        static final double LAKE_MIN_DEPTH = 2.0;
    }

    /** 道を引く区間（ブロック座標）。 */
    public record RoadRequest(double fromX, double fromZ, double toX, double toZ) {}

    /** 街（平らにする場所）。 */
    public record TownPad(double x, double z, double radius) {}

    public static TerrainBake bake(TerrainDesign design, long seed, DoubleBinaryOperator v1Height,
                                   List<TownPad> towns, List<RoadRequest> roads, Logger log) {
        long start = System.currentTimeMillis();
        TerrainDesign.Region region = design.region();
        int n = region.cells();
        double cell = region.cell();
        BakeNoise noise = new BakeNoise(seed);
        Random random = new Random(seed ^ 0x5DEECE66DL);

        // ---- 1. 設計の高さ ----
        float[] design0 = new float[n * n];
        boolean[] gorge = new boolean[n * n];
        boolean[] pad = new boolean[n * n];
        List<Shape> shapes = prepare(design, noise, random);
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                double x = region.x0() + (i + 0.5) * cell;
                double z = region.z0() + (j + 0.5) * cell;
                int idx = j * n + i;
                double[] g = new double[1];
                double h = designHeight(design, noise, shapes, x, z, g);
                gorge[idx] = g[0] > 0.05;
                // 縁は今までの地形に寄せる
                double w = edgeWeight(region, x, z);
                if (w < 1) {
                    h = v1Height.applyAsDouble(x, z) * (1 - w) + h * w;
                }
                design0[idx] = (float) Math.min(h, 440.0);
            }
        }
        // 街の場所：少し高い平らな台にして、川がよけて流れるようにする
        for (TownPad town : towns) {
            int ci = (int) Math.floor((town.x() - region.x0()) / cell);
            int cj = (int) Math.floor((town.z() - region.z0()) / cell);
            if (ci < 0 || cj < 0 || ci >= n || cj >= n) {
                continue;
            }
            float level = design0[cj * n + ci] + 4;
            int r = (int) Math.ceil((town.radius() + 24) / cell);
            for (int dj = -r; dj <= r; dj++) {
                for (int di = -r; di <= r; di++) {
                    int ii = ci + di;
                    int jj = cj + dj;
                    if (ii < 0 || jj < 0 || ii >= n || jj >= n || Math.hypot(di, dj) * cell > town.radius() + 24) {
                        continue;
                    }
                    design0[jj * n + ii] = Math.max(design0[jj * n + ii], level);
                    pad[jj * n + ii] = true;
                }
            }
        }
        log.info("[地形v2] 設計の高さ: {} ms", System.currentTimeMillis() - start);

        // ---- 2. 侵食 ----
        float[] h = design0.clone();
        boolean[] fixed = new boolean[n * n];
        for (int k = 0; k < n; k++) {
            fixed[k] = fixed[(n - 1) * n + k] = fixed[k * n] = fixed[k * n + n - 1] = true;
        }
        double[] kField = new double[n * n];
        float[] talus = new float[n * n];
        TerrainDesign.Erosion er = design.erosion();
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int idx = j * n + i;
                double x = region.x0() + (i + 0.5) * cell;
                double z = region.z0() + (j + 0.5) * cell;
                kField[idx] = pad[idx] ? 0 : er.k() * (0.6 + 0.8 * BakeNoise.smooth(0.5 + noise.fbm(x, z, 700, 3, 21)));
                double angle = gorge[idx] ? er.talusGorge() : design0[idx] > er.highAbove() ? er.talusHigh() : er.talus();
                talus[idx] = (float) (Math.tan(Math.toRadians(angle)) * cell);
            }
        }
        float[] filled = new float[n * n];
        int[] receiver = new int[n * n];
        float[] distance = new float[n * n];
        int[] order = new int[n * n];
        double[] area = new double[n * n];
        for (int it = 0; it < er.iterations(); it++) {
            Hydro.priorityFill(h, fixed, n, 0.02f, filled);
            Hydro.receivers(filled, fixed, n, cell, random, 1f, receiver, distance);
            Hydro.order(filled, order);
            float[] eroded = filled.clone();
            Hydro.streamPower(eroded, receiver, distance, order, kField, area, cell * cell);
            for (int idx = 0; idx < n * n; idx++) {
                h[idx] -= Math.max(0, filled[idx] - eroded[idx]); // 削った分だけ下げる（湖の窪みはそのまま残る）
            }
            Hydro.thermal(h, fixed, pad, talus, n, 2);
        }
        log.info("[地形v2] 侵食 {} 回: {} ms", er.iterations(), System.currentTimeMillis() - start);

        // ---- 3. 水（湖・川） ----
        Hydro.priorityFill(h, fixed, n, 0.0f, filled);
        float[] routing = new float[n * n];
        Hydro.priorityFill(h, fixed, n, 0.02f, routing);
        Hydro.receivers(routing, fixed, n, cell, null, 0f, receiver, distance);
        Hydro.order(routing, order);
        Hydro.accumulate(receiver, order, area, cell * cell);
        float[] water = new float[n * n];
        byte[] river = new byte[n * n];
        Arrays.fill(water, Float.NaN);
        // 湖：窪みにたまる水。小さな窪み・高い所の窪みは埋めて平らにする（侵食でできた細かい穴を残さない）
        boolean[] lakeCell = new boolean[n * n];
        for (int idx = 0; idx < n * n; idx++) {
            lakeCell[idx] = !pad[idx] && filled[idx] - h[idx] > 0.5f;
        }
        int[] stack = new int[n * n];
        boolean[] seen = new boolean[n * n];
        List<Integer> component = new ArrayList<>();
        for (int start0 = 0; start0 < n * n; start0++) {
            if (!lakeCell[start0] || seen[start0]) {
                continue;
            }
            component.clear();
            int top = 0;
            stack[top++] = start0;
            seen[start0] = true;
            double maxDepth = 0;
            while (top > 0) {
                int idx = stack[--top];
                component.add(idx);
                maxDepth = Math.max(maxDepth, filled[idx] - h[idx]);
                int i = idx % n;
                int j = idx / n;
                for (int k = 0; k < 4; k++) {
                    int ii = i + (k == 0 ? 1 : k == 1 ? -1 : 0);
                    int jj = j + (k == 2 ? 1 : k == 3 ? -1 : 0);
                    if (ii < 0 || jj < 0 || ii >= n || jj >= n) {
                        continue;
                    }
                    int nb = jj * n + ii;
                    if (lakeCell[nb] && !seen[nb]) {
                        seen[nb] = true;
                        stack[top++] = nb;
                    }
                }
            }
            boolean keep = component.size() >= ModLimits.LAKE_MIN_CELLS && maxDepth >= ModLimits.LAKE_MIN_DEPTH
                    && filled[start0] < design.mountainBiomeAbove();
            for (int idx : component) {
                if (keep) {
                    water[idx] = filled[idx];
                } else {
                    h[idx] = filled[idx]; // 埋める
                }
            }
        }
        double minArea = design.river().minCells() * cell * cell;
        for (int idx = 0; idx < n * n; idx++) {
            if (!pad[idx] && Float.isNaN(water[idx]) && area[idx] > minArea && !fixed[idx]) {
                river[idx] = 1;
                water[idx] = h[idx] - 0.4f;
            }
        }
        log.info("[地形v2] 水: {} ms", System.currentTimeMillis() - start);

        // ---- 4. 道 ----
        List<float[]> roadLines = new ArrayList<>();
        for (RoadRequest request : roads) {
            float[] line = Hydro.road(h, water, n, region, request, noise);
            if (line != null) {
                roadLines.add(line);
            }
        }
        log.info("[地形v2] 道 {} 本: {} ms", roadLines.size(), System.currentTimeMillis() - start);

        return new TerrainBake(design, seed, h, water, river, roadLines);
    }

    /** 縁からの距離で 0（今までの地形）〜1（v2）。 */
    public static double edgeWeight(TerrainDesign.Region region, double x, double z) {
        double d = Math.min(Math.min(x - region.x0(), region.x0() + region.size() - x),
                Math.min(z - region.z0(), region.z0() + region.size() - z));
        return BakeNoise.smooth(d / region.blend());
    }

    // =====================================================================
    // 設計の高さ
    // =====================================================================

    /** 部品を、シードでずらした形にしておく。 */
    private record Shape(Landform form, double[] xs, double[] zs, long salt) {}

    private static List<Shape> prepare(TerrainDesign design, BakeNoise noise, Random random) {
        List<Shape> shapes = new ArrayList<>();
        long salt = 1000;
        for (Landform form : design.landforms()) {
            salt += 101;
            List<double[]> pts = new ArrayList<>();
            switch (form.type()) {
                case "arc" -> {
                    double phase = random.nextDouble() * Math.PI * 2;
                    int count = 10;
                    for (int k = 0; k < count; k++) {
                        double a = Math.toRadians(form.from() + (form.to() - form.from()) * k / (count - 1.0));
                        double r = form.radius() + form.wobble() * Math.sin(a * 3.0 + phase);
                        pts.add(new double[]{form.center().get(0) + Math.cos(a) * r, form.center().get(1) + Math.sin(a) * r});
                    }
                }
                case "range", "gorge" -> {
                    for (List<Double> p : form.points()) {
                        pts.add(new double[]{p.get(0), p.get(1)});
                    }
                }
                default -> {}
            }
            // シードで点をずらす（峡谷は両端を動かさない）
            for (int k = 0; k < pts.size(); k++) {
                boolean pinned = "gorge".equals(form.type()) && (k == 0 || k == pts.size() - 1);
                double amount = "gorge".equals(form.type()) ? design.jitter() * 0.4 : design.jitter();
                if (!pinned) {
                    pts.get(k)[0] += (random.nextDouble() * 2 - 1) * amount;
                    pts.get(k)[1] += (random.nextDouble() * 2 - 1) * amount;
                }
            }
            shapes.add(new Shape(form, pts.stream().mapToDouble(p -> p[0]).toArray(), pts.stream().mapToDouble(p -> p[1]).toArray(), salt));
        }
        return shapes;
    }

    private static double designHeight(TerrainDesign design, BakeNoise noise, List<Shape> shapes, double x, double z, double[] gorgeOut) {
        double base = design.baseHeight();
        // ゆがめた座標：まっすぐな線・きれいな円ができないように
        double wx = x + 320 * noise.fbm(x, z, 1700, 4, 1);
        double wz = z + 320 * noise.fbm(x, z, 1700, 4, 2);
        double mx = wx + 180 * noise.fbm(x, z, 420, 4, 40);
        double mz = wz + 180 * noise.fbm(x, z, 420, 4, 41);
        double h = base + design.baseNoise() * noise.fbm(wx, wz, 1300, 5, 3) + 0.8 * design.baseNoise() * noise.ridged(wx, wz, 600, 5, 4);
        double gorge = 0;
        for (Shape s : shapes) {
            Landform f = s.form();
            switch (f.type()) {
                case "range", "arc" -> {
                    double d = polylineDistance(wx, wz, s.xs(), s.zs(), null);
                    if (d >= f.width()) {
                        continue;
                    }
                    // 斜面は下へいくほどゆるく（くぼんだ形）、尾根は細く
                    double prof = Math.pow(1 - BakeNoise.smooth(d / f.width()), 1.8);
                    double rr = 0.6 * noise.ridged(mx, mz, f.width() * 0.75, 6, s.salt()) + 0.4 * noise.ridged(mx, mz, 260, 5, s.salt() + 5);
                    double add = (f.peak() - base) * prof * (1 - f.rough() + f.rough() * rr) * (0.9 + 0.1 * prof);
                    h = Math.max(h, base + add);
                }
                case "basin" -> {
                    // 盆地はゆがめない座標で（街が真ん中にあるので）
                    double cx = f.center().get(0);
                    double cz = f.center().get(1);
                    double bx = x + 50 * noise.fbm(x, z, 400, 3, s.salt());
                    double bz = z + 50 * noise.fbm(x, z, 400, 3, s.salt() + 1);
                    double rb = Math.hypot(bx - cx, (bz - cz) * 1.12);
                    double w = 1 - BakeNoise.smooth((rb - f.radius()) / Math.max(1, f.width()));
                    if (w > 0) {
                        double floor = f.floor() + 6 * noise.fbm(x, z, 200, 3, s.salt() + 2);
                        h = h * (1 - w) + floor * w;
                    }
                }
                case "gorge" -> {
                    double gx = x + f.meander() * noise.fbm(x, z, 360, 2, s.salt());
                    double gz = z + f.meander() * noise.fbm(x, z, 360, 2, s.salt() + 1);
                    double[] t = new double[1];
                    double d = polylineDistance(gx, gz, s.xs(), s.zs(), t);
                    // 底は幅の半分まで平ら（川と道が並んで通れる）、そこから壁
                    double slot = 1 - BakeNoise.smooth((d - f.width() * 0.5) / (f.width() * 0.5));
                    if (slot > 0) {
                        double floor = f.floor() + (f.floorEnd() - f.floor()) * t[0];
                        h = h * (1 - slot) + Math.min(h, floor) * slot;
                        gorge = Math.max(gorge, slot);
                    }
                }
                case "lake" -> {
                    double cx = f.center().get(0);
                    double cz = f.center().get(1);
                    double e = Math.hypot((x - cx) / f.rx(), (z - cz) / f.rz()) + 0.2 * noise.fbm(x, z, 120, 3, s.salt());
                    if (e < 1) {
                        h -= f.depth() * (1 - BakeNoise.smooth((e - 0.6) / 0.4));
                    }
                }
                default -> {}
            }
        }
        // 細かい不規則さ（水が自然な道筋を見つけられるように）
        h += (h - base) * 0.06 * noise.fbm(x, z, 140, 4, 90) + 1.5 * noise.fbm(x, z, 50, 3, 91);
        gorgeOut[0] = gorge;
        return h;
    }

    /** 折れ線までの距離。t があれば、いちばん近い所が折れ線のどこか（0〜1、長さの割合）を入れる。 */
    static double polylineDistance(double px, double pz, double[] xs, double[] zs, double[] t) {
        double best = Double.MAX_VALUE;
        double total = 0;
        double bestAlong = 0;
        for (int k = 0; k + 1 < xs.length; k++) {
            double vx = xs[k + 1] - xs[k];
            double vz = zs[k + 1] - zs[k];
            double len2 = vx * vx + vz * vz;
            double u = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - xs[k]) * vx + (pz - zs[k]) * vz) / len2));
            double d = Math.hypot(px - xs[k] - u * vx, pz - zs[k] - u * vz);
            double len = Math.sqrt(len2);
            if (d < best) {
                best = d;
                bestAlong = total + u * len;
            }
            total += len;
        }
        if (t != null) {
            t[0] = total == 0 ? 0 : bestAlong / total;
        }
        return best;
    }
}
