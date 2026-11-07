package io.github.kurumimnm.kurumimnmod.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/**
 * 山脈の設定（地図 JSON の {@code mountains}）。
 * <p>
 * 尾根線（ridges）からの距離で「山の中の位置 p」（0 = ふもと、1 = 尾根の中心）を求め、そこから高さを決める。
 * 断面は「ふもとの絶壁 → 険しい斜面 → 頂上 → 反対側の斜面」で、場所ごとに形が変わる。
 * <ul>
 *   <li>foot_wall … ふもとの絶壁の高さ（場所ごとに min〜max。0 なら絶壁なしでそのまま斜面）</li>
 *   <li>peak … 頂上の高さ（ふもとの地面から何ブロック上か。場所ごとに min〜max、scale は変化の波の大きさ）</li>
 *   <li>crest_shift … 頂上の位置を尾根の中心からずらす量（0〜1）。ずれた側は急に、反対側はなだらかになる</li>
 *   <li>roughness … 斜面のでこぼこ（amplitude ブロック、scale ブロック。複数重ねられる）</li>
 *   <li>shelves … ところどころにできる平らな段。coverage = 段ができる場所の割合（0〜1）、
 *       level = 段の高さ（ふもと〜頂上の何割か）、band = 段の奥の絶壁の高さ、scale = 段がある場所の波の大きさ</li>
 *   <li>jitter … ふもとの線をぐにゃっとさせる量</li>
 * </ul>
 *
 * @param biome          山の中のバイオーム
 * @param biomeThreshold この p より内側を山のバイオームにする
 * @param footRatio      ふもとの絶壁の幅（p の割合）
 */
