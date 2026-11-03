package io.github.kurumimnm.kurumimnmod.client.hud;

import io.github.kurumimnm.kurumimnmod.client.status.ClientStatusPlaceholder;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;

/**
 * ホットバーの右に表示する「レベルエンブレム」。
 * <pre>
 *   ╭─金の縁─╮
 *   │  Lv   │   中央にレベル数値
 *   │  12   │   経験値が貯まると、下から金色の液体が満ちていく
 *   ╰───────╯   レベルアップ時は白く光り、外側に光の輪が広がる
 * </pre>
 * 色・大きさは {@link ModConstants} の LEVEL_* で調整できる。
 */
final class LevelEmblemHud {

    private LevelEmblemHud() {}

    /** 前回描画時のレベル（レベルアップ検出用）。-1 = 未初期化。 */
    private static int lastLevel = -1;
    /** レベルアップ演出の開始時刻（ナノ秒）。0 = 演出なし。 */
    private static long flashStartNanos = 0L;

    static void render(GuiGraphicsExtractor g, Minecraft mc, int screenW, int screenH) {
        int level = ClientStatusPlaceholder.level();
        float progress = ClientStatusPlaceholder.levelProgress();
        updateFlash(level);

        int size = ModConstants.LEVEL_EMBLEM_SIZE;
        float r = size / 2f;
        int x = screenW / 2 + 91 + ModConstants.LEVEL_EMBLEM_MARGIN + rightSideOffset(mc);
        int y = screenH - size;
        float cx = x + r;
        float cy = y + r;

        long now = System.nanoTime();
        float time = (now % 4_000_000_000L) / 1_000_000_000f; // 0〜4 秒でループするアニメーション用の時間
        float flash = flashAmount(now);

        // ---- 1. レベルアップ時：外側に広がる光の輪 ----
        if (flash > 0f) {
            float spread = (1f - flash) * 7f;
            int ringColor = HudDraw.withAlpha(ModConstants.LEVEL_COLOR_SURFACE, flash);
            HudDraw.ring(g, cx, cy, r + 1 + spread, r + spread - 0.5f, row -> ringColor);
        }

        // ---- 2. 外枠（濃い金）→ 金のリング（上が明るく下が暗いグラデーション） ----
        HudDraw.ring(g, cx, cy, r, 0, row -> ModConstants.LEVEL_COLOR_RIM_DARK);
        float rimR = r - 1;
        HudDraw.ring(g, cx, cy, rimR, 0, row -> HealthColor.lerpColor(
                ModConstants.LEVEL_COLOR_RIM_LIGHT, ModConstants.LEVEL_COLOR_RIM_SHADE, row / (rimR * 2)));

        // ---- 3. 内側：空の部分（灰色）＋ 下から満ちる経験値（金色の液体） ----
        float innerR = r - 3;
        int innerTop = (int) Math.floor(cy - innerR);
        int innerRows = (int) Math.ceil(cy + innerR) - innerTop;
        int filledRows = Math.round(innerRows * HudDraw.clamp01(progress));
        int surfaceRow = innerRows - filledRows; // この行が液面
        // 満タン間近（90%〜）は液面がゆっくり明滅して「もうすぐレベルアップ」を知らせる
        float pulse = progress >= 0.9f ? 0.5f + 0.5f * (float) Math.sin(time * Math.PI * 2) : 0f;

        HudDraw.ring(g, cx, cy, innerR, 0, row -> {
            if (row < surfaceRow) {
                return HealthColor.lerpColor(ModConstants.LEVEL_COLOR_EMPTY_TOP, ModConstants.LEVEL_COLOR_EMPTY_BOTTOM,
                        row / (float) innerRows);
            }
            if (row == surfaceRow && filledRows > 0) {
                return HealthColor.lerpColor(ModConstants.LEVEL_COLOR_SURFACE, 0xFFFFFFFF, pulse);
            }
            float depth = (row - surfaceRow) / (float) Math.max(1, filledRows);
            return HealthColor.lerpColor(ModConstants.LEVEL_COLOR_FILL_TOP, ModConstants.LEVEL_COLOR_FILL_BOTTOM, depth);
        });

        // ---- 4. ガラスのような光沢（左上に小さなハイライト） ----
        g.fill(x + 6, y + 5, x + 9, y + 6, 0x60FFFFFF);
        g.fill(x + 5, y + 6, x + 6, y + 9, 0x60FFFFFF);

        // ---- 5. レベルアップ時：内側全体を白く光らせる ----
        if (flash > 0f) {
            int glow = HudDraw.withAlpha(0xFFFFFFFF, flash * 0.8f);
            HudDraw.ring(g, cx, cy, innerR, 0, row -> glow);
        }

        // ---- 6. 文字（小さな「Lv」＋ レベル数値。縁取りで読みやすく） ----
        Font font = mc.font;
        g.pose().pushMatrix();
        g.pose().translate(cx, y + 4.5f);
        g.pose().scale(0.5f, 0.5f);
        HudDraw.outlinedText(g, font, "Lv", -font.width("Lv") / 2, 0, ModConstants.LEVEL_COLOR_RIM_LIGHT,
                ModConstants.LEVEL_COLOR_TEXT_OUTLINE);
        g.pose().popMatrix();

        String lv = String.valueOf(level);
        // 3 桁以上は少し縮小して枠に収める
        float numScale = lv.length() >= 3 ? 0.65f : 0.85f;
        g.pose().pushMatrix();
        g.pose().translate(cx, cy + 2f);
        g.pose().scale(numScale, numScale);
        HudDraw.outlinedText(g, font, lv, -font.width(lv) / 2 + 1, -4, ModConstants.LEVEL_COLOR_TEXT,
                ModConstants.LEVEL_COLOR_TEXT_OUTLINE);
        g.pose().popMatrix();
    }

    /**
     * ホットバーの右側に他の表示があるときは、その分エンブレムを右へずらす。
     * <ul>
     *   <li>利き手が左でオフハンドに物を持っている → オフハンドのスロットが右側に出る</li>
     *   <li>攻撃インジケーターが「ホットバー」設定で利き手が右 → インジケーターが右側に出る</li>
     * </ul>
     */
    private static int rightSideOffset(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) {
            return 0;
        }
        int offset = 0;
        boolean leftHanded = player.getMainArm() == HumanoidArm.LEFT;
        if (leftHanded && !player.getOffhandItem().isEmpty()) {
            offset += 29;
        }
        if (!leftHanded && mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) {
            offset += 22;
        }
        return offset;
    }

    /** レベルが上がったら演出を開始する。 */
    private static void updateFlash(int level) {
        if (lastLevel >= 0 && level > lastLevel) {
            flashStartNanos = System.nanoTime();
        }
        lastLevel = level;
    }

    /** 演出の強さ（1 = 開始直後 → 0 = 終了）。 */
    private static float flashAmount(long now) {
        if (flashStartNanos == 0L) {
            return 0f;
        }
        float t = (now - flashStartNanos) / 1_000_000_000f / ModConstants.LEVEL_UP_FLASH_SECONDS;
        if (t >= 1f) {
            flashStartNanos = 0L;
            return 0f;
        }
        return 1f - t;
    }
}
