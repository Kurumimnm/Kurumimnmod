package io.github.kurumimnm.kurumimnmod.client.magic;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.magic.MagicWand;
import io.github.kurumimnm.kurumimnmod.network.CastWandPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * 杖の左クリック（発動）の入力処理（クライアント専用）。
 * <ol>
 *   <li>tick の最初（バニラが攻撃キーを処理する前）に攻撃キーのクリックを横取りし、
 *       クリックがあれば {@link CastWandPayload} をサーバーへ送る。
 *       照準（右クリック長押し）中でも、照準なしでも発動できる</li>
 *   <li>杖を持っている間は、左クリック長押しによるブロック破壊・近接攻撃を無効にする</li>
 * </ol>
 * 攻撃キーの割り当てを変えていても、そのキーで発動する。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID, value = Dist.CLIENT)
public final class WandInputHandler {

    private WandInputHandler() {}

    @SubscribeEvent
    static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gui.screen() != null || !holdingWand(player)) {
            return;
        }
        boolean clicked = false;
        while (mc.options.keyAttack.consumeClick()) {
            clicked = true;
        }
        // 連打しても 1 tick に 1 回だけ送る（クールダウンはサーバー側で判定）
        if (clicked) {
            ClientPacketDistributor.sendToServer(CastWandPayload.INSTANCE);
        }
    }

    /** 杖を持っている間は、攻撃キーによるブロック破壊・攻撃をしない（腕も振らない）。 */
    @SubscribeEvent
    static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (event.isAttack() && player != null && holdingWand(player)) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    private static boolean holdingWand(LocalPlayer player) {
        return player.getMainHandItem().getItem() instanceof MagicWand;
    }
}
