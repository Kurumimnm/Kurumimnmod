package io.github.kurumimnm.kurumimnmod.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import io.github.kurumimnm.kurumimnmod.world.terrain.TerrainBake;
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
 * { "type": "kurumimnmod:field_roads", "map": "kurumimnmod:astralia" }
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
        TerrainBake bake = fieldMap.bake();
        if (bake != null) {
            for (TerrainBake.Bridge bridge : bake.bridges()) {
                placed |= placeBridge(level, bridge, chunkX, chunkZ);
            }
        }
        return placed;
    }

    // =====================================================================
    // 橋（設計図の bridge）
    // =====================================================================

    /** 橋脚の間隔（ブロック）。 */
    private static final int PILLAR_SPACING = 24;
    /** これより深い所には橋脚を立てない（吊り橋のように見せる）。 */
    private static final int PILLAR_MAX_DEPTH = 70;

    /** このチャンクにかかる橋の部分を作る。床・手すり・橋脚。 */
    private static boolean placeBridge(WorldGenLevel level, TerrainBake.Bridge bridge, int chunkX, int chunkZ) {
        double vx = bridge.toX() - bridge.fromX();
        double vz = bridge.toZ() - bridge.fromZ();
        double length = Math.sqrt(vx * vx + vz * vz);
        if (length < 1) {
            return false;
        }
        double ux = vx / length;
        double uz = vz / length;
        double half = bridge.width() / 2.0;
        BlockState deck = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState rail = Blocks.STONE_BRICK_WALL.defaultBlockState();
        BlockState pillar = Blocks.STONE_BRICKS.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                double px = chunkX + dx + 0.5 - bridge.fromX();
                double pz = chunkZ + dz + 0.5 - bridge.fromZ();
                double along = px * ux + pz * uz;
                double side = Math.abs(-px * uz + pz * ux);
                if (along < -2 || along > length + 2 || side > half) {
                    continue;
                }
                int x = chunkX + dx;
                int z = chunkZ + dz;
                int y = bridge.deckAt(along);
                pos.set(x, y, z);
                level.setBlock(pos, deck, 2);
                boolean edge = side > half - 1;
                for (int up = 1; up <= 4; up++) {
                    pos.set(x, y + up, z);
                    level.setBlock(pos, up == 1 && edge ? rail : Blocks.AIR.defaultBlockState(), 2);
                }
                // 橋脚：一定の間隔で、地面まで（深すぎる所には立てない）
                boolean pillarHere = (((int) Math.floor(along)) % PILLAR_SPACING + PILLAR_SPACING) % PILLAR_SPACING < 3
                        && side < half - 1 && along > 4 && along < length - 4;
                if (pillarHere) {
                    int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                    if (y - ground < PILLAR_MAX_DEPTH) {
                        for (int yy = y - 1; yy >= ground; yy--) {
                            pos.set(x, yy, z);
                            level.setBlock(pos, pillar, 2);
                        }
                    }
                }
                placed = true;
            }
        }
        return placed;
    }
}
