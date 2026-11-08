package io.github.kurumimnm.kurumimnmod.client.quest;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.client.ModKeyMappings;
import io.github.kurumimnm.kurumimnmod.network.QuestActionPayload;
import io.github.kurumimnm.kurumimnmod.quest.PlayerQuest;
import io.github.kurumimnm.kurumimnmod.quest.QuestDefinition;
import io.github.kurumimnm.kurumimnmod.registry.ModWorldgen;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * クエスト画面（初期キー: J）。
 * <ul>
 *   <li>クエストを受けているとき：クエスト名（カーソルを合わせると説明）、フィールドの一覧（クリア済み・今いる所・入れる所・まだ先）、
 *       今の目標、［フィールドへ］［辞退］ボタン。辞退は 2 回押すと実行（受けた場所へ戻る）</li>
 *   <li>受けていないとき：クエストの一覧と［受注］ボタン（今は無条件ですぐ転移。受付の仕組みは後で作る）</li>
 * </ul>
 */
public class QuestScreen extends Screen {

    private static final String KEY = "screen." + Kurumimnmod.MOD_ID + ".quest";
    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 220;
    private static final int PADDING = 10;
    private static final int ROW = 12;
    private static final int COLOR_PANEL = 0xE0101010;
    private static final int COLOR_BORDER = 0xFF606060;
    private static final int COLOR_TITLE = 0xFFFFD94A;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_MUTED = 0xFF909090;
    private static final int COLOR_DONE = 0xFF7CFC7C;

    private int left;
    private int top;
    /** 辞退ボタンを 1 回押した（もう 1 回で実行）。 */
    private boolean confirmAbandon;

