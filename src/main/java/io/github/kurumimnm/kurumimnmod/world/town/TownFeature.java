package io.github.kurumimnm.kurumimnmod.world.town;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kurumimnm.kurumimnmod.world.FieldMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * 街を建てる地物（feature）。チャンクごとに 1 回呼ばれ、そのチャンクに入る街の部分だけを置く。
 * <p>
 * 街の設計図は {@link TownPlanner} が街ごとに 1 回作る。木や草が生えたあとに片付けてから建てるため、
 * バイオームの「top_layer_modification」の段階の最後に入れている。
 * <pre>{@code
 * data/kurumimnmod/worldgen/feature/field_towns.json:
 * { "type": "kurumimnmod:field_towns", "map": "kurumimnmod:astralia" }
 * }</pre>
 */
public record TownFeature(Holder<FieldMap> map) implements Feature {

    public static final MapCodec<TownFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            FieldMap.CODEC.fieldOf("map").forGetter(TownFeature::map)
    ).apply(i, TownFeature::new));

    /** 片付ける高さ（地面からブロック）。 */
    private static final int CLEAR_HEIGHT = 40;
    /** 地面の穴を埋める深さ（地面からブロック）。 */
    private static final int FILL_DEPTH = 8;

    @Override
    public MapCodec<TownFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        FieldMap fieldMap = map.value();
        ChunkPos chunk = ChunkPos.containing(origin);
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (FieldMap.Town town : fieldMap.towns()) {
            int reach = town.radius() + 8;
            if (minX + 15 < town.x() - reach || minX > town.x() + reach
                    || minZ + 15 < town.z() - reach || minZ > town.z() + reach) {
                continue;
            }
            TownPlan plan = TownPlanner.plan(fieldMap, town);
            // 片付け（地面より上の木・草・雪など）と、地面の穴埋め（池などで空いた穴）
            for (TownPlan.Column column : plan.clearColumns(chunk)) {
                for (int y = plan.groundY() - FILL_DEPTH; y <= plan.groundY(); y++) {
                    pos.set(column.x(), y, column.z());
                    var state = level.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty()) {
                        level.setBlock(pos, (y == plan.groundY() ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState(), 2);
                    }
                }
                for (int y = plan.groundY() + 1; y <= plan.groundY() + CLEAR_HEIGHT; y++) {
                    pos.set(column.x(), y, column.z());
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
            for (TownPlan.Entry entry : plan.entries(chunk)) {
                pos.set(entry.x(), entry.y(), entry.z());
                level.setBlock(pos, entry.state(), 2);
                placed = true;
            }
        }
        return placed;
    }
}
