package io.github.kurumimnm.kurumimnmod.registry;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.item.ScarecrowConfigStickItem;
import io.github.kurumimnm.kurumimnmod.item.ScarecrowSummonStickItem;
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
    // カカシ（ステータス実験用）
    // =====================================================================

    /** カカシ召喚用ステッキ（ブロックを右クリックで召喚。OP のみ）。 */
    public static final DeferredItem<ScarecrowSummonStickItem> SCARECROW_SUMMON_STICK =
            ITEMS.registerItem("scarecrow_summon_stick", ScarecrowSummonStickItem::new, p -> p.stacksTo(1));

    /** カカシ設定用ステッキ（カカシを狙って左クリックで設定画面。誰でも使える）。 */
    public static final DeferredItem<ScarecrowConfigStickItem> SCARECROW_CONFIG_STICK =
            ITEMS.registerItem("scarecrow_config_stick", ScarecrowConfigStickItem::new, p -> p.stacksTo(1));
}
