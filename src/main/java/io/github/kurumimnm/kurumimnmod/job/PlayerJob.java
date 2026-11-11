package io.github.kurumimnm.kurumimnmod.job;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * プレイヤーの職業の状態（データアタッチメント {@code ModAttachments.JOB}）。
 *
 * @param unlocked 職業が解放されているか（チュートリアルの達成で解放）
 * @param job      選んだ職業（まだ選んでいなければ empty）
 */
public record PlayerJob(boolean unlocked, Optional<Identifier> job) {

    public static final PlayerJob EMPTY = new PlayerJob(false, Optional.empty());

    public static final Codec<PlayerJob> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("unlocked", false).forGetter(PlayerJob::unlocked),
            Identifier.CODEC.optionalFieldOf("job").forGetter(PlayerJob::job)
    ).apply(i, PlayerJob::new));

    public static final StreamCodec<ByteBuf, PlayerJob> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PlayerJob::unlocked,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), PlayerJob::job,
            PlayerJob::new);

    /** 職業を選べる状態か（解放済みで、まだ選んでいない）。 */
    public boolean canChoose() {
        return unlocked && job.isEmpty();
    }

    public PlayerJob unlock() {
        return new PlayerJob(true, job);
    }

    public PlayerJob withJob(Optional<Identifier> newJob) {
        return new PlayerJob(unlocked, newJob);
    }
}
