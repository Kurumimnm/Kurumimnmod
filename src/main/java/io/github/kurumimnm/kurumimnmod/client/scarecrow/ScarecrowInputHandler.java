package io.github.kurumimnm.kurumimnmod.client.scarecrow;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.entity.ScarecrowEntity;
import io.github.kurumimnm.kurumimnmod.item.ScarecrowConfigStickItem;
import io.github.kurumimnm.kurumimnmod.network.RequestScarecrowSettingsPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * 設定用ステッキの左クリック（クライアント専用）。
 * <p>
 * 設定用ステッキをメインハンドに持ち、カカシにカーソル（クロスヘア）を合わせて攻撃キーを押すと、
 * 攻撃の代わりにサーバーへ設定をリクエストする。返事（OpenScarecrowSettingsPayload）が届くと設定画面が開く。
 * カカシ以外を狙っているときは何もしない（普通の左クリック）。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID, value = Dist.CLIENT)
public final class ScarecrowInputHandler {

    private ScarecrowInputHandler() {}

    @SubscribeEvent
    static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !(player.getMainHandItem().getItem() instanceof ScarecrowConfigStickItem)) {
            return;
        }
        if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof ScarecrowEntity scarecrow) {
            // 攻撃はしない（腕は振ってクリックした手応えを出す）
            event.setCanceled(true);
            event.setSwingHand(true);
            ClientPacketDistributor.sendToServer(new RequestScarecrowSettingsPayload(scarecrow.getId()));
        }
    }
}
