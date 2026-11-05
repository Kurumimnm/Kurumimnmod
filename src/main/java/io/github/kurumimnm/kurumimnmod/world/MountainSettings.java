package io.github.kurumimnm.kurumimnmod.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/**
 * 山脈の設定（地図 JSON の {@code mountains}）。
 * <p>
 * 尾根線（ridges）からの距離で「山の中の位置 p」（0 = ふもと、1 = 尾根の中心）を求め、
 * 断面の形（profile）に当てはめて高さを決める。
 *
 * <h2>断面の形（profile）</h2>
 * {@code [{"p": 0, "h": 0}, {"p": 0.04, "h": 150}, ...]} のように、p と高さ h（ふもとの地面から何ブロック上か）の組を並べる。
 * 点と点の間はまっすぐつなぐので、
 * <ul>
 *   <li>p が少ししか進まないのに h が大きく上がる区間 → 絶壁・急斜面</li>
 *   <li>h がほとんど変わらない区間 → 平らな場所（戦闘できる台地）</li>
 * </ul>
 * になる。初期設定は「凸」の形（絶壁 → 下の段 → 絶壁 → 上の段 → 山頂）。
 *
 * <h2>場所ごとのばらつき</h2>
 * <ul>
 *   <li>shelf_noise … 3 つ目以降の点の p を場所ごとにずらす量。下の段の幅が場所によって広い・ほぼない、になる</li>
 *   <li>height_noise … 2 つ目以降の点の高さを場所ごとにずらす量（ブロック）。平らな場所の高さが場所によって違う</li>
 *   <li>jitter … ふもとの線をぐにゃっとさせる量</li>
 * </ul>
 *
 * @param biome          山の中のバイオーム
 * @param biomeThreshold この p より内側を山のバイオームにする
 */
public record MountainSettings(Holder<Biome> biome, double biomeThreshold, List<ProfilePoint> profile,
                               double shelfNoise, double heightNoise, FieldMap.Warp jitter, List<Ridge> ridges) {

    /** 断面の形の 1 点。 */
    public record ProfilePoint(double p, double h) {
        public static final Codec<ProfilePoint> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("p").forGetter(ProfilePoint::p),
                Codec.DOUBLE.fieldOf("h").forGetter(ProfilePoint::h)
        ).apply(i, ProfilePoint::new));
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

    public static final Codec<MountainSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Biome.CODEC.fieldOf("biome").forGetter(MountainSettings::biome),
            Codec.DOUBLE.fieldOf("biome_threshold").forGetter(MountainSettings::biomeThreshold),
            ProfilePoint.CODEC.listOf().fieldOf("profile").forGetter(MountainSettings::profile),
            Codec.DOUBLE.optionalFieldOf("shelf_noise", 0.0).forGetter(MountainSettings::shelfNoise),
            Codec.DOUBLE.optionalFieldOf("height_noise", 0.0).forGetter(MountainSettings::heightNoise),
            FieldMap.Warp.CODEC.fieldOf("jitter").forGetter(MountainSettings::jitter),
            Ridge.CODEC.listOf().fieldOf("ridges").forGetter(MountainSettings::ridges)
    ).apply(i, MountainSettings::new));

    // ノイズの種類（確認用スクリプト tools/fieldmap/preview.py と同じ値にすること）
    private static final long JITTER_SALT_X = 303;
    private static final long JITTER_SALT_Z = 404;
    private static final long SHELF_SALT = 707;
    private static final long HEIGHT_SALT = 808;
    /** 段の幅・高さのばらつきの波の大きさ（ブロック）。 */
    private static final double VARIATION_SCALE = 450.0;

    /**
     * 山脈の中での位置（0 = 山の外・ふもと、1 = 尾根の中心）。
     * 一番近い尾根線までの距離から求める。
     */
    public double position(double x, double z) {
        // ふもとの線がまっすぐにならないよう、座標を少しずらす
        double nx = x / jitter.scale();
        double nz = z / jitter.scale();
        double jx = x + jitter.amplitude() * FieldNoise.fbm(nx, nz, JITTER_SALT_X);
        double jz = z + jitter.amplitude() * FieldNoise.fbm(nx, nz, JITTER_SALT_Z);

        double best = 0;
        for (Ridge ridge : ridges) {
            List<FieldMap.Point> points = ridge.points();
            int n = points.size();
            if (n == 0) {
                continue;
            }
            double minDistance = Double.MAX_VALUE;
            if (n == 1) {
                minDistance = Math.hypot(jx - points.get(0).x(), jz - points.get(0).z());
            }
            int segments = ridge.closed() ? n : n - 1;
            for (int s = 0; s < segments; s++) {
                FieldMap.Point a = points.get(s);
                FieldMap.Point b = points.get((s + 1) % n);
                minDistance = Math.min(minDistance, Geometry.distanceToSegment(jx, jz, a.x(), a.z(), b.x(), b.z()));
            }
            best = Math.max(best, 1 - minDistance / ridge.width());
        }
        return best;
    }

    /** 山の中での位置 p の地点 (x, z) を、ふもとの地面から何ブロック盛り上げるか。 */
    public double heightAt(double p, double x, double z) {
        if (p <= 0 || profile.isEmpty()) {
            return 0;
        }
        double shelfShift = shelfNoise * FieldNoise.fbm(x / VARIATION_SCALE, z / VARIATION_SCALE, SHELF_SALT);
        double heightShift = heightNoise * FieldNoise.fbm(x / VARIATION_SCALE, z / VARIATION_SCALE, HEIGHT_SALT);

        double prevP = 0;
        double prevH = 0;
        for (int i = 0; i < profile.size(); i++) {
            ProfilePoint point = profile.get(i);
            double pointP = point.p() + (i >= 2 ? shelfShift : 0);
            pointP = Math.max(pointP, prevP + 0.005); // 順番が入れ替わらないように
            double pointH = point.h() + (i >= 1 ? heightShift : 0);
            if (p <= pointP) {
                double t = (p - prevP) / (pointP - prevP);
                return prevH + (pointH - prevH) * t;
            }
            prevP = pointP;
            prevH = pointH;
        }
        return prevH;
    }

    /** 盛り上げる高さの最大値（ブロック）。 */
    public double maxHeight() {
        return profile.stream().mapToDouble(ProfilePoint::h).max().orElse(0) + Math.abs(heightNoise);
    }
}
