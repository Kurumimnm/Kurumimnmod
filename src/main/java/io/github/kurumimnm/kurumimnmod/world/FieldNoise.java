package io.github.kurumimnm.kurumimnmod.world;

/**
 * フィールドの境界をぐにゃっと自然な形にするための、軽いノイズ。
 * <p>
 * ワールドのシード値に関係なく、どのワールドでも同じ形になる（「骨組みは固定」の方針）。
 * 設計時に地図の確認用画像を作るスクリプト（Python）と同じ計算式なので、式を変えるときは両方そろえること。
 */
public final class FieldNoise {

    private FieldNoise() {}

    /** 整数の格子点ごとに -1〜1 の値を返すハッシュ。 */
    private static double hash(long ix, long iz, long salt) {
        long h = ix * 0x9E3779B97F4A7C15L + iz * 0xC2B2AE3D27D4EB4FL + salt * 0x165667B19E3779F9L;
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 29)) * 0x94D049BB133111EBL;
        h = h ^ (h >>> 32);
        return ((h >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
    }

    /** なめらかな値ノイズ（格子点の値を補間）。 */
    private static double valueNoise(double x, double z, long salt) {
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

    /** 大きな揺らぎ＋細かい揺らぎの 2 段重ね。 */
    public static double fbm(double x, double z, long salt) {
        return valueNoise(x, z, salt) * 0.65 + valueNoise(x * 2.3, z * 2.3, salt + 7) * 0.35;
    }
}
