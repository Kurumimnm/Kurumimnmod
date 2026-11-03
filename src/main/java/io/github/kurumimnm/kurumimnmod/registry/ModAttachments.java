package io.github.kurumimnm.kurumimnmod.registry;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.level.KillContribution;
import io.github.kurumimnm.kurumimnmod.level.PlayerLevel;
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
     * アシスト判定用の記録（誰がダメージ／デバフ／バフで関わったか）。
     * サーバー側だけで使う一時データなので、保存も同期もしない。
     */
    public static final Supplier<AttachmentType<KillContribution>> KILL_CONTRIBUTION = ATTACHMENT_TYPES.register("kill_contribution",
            () -> AttachmentType.builder(KillContribution::new).build());
}
