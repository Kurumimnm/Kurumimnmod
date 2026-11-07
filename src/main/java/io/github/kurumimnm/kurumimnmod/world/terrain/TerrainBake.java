package io.github.kurumimnm.kurumimnmod.world.terrain;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.imageio.ImageIO;

/**
 * 計算済みの地形 v2（{@link TerrainBaker} の結果）。ワールドのフォルダに保存し、地形の生成中に読む。
 * <ul>
 *   <li>{@link #height}：地面の高さ。マスの間はなめらかに補間し、急な斜面には岩のでこぼこを足す</li>
 *   <li>{@link #waterLevel}：湖・川の水面の高さ（水がなければ NaN）</li>
 *   <li>{@link #roadDistance}：いちばん近い道の中心線までの距離</li>
 * </ul>
 * 範囲の縁（{@link TerrainDesign.Region#blend()}）では、今までの地形（v1）へなめらかにつなぐ。
 */
public final class TerrainBake {

    /** 保存形式の版。計算のしかたを変えたら上げる（古い保存データは計算し直す）。 */
    public static final int FORMAT = 1;
    private static final int MAGIC = 0x4B4D5432; // "KMT2"

    private final TerrainDesign design;
    private final TerrainDesign.Region region;
    private final long seed;
    private final int n;
    private final float[] height;
    private final float[] water;
    private final byte[] river;
    private final List<float[]> roads;
    private final float[] slope;
    private final BakeNoise noise;

