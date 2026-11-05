package io.github.kurumimnm.kurumimnmod.world;

/**
 * ゲート（赤い壁）のルール。サーバーとクライアントの両方で同じ判定をする。
 * <ul>
 *   <li>壁は「場所」の境目（街 ↔ フィールド、フィールド ↔ フィールド）にある。同じフィールドのエリア同士の間にはない</li>
 *   <li>止めるのは「入るとき」だけ。行ったことのある場所へは自由に戻れる</li>
 *   <li>街から周りのフィールドへは自由に出られる（一度行った街の周りの壁は消える）</li>
 *   <li>フィールド A から、まだ行ったことのない場所（フィールド B・街）へ入るには、A のフィールドボスを倒している必要がある</li>
 * </ul>
 */
public final class GateRules {

    private GateRules() {}

    /** from（今いる場所）から to へ入れるか。 */
    public static boolean canEnter(PlayerExploration exploration, FieldRegion from, FieldRegion to) {
        if (from.sameZone(to)) {
            return true; // 同じ街・同じフィールドの中（エリア間）は壁なし
        }
        if (to.isTown() ? exploration.towns().contains(to.town()) : exploration.fields().contains(to.field())) {
            return true; // 行ったことのある場所へは戻れる
        }
        if (from.isTown()) {
            return true; // 街からはどのフィールドへも出られる
        }
        return exploration.clearedFields().contains(from.field()); // 今いるフィールドのボスを倒していれば先へ進める
    }

    /** a と b の境目に壁を表示するか（どちらか向きに通れなければ表示）。 */
    public static boolean hasWall(PlayerExploration exploration, FieldRegion a, FieldRegion b) {
        return !canEnter(exploration, a, b) || !canEnter(exploration, b, a);
    }
}
