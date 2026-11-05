package io.github.kurumimnm.kurumimnmod.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * フィールドマップに従ってバイオームを決める BiomeSource。
 * <p>
 * バニラのように気候ノイズでランダムに決めるのではなく、{@link FieldMap} の
 * 「いちばん近いエリアの基準点」が属するフィールドのバイオームを使う（山脈の中は山脈のバイオーム）。
 * 高さ（Y）には関係なく、同じ X・Z なら地上も地下も同じバイオームになる。
 * <p>
 * ワールドプリセットの JSON では次のように書く:
 * <pre>{@code
 * "biome_source": { "type": "kurumimnmod:field_map", "map": "kurumimnmod:first_continent" }
 * }</pre>
 */
public final class FieldMapBiomeSource extends BiomeSource {

    public static final MapCodec<FieldMapBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            FieldMap.CODEC.fieldOf("map").forGetter(FieldMapBiomeSource::map)
    ).apply(i, FieldMapBiomeSource::new));

    private final Holder<FieldMap> map;

    public FieldMapBiomeSource(Holder<FieldMap> map) {
        this.map = map;
    }

    public Holder<FieldMap> map() {
        return map;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        FieldMap fieldMap = map.value();
        Stream<Holder<Biome>> fieldBiomes = fieldMap.fields().stream().map(FieldMap.Field::biome);
        return fieldMap.mountains()
                .map(m -> Stream.concat(fieldBiomes, Stream.of(m.biome())))
                .orElse(fieldBiomes);
    }

    @Override
    public BiomeResolver createResolver(Climate.Sampler sampler) {
        return new Resolver(map.value());
    }

    /**
     * 実際にバイオームを返す部分。
     * 同じ柱（X・Z が同じ）を高さ違いで続けて聞かれることが多いので、直前の結果を覚えておく。
     * （複数のスレッドから呼ばれても壊れないよう、覚える内容は 1 つの不変オブジェクトにまとめている）
     */
    private static final class Resolver implements BiomeResolver {
        private record Memo(int quartX, int quartZ, Holder<Biome> biome) {}

        private final FieldMap map;
        private Memo last;

        private Resolver(FieldMap map) {
            this.map = map;
        }

        @Override
        public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ) {
            Memo memo = last;
            if (memo != null && memo.quartX() == quartX && memo.quartZ() == quartZ) {
                return memo.biome();
            }
            // クォート（4 ブロック）単位 → ブロック座標（マスの中心）
            double x = QuartPos.toBlock(quartX) + 2;
            double z = QuartPos.toBlock(quartZ) + 2;
            Holder<Biome> biome = map.biomeAt(x, z);
            last = new Memo(quartX, quartZ, biome);
            return biome;
        }
    }
}
