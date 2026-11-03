package io.github.kurumimnm.kurumimnmod.client.scarecrow;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.entity.ScarecrowEntity;
import io.github.kurumimnm.kurumimnmod.network.DeleteScarecrowPayload;
import io.github.kurumimnm.kurumimnmod.network.UpdateScarecrowSettingsPayload;
import io.github.kurumimnm.kurumimnmod.scarecrow.ScarecrowSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * カカシの設定画面（設定用ステッキでカカシを左クリックすると開く）。
 * <pre>
 * ┌──────────── カカシの設定 ─────────────┐
 * │ 被弾                    │ 反撃                  │
 * │ 最大HP        [ 100 ]   │ 反撃する        [✓]   │
 * │ HP無限          [✓]     │ 攻撃力        [ 4 ]   │
 * │ 防御力        [   1 ]   │ 攻撃間隔(秒)  [ 2 ]   │
 * │ アーマー      [   0 ]   │ 射程(ブロック)[ 3 ]   │
 * │ 魔法耐性(%)   [   0 ]   │                       │
 * │ ノックバック耐性(%)[0]  │                       │
 * │ ノックバックなし  [ ]   │                       │
 * ├──────────────────────────────────────┤
 * │ [初期値に戻す] [削除※OP] [キャンセル] [保存]    │
 * └──────────────────────────────────────┘
 * </pre>
 * ・項目名にカーソルを合わせると説明（入力できる範囲）が出る
 * ・数値として読めない／範囲外の入力は赤字になり、その間は保存できない
 * ・削除は誤操作防止のため 2 回押し（1 回目で「本当に削除？」に変わる）
 * ・カカシが消えた（削除・描画距離の外へ出た）ら自動で閉じる
 */
public class ScarecrowSettingsScreen extends Screen {

    private static final String KEY = "screen." + Kurumimnmod.MOD_ID + ".scarecrow";

    // ---- レイアウト（GUI ピクセル） ----
    private static final int PANEL_WIDTH = 330;
    private static final int PANEL_HEIGHT = 236;
    private static final int PADDING = 10;
    private static final int COLUMN_GAP = 12;
    private static final int ROW_HEIGHT = 22;
    private static final int ROWS_TOP = 40;
    private static final int FIELD_WIDTH = 56;
    private static final int FIELD_HEIGHT = 16;

    // ---- 色（ARGB） ----
    private static final int COLOR_PANEL = 0xE0101010;
    private static final int COLOR_PANEL_BORDER = 0xFF606060;
    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_SECTION = 0xFFF2C230;
    private static final int COLOR_TEXT_OK = 0xFFE0E0E0;
    private static final int COLOR_TEXT_ERROR = 0xFFFF5555;

    private final int entityId;
    private final boolean canDelete;
    private ScarecrowSettings initial;

    private int left;
    private int top;

    // ---- 入力欄 ----
    private final List<NumberField> fields = new ArrayList<>();
    private NumberField maxHealth;
    private NumberField defense;
    private NumberField armor;
    private NumberField magicResistance;
    private NumberField knockbackResistance;
    private NumberField counterDamage;
    private NumberField counterInterval;
    private NumberField counterRange;
    private Checkbox infiniteHealth;
    private Checkbox counterEnabled;
    private Checkbox noKnockback;

    private Button saveButton;
    private Button deleteButton;
    /** 削除ボタンを 1 回押した状態（もう一度押すと削除）。 */
    private boolean deleteArmed;

    public ScarecrowSettingsScreen(int entityId, ScarecrowSettings settings, boolean canDelete) {
        super(Component.translatable(KEY));
        this.entityId = entityId;
        this.initial = settings;
        this.canDelete = canDelete;
    }

