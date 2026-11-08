package io.github.kurumimnm.kurumimnmod.network;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.quest.QuestManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * クライアント → サーバー：ゲートの画面で行き先を選んだ。条件はサーバーで確かめ直す。
 *
 * @param kind  {@link OpenGatePayload.Option#kind()} と同じ
 * @param field 行き先のフィールドの ID
 */
public record ChooseGatePayload(byte kind, String field) implements CustomPacketPayload {

    public static final Type<ChooseGatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "choose_gate"));

    public static final StreamCodec<ByteBuf, ChooseGatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, ChooseGatePayload::kind,
            ByteBufCodecs.STRING_UTF8, ChooseGatePayload::field,
            ChooseGatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChooseGatePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            QuestManager.chooseGate(player, payload.kind(), payload.field());
        }
    }
}
