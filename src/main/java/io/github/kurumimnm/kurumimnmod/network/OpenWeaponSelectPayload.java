package io.github.kurumimnm.kurumimnmod.network;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * サーバー → クライアント：クエストを受ける前に、最初の武器の選択画面を開く。
 * 選択肢はクエストの定義（クライアントにも同期されている）の starter_weapons から出す。
 *
 * @param quest 受けようとしているクエスト
 */
public record OpenWeaponSelectPayload(Identifier quest) implements CustomPacketPayload {

    public static final Type<OpenWeaponSelectPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "open_weapon_select"));

    public static final StreamCodec<ByteBuf, OpenWeaponSelectPayload> STREAM_CODEC =
            Identifier.STREAM_CODEC.map(OpenWeaponSelectPayload::new, OpenWeaponSelectPayload::quest);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
