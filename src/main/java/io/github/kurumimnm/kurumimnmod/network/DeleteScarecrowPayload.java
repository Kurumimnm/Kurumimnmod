package io.github.kurumimnm.kurumimnmod.network;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.entity.ScarecrowEntity;
import io.github.kurumimnm.kurumimnmod.scarecrow.ScarecrowPermissions;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * クライアント → サーバー：設定画面の「削除」ボタンを押した。
 * OP（権限レベル 2 以上）でなければ何もしない（ボタンは OP にしか表示されないが、念のためサーバーでも確認）。
 *
 * @param entityId カカシのエンティティ ID
 */
public record DeleteScarecrowPayload(int entityId) implements CustomPacketPayload {

    public static final Type<DeleteScarecrowPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "delete_scarecrow"));

    public static final StreamCodec<ByteBuf, DeleteScarecrowPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(DeleteScarecrowPayload::new, DeleteScarecrowPayload::entityId);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** サーバー側の処理（メインスレッド）。 */
    public static void handle(DeleteScarecrowPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!ScarecrowPermissions.canManage(player)) {
            player.sendOverlayMessage(Component.translatable("message.kurumimnmod.scarecrow.op_only")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        ScarecrowEntity scarecrow = ScarecrowPermissions.findTarget(player, payload.entityId());
        if (scarecrow == null) {
            player.sendOverlayMessage(Component.translatable("message.kurumimnmod.scarecrow.not_found")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        scarecrow.level().playSound(null, scarecrow.getX(), scarecrow.getY(), scarecrow.getZ(),
                SoundEvents.ARMOR_STAND_BREAK, SoundSource.BLOCKS, 1f, 0.8f);
        scarecrow.discard();
        player.sendOverlayMessage(Component.translatable("message.kurumimnmod.scarecrow.deleted"));
    }
}
