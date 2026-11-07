package io.github.kurumimnm.kurumimnmod.world;

/**
 * ゲート（赤い壁）のルール。サーバーとクライアントの両方で同じ判定をする。
 * <ul>
 *   <li>壁は「場所」の境目（街 ↔ フィールド、フィールド ↔ フィールド）にある。同じフィールドのエリア同士の間にはない</li>
 *   <li>止めるのは「入るとき」だけ。行ったことのある場所へは自由に戻れる</li>
 *   <li>場所ごとに「入るのに必要な進行フラグ」（地図の unlock）がある。フラグを持っていれば入れる
 *       （例：gate＝緋の関所のボスを倒した。設計書 B-4 の進行フラグ）</li>
 *   <li>フラグは実績 kurumimnmod:progress/フラグ名 で管理する（{@link ExplorationTracker}）</li>
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
        return to.unlock() == null || to.unlock().isEmpty() || exploration.flags().contains(to.unlock());
    }

    /** a と b の境目に壁を表示するか（どちらか向きに通れなければ表示）。 */
    public static boolean hasWall(PlayerExploration exploration, FieldRegion a, FieldRegion b) {
        return !canEnter(exploration, a, b) || !canEnter(exploration, b, a);
    }
}
