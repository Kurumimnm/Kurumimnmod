package io.github.kurumimnm.kurumimnmod.client.hud;

import io.github.kurumimnm.kurumimnmod.client.status.ClientStatusPlaceholder;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * 画面下部（ホットバーの上）に表示するプレイヤーのステータス HUD。
 * <pre>
 *            ( Lv )          ← レベルアイコン（灰色 → 経験値に応じて下から黄色）
 *   [■■■■■ HP 20/20 ■■■■■]  ← HP バー（緑）
 *   [■■■■■ MP 20/20 ■■■■■]  ← MP バー（青）
 *   [      ホットバー       ]
 * </pre>
 * バニラのハート・空腹ゲージ・経験値バーの代わりに表示する（登録は {@link ModHudLayers}）。
 */
public final class PlayerStatusHud {

    private PlayerStatusHud() {}

    /** HP 減少演出用：表示中の「残像」の割合。 */
    private static float hpTrail = 1f;
    private static long lastFrameNanos = 0L;

    /** 現在の HUD 全体の上端（GUI 座標の画面下端からの距離）。酸素ゲージ等の配置に使う。 */
    static int topFromBottom = 0;

    /** HUD を描画すべき状態か（F1 で非表示・クリエイティブ/スペクテイター中は描かない）。 */
    static boolean shouldRender(Minecraft mc) {
        return mc.player != null
                && mc.gameMode != null
                && mc.gameMode.canHurtPlayer()
                && !mc.gameRenderer.gameRenderState().guiRenderState.isHudHidden;
    }

    /** 乗り物のジャンプゲージを表示中か（表示中は HUD を少し上にずらす）。 */
    static boolean isJumpBarShown(Minecraft mc) {
        LocalPlayer player = mc.player;
        return player != null && player.jumpableVehicle() != null;
    }

    /** GUI レイヤーとして呼ばれる描画処理。 */
    public static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!shouldRender(mc)) {
            return;
        }
        Font font = mc.font;

        int screenW = g.guiWidth();
        int screenH = g.guiHeight();
        int barW = ModConstants.HUD_BAR_WIDTH;
        int barH = ModConstants.HUD_BAR_HEIGHT;
        int x = screenW / 2 - barW / 2;

        // ---- 縦の配置（下から：ホットバー → MP → HP → レベル） ----
        int bottom = 22 + ModConstants.HUD_HOTBAR_MARGIN + (isJumpBarShown(mc) ? ModConstants.HUD_JUMP_BAR_OFFSET : 0);
        int mpY = screenH - bottom - barH;
        int hpY = mpY - 1 - ModConstants.HUD_BAR_GAP - 1 - barH; // 枠線 1px ずつを考慮
        int iconSize = ModConstants.HUD_LEVEL_ICON_SIZE;
        int iconY = hpY - 1 - ModConstants.HUD_LEVEL_ICON_GAP - iconSize;

        // ---- HP ----
        float hp = ClientStatusPlaceholder.health();
        float maxHp = ClientStatusPlaceholder.maxHealth();
        float hpRatio = hp / maxHp;
        updateTrail(hpRatio);
        int hpColor = hpRatio <= ModConstants.HUD_HP_LOW_RATIO ? ModConstants.HUD_COLOR_HP_LOW : ModConstants.HUD_COLOR_HP;
        HudDraw.bar(g, x, hpY, barW, barH, hpRatio, hpColor, hpTrail, ModConstants.HUD_COLOR_HP_TRAIL);
        HudDraw.centeredLabel(g, font, format(hp) + " / " + format(maxHp), x, hpY, barW, barH);

        // ---- MP ----
        float mp = ClientStatusPlaceholder.mana();
        float maxMp = ClientStatusPlaceholder.maxMana();
        HudDraw.bar(g, x, mpY, barW, barH, mp / Math.max(1, maxMp), ModConstants.HUD_COLOR_MP, 0, 0);
        HudDraw.centeredLabel(g, font, format(mp) + " / " + format(maxMp), x, mpY, barW, barH);

        // ---- レベル（丸アイコン） ----
        int iconX = screenW / 2 - iconSize / 2;
        HudDraw.levelCircle(g, iconX, iconY, iconSize, ClientStatusPlaceholder.levelProgress(),
                ModConstants.HUD_COLOR_LEVEL_EMPTY, ModConstants.HUD_COLOR_LEVEL_FILL);
        String lv = String.valueOf(ClientStatusPlaceholder.level());
        g.text(font, lv, iconX + (iconSize - font.width(lv)) / 2 + 1, iconY + (iconSize - 7) / 2, ModConstants.HUD_COLOR_TEXT, true);

        // ---- 他の HUD（酸素ゲージや他 MOD の表示）を HUD の上に積むための高さ調整 ----
        // NeoForge の Hud#leftHeight / rightHeight は「次の行を画面下端から何 px の位置に描くか」
        int used = screenH - hpY + 2;
        topFromBottom = used;
        var hud = mc.gui.hud;
        hud.leftHeight = Math.max(hud.leftHeight, used + 9);
        hud.rightHeight = Math.max(hud.rightHeight, used + 9);
    }

    /** HP の残像を、フレーム時間に応じて現在値へ近づける。 */
    private static void updateTrail(float hpRatio) {
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 0f : (now - lastFrameNanos) / 1_000_000_000f;
        lastFrameNanos = now;
        if (hpRatio >= hpTrail) {
            hpTrail = hpRatio; // 回復時は即座に追従
        } else {
            hpTrail = Math.max(hpRatio, hpTrail - ModConstants.HUD_DAMAGE_TRAIL_SPEED * Math.min(dt, 0.1f));
        }
    }

    /** HP/MP の表示用フォーマット（小数は切り上げて整数表示）。 */
    private static String format(float v) {
        return String.valueOf(Mth.ceil(v));
    }
}
