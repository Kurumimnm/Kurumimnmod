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
 * クライアント → サーバー：最初の武器を選んでクエストを受ける。受けられるかはサーバーで確かめ直す。
 *
 * @param quest  受けるクエスト
 * @param weapon 選んだ武器（starter_weapons の番号）
 */
public record ChooseWeaponPayload(Identifier quest, int weapon) implements CustomPacketPayload {

    public static final Type<ChooseWeaponPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "choose_weapon"));

    public static final StreamCodec<ByteBuf, ChooseWeaponPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, ChooseWeaponPayload::quest,
            ByteBufCodecs.VAR_INT, ChooseWeaponPayload::weapon,
            ChooseWeaponPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChooseWeaponPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            QuestManager.accept(player, payload.quest(), payload.weapon());
        }
    }
}
