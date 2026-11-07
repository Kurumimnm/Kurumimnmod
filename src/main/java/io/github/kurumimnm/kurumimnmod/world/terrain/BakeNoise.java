package io.github.kurumimnm.kurumimnmod.world.terrain;

/**
 * 地形 v2 の計算で使うノイズ（シード値で変わる）。
 * <p>
 * 値ノイズ（格子点に乱数を置いてなめらかにつなぐ）を何段も重ねる fbm と、尾根のような鋭い線ができる ridged。
 * 試作（tools/terrain_v2_proto）と同じ考え方。
 */
public final class BakeNoise {

    private final long seed;

    public BakeNoise(long seed) {
        this.seed = seed;
    }

    /** 整数の格子点ごとに -1〜1。 */
    private double hash(long ix, long iz, long salt) {
        long h = ix * 0x9E3779B97F4A7C15L + iz * 0xC2B2AE3D27D4EB4FL + (salt + seed * 0x2545F4914F6CDD1DL) * 0x165667B19E3779F9L;
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 29)) * 0x94D049BB133111EBL;
        h = h ^ (h >>> 32);
        return ((h >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
    }

    /** なめらかな値ノイズ（-1〜1）。 */
    public double value(double x, double z, long salt) {
        long ix = (long) Math.floor(x);
        long iz = (long) Math.floor(z);
        double fx = x - ix;
        double fz = z - iz;
        double sx = fx * fx * (3 - 2 * fx);
        double sz = fz * fz * (3 - 2 * fz);
        double a = hash(ix, iz, salt);
        double b = hash(ix + 1, iz, salt);
        double c = hash(ix, iz + 1, salt);
        double d = hash(ix + 1, iz + 1, salt);
        double top = a + (b - a) * sx;
        double bottom = c + (d - c) * sx;
        return top + (bottom - top) * sz;
    }

    /** 何段も重ねたノイズ（おおよそ -1〜1）。scale＝いちばん大きな波の大きさ（ブロック）。 */
    public double fbm(double x, double z, double scale, int octaves, long salt) {
        double sum = 0;
        double amp = 1;
        double total = 0;
        for (int o = 0; o < octaves; o++) {
            sum += amp * value(x / scale, z / scale, salt + o * 17L);
            total += amp;
            amp *= 0.5;
            scale /= 2.0;
        }
        return sum / total;
    }

    /** 尾根のような鋭い線ができるノイズ（0〜1）。山の支尾根・谷の模様に使う。 */
    public double ridged(double x, double z, double scale, int octaves, long salt) {
        double sum = 0;
        double amp = 1;
        double total = 0;
        double prev = 1;
        for (int o = 0; o < octaves; o++) {
            double v = 1 - Math.abs(value(x / scale, z / scale, salt + o * 31L));
            v = v * v * prev;
            sum += amp * v;
            total += amp;
            prev = Math.min(1, Math.max(0, v * 1.6));
            amp *= 0.5;
            scale /= 2.1;
        }
        return sum / total;
    }

    /** 0〜1 になめらかにつなぐ。 */
    public static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }
}
