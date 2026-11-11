package io.github.kurumimnm.kurumimnmod.level;

import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;

/**
 * レベルアップに必要な経験値の計算。
 * <pre>
 *   必要経験値(Lv) = baseExp + growth × (Lv - 初期レベル) ^ exponent   （小数は四捨五入、最低 1）
 *   ※ 初期レベルは Lv0（ModConstants.INITIAL_LEVEL）なので、実質 baseExp + growth × Lv ^ exponent
 *
 *   baseExp  … Lv0 → 1 に必要な経験値（カーブの基準）
 *   growth   … 1 レベルごとの増え方の大きさ
 *   exponent … 増え方のカーブ（1.0 = 一定ペース、大きいほど高レベルで重くなる）
 *
 *   デフォルト（baseExp = 20, growth = 4, exponent = 1.5）の目安:
 *     Lv0 → 1 :  20     Lv5  → 6  :    65
 *     Lv1 → 2 :  24     Lv10 → 11 :   146
 *     Lv2 → 3 :  31     Lv20 → 21 :   378
 *     Lv3 → 4 :  41     Lv50 → 51 : 1,434
 * </pre>
 * 係数は Config（kurumimnmod-synced.toml の [level]）で調整できる。
 * クライアントにも同期されるので、HUD の進捗表示もサーバーと同じ計算になる。
 */
public final class LevelCurve {

    private LevelCurve() {}

    /** 指定レベルから次のレベルへ上がるのに必要な経験値。 */
    public static long requiredExp(int level) {
        int steps = Math.max(0, level - ModConstants.INITIAL_LEVEL);
        double value = ModConfigs.levelBaseExp() + ModConfigs.levelGrowth() * Math.pow(steps, ModConfigs.levelExponent());
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
