package io.github.kurumimnm.kurumimnmod.level;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * プレイヤーのレベルが上がったときに発火するイベント（サーバー側・NeoForge.EVENT_BUS）。
 * <p>
 * レベルアップによるステータス上昇は、ステータス設定側でこのイベントを受け取って実装する。
 * <pre>{@code
 * @SubscribeEvent
 * static void onLevelUp(PlayerLevelUpEvent event) {
 *     int gained = event.getNewLevel() - event.getOldLevel(); // 一度に複数レベル上がることもある
 *     ...
 * }
 * }</pre>
 * ※ レベルが下がった場合（コマンドで下げた等）は発火しない。
 */
public class PlayerLevelUpEvent extends Event {

    private final ServerPlayer player;
    private final int oldLevel;
    private final int newLevel;

    public PlayerLevelUpEvent(ServerPlayer player, int oldLevel, int newLevel) {
        this.player = player;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    /** 上がる前のレベル。 */
    public int getOldLevel() {
        return oldLevel;
    }

    /** 上がった後のレベル。 */
    public int getNewLevel() {
        return newLevel;
    }
}