    TerrainBake(TerrainDesign design, long seed, float[] height, float[] water, byte[] river, List<float[]> roads) {
        this.design = design;
        this.region = design.region();
        this.seed = seed;
        this.n = region.cells();
        this.height = height;
        this.water = water;
        this.river = river;
        this.roads = roads;
        this.noise = new BakeNoise(seed ^ 0x9E3779B9L);
        // 斜面の角度（度）。岩のでこぼこを足す量に使う
        this.slope = new float[n * n];
        double cell = region.cell();
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                double gx = (height[j * n + Math.min(n - 1, i + 1)] - height[j * n + Math.max(0, i - 1)]) / (2 * cell);
                double gz = (height[Math.min(n - 1, j + 1) * n + i] - height[Math.max(0, j - 1) * n + i]) / (2 * cell);
                slope[j * n + i] = (float) Math.toDegrees(Math.atan(Math.hypot(gx, gz)));
            }
        }
    }

    public TerrainDesign design() {
        return design;
    }

    public long seed() {
        return seed;
    }

    /** この地点が計算した範囲（縁のつなぎ目を含む）の中か。 */
    public boolean covers(double x, double z) {
        return x > region.x0() && z > region.z0() && x < region.x0() + region.size() && z < region.z0() + region.size();
    }

    /** 0（今までの地形）〜1（v2）。 */
    public double weight(double x, double z) {
        return covers(x, z) ? TerrainBaker.edgeWeight(region, x, z) : 0;
    }

    // =====================================================================
    // 読み出し
    // =====================================================================

    /** v2 の地面の高さ（つなぎ目の処理なし）。 */
    public double rawHeight(double x, double z) {
        return bicubic(height, x, z);
    }

    /** 地面の高さ。v1 は今までの地形の高さ（縁でつなぐのに使う）。 */
    public double height(double x, double z, double v1) {
        double w = weight(x, z);
        if (w <= 0) {
            return v1;
        }
        double h = bicubic(height, x, z);
        // 細かい岩肌：急な斜面ほどごつごつ。平らな所はわずかにうねる
        double s = bilinear(slope, x, z);
        double rough = noise.fbm(x, z, 24, 4, 70) * (1.0 + 5.0 * BakeNoise.smooth((s - 25) / 20));
        // 急な岩壁には地層の段（横じま）をつける
        double strata = s > 45 ? Math.sin(h / 3.0) * 0.8 * BakeNoise.smooth((s - 45) / 15) : 0;
        h += rough * 2.0 + strata;
        // 川床：水面より depth だけ下げる
        double r = bilinear01(river, x, z);
        if (r > 0.25) {
            double level = waterLevelRaw(x, z);
            if (!Double.isNaN(level)) {
                double bed = level - design.river().depth();
                double t = BakeNoise.smooth((r - 0.25) / 0.5);
                if (h > bed) {
                    h = h + (bed - h) * t;
                }
            }
        }
        return v1 + (h - v1) * w;
    }

    /** 湖・川の水面（なければ NaN）。縁のつなぎ目では水を置かない。 */
    public double waterLevel(double x, double z) {
        if (weight(x, z) < 0.999) {
            return Double.NaN;
        }
        return waterLevelRaw(x, z);
    }

    private double waterLevelRaw(double x, double z) {
        double fx = (x - region.x0()) / region.cell() - 0.5;
        double fz = (z - region.z0()) / region.cell() - 0.5;
        int i = (int) Math.floor(fx);
        int j = (int) Math.floor(fz);
        double u = fx - i;
        double v = fz - j;
        double sum = 0;
        double weight = 0;
        for (int dj = 0; dj <= 1; dj++) {
            for (int di = 0; di <= 1; di++) {
                float level = at(water, i + di, j + dj);
                if (!Float.isNaN(level)) {
                    double w = (di == 0 ? 1 - u : u) * (dj == 0 ? 1 - v : v);
                    sum += level * w;
                    weight += w;
                }
            }
        }
        return weight < 0.3 ? Double.NaN : sum / weight;
    }

    /** 山のバイオーム（岩肌・雪線より上は雪）にするか。 */
    public boolean isMountain(double x, double z) {
        return weight(x, z) >= 0.5 && rawHeight(x, z) > design.mountainBiomeAbove() + 15 * noise.fbm(x, z, 90, 2, 71);
    }

    /** いちばん近い道の中心線までの距離（道がなければ大きな値）。 */
    public double roadDistance(double x, double z) {
        double best = Double.MAX_VALUE;
        for (float[] line : roads) {
            for (int k = 0; k + 3 < line.length; k += 2) {
                double ax = line[k];
                double az = line[k + 1];
                double bx = line[k + 2];
                double bz = line[k + 3];
                if (x < Math.min(ax, bx) - 8 || x > Math.max(ax, bx) + 8 || z < Math.min(az, bz) - 8 || z > Math.max(az, bz) + 8) {
                    continue;
                }
                double vx = bx - ax;
                double vz = bz - az;
                double len2 = vx * vx + vz * vz;
                double u = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((x - ax) * vx + (z - az) * vz) / len2));
                best = Math.min(best, Math.hypot(x - ax - u * vx, z - az - u * vz));
            }
        }
        return best;
    }

    // ---- 補間 ----

    private float at(float[] grid, int i, int j) {
        i = Math.max(0, Math.min(n - 1, i));
        j = Math.max(0, Math.min(n - 1, j));
        return grid[j * n + i];
    }

    private double bilinear(float[] grid, double x, double z) {
        double fx = (x - region.x0()) / region.cell() - 0.5;
        double fz = (z - region.z0()) / region.cell() - 0.5;
        int i = (int) Math.floor(fx);
        int j = (int) Math.floor(fz);
        double u = fx - i;
        double v = fz - j;
        double a = at(grid, i, j) + (at(grid, i + 1, j) - at(grid, i, j)) * u;
        double b = at(grid, i, j + 1) + (at(grid, i + 1, j + 1) - at(grid, i, j + 1)) * u;
        return a + (b - a) * v;
    }

    private double bilinear01(byte[] grid, double x, double z) {
        double fx = (x - region.x0()) / region.cell() - 0.5;
        double fz = (z - region.z0()) / region.cell() - 0.5;
        int i = (int) Math.floor(fx);
        int j = (int) Math.floor(fz);
        double u = fx - i;
        double v = fz - j;
        double a = b(grid, i, j) + (b(grid, i + 1, j) - b(grid, i, j)) * u;
        double c = b(grid, i, j + 1) + (b(grid, i + 1, j + 1) - b(grid, i, j + 1)) * u;
        return a + (c - a) * v;
    }

    private int b(byte[] grid, int i, int j) {
        i = Math.max(0, Math.min(n - 1, i));
        j = Math.max(0, Math.min(n - 1, j));
        return grid[j * n + i];
    }

    /** なめらかな補間（Catmull-Rom）。 */
    private double bicubic(float[] grid, double x, double z) {
        double fx = (x - region.x0()) / region.cell() - 0.5;
        double fz = (z - region.z0()) / region.cell() - 0.5;
        int i = (int) Math.floor(fx);
        int j = (int) Math.floor(fz);
        double u = fx - i;
        double v = fz - j;
        double[] col = new double[4];
        for (int m = -1; m <= 2; m++) {
            col[m + 1] = cubic(at(grid, i - 1, j + m), at(grid, i, j + m), at(grid, i + 1, j + m), at(grid, i + 2, j + m), u);
        }
        return cubic(col[0], col[1], col[2], col[3], v);
    }

    private static double cubic(double p0, double p1, double p2, double p3, double t) {
        return p1 + 0.5 * t * (p2 - p0 + t * (2 * p0 - 5 * p1 + 4 * p2 - p3 + t * (3 * (p1 - p2) + p3 - p0)));
    }

    // =====================================================================
    // 保存・読み込み
    // =====================================================================

    public void save(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (OutputStream raw = Files.newOutputStream(tmp);
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(raw)))) {
            out.writeInt(MAGIC);
            out.writeInt(FORMAT);
            out.writeLong(seed);
            out.writeInt(design.hashCode());
            out.writeInt(n);
            for (float v : height) {
                out.writeFloat(v);
            }
            for (float v : water) {
                out.writeFloat(v);
            }
            out.write(river);
            out.writeInt(roads.size());
            for (float[] line : roads) {
                out.writeInt(line.length);
                for (float v : line) {
                    out.writeFloat(v);
                }
            }
        }
        Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /** 保存データを読む。シード・設計図・形式が違えば null（計算し直す）。 */
    public static TerrainBake load(Path file, TerrainDesign design, long seed) throws IOException {
        if (!Files.exists(file)) {
            return null;
        }
        try (InputStream raw = Files.newInputStream(file);
             DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(raw)))) {
            if (in.readInt() != MAGIC || in.readInt() != FORMAT || in.readLong() != seed
                    || in.readInt() != design.hashCode() || in.readInt() != design.region().cells()) {
                return null;
            }
            int n = design.region().cells();
            float[] height = new float[n * n];
            float[] water = new float[n * n];
            byte[] river = new byte[n * n];
            for (int k = 0; k < height.length; k++) {
                height[k] = in.readFloat();
            }
            for (int k = 0; k < water.length; k++) {
                water[k] = in.readFloat();
            }
            in.readFully(river);
            int count = in.readInt();
            List<float[]> roads = new ArrayList<>();
            for (int r = 0; r < count; r++) {
                float[] line = new float[in.readInt()];
                for (int k = 0; k < line.length; k++) {
                    line[k] = in.readFloat();
                }
                roads.add(line);
            }
            return new TerrainBake(design, seed, height, water, river, roads);
        }
    }

    /** 確認用の画像（上から見た地図：陰影・高さの色・水・道）。 */
    public void writePreview(Path file) throws IOException {
        BufferedImage image = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
        double cell = region.cell();
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int idx = j * n + i;
                double h = height[idx];
                double gx = (at(height, i + 1, j) - at(height, i - 1, j)) / (2 * cell);
                double gz = (at(height, i, j + 1) - at(height, i, j - 1)) / (2 * cell);
                // 北西からの光
                double shade = Math.max(0, Math.min(1, (0.6 + (-gx * -0.5 + -gz * -0.5)) / Math.sqrt(1 + gx * gx + gz * gz)));
                int rgb;
                if (!Float.isNaN(water[idx])) {
                    rgb = river[idx] == 1 ? 0x3E78C8 : 0x2F5FA8;
                } else {
                    double t = Math.max(0, Math.min(1, (h - 70) / 340));
                    int r = (int) (90 + 120 * t);
                    int g = (int) (140 + 60 * t - 40 * t * t);
                    int b = (int) (70 + 150 * t * t);
                    if (h > 330) {
                        r = g = b = 235;
                    }
                    double k = 0.35 + 0.85 * shade;
                    rgb = (clamp255(r * k) << 16) | (clamp255(g * k) << 8) | clamp255(b * k);
                }
                image.setRGB(i, j, rgb);
            }
        }
        for (float[] line : roads) {
            for (int k = 0; k + 1 < line.length; k += 2) {
                int i = (int) ((line[k] - region.x0()) / cell);
                int j = (int) ((line[k + 1] - region.z0()) / cell);
                if (i >= 0 && j >= 0 && i < n && j < n) {
                    image.setRGB(i, j, 0xF4E2A0);
                }
            }
        }
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "png", file.toFile());
    }

    private static int clamp255(double v) {
        return (int) Math.max(0, Math.min(255, v));
    }
}
