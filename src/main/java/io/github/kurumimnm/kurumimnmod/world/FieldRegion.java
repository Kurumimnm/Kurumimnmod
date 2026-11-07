package io.github.kurumimnm.kurumimnmod.world;

/**
 * 「場所」。街の中か、フィールドのどのエリアか。
 * <p>
 * ゲートの壁は「場所」が変わるところ（街 ↔ フィールド、フィールド ↔ フィールド）にできる。
 * 同じフィールドのエリア同士の間には壁はない。
 *
 * @param town  街の id（フィールドなら null）
 * @param field フィールドの番号（街なら 0）
 * @param area  エリアの番号（街なら 0）
 * @param unlock 入るのに必要な進行フラグ（空なら誰でも入れる。{@link GateRules}）
 */
public record FieldRegion(String town, int field, int area, String unlock) {

    public static FieldRegion town(String id) {
        return new FieldRegion(id, 0, 0, "");
    }

    public static FieldRegion town(String id, String unlock) {
        return new FieldRegion(id, 0, 0, unlock);
    }

    public static FieldRegion field(int field, int area) {
        return new FieldRegion(null, field, area, "");
    }

    public static FieldRegion field(int field, int area, String unlock) {
        return new FieldRegion(null, field, area, unlock);
    }

    public boolean isTown() {
        return town != null;
    }

    /** 同じ街、または同じフィールド（エリアは問わない）なら true。 */
    public boolean sameZone(FieldRegion other) {
        if (isTown() || other.isTown()) {
            return isTown() && other.isTown() && town.equals(other.town);
        }
        return field == other.field;
    }

    /** エリアを 1 つの数にしたもの（フィールド番号 × 100 ＋ エリア番号）。 */
    public int areaKey() {
        return areaKey(field, area);
    }

    public static int areaKey(int field, int area) {
        return field * 100 + area;
    }
}
