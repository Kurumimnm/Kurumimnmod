package io.github.kurumimnm.kurumimnmod.status;

/**
 * MOD の「防御力」（被ダメージを固定値で減らすステータス）を持つ Mob が実装するインターフェース。
 * <p>
 * プレイヤーの防御力ステータスと同じ式で軽減する（{@link StatManager#applyDefenseStat(float, double)}。受けるダメージが防御力以下なら被ダメ 1）。
 * 軽減の処理は {@link StatEvents} の onDamagePre がまとめて行うので、Mob 側は値を返すだけでよい。
 * 敵の VIT と同じ式（軽減量 ＝ 防御力 × 0.5）。
 */
public interface DefenseHolder {

    /** 現在の防御力（サーバー側で参照される）。 */
    int getDefense();
}