    /** 数値の入力欄（EditBox）と、その値の検査方法の組。 */
    private record NumberField(EditBox box, Predicate<Double> valid) {
        /** 入力が数値として読めて範囲内なら値、そうでなければ null。 */
        Double value() {
            try {
                double v = Double.parseDouble(box.getValue().trim());
                return Double.isFinite(v) && valid.test(v) ? v : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    // =====================================================================
    // 初期化（ウィジェットの配置）
    // =====================================================================

    @Override
    protected void init() {
        this.left = (this.width - PANEL_WIDTH) / 2;
        this.top = (this.height - PANEL_HEIGHT) / 2;
        this.fields.clear();
        this.deleteArmed = false;
        ScarecrowSettings s = this.initial;

        // ---- 左列：被弾 ----
        int col = leftColumnX();
        int row = 0;
        maxHealth = number(col, row++, "max_health", s.maxHealth(),
                ModConstants.SCARECROW_MIN_HEALTH, ModConstants.SCARECROW_MAX_HEALTH_LIMIT);
        infiniteHealth = checkbox(col, row++, "infinite_health", s.infiniteHealth());
        defense = number(col, row++, "defense", s.defense(), 0, ModConstants.SCARECROW_DEFENSE_LIMIT);
        armor = number(col, row++, "armor", s.armor(), 0, ModConstants.SCARECROW_ARMOR_LIMIT);
        magicResistance = number(col, row++, "magic_resistance", s.magicResistance(), 0, 100);
        knockbackResistance = number(col, row++, "knockback_resistance", s.knockbackResistance(), 0, 100);
        noKnockback = checkbox(col, row, "no_knockback", s.noKnockback());

        // ---- 右列：反撃 ----
        col = rightColumnX();
        row = 0;
        counterEnabled = checkbox(col, row++, "counter_enabled", s.counterEnabled());
        counterDamage = number(col, row++, "counter_damage", s.counterDamage(), 0, ModConstants.SCARECROW_COUNTER_DAMAGE_LIMIT);
        counterInterval = number(col, row++, "counter_interval", s.counterInterval(),
                ModConstants.SCARECROW_COUNTER_INTERVAL_MIN, ModConstants.SCARECROW_COUNTER_INTERVAL_MAX);
        counterRange = number(col, row, "counter_range", s.counterRange(),
                ModConstants.SCARECROW_COUNTER_RANGE_MIN, ModConstants.SCARECROW_COUNTER_RANGE_MAX);

        // ---- 下のボタン ----
        int buttonY = this.top + PANEL_HEIGHT - PADDING - 20;
        int buttonWidth = (PANEL_WIDTH - PADDING * 2 - 3 * 4) / 4;
        int x = this.left + PADDING;
        addRenderableWidget(Button.builder(Component.translatable(KEY + ".reset"), b -> resetToDefaults())
                .bounds(x, buttonY, buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable(KEY + ".reset.desc")))
                .build());
        x += buttonWidth + 4;
        this.deleteButton = Button.builder(Component.translatable(KEY + ".delete"), b -> onDeletePressed())
                .bounds(x, buttonY, buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable(KEY + ".delete.desc")))
                .build();
        // 削除は OP だけ（それ以外のプレイヤーにはボタン自体を出さない）
        this.deleteButton.visible = this.canDelete;
        addRenderableWidget(this.deleteButton);
        x += buttonWidth + 4;
        addRenderableWidget(Button.builder(Component.translatable(KEY + ".cancel"), b -> onClose())
                .bounds(x, buttonY, buttonWidth, 20).build());
        x += buttonWidth + 4;
        this.saveButton = addRenderableWidget(Button.builder(Component.translatable(KEY + ".save"), b -> save())
                .bounds(x, buttonY, buttonWidth, 20).build());
    }

    private int columnWidth() {
        return (PANEL_WIDTH - PADDING * 2 - COLUMN_GAP) / 2;
    }

    private int leftColumnX() {
        return this.left + PADDING;
    }

    private int rightColumnX() {
        return leftColumnX() + columnWidth() + COLUMN_GAP;
    }

    private int rowY(int row) {
        return this.top + ROWS_TOP + row * ROW_HEIGHT;
    }

    /** 項目名（カーソルを合わせると説明）を置く。 */
    private void label(int columnX, int row, String id) {
        Component name = Component.translatable(KEY + "." + id);
        StringWidget widget = new StringWidget(this.font.width(name), FIELD_HEIGHT, name, this.font);
        widget.setPosition(columnX, rowY(row));
        widget.setTooltip(Tooltip.create(Component.translatable(KEY + "." + id).withStyle(ChatFormatting.YELLOW)
                .append("\n")
                .append(Component.translatable(KEY + "." + id + ".desc").withStyle(ChatFormatting.GRAY))));
        addRenderableWidget(widget);
    }

    /** 数値の入力欄を 1 行追加する（min〜max の範囲だけ有効）。 */
    private NumberField number(int columnX, int row, String id, double value, double min, double max) {
        label(columnX, row, id);
        EditBox box = new EditBox(this.font, columnX + columnWidth() - FIELD_WIDTH, rowY(row), FIELD_WIDTH, FIELD_HEIGHT,
                Component.translatable(KEY + "." + id));
        box.setMaxLength(12);
        box.setValue(formatNumber(value));
        NumberField field = new NumberField(box, v -> v >= min && v <= max);
        this.fields.add(field);
        addRenderableWidget(box);
        return field;
    }

    /** ON/OFF のチェックボックスを 1 行追加する。 */
    private Checkbox checkbox(int columnX, int row, String id, boolean selected) {
        label(columnX, row, id);
        int size = Checkbox.getBoxSize(this.font);
        Checkbox box = Checkbox.builder(Component.empty(), this.font)
                .pos(columnX + columnWidth() - size, rowY(row) + (FIELD_HEIGHT - size) / 2)
                .selected(selected)
                .build();
        return addRenderableWidget(box);
    }

