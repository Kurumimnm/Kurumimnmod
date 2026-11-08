package io.github.kurumimnm.kurumimnmod.registry;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.quest.QuestGateBlock;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * ブロックの登録クラス。
 * <p>
 * 登録例（ブロック本体＋対応する BlockItem）:
 * <pre>{@code
 * public static final DeferredBlock<Block> WALNUT_PLANKS =
 *         BLOCKS.registerSimpleBlock("walnut_planks", p -> p.mapColor(MapColor.WOOD));
 * // ModItems 側:
 * public static final DeferredItem<BlockItem> WALNUT_PLANKS_ITEM =
 *         ITEMS.registerSimpleBlockItem("walnut_planks", ModBlocks.WALNUT_PLANKS);
 * }</pre>
 * 表示名は lang ファイルに {@code "block.kurumimnmod.walnut_planks": "..."} で定義する。
 */
public final class ModBlocks {

    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Kurumimnmod.MOD_ID);

    /**
     * クエストのゲート（フィールドの出口・中間拠点の出発ゲート）。地形の生成で置く。壊せない・光る・アイテムなし。
     */
    public static final DeferredBlock<QuestGateBlock> QUEST_GATE = BLOCKS.registerBlock("quest_gate", QuestGateBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(-1.0F, 3600000.0F).lightLevel(state -> 12).noLootTable());
}
