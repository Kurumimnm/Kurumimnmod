package io.github.kurumimnm.kurumimnmod.network;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.entity.ScarecrowEntity;
import io.github.kurumimnm.kurumimnmod.scarecrow.ScarecrowPermissions;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * クライアント → サーバー：設定用ステッキでカカシを左クリックした。設定画面を開きたい。
 * サーバーは距離をチェックしてから {@link OpenScarecrowSettingsPayload} を返す。
 *
 * @param entityId カカシのエンティティ ID
 */
public record RequestScarecrowSettingsPayload(int entityId) implements CustomPacketPayload {

    public static final Type<RequestScarecrowSettingsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "request_scarecrow_settings"));

    public static final StreamCodec<ByteBuf, RequestScarecrowSettingsPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(RequestScarecrowSettingsPayload::new, RequestScarecrowSettingsPayload::entityId);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** サーバー側の処理（メインスレッド）。 */
    public static void handle(RequestScarecrowSettingsPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        ScarecrowEntity scarecrow = ScarecrowPermissions.findTarget(player, payload.entityId());
        if (scarecrow != null) {
            PacketDistributor.sendToPlayer(player, new OpenScarecrowSettingsPayload(
                    scarecrow.getId(), scarecrow.getSettings(), ScarecrowPermissions.canManage(player)));
        }
    }
}