    public QuestScreen() {
        super(Component.translatable(KEY));
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_WIDTH) / 2;
        this.top = (this.height - PANEL_HEIGHT) / 2;
        PlayerQuest q = ClientQuest.state();
        if (q.quest().isPresent()) {
            initActive(q, q.quest().get());
        } else {
            initList(q);
        }
        this.addRenderableWidget(Button.builder(Component.translatable(KEY + ".close"), b -> this.onClose())
                .bounds(this.left + PANEL_WIDTH - PADDING - 60, this.top + PANEL_HEIGHT - 26, 60, 18).build());
    }

    /** 受けているクエストの画面。 */
    private void initActive(PlayerQuest q, Identifier id) {
        // クエスト名（カーソルで説明）
        Component name = Component.translatable(QuestDefinition.nameKey(id));
        StringWidget title = new StringWidget(this.font.width(name), ROW, name, this.font);
        title.setPosition(this.left + PADDING, this.top + PADDING + 12);
        title.setTooltip(Tooltip.create(Component.translatable(QuestDefinition.descriptionKey(id))));
        this.addRenderableWidget(title);

        int buttonY = this.top + PANEL_HEIGHT - 26;
        boolean inQuestWorld = this.minecraft != null && this.minecraft.player != null
                && this.minecraft.player.level().dimension().equals(ModWorldgen.QUEST_LEVEL);
        Button travel = Button.builder(Component.translatable(KEY + ".travel"), b -> {
            ClientPacketDistributor.sendToServer(new QuestActionPayload(QuestActionPayload.TRAVEL, ""));
            this.onClose();
        }).bounds(this.left + PADDING, buttonY, 80, 18).build();
        travel.active = !inQuestWorld;
        travel.setTooltip(Tooltip.create(Component.translatable(KEY + ".travel.tooltip")));
        this.addRenderableWidget(travel);

        Button abandon = Button.builder(Component.translatable(KEY + ".abandon"), b -> {
            if (!this.confirmAbandon) {
                this.confirmAbandon = true;
                b.setMessage(Component.translatable(KEY + ".abandon.confirm").withStyle(ChatFormatting.RED));
                return;
            }
            ClientPacketDistributor.sendToServer(new QuestActionPayload(QuestActionPayload.ABANDON, ""));
            this.onClose();
        }).bounds(this.left + PADDING + 84, buttonY, 92, 18).build();
        abandon.setTooltip(Tooltip.create(Component.translatable(KEY + ".abandon.tooltip")));
        this.addRenderableWidget(abandon);
    }

    /** クエストの一覧（受けていないとき）。 */
    private void initList(PlayerQuest q) {
        int y = this.top + PADDING + 26;
        for (Map.Entry<Identifier, QuestDefinition> e : ClientQuest.all()) {
            Identifier id = e.getKey();
            Component name = Component.translatable(QuestDefinition.nameKey(id));
            if (q.completed().contains(id)) {
                name = Component.translatable(KEY + ".completed_mark", name);
            }
            StringWidget label = new StringWidget(PANEL_WIDTH - PADDING * 2 - 66, ROW, name, this.font);
            label.setPosition(this.left + PADDING, y + 3);
            label.setTooltip(Tooltip.create(Component.translatable(QuestDefinition.descriptionKey(id))));
            this.addRenderableWidget(label);
            this.addRenderableWidget(Button.builder(Component.translatable(KEY + ".accept"), b -> {
                ClientPacketDistributor.sendToServer(new QuestActionPayload(QuestActionPayload.ACCEPT, id.toString()));
                this.onClose();
            }).bounds(this.left + PANEL_WIDTH - PADDING - 60, y, 60, 18).build());
            y += 22;
            if (y > this.top + PANEL_HEIGHT - 50) {
                break;
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(g, mouseX, mouseY, partialTicks);
        g.fill(this.left - 1, this.top - 1, this.left + PANEL_WIDTH + 1, this.top + PANEL_HEIGHT + 1, COLOR_BORDER);
        g.fill(this.left, this.top, this.left + PANEL_WIDTH, this.top + PANEL_HEIGHT, COLOR_PANEL);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(g, mouseX, mouseY, partialTicks);
        int x = this.left + PADDING;
        int y = this.top + PADDING;
        PlayerQuest q = ClientQuest.state();
        if (q.quest().isEmpty()) {
            g.text(this.font, Component.translatable(KEY + ".list"), x, y, COLOR_TITLE, true);
            if (ClientQuest.all().isEmpty()) {
                g.text(this.font, Component.translatable(KEY + ".list.empty"), x, y + 26, COLOR_MUTED, true);
            }
            return;
        }
        Identifier id = q.quest().get();
        g.text(this.font, Component.translatable(KEY + ".current"), x, y, COLOR_MUTED, true);
        Optional<QuestDefinition> def = ClientQuest.definition(id);
        if (def.isEmpty()) {
            return;
        }
        // 今の目標
        y += 30;
        g.text(this.font, Component.translatable("hud." + Kurumimnmod.MOD_ID + ".quest.goal", ClientQuest.objective(q, def.get())),
                x, y, ClientQuest.objectiveDone(q, def.get()) ? COLOR_DONE : COLOR_TEXT, true);
        // フィールドの一覧
        y += 16;
        g.fill(x, y - 4, this.left + PANEL_WIDTH - PADDING, y - 3, COLOR_BORDER);
        g.text(this.font, Component.translatable(KEY + ".fields"), x, y, COLOR_MUTED, true);
        y += ROW;
        List<String> order = def.get().fieldOrder();
        for (String fieldId : order) {
            if (y > this.top + PANEL_HEIGHT - 40) {
                break;
            }
            boolean cleared = q.cleared().contains(fieldId);
            boolean here = q.inField() && q.location().equals(fieldId);
            boolean open = def.get().available(fieldId, q.cleared());
            Component line;
            int color;
            if (cleared) {
                line = Component.translatable(KEY + ".field.cleared", ClientQuest.fieldTitle(id, def.get(), fieldId));
                color = COLOR_DONE;
            } else if (here) {
                line = Component.translatable(KEY + ".field.here", ClientQuest.fieldTitle(id, def.get(), fieldId));
                color = COLOR_TITLE;
            } else if (open) {
                line = Component.translatable(KEY + ".field.open", ClientQuest.fieldTitle(id, def.get(), fieldId));
                color = COLOR_TEXT;
            } else {
                // まだ先のフィールドは名前を伏せる
                int number = def.get().field(fieldId).map(QuestDefinition.Field::number).orElse(0);
                line = Component.translatable(KEY + ".field.unknown", number);
                color = COLOR_MUTED;
            }
            g.text(this.font, line, x + 4, y, color, true);
            y += ROW;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!ModKeyMappings.OPEN_QUEST.isUnbound() && ModKeyMappings.OPEN_QUEST.matches(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
