package io.github.kurumimnm.kurumimnmod.client.screen;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.client.status.ClientStatus;
import io.github.kurumimnm.kurumimnmod.network.AllocateStatsPayload;
import io.github.kurumimnm.kurumimnmod.status.AllocType;
import io.github.kurumimnm.kurumimnmod.status.StatManager;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * ステータス付与画面（2 ページ目・初期キー: K）。
 * <pre>
 * ┌[ステータス][ステータス付与]┐
 * │ 振り分け可能ポイント: 3      │
 * ├────────────────────────────┤
 * │ 体力        2 (+1)  [-][+] │ ← 名前にカーソルを合わせると効果の説明
 * │ 魔力 / 攻撃力 / 防御力 / 知力
 * ├────────────────────────────┤
 * │        [リセット] [決定]     │
 * └────────────────────────────┘
 * </pre>
 * [+][-] は「仮振り」で、[決定] を押したときに初めてサーバーへ送って確定する（誤操作防止）。
 * [リセット] は仮振りの取り消し（確定済みのポイントは戻らない）。
 */
public class StatAllocationScreen extends StatusPageScreen {

    private static final int ROW_HEIGHT = 22;
    private static final String KEY = "screen." + Kurumimnmod.MOD_ID + ".allocation";

    /** 仮振り中のポイント（決定前）。 */
    private final Map<AllocType, Integer> pending = new EnumMap<>(AllocType.class);
    private final Map<AllocType, Button> minusButtons = new EnumMap<>(AllocType.class);
    private final Map<AllocType, Button> plusButtons = new EnumMap<>(AllocType.class);
    private Button resetButton;
    private Button confirmButton;

    public StatAllocationScreen() {
        super(Page.ALLOCATION, Component.translatable(KEY));
    }

    private int rowsTop() {
        return this.top + CONTENT_TOP + 20;
    }

    private int footerTop() {
        return rowsTop() + AllocType.values().length * ROW_HEIGHT + 6;
    }

    @Override
    protected void initPage() {
        this.minusButtons.clear();
        this.plusButtons.clear();

        int y = rowsTop();
        for (AllocType type : AllocType.values()) {
            // 名前（ツールチップ付き）
            Component name = Component.translatable(type.nameKey());
            StringWidget label = new StringWidget(this.font.width(name), 18, name, this.font);
            label.setPosition(contentLeft(), y);
            label.setTooltip(Tooltip.create(Component.translatable(type.nameKey()).withStyle(ChatFormatting.YELLOW)
                    .append("\n")
                    .append(Component.translatable(type.descriptionKey(), StatManager.perPoint(type)).withStyle(ChatFormatting.GRAY))));
            this.addRenderableWidget(label);

            // [-] [+]
            Button minus = Button.builder(Component.literal("-"), b -> changePending(type, -1))
                    .bounds(contentRight() - 42, y, 20, 18).build();
            Button plus = Button.builder(Component.literal("+"), b -> changePending(type, 1))
                    .bounds(contentRight() - 20, y, 20, 18).build();
            this.minusButtons.put(type, this.addRenderableWidget(minus));
            this.plusButtons.put(type, this.addRenderableWidget(plus));
            y += ROW_HEIGHT;
        }

        // [リセット] [決定]
        int buttonWidth = 70;
        int footerY = footerTop() + 6;
        int center = this.left + PANEL_WIDTH / 2;
        this.resetButton = this.addRenderableWidget(Button.builder(Component.translatable(KEY + ".reset"), b -> this.pending.clear())
                .bounds(center - buttonWidth - 4, footerY, buttonWidth, 20).build());
        this.confirmButton = this.addRenderableWidget(Button.builder(Component.translatable(KEY + ".confirm"), b -> confirm())
                .bounds(center + 4, footerY, buttonWidth, 20).build());
    }

    // ---- 仮振り ----

    private int pendingTotal() {
        int sum = 0;
        for (int v : this.pending.values()) {
            sum += v;
        }
        return sum;
    }

    /** 振り分け可能ポイント（所持ポイント − 仮振り中）。 */
    private int remaining() {
        return ClientStatus.unspentPoints() - pendingTotal();
    }

    private void changePending(AllocType type, int delta) {
        int current = this.pending.getOrDefault(type, 0);
        int next = current + delta;
        if (next < 0 || (delta > 0 && remaining() <= 0)) {
            return;
        }
        if (next == 0) {
            this.pending.remove(type);
        } else {
            this.pending.put(type, next);
        }
    }

    /** 仮振りをサーバーへ送って確定する。 */
    private void confirm() {
        if (this.pending.isEmpty() || remaining() < 0) {
            return;
        }
        ClientPacketDistributor.sendToServer(new AllocateStatsPayload(Map.copyOf(this.pending)));
        this.pending.clear();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        // サーバー側でポイントが変わった（リセットコマンド等）ときに仮振りが所持数を超えないようにする
        if (remaining() < 0) {
            this.pending.clear();
        }

        // ---- ボタンの有効／無効 ----
        boolean canAdd = remaining() > 0;
        for (AllocType type : AllocType.values()) {
            this.plusButtons.get(type).active = canAdd;
            this.minusButtons.get(type).active = this.pending.getOrDefault(type, 0) > 0;
        }
        this.resetButton.active = !this.pending.isEmpty();
        this.confirmButton.active = !this.pending.isEmpty();

        super.extractRenderState(g, mouseX, mouseY, partialTicks);

        int x = contentLeft();

        // ---- 振り分け可能ポイント ----
        g.text(this.font, Component.translatable(KEY + ".points", remaining()), x, this.top + CONTENT_TOP, COLOR_POINTS, true);
        separator(g, rowsTop() - 6);

        // ---- 各項目：振り分け済み (+仮振り) ----
        int y = rowsTop();
        int valueRight = contentRight() - 48;
        for (AllocType type : AllocType.values()) {
            int textY = y + 5;
            int pend = this.pending.getOrDefault(type, 0);
            String pendText = pend > 0 ? " (+" + pend + ")" : "";
            String value = String.valueOf(ClientStatus.allocated(type));
            int pendWidth = this.font.width(pendText);
            g.text(this.font, value, valueRight - pendWidth - this.font.width(value), textY, COLOR_VALUE, true);
            if (pend > 0) {
                g.text(this.font, pendText, valueRight - pendWidth, textY, COLOR_PENDING, true);
            }
            y += ROW_HEIGHT;
        }

        separator(g, footerTop());
    }
}
