package io.github.kurumimnm.kurumimnmod.job;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * プレイヤーの職業の状態（データアタッチメント {@code ModAttachments.JOB}）。
 * <p>
 * レベルアップでの上昇量は職業ごとに違うので、「どのレベルからどの職業だったか」を覚えておき、
 * ステータスはその履歴から毎回計算する（Config・JSON の数値を変えても整合が取れる）。
 * 例：剣士で Lv0〜49、Lv50 で魔剣士 → past = [剣士(0)]、job = 魔剣士、since = 50
 *
 * @param unlocked 職業が解放されているか（チュートリアルの達成で解放）
 * @param job      今の職業（まだ選んでいなければ empty）
 * @param since    今の職業になったレベル（最初の職業は初期レベル＝それまでのレベルアップにもさかのぼって効く）
 * @param past     以前の職業（古い順）
 * @param promoted 今の職業の分岐（promotion）をもう選んだか
 */
public record PlayerJob(boolean unlocked, Optional<Identifier> job, int since, List<Stage> past, boolean promoted) {

    public static final PlayerJob EMPTY = new PlayerJob(false, Optional.empty(), ModConstants.INITIAL_LEVEL, List.of(), false);

    /** 以前の職業 1 つ分（fromLevel から、次の職業になるまで）。 */
    public record Stage(Identifier job, int fromLevel) {

        public static final Codec<Stage> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("job").forGetter(Stage::job),
                Codec.INT.fieldOf("from").forGetter(Stage::fromLevel)
        ).apply(i, Stage::new));

        public static final StreamCodec<ByteBuf, Stage> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Stage::job,
                ByteBufCodecs.VAR_INT, Stage::fromLevel,
                Stage::new);
    }

    public static final Codec<PlayerJob> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("unlocked", false).forGetter(PlayerJob::unlocked),
            Identifier.CODEC.optionalFieldOf("job").forGetter(PlayerJob::job),
            Codec.INT.optionalFieldOf("since", ModConstants.INITIAL_LEVEL).forGetter(PlayerJob::since),
            Stage.CODEC.listOf().optionalFieldOf("past", List.of()).forGetter(PlayerJob::past),
            Codec.BOOL.optionalFieldOf("promoted", false).forGetter(PlayerJob::promoted)
    ).apply(i, PlayerJob::new));

    public static final StreamCodec<ByteBuf, PlayerJob> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PlayerJob::unlocked,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), PlayerJob::job,
            ByteBufCodecs.VAR_INT, PlayerJob::since,
            Stage.STREAM_CODEC.apply(ByteBufCodecs.list()), PlayerJob::past,
            ByteBufCodecs.BOOL, PlayerJob::promoted,
            PlayerJob::new);

    public PlayerJob {
        past = List.copyOf(past);
    }

    /** 最初の職業を選べる状態か（解放済みで、まだ選んでいない）。 */
    public boolean canChoose() {
        return unlocked && job.isEmpty();
    }

    public PlayerJob unlock() {
        return new PlayerJob(true, job, since, past, promoted);
    }

    /** 最初の職業を選ぶ（それまでのレベルアップにもさかのぼって効く）。 */
    public PlayerJob choose(Identifier newJob) {
        return new PlayerJob(true, Optional.of(newJob), ModConstants.INITIAL_LEVEL, List.of(), false);
    }

    /**
     * 職業を変える（分岐・上位職・コマンド）。今の職業は履歴に回し、新しい職業は level から効く。
     * まだ職業がなければ {@link #choose} と同じ。
     */
    public PlayerJob changeTo(Identifier newJob, int level) {
        if (job.isEmpty()) {
            return choose(newJob);
        }
        List<Stage> list = new ArrayList<>(past);
        list.add(new Stage(job.get(), since));
        return new PlayerJob(true, Optional.of(newJob), level, list, false);
    }

    /** 分岐で「そのまま続ける」を選んだ。 */
    public PlayerJob markPromoted() {
        return new PlayerJob(unlocked, job, since, past, true);
    }

    /** 職業を取り消す（解放はそのまま。履歴も消える）。 */
    public PlayerJob cleared() {
        return new PlayerJob(unlocked, Optional.empty(), ModConstants.INITIAL_LEVEL, List.of(), false);
    }
}
