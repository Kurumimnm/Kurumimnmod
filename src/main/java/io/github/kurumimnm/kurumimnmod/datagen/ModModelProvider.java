package io.github.kurumimnm.kurumimnmod.datagen;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.registry.ModItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.data.PackOutput;

/**
 * アイテム・ブロックのモデル JSON を生成する Provider。
 * <p>
 * Vanilla の ModelProvider を使用。MOD に登録されたアイテム・ブロックのうち
 * モデルが定義されていないものがあると runData がエラーになるので、書き忘れに気付ける。
 * <p>
 * よく使う書き方:
 * <pre>{@code
 * // 平面アイテム（textures/item/walnut.png を使う）
 * itemModels.generateFlatItem(ModItems.WALNUT.get(), ModelTemplates.FLAT_ITEM);
 * // 全面同じテクスチャのブロック（textures/block/walnut_planks.png）＋ブロックアイテム
 * blockModels.createTrivialCube(ModBlocks.WALNUT_PLANKS.get());
 * }</pre>
 * GeckoLib で描画するアイテム・ブロックは、ここではなく GeckoLib 側でモデルを扱う。
 */
public class ModModelProvider extends ModelProvider {

    public ModModelProvider(PackOutput output) {
        super(output, Kurumimnmod.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // ---- ブロック ----

        // ---- アイテム ----
        // 杖・剣などの「手に持つ」アイテムは HANDHELD（持ったときに斜めに構える）
        itemModels.generateFlatItem(ModItems.ICE_WAND.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
    }
}
