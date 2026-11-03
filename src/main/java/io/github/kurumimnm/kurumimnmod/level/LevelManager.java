package io.github.kurumimnm.kurumimnmod.level;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import io.github.kurumimnm.kurumimnmod.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

/**
 * プレイヤーのレベル・経験値を読み書きする窓口。
 * <p>
 * レベルや経験値を変更するときは、必ずこのクラスのメソッドを使うこと
 * （レベルアップ処理・通知・クライアント同期をここでまとめて行うため）。
 */
public final class LevelManager {

    private LevelManager() {}

    /** 現在のレベル・経験値を取得する（クライアント側でも使える）。 */
    public static PlayerLevel get(Player player) {
        return player.getData(ModAttachments.PLAYER_LEVEL);
    }

    /** 経験値を獲得した理由（通知メッセージの出し分け用）。 */
    public enum Reason {
        /** とどめを刺した */
        KILL,
        /** アシスト */
        ASSIST,
        /** コマンド等（通知なし） */
        COMMAND
    }

    /**
     * 経験値を加算する。必要経験値に達したらレベルアップ（一度に複数レベル上がることもある）。
     * あふれた経験値は次のレベルに持ち越す。最大レベルでは経験値は貯まらない。
     *
     * @return 上がったレベル数
     */
    public static int addExp(ServerPlayer player, long amount, Reason reason) {
        if (amount <= 0) {
            return 0;
        }
        PlayerLevel current = get(player);
        int level = current.level();
        long exp = current.exp() + amount;
        if (exp < 0) {
            exp = Long.MAX_VALUE; // オーバーフロー対策
        }

        // ---- レベルアップ処理（必要経験値を引きながら繰り返す） ----
        while (!LevelCurve.isMaxLevel(level)) {
            long required = LevelCurve.requiredExp(level);
            if (exp < required) {
                break;
            }
            exp -= required;
            level++;
        }
        if (LevelCurve.isMaxLevel(level)) {
            exp = 0;
        }

        player.setData(ModAttachments.PLAYER_LEVEL, new PlayerLevel(level, exp));

        // ---- 通知 ----
        if (reason != Reason.COMMAND && ModConfigs.showExpMessages()) {
            String key = reason == Reason.ASSIST ? "message.kurumimnmod.exp_gain.assist" : "message.kurumimnmod.exp_gain";
            player.sendSystemMessage(Component.translatable(key, amount).withStyle(ChatFormatting.YELLOW), true);
        }
        int gained = level - current.level();
        if (gained > 0) {
            onLevelUp(player, current.level(), level);
        }
        if (ModConfigs.isDebugLogging()) {
            Kurumimnmod.LOGGER.info("[{}] {} +{} exp ({}) -> Lv{} {}/{}", Kurumimnmod.MOD_ID,
                    player.getName().getString(), amount, reason, level, exp, LevelCurve.requiredExp(level));
        }
        return gained;
    }

    /** レベルを直接設定する（経験値は 0 に戻る）。コマンド用。 */
    public static void setLevel(ServerPlayer player, int level) {
        int oldLevel = get(player).level();
        int newLevel = LevelCurve.clampLevel(level);
        player.setData(ModAttachments.PLAYER_LEVEL, new PlayerLevel(newLevel, 0L));
        if (newLevel > oldLevel) {
            // コマンドで上げた場合も、ステータス上昇などが同じように処理されるようイベントは出す（演出はなし）
            NeoForge.EVENT_BUS.post(new PlayerLevelUpEvent(player, oldLevel, newLevel));
        }
    }

    /**
     * レベルアップしたときの処理。
     * バニラのレベルアップ音を鳴らし、チャットに通知する（HUD のエンブレムも自動で光る）。
     * ステータス上昇などは {@link PlayerLevelUpEvent} を受け取る側（ステータス設定）で実装する。
     */
    private static void onLevelUp(ServerPlayer player, int oldLevel, int newLevel) {
        NeoForge.EVENT_BUS.post(new PlayerLevelUpEvent(player, oldLevel, newLevel));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.75f, 1.0f);
        player.sendSystemMessage(Component.translatable("message.kurumimnmod.level_up", newLevel)
                .withStyle(ChatFormatting.GOLD));
    }
}
