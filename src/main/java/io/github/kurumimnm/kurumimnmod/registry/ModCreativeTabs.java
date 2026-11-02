package io.github.kurumimnm.kurumimnmod.registry;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * クリエイティブタブの登録クラス。
 * <p>
 * MOD 専用タブを 1 つ用意し、{@link ModItems#ITEMS} に登録された
 * すべてのアイテム（BlockItem 含む）を登録順に自動で並べる。
 * そのため、アイテムを追加するたびにここを編集する必要はない。
 */
public final class ModCreativeTabs {

    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Kurumimnmod.MOD_ID);

    /** Kurumimnmod のメインタブ（戦闘タブの後ろに配置）。 */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    // タブ名は lang ファイルの "itemGroup.kurumimnmod.main"
                    .title(Component.translatable("itemGroup." + Kurumimnmod.MOD_ID + ".main"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    // TODO: MOD のアイテムができたら、代表アイテムのアイコンに差し替える
                    .icon(() -> Items.BOOK.getDefaultInstance())
                    .displayItems((parameters, output) ->
                            ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                    .build());
}
