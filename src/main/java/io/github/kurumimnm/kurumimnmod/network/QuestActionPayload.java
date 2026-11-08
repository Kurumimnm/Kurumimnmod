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
 * クライアント → サーバー：クエスト画面の操作（受注・辞退・フィールドへ移動）。
 *
 * @param action {@link #ACCEPT}／{@link #ABANDON}／{@link #TRAVEL}
 * @param quest  受けるクエストの ID（ACCEPT のとき）
 */
public record QuestActionPayload(byte action, String quest) implements CustomPacketPayload {

    /** クエストを受ける（今は無条件で、すぐ最初のフィールドへ転移）。 */
    public static final byte ACCEPT = 0;
    /** 受けているクエストを辞退する（受けた場所へ戻る）。 */
    public static final byte ABANDON = 1;
    /** クエストの場所（最初のフィールド・中間拠点）へ移動する。 */
    public static final byte TRAVEL = 2;

    public static final Type<QuestActionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "quest_action"));

    public static final StreamCodec<ByteBuf, QuestActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, QuestActionPayload::action,
            ByteBufCodecs.STRING_UTF8, QuestActionPayload::quest,
            QuestActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuestActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        switch (payload.action()) {
            case ACCEPT -> {
                Identifier id = Identifier.tryParse(payload.quest());
                if (id != null) {
                    QuestManager.accept(player, id);
                }
            }
            case ABANDON -> QuestManager.abandon(player);
            case TRAVEL -> QuestManager.travel(player);
            default -> { }
        }
    }
}
