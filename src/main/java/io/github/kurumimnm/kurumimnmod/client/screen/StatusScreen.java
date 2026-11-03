package io.github.kurumimnm.kurumimnmod.client.screen;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.client.status.ClientStatusPlaceholder;
import io.github.kurumimnm.kurumimnmod.status.StatType;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * ステータス画面（K キーで開く）。
 * <pre>
 * ┌──────────── ステータス ────────────┐
 * │ レベル 3        EXP 45 / 100 (45%)  │
 * │ HP 20 / 20      MP 20 / 20          │
 * │ 残りポイント: 2                     │
 * │ STR   0   [+]                       │
 * │ VIT   0   [+]   ...                 │
 * └─────────────────────────────────────┘
 * </pre>
 * 【仮置き】値は {@link ClientStatusPlaceholder} から取得している。
 * ステータスを振っても現時点では効果はなく、保存もされない。
 */
public class StatusScreen extends Screen {

    // ---- レイアウト（GUI ピクセル） ----
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 176;
    private static final int ROW_HEIGHT = 20;
    private static final int PADDING = 10;

    private static final int COLOR_PANEL = 0xE0101010;
    private static final int COLOR_PANEL_BORDER = 0xFF606060;
    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_LABEL = 0xFFC0C0C0;
    private static final int COLOR_VALUE = 0xFFFFFFFF;
    private static final int COLOR_POINTS = 0xFFF2C230;

    private final Map<StatType, Button> plusButtons = new EnumMap<>(StatType.class);

    private int left;
    private int top;

    public StatusScreen() {
        super(Component.translatable("screen." + Kurumimnmod.MOD_ID + ".status"));
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_WIDTH) / 2;
        this.top = (this.height - PANEL_HEIGHT) / 2;
        this.plusButtons.clear();

        // ステータスごとの「+」ボタン
        int y = statsTop();
        for (StatType type : StatType.values()) {
            Button button = Button.builder(Component.literal("+"), b -> ClientStatusPlaceholder.allocate(type))
                    .bounds(this.left + PANEL_WIDTH - PADDING - 20, y, 20, 18)
                    .tooltip(Tooltip.create(Component.translatable(type.descriptionKey())))
                    .build();
            this.plusButtons.put(type, this.addRenderableWidget(button));
            y += ROW_HEIGHT;
        }
    }

    /** ステータス一覧の開始 Y 座標。 */
    private int statsTop() {
        return this.top + 62;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        // ポイントが無いときは「+」を押せないようにする
        boolean canAllocate = ClientStatusPlaceholder.unspentPoints() > 0;
        this.plusButtons.values().forEach(b -> b.active = canAllocate);

        // super で「背景（extractBackground：ぼかし＋パネル）→ ボタン」が描かれ、文字はその上に描く
        super.extractRenderState(g, mouseX, mouseY, partialTicks);

        int x = this.left + PADDING;
        int right = this.left + PANEL_WIDTH - PADDING;

        // タイトル
        g.centeredText(this.font, this.title, this.width / 2, this.top + 8, COLOR_TITLE);

        // レベル・経験値
        int y = this.top + 24;
        g.text(this.font, Component.translatable("screen.kurumimnmod.status.level", ClientStatusPlaceholder.level()), x, y, COLOR_VALUE, true);
        Component progress = Component.translatable("screen.kurumimnmod.status.exp",
                ClientStatusPlaceholder.exp(), ClientStatusPlaceholder.requiredExp(),
                Mth.floor(ClientStatusPlaceholder.levelProgress() * 100));
        g.text(this.font, progress, right - this.font.width(progress), y, COLOR_LABEL, true);

        // HP・MP
        y += 12;
        g.text(this.font, Component.translatable("screen.kurumimnmod.status.hp",
                Mth.ceil(ClientStatusPlaceholder.health()), Mth.ceil(ClientStatusPlaceholder.maxHealth())), x, y, COLOR_VALUE, true);
        Component mp = Component.translatable("screen.kurumimnmod.status.mp",
                Mth.ceil(ClientStatusPlaceholder.mana()), Mth.ceil(ClientStatusPlaceholder.maxMana()));
        g.text(this.font, mp, right - this.font.width(mp), y, COLOR_VALUE, true);

        // 残りポイント
        y += 12;
        g.text(this.font, Component.translatable("screen.kurumimnmod.status.points", ClientStatusPlaceholder.unspentPoints()),
                x, y, COLOR_POINTS, true);

        // 各ステータス
        y = statsTop();
        for (StatType type : StatType.values()) {
            int textY = y + 5;
            g.text(this.font, Component.translatable(type.nameKey()), x, textY, COLOR_LABEL, true);
            String value = String.valueOf(ClientStatusPlaceholder.stat(type));
            g.text(this.font, value, right - 28 - this.font.width(value), textY, COLOR_VALUE, true);
            y += ROW_HEIGHT;
        }
    }

    /** 背景（ぼかし）の上、ボタンの下にパネルを描く。 */
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(g, mouseX, mouseY, partialTicks);
        g.fill(this.left - 1, this.top - 1, this.left + PANEL_WIDTH + 1, this.top + PANEL_HEIGHT + 1, COLOR_PANEL_BORDER);
        g.fill(this.left, this.top, this.left + PANEL_WIDTH, this.top + PANEL_HEIGHT, COLOR_PANEL);
        // 区切り線
        g.fill(this.left + PADDING, statsTop() - 6, this.left + PANEL_WIDTH - PADDING, statsTop() - 5, COLOR_PANEL_BORDER);
    }

    /** シングルプレイでもゲームを止めない（インベントリと同じ挙動）。 */
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
