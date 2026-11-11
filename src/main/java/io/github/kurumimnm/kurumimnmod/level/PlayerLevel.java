package io.github.kurumimnm.kurumimnmod.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * プレイヤーのレベルと経験値（プレイヤーにアタッチして保存・同期するデータ）。
 * <p>
 * 不変（record）なので、値を変えるときは {@link LevelManager} 経由で新しいインスタンスを
 * {@code player.setData(...)} する。setData するとクライアントへ自動で同期される。
 *
 * @param level 現在のレベル（{@link ModConstants#INITIAL_LEVEL} 以上）
 * @param exp   現在のレベル内で貯まっている経験値（次のレベルに上がると 0 から数え直し）
 */
public record PlayerLevel(int level, long exp) {

    /** 初期状態（Lv0・経験値 0）。 */
    public static final PlayerLevel DEFAULT = new PlayerLevel(ModConstants.INITIAL_LEVEL, 0L);

    /** セーブデータ（NBT）用。 */
    public static final MapCodec<PlayerLevel> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(PlayerLevel::level),
            Codec.LONG.fieldOf("exp").forGetter(PlayerLevel::exp)
    ).apply(instance, PlayerLevel::new));

    /** サーバー → クライアント同期用。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerLevel> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlayerLevel::level,
            ByteBufCodecs.VAR_LONG, PlayerLevel::exp,
            PlayerLevel::new);

    /** 次のレベルまでに必要な経験値。 */
    public long requiredExp() {
        return LevelCurve.requiredExp(level);
    }

    /** 次のレベルまでの進捗（0.0〜1.0）。最大レベルのときは 1.0。 */
    public float progress() {
        if (LevelCurve.isMaxLevel(level)) {
            return 1f;
        }
        return Math.clamp((float) ((double) exp / requiredExp()), 0f, 1f);
    }
}
