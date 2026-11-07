package io.github.kurumimnm.kurumimnmod.world.terrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

/**
 * 地形 v2 の「設計図」（地図 JSON の {@code design}）。
 * <p>
 * ここに書くのは「どのあたりに、どんな地形の部品（山脈・盆地・峡谷・湖…）を置くか」だけ。
 * ワールドを作るときに、シード値で部品の形を少しずつ変え、川による侵食をシミュレーションして本物らしい地形にする
 * （{@link TerrainBaker}）。計算した結果はワールドのフォルダに保存する（{@link TerrainBakes}）。
 *
 * @param region     計算する範囲。範囲の外は今までの地形（v1）で、境目は blend ブロックかけてなめらかにつなぐ
 * @param baseHeight 低地の基準の高さ（Y）
 * @param baseNoise  低地の起伏の大きさ（ブロック）
 * @param jitter     シードで部品の点をずらす最大量（ブロック）。盆地・湖の中心・峡谷の両端はずらさない
 * @param erosion    侵食の設定
 * @param river      川の設定
 * @param mountainBiomeAbove この高さ（Y）より上は「山のバイオーム」（岩肌・雪線より上は雪）
 * @param landforms  地形の部品。上から順に重ねる
 */
public record TerrainDesign(Region region, double baseHeight, double baseNoise, double jitter,
                            Erosion erosion, River river, double mountainBiomeAbove, List<Landform> landforms) {

    public static final Codec<TerrainDesign> CODEC = RecordCodecBuilder.create(i -> i.group(
            Region.CODEC.fieldOf("region").forGetter(TerrainDesign::region),
            Codec.DOUBLE.fieldOf("base_height").forGetter(TerrainDesign::baseHeight),
            Codec.DOUBLE.optionalFieldOf("base_noise", 10.0).forGetter(TerrainDesign::baseNoise),
            Codec.DOUBLE.optionalFieldOf("jitter", 100.0).forGetter(TerrainDesign::jitter),
            Erosion.CODEC.fieldOf("erosion").forGetter(TerrainDesign::erosion),
            River.CODEC.fieldOf("river").forGetter(TerrainDesign::river),
            Codec.DOUBLE.optionalFieldOf("mountain_biome_above", 200.0).forGetter(TerrainDesign::mountainBiomeAbove),
            Landform.CODEC.listOf().fieldOf("landforms").forGetter(TerrainDesign::landforms)
    ).apply(i, TerrainDesign::new));

    /**
     * 計算する範囲。
     *
     * @param x0    西の端（X）
     * @param z0    北の端（Z）
     * @param size  一辺の長さ（ブロック）
     * @param cell  計算の細かさ（1 マスのブロック数）。小さいほど細かいが重い（8 推奨）
     * @param blend 範囲の縁で、今までの地形へなめらかにつなぐ幅（ブロック）
     */
    public record Region(int x0, int z0, int size, int cell, int blend) {
        public static final Codec<Region> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("x0").forGetter(Region::x0),
                Codec.INT.fieldOf("z0").forGetter(Region::z0),
                Codec.INT.fieldOf("size").forGetter(Region::size),
                Codec.INT.optionalFieldOf("cell", 8).forGetter(Region::cell),
                Codec.INT.optionalFieldOf("blend", 384).forGetter(Region::blend)
        ).apply(i, Region::new));

        public int cells() {
            return size / cell;
        }
    }

    /**
     * 侵食の設定。
     *
     * @param iterations   繰り返す回数（多いほど谷が深くなる）
     * @param k            川が地面を削る強さ
     * @param talus        崩れずに残れる斜面の角度（度）。ふつうの土地
     * @param talusHigh    高い山（highAbove より上）の角度。大きいほど岩壁が残る
     * @param highAbove    「高い山」とみなす設計の高さ（Y）
     * @param talusGorge   峡谷の壁の角度（ほぼ垂直にする）
     */
    public record Erosion(int iterations, double k, double talus, double talusHigh, double highAbove, double talusGorge) {
        public static final Codec<Erosion> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("iterations", 120).forGetter(Erosion::iterations),
                Codec.DOUBLE.optionalFieldOf("k", 0.012).forGetter(Erosion::k),
                Codec.DOUBLE.optionalFieldOf("talus", 40.0).forGetter(Erosion::talus),
                Codec.DOUBLE.optionalFieldOf("talus_high", 52.0).forGetter(Erosion::talusHigh),
                Codec.DOUBLE.optionalFieldOf("high_above", 260.0).forGetter(Erosion::highAbove),
                Codec.DOUBLE.optionalFieldOf("talus_gorge", 80.0).forGetter(Erosion::talusGorge)
        ).apply(i, Erosion::new));
    }

    /**
     * 川の設定。
     *
     * @param minCells この数より多いマスから水が集まる所を川にする
     * @param depth    川の深さ（ブロック）
     */
    public record River(int minCells, double depth) {
        public static final Codec<River> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("min_cells", 900).forGetter(River::minCells),
                Codec.DOUBLE.optionalFieldOf("depth", 3.0).forGetter(River::depth)
        ).apply(i, River::new));
    }

    /**
     * 地形の部品。type で中身の意味が変わる（使わない項目は省略してよい）。
     * <ul>
     *   <li>{@code range}：points に沿った山並み。peak＝頂上の高さ、width＝尾根から麓までの幅、rough＝ごつごつ具合（0〜1）</li>
     *   <li>{@code arc}：center を中心に radius の円弧に沿った山並み（from〜to 度、0 度＝東、90 度＝南）。wobble＝半径の揺れ</li>
     *   <li>{@code basin}：center の周り radius の内側を floor の高さの盆地にする。width＝周りの山へつなぐ幅</li>
     *   <li>{@code gorge}：points に沿って幅 width の切れ込み。底の高さは floor から floorEnd へ下がる。meander＝蛇行の大きさ</li>
     *   <li>{@code lake}：center の周り rx×rz の窪み（depth の深さ）。水がたまって湖になる</li>
     * </ul>
     */
    public record Landform(String type, List<List<Double>> points, List<Double> center, double peak, double width,
                           double rough, double radius, double from, double to, double wobble, double floor,
                           double floorEnd, double meander, double rx, double rz, double depth) {
        private static final Codec<List<Double>> POINT = Codec.DOUBLE.listOf();
        public static final Codec<Landform> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("type").forGetter(Landform::type),
                POINT.listOf().optionalFieldOf("points", List.of()).forGetter(Landform::points),
                POINT.optionalFieldOf("center", List.of(0.0, 0.0)).forGetter(Landform::center),
                Codec.DOUBLE.optionalFieldOf("peak", 0.0).forGetter(Landform::peak),
                Codec.DOUBLE.optionalFieldOf("width", 0.0).forGetter(Landform::width),
                Codec.DOUBLE.optionalFieldOf("rough", 0.6).forGetter(Landform::rough),
                Codec.DOUBLE.optionalFieldOf("radius", 0.0).forGetter(Landform::radius),
                Codec.DOUBLE.optionalFieldOf("from", 0.0).forGetter(Landform::from),
                Codec.DOUBLE.optionalFieldOf("to", 0.0).forGetter(Landform::to),
                Codec.DOUBLE.optionalFieldOf("wobble", 0.0).forGetter(Landform::wobble),
                Codec.DOUBLE.optionalFieldOf("floor", 0.0).forGetter(Landform::floor),
                Codec.DOUBLE.optionalFieldOf("floor_end", 0.0).forGetter(Landform::floorEnd),
                Codec.DOUBLE.optionalFieldOf("meander", 0.0).forGetter(Landform::meander),
                Codec.DOUBLE.optionalFieldOf("rx", 0.0).forGetter(Landform::rx),
                Codec.DOUBLE.optionalFieldOf("rz", 0.0).forGetter(Landform::rz),
                Codec.DOUBLE.optionalFieldOf("depth", 0.0).forGetter(Landform::depth)
        ).apply(i, Landform::new));
    }
}
