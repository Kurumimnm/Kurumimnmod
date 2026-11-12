package io.github.kurumimnm.kurumimnmod.network;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** サーバー → クライアント：職業の選択画面を開く（職業が解放されたとき・まだ選んでいない状態でログインしたとき）。 */
public record OpenJobSelectPayload() implements CustomPacketPayload {

    public static final OpenJobSelectPayload INSTANCE = new OpenJobSelectPayload();

    public static final Type<OpenJobSelectPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "open_job_select"));

    public static final StreamCodec<ByteBuf, OpenJobSelectPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
