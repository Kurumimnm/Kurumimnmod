package io.github.kurumimnm.kurumimnmod.level;

import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;

/**
 * レベルアップに必要な経験値の計算。
 * <pre>
 *   必要経験値(Lv) = baseExp × Lv ^ exponent   （小数は四捨五入、最低 1）
 *
 *   デフォルト（baseExp = 100, exponent = 1.5）の目安:
 *     Lv1 → 2 :    100     Lv10 → 11 :  3,162
 *     Lv2 → 3 :    283     Lv20 → 21 :  8,944
 *     Lv5 → 6 :  1,118     Lv50 → 51 : 35,355
 * </pre>
 * 係数は Config（kurumimnmod-synced.toml の [level]）で調整できる。
 * クライアントにも同期されるので、HUD の進捗表示もサーバーと同じ計算になる。
 */
public final class LevelCurve {

    private LevelCurve() {}

    /** 指定レベルから次のレベルへ上がるのに必要な経験値。 */
    public static long requiredExp(int level) {
        double value = ModConfigs.levelBaseExp() * Math.pow(Math.max(1, level), ModConfigs.levelExponent());
        return Math.max(1L, Math.round(value));
    }

    /** 最大レベルに達しているか（maxLevel = 0 なら上限なし）。 */
    public static boolean isMaxLevel(int level) {
        int max = ModConfigs.maxLevel();
        return max > 0 && level >= max;
    }

    /** レベルを有効な範囲（初期レベル〜最大レベル）に収める。 */
    public static int clampLevel(int level) {
        int clamped = Math.max(ModConstants.INITIAL_LEVEL, level);
        int max = ModConfigs.maxLevel();
        return max > 0 ? Math.min(clamped, max) : clamped;
    }
}
