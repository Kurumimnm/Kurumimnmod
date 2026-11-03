package io.github.kurumimnm.kurumimnmod.client.scarecrow;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.network.OpenScarecrowSettingsPayload;
import io.github.kurumimnm.kurumimnmod.network.ScarecrowDamagePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/**
 * サーバー → クライアントのカカシ関連パケットを受け取ったときの処理（クライアント専用）。
 * パケットの形式は network/ModNetwork で登録済み。ここでは処理だけを登録する（メインスレッドで実行される）。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID, value = Dist.CLIENT)
public final class ScarecrowClientPayloads {

    private ScarecrowClientPayloads() {}

    @SubscribeEvent
    static void onRegisterClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        // 設定画面を開く（ほかの画面を開いている間は開かない）
        event.register(OpenScarecrowSettingsPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gui.screen() == null) {
                mc.gui.setScreen(new ScarecrowSettingsScreen(payload.entityId(), payload.settings(), payload.canDelete()));
            }
        });
        // ダメージ数字を追加
        event.register(ScarecrowDamagePayload.TYPE, (payload, context) -> DamageNumbers.add(payload));
    }
}
