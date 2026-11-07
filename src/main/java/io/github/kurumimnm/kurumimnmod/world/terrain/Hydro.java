package io.github.kurumimnm.kurumimnmod.world.terrain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 水の流れと侵食の計算（{@link TerrainBaker} の部品）。格子は n×n、添字は j*n+i。
 */
final class Hydro {

    private Hydro() {}

    private static final int[] DI = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] DJ = {-1, -1, -1, 0, 0, 1, 1, 1};

    // ---- 小さな優先度つき待ち行列（高さの低い順）。箱詰めしない配列版 ----
    private static final class Heap {
        float[] key = new float[1 << 16];
        int[] val = new int[1 << 16];
        int size;

        void push(float k, int v) {
            if (size == key.length) {
                key = Arrays.copyOf(key, size * 2);
                val = Arrays.copyOf(val, size * 2);
            }
            int c = size++;
            while (c > 0) {
                int p = (c - 1) >> 1;
                if (key[p] <= k) {
                    break;
                }
                key[c] = key[p];
                val[c] = val[p];
                c = p;
            }
            key[c] = k;
            val[c] = v;
        }

        int pop() {
            int top = val[0];
            float k = key[--size];
            int v = val[size];
            int c = 0;
            while (true) {
                int l = 2 * c + 1;
                if (l >= size) {
                    break;
                }
                int r = l + 1;
                int m = r < size && key[r] < key[l] ? r : l;
                if (key[m] >= k) {
                    break;
                }
                key[c] = key[m];
                val[c] = val[m];
                c = m;
            }
            key[c] = k;
            val[c] = v;
            return top;
        }
    }

    /** 窪みを埋めた高さ（水がどこかへ流れ出られる高さ）。eps＞0 なら埋めた所にもわずかな傾きをつける。 */
    static void priorityFill(float[] h, boolean[] fixed, int n, float eps, float[] out) {
        System.arraycopy(h, 0, out, 0, h.length);
        boolean[] closed = new boolean[h.length];
        Heap heap = new Heap();
        for (int idx = 0; idx < h.length; idx++) {
            if (fixed[idx]) {
                heap.push(out[idx], idx);
                closed[idx] = true;
            }
        }
        while (heap.size > 0) {
            int idx = heap.pop();
            float hv = out[idx];
            int i = idx % n;
            int j = idx / n;
            for (int k = 0; k < 8; k++) {
                int ii = i + DI[k];
                int jj = j + DJ[k];
                if (ii < 0 || jj < 0 || ii >= n || jj >= n) {
                    continue;
                }
                int nb = jj * n + ii;
                if (closed[nb]) {
                    continue;
                }
                closed[nb] = true;
                if (out[nb] <= hv + eps) {
                    out[nb] = hv + eps;
                }
                heap.push(out[nb], nb);
            }
        }
    }

    /**
     * 流れる先のマス。下っている隣のマスから、急なほど選ばれやすいように乱数で選ぶ
     * （いつも一番急な方へ流すと、斜面にまっすぐな筋が並んでしまうため）。random が null なら一番急な方。
     */
    static void receivers(float[] h, boolean[] fixed, int n, double cell, Random random, float jitter,
                          int[] receiver, float[] distance) {
        double[] weight = new double[8];
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int idx = j * n + i;
                receiver[idx] = idx;
                distance[idx] = (float) cell;
                if (fixed[idx]) {
                    continue;
                }
                double total = 0;
                double best = 0;
                int bestK = -1;
                for (int k = 0; k < 8; k++) {
                    int ii = i + DI[k];
                    int jj = j + DJ[k];
                    weight[k] = 0;
                    if (ii < 0 || jj < 0 || ii >= n || jj >= n) {
                        continue;
                    }
                    double d = (DI[k] != 0 && DJ[k] != 0) ? cell * 1.41421356 : cell;
                    double s = (h[idx] - h[jj * n + ii]) / d;
                    if (s > 0) {
                        weight[k] = s * s;
                        total += weight[k];
                        if (s > best) {
                            best = s;
                            bestK = k;
                        }
                    }
                }
                if (bestK < 0) {
                    continue;
                }
                int pick = bestK;
                if (random != null && jitter > 0) {
                    double r = random.nextDouble() * total;
                    for (int k = 0; k < 8; k++) {
                        r -= weight[k];
                        if (weight[k] > 0 && r <= 0) {
                            pick = k;
                            break;
                        }
                    }
                }
                receiver[idx] = (j + DJ[pick]) * n + i + DI[pick];
                distance[idx] = (float) ((DI[pick] != 0 && DJ[pick] != 0) ? cell * 1.41421356 : cell);
            }
        }
    }

    /** 低い順の並び（下流 → 上流）。 */
    static void order(float[] h, int[] out) {
        long[] keys = new long[h.length];
        for (int idx = 0; idx < h.length; idx++) {
            // 正の float はビット列の大小と値の大小が同じ
            keys[idx] = ((long) Float.floatToIntBits(Math.max(h[idx], 0f)) << 32) | idx;
        }
        Arrays.parallelSort(keys);
        for (int t = 0; t < keys.length; t++) {
            out[t] = (int) keys[t];
        }
    }

    /** 集水面積（そのマスより上流から水が集まる広さ）。 */
    static void accumulate(int[] receiver, int[] order, double[] area, double cellArea) {
        Arrays.fill(area, cellArea);
        for (int t = order.length - 1; t >= 0; t--) {
            int idx = order[t];
            int r = receiver[idx];
            if (r != idx) {
                area[r] += area[idx];
            }
        }
    }

    /** 川による侵食（暗黙解法。集水面積が大きいほど強く削る）。 */
    static void streamPower(float[] h, int[] receiver, float[] distance, int[] order, double[] k, double[] area, double cellArea) {
        accumulate(receiver, order, area, cellArea);
        for (int idx : order) {
            int r = receiver[idx];
            if (r != idx && k[idx] > 0) {
                double f = k[idx] * Math.sqrt(area[idx]) / distance[idx];
                h[idx] = (float) ((h[idx] + f * h[r]) / (1 + f));
            }
        }
    }

    /** 急すぎる斜面を崩す（talus＝1 マスで許される高さの差）。斜めの隣も見るので、格子の向きの筋ができにくい。 */
    static void thermal(float[] h, boolean[] fixed, boolean[] keep, float[] talus, int n, int sweeps) {
        for (int s = 0; s < sweeps; s++) {
            // 向きの偏りが出ないよう、往復で調べる
            boolean reverse = (s & 1) == 1;
            for (int jj0 = 1; jj0 < n - 1; jj0++) {
                int j = reverse ? n - 1 - jj0 : jj0;
                for (int ii0 = 1; ii0 < n - 1; ii0++) {
                    int i = reverse ? n - 1 - ii0 : ii0;
                    int idx = j * n + i;
                    if (fixed[idx] || keep[idx]) {
                        continue;
                    }
                    for (int k = 0; k < 8; k++) {
                        int nb = (j + DJ[k]) * n + i + DI[k];
                        float limit = (DI[k] != 0 && DJ[k] != 0) ? talus[idx] * 1.41421356f : talus[idx];
                        float d = h[idx] - h[nb];
                        if (d > limit) {
                            float move = 0.25f * (d - limit);
                            h[idx] -= move;
                            if (!fixed[nb] && !keep[nb]) {
                                h[nb] += move;
                            }
                        }
                    }
                }
            }
        }
    }

    // =====================================================================
    // 道（経路探索）
    // =====================================================================

    /** 道の点の列をつないだ折れ線（ブロック座標 [x0, z0, x1, z1, ...]）。区間ごとに経路探索（橋はまっすぐ）。 */
    static float[] road(float[] h, float[] water, byte[] mask, int n, TerrainDesign.Region region,
                        TerrainBaker.RoadRequest request, BakeNoise noise) {
        List<double[]> all = new ArrayList<>();
        List<double[]> pts = request.points();
        for (int k = 0; k + 1 < pts.size(); k++) {
            List<double[]> part;
            if (request.straight().get(k)) {
                part = new ArrayList<>();
                part.add(pts.get(k));
                part.add(pts.get(k + 1));
            } else {
                part = segment(h, water, mask, n, region, pts.get(k), pts.get(k + 1), noise);
                if (part == null) {
                    part = new ArrayList<>();
                    part.add(pts.get(k));
                    part.add(pts.get(k + 1));
                }
            }
            if (!all.isEmpty()) {
                part.remove(0);
            }
            all.addAll(part);
        }
        float[] line = new float[all.size() * 2];
        for (int k = 0; k < all.size(); k++) {
            line[2 * k] = (float) all.get(k)[0];
            line[2 * k + 1] = (float) all.get(k)[1];
        }
        return line;
    }

    /** 1 区間の経路探索。坂・水・崖・峡谷を避ける。 */
    private static List<double[]> segment(float[] h, float[] water, byte[] mask, int n, TerrainDesign.Region region,
                                          double[] from, double[] to, BakeNoise noise) {
        double cell = region.cell();
        int si = clamp((int) ((from[0] - region.x0()) / cell), n);
        int sj = clamp((int) ((from[1] - region.z0()) / cell), n);
        int ti = clamp((int) ((to[0] - region.x0()) / cell), n);
        int tj = clamp((int) ((to[1] - region.z0()) / cell), n);
        double[] g = new double[n * n];
        Arrays.fill(g, Double.MAX_VALUE);
        int[] prev = new int[n * n];
        Arrays.fill(prev, -1);
        Heap open = new Heap();
        int start = sj * n + si;
        int goal = tj * n + ti;
        g[start] = 0;
        open.push(0, start);
        boolean[] done = new boolean[n * n];
        while (open.size > 0) {
            int idx = open.pop();
            if (done[idx]) {
                continue;
            }
            done[idx] = true;
            if (idx == goal) {
                break;
            }
            int i = idx % n;
            int j = idx / n;
            for (int k = 0; k < 8; k++) {
                int ii = i + DI[k];
                int jj = j + DJ[k];
                if (ii < 0 || jj < 0 || ii >= n || jj >= n) {
                    continue;
                }
                int nb = jj * n + ii;
                double d = (DI[k] != 0 && DJ[k] != 0) ? cell * 1.41421356 : cell;
                double grade = Math.abs(h[nb] - h[idx]) / d;
                double x = region.x0() + (ii + 0.5) * cell;
                double z = region.z0() + (jj + 0.5) * cell;
                double c = d * (1 + 30 * grade * grade + (grade > 0.35 ? 200 : 0) + 0.25 * (1 + noise.fbm(x, z, 160, 2, 777)));
                if (!Float.isNaN(water[nb])) {
                    c += 40 * d;
                }
                if ((mask[nb] & (Design.GORGE | Design.SEA)) != 0) {
                    c += 400 * d; // 峡谷の壁・海は通らない（橋で渡る）
                }
                double ng = g[idx] + c;
                if (ng < g[nb]) {
                    g[nb] = ng;
                    prev[nb] = idx;
                    double hx = (ti - ii) * cell;
                    double hz = (tj - jj) * cell;
                    open.push((float) (ng + Math.sqrt(hx * hx + hz * hz)), nb);
                }
            }
        }
        if (prev[goal] == -1 && goal != start) {
            return null;
        }
        List<double[]> pts = new ArrayList<>();
        for (int cur = goal; cur != -1; cur = prev[cur]) {
            pts.add(0, new double[]{region.x0() + (cur % n + 0.5) * cell, region.z0() + (cur / n + 0.5) * cell});
        }
        pts.set(0, from);
        if (pts.size() == 1) {
            pts.add(to);
        } else {
            pts.set(pts.size() - 1, to);
        }
        // 角を丸める（チャイキン法 2 回）
        for (int pass = 0; pass < 2; pass++) {
            List<double[]> next = new ArrayList<>();
            next.add(pts.get(0));
            for (int k = 0; k + 1 < pts.size(); k++) {
                double[] a = pts.get(k);
                double[] b = pts.get(k + 1);
                next.add(new double[]{a[0] * 0.75 + b[0] * 0.25, a[1] * 0.75 + b[1] * 0.25});
                next.add(new double[]{a[0] * 0.25 + b[0] * 0.75, a[1] * 0.25 + b[1] * 0.75});
            }
            next.add(pts.get(pts.size() - 1));
            pts = next;
        }
        return pts;
    }

    private static int clamp(int v, int n) {
        return Math.max(1, Math.min(n - 2, v));
    }
}