public record MountainSettings(Holder<Biome> biome, double biomeThreshold, Range footWall, double footRatio,
                               Peak peak, double crestShift, List<FieldMap.Warp> roughness, Shelves shelves,
                               FieldMap.Warp jitter, List<Ridge> ridges) {

    /** 最小〜最大。 */
    public record Range(double min, double max) {
        public static final Codec<Range> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("min").forGetter(Range::min),
                Codec.DOUBLE.fieldOf("max").forGetter(Range::max)
        ).apply(i, Range::new));
    }

    /** 頂上の高さ。 */
    public record Peak(double min, double max, double scale) {
        public static final Codec<Peak> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("min").forGetter(Peak::min),
                Codec.DOUBLE.fieldOf("max").forGetter(Peak::max),
                Codec.DOUBLE.fieldOf("scale").forGetter(Peak::scale)
        ).apply(i, Peak::new));
    }

    /** 平らな段。 */
    public record Shelves(double coverage, double level, double band, double scale) {
        public static final Codec<Shelves> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("coverage").forGetter(Shelves::coverage),
                Codec.DOUBLE.fieldOf("level").forGetter(Shelves::level),
                Codec.DOUBLE.fieldOf("band").forGetter(Shelves::band),
                Codec.DOUBLE.fieldOf("scale").forGetter(Shelves::scale)
        ).apply(i, Shelves::new));
    }

    /**
     * 尾根線。points を順につないだ線が尾根の中心になり、そこから width ブロック離れたところが山のふもと。
     * closed = true なら最後の点と最初の点もつなぐ（輪にする）。
     */
    public record Ridge(double width, boolean closed, List<FieldMap.Point> points) {
        public static final Codec<Ridge> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("width").forGetter(Ridge::width),
                Codec.BOOL.optionalFieldOf("closed", false).forGetter(Ridge::closed),
                FieldMap.Point.CODEC.listOf().fieldOf("points").forGetter(Ridge::points)
        ).apply(i, Ridge::new));
    }

    /** 山の中での位置。p = 0（ふもと）〜1（尾根の中心）、side = 尾根線のどちら側か（+1 / -1）。 */
    public record Position(double p, int side) {
        public static final Position OUTSIDE = new Position(0, 1);
    }

    public static final Codec<MountainSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Biome.CODEC.fieldOf("biome").forGetter(MountainSettings::biome),
            Codec.DOUBLE.fieldOf("biome_threshold").forGetter(MountainSettings::biomeThreshold),
            Range.CODEC.fieldOf("foot_wall").forGetter(MountainSettings::footWall),
            Codec.DOUBLE.optionalFieldOf("foot_ratio", 0.02).forGetter(MountainSettings::footRatio),
            Peak.CODEC.fieldOf("peak").forGetter(MountainSettings::peak),
            Codec.DOUBLE.optionalFieldOf("crest_shift", 0.0).forGetter(MountainSettings::crestShift),
            FieldMap.Warp.CODEC.listOf().optionalFieldOf("roughness", List.of()).forGetter(MountainSettings::roughness),
            Shelves.CODEC.fieldOf("shelves").forGetter(MountainSettings::shelves),
            FieldMap.Warp.CODEC.fieldOf("jitter").forGetter(MountainSettings::jitter),
            Ridge.CODEC.listOf().fieldOf("ridges").forGetter(MountainSettings::ridges)
    ).apply(i, MountainSettings::new));

    // ノイズの種類と波の大きさ（確認用スクリプト tools/fieldmap/preview.py と同じ値にすること）
    private static final long JITTER_SALT_X = 303;
    private static final long JITTER_SALT_Z = 404;
    private static final long CREST_SALT = 707;
    private static final long WALL_SALT = 808;
    private static final long PEAK_SALT = 1212;
    private static final long SHELF_SALT = 1414;
    private static final long ROUGH_SALT = 1515;
    private static final long SHELF_LEVEL_SALT = 1616;
    private static final double WALL_SCALE = 350.0;
    private static final double CREST_SCALE = 600.0;
    private static final double SHELF_LEVEL_SCALE = 400.0;
    /** 頂上の手前で高さが頭打ちになる位置（q の割合）。1 に近いほど頂上がとがる。 */
    private static final double SUMMIT_Q = 0.94;

    /** 地点 (x, z) の、山の中での位置。一番近い尾根線までの距離から求める。 */
    public Position position(double x, double z) {
        // ふもとの線がまっすぐにならないよう、座標を少しずらす
        double nx = x / jitter.scale();
        double nz = z / jitter.scale();
        double jx = x + jitter.amplitude() * FieldNoise.fbm(nx, nz, JITTER_SALT_X);
        double jz = z + jitter.amplitude() * FieldNoise.fbm(nx, nz, JITTER_SALT_Z);

        double bestP = 0;
        int bestSide = 1;
        for (Ridge ridge : ridges) {
            List<FieldMap.Point> points = ridge.points();
            int n = points.size();
            if (n == 0) {
                continue;
            }
            double minDistance = Double.MAX_VALUE;
            int side = 1;
            if (n == 1) {
                minDistance = Math.hypot(jx - points.get(0).x(), jz - points.get(0).z());
            }
            int segments = ridge.closed() ? n : n - 1;
            for (int s = 0; s < segments; s++) {
                FieldMap.Point a = points.get(s);
                FieldMap.Point b = points.get((s + 1) % n);
                double d = Geometry.distanceToSegment(jx, jz, a.x(), a.z(), b.x(), b.z());
                if (d < minDistance) {
                    minDistance = d;
                    // 線分のどちら側か（外積の符号）
                    double cross = (b.x() - a.x()) * (jz - a.z()) - (b.z() - a.z()) * (jx - a.x());
                    side = cross >= 0 ? 1 : -1;
                }
            }
            double p = 1 - minDistance / ridge.width();
            if (p > bestP) {
                bestP = p;
                bestSide = side;
            }
        }
        return bestP <= 0 ? Position.OUTSIDE : new Position(bestP, bestSide);
    }

    /** 山の中の位置 pos の地点 (x, z) を、ふもとの地面から何ブロック盛り上げるか。 */
    public double heightAt(Position pos, double x, double z) {
        double p = pos.p();
        if (p <= 0) {
            return 0;
        }
        // ふもとの絶壁（場所ごとに高さが違う。0 なら絶壁なし）
        double wall = footWall.min() + (footWall.max() - footWall.min())
                * smooth(0.5 + 0.8 * FieldNoise.fbm(x / WALL_SCALE, z / WALL_SCALE, WALL_SALT));
        if (p < footRatio) {
            return wall * smooth(p / footRatio);
        }

        // 頂上までの進み具合 q（0 = ふもと、1 = 頂上）。頂上の線は尾根の中心からずれ、ずれた側が急になる
        double u = pos.side() * (1 - p);   // -1〜1（尾根の中心が 0）
        double u0 = crestShift * FieldNoise.fbm(x / CREST_SCALE, z / CREST_SCALE, CREST_SALT);
        double q = u >= u0 ? (1 - u) / (1 - u0) : (u + 1) / (1 + u0);
        q = clamp((q - footRatio) / (1 - footRatio));

        // 頂上の高さ（場所ごとに違う）
        double top = peak.min() + (peak.max() - peak.min())
                * smooth(0.5 + 0.9 * FieldNoise.fbm(x / peak.scale(), z / peak.scale(), PEAK_SALT));
        double height = wall + (top - wall) * Math.min(1, q / SUMMIT_Q);

        // 平らな段：段がある場所では、ある高さで平らになり、その奥は絶壁
        double shelfWeight = smooth((FieldNoise.fbm(x / shelves.scale(), z / shelves.scale(), SHELF_SALT)
                - (1 - 2 * shelves.coverage())) / 0.25);
        double level = wall + (top - wall) * (shelves.level()
                + 0.15 * FieldNoise.fbm(x / SHELF_LEVEL_SCALE, z / SHELF_LEVEL_SCALE, SHELF_LEVEL_SALT));
        double flat = 0;
        if (shelfWeight > 0 && height > level && height < level + shelves.band()) {
            height += (level - height) * shelfWeight;
            flat = shelfWeight;
        }

        // 斜面のでこぼこ（段の上は平らなまま）
        double rough = 0;
        for (int i = 0; i < roughness.size(); i++) {
            FieldMap.Warp octave = roughness.get(i);
            rough += octave.amplitude() * FieldNoise.fbm(x / octave.scale(), z / octave.scale(), ROUGH_SALT + 2L * i);
        }
        height += rough * Math.min(1, q * 3) * (1 - flat);
        return height;
    }

    private static double clamp(double t) {
        return Math.max(0, Math.min(1, t));
    }

    private static double smooth(double t) {
        t = clamp(t);
        return t * t * (3 - 2 * t);
    }
}
