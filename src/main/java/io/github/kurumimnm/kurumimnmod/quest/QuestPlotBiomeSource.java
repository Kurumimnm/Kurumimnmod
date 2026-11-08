package io.github.kurumimnm.kurumimnmod.quest;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * クエスト用ディメンションの BiomeSource。区画（フィールド）のバイオームをそのまま使い、区画の外は void。
 * <pre>{@code
 * "biome_source": { "type": "kurumimnmod:quest_plots", "biomes": "#kurumimnmod:quest", "void": "kurumimnmod:quest_void" }
 * }</pre>
 * biomes = 区画に使えるバイオームの一覧（クエストの JSON の biome はこの中から選ぶ）。
 */
public final class QuestPlotBiomeSource extends BiomeSource {

    public static final MapCodec<QuestPlotBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(QuestPlotBiomeSource::biomes),
            Biome.CODEC.fieldOf("void").forGetter(QuestPlotBiomeSource::voidBiome)
    ).apply(i, QuestPlotBiomeSource::new));

    private final HolderSet<Biome> biomes;
    private final Holder<Biome> voidBiome;
    /** バイオームの ID → Holder（引いたものを覚えておく）。 */
    private final Map<Identifier, Holder<Biome>> byId = new ConcurrentHashMap<>();

    public QuestPlotBiomeSource(HolderSet<Biome> biomes, Holder<Biome> voidBiome) {
        this.biomes = biomes;
        this.voidBiome = voidBiome;
    }

    public HolderSet<Biome> biomes() {
        return biomes;
    }

    public Holder<Biome> voidBiome() {
        return voidBiome;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(biomes.stream(), Stream.of(voidBiome));
    }

    private Holder<Biome> lookup(Identifier id) {
        return byId.computeIfAbsent(id, key -> biomes.stream()
                .filter(h -> h.unwrapKey().map(k -> k.identifier().equals(key)).orElse(false))
                .findFirst()
                .orElse(voidBiome));
    }

    @Override
    public BiomeResolver createResolver(Climate.Sampler sampler) {
        return (quartX, quartY, quartZ) -> {
            Identifier id = QuestPlots.biome(QuartPos.toBlock(quartX) + 2, QuartPos.toBlock(quartZ) + 2);
            return id == null ? voidBiome : lookup(id);
        };
    }
}
