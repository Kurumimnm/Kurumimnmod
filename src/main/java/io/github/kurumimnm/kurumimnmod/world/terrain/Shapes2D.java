package io.github.kurumimnm.kurumimnmod.world.terrain;

import io.github.kurumimnm.kurumimnmod.world.FieldNoise;
import java.util.List;

/**
 * 地形の部品の形（多角形・折れ線）の計算。シード値に関係しない（海岸線は地図とフィールドの判定でも同じ形を使う）。
 */
public final class Shapes2D {

    private Shapes2D() {}

    /** 点の列を配列にする。 */
    public static double[][] toArrays(List<List<Double>> points) {
        double[] xs = new double[points.size()];
        double[] zs = new double[points.size()];
        for (int k = 0; k < points.size(); k++) {
            xs[k] = points.get(k).get(0);
            zs[k] = points.get(k).get(1);
        }
        return new double[][]{xs, zs};
    }

    /** 多角形までの距離（中なら正、外なら負）。 */
    public static double signedPolygonDistance(double px, double pz, double[] xs, double[] zs) {
        boolean inside = false;
        double best = Double.MAX_VALUE;
        int n = xs.length;
        for (int a = 0, b = n - 1; a < n; b = a++) {
            double ax = xs[a];
            double az = zs[a];
            double bx = xs[b];
            double bz = zs[b];
            if ((az > pz) != (bz > pz) && px < (bx - ax) * (pz - az) / (bz - az) + ax) {
                inside = !inside;
            }
            double vx = bx - ax;
            double vz = bz - az;
            double len2 = vx * vx + vz * vz;
            double u = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * vx + (pz - az) * vz) / len2));
            double dx = px - ax - u * vx;
            double dz = pz - az - u * vz;
            best = Math.min(best, dx * dx + dz * dz);
        }
        double d = Math.sqrt(best);
        return inside ? d : -d;
    }

    /** 海岸線からの距離（陸なら正、海なら負）。輪郭に細かいゆらぎ（入り江・岬）を足す。 */
    public static double coastDistance(double x, double z, double[] xs, double[] zs) {
        double wiggle = 90 * FieldNoise.fbm(x / 420.0, z / 420.0, 4401) + 35 * FieldNoise.fbm(x / 120.0, z / 120.0, 4402);
        return signedPolygonDistance(x, z, xs, zs) + wiggle;
    }

    /** 折れ線までの距離。t があれば、いちばん近い所が折れ線のどこか（0〜1、長さの割合）を入れる。 */
    public static double polylineDistance(double px, double pz, double[] xs, double[] zs, double[] t) {
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

    /** 折れ線の外接矩形（[minX, minZ, maxX, maxZ]）。 */
    public static double[] bounds(double[] xs, double[] zs, double margin) {
        double minX = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (int k = 0; k < xs.length; k++) {
            minX = Math.min(minX, xs[k]);
            maxX = Math.max(maxX, xs[k]);
            minZ = Math.min(minZ, zs[k]);
            maxZ = Math.max(maxZ, zs[k]);
        }
        return new double[]{minX - margin, minZ - margin, maxX + margin, maxZ + margin};
    }

    public static boolean inBounds(double[] b, double x, double z) {
        return x >= b[0] && z >= b[1] && x <= b[2] && z <= b[3];
    }
}
