package io.github.kurumimnm.kurumimnmod.client.screen;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.client.ModKeyMappings;
import io.github.kurumimnm.kurumimnmod.client.status.ClientStatus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * ステータス系画面の共通部分（パネル背景・上部のページ切り替えタブ・ショートカットキー）。
 * <pre>
 * ┌[ ステータス ][ ステータス付与 (3) ]┐ ← タブ（今のページは押せない）
 * │                                    │
 * │        各ページの中身               │
 * └────────────────────────────────────┘
 * </pre>
 * ・開いているページのキー（Tab / K）をもう一度押すと閉じる
 * ・もう一方のキーを押すとそのページへ切り替える
 */
public abstract class StatusPageScreen extends Screen {

    /** ページの種類。 */
    public enum Page {
        STATUS, ALLOCATION
    }

    // ---- レイアウト（GUI ピクセル） ----
    protected static final int PANEL_WIDTH = 240;
    protected static final int PANEL_HEIGHT = 214;
    protected static final int PADDING = 10;
    protected static final int TAB_HEIGHT = 18;
    /** タブの下、中身の開始位置（パネル上端から）。 */
    protected static final int CONTENT_TOP = 30;

    // ---- 色（ARGB） ----
    protected static final int COLOR_PANEL = 0xE0101010;
    protected static final int COLOR_PANEL_BORDER = 0xFF606060;
    protected static final int COLOR_HEADER = 0xFFFFFFFF;
    protected static final int COLOR_LABEL = 0xFFC0C0C0;
    protected static final int COLOR_VALUE = 0xFFFFFFFF;
    protected static final int COLOR_POINTS = 0xFFF2C230;
    protected static final int COLOR_PENDING = 0xFF6CE06C;
    protected static final int COLOR_MUTED = 0xFF808080;

    private final Page page;
    protected int left;
    protected int top;

    protected StatusPageScreen(Page page, Component title) {
        super(title);
        this.page = page;
    }

    /** ページを開く（キー操作・タブから呼ぶ）。 */
    public static Screen create(Page page) {
        return page == Page.STATUS ? new StatusScreen() : new StatAllocationScreen();
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_WIDTH) / 2;
        this.top = (this.height - PANEL_HEIGHT) / 2;

        // ---- ページ切り替えタブ ----
        int tabWidth = (PANEL_WIDTH - PADDING * 2 - 4) / 2;
        Button statusTab = Button.builder(Component.translatable("screen." + Kurumimnmod.MOD_ID + ".status"),
                        b -> switchTo(Page.STATUS))
                .bounds(this.left + PADDING, this.top + 6, tabWidth, TAB_HEIGHT)
                .build();
        int unspent = ClientStatus.unspentPoints();
        Component allocLabel = unspent > 0
                ? Component.translatable("screen." + Kurumimnmod.MOD_ID + ".allocation.tab_points", unspent)
                : Component.translatable("screen." + Kurumimnmod.MOD_ID + ".allocation");
        Button allocTab = Button.builder(allocLabel, b -> switchTo(Page.ALLOCATION))
                .bounds(this.left + PADDING + tabWidth + 4, this.top + 6, tabWidth, TAB_HEIGHT)
                .build();
        statusTab.active = this.page != Page.STATUS;
        allocTab.active = this.page != Page.ALLOCATION;
        this.addRenderableWidget(statusTab);
        this.addRenderableWidget(allocTab);

        initPage();
    }

    /** 各ページのウィジェット（ボタン等）を追加する。 */
    protected abstract void initPage();

    private void switchTo(Page target) {
        if (target != this.page && this.minecraft != null) {
            this.minecraft.gui.setScreen(create(target));
        }
    }

    /** ショートカットキーでページ切り替え・閉じる。 */
    @Override
    public boolean keyPressed(KeyEvent event) {
        Page pressed = pageForKey(event);
        if (pressed != null) {
            if (pressed == this.page) {
                this.onClose();
            } else {
                switchTo(pressed);
            }
            return true;
        }
        return super.keyPressed(event);
    }

    private static Page pageForKey(KeyEvent event) {
        if (matches(ModKeyMappings.OPEN_STATUS, event)) {
            return Page.STATUS;
        }
        if (matches(ModKeyMappings.OPEN_ALLOCATION, event)) {
            return Page.ALLOCATION;
        }
        return null;
    }

    private static boolean matches(KeyMapping mapping, KeyEvent event) {
        return !mapping.isUnbound() && mapping.matches(event);
    }

    /** 背景（ぼかし）の上、ボタンの下にパネルを描く。 */
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(g, mouseX, mouseY, partialTicks);
        g.fill(this.left - 1, this.top - 1, this.left + PANEL_WIDTH + 1, this.top + PANEL_HEIGHT + 1, COLOR_PANEL_BORDER);
        g.fill(this.left, this.top, this.left + PANEL_WIDTH, this.top + PANEL_HEIGHT, COLOR_PANEL);
    }

    /** 横の区切り線を描く。 */
    protected void separator(GuiGraphicsExtractor g, int y) {
        g.fill(this.left + PADDING, y, this.left + PANEL_WIDTH - PADDING, y + 1, COLOR_PANEL_BORDER);
    }

    protected int contentLeft() {
        return this.left + PADDING;
    }

    protected int contentRight() {
        return this.left + PANEL_WIDTH - PADDING;
    }

    /** シングルプレイでもゲームを止めない（インベントリと同じ挙動）。 */
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
