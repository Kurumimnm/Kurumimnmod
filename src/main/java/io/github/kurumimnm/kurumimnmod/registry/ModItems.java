package io.github.kurumimnm.kurumimnmod.registry;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.item.ScarecrowConfigStickItem;
import io.github.kurumimnm.kurumimnmod.item.ScarecrowSummonStickItem;
import io.github.kurumimnm.kurumimnmod.item.IceWandItem;
import io.github.kurumimnm.kurumimnmod.item.MagicArrowItem;
import net.minecraft.world.item.SpawnEggItem;
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

    // =====================================================================
    // 魔法の杖
    // =====================================================================

    /** 氷柱の杖（右クリック長押しで照準、左クリックで頭上から氷柱を発射）。 */
    public static final DeferredItem<IceWandItem> ICE_WAND =
            ITEMS.registerItem("ice_wand", IceWandItem::new, p -> p.stacksTo(1));

    // =====================================================================
    // 狩人系の武器
    // =====================================================================

    /**
     * 魔法の矢（仮）。弓・クロスボウで撃つと魔法攻撃（知力の倍率）になる。見た目はバニラの矢と同じ（仮）。
     * 入手方法（クラフト）は未定のため、今は耐久無効（スタック可）のみ設定。
     */
    public static final DeferredItem<MagicArrowItem> MAGIC_ARROW =
            ITEMS.registerItem("magic_arrow", MagicArrowItem::new, p -> p.stacksTo(64));

    // =====================================================================
    // スポーンエッグ
    // =====================================================================

    /** グリフォンのスポーンエッグ（テクスチャは textures/item/griffin_spawn_egg.png）。 */
    public static final DeferredItem<SpawnEggItem> GRIFFIN_SPAWN_EGG =
            ITEMS.registerItem("griffin_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.GRIFFIN.get()));
    public static final DeferredItem<SpawnEggItem> MINION_SPAWN_EGG =
            ITEMS.registerItem("minion_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.MINION.get()));
    public static final DeferredItem<SpawnEggItem> SLIME_SPAWN_EGG =
            ITEMS.registerItem("slime_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.SLIME.get()));
    public static final DeferredItem<SpawnEggItem> GAROU_SPAWN_EGG =
            ITEMS.registerItem("garou_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.GAROU.get()));
    public static final DeferredItem<SpawnEggItem> KOBOLD_SPAWN_EGG =
            ITEMS.registerItem("kobold_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.KOBOLD.get()));
    public static final DeferredItem<SpawnEggItem> KOBOLD_LORD_SPAWN_EGG =
            ITEMS.registerItem("kobold_lord_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.KOBOLD_LORD.get()));
    /** アストラ・ヴェイルのスポーンエッグ（見た目の確認用。置いた場所の 52 ブロック上に浮かぶ）。 */
    public static final DeferredItem<SpawnEggItem> ASTRA_VEIL_SPAWN_EGG =
            ITEMS.registerItem("astra_veil_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.ASTRA_VEIL.get()));
    /** アストラのスポーンエッグ（見た目の確認用。置いた場所の 150 ブロック上に浮かぶ）。 */
    public static final DeferredItem<SpawnEggItem> ASTRA_SPAWN_EGG =
            ITEMS.registerItem("astra_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.ASTRA.get()));
}
