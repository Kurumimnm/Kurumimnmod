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
 *   <li><b>設計の高さ</b>：設計図の部品（海岸・地域の高さ・台地・山並み・火山・盆地・峡谷…）を重ねる。
 *       山並みなどはシード値で形が少し変わる</li>
 *   <li><b>侵食</b>：雨水の流れを計算し、水が集まる所ほど地面を削る（谷・支尾根ができる）。急すぎる斜面は崩す。
 *       台地の崖・峡谷の壁は崩さない</li>
 *   <li><b>水</b>：海、窪みの湖、水が集まる所の川、設計した川（紅河・翠河など）</li>
 *   <li><b>道</b>：坂がゆるく、水と崖を避ける道筋を探す（橋は設計どおりにまっすぐ渡る）</li>
 * </ol>
 * 結果は {@link TerrainBake}。
 */
public final class TerrainBaker {

    private TerrainBaker() {}

    /** 細かい決まり（設計図で変えるほどではないもの）。 */
    static final class Limits {
        /** これより小さい窪み（マス数）は湖にせず埋める。 */
        static final int LAKE_MIN_CELLS = 24;
        /** これより浅い窪み（ブロック）は湖にせず埋める。 */
        static final double LAKE_MIN_DEPTH = 2.0;
        /** 地形の上限（Y）。 */
        static final double MAX_HEIGHT = 300.0;
    }

    /** 道（街と街を結ぶ点の列。straight[k] が true なら k → k+1 はまっすぐ＝橋）。 */
    public record RoadRequest(List<double[]> points, List<Boolean> straight) {}

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
        Design shapes = Design.prepare(design, random);

        // ---- 1. 設計の高さ ----
        float[] design0 = new float[n * n];
        byte[] mask = new byte[n * n];
        java.util.stream.IntStream.range(0, n).parallel().forEach(j -> {
            int[] m = new int[1];
            for (int i = 0; i < n; i++) {
                double x = region.x0() + (i + 0.5) * cell;
                double z = region.z0() + (j + 0.5) * cell;
                m[0] = 0;
                double h = shapes.height(design, noise, x, z, m, true);
                double w = edgeWeight(region, x, z);
                if (w < 1) {
                    h = v1Height.applyAsDouble(x, z) * (1 - w) + h * w;
                }
                design0[j * n + i] = (float) Math.min(h, Limits.MAX_HEIGHT);
                mask[j * n + i] = (byte) m[0];
            }
        });
        boolean[] pad = new boolean[n * n];
        for (TownPad town : towns) {
            int ci = (int) Math.floor((town.x() - region.x0()) / cell);
            int cj = (int) Math.floor((town.z() - region.z0()) / cell);
            if (ci < 0 || cj < 0 || ci >= n || cj >= n) {
                continue;
            }
            float level = design0[cj * n + ci] + 2;
            int r = (int) Math.ceil((town.radius() + 24) / cell);
            for (int dj = -r; dj <= r; dj++) {
                for (int di = -r; di <= r; di++) {
                    int ii = ci + di;
                    int jj = cj + dj;
                    if (ii < 0 || jj < 0 || ii >= n || jj >= n || Math.hypot(di, dj) * cell > town.radius() + 24) {
                        continue;
                    }
                    int idx = jj * n + ii;
                    if ((mask[idx] & Design.DRY) == 0) {
                        design0[idx] = Math.max(design0[idx], level);
                    }
                    pad[idx] = true;
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
        boolean[] keep = new boolean[n * n];
        double[] kField = new double[n * n];
        float[] talus = new float[n * n];
        TerrainDesign.Erosion er = design.erosion();
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int idx = j * n + i;
                double x = region.x0() + (i + 0.5) * cell;
                double z = region.z0() + (j + 0.5) * cell;
                boolean hard = (mask[idx] & (Design.HARD | Design.DRY)) != 0;
                keep[idx] = pad[idx] || hard;
                boolean sea = (mask[idx] & Design.SEA) != 0;
                kField[idx] = keep[idx] || sea ? 0 : er.k() * (0.6 + 0.8 * BakeNoise.smooth(0.5 + noise.fbm(x, z, 700, 3, 21)));
                double angle = (mask[idx] & Design.GORGE) != 0 ? er.talusGorge()
                        : design0[idx] > er.highAbove() ? er.talusHigh() : er.talus();
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
                h[idx] -= Math.max(0, filled[idx] - eroded[idx]);
            }
            Hydro.thermal(h, fixed, keep, talus, n, 2);
        }
        log.info("[地形v2] 侵食 {} 回: {} ms", er.iterations(), System.currentTimeMillis() - start);

        // ---- 3. 水 ----
        Hydro.priorityFill(h, fixed, n, 0.0f, filled);
        float[] routing = new float[n * n];
        Hydro.priorityFill(h, fixed, n, 0.02f, routing);
        Hydro.receivers(routing, fixed, n, cell, null, 0f, receiver, distance);
        Hydro.order(routing, order);
        Hydro.accumulate(receiver, order, area, cell * cell);
        float[] water = new float[n * n];
        byte[] river = new byte[n * n];
        Arrays.fill(water, Float.NaN);
        double seaLevel = shapes.seaLevel();
        // 海
        for (int idx = 0; idx < n * n; idx++) {
            if ((mask[idx] & Design.SEA) != 0 && h[idx] < seaLevel) {
                water[idx] = (float) seaLevel;
            }
        }
        // 湖：窪みにたまる水。小さい・浅い・高すぎる窪みは埋める。設計した窪み（峡谷・盆地・裂け目など）はそのまま残す
        lakes(h, filled, water, mask, pad, n, design);
        // 水が集まる所の川
        double minArea = design.river().minCells() * cell * cell;
        for (int idx = 0; idx < n * n; idx++) {
            if (!pad[idx] && Float.isNaN(water[idx]) && area[idx] > minArea && !fixed[idx] && (mask[idx] & Design.DRY) == 0) {
                river[idx] = 1;
                water[idx] = h[idx] - 0.4f;
            }
        }
        log.info("[地形v2] 水: {} ms", System.currentTimeMillis() - start);

        // ---- 4. 道 ----
        List<float[]> roadLines = new ArrayList<>();
        for (RoadRequest request : roads) {
            float[] line = Hydro.road(h, water, mask, n, region, request, noise);
            if (line != null) {
                roadLines.add(line);
            }
        }
        log.info("[地形v2] 道 {} 本: {} ms", roadLines.size(), System.currentTimeMillis() - start);

        return new TerrainBake(design, seed, h, water, river, roadLines);
    }

