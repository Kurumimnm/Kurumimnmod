package io.github.kurumimnm.kurumimnmod.job;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 職業の武器制限（サーバー側）。
 * <p>
 * 職業で使えない武器（JSON の weapons に入っていない、タグ kurumimnmod:weapons のアイテム）を
 * メインハンドに持って直接攻撃したとき、そのダメージを無効にしてアクションバーで知らせる。
 * 攻撃そのもの（振る動作）は止めないので、ブロックの採掘などには影響しない。
 * 弓・クロスボウは引き絞り始めを止める（{@link #onUseStart}）。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class JobEvents {

    private JobEvents() {}

    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        // 直接攻撃（矢などの飛び道具は対象外）
        if (!(event.getSource().getDirectEntity() instanceof ServerPlayer player)
                || event.getSource().getEntity() != player) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        boolean allowed = JobManager.definition(player).map(def -> def.canUseWeapon(weapon)).orElse(true);
        if (!allowed) {
            event.setCanceled(true);
            player.sendOverlayMessage(Component.translatable("message." + Kurumimnmod.MOD_ID + ".job.weapon_locked"));
        }
    }

    /**
     * 弓・クロスボウなど「使う（引き絞る）」武器：職業で使えなければ引き始めを止める。
     * クライアント側でも止めて、引き絞る動きが一瞬出ないようにする（職業は本人に同期済み）。
     */
    @SubscribeEvent
    static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack stack = event.getItem();
        boolean allowed = JobManager.definition(player).map(def -> def.canUseWeapon(stack)).orElse(true);
        if (!allowed) {
            event.setCanceled(true);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendOverlayMessage(Component.translatable("message." + Kurumimnmod.MOD_ID + ".job.weapon_locked"));
            }
        }
    }
}
