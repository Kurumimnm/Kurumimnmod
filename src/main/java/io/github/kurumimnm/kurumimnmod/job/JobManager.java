package io.github.kurumimnm.kurumimnmod.job;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.level.LevelManager;
import io.github.kurumimnm.kurumimnmod.level.PlayerLevelUpEvent;
import io.github.kurumimnm.kurumimnmod.network.OpenJobSelectPayload;
import io.github.kurumimnm.kurumimnmod.quest.QuestCompletedEvent;
import io.github.kurumimnm.kurumimnmod.registry.ModAttachments;
import io.github.kurumimnm.kurumimnmod.status.StatManager;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 職業の解放・選択・分岐と、ステータス計算用の値（サーバー・クライアント共通の読み取りあり）。
 * <ul>
 *   <li><b>解放</b>：{@code unlock_job: true} のクエスト（チュートリアル）を達成すると解放され、選択画面が開く</li>
 *   <li><b>選択</b>：{@code selectable: true} の職業から 1 つ選ぶ。あとから変えられない</li>
 *   <li><b>分岐</b>：職業の promotion.level（Lv50）になると、options から選ぶ画面が開く（自分自身＝そのまま）</li>
 *   <li><b>上位職</b>（剣聖など）：今はコマンド {@code /kmod job set} でのみ変更できる</li>
 * </ul>
 * 職業が変わったら {@link StatManager#applyAttributes} で最大 HP などをすぐ反映する。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class JobManager {

    private JobManager() {}

    private static final String MSG = "message." + Kurumimnmod.MOD_ID + ".job.";

    // =====================================================================
    // 読み取り（サーバー・クライアント共通）
    // =====================================================================

    public static PlayerJob get(Player player) {
        return player.getData(ModAttachments.JOB);
    }

    private static void set(ServerPlayer player, PlayerJob job) {
        player.setData(ModAttachments.JOB, job);
    }

    /** 職業のレジストリ（クライアントにも同期されている）。 */
    public static Optional<Registry<JobDefinition>> registry(Player player) {
        return player.level().registryAccess().lookup(JobRegistry.JOB);
    }

    public static Optional<JobDefinition> definition(Player player, Identifier id) {
        return registry(player).flatMap(r -> r.getOptional(id));
    }

    /** 今の職業の設計（職業なし・データが消えた場合は empty）。 */
    public static Optional<JobDefinition> definition(Player player) {
        return get(player).job().flatMap(id -> definition(player, id));
    }

    /** 今の職業のステータス倍率（職業なしは 1.0）。 */
    public static JobDefinition.Multipliers multipliers(Player player) {
        return definition(player).map(JobDefinition::stats).orElse(JobDefinition.Multipliers.NONE);
    }

    /**
     * 職業によるレベルアップ上昇量の合計（履歴の職業ごとに、その職業だった間のレベル数 × 上昇量）。
     * レベルが下がった場合（コマンド）は、今のレベルまでの分だけ数える。
     */
    public static JobDefinition.LevelGains totalGains(Player player) {
        PlayerJob state = get(player);
        if (state.job().isEmpty()) {
            return JobDefinition.LevelGains.NONE;
        }
        int level = LevelManager.get(player).level();
        double hp = 0, mp = 0, crit = 0;
        List<PlayerJob.Stage> past = state.past();
        for (int i = 0; i < past.size(); i++) {
            int to = i + 1 < past.size() ? past.get(i + 1).fromLevel() : state.since();
            int levels = Math.max(0, Math.min(level, to) - past.get(i).fromLevel());
            if (levels > 0) {
                JobDefinition.LevelGains g = definition(player, past.get(i).job()).map(JobDefinition::perLevel)
                        .orElse(JobDefinition.LevelGains.NONE);
                hp += g.hp() * levels;
                mp += g.mp() * levels;
                crit += g.critChance() * levels;
            }
        }
        int levels = Math.max(0, level - state.since());
        JobDefinition.LevelGains g = definition(player).map(JobDefinition::perLevel).orElse(JobDefinition.LevelGains.NONE);
        return new JobDefinition.LevelGains(hp + g.hp() * levels, mp + g.mp() * levels, crit + g.critChance() * levels);
    }

    /** 分岐の選択肢（分岐できる状態でなければ empty）。 */
    public static Optional<JobDefinition.Promotion> pendingPromotion(Player player) {
        PlayerJob state = get(player);
        if (state.job().isEmpty() || state.promoted()) {
            return Optional.empty();
        }
        return definition(player).flatMap(JobDefinition::promotion)
                .filter(p -> LevelManager.get(player).level() >= p.level());
    }

    /** 選択画面を開く状態か（最初の選択、または分岐）。 */
    public static boolean hasChoice(Player player) {
        return get(player).canChoose() || pendingPromotion(player).isPresent();
    }

    // =====================================================================
    // 変更（サーバー側）
    // =====================================================================

    /** 職業を解放して、選択画面を開く（すでに解放済みなら画面を開くだけ）。 */
    public static void unlock(ServerPlayer player) {
        PlayerJob job = get(player);
        if (!job.unlocked()) {
            set(player, job.unlock());
            player.sendSystemMessage(Component.translatable(MSG + "unlocked"));
        }
        openSelection(player);
    }

    /** 選べる状態なら選択画面を開く（職業がまだなくても開く。画面に「準備中」と出る）。 */
    public static void openSelection(ServerPlayer player) {
        if (hasChoice(player)) {
            PacketDistributor.sendToPlayer(player, OpenJobSelectPayload.INSTANCE);
        }
    }

    /**
     * 選択画面で選んだ職業を受け取る。選べる状態か・選べる職業かはここで確かめ直す。
     * <ul>
     *   <li>最初の選択：selectable の職業のみ</li>
     *   <li>分岐：promotion.options のみ。今の職業と同じなら「そのまま続ける」</li>
     * </ul>
     */
    public static void select(ServerPlayer player, Identifier id) {
        PlayerJob state = get(player);
        if (state.canChoose()) {
            if (definition(player, id).filter(JobDefinition::selectable).isEmpty()) {
                return;
            }
            apply(player, state.choose(id), id);
            return;
        }
        Optional<JobDefinition.Promotion> promotion = pendingPromotion(player);
        if (promotion.isEmpty() || !promotion.get().options().contains(id) || definition(player, id).isEmpty()) {
            return;
        }
        if (state.job().map(id::equals).orElse(false)) {
            set(player, state.markPromoted());
            player.sendSystemMessage(Component.translatable(MSG + "kept", name(id)));
        } else {
            apply(player, state.changeTo(id, LevelManager.get(player).level()), id);
        }
    }

    /** 職業を強制的に変える（OP のコマンド用）。解放されていなくても変えられる。 */
    public static boolean forceSet(ServerPlayer player, Identifier id) {
        if (definition(player, id).isEmpty()) {
            return false;
        }
        apply(player, get(player).changeTo(id, LevelManager.get(player).level()), id);
        return true;
    }

    /** 選んだ職業を取り消す（解放はそのまま。OP のコマンド用）。 */
    public static void reset(ServerPlayer player) {
        set(player, get(player).cleared());
        StatManager.applyAttributes(player);
        openSelection(player);
    }

    private static void apply(ServerPlayer player, PlayerJob state, Identifier id) {
        float oldMax = player.getMaxHealth();
        set(player, state);
        StatManager.applyAttributes(player);
        // 最大 HP が増えた分だけ回復（職業についた直後にバーが減って見えないように）
        float gained = player.getMaxHealth() - oldMax;
        if (gained > 0) {
            player.heal(gained);
        }
        player.sendSystemMessage(Component.translatable(MSG + "selected", name(id)));
    }

    private static Component name(Identifier id) {
        return Component.translatable(JobDefinition.nameKey(id));
    }

    // =====================================================================
    // イベント
    // =====================================================================

    @SubscribeEvent
    static void onQuestCompleted(QuestCompletedEvent event) {
        if (event.getDefinition().unlockJob()) {
            unlock(event.getPlayer());
        }
    }

    /** 分岐のレベルになったら選択画面を開く。レベルアップで増えた最大 HP の分は回復する。 */
    @SubscribeEvent
    static void onLevelUp(PlayerLevelUpEvent event) {
        ServerPlayer player = event.getPlayer();
        if (get(player).job().isEmpty()) {
            return;
        }
        float oldMax = player.getMaxHealth();
        StatManager.applyAttributes(player);
        float gained = player.getMaxHealth() - oldMax;
        if (gained > 0) {
            player.heal(gained);
        }
        if (pendingPromotion(player).isPresent()) {
            player.sendSystemMessage(Component.translatable(MSG + "promotion"));
            openSelection(player);
        }
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        boolean any = registry(player).map(r -> r.size() > 0).orElse(false);
        if (any && hasChoice(player)) {
            openSelection(player);
        }
    }
}
