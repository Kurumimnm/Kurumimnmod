package io.github.kurumimnm.kurumimnmod.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kurumimnm.kurumimnmod.registry.ModWorldgen;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.world.level.biome.Biome;

/**
 * ワールド全体の地図（街の位置・フィールド・エリア）。
 * <p>
 * データパックの {@code data/kurumimnmod/kurumimnmod/field_map/<名前>.json} から読み込む。
 * 地図を変えたいときは JSON を書き換えるだけでよい（コードの変更は不要）。
 * ※ すでに生成されたチャンクは作り直されないので、確認は新しいワールド（または未探索の場所）で行う。
 *
 * <h2>しくみ</h2>
 * 各フィールドは「エリアの基準点（area seed）」をいくつか持つ。
 * ある地点がどのフィールドのどのエリアかは「いちばん近い基準点」で決まる（ボロノイ分割）。
 * 境界がまっすぐにならないよう、判定の前に座標をノイズで少しずらす（warp）。
 *
 * <h2>危険度</h2>
 * 危険度 = フィールドの tier（出発する街の番号）＋ エリア番号。
 * 本道（エリア 1）から外れるほど数字が上がり、敵が強くなる想定。
 *
 * <h2>山脈</h2>
 * mountains（省略可）に尾根線を書くと、その周りの地形が盛り上がる（{@link MountainUpliftFunction}）。
 * ふもとは登れない絶壁、上のほうは戦闘できる広い場所、中心が山頂。山の中のバイオームは山脈用になる。
 */
public final class FieldMap {

    // =====================================================================
    // JSON の各要素
    // =====================================================================

