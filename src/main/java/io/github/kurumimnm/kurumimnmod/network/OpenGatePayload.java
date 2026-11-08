package io.github.kurumimnm.kurumimnmod.network;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * サーバー → クライアント：ゲートの行き先を選ぶ画面を開く。
 *
 * @param from    今いるフィールドの ID（"" = 中間拠点の出発ゲート）
 * @param options 選択肢
 */
public record OpenGatePayload(String from, List<Option> options) implements CustomPacketPayload {

    /**
     * 選択肢 1 つ。
     *
     * @param kind      0 = フィールドへ、1 = 中間拠点へ、2 = クエスト達成（受けた場所へ帰る）
     * @param field     行き先のフィールドの ID（kind = 0 のとき）
     * @param available 今選べるか（false = 条件が足りない）
     */
    public record Option(byte kind, String field, boolean available) {
        public static final StreamCodec<ByteBuf, Option> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BYTE, Option::kind,
                ByteBufCodecs.STRING_UTF8, Option::field,
                ByteBufCodecs.BOOL, Option::available,
                Option::new);
    }

    public static final Type<OpenGatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "open_gate"));

    public static final StreamCodec<ByteBuf, OpenGatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenGatePayload::from,
            Option.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenGatePayload::options,
            OpenGatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
