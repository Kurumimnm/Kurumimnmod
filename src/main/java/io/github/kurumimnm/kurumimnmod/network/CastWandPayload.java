package io.github.kurumimnm.kurumimnmod.network;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.magic.MagicWand;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * クライアント → サーバー：杖を持って左クリックした（魔法を発動したい）。
 * <p>
 * 中身は空。狙う場所・MP・クールダウンはすべてサーバー側で判定する
 * （改造クライアントから好きな場所を指定されないようにするため）。
 */
public record CastWandPayload() implements CustomPacketPayload {

    public static final CastWandPayload INSTANCE = new CastWandPayload();

    public static final Type<CastWandPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "cast_wand"));

    public static final StreamCodec<ByteBuf, CastWandPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** サーバー側の処理（メインスレッドで実行される）。メインハンドの杖で発動する。 */
    public static void handle(CastWandPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.isAlive()) {
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof MagicWand wand) {
                wand.cast(player, stack);
            }
        }
    }
}
