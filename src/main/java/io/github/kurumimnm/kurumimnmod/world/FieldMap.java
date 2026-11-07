package io.github.kurumimnm.kurumimnmod.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kurumimnm.kurumimnmod.registry.ModWorldgen;
import java.util.List;
import java.util.Optional;
import io.github.kurumimnm.kurumimnmod.world.terrain.TerrainBake;
import io.github.kurumimnm.kurumimnmod.world.terrain.TerrainBakes;
import io.github.kurumimnm.kurumimnmod.world.terrain.TerrainDesign;
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
 * 推奨レベル = フィールドの tier（設計書の推奨Lv）。エリアの番号は区別のための番号で、危険度には足さない。
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

    /**
     * 街。number = 街の番号（0 = 始まりの街）。座標はブロック単位（北 = -Z）。
     * radius = 街の範囲（この円の中が「街」。周りのフィールドとの境目にゲートの壁ができる）。
     * type = 街の種類（village / town / city / port_town など。{@link io.github.kurumimnm.kurumimnmod.world.town.TownPlanner}）、
     * style = 建物の素材（oak / spruce / birch / dark_oak / acacia / sandstone）。
     */
    public record Town(String id, int number, int x, int z, int radius, String type, String style, String unlock) {
        public static final Codec<Town> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Town::id),
                Codec.INT.fieldOf("number").forGetter(Town::number),
                Codec.INT.fieldOf("x").forGetter(Town::x),
                Codec.INT.fieldOf("z").forGetter(Town::z),
                Codec.INT.optionalFieldOf("radius", 48).forGetter(Town::radius),
                Codec.STRING.optionalFieldOf("type", "town").forGetter(Town::type),
                Codec.STRING.optionalFieldOf("style", "oak").forGetter(Town::style),
                Codec.STRING.optionalFieldOf("unlock", "").forGetter(Town::unlock)
        ).apply(i, Town::new));

        public boolean contains(double px, double pz) {
            double dx = px - x;
            double dz = pz - z;
            return dx * dx + dz * dz <= (double) radius * radius;
        }
    }

    /**
     * エリアの基準点。area = エリア番号。y = エリアの高さ（上層・下層を分けるのに使う。省略すると高さを見ない）。
     */
    public record AreaSeed(int area, int x, int z, int y) {
        /** 高さの指定なし。 */
        public static final int NO_Y = Integer.MIN_VALUE;
        public static final Codec<AreaSeed> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("area").forGetter(AreaSeed::area),
                Codec.INT.fieldOf("x").forGetter(AreaSeed::x),
                Codec.INT.fieldOf("z").forGetter(AreaSeed::z),
                Codec.INT.optionalFieldOf("y", NO_Y).forGetter(AreaSeed::y)
        ).apply(i, AreaSeed::new));
    }

    /**
     * フィールド。biome = このフィールドのバイオーム、tier = 推奨レベル、
     * road = このフィールドを通る道の種類と状態（省略すると道なし。{@link RoadSettings} 参照）。
     * unlock = 入るのに必要な進行フラグ（例：gate。空なら誰でも入れる。{@link GateRules}）、
     * circle = [x, z, 半径] の円で範囲を決める（旧都など。基準点より優先）、sea = 海（海岸線の外）、
     * mountain_biome = 高い所を山のバイオームにするか（雪山のフィールドは false）。
     */
    public record Field(String id, int number, Holder<Biome> biome, int tier, List<AreaSeed> areas,
                        RoadSettings.FieldRoad road, int mapColor, String unlock, List<Double> circle, boolean sea,
                        boolean mountainBiome) {
        public static final Codec<Field> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Field::id),
                Codec.INT.fieldOf("number").forGetter(Field::number),
                Biome.CODEC.fieldOf("biome").forGetter(Field::biome),
                Codec.INT.fieldOf("tier").forGetter(Field::tier),
                AreaSeed.CODEC.listOf().fieldOf("areas").forGetter(Field::areas),
                RoadSettings.FieldRoad.CODEC.optionalFieldOf("road", RoadSettings.FieldRoad.NONE).forGetter(Field::road),
                COLOR_CODEC.optionalFieldOf("map_color", 0x7FB238).forGetter(Field::mapColor),
                Codec.STRING.optionalFieldOf("unlock", "").forGetter(Field::unlock),
                Codec.DOUBLE.listOf().optionalFieldOf("circle", List.of()).forGetter(Field::circle),
                Codec.BOOL.optionalFieldOf("sea", false).forGetter(Field::sea),
                Codec.BOOL.optionalFieldOf("mountain_biome", true).forGetter(Field::mountainBiome)
        ).apply(i, Field::new));

        /** 円で範囲を決めるフィールド（旧都など）なら、その円の中か。 */
        public boolean inCircle(double x, double z) {
            if (circle.size() < 3) {
                return false;
            }
            double dx = x - circle.get(0);
            double dz = z - circle.get(1);
            return dx * dx + dz * dz <= circle.get(2) * circle.get(2);
        }
    }

    /** 道の判定結果。block = 置くブロック。 */
    public record RoadHit(RoadSettings.Style style, net.minecraft.world.level.block.Block block) {}

    /** 色（"#RRGGBB" の文字列）。地図画面で使う。 */
    public static final Codec<Integer> COLOR_CODEC = Codec.STRING.comapFlatMap(text -> {
        try {
            return DataResult.success(Integer.parseInt(text.startsWith("#") ? text.substring(1) : text, 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "Invalid color: " + text);
        }
    }, color -> String.format("#%06X", color & 0xFFFFFF));

    /** 座標（ブロック単位）。 */
    public record Point(int x, int z) {
        public static final Codec<Point> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x").forGetter(Point::x),
                Codec.INT.fieldOf("z").forGetter(Point::z)
        ).apply(i, Point::new));
    }

    /**
     * 判定結果。danger = 推奨レベル（フィールドの tier。HUD の表示と同じ）。
     * inMountains = 山脈の中（バイオームが山脈になる範囲）かどうか。
     */
    public record Location(Field field, int area, boolean inMountains) {
        public int danger() {
            return field.tier();
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
            RoadSettings.CODEC.optionalFieldOf("roads").forGetter(FieldMap::roads),
            TerrainDesign.CODEC.optionalFieldOf("design").forGetter(FieldMap::design)
    ).apply(i, FieldMap::new)).validate(FieldMap::validate);

    /** フィールドが 1 つもない地図は読み込みエラーにする。 */
    private static DataResult<FieldMap> validate(FieldMap map) {
        return map.fields().isEmpty()
                ? DataResult.error(() -> "Field map must have at least one field")
                : DataResult.success(map);
    }

    /** 「kurumimnmod:astralia」のように、登録済みの地図を名前で参照する Codec。 */
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
    /** 街の周りで、地面を街の高さへなだらかにつなぐ幅（ブロック）。 */
    public static final int TOWN_BLEND = 24;
    /** 港町の港（水）の深さ・位置（街の中心から東へ半径の何割）・大きさ（半径の何割）。 */
    public static final int HARBOR_DEPTH = 4;
    public static final double HARBOR_OFFSET = 0.55;
    public static final double HARBOR_RADIUS = 0.42;

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

    /** 地形 v2 の設計図（あれば、ワールド作成時にその範囲の地形を計算する）。 */
    private final Optional<TerrainDesign> design;
    /** 計算済みの地形 v2（サーバーでワールドを開いている間だけ。{@link TerrainBakes} が入れる）。 */
    private volatile @org.jspecify.annotations.Nullable TerrainBake bake;

    // 海・円のフィールドと、海岸線（設計図の coast）
    private final @org.jspecify.annotations.Nullable Field seaField;
    private final List<Field> circleFields;
    private final double @org.jspecify.annotations.Nullable [] coastXs;
    private final double @org.jspecify.annotations.Nullable [] coastZs;

    // 判定を速くするため、基準点を配列に展開しておく
    private final int[] seedX;
    private final int[] seedZ;
    private final int[] seedArea;
    private final Field[] seedField;

    public FieldMap(Warp warp, List<Town> towns, List<Field> fields, TerrainSettings terrain,
                    Optional<MountainSettings> mountains, Optional<RoadSettings> roads, Optional<TerrainDesign> design) {
        this.design = design;
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

        this.seaField = this.fields.stream().filter(Field::sea).findFirst().orElse(null);
        this.circleFields = this.fields.stream().filter(f -> f.circle().size() >= 3).toList();
        double[][] coast = design.flatMap(d -> d.landforms().stream().filter(l -> l.type().equals("coast")).findFirst())
                .map(l -> io.github.kurumimnm.kurumimnmod.world.terrain.Shapes2D.toArrays(l.points())).orElse(null);
        this.coastXs = coast == null ? null : coast[0];
        this.coastZs = coast == null ? null : coast[1];

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

    public Optional<TerrainDesign> design() {
        return design;
    }

    /** 計算済みの地形 v2 を持たせる（null で外す）。 */
    public void attachBake(@org.jspecify.annotations.Nullable TerrainBake bake) {
        this.bake = bake;
    }

    public @org.jspecify.annotations.Nullable TerrainBake bake() {
        return bake;
    }

    /** id から街を探す。 */
    public Optional<Town> town(String id) {
        return towns.stream().filter(t -> t.id().equals(id)).findFirst();
    }

    /** 番号からフィールドを探す。 */
    public Optional<Field> field(int number) {
        return fields.stream().filter(f -> f.number() == number).findFirst();
    }

    /**
     * その地点の「場所」（街の中なら街、それ以外はフィールドとエリア）。
     * ゲートの壁・場所の名前の表示・地図で使う。
     */
    public FieldRegion regionAt(double x, double z) {
        for (Town town : towns) {
            if (town.contains(x, z)) {
                return FieldRegion.town(town.id(), unlockOf(FieldRegion.town(town.id())));
            }
        }
        Location location = locate(x, z);
        return FieldRegion.field(location.field().number(), location.area(), location.field().unlock());
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
        // 海岸線の外は海のフィールド
        if (seaField != null && coastXs != null
                && io.github.kurumimnm.kurumimnmod.world.terrain.Shapes2D.coastDistance(x, z, coastXs, coastZs) < 0) {
            return new Location(seaField, nearestArea(seaField, x, z), false);
        }
        // 円で決めるフィールド（旧都など）
        for (Field field : circleFields) {
            if (field.inCircle(x, z)) {
                return new Location(field, nearestArea(field, x, z), false);
            }
        }
        // 境界を自然な形にするため、判定する座標を少しずらす
        double nx = x / warp.scale();
        double nz = z / warp.scale();
        double wx = x + warp.amplitude() * FieldNoise.fbm(nx, nz, WARP_SALT_X);
        double wz = z + warp.amplitude() * FieldNoise.fbm(nx, nz, WARP_SALT_Z);

        // いちばん近い基準点を探す（基準点は数百個なので全件調べても十分速い）
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < seedField.length; i++) {
            if (seedField[i].sea() || seedField[i].circle().size() >= 3) {
                continue;
            }
            double dx = seedX[i] - wx;
            double dz = seedZ[i] - wz;
            double distance = dx * dx + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        if (best < 0) {
            best = 0;
        }
        TerrainBake v2 = bake;
        boolean inMountains;
        if (!seedField[best].mountainBiome()) {
            inMountains = false;
        } else if (v2 != null && v2.weight(x, z) >= 0.5) {
            inMountains = mountains.isPresent() && v2.isMountain(x, z); // 地形 v2：高い所が山のバイオーム
        } else {
            inMountains = mountains.isPresent() && mountainPosition(x, z) > mountains.get().biomeThreshold();
        }
        return new Location(seedField[best], seedArea[best], inMountains);
    }

    /** フィールドの中で (x, z) にいちばん近いエリア（高さは見ない）。 */
    private static int nearestArea(Field field, double x, double z) {
        int best = field.areas().isEmpty() ? 1 : field.areas().get(0).area();
        double bestDistance = Double.MAX_VALUE;
        for (AreaSeed seed : field.areas()) {
            double dx = seed.x() - x;
            double dz = seed.z() - z;
            double d = dx * dx + dz * dz;
            if (d < bestDistance) {
                bestDistance = d;
                best = seed.area();
            }
        }
        return best;
    }

    /** 上層・下層を区別するときの高さの重み（高さ 1 ブロックの差を水平何ブロックとみなすか）。 */
    private static final double AREA_Y_WEIGHT = 8.0;

    /**
     * 高さも見た「場所」。同じ XZ に上層と下層のエリアがあるとき（峡谷の上と底、地下など）、高さで分ける。
     * フィールドは {@link #regionAt(double, double)} と同じ（壁は XZ だけで決まる）。
     */
    public FieldRegion regionAt(double x, double y, double z) {
        FieldRegion flat = regionAt(x, z);
        if (flat.isTown()) {
            return flat;
        }
        Optional<Field> field = field(flat.field());
        if (field.isEmpty() || field.get().areas().stream().noneMatch(a -> a.y() != AreaSeed.NO_Y)) {
            return flat;
        }
        int best = flat.area();
        double bestDistance = Double.MAX_VALUE;
        for (AreaSeed seed : field.get().areas()) {
            double dx = seed.x() - x;
            double dz = seed.z() - z;
            double dy = seed.y() == AreaSeed.NO_Y ? 0 : (seed.y() - y) * AREA_Y_WEIGHT;
            double d = dx * dx + dz * dz + dy * dy;
            if (d < bestDistance) {
                bestDistance = d;
                best = seed.area();
            }
        }
        return FieldRegion.field(flat.field(), best, flat.unlock());
    }

    /** その場所に入るのに必要な進行フラグ（空なら不要）。街は、指定がなければ街の中心があるフィールドと同じ。 */
    public String unlockOf(FieldRegion region) {
        if (region.isTown()) {
            Optional<Town> town = town(region.town());
            if (town.isEmpty()) {
                return "";
            }
            if (!town.get().unlock().isEmpty()) {
                return town.get().unlock().equals("none") ? "" : town.get().unlock();
            }
            Location location = locate(town.get().x(), town.get().z());
            return location == null ? "" : location.field().unlock();
        }
        return field(region.field()).map(Field::unlock).orElse("");
    }

    /** 地図で使っている進行フラグの一覧。 */
    public java.util.Set<String> flags() {
        java.util.Set<String> flags = new java.util.TreeSet<>();
        fields.forEach(f -> {
            if (!f.unlock().isEmpty()) {
                flags.add(f.unlock());
            }
        });
        towns.forEach(t -> {
            if (!t.unlock().isEmpty() && !t.unlock().equals("none")) {
                flags.add(t.unlock());
            }
        });
        return flags;
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
        double height;
        TerrainBake v2 = bake;
        if (v2 != null && v2.covers(x, z)) {
            // 地形 v2 の範囲。縁では今までの地形とまぜる
            double w = v2.weight(x, z);
            height = v2.height(x, z, w < 1 ? v1BaseHeight(x, z) : 0);
        } else {
            height = v1BaseHeight(x, z);
        }
        // 街の中は平らにして、周りはなだらかにつなぐ
        for (Town town : towns) {
            double dx = x - town.x();
            double dz = z - town.z();
            double reach = town.radius() + TOWN_BLEND;
            if (Math.abs(dx) > reach || Math.abs(dz) > reach) {
                continue;
            }
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance >= reach) {
                continue;
            }
            // 地面の一番上のブロックがちょうど街の高さになるように +0.5
            double flat = townGroundY(town) + 0.5;
            if (inHarbor(town, x, z)) {
                flat -= HARBOR_DEPTH;
            }
            double t = distance <= town.radius() ? 0 : (distance - town.radius()) / TOWN_BLEND;
            t = t * t * (3 - 2 * t);
            height = flat + (height - flat) * t;
        }
        return Math.min(height, terrain.maxHeight());
    }

    /** 今までの地形（v1）：基本の地面＋山脈。街の平らにする処理の前。 */
    public double v1BaseHeight(double x, double z) {
        double height = terrain.groundHeight(x, z);
        if (mountains.isPresent()) {
            MountainSettings m = mountains.get();
            MountainSettings.Position position = m.position(x, z);
            if (position.p() > 0) {
                height += m.heightAt(position, x, z);
            }
        }
        return height;
    }

    /** 街の地面の高さ（地面の一番上のブロックの Y）。 */
    public int townGroundY(Town town) {
        TerrainBake v2 = bake;
        if (v2 != null && v2.weight(town.x(), town.z()) >= 0.5) {
            return (int) Math.round(v2.rawHeight(town.x(), town.z()));
        }
        return (int) Math.round(terrain.groundHeight(town.x(), town.z()));
    }

    /** 港町の港（水）の中か。 */
    public boolean inHarbor(Town town, double x, double z) {
        if (!"port_town".equals(town.type())) {
            return false;
        }
        double hx = town.x() + town.radius() * HARBOR_OFFSET;
        double dx = x - hx;
        double dz = z - town.z();
        double hr = town.radius() * HARBOR_RADIUS;
        return dx * dx + dz * dz <= hr * hr;
    }

    /**
     * その地点に道があれば、置くブロックを返す（なければ null）。
     * 道はルートの線分の近く・山脈の外・フィールドの road が none 以外の場所にだけできる。
     */
    public RoadHit roadAt(double x, double z) {
        TerrainBake v2 = bake;
        if (v2 != null && v2.weight(x, z) >= 0.5) {
            return roadAtV2(v2, x, z);
        }
        return roadAtV1(x, z);
    }

    /** 地形 v2 の道（計算のときに経路探索で引いた道）。 */
    private RoadHit roadAtV2(TerrainBake v2, double x, double z) {
        if (roads.isEmpty()) {
            return null;
        }
        RoadSettings r = roads.get();
        double distance = v2.roadDistance(x, z);
        if (distance > r.maxHalfWidth() + 1) {
            return null;
        }
        for (Town town : towns) {
            if (town.contains(x, z)) {
                return null;
            }
        }
        if (!Double.isNaN(v2.waterLevel(x, z)) || v2.onBridge(x, z)) {
            return null; // 水の上・橋の上には敷かない（橋は橋の床を別に作る）
        }
        RoadSettings.FieldRoad fieldRoad = locate(x, z).field().road();
        RoadSettings.Style style = r.styles().get(fieldRoad.isNone() ? "dirt" : fieldRoad.style());
        if (style == null) {
            style = r.styles().values().iterator().next();
        }
        double halfWidth = style.width() / 2.0;
        if (distance > halfWidth) {
            return null;
        }
        boolean edge = halfWidth >= 1.5 && distance > halfWidth - 1;
        return new RoadHit(style, edge ? style.edge() : style.surface());
    }

    /** (x, z) のまわり radius 以内で、今までの地形（v1）の道がある一番近い地点（なければ (x, z) のまま）。 */
    public double[] nearestV1Road(double x, double z, int radius) {
        double[] best = {x, z};
        double bestDistance = Double.MAX_VALUE;
        for (int dz = -radius; dz <= radius; dz += 2) {
            for (int dx = -radius; dx <= radius; dx += 2) {
                double d = dx * dx + dz * dz;
                if (d < bestDistance && roadAtV1(x + dx, z + dz) != null) {
                    bestDistance = d;
                    best = new double[]{x + dx, z + dz};
                }
            }
        }
        return best;
    }

    /** 今までの地形（v1）の道：ルートの線分の近く。 */
    private RoadHit roadAtV1(double x, double z) {
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

        // 街の中には街道を敷かない（街の中の道は街の設計図で作る）
        for (Town town : towns) {
            if (town.contains(x, z)) {
                return null;
            }
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
