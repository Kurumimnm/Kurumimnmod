package io.github.kurumimnm.kurumimnmod.world;

/** 地図の計算で使う小さな幾何の関数。 */
final class Geometry {

    private Geometry() {}

    /** 点 (x, z) から線分 (ax, az)-(bx, bz) までの距離。 */
    static double distanceToSegment(double x, double z, double ax, double az, double bx, double bz) {
        double vx = bx - ax;
        double vz = bz - az;
        double lengthSq = vx * vx + vz * vz;
        double t = lengthSq == 0 ? 0 : ((x - ax) * vx + (z - az) * vz) / lengthSq;
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(x - (ax + vx * t), z - (az + vz * t));
    }
}
