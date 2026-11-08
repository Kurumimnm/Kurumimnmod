package io.github.kurumimnm.kurumimnmod.world.town;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 街の建物の素材（地図 JSON の街の {@code style}）。
 * oak / spruce / birch / dark_oak / acacia / sandstone / medieval（中世：石造りの 1 階＋漆喰と木組みの 2 階）の 7 種類。
 * 知らない名前は oak になる。
 *
 * @param planks     壁
 * @param log        柱（家の角）
 * @param roofStairs 屋根の階段
 * @param roofBlock  屋根の妻（三角の部分）・てっぺん
 * @param door       ドア
 * @param fence      柵・街灯の柱
 * @param foundation 土台・床
 * @param street     街の中の道
 * @param lowerWall  1 階の壁（null なら planks と同じ）。中世の家は石造り
 * @param timbered   2 階より上を木組み（梁と柱を見せる）にするか
 */
public record TownStyle(Block planks, Block log, Block roofStairs, Block roofBlock, Block door, Block fence,
                        Block foundation, Block street, @Nullable Block lowerWall, boolean timbered) {

    public static TownStyle of(String name) {
        return switch (name) {
            case "spruce" -> wood("spruce", "dark_oak_stairs", "dark_oak_planks");
            case "birch" -> wood("birch", "spruce_stairs", "spruce_planks");
            case "dark_oak" -> wood("dark_oak", "deepslate_tile_stairs", "deepslate_tiles");
            case "acacia" -> wood("acacia", "mud_brick_stairs", "mud_bricks");
            case "sandstone" -> new TownStyle(block("smooth_sandstone"), block("cut_sandstone"),
                    block("sandstone_stairs"), block("cut_sandstone"), block("acacia_door"), block("acacia_fence"),
                    block("sandstone"), block("smooth_sandstone"), null, false);
            // 中世：1 階は石、2 階は白い漆喰（カルサイト）に濃い木の梁、屋根は暗い石板
            case "medieval" -> new TownStyle(block("calcite"), block("stripped_dark_oak_log"),
                    block("deepslate_tile_stairs"), block("deepslate_tiles"), block("dark_oak_door"), block("dark_oak_fence"),
                    block("cobblestone"), block("cobblestone"), block("stone_bricks"), true);
            default -> wood("oak", "spruce_stairs", "spruce_planks");
        };
    }

    private static TownStyle wood(String wood, String roofStairs, String roofBlock) {
        return new TownStyle(block(wood + "_planks"), block(wood + "_log"), block(roofStairs), block(roofBlock),
                block(wood + "_door"), block(wood + "_fence"), block("cobblestone"), block("stone_bricks"), null, false);
    }

    /** ブロックを名前（minecraft: を省略）で取り出す。 */
    public static Block block(String id) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id));
    }

    public static BlockState state(String id) {
        return block(id).defaultBlockState();
    }
}
