package io.github.kurumimnm.kurumimnmod.quest;

import io.github.kurumimnm.kurumimnmod.registry.ModAttachments;
import net.minecraft.world.entity.player.Player;

/**
 * 所持金（G）。プレイヤーのデータアタッチメント {@link ModAttachments#MONEY} に保存し、本人に同期する。
 * 今はクエストの報酬でだけ増える（ショップは後で作る）。
 */
public final class Money {

    private Money() {}

    public static long get(Player player) {
        return player.getData(ModAttachments.MONEY);
    }

    public static void set(Player player, long amount) {
        player.setData(ModAttachments.MONEY, Math.max(0, amount));
    }

    public static void add(Player player, long amount) {
        set(player, get(player) + amount);
    }
}
