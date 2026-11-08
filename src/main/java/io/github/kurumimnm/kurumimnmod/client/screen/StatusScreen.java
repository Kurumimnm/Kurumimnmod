package io.github.kurumimnm.kurumimnmod.client.screen;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.client.status.ClientStatus;
import io.github.kurumimnm.kurumimnmod.quest.Money;
import io.github.kurumimnm.kurumimnmod.status.StatType;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

/**
 * ステータス画面（1 ページ目・初期キー: Tab）。
 * <pre>
 * ┌[ステータス][ステータス付与]┐
 * │ レベル 3     EXP 45 / 100  │
 * │ 次のレベルまで 55   $ 120   │ ← 所持金
 * │ HP 40 / 40   MP 20 / 20    │
 * ├────────────────────────────┤
 * │ 攻撃力              3      │ ← 名前にカーソルを合わせると説明文
 * │ 防御力 / 知力 / 技量 / ラック / 抵抗力
 * ├────────────────────────────┤
 * │ 所持パッシブ                │
 * │  なし                       │
 * └────────────────────────────┘
 * </pre>
 */
public class StatusScreen extends StatusPageScreen {

    private static final int ROW_HEIGHT = 12;
    private static final String KEY = "screen." + Kurumimnmod.MOD_ID + ".status";

    /** ステータス名のラベル（ツールチップ付き）。 */
    private final Map<StatType, StringWidget> labels = new EnumMap<>(StatType.class);

    public StatusScreen() {
        super(Page.STATUS, Component.translatable(KEY));
    }

    // ---- 縦の配置 ----
    private int infoTop() {
        return this.top + CONTENT_TOP;
    }

    private int statsTop() {
        return infoTop() + 42;
    }

    private int passiveTop() {
        return statsTop() + visibleStatCount() * ROW_HEIGHT + 8;
    }

    private static int visibleStatCount() {
        int n = 0;
        for (StatType type : StatType.values()) {
            if (type.visible()) {
                n++;
            }
        }
        return n;
    }

    @Override
    protected void initPage() {
        this.labels.clear();
        int y = statsTop();
        for (StatType type : StatType.values()) {
            if (!type.visible()) {
                continue;
            }
            Component name = Component.translatable(type.nameKey());
            StringWidget label = new StringWidget(this.font.width(name), ROW_HEIGHT, name, this.font);
            label.setPosition(contentLeft(), y);
            this.labels.put(type, this.addRenderableWidget(label));
            y += ROW_HEIGHT;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        // ツールチップの数値（現在のクリ率など）を最新にする
        this.labels.forEach((type, label) -> label.setTooltip(Tooltip.create(tooltip(type))));

        super.extractRenderState(g, mouseX, mouseY, partialTicks);

        int x = contentLeft();
        int right = contentRight();

        // ---- レベル・経験値 ----
        int y = infoTop();
        g.text(this.font, Component.translatable(KEY + ".level", ClientStatus.level()), x, y, COLOR_HEADER, true);
        Component exp = Component.translatable(KEY + ".exp", ClientStatus.exp(), ClientStatus.requiredExp(),
                Mth.floor(ClientStatus.levelProgress() * 100));
        g.text(this.font, exp, right - this.font.width(exp), y, COLOR_LABEL, true);

        y += 12;
        long toNext = Math.max(0, ClientStatus.requiredExp() - ClientStatus.exp());
        g.text(this.font, Component.translatable(KEY + ".next", toNext), x, y, COLOR_LABEL, true);
        // 所持金（右側）
        long money = this.minecraft == null || this.minecraft.player == null ? 0 : Money.get(this.minecraft.player);
        Component moneyText = Component.translatable(KEY + ".money", money);
        g.text(this.font, moneyText, right - this.font.width(moneyText), y, COLOR_POINTS, true);

        // ---- HP・MP ----
        y += 12;
        g.text(this.font, Component.translatable(KEY + ".hp",
                Mth.ceil(ClientStatus.health()), Mth.ceil(ClientStatus.maxHealth())), x, y, COLOR_VALUE, true);
        Component mp = Component.translatable(KEY + ".mp",
                Mth.floor(ClientStatus.mana()), Mth.floor(ClientStatus.maxMana()));
        g.text(this.font, mp, right - this.font.width(mp), y, COLOR_VALUE, true);

        separator(g, statsTop() - 5);

        // ---- 各ステータスの値（名前は StringWidget が描画） ----
        y = statsTop();
        for (StatType type : StatType.values()) {
            if (!type.visible()) {
                continue;
            }
            String value = String.valueOf(Mth.floor(ClientStatus.stat(type)));
            g.text(this.font, value, right - this.font.width(value), y + 2, COLOR_VALUE, true);
            y += ROW_HEIGHT;
        }

        // ---- 所持パッシブ（パッシブの仕組みができたらここに一覧を出す） ----
        int py = passiveTop();
        separator(g, py - 5);
        g.text(this.font, Component.translatable(KEY + ".passives"), x, py, COLOR_HEADER, true);
        g.text(this.font, Component.translatable(KEY + ".passives.none"), x + 8, py + 12, COLOR_MUTED, true);
    }

    /** ステータスのツールチップ（説明文＋現在の効果）。 */
    private static Component tooltip(StatType type) {
        MutableComponent text = Component.translatable(type.nameKey()).withStyle(ChatFormatting.YELLOW)
                .append("\n")
                .append(Component.translatable(type.descriptionKey()).withStyle(ChatFormatting.GRAY));
        Component effect = switch (type) {
            case ATTACK -> Component.translatable(type.nameKey() + ".effect", format(ClientStatus.attackMultiplier()));
            case DEFENSE -> Component.translatable(type.nameKey() + ".effect", format(ClientStatus.defenseReduction()));
            case INTELLIGENCE -> Component.translatable(type.nameKey() + ".effect", format(ClientStatus.magicMultiplier()));
            case DEXTERITY -> Component.translatable(type.nameKey() + ".effect",
                    format(ClientStatus.critChance() * 100), format(ClientStatus.critMultiplier()));
            default -> null;
        };
        if (effect != null) {
            text.append("\n").append(effect.copy().withStyle(ChatFormatting.AQUA));
        }
        return text;
    }

    /** 小数を「1.2」「1.25」のように表示（不要な 0 は省く）。 */
    private static String format(double value) {
        String s = String.format(Locale.ROOT, "%.2f", value);
        return s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
    }
}
