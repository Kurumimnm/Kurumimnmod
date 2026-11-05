package io.github.kurumimnm.kurumimnmod.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * 街道を敷く地物（feature）。チャンクごとに 1 回呼ばれ、そのチャンクの中で道になる場所の地面を道のブロックに置き換える。
 * <p>
 * 道になるかどうか・どのブロックかは {@link FieldMap#roadAt} が決める（地図 JSON の roads とフィールドごとの road）。
 * 木や草より先に置くため、バイオームの「local_modifications」の段階に入れている。
 * <pre>{@code
 * data/kurumimnmod/worldgen/feature/field_roads.json:
 * { "type": "kurumimnmod:field_roads", "map": "kurumimnmod:first_continent" }
 * }</pre>
 */
public record FieldRoadFeature(Holder<FieldMap> map) implements Feature {

    public static final MapCodec<FieldRoadFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            FieldMap.CODEC.fieldOf("map").forGetter(FieldRoadFeature::map)
    ).apply(i, FieldRoadFeature::new));

    @Override
    public MapCodec<FieldRoadFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        FieldMap fieldMap = map.value();
        int chunkX = origin.getX() & ~15;
        int chunkZ = origin.getZ() & ~15;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunkX + dx;
                int z = chunkZ + dz;
                FieldMap.RoadHit hit = fieldMap.roadAt(x, z);
                if (hit == null) {
                    continue;
                }
                // 地面の一番上のブロックを道にする
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                pos.set(x, top, z);
                BlockState ground = level.getBlockState(pos);
                if (ground.isAir()) {
                    continue;
                }
                level.setBlock(pos, hit.block().defaultBlockState(), 2);
                // 道の上の草・雪などを取り除く（2 ブロック分）
                for (int up = 1; up <= 2; up++) {
                    pos.set(x, top + up, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                placed = true;
            }
        }
        return placed;
    }
}
