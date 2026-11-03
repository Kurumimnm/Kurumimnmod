package io.github.kurumimnm.kurumimnmod.scarecrow;

import io.github.kurumimnm.kurumimnmod.config.ModConstants;

/**
 * カカシに表示するダメージ数字の種類（色・大きさを変える）。
 * 色は {@link ModConstants} の DAMAGE_NUMBER_COLOR_*。
 */
public enum DamageKind {
    /** 通常の攻撃（近接・弓など）。白 */
    NORMAL(ModConstants.DAMAGE_NUMBER_COLOR_NORMAL),
    /** クリティカル。金・大きめ */
    CRITICAL(ModConstants.DAMAGE_NUMBER_COLOR_CRIT),
    /** 魔法攻撃（kurumimnmod:magic_attack タグ / バニラの魔法）。水色 */
    MAGIC(ModConstants.DAMAGE_NUMBER_COLOR_MAGIC),
    /** 攻撃者のいないダメージ（炎上・毒・ウィザーなど）。灰 */
    OTHER(ModConstants.DAMAGE_NUMBER_COLOR_OTHER),
    /** HP を削り切った一撃（有限 HP のとき。直後に全回復する）。赤 */
    BREAK(ModConstants.DAMAGE_NUMBER_COLOR_BREAK);

    private static final DamageKind[] VALUES = values();

    private final int color;

    DamageKind(int color) {
        this.color = color;
    }

    public int color() {
        return color;
    }

    /** 通信で受け取った番号から復元する（範囲外は NORMAL）。 */
    public static DamageKind byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : NORMAL;
    }
}
