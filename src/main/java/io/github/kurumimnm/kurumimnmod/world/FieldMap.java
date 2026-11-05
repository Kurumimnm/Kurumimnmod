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
 * <h2>地形</h2>
 * 地面の高さはすべてこのMODで計算する（{@link TerrainHeightFunction}）。
 * terrain = 基本の地面、mountains = 山脈（{@link MountainSettings}）。丘・渓谷・川・洞窟も今後ここに足していく。
 *
 * <h2>道</h2>
 * roads = 街と街を結ぶ道（{@link RoadSettings}）。道の見た目と有無はフィールドごとの road で決まる。
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

    /**
     * フィールド。biome = このフィールドのバイオーム、tier = 危険度の基準（出発する街の番号）、
     * road = このフィールドを通る道の種類と状態（省略すると道なし。{@link RoadSettings} 参照）。
     */
    public record Field(String id, int number, Holder<Biome> biome, int tier, List<AreaSeed> areas,
                        RoadSettings.FieldRoad road) {
        public static final Codec<Field> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Field::id),
                Codec.INT.fieldOf("number").forGetter(Field::number),
                Biome.CODEC.fieldOf("biome").forGetter(Field::biome),
                Codec.INT.fieldOf("tier").forGetter(Field::tier),
                AreaSeed.CODEC.listOf().fieldOf("areas").forGetter(Field::areas),
                RoadSettings.FieldRoad.CODEC.optionalFieldOf("road", RoadSettings.FieldRoad.NONE).forGetter(Field::road)
        ).apply(i, Field::new));
    }

    /** 道の判定結果。block = 置くブロック。 */
    public record RoadHit(RoadSettings.Style style, net.minecraft.world.level.block.Block block) {}

    /** 座標（ブロック単位）。 */
    public record Point(int x, int z) {
        public static final Codec<Point> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(Point::x),
                Codec.INT.fieldOf("z").forGetter(Point::z)
        ).apply(i, Point::new));
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
            TerrainSettings.CODEC.fieldOf("terrain").forGetter(FieldMap::terrain),
            MountainSettings.CODEC.optionalFieldOf("mountains").forGetter(FieldMap::mountains),
            RoadSettings.CODEC.optionalFieldOf("roads").forGetter(FieldMap::roads)
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
    private static final long ROAD_SALT_X = 909;
    private static final long ROAD_SALT_Z = 1010;
    private static final long ROAD_GAP_SALT = 1111;
    /** 「worn」の道が途切れる割合の目安（ノイズがこの値より小さいところが途切れる）。 */
    private static final double WORN_GAP_THRESHOLD = -0.15;
    /** 「worn」の道の途切れの波の大きさ（ブロック）。 */
    private static final double WORN_GAP_SCALE = 36.0;

    private final Warp warp;
    private final List<Town> towns;
    private final List<Field> fields;
    private final TerrainSettings terrain;
    private final Optional<MountainSettings> mountains;
    private final Optional<RoadSettings> roads;
    // 道の線分（from → to）を配列に展開したもの
    private final double[] roadAX;
    private final double[] roadAZ;
    private final double[] roadBX;
    private final double[] roadBZ;

    // 判定を速くするため、基準点を配列に展開しておく
    private final int[] seedX;
    private final int[] seedZ;
    private final int[] seedArea;
    private final Field[] seedField;

    public FieldMap(Warp warp, List<Town> towns, List<Field> fields, TerrainSettings terrain,
                    Optional<MountainSettings> mountains, Optional<RoadSettings> roads) {
        this.warp = warp;
        this.towns = List.copyOf(towns);
        this.fields = List.copyOf(fields);
        this.terrain = terrain;
        this.mountains = mountains;
        this.roads = roads;

        // 道のルートを線分にする（街の id → 座標）
        java.util.List<double[]> segments = new java.util.ArrayList<>();
        roads.ifPresent(r -> {
            for (RoadSettings.Route route : r.routes()) {
                Optional<Town> from = this.towns.stream().filter(t -> t.id().equals(route.from())).findFirst();
                Optional<Town> to = this.towns.stream().filter(t -> t.id().equals(route.to())).findFirst();
                if (from.isEmpty() || to.isEmpty()) {
                    continue; // 存在しない街の名前は無視
                }
                java.util.List<double[]> points = new java.util.ArrayList<>();
                points.add(new double[]{from.get().x(), from.get().z()});
                for (Point via : route.via()) {
                    points.add(new double[]{via.x(), via.z()});
                }
                points.add(new double[]{to.get().x(), to.get().z()});
                for (int s = 0; s + 1 < points.size(); s++) {
                    segments.add(new double[]{points.get(s)[0], points.get(s)[1], points.get(s + 1)[0], points.get(s + 1)[1]});
                }
            }
        });
        this.roadAX = segments.stream().mapToDouble(v -> v[0]).toArray();
        this.roadAZ = segments.stream().mapToDouble(v -> v[1]).toArray();
        this.roadBX = segments.stream().mapToDouble(v -> v[2]).toArray();
        this.roadBZ = segments.stream().mapToDouble(v -> v[3]).toArray();

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

    public TerrainSettings terrain() {
        return terrain;
    }

    public Optional<MountainSettings> mountains() {
        return mountains;
    }

    public Optional<RoadSettings> roads() {
        return roads;
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

    /** 山脈の中での位置（0 = 山の外・ふもと、1 = 尾根の中心）。山脈がなければ常に 0。 */
    public double mountainPosition(double x, double z) {
        return mountains.map(m -> m.position(x, z).p()).orElse(0.0);
    }

    /**
     * 地面の高さ（Y）。地形の JSON（density_function の kurumimnmod:terrain_height）から呼ばれる。
     * 基本の地面（terrain）＋ 山脈（mountains）。上限は terrain.max_height。
     */
    public double terrainHeight(double x, double z) {
        double height = terrain.groundHeight(x, z);
        if (mountains.isPresent()) {
            MountainSettings m = mountains.get();
            MountainSettings.Position position = m.position(x, z);
            if (position.p() > 0) {
                height += m.heightAt(position, x, z);
            }
        }
        return Math.min(height, terrain.maxHeight());
    }

    /**
     * その地点に道があれば、置くブロックを返す（なければ null）。
     * 道はルートの線分の近く・山脈の外・フィールドの road が none 以外の場所にだけできる。
     */
    public RoadHit roadAt(double x, double z) {
        if (roads.isEmpty() || roadAX.length == 0) {
            return null;
        }
        RoadSettings r = roads.get();
        // 道がまっすぐにならないよう、座標を少しずらす
        double nx = x / r.jitter().scale();
        double nz = z / r.jitter().scale();
        double jx = x + r.jitter().amplitude() * FieldNoise.fbm(nx, nz, ROAD_SALT_X);
        double jz = z + r.jitter().amplitude() * FieldNoise.fbm(nx, nz, ROAD_SALT_Z);

        double limit = r.maxHalfWidth() + 1;
        double distance = Double.MAX_VALUE;
        for (int i = 0; i < roadAX.length; i++) {
            // 線分を囲む四角の外ならスキップ（速くするため）
            if (jx < Math.min(roadAX[i], roadBX[i]) - limit || jx > Math.max(roadAX[i], roadBX[i]) + limit
                    || jz < Math.min(roadAZ[i], roadBZ[i]) - limit || jz > Math.max(roadAZ[i], roadBZ[i]) + limit) {
                continue;
            }
            distance = Math.min(distance, Geometry.distanceToSegment(jx, jz, roadAX[i], roadAZ[i], roadBX[i], roadBZ[i]));
        }
        if (distance > limit) {
            return null;
        }

        Location location = locate(x, z);
        RoadSettings.FieldRoad fieldRoad = location.field().road();
        if (location.inMountains() || fieldRoad.isNone()) {
            return null;
        }
        RoadSettings.Style style = r.styles().get(fieldRoad.style());
        if (style == null) {
            return null;
        }
        double halfWidth = style.width() / 2.0;
        if (fieldRoad.isWorn()) {
            // 荒れた道：ところどころ途切れて、細くなる
            if (FieldNoise.fbm(x / WORN_GAP_SCALE, z / WORN_GAP_SCALE, ROAD_GAP_SALT) < WORN_GAP_THRESHOLD) {
                return null;
            }
            halfWidth -= 0.5;
        }
        if (distance > halfWidth) {
            return null;
        }
        // 中央は surface、端の 1 ブロックは edge
        boolean edge = halfWidth >= 1.5 && distance > halfWidth - 1;
        return new RoadHit(style, edge ? style.edge() : style.surface());
    }
}