    /** 整数ならそのまま、小数なら最大 2 桁で表示する（100.0 → "100"、2.5 → "2.5"）。 */
    private static String formatNumber(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        String s = String.format(java.util.Locale.ROOT, "%.2f", value);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    // =====================================================================
    // ボタンの処理
    // =====================================================================

    /** 入力欄を Config の初期値（[scarecrow]）に戻す（まだ保存はしない）。 */
    private void resetToDefaults() {
        this.initial = ScarecrowSettings.fromConfig();
        rebuildWidgets();
    }

    /** 入力内容を 1 つの設定にまとめる。読めない値が 1 つでもあれば null。 */
    private ScarecrowSettings collect() {
        for (NumberField field : this.fields) {
            if (field.value() == null) {
                return null;
            }
        }
        return new ScarecrowSettings(
                maxHealth.value().floatValue(),
                infiniteHealth.selected(),
                (int) Math.round(defense.value()),
                armor.value().floatValue(),
                magicResistance.value().floatValue(),
                knockbackResistance.value().floatValue(),
                noKnockback.selected(),
                counterEnabled.selected(),
                counterDamage.value().floatValue(),
                counterInterval.value().floatValue(),
                counterRange.value().floatValue());
    }

    private void save() {
        ScarecrowSettings settings = collect();
        if (settings == null) {
            return;
        }
        ClientPacketDistributor.sendToServer(new UpdateScarecrowSettingsPayload(this.entityId, settings.sanitized()));
        onClose();
    }

    private void onDeletePressed() {
        if (!this.deleteArmed) {
            this.deleteArmed = true;
            this.deleteButton.setMessage(Component.translatable(KEY + ".delete.confirm").withStyle(ChatFormatting.RED));
            return;
        }
        ClientPacketDistributor.sendToServer(new DeleteScarecrowPayload(this.entityId));
        onClose();
    }

    // =====================================================================
    // 毎 tick・描画
    // =====================================================================

    /** カカシが消えたら閉じる（距離は問わない）。 */
    @Override
    public void tick() {
        super.tick();
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        Entity entity = this.minecraft.level.getEntity(this.entityId);
        if (!(entity instanceof ScarecrowEntity) || !entity.isAlive()) {
            onClose();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(g, mouseX, mouseY, partialTicks);
        g.fill(this.left - 1, this.top - 1, this.left + PANEL_WIDTH + 1, this.top + PANEL_HEIGHT + 1, COLOR_PANEL_BORDER);
        g.fill(this.left, this.top, this.left + PANEL_WIDTH, this.top + PANEL_HEIGHT, COLOR_PANEL);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        // ---- 入力チェック：不正な欄は赤字、1 つでもあれば保存できない ----
        boolean allValid = true;
        for (NumberField field : this.fields) {
            boolean ok = field.value() != null;
            field.box().setTextColor(ok ? COLOR_TEXT_OK : COLOR_TEXT_ERROR);
            allValid &= ok;
        }
        this.saveButton.active = allValid;
        // ノックバックなしの間は、ノックバック耐性の値は使われないので編集できないようにする
        boolean kbEditable = !this.noKnockback.selected();
        this.knockbackResistance.box().setEditable(kbEditable);
        this.knockbackResistance.box().active = kbEditable;

        super.extractRenderState(g, mouseX, mouseY, partialTicks);

        // ---- タイトル ----
        int titleWidth = this.font.width(this.title);
        g.text(this.font, this.title, this.left + (PANEL_WIDTH - titleWidth) / 2, this.top + 8, COLOR_TITLE, true);
        // ---- 見出し ----
        g.text(this.font, Component.translatable(KEY + ".section.defense"), leftColumnX(), this.top + 24, COLOR_SECTION, true);
        g.text(this.font, Component.translatable(KEY + ".section.counter"), rightColumnX(), this.top + 24, COLOR_SECTION, true);
        // ---- 区切り線（列の間・ボタンの上） ----
        int dividerX = leftColumnX() + columnWidth() + COLUMN_GAP / 2;
        g.fill(dividerX, this.top + 22, dividerX + 1, this.top + PANEL_HEIGHT - PADDING - 28, COLOR_PANEL_BORDER);
        int footerY = this.top + PANEL_HEIGHT - PADDING - 26;
        g.fill(this.left + PADDING, footerY, this.left + PANEL_WIDTH - PADDING, footerY + 1, COLOR_PANEL_BORDER);
    }

    /** シングルプレイでもゲームを止めない（設定しながらカカシの様子を見られるように）。 */
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
