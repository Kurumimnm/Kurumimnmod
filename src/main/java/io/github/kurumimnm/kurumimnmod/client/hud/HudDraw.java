package io.github.kurumimnm.kurumimnmod.client.hud;

import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import java.util.function.IntUnaryOperator;
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
            // 上端に 1px のハイライト、下端に 1px の影を入れて立体感を出す
            g.fill(x, y, x + fillW, y + 1, brighten(fillColor, 50));
            if (height >= 4) {
                g.fill(x, y + height - 1, x + fillW, y + height, darken(fillColor, 40));
            }
        }
    }

    /**
     * ゲージの中央に数値テキストを縮小して重ねる。
     * 細いバーでも読めるよう {@link ModConstants#HUD_BAR_TEXT_SCALE} で縮小する。
     */
    static void centeredLabel(GuiGraphicsExtractor g, Font font, String text, int x, int y, int width, int height) {
        float scale = ModConstants.HUD_BAR_TEXT_SCALE;
        float cx = x + width / 2f;
        float cy = y + height / 2f;
        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().scale(scale, scale);
        // 数字のグリフは高さ 7px なので、縦方向は -3.5 で中央に揃う
        g.text(font, text, -font.width(text) / 2, -4, ModConstants.HUD_COLOR_TEXT, true);
        g.pose().popMatrix();
    }

    /**
     * 円（またはドーナツ形）を行ごとの fill で描く。どの GUI スケールでもくっきり表示される。
     *
     * @param cx,cy       中心（GUI 座標）
     * @param rOuter      外側の半径
     * @param rInner      内側の半径（0 なら塗りつぶしの円）
     * @param rowColor    行（0 = 一番上）ごとの色を返す関数。0 を返した行は描かない
     */
    static void ring(GuiGraphicsExtractor g, float cx, float cy, float rOuter, float rInner, IntUnaryOperator rowColor) {
        int top = (int) Math.floor(cy - rOuter);
        int bottom = (int) Math.ceil(cy + rOuter);
        for (int py = top; py < bottom; py++) {
            float dy = py + 0.5f - cy;
            float outerHalf = halfWidth(rOuter, dy);
            if (outerHalf <= 0) {
                continue;
            }
            int color = rowColor.applyAsInt(py - top);
            if ((color >>> 24) == 0) {
                continue;
            }
            int ol = Math.round(cx - outerHalf);
            int or = Math.round(cx + outerHalf);
            float innerHalf = halfWidth(rInner, dy);
            if (innerHalf <= 0) {
                g.fill(ol, py, or, py + 1, color);
            } else {
                int il = Math.round(cx - innerHalf);
                int ir = Math.round(cx + innerHalf);
                if (il > ol) g.fill(ol, py, il, py + 1, color);
                if (or > ir) g.fill(ir, py, or, py + 1, color);
            }
        }
    }

    /** 半径 r の円の、中心から dy 離れた行の半幅。 */
    private static float halfWidth(float r, float dy) {
        float v = r * r - dy * dy;
        return v <= 0 ? 0 : (float) Math.sqrt(v);
    }

    /** 文字に 1px の縁取りを付けて描く（小さい文字でも背景に埋もれないように）。 */
    static void outlinedText(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color, int outline) {
        g.text(font, text, x - 1, y, outline, false);
        g.text(font, text, x + 1, y, outline, false);
        g.text(font, text, x, y - 1, outline, false);
        g.text(font, text, x, y + 1, outline, false);
        g.text(font, text, x, y, color, false);
    }

    static float clamp01(float v) {
        return Math.clamp(v, 0f, 1f);
    }

    /** アルファだけを掛けた色を返す（0〜1）。 */
    static int withAlpha(int argb, float alpha) {
        int a = Math.round((argb >>> 24) * clamp01(alpha));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    /** 色を少し明るくする（ハイライト用）。 */
    static int brighten(int argb, int amount) {
        return shift(argb, amount);
    }

    /** 色を少し暗くする（影用）。 */
    static int darken(int argb, int amount) {
        return shift(argb, -amount);
    }

    private static int shift(int argb, int amount) {
        int a = argb >>> 24;
        int r = Math.clamp(((argb >> 16) & 0xFF) + amount, 0, 255);
        int gr = Math.clamp(((argb >> 8) & 0xFF) + amount, 0, 255);
        int b = Math.clamp((argb & 0xFF) + amount, 0, 255);
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }
}
