package io.github.kurumimnm.kurumimnmod.entity;

/**
 * アストラ・ヴェイルの部位（モデルのボーン名との対応表）。
 * <p>
 * 今は一覧だけ（攻撃・ダメージ判定は未実装）。将来、部位ごとの攻撃・当たり判定・専用アニメを作るときに、
 * ここからボーン名を引く。ボーンは tools/astra_veil/generate.py が作る（名前を変えたらここも合わせる）。
 * 触腕は「根元のボーン（例：lat_a2）→ 節のボーン（lat_a2_0, lat_a2_1, …）」の鎖になっている。
 * 根元のボーンを回せば触腕全体が動き、節のボーンを回せば途中から曲がる。
 */
public enum AstraVeilPart {
    /** 頭部（傘）。顔・冠・傘のふちの葉と膜・触角・上へ伸びる触腕がぶら下がる。 */
    HEAD("head", 1),
    /** 顔（仮面・くちばし・冠のとげ・発光器官）。 */
    FACE("face", 1),
    /** 胴全体（首〜胴）。body_0〜body_5 の節が上から下へ連なる。 */
    TORSO("torso", 1),
    /** 大型触腕の付け根（傘の裏）。 */
    MANTLE("mantle", 1),
    /** 左右へ広がる大型触腕（lat_a0〜5 / lat_b0〜5）。 */
    TENTACLE_SIDE_A("lat_a", 6),
    TENTACLE_SIDE_B("lat_b", 6),
    /** 上へ伸びる大型触腕（up_a0〜2 / up_b0〜2）。 */
    TENTACLE_UP_A("up_a", 3),
    TENTACLE_UP_B("up_b", 3),
    /** 後ろへ伸びる大型触腕（back_0〜7）。 */
    TENTACLE_BACK("back_", 8),
    /** 前へ伸びる大型触腕（front_0〜3）。 */
    TENTACLE_FRONT("front_", 4),
    /** 下へ垂れる大型触腕（down_0〜4）。 */
    TENTACLE_DOWN("down_", 5),
    /** 帯状の長い触腕（rib_0〜9）。 */
    RIBBON("rib_", 10),
    /** 顔の琥珀色の発光器官（eye_a0〜5 / eye_b0〜5）。 */
    EYE_A("eye_a", 6),
    EYE_B("eye_b", 6),
    /** 胴の前面に並ぶ青い発光器官（core_organ_0〜4）。 */
    CORE_ORGAN("core_organ_", 5);

    private final String bonePrefix;
    private final int count;

    AstraVeilPart(String bonePrefix, int count) {
        this.bonePrefix = bonePrefix;
        this.count = count;
    }

    /** この部位のボーンの数（1 のものは {@link #bone()} がそのままボーン名）。 */
    public int count() {
        return this.count;
    }

    /** 1 つだけの部位のボーン名。 */
    public String bone() {
        return this.bonePrefix;
    }

    /** index 番目（0 始まり）のボーン名（触腕なら根元のボーン）。 */
    public String bone(int index) {
        return this.count == 1 ? this.bonePrefix : this.bonePrefix + index;
    }
}
