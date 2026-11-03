package io.github.kurumimnm.kurumimnmod.client.hud;

import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * HUD 描画で共通して使う小さな描画ヘルパー。
 * テクスチャを使わず fill（塗りつぶし）だけで描くので、リソースパック不要・軽量。
 */
final class HudDraw {

    private HudDraw() {}

    /**
     * 枠付きのゲージを描く。
     *
     * @param ratio      満タンに対する割合（0〜1）
     * @param trailRatio 減少演出用の「残像」の割合（ratio 以上なら ratio〜trailRatio を trailColor で描く。不要なら 0）
     */
    static void bar(GuiGraphicsExtractor g, int x, int y, int width, int height,
                    float ratio, int fillColor, float trailRatio, int trailColor) {
        // 枠
        g.fill(x - 1, y - 1, x + width + 1, y + height + 1, ModConstants.HUD_COLOR_BORDER);
        // 背景
        g.fill(x, y, x + width, y + height, ModConstants.HUD_COLOR_BACKGROUND);

        int fillW = Math.round(width * clamp01(ratio));
        int trailW = Math.round(width * clamp01(trailRatio));
        // 残像（減った分）
        if (trailW > fillW) {
            g.fill(x + fillW, y, x + trailW, y + height, trailColor);
        }
        // 本体
        if (fillW > 0) {
            g.fill(x, y, x + fillW, y + height, fillColor);
            // 上端に 1px のハイライトを入れて立体感を出す
            g.fill(x, y, x + fillW, y + 1, brighten(fillColor));
        }
    }

    /** ゲージの中央に数値テキストを描く。 */
    static void centeredLabel(GuiGraphicsExtractor g, Font font, String text, int x, int y, int width, int height) {
        int tx = x + (width - font.width(text)) / 2;
        // 数字のグリフは高さ 7px なので、バーの中央に来るよう調整
        int ty = y + (height - 7) / 2;
        g.text(font, text, tx, ty, ModConstants.HUD_COLOR_TEXT, true);
    }

    /**
     * 丸いレベルアイコンを描く。下から progress の割合だけ fillColor で塗る。
     * ピクセル単位で行ごとに fill しているので、どの GUI スケールでもくっきり表示される。
     */
    static void levelCircle(GuiGraphicsExtractor g, int x, int y, int size, float progress, int emptyColor, int fillColor) {
        float r = size / 2f;
        int filledRows = Math.round(size * clamp01(progress));
        for (int row = 0; row < size; row++) {
            // 行の中心から円の幅を求める
            float dy = row + 0.5f - r;
            float half = (float) Math.sqrt(Math.max(0, r * r - dy * dy));
            int left = Math.round(r - half);
            int right = Math.round(r + half);
            if (right <= left) {
                continue;
            }
            // 外枠（1px）
            g.fill(x + left, y + row, x + right, y + row + 1, ModConstants.HUD_COLOR_BORDER);

            // 内側（半径 -1）
            float ri = r - 1;
            float half2 = (float) Math.sqrt(Math.max(0, ri * ri - dy * dy));
            int l2 = Math.round(r - half2);
            int r2 = Math.round(r + half2);
            if (r2 > l2 && row > 0 && row < size - 1) {
                boolean filled = row >= size - filledRows;
                g.fill(x + l2, y + row, x + r2, y + row + 1, filled ? fillColor : emptyColor);
            }
        }
    }

    static float clamp01(float v) {
        return Math.clamp(v, 0f, 1f);
    }

    /** 色を少し明るくする（ハイライト用）。 */
    static int brighten(int argb) {
        int a = argb >>> 24;
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 50);
        int gr = Math.min(255, ((argb >> 8) & 0xFF) + 50);
        int b = Math.min(255, (argb & 0xFF) + 50);
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }
}
