package io.github.kurumimnm.kurumimnmod.status;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;

/**
 * プレイヤーのステータス（最終的な値）の種類。
 * <p>
 * 値は「初期値（Config）＋振り分けによる上昇」で決まり、{@link StatManager#value} で取得する。
 * <ul>
 *   <li>{@link #visible()} が true のものはステータス画面に表示する</li>
 *   <li>攻撃速度・移動速度・ジャンプ力は倍率（1.0 = バニラ）。内部用で画面には出さない</li>
 * </ul>
 * 表示名は lang の {@code stat.kurumimnmod.<id>}、説明文は {@code stat.kurumimnmod.<id>.desc}。
 */
public enum StatType {
    /** 攻撃力：与えるダメージの倍率 */
    ATTACK("attack", true),
    /** 防御力：敵から受けるダメージを固定値で軽減 */
    DEFENSE("defense", true),
    /** 知力：魔法ダメージ（魔法の実装時に効果を追加） */
    INTELLIGENCE("intelligence", true),
    /** 技量：クリティカル率・クリティカル倍率 */
    DEXTERITY("dexterity", true),
    /** ラック：バニラの「幸運」属性に連動 */
    LUCK("luck", true),
    /** 抵抗力：デバフ耐性など（デバフ・ギミックの実装時に効果を追加） */
    RESISTANCE("resistance", true),
    /** 攻撃速度（倍率） */
    ATTACK_SPEED("attack_speed", false),
    /** 移動速度（倍率） */
    MOVE_SPEED("move_speed", false),
    /** ジャンプ力（倍率） */
    JUMP_POWER("jump_power", false);

    private final String id;
    private final boolean visible;

    StatType(String id, boolean visible) {
        this.id = id;
        this.visible = visible;
    }

    public String id() {
        return id;
    }

    /** ステータス画面に表示するか。 */
    public boolean visible() {
        return visible;
    }

    public String nameKey() {
        return "stat." + Kurumimnmod.MOD_ID + "." + id;
    }

    public String descriptionKey() {
        return nameKey() + ".desc";
    }
}
