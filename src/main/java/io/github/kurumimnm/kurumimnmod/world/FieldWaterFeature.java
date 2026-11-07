package io.github.kurumimnm.kurumimnmod.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.world.terrain.TerrainBake;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * 地形 v2 の湖・川の水を置く地物（feature）。
 * <p>
 * 計算済みの水面（{@link TerrainBake#waterLevel}）が地面より高い所に水を入れ、底を砂・砂利にする。
 * 街の中とそのまわり（平らにする所）には置かない。道より先に置く（local_modifications の最初）。
 */
public record FieldWaterFeature(Holder<FieldMap> map) implements Feature {

    public static final MapCodec<FieldWaterFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            FieldMap.CODEC.fieldOf("map").forGetter(FieldWaterFeature::map)
    ).apply(i, FieldWaterFeature::new));

    @Override
    public MapCodec<FieldWaterFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        FieldMap fieldMap = map.value();
        TerrainBake bake = fieldMap.bake();
        if (bake == null) {
            return false;
        }
        int chunkX = origin.getX() & ~15;
        int chunkZ = origin.getZ() & ~15;
        if (!bake.covers(chunkX, chunkZ) && !bake.covers(chunkX + 15, chunkZ + 15)) {
            return false;
        }
        BlockState waterState = Blocks.WATER.defaultBlockState();
        BlockState shallow = block(ModConstants.TERRAIN_WATER_BED_SHALLOW);
        BlockState deep = block(ModConstants.TERRAIN_WATER_BED_DEEP);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = chunkX + dx;
                int z = chunkZ + dz;
                double levelY = bake.waterLevel(x + 0.5, z + 0.5);
                if (Double.isNaN(levelY) || nearTown(fieldMap, x, z)) {
                    continue;
                }
                int surface = (int) Math.floor(levelY);
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                if (surface <= top) {
                    continue;
                }
                // 底
                pos.set(x, top, z);
                level.setBlock(pos, surface - top >= ModConstants.TERRAIN_WATER_DEEP ? deep : shallow, 2);
                // 水
                for (int y = top + 1; y <= surface; y++) {
                    pos.set(x, y, z);
                    level.setBlock(pos, waterState, 2);
                }
                placed = true;
            }
        }
        return placed;
    }

    private static boolean nearTown(FieldMap map, int x, int z) {
        for (FieldMap.Town town : map.towns()) {
            double dx = x - town.x();
            double dz = z - town.z();
            double reach = town.radius() + FieldMap.TOWN_BLEND;
            if (dx * dx + dz * dz < reach * reach) {
                return true;
            }
        }
        return false;
    }

    private static BlockState block(String id) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id)).defaultBlockState();
    }
}