    /** 境界のゆがみ。amplitude = ずらす最大量（ブロック）、scale = ゆがみの波の大きさ（ブロック）。 */
    public record Warp(double amplitude, double scale) {
        public static final Codec<Warp> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("amplitude").forGetter(Warp::amplitude),
                Codec.DOUBLE.fieldOf("scale").forGetter(Warp::scale)
        ).apply(i, Warp::new));
    }

    /** 街。number = 街の番号（0 = 始まりの街）。座標はブロック単位（北 = -Z）。 */
    public record Town(String id, int number, int x, int z) {
        public static final Codec<Town> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Town::id),
                Codec.INT.fieldOf("number").forGetter(Town::number),
                Codec.INT.fieldOf("x").forGetter(Town::x),
                Codec.INT.fieldOf("z").forGetter(Town::z)
        ).apply(i, Town::new));
    }

    /** エリアの基準点。area = エリア番号（1 = 本道、大きいほど深い）。 */
    public record AreaSeed(int area, int x, int z) {
        public static final Codec<AreaSeed> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("area").forGetter(AreaSeed::area),
                Codec.INT.fieldOf("x").forGetter(AreaSeed::x),
                Codec.INT.fieldOf("z").forGetter(AreaSeed::z)
        ).apply(i, AreaSeed::new));
    }

    /** フィールド。biome = このフィールドのバイオーム、tier = 危険度の基準（出発する街の番号）。 */
    public record Field(String id, int number, Holder<Biome> biome, int tier, List<AreaSeed> areas) {
        public static final Codec<Field> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Field::id),
                Codec.INT.fieldOf("number").forGetter(Field::number),
                Biome.CODEC.fieldOf("biome").forGetter(Field::biome),
                Codec.INT.fieldOf("tier").forGetter(Field::tier),
                AreaSeed.CODEC.listOf().fieldOf("areas").forGetter(Field::areas)
        ).apply(i, Field::new));
    }

    /** 座標（ブロック単位）。 */
    public record Point(int x, int z) {
        public static final Codec<Point> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(Point::x),
                Codec.INT.fieldOf("z").forGetter(Point::z)
        ).apply(i, Point::new));
    }

    /**
     * 山脈の尾根線。points を順につないだ線が尾根の中心になり、そこから width ブロック離れたところが山のふもと。
     * closed = true なら最後の点と最初の点もつなぐ（輪にする）。
     */
    public record Ridge(double width, boolean closed, List<Point> points) {
        public static final Codec<Ridge> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("width").forGetter(Ridge::width),
                Codec.BOOL.optionalFieldOf("closed", false).forGetter(Ridge::closed),
                Point.CODEC.listOf().fieldOf("points").forGetter(Ridge::points)
        ).apply(i, Ridge::new));
    }

    /**
     * 山脈の設定。高さはふもとの地面から何ブロック盛り上げるか。
     * <pre>
     * 高さ
     *  peak   ┤                    ／￣＼      ← 山頂（尾根の中心）
     *  ledge  ┤        ＿＿＿＿＿／        ← 上のほうの広い場所（戦闘できる）
     *  wall   ┤   ┌──             ← 絶壁の上
     *         │   │                ← 絶壁（登れない）
     *    0    ┼───┘
     *         ふもと  wall_ratio   ledge_ratio    中心
     * </pre>
     * wall_ratio / ledge_ratio は「ふもと→尾根の中心」を 0〜1 としたときの位置。
     * biome_threshold より内側（山の中）は biome のバイオームになる。
     * jitter = 山のふもとの線をぐにゃっとさせる量（amplitude ブロック、scale は波の大きさ）。
     */
    public record Mountains(Holder<Biome> biome, double biomeThreshold,
                            double wallHeight, double ledgeHeight, double peakHeight,
                            double wallRatio, double ledgeRatio, Warp jitter, List<Ridge> ridges) {
        public static final Codec<Mountains> CODEC = RecordCodecBuilder.create(i -> i.group(
                Biome.CODEC.fieldOf("biome").forGetter(Mountains::biome),
                Codec.DOUBLE.fieldOf("biome_threshold").forGetter(Mountains::biomeThreshold),
                Codec.DOUBLE.fieldOf("wall_height").forGetter(Mountains::wallHeight),
                Codec.DOUBLE.fieldOf("ledge_height").forGetter(Mountains::ledgeHeight),
                Codec.DOUBLE.fieldOf("peak_height").forGetter(Mountains::peakHeight),
                Codec.DOUBLE.fieldOf("wall_ratio").forGetter(Mountains::wallRatio),
                Codec.DOUBLE.fieldOf("ledge_ratio").forGetter(Mountains::ledgeRatio),
                Warp.CODEC.fieldOf("jitter").forGetter(Mountains::jitter),
                Ridge.CODEC.listOf().fieldOf("ridges").forGetter(Mountains::ridges)
        ).apply(i, Mountains::new));

        /** 山の中での位置 p（0 = ふもと、1 = 尾根の中心）から、盛り上げる高さ（ブロック）を求める。 */
        public double heightAt(double p) {
            if (p <= 0) {
                return 0;
            }
            if (p < wallRatio) {
                return wallHeight * smooth(p / wallRatio);
            }
            if (p < ledgeRatio) {
                return wallHeight + (ledgeHeight - wallHeight) * (p - wallRatio) / (ledgeRatio - wallRatio);
            }
            return ledgeHeight + (peakHeight - ledgeHeight) * smooth((p - ledgeRatio) / (1 - ledgeRatio));
        }

        private static double smooth(double t) {
            t = Math.max(0, Math.min(1, t));
            return t * t * (3 - 2 * t);
        }
    }

    /**
     * 判定結果。danger = 危険度（tier + エリア番号）。
     * inMountains = 山脈の中（バイオームが山脈になる範囲）かどうか。
     */
    public record Location(Field field, int area, boolean inMountains) {
        public int danger() {
            return field.tier() + area;
        }
    }

    // =====================================================================
    // Codec
    // =====================================================================

    /** JSON の中身そのものを読む Codec。 */
    public static final Codec<FieldMap> DIRECT_CODEC = RecordCodecBuilder.<FieldMap>create(i -> i.group(
            Warp.CODEC.fieldOf("warp").forGetter(FieldMap::warp),
            Town.CODEC.listOf().fieldOf("towns").forGetter(FieldMap::towns),
            Field.CODEC.listOf().fieldOf("fields").forGetter(FieldMap::fields),
            Mountains.CODEC.optionalFieldOf("mountains").forGetter(FieldMap::mountains)
    ).apply(i, FieldMap::new)).validate(FieldMap::validate);

    /** フィールドが 1 つもない地図は読み込みエラーにする。 */
    private static DataResult<FieldMap> validate(FieldMap map) {
        return map.fields().isEmpty()
                ? DataResult.error(() -> "Field map must have at least one field")
                : DataResult.success(map);
    }

    /** 「kurumimnmod:first_continent」のように、登録済みの地図を名前で参照する Codec。 */
    public static final Codec<Holder<FieldMap>> CODEC = RegistryCodecs.holder(ModWorldgen.FIELD_MAP, DIRECT_CODEC);

    // =====================================================================
    // 本体
    // =====================================================================

    /** ノイズの種類（x 方向・z 方向で別のノイズを使う）。確認用スクリプトと同じ値にすること。 */
    private static final long WARP_SALT_X = 101;
    private static final long WARP_SALT_Z = 202;
    private static final long MOUNTAIN_SALT_X = 303;
    private static final long MOUNTAIN_SALT_Z = 404;
    /** 地形の「高さ 1」が何ブロックか（バニラの地形の depth は 128 ブロックで 1 変わる）。 */
    private static final double BLOCKS_PER_DEPTH = 128.0;

    private final Warp warp;
    private final List<Town> towns;
    private final List<Field> fields;
    private final Optional<Mountains> mountains;

    // 判定を速くするため、基準点を配列に展開しておく
    private final int[] seedX;
    private final int[] seedZ;
    private final int[] seedArea;
    private final Field[] seedField;

    public FieldMap(Warp warp, List<Town> towns, List<Field> fields, Optional<Mountains> mountains) {
        this.warp = warp;
        this.towns = List.copyOf(towns);
        this.fields = List.copyOf(fields);
        this.mountains = mountains;

        int count = 0;
        for (Field field : this.fields) {
            count += field.areas().size();
        }
        this.seedX = new int[count];
        this.seedZ = new int[count];
        this.seedArea = new int[count];
        this.seedField = new Field[count];
        int index = 0;
        for (Field field : this.fields) {
            for (AreaSeed seed : field.areas()) {
                seedX[index] = seed.x();
                seedZ[index] = seed.z();
                seedArea[index] = seed.area();
                seedField[index] = field;
                index++;
            }
        }
    }

    public Warp warp() {
        return warp;
    }

    public List<Town> towns() {
        return towns;
    }

    public List<Field> fields() {
        return fields;
    }

    public Optional<Mountains> mountains() {
        return mountains;
    }

    /** 番号から街を探す。 */
    public Optional<Town> town(int number) {
        return towns.stream().filter(t -> t.number() == number).findFirst();
    }

    /**
     * ブロック座標 (x, z) がどのフィールドのどのエリアかを返す。
     * フィールドが 1 つもない地図では null。
     */
    public Location locate(double x, double z) {
        if (seedField.length == 0) {
            return null;
        }
        // 境界を自然な形にするため、判定する座標を少しずらす
        double nx = x / warp.scale();
        double nz = z / warp.scale();
        double wx = x + warp.amplitude() * FieldNoise.fbm(nx, nz, WARP_SALT_X);
        double wz = z + warp.amplitude() * FieldNoise.fbm(nx, nz, WARP_SALT_Z);

        // いちばん近い基準点を探す（基準点は数百個なので全件調べても十分速い）
        int best = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < seedField.length; i++) {
            double dx = seedX[i] - wx;
            double dz = seedZ[i] - wz;
            double distance = dx * dx + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        boolean inMountains = mountains.isPresent()
                && mountainPosition(x, z) > mountains.get().biomeThreshold();
        return new Location(seedField[best], seedArea[best], inMountains);
    }

    /** その地点のバイオーム（山脈の中なら山脈のバイオーム）。 */
    public Holder<Biome> biomeAt(double x, double z) {
        Location location = locate(x, z);
        if (location.inMountains()) {
            return mountains.get().biome();
        }
        return location.field().biome();
    }

    /**
     * 山脈の中での位置（0 = 山の外・ふもと、1 = 尾根の中心）。
     * 一番近い尾根線までの距離から求める。山脈が設定されていなければ常に 0。
     */
    public double mountainPosition(double x, double z) {
        if (mountains.isEmpty()) {
            return 0;
        }
        Mountains m = mountains.get();
        // ふもとの線がまっすぐにならないよう、座標を少しずらす
        double nx = x / m.jitter().scale();
        double nz = z / m.jitter().scale();
        double jx = x + m.jitter().amplitude() * FieldNoise.fbm(nx, nz, MOUNTAIN_SALT_X);
        double jz = z + m.jitter().amplitude() * FieldNoise.fbm(nx, nz, MOUNTAIN_SALT_Z);

        double best = 0;
        for (Ridge ridge : m.ridges()) {
            List<Point> points = ridge.points();
            int n = points.size();
            if (n == 0) {
                continue;
            }
            double minDistance = Double.MAX_VALUE;
            int segments = ridge.closed() ? n : n - 1;
            if (n == 1) {
                minDistance = Math.hypot(jx - points.get(0).x(), jz - points.get(0).z());
            }
            for (int i = 0; i < segments; i++) {
                Point a = points.get(i);
                Point b = points.get((i + 1) % n);
                minDistance = Math.min(minDistance, distanceToSegment(jx, jz, a, b));
            }
            best = Math.max(best, 1 - minDistance / ridge.width());
        }
        return best;
    }

    /**
     * 地形を盛り上げる量（地形の depth 単位。1 = 128 ブロック）。
     * 地形の JSON（density_function の kurumimnmod:mountain_uplift）から呼ばれる。
     */
    public double mountainUplift(double x, double z) {
        if (mountains.isEmpty()) {
            return 0;
        }
        return mountains.get().heightAt(mountainPosition(x, z)) / BLOCKS_PER_DEPTH;
    }

    /** 盛り上げる量の最大値（depth 単位）。 */
    public double maxMountainUplift() {
        return mountains.map(m -> Math.max(m.peakHeight(), Math.max(m.ledgeHeight(), m.wallHeight())) / BLOCKS_PER_DEPTH)
                .orElse(0.0);
    }

    private static double distanceToSegment(double x, double z, Point a, Point b) {
        double vx = b.x() - a.x();
        double vz = b.z() - a.z();
        double lengthSq = vx * vx + vz * vz;
        double t = lengthSq == 0 ? 0 : ((x - a.x()) * vx + (z - a.z()) * vz) / lengthSq;
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(x - (a.x() + vx * t), z - (a.z() + vz * t));
    }
}
