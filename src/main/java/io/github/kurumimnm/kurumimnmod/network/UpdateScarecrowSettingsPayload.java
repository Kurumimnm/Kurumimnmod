package io.github.kurumimnm.kurumimnmod.network;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.entity.ScarecrowEntity;
import io.github.kurumimnm.kurumimnmod.scarecrow.ScarecrowPermissions;
import io.github.kurumimnm.kurumimnmod.scarecrow.ScarecrowSettings;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * クライアント → サーバー：設定画面で「保存」した。
 * 設定の変更は誰でもできる（距離だけチェック）。値の範囲はサーバー側で丸め直す。
 *
 * @param entityId カカシのエンティティ ID
 * @param settings 新しい設定
 */
public record UpdateScarecrowSettingsPayload(int entityId, ScarecrowSettings settings) implements CustomPacketPayload {

    public static final Type<UpdateScarecrowSettingsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "update_scarecrow_settings"));

    public static final StreamCodec<ByteBuf, UpdateScarecrowSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UpdateScarecrowSettingsPayload::entityId,
            ScarecrowSettings.STREAM_CODEC, UpdateScarecrowSettingsPayload::settings,
            UpdateScarecrowSettingsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** サーバー側の処理（メインスレッド）。 */
    public static void handle(UpdateScarecrowSettingsPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        ScarecrowEntity scarecrow = ScarecrowPermissions.findTarget(player, payload.entityId());
        if (scarecrow == null) {
            player.sendOverlayMessage(Component.translatable("message.kurumimnmod.scarecrow.too_far")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        scarecrow.setSettings(payload.settings().sanitized());
        player.sendOverlayMessage(Component.translatable("message.kurumimnmod.scarecrow.saved")
                .withStyle(ChatFormatting.GREEN));
    }
}
