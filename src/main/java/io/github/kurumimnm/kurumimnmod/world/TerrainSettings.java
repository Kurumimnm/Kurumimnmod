package io.github.kurumimnm.kurumimnmod.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

/**
 * 地面の基本の高さ（地図 JSON の {@code terrain}）。
 * <p>
 * このMODの地形はバニラの地形生成を使わず、「地面の高さ」をこのMODで計算して作る。
 * 基本はゆるやかな起伏だけの地面で、山・丘・渓谷・川・洞窟などはすべてこのMODの設定で足していく。
 *
 * @param baseHeight 地面の基準の高さ（Y）
 * @param maxHeight  地面の最高の高さ（Y）。世界の上限（Y447）より少し下にする
 * @param noise      起伏。amplitude = 上下する量（ブロック）、scale = 起伏の波の大きさ（ブロック）。複数重ねられる
 */
public record TerrainSettings(double baseHeight, double maxHeight, List<FieldMap.Warp> noise) {

    public static final Codec<TerrainSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("base_height").forGetter(TerrainSettings::baseHeight),
            Codec.DOUBLE.fieldOf("max_height").forGetter(TerrainSettings::maxHeight),
            FieldMap.Warp.CODEC.listOf().optionalFieldOf("noise", List.of()).forGetter(TerrainSettings::noise)
    ).apply(i, TerrainSettings::new));

    /** 起伏のノイズの種類（1 段目が 505、2 段目が 506 …）。確認用スクリプトと同じ値にすること。 */
    private static final long NOISE_SALT = 505;

    /** 山などを足す前の、地面の高さ。 */
    public double groundHeight(double x, double z) {
        double height = baseHeight;
        for (int i = 0; i < noise.size(); i++) {
            FieldMap.Warp octave = noise.get(i);
            height += octave.amplitude() * FieldNoise.fbm(x / octave.scale(), z / octave.scale(), NOISE_SALT + i);
        }
        return height;
    }
}
