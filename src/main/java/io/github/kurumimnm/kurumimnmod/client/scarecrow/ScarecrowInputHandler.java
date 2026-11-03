package io.github.kurumimnm.kurumimnmod.client.scarecrow;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.entity.ScarecrowEntity;
import io.github.kurumimnm.kurumimnmod.item.ScarecrowConfigStickItem;
import io.github.kurumimnm.kurumimnmod.network.RequestScarecrowSettingsPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
 * <p>
 * 狙える距離は通常の攻撃の届く距離ではなく {@link ModConstants#SCARECROW_CONFIG_PICK_RANGE}（遠くからでも可）。
 * 壁越しのカカシは狙えない。カカシ以外を狙っているときは何もしない（普通の左クリック）。
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
        ScarecrowEntity scarecrow = pickScarecrow(mc, player);
        if (scarecrow != null) {
            // 攻撃はしない（腕は振ってクリックした手応えを出す）
            event.setCanceled(true);
            event.setSwingHand(true);
            ClientPacketDistributor.sendToServer(new RequestScarecrowSettingsPayload(scarecrow.getId()));
        }
    }

    /**
     * クロスヘアの先にあるカカシを探す（遠距離対応）。
     * <ol>
     *   <li>近距離はバニラの判定（mc.hitResult）をそのまま使う</li>
     *   <li>それ以外は視線の先をブロックに当たるまで調べ、その手前にあるカカシを探す</li>
     * </ol>
     */
    private static ScarecrowEntity pickScarecrow(Minecraft mc, LocalPlayer player) {
        if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof ScarecrowEntity scarecrow) {
            return scarecrow;
        }
        double range = ModConstants.SCARECROW_CONFIG_PICK_RANGE;
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(range));
        // 壁越しには狙えないよう、先にブロックで視線を止める
        HitResult blockHit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }
        AABB searchBox = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, end, searchBox,
                e -> e instanceof ScarecrowEntity && e.isAlive(), eye.distanceToSqr(end));
        return entityHit != null && entityHit.getEntity() instanceof ScarecrowEntity scarecrow ? scarecrow : null;
    }
}
