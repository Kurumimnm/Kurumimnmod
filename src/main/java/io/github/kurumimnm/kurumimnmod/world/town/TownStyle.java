package io.github.kurumimnm.kurumimnmod.world.town;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 街の建物の素材（地図 JSON の街の {@code style}）。
 * oak / spruce / birch / dark_oak / acacia / sandstone の 6 種類。知らない名前は oak になる。
 *
 * @param planks     壁
 * @param log        柱（家の角）
 * @param roofStairs 屋根の階段
 * @param roofBlock  屋根の妻（三角の部分）・てっぺん
 * @param door       ドア
 * @param fence      柵・街灯の柱
 * @param foundation 土台・床
 * @param street     街の中の道
 */
public record TownStyle(Block planks, Block log, Block roofStairs, Block roofBlock, Block door, Block fence,
                        Block foundation, Block street) {

    public static TownStyle of(String name) {
        return switch (name) {
            case "spruce" -> wood("spruce", "dark_oak_stairs", "dark_oak_planks");
            case "birch" -> wood("birch", "spruce_stairs", "spruce_planks");
            case "dark_oak" -> wood("dark_oak", "deepslate_tile_stairs", "deepslate_tiles");
            case "acacia" -> wood("acacia", "mud_brick_stairs", "mud_bricks");
            case "sandstone" -> new TownStyle(block("smooth_sandstone"), block("cut_sandstone"),
                    block("sandstone_stairs"), block("cut_sandstone"), block("acacia_door"), block("acacia_fence"),
                    block("sandstone"), block("smooth_sandstone"));
            default -> wood("oak", "spruce_stairs", "spruce_planks");
        };
    }

    private static TownStyle wood(String wood, String roofStairs, String roofBlock) {
        return new TownStyle(block(wood + "_planks"), block(wood + "_log"), block(roofStairs), block(roofBlock),
                block(wood + "_door"), block(wood + "_fence"), block("cobblestone"), block("stone_bricks"));
    }

    /** ブロックを名前（minecraft: を省略）で取り出す。 */
    public static Block block(String id) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id));
    }

    public static BlockState state(String id) {
        return block(id).defaultBlockState();
    }
}
