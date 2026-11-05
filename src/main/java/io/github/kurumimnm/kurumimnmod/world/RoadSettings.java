package io.github.kurumimnm.kurumimnmod.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

/**
 * 街道の設定（地図 JSON の {@code roads}）。
 * <p>
 * 街と街を結ぶルート（routes）に沿って道を敷く。道の見た目と有無は、通る場所のフィールドの設定
 * （各フィールドの {@code road}）で決まる。
 * <ul>
 *   <li>{@code "condition": "full"} … 開拓されていて、道がずっと続いている</li>
 *   <li>{@code "condition": "worn"} … 道はあるが、ところどころ途切れている・細い</li>
 *   <li>{@code "condition": "none"} … 道がない（危険な場所・人が入れない環境）</li>
 * </ul>
 * 山脈の中には道を敷かない。
 *
 * @param jitter 道をくねらせる量（amplitude ブロック、scale は曲がりの大きさ）
 * @param styles 道の種類（名前 → 使うブロックと幅）
 * @param routes 道を通すルート
 */
public record RoadSettings(FieldMap.Warp jitter, Map<String, Style> styles, List<Route> routes) {

    /**
     * 道の種類。surface = 道の中央のブロック、edge = 道の端のブロック、width = 道幅（ブロック）。
     */
    public record Style(Block surface, Block edge, int width) {
        public static final Codec<Style> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.BLOCK.byNameCodec().fieldOf("surface").forGetter(Style::surface),
                BuiltInRegistries.BLOCK.byNameCodec().fieldOf("edge").forGetter(Style::edge),
                Codec.intRange(1, 15).fieldOf("width").forGetter(Style::width)
        ).apply(i, Style::new));
    }

    /** ルート。from / to は街の id。via = 途中で通る点（省略可）。 */
    public record Route(String from, String to, List<FieldMap.Point> via) {
        public static final Codec<Route> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("from").forGetter(Route::from),
                Codec.STRING.fieldOf("to").forGetter(Route::to),
                FieldMap.Point.CODEC.listOf().optionalFieldOf("via", List.of()).forGetter(Route::via)
        ).apply(i, Route::new));
    }

    /** フィールドごとの道の設定。style = 道の種類の名前、condition = full / worn / none。 */
    public record FieldRoad(String style, String condition) {
        public static final Codec<FieldRoad> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("style").forGetter(FieldRoad::style),
                Codec.STRING.fieldOf("condition").forGetter(FieldRoad::condition)
        ).apply(i, FieldRoad::new));

        public static final FieldRoad NONE = new FieldRoad("", "none");

        public boolean isNone() {
            return "none".equals(condition);
        }

        public boolean isWorn() {
            return "worn".equals(condition);
        }
    }

    public static final Codec<RoadSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            FieldMap.Warp.CODEC.fieldOf("jitter").forGetter(RoadSettings::jitter),
            Codec.unboundedMap(Codec.STRING, Style.CODEC).fieldOf("styles").forGetter(RoadSettings::styles),
            Route.CODEC.listOf().fieldOf("routes").forGetter(RoadSettings::routes)
    ).apply(i, RoadSettings::new));

    /** 一番広い道の半分の幅（道の近くかどうかの判定に使う）。 */
    public double maxHalfWidth() {
        return styles.values().stream().mapToInt(Style::width).max().orElse(1) / 2.0;
    }
}
