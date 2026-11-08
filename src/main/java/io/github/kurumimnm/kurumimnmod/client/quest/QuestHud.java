package io.github.kurumimnm.kurumimnmod.client.quest;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.quest.PlayerQuest;
import io.github.kurumimnm.kurumimnmod.quest.QuestDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * 画面左上のクエスト表示（クライアント専用）。クエストを受けているときだけ出す。
 * <pre>
 * はじまりの試練            ← クエスト名
 * フィールド 1：草原の小道   ← いる場所（中間拠点なら「中間拠点」）
 * 目標：モンスターを倒す 3 / 8 ← クリア条件（クリアしたら緑で「クリア！ ゲートが開いた」）
 * </pre>
 * 位置・色は {@link ModConstants} の QUEST_HUD_* / QUEST_COLOR_*。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID, value = Dist.CLIENT)
public final class QuestHud {

    private QuestHud() {}

    private static final String KEY = "hud." + Kurumimnmod.MOD_ID + ".quest.";

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "quest"), QuestHud::render);
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        PlayerQuest q = ClientQuest.state();
        if (q.quest().isEmpty()) {
            return;
        }
        Identifier id = q.quest().get();
        Optional<QuestDefinition> def = ClientQuest.definition(id);
        if (def.isEmpty()) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        lines.add(Component.translatable(QuestDefinition.nameKey(id)));
        colors.add(ModConstants.QUEST_COLOR_TITLE);
        if (q.inHub()) {
            lines.add(Component.translatable(KEY + "hub"));
            colors.add(ModConstants.QUEST_COLOR_FIELD);
        } else if (q.inField()) {
            lines.add(ClientQuest.fieldTitle(id, def.get(), q.location()));
            colors.add(ModConstants.QUEST_COLOR_FIELD);
        }
        lines.add(Component.translatable(KEY + "goal", ClientQuest.objective(q, def.get())));
        colors.add(ClientQuest.objectiveDone(q, def.get()) ? ModConstants.QUEST_COLOR_CLEARED : ModConstants.QUEST_COLOR_GOAL);

        Font font = mc.font;
        int x = ModConstants.QUEST_HUD_X;
        int y = ModConstants.QUEST_HUD_Y;
        int width = 0;
        for (Component line : lines) {
            width = Math.max(width, font.width(line));
        }
        g.fill(x - 2, y - 2, x + width + 2, y + lines.size() * 10, ModConstants.QUEST_COLOR_BACKGROUND);
        for (int i = 0; i < lines.size(); i++) {
            g.text(font, lines.get(i), x, y + i * 10, colors.get(i), true);
        }
    }
}
