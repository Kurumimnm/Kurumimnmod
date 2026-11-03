package io.github.kurumimnm.kurumimnmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.client.screen.StatusPageScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * MOD のキー割り当て（操作設定画面の「Kurumimnmod」カテゴリに表示される）。
 * <p>
 * 表示名は lang ファイルの {@code key.kurumimnmod.*} / {@code key.category.kurumimnmod.main}。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID, value = Dist.CLIENT)
public final class ModKeyMappings {

    private ModKeyMappings() {}

    /** 操作設定のカテゴリ。 */
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "main"));

    /**
     * ステータス画面を開く（初期値: Tab）。
     * ※ バニラの「プレイヤー一覧」と同じキーなので、マルチでは一覧も同時に表示される。操作設定で変更可能。
     */
    public static final KeyMapping OPEN_STATUS =
            new KeyMapping("key." + Kurumimnmod.MOD_ID + ".open_status", InputConstants.KEY_TAB, CATEGORY);

    /** ステータス付与画面を開く（初期値: K）。 */
    public static final KeyMapping OPEN_ALLOCATION =
            new KeyMapping("key." + Kurumimnmod.MOD_ID + ".open_allocation", InputConstants.KEY_K, CATEGORY);

    @SubscribeEvent
    static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN_STATUS);
        event.register(OPEN_ALLOCATION);
    }

    /** 毎 tick キー入力を確認して、押されていたら対応する画面を開く。 */
    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        // 画面を開いている間のキー操作（切り替え・閉じる）は StatusPageScreen#keyPressed で処理する
        while (OPEN_STATUS.consumeClick()) {
            open(mc, StatusPageScreen.Page.STATUS);
        }
        while (OPEN_ALLOCATION.consumeClick()) {
            open(mc, StatusPageScreen.Page.ALLOCATION);
        }
    }

    private static void open(Minecraft mc, StatusPageScreen.Page page) {
        if (mc.player != null && mc.gui.screen() == null) {
            mc.gui.setScreen(StatusPageScreen.create(page));
        }
    }
}
