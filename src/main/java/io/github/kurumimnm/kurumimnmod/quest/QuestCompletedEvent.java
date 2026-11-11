package io.github.kurumimnm.kurumimnmod.quest;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * クエストを達成したときのイベント（{@code NeoForge.EVENT_BUS}、サーバー側）。
 * 報酬のお金を渡し、受けた場所へ戻した<b>あと</b>に出す。
 * <p>
 * クエストの仕組み（feature/world）とほかの仕組みをつなぐ口。例：{@link QuestDefinition#unlockJob()} が true なら、
 * 職業の仕組みがこれを受けて職業を解放し、選択画面を開く。
 */
public class QuestCompletedEvent extends Event {

    private final ServerPlayer player;
    private final Identifier questId;
    private final QuestDefinition definition;

    public QuestCompletedEvent(ServerPlayer player, Identifier questId, QuestDefinition definition) {
        this.player = player;
        this.questId = questId;
        this.definition = definition;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public Identifier getQuestId() {
        return questId;
    }

    public QuestDefinition getDefinition() {
        return definition;
    }
}
