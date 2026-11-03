package io.github.kurumimnm.kurumimnmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.client.screen.StatusScreen;
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

    /** ステータス画面を開く（初期値: K）。 */
    public static final KeyMapping OPEN_STATUS =
            new KeyMapping("key." + Kurumimnmod.MOD_ID + ".open_status", InputConstants.KEY_K, CATEGORY);

    @SubscribeEvent
    static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN_STATUS);
    }

    /** 毎 tick キー入力を確認して、押されていたら画面を開く。 */
    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN_STATUS.consumeClick()) {
            if (mc.player != null && mc.gui.screen() == null) {
                mc.gui.setScreen(new StatusScreen());
            }
        }
    }
}
