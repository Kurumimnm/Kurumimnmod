package io.github.kurumimnm.kurumimnmod.network;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.job.JobManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** クライアント → サーバー：選択画面で職業を選んだ。選べるかはサーバーで確かめ直す。 */
public record SelectJobPayload(Identifier job) implements CustomPacketPayload {

    public static final Type<SelectJobPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "select_job"));

    public static final StreamCodec<ByteBuf, SelectJobPayload> STREAM_CODEC =
            Identifier.STREAM_CODEC.map(SelectJobPayload::new, SelectJobPayload::job);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SelectJobPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            JobManager.select(player, payload.job());
        }
    }
}
