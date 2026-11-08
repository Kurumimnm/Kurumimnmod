package io.github.kurumimnm.kurumimnmod.client.quest;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.network.OpenGatePayload;
import io.github.kurumimnm.kurumimnmod.quest.PlayerQuest;
import io.github.kurumimnm.kurumimnmod.quest.QuestDefinition;
import io.github.kurumimnm.kurumimnmod.registry.ModAttachments;
import io.github.kurumimnm.kurumimnmod.registry.ModWorldgen;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/**
 * クエストのクライアント側の共通処理（クライアント専用）。
 * クエストの一覧はサーバーから同期されたレジストリ、自分の進み具合はデータアタッチメント（{@link PlayerQuest}）から読む。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID, value = Dist.CLIENT)
public final class ClientQuest {

    private ClientQuest() {}

    private static final String KEY = "hud." + Kurumimnmod.MOD_ID + ".quest.";

    @SubscribeEvent
    static void onRegisterClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(OpenGatePayload.TYPE, (payload, context) ->
                Minecraft.getInstance().gui.setScreen(new GateScreen(payload.from(), payload.options())));
    }

    /** 自分のクエストの状態。 */
    public static PlayerQuest state() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? PlayerQuest.EMPTY : mc.player.getData(ModAttachments.QUEST);
    }

    public static Optional<Registry<QuestDefinition>> registry() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection == null ? Optional.empty() : connection.registryAccess().lookup(ModWorldgen.QUEST);
    }

    public static Optional<QuestDefinition> definition(Identifier id) {
        return registry().flatMap(r -> r.getOptional(id));
    }

    /** クエストの一覧（並び順 sort → ID）。 */
    public static List<Map.Entry<Identifier, QuestDefinition>> all() {
        List<Map.Entry<Identifier, QuestDefinition>> list = new ArrayList<>();
        registry().ifPresent(r -> r.keySet().forEach(id -> list.add(Map.entry(id, r.getValue(id)))));
        list.sort(Comparator.comparingInt((Map.Entry<Identifier, QuestDefinition> e) -> e.getValue().sort())
                .thenComparing(e -> e.getKey().toString()));
        return list;
    }

    /** 「フィールド 1：草原の小道」。 */
    public static Component fieldTitle(Identifier quest, QuestDefinition def, String fieldId) {
        int number = def.field(fieldId).map(QuestDefinition.Field::number).orElse(0);
        return Component.translatable(KEY + "field", number, Component.translatable(QuestDefinition.fieldKey(quest, fieldId)));
    }

    /** 今の目標（HUD・クエスト画面の 1 行）。 */
    public static Component objective(PlayerQuest q, QuestDefinition def) {
        if (q.cleared().contains(def.goal())) {
            return Component.translatable(KEY + "goal.done");
        }
        if (q.inHub()) {
            return Component.translatable(KEY + "goal.hub");
        }
        if (!q.inField()) {
            return Component.translatable(KEY + "goal.travel");
        }
        if (q.cleared().contains(q.location())) {
            return Component.translatable(KEY + "goal.cleared");
        }
        QuestDefinition.Clear clear = def.field(q.location()).map(QuestDefinition.Field::clear).orElse(QuestDefinition.Clear.REACH_GATE);
        return switch (clear.type()) {
            case QuestDefinition.Clear.KILL -> Component.translatable(KEY + "goal.kill", q.kills(), clear.count());
            case QuestDefinition.Clear.BOSS -> Component.translatable(KEY + "goal.boss");
            case QuestDefinition.Clear.WIPE -> Component.translatable(KEY + "goal.wipe", q.kills(),
                    def.field(q.location()).map(QuestDefinition.Field::placedTotal).orElse(0));
            default -> Component.translatable(KEY + "goal.reach");
        };
    }

    /** 今の目標を達成しているか（色分け用）。 */
    public static boolean objectiveDone(PlayerQuest q, QuestDefinition def) {
        return q.cleared().contains(def.goal()) || (q.inField() && q.cleared().contains(q.location()));
    }
}
