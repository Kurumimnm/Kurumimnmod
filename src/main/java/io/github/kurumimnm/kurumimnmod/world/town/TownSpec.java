package io.github.kurumimnm.kurumimnmod.world.town;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

/**
 * 建てる街の設定（地物の JSON に書く）。
 * <pre>{@code
 * { "id": "start_town", "x": 0, "z": 0, "radius": 72, "type": "start_town", "style": "medieval", "ground_y": 64 }
 * }</pre>
 *
 * @param id      街の名前（設計図の乱数のもと。同じ id なら毎回同じ街になる）
 * @param x       中心の X
 * @param z       中心の Z
 * @param radius  範囲（ブロック）
 * @param type    種類（{@link TownPlanner} の一覧）
 * @param style   素材（{@link TownStyle}）
 * @param groundY 地面の一番上のブロックの Y
 */
public record TownSpec(String id, int x, int z, int radius, String type, String style, int groundY) {

    public static final Codec<TownSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(TownSpec::id),
            Codec.INT.fieldOf("x").forGetter(TownSpec::x),
            Codec.INT.fieldOf("z").forGetter(TownSpec::z),
            Codec.INT.fieldOf("radius").forGetter(TownSpec::radius),
            Codec.STRING.fieldOf("type").forGetter(TownSpec::type),
            Codec.STRING.optionalFieldOf("style", "oak").forGetter(TownSpec::style),
            Codec.INT.fieldOf("ground_y").forGetter(TownSpec::groundY)
    ).apply(i, TownSpec::new));

    /** 道の向き（東・南・西・北の十字。ラジアン、東 = 0、南 = π/2）。 */
    public List<Double> streetAngles() {
        return List.of(0.0, Math.PI / 2, Math.PI, Math.PI * 1.5);
    }
}
