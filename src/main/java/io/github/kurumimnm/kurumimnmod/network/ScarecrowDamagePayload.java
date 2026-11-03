package io.github.kurumimnm.kurumimnmod.network;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * サーバー → クライアント：カカシがダメージを受けたので、頭上に数字を出す。
 * カカシを見ている（追跡している）全プレイヤーに送る。
 *
 * @param entityId カカシのエンティティ ID
 * @param amount   最終ダメージ
 * @param kind     表示の種類（DamageKind の番号）
 */
public record ScarecrowDamagePayload(int entityId, float amount, int kind) implements CustomPacketPayload {

    public static final Type<ScarecrowDamagePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "scarecrow_damage"));

    public static final StreamCodec<ByteBuf, ScarecrowDamagePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ScarecrowDamagePayload::entityId,
            ByteBufCodecs.FLOAT, ScarecrowDamagePayload::amount,
            ByteBufCodecs.VAR_INT, ScarecrowDamagePayload::kind,
            ScarecrowDamagePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
