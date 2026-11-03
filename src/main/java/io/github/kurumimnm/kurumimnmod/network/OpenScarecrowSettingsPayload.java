package io.github.kurumimnm.kurumimnmod.network;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.scarecrow.ScarecrowSettings;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * サーバー → クライアント：カカシの現在の設定を送り、設定画面を開かせる。
 * （{@link RequestScarecrowSettingsPayload} への返事）
 *
 * @param entityId  カカシのエンティティ ID
 * @param settings  現在の設定
 * @param canDelete 削除ボタンを表示するか（OP のとき true）
 */
public record OpenScarecrowSettingsPayload(int entityId, ScarecrowSettings settings, boolean canDelete)
        implements CustomPacketPayload {

    public static final Type<OpenScarecrowSettingsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "open_scarecrow_settings"));

    public static final StreamCodec<ByteBuf, OpenScarecrowSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenScarecrowSettingsPayload::entityId,
            ScarecrowSettings.STREAM_CODEC, OpenScarecrowSettingsPayload::settings,
            ByteBufCodecs.BOOL, OpenScarecrowSettingsPayload::canDelete,
            OpenScarecrowSettingsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
