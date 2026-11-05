package io.github.kurumimnm.kurumimnmod.network;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.world.FieldLookup;
import io.netty.buffer.ByteBuf;
import net.minecraft.commands.Commands;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * クライアント → サーバー：地図画面でクリックした場所へテレポートしたい（OP 専用）。
 * 権限はサーバー側で確認する。高さはその場所の地面の上。
 *
 * @param x 行き先の X（ブロック）
 * @param z 行き先の Z（ブロック）
 */
public record MapTeleportPayload(int x, int z) implements CustomPacketPayload {

    public static final Type<MapTeleportPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "map_teleport"));

    public static final StreamCodec<ByteBuf, MapTeleportPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MapTeleportPayload::x,
            ByteBufCodecs.VAR_INT, MapTeleportPayload::z,
            MapTeleportPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** サーバー側の処理（メインスレッド）。OP（権限レベル 2 以上）でフィールドマップのワールドにいるときだけ。 */
    public static void handle(MapTeleportPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.level();
        if (!Commands.LEVEL_GAMEMASTERS.check(player.permissions()) || FieldLookup.map(level).isEmpty()) {
            return;
        }
        // チャンクを読み込んで（必要なら生成して）、地面の高さを調べる
        level.getChunk(payload.x() >> 4, payload.z() >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, payload.x(), payload.z());
        player.teleportTo(payload.x() + 0.5, y + 0.1, payload.z() + 0.5);
    }
}
