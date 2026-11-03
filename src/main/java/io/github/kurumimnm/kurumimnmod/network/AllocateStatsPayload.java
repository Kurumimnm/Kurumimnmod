package io.github.kurumimnm.kurumimnmod.network;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.status.AllocType;
import io.github.kurumimnm.kurumimnmod.status.PlayerStats;
import io.github.kurumimnm.kurumimnmod.status.StatManager;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * クライアント → サーバー：ステータス付与画面で「決定」したポイントを送る。
 * <p>
 * サーバー側で所持ポイントを再チェックするので、改造クライアントから不正な値が来ても反映されない。
 *
 * @param points 項目ごとに追加で振るポイント数
 */
public record AllocateStatsPayload(Map<AllocType, Integer> points) implements CustomPacketPayload {

    public static final Type<AllocateStatsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "allocate_stats"));

    public static final StreamCodec<ByteBuf, AllocateStatsPayload> STREAM_CODEC =
            PlayerStats.POINTS_STREAM_CODEC.map(AllocateStatsPayload::new, AllocateStatsPayload::points);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** サーバー側の処理（メインスレッドで実行される）。 */
    public static void handle(AllocateStatsPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            StatManager.allocate(player, payload.points());
        }
    }
}
