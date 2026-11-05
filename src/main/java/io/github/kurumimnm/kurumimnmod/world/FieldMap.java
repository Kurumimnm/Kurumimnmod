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

    /** 判定結果。danger = 危険度（tier + エリア番号）。 */
    public record Location(Field field, int area) {
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
            Field.CODEC.listOf().fieldOf("fields").forGetter(FieldMap::fields)
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

    private final Warp warp;
    private final List<Town> towns;
    private final List<Field> fields;

    // 判定を速くするため、基準点を配列に展開しておく
    private final int[] seedX;
    private final int[] seedZ;
    private final int[] seedArea;
    private final Field[] seedField;

    public FieldMap(Warp warp, List<Town> towns, List<Field> fields) {
        this.warp = warp;
        this.towns = List.copyOf(towns);
        this.fields = List.copyOf(fields);

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
        return new Location(seedField[best], seedArea[best]);
    }
}