    private static void lakes(float[] h, float[] filled, float[] water, byte[] mask, boolean[] pad, int n, TerrainDesign design) {
        boolean[] lakeCell = new boolean[n * n];
        for (int idx = 0; idx < n * n; idx++) {
            lakeCell[idx] = !pad[idx] && Float.isNaN(water[idx]) && (mask[idx] & Design.SEA) == 0 && filled[idx] - h[idx] > 0.5f;
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
            boolean designed = false;
            boolean lakeShape = false;
            while (top > 0) {
                int idx = stack[--top];
                component.add(idx);
                maxDepth = Math.max(maxDepth, filled[idx] - h[idx]);
                designed |= (mask[idx] & Design.DRY) != 0;
                lakeShape |= (mask[idx] & Design.LAKE) != 0;
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
            if (designed && !lakeShape) {
                continue; // 峡谷・裂け目・盆地などの設計した窪みは、水を張らずにそのまま
            }
            boolean keep = lakeShape || (component.size() >= Limits.LAKE_MIN_CELLS && maxDepth >= Limits.LAKE_MIN_DEPTH
                    && filled[start0] < design.mountainBiomeAbove());
            for (int idx : component) {
                if (keep) {
                    water[idx] = filled[idx];
                } else {
                    h[idx] = filled[idx];
                }
            }
        }
    }

    /** 縁からの距離で 0（範囲の外の地形）〜1（v2）。 */
    public static double edgeWeight(TerrainDesign.Region region, double x, double z) {
        double d = Math.min(Math.min(x - region.x0(), region.x0() + region.size() - x),
                Math.min(z - region.z0(), region.z0() + region.size() - z));
        return BakeNoise.smooth(d / region.blend());
    }

    static double polylineDistance(double px, double pz, double[] xs, double[] zs, double[] t) {
        return Shapes2D.polylineDistance(px, pz, xs, zs, t);
    }
}
