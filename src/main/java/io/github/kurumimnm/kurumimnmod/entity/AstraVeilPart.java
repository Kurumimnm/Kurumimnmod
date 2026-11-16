package io.github.kurumimnm.kurumimnmod.entity;

/**
 * アストラ・ヴェイルの部位（モデルのボーン名との対応表）。
 * <p>
 * 今は一覧だけ（攻撃・ダメージ判定は未実装）。将来、部位ごとの攻撃・当たり判定・専用アニメを作るときに、
 * ここからボーン名を引く。ボーンは tools/astra_veil/generate.py が作る（名前を変えたらここも合わせる）。
 * 触腕は「根元のボーン（例：tent_a2）→ 節のボーン（tent_a2_0, tent_a2_1, …）」の鎖になっている。
 * 根元のボーンを回せば触腕全体が動き、節のボーンを回せば途中から曲がる。
 */
public enum AstraVeilPart {
    /** 頭部（傘）。顔・頭の芯・冠の星・傘のふちの葉と膜・触角・ふちの細い触腕がぶら下がる。 */
    HEAD("head", 1),
    /** 顔（仮面・くちばし・冠のとげ・発光器官）。 */
    FACE("face", 1),
    /** 首〜胴全体の付け根（頭より少し遅れて向きを変える）。 */
    TORSO("torso", 1),
    /** 首・胴の関節（body_0〜1 = 首、body_2〜3 = 胸、body_4〜14 = 胴〜尾）。見た目の箱は body_N_s（呼吸でふくらむ殻）。 */
    BODY("body_", 15),
    /** 大型触腕（胸元から左右 4 本ずつ。tent_a0〜3 = 右、tent_b0〜3 = 左。0 が前、3 が後ろ）。 */
    TENTACLE_RIGHT("tent_a", 4),
    TENTACLE_LEFT("tent_b", 4),
    /** 尾びれ（tail_fin_0〜4）と、尾から流れる飾り帯（streamer_0〜3）。 */
    TAIL_FIN("tail_fin_", 5),
    TAIL_STREAMER("streamer_", 4),
    /** 顔の琥珀色の発光器官（eye_a0〜5 / eye_b0〜5）。 */
    EYE_A("eye_a", 6),
    EYE_B("eye_b", 6),
    /** 腹に並ぶ青い発光器官（belly_organ_2〜12。番号は胴の関節の番号）。 */
    BELLY_ORGAN("belly_organ_", 15);

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
