package io.github.kurumimnm.kurumimnmod.registry;

import com.mojang.serialization.Codec;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.status.PlayerStats;
import net.minecraft.network.codec.ByteBufCodecs;
import io.github.kurumimnm.kurumimnmod.level.KillContribution;
import io.github.kurumimnm.kurumimnmod.level.PlayerLevel;
import io.github.kurumimnm.kurumimnmod.job.PlayerJob;
import io.github.kurumimnm.kurumimnmod.quest.PlayerQuest;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * データアタッチメント（エンティティ等に付ける独自データ）の登録クラス。
 */
public final class ModAttachments {

    private ModAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Kurumimnmod.MOD_ID);

    /**
     * プレイヤーのレベル・経験値。
     * <ul>
     *   <li>セーブデータに保存される</li>
     *   <li>死亡してもリセットされない（copyOnDeath）</li>
     *   <li>本人のクライアントにだけ同期される（HUD 表示用）</li>
     * </ul>
     */
    public static final Supplier<AttachmentType<PlayerLevel>> PLAYER_LEVEL = ATTACHMENT_TYPES.register("player_level",
            () -> AttachmentType.builder(() -> PlayerLevel.DEFAULT)
                    .serialize(PlayerLevel.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, PlayerLevel.STREAM_CODEC)
                    .build());

    /**
     * プレイヤーが振り分けたステータスポイント。
     * 保存・死亡後も維持・本人にだけ同期（ステータス画面・HUD 表示用）。
     */
    public static final Supplier<AttachmentType<PlayerStats>> PLAYER_STATS = ATTACHMENT_TYPES.register("player_stats",
            () -> AttachmentType.builder(() -> PlayerStats.DEFAULT)
                    .serialize(PlayerStats.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, PlayerStats.STREAM_CODEC)
                    .build());

    /**
     * 現在の MP（-1 = 満タン扱い。最大 MP はステータスから計算する）。
     * 保存・本人にだけ同期。死亡時はコピーしない（リスポーン時は満タン）。
     */
    public static final Supplier<AttachmentType<Float>> PLAYER_MANA = ATTACHMENT_TYPES.register("player_mana",
            () -> AttachmentType.builder(() -> -1f)
                    .serialize(Codec.FLOAT.fieldOf("mana"))
                    .sync((holder, to) -> holder == to, ByteBufCodecs.FLOAT)
                    .build());

    /**
     * クエストの状態（受けているクエスト・いる場所・クリアしたフィールドなど）。
     * 保存・死亡後も維持・本人にだけ同期（HUD・クエスト画面に使う）。
     */
    public static final Supplier<AttachmentType<PlayerQuest>> QUEST = ATTACHMENT_TYPES.register("quest",
            () -> AttachmentType.builder(() -> PlayerQuest.EMPTY)
                    .serialize(PlayerQuest.CODEC.fieldOf("quest"))
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, PlayerQuest.STREAM_CODEC)
                    .build());

    /**
     * 職業（解放されたか・選んだ職業）。保存・死亡後も維持・本人にだけ同期（選択画面に使う）。
     */
    public static final Supplier<AttachmentType<PlayerJob>> JOB = ATTACHMENT_TYPES.register("job",
            () -> AttachmentType.builder(() -> PlayerJob.EMPTY)
                    .serialize(PlayerJob.CODEC.fieldOf("job"))
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, PlayerJob.STREAM_CODEC)
                    .build());

    /**
     * 所持金（G）。保存・死亡後も維持・本人にだけ同期（ステータス画面に表示）。
     */
    public static final Supplier<AttachmentType<Long>> MONEY = ATTACHMENT_TYPES.register("money",
            () -> AttachmentType.builder(() -> 0L)
                    .serialize(Codec.LONG.fieldOf("money"))
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, ByteBufCodecs.VAR_LONG)
                    .build());

    /**
     * アシスト判定用の記録（誰がダメージ／デバフ／バフで関わったか）。
     * サーバー側だけで使う一時データなので、保存も同期もしない。
     */
    public static final Supplier<AttachmentType<KillContribution>> KILL_CONTRIBUTION = ATTACHMENT_TYPES.register("kill_contribution",
            () -> AttachmentType.builder(KillContribution::new).build());
}
