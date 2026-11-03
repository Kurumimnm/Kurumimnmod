package io.github.kurumimnm.kurumimnmod.status;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;

/**
 * プレイヤーが振り分けられるステータスの種類。
 * <p>
 * 表示名は lang ファイルの {@code stat.kurumimnmod.<id>}（略称）と
 * {@code stat.kurumimnmod.<id>.desc}（説明）で定義する。
 * 効果（攻撃力・最大 HP などへの反映）は後日の本実装で追加する。
 */
public enum StatType {
    /** 筋力：物理攻撃力など（予定） */
    STR("str"),
    /** 体力：最大 HP など（予定） */
    VIT("vit"),
    /** 知力：最大 MP・魔法攻撃力など（予定） */
    INT("int"),
    /** 器用さ：命中・クリティカルなど（予定） */
    DEX("dex"),
    /** 敏捷：移動速度・攻撃速度など（予定） */
    AGI("agi");

    private final String id;

    StatType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** 略称（STR 等）の翻訳キー。 */
    public String nameKey() {
        return "stat." + Kurumimnmod.MOD_ID + "." + id;
    }

    /** 説明文の翻訳キー。 */
    public String descriptionKey() {
        return nameKey() + ".desc";
    }
}
