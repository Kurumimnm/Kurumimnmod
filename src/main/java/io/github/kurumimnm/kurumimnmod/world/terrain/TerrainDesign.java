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
     * 地形の部品。type で意味が変わる。数値の項目は自由に書け（{@link #num}）、文字の項目は {@link #str}。
     * <ul>
     *   <li>{@code coast}：points＝陸地の輪郭（多角形）。外は海。beach＝海へ下がる幅、ocean_floor＝沖の海底の高さ</li>
     *   <li>{@code area}：points（多角形）または center＋radius（円）の範囲を height の高さにする。noise＝起伏、edge＝周りへつなぐ幅、
     *       style＝plain（なだらか）／plateau（崖で区切った台地。崖は崩れない）／dunes（砂丘）／mesas（メサが点在）、
     *       wobble＝輪郭の大きなうねり（ブロック）、lakes＝0 にすると自然の湖を作らない</li>
     *   <li>{@code range}：points に沿った山並み。peak＝頂上の高さ、width＝尾根から麓までの幅、rough＝ごつごつ具合（0〜1）</li>
     *   <li>{@code arc}：center を中心に radius の円弧に沿った山並み（from〜to 度、0 度＝東、90 度＝南）。wobble＝半径の揺れ</li>
     *   <li>{@code basin}：center の周り radius の内側を floor の高さの盆地にする。width＝周りへつなぐ幅</li>
     *   <li>{@code crater}：center の周り radius のすり鉢。floor＝底、rim＝縁の盛り上がり（ブロック）</li>
     *   <li>{@code volcano}：center、radius（裾野）、peak（火口の縁）、crater（火口の半径）、floor（火口の底）、base（裾野の高さ）</li>
     *   <li>{@code canyon}：points に沿った切り立った谷。width＝底の幅、wall＝壁の厚み（小さいほど垂直）、floor→floor_end＝底の高さ、
     *       rim＝縁の高さ、water＝底の水面（沼。省略で水なし）</li>
     *   <li>{@code gorge}：points に沿った切れ込み（侵食の前に入れる。川が流れる峡谷）。width、floor→floor_end、meander＝蛇行</li>
     *   <li>{@code river}：points に沿った川（侵食の後に掘る）。width＝水面の幅、depth＝深さ、level→level_end＝水面の高さ、bank＝岸の幅</li>
     *   <li>{@code lake}：center の周り rx×rz の窪み（depth の深さ）。水がたまって湖になる</li>
     *   <li>{@code bridge}：points の 2 点を結ぶ橋。deck＝橋の面の高さ、width＝幅。道はここを通る</li>
     * </ul>
     * jitter（シードで形を変える）は range・arc・crater にだけかかる。海岸・台地・谷・川・橋は設計どおりの位置。
     */
    public record Landform(String type, List<List<Double>> points, List<Double> center,
                           java.util.Map<String, Double> nums, java.util.Map<String, String> strs) {

        public double num(String key, double fallback) {
            Double v = nums.get(key);
            return v == null ? fallback : v;
        }

        public boolean has(String key) {
            return nums.containsKey(key);
        }

        public String str(String key, String fallback) {
            return strs.getOrDefault(key, fallback);
        }

        public double cx() {
            return center.isEmpty() ? 0 : center.get(0);
        }

        public double cz() {
            return center.size() < 2 ? 0 : center.get(1);
        }

        /** 平らな JSON（{"type":..., "points":..., "peak": 500, "style": "plateau"}）を読む。数値以外の項目は文字として持つ。 */
        public static final Codec<Landform> CODEC = Codec.PASSTHROUGH.comapFlatMap(Landform::parse, Landform::encode);

        private static <T> com.mojang.serialization.DataResult<Landform> parse(com.mojang.serialization.Dynamic<T> dyn) {
            String type = dyn.get("type").asString("");
            if (type.isEmpty()) {
                return com.mojang.serialization.DataResult.error(() -> "landform needs a type");
            }
            List<List<Double>> points = dyn.get("points").asList(p -> p.asList(v -> v.asDouble(0)));
            List<Double> center = dyn.get("center").asList(v -> v.asDouble(0));
            java.util.Map<String, Double> nums = new java.util.TreeMap<>();
            java.util.Map<String, String> strs = new java.util.TreeMap<>();
            dyn.getMapValues().result().ifPresent(map -> map.forEach((k, v) -> {
                String key = k.asString("");
                if (key.equals("type") || key.equals("points") || key.equals("center")) {
                    return;
                }
                var number = v.asNumber().result();
                if (number.isPresent()) {
                    nums.put(key, number.get().doubleValue());
                } else {
                    v.asString().result().ifPresent(text -> strs.put(key, text));
                }
            }));
            return com.mojang.serialization.DataResult.success(new Landform(type, List.copyOf(points), List.copyOf(center),
                    java.util.Collections.unmodifiableMap(nums), java.util.Collections.unmodifiableMap(strs)));
        }

        private com.mojang.serialization.Dynamic<?> encode() {
            com.mojang.serialization.JsonOps ops = com.mojang.serialization.JsonOps.INSTANCE;
            com.google.gson.JsonObject o = new com.google.gson.JsonObject();
            o.addProperty("type", type);
            if (!points.isEmpty()) {
                com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                for (List<Double> p : points) {
                    com.google.gson.JsonArray q = new com.google.gson.JsonArray();
                    p.forEach(q::add);
                    arr.add(q);
                }
                o.add("points", arr);
            }
            if (!center.isEmpty()) {
                com.google.gson.JsonArray q = new com.google.gson.JsonArray();
                center.forEach(q::add);
                o.add("center", q);
            }
            nums.forEach(o::addProperty);
            strs.forEach(o::addProperty);
            return new com.mojang.serialization.Dynamic<>(ops, o);
        }
    }
}
