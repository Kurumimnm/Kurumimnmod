package io.github.kurumimnm.kurumimnmod.job;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.network.OpenJobSelectPayload;
import io.github.kurumimnm.kurumimnmod.quest.QuestCompletedEvent;
import io.github.kurumimnm.kurumimnmod.registry.ModAttachments;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 職業の解放と選択（サーバー側）。
 * <ul>
 *   <li><b>解放</b>：{@code unlock_job: true} のクエスト（チュートリアル）を達成すると解放され、選択画面が開く</li>
 *   <li><b>選択</b>：選択画面で 1 つ選ぶ（{@link #select}）。選べるのは 1 回だけ（選び直しは OP の /kmod job reset）</li>
 *   <li>解放済みでまだ選んでいなければ、ログインしたときにも選択画面を開く（職業がまだ 1 つもなければ開かない）</li>
 * </ul>
 * 職業の中身（レベルアップでの上がり方・ステータスの倍率・使える武器）は、職業を追加するときに作る。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class JobManager {

    private JobManager() {}

    private static final String MSG = "message." + Kurumimnmod.MOD_ID + ".job.";

    public static PlayerJob get(ServerPlayer player) {
        return player.getData(ModAttachments.JOB);
    }

    private static void set(ServerPlayer player, PlayerJob job) {
        player.setData(ModAttachments.JOB, job);
    }

    private static Optional<Registry<JobDefinition>> registry(ServerPlayer player) {
        return player.level().getServer().registryAccess().lookup(JobRegistry.JOB);
    }

    private static boolean hasAnyJob(ServerPlayer player) {
        return registry(player).map(r -> r.size() > 0).orElse(false);
    }

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
        if (get(player).canChoose()) {
            PacketDistributor.sendToPlayer(player, OpenJobSelectPayload.INSTANCE);
        }
    }

    /** 職業を選ぶ（選択画面から）。解放済み・まだ選んでいない・その職業がある、のときだけ。 */
    public static void select(ServerPlayer player, Identifier id) {
        PlayerJob job = get(player);
        if (!job.canChoose()) {
            return;
        }
        if (registry(player).flatMap(r -> r.getOptional(id)).isEmpty()) {
            return;
        }
        set(player, job.withJob(Optional.of(id)));
        player.sendSystemMessage(Component.translatable(MSG + "selected", Component.translatable(JobDefinition.nameKey(id))));
    }

    /** 選んだ職業を取り消す（解放はそのまま。OP のコマンド用）。 */
    public static void reset(ServerPlayer player) {
        set(player, get(player).withJob(Optional.empty()));
        openSelection(player);
    }

    @SubscribeEvent
    static void onQuestCompleted(QuestCompletedEvent event) {
        if (event.getDefinition().unlockJob()) {
            unlock(event.getPlayer());
        }
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && get(player).canChoose() && hasAnyJob(player)) {
            openSelection(player);
        }
    }
}
