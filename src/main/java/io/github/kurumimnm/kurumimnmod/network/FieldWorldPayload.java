package io.github.kurumimnm.kurumimnmod.network;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * サーバー → クライアント：今いるディメンションが使っている地図（フィールドマップ）の名前。
 * フィールドマップのワールドでなければ空。ログイン・ディメンション移動・リスポーンのときに送る。
 * 地図の中身はデータパックのレジストリとして同期済みなので、ここでは名前だけ送る。
 *
 * @param map 地図の名前（例：kurumimnmod:first_continent）
 */
public record FieldWorldPayload(Optional<Identifier> map) implements CustomPacketPayload {

    public static final Type<FieldWorldPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "field_world"));

    public static final StreamCodec<ByteBuf, FieldWorldPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), FieldWorldPayload::map,
            FieldWorldPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
