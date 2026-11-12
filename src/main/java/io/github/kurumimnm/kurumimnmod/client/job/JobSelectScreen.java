package io.github.kurumimnm.kurumimnmod.client.job;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.job.JobDefinition;
import io.github.kurumimnm.kurumimnmod.network.SelectJobPayload;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * 職業の選択画面（チュートリアルを達成して職業が解放されたとき・まだ選んでいない状態でログインしたとき、
 * 職業の分岐レベル（Lv50）に達したときに開く）。
 * <ol>
 *   <li>職業のボタンを押す（カーソルを合わせると説明）</li>
 *   <li>下の「〇〇に決める」を押すと決定（あとから変えられない）</li>
 * </ol>
 * 職業がまだ 1 つもないときは「準備中」と出す。「あとで選ぶ」で閉じても、次にログインしたときにまた開く。
 */
public class JobSelectScreen extends Screen {

    private static final String KEY = "screen." + Kurumimnmod.MOD_ID + ".job";
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 4;
    private static final int COLOR_TITLE = 0xFFFFD94A;
    private static final int COLOR_TEXT = 0xFFE0E0E0;
    private static final int COLOR_NOTE = 0xFFA0A0A0;

    /** 今選んでいる（決定前の）職業。 */
    private @Nullable Identifier chosen;
    private int top;

    public JobSelectScreen() {
        super(Component.translatable(KEY));
    }

    @Override
    protected void init() {
        List<Map.Entry<Identifier, JobDefinition>> jobs = ClientJob.choices();
        int x = (this.width - BUTTON_WIDTH) / 2;
        int rows = Math.max(jobs.size(), 2) + 2;
        this.top = (this.height - rows * (BUTTON_HEIGHT + GAP)) / 2 + 10;
        int y = this.top;
        for (Map.Entry<Identifier, JobDefinition> e : jobs) {
            Identifier id = e.getKey();
            Component name = Component.translatable(JobDefinition.nameKey(id));
            Component label = id.equals(this.chosen) ? Component.translatable(KEY + ".chosen", name) : name;
            Button button = Button.builder(label, b -> {
                this.chosen = id;
                this.rebuildWidgets();
            }).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build();
            button.setTooltip(Tooltip.create(Component.translatable(JobDefinition.descriptionKey(id))));
            this.addRenderableWidget(button);
            y += BUTTON_HEIGHT + GAP;
        }
        if (jobs.isEmpty()) {
            y += 2 * (BUTTON_HEIGHT + GAP);
        }
        y += 6;
        if (this.chosen != null) {
            Identifier id = this.chosen;
            Component name = Component.translatable(JobDefinition.nameKey(id));
            this.addRenderableWidget(Button.builder(Component.translatable(KEY + ".confirm", name), b -> {
                ClientPacketDistributor.sendToServer(new SelectJobPayload(id));
                this.onClose();
            }).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
            y += BUTTON_HEIGHT + GAP;
        }
        this.addRenderableWidget(Button.builder(Component.translatable(jobs.isEmpty() ? KEY + ".close" : KEY + ".later"),
                b -> this.onClose()).bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(g, mouseX, mouseY, partialTicks);
        int cx = this.width / 2;
        // 分岐（Lv50 など）のときは見出しを変える
        String head = ClientJob.isPromotion() ? KEY + ".promotion" : KEY;
        g.centeredText(this.font, Component.translatable(head + ".title"), cx, this.top - 34, COLOR_TITLE);
        g.centeredText(this.font, Component.translatable(head + ".note"), cx, this.top - 20, COLOR_NOTE);
        if (ClientJob.choices().isEmpty()) {
            g.centeredText(this.font, Component.translatable(KEY + ".empty.1"), cx, this.top + 8, COLOR_TEXT);
            g.centeredText(this.font, Component.translatable(KEY + ".empty.2"), cx, this.top + 22, COLOR_NOTE);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
