package io.github.kurumimnm.kurumimnmod.client.quest;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.network.ChooseGatePayload;
import io.github.kurumimnm.kurumimnmod.network.OpenGatePayload;
import io.github.kurumimnm.kurumimnmod.quest.PlayerQuest;
import io.github.kurumimnm.kurumimnmod.quest.QuestDefinition;
import io.github.kurumimnm.kurumimnmod.quest.QuestManager;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * ゲートの行き先を選ぶ画面（ゲートを右クリックしたときにサーバーから開かれる）。
 * 選択肢：次のフィールド（条件が足りないものは押せない）・中間拠点・クエスト達成（受けた場所へ帰る）。
 */
public class GateScreen extends Screen {

    private static final String KEY = "screen." + Kurumimnmod.MOD_ID + ".gate";
    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 4;

    private final String from;
    private final List<OpenGatePayload.Option> options;

    public GateScreen(String from, List<OpenGatePayload.Option> options) {
        super(Component.translatable(KEY));
        this.from = from;
        this.options = options;
    }

    @Override
    protected void init() {
        int count = this.options.size() + 1;
        int x = (this.width - BUTTON_WIDTH) / 2;
        int y = (this.height - count * (BUTTON_HEIGHT + GAP)) / 2 + 10;
        PlayerQuest q = ClientQuest.state();
        Optional<Identifier> questId = q.quest();
        Optional<QuestDefinition> def = questId.flatMap(ClientQuest::definition);
        for (OpenGatePayload.Option option : this.options) {
            Component label = switch (option.kind()) {
                case QuestManager.OPTION_HUB -> Component.translatable(KEY + ".hub");
                case QuestManager.OPTION_FINISH -> Component.translatable(KEY + ".finish");
                default -> questId.isPresent() && def.isPresent()
                        ? ClientQuest.fieldTitle(questId.get(), def.get(), option.field())
                        : Component.literal(option.field());
            };
            if (!option.available()) {
                label = Component.translatable(KEY + ".locked", label);
            }
            Button button = Button.builder(label, b -> {
                ClientPacketDistributor.sendToServer(new ChooseGatePayload(option.kind(), option.field()));
                this.onClose();
            }).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
            button.active = option.available();
            if (!option.available()) {
                button.setTooltip(Tooltip.create(Component.translatable(KEY + ".locked.tooltip")));
            }
            this.addRenderableWidget(button);
            y += BUTTON_HEIGHT + GAP;
        }
        this.addRenderableWidget(Button.builder(Component.translatable(KEY + ".stay"), b -> this.onClose())
                .bounds(x, y + 6, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(g, mouseX, mouseY, partialTicks);
        int count = this.options.size() + 1;
        int top = (this.height - count * (BUTTON_HEIGHT + GAP)) / 2 + 10;
        Component title = Component.translatable(this.from.isEmpty() ? KEY + ".title.hub" : KEY + ".title");
        g.centeredText(this.font, title, this.width / 2, top - 22, 0xFFFFD94A);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
