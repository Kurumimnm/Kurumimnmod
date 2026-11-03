package io.github.kurumimnm.kurumimnmod.registry;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.item.IceWandItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * アイテムの登録クラス。
 * <p>
 * 登録例:
 * <pre>{@code
 * public static final DeferredItem<Item> WALNUT =
 *         ITEMS.registerSimpleItem("walnut", p -> p.stacksTo(64));
 * }</pre>
 * 表示名はハードコードせず、lang ファイルに
 * {@code "item.kurumimnmod.walnut": "..."} の形で定義すること。
 * ここに登録したアイテムは {@link ModCreativeTabs} の MOD タブへ自動で並ぶ。
 */
public final class ModItems {

    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Kurumimnmod.MOD_ID);

    // =====================================================================
    // 魔法の杖
    // =====================================================================

    /** 氷柱の杖（右クリック長押しで照準、左クリックで頭上から氷柱を発射）。 */
    public static final DeferredItem<IceWandItem> ICE_WAND =
            ITEMS.registerItem("ice_wand", IceWandItem::new, p -> p.stacksTo(1));
}
