package io.github.kurumimnm.kurumimnmod.client.hud;

import io.github.kurumimnm.kurumimnmod.config.ModConstants;

/**
 * HP の割合から表示色を求める（緑 → 黄 → オレンジ → 赤）。
 * プレイヤーの HP バーと敵の頭上 HP バーで共通で使う。
 * 色と切り替わる割合は {@link ModConstants#HP_COLORS} / {@link ModConstants#HP_COLOR_STOPS}。
 */
public final class HealthColor {

    private HealthColor() {}

    /** @param ratio HP の割合（0〜1） */
    public static int forRatio(float ratio) {
        float[] stops = ModConstants.HP_COLOR_STOPS;
        int[] colors = ModConstants.HP_COLORS;
        if (ratio <= stops[0]) {
            return colors[0];
        }
        for (int i = 1; i < stops.length; i++) {
            if (ratio <= stops[i]) {
                float t = (ratio - stops[i - 1]) / (stops[i] - stops[i - 1]);
                return lerpColor(colors[i - 1], colors[i], t);
            }
        }
        return colors[colors.length - 1];
    }

    /** 2 色を t（0〜1）で混ぜる。 */
    public static int lerpColor(int from, int to, float t) {
        t = Math.clamp(t, 0f, 1f);
        int a = lerp(from >>> 24, to >>> 24, t);
        int r = lerp((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = lerp((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = lerp(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }
}
