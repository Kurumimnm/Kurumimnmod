package io.github.kurumimnm.kurumimnmod.network;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * サーバー → クライアント：計算済みの地形 v2 の粗いデータ（32 ブロック単位の高さと水面、gzip）。
 * 地図画面（M キー）を実際の地形の色・陰影で描くのに使う。ログイン・ディメンション移動のときに送る。
 *
 * @param data {@link io.github.kurumimnm.kurumimnmod.world.terrain.TerrainBake#toNetwork} で作ったもの
 */
public record FieldTerrainPayload(byte[] data) implements CustomPacketPayload {

    /** 受け取る最大の大きさ（バイト）。 */
    public static final int MAX_BYTES = 4 * 1024 * 1024;

    public static final Type<FieldTerrainPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "field_terrain"));

    public static final StreamCodec<ByteBuf, FieldTerrainPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.byteArray(MAX_BYTES), FieldTerrainPayload::data,
            FieldTerrainPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
