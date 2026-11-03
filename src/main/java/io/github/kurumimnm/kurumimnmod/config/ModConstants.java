package io.github.kurumimnm.kurumimnmod.config;

/**
 * コード内で使う固定の数値（定数）をまとめるクラス。
 * <p>
 * アイテムの耐久値・攻撃力・攻撃速度、ブロックの硬さ・爆発耐性、
 * エンティティの基本ステータス（体力・移動速度）など、
 * 「レジストリ登録時に決まる値」はここに集約する。
 * 値を変えたいときはこのファイルだけを編集すればよいようにする。
 * <p>
 * ゲーム中に調整したい値（スポーン確率など）は {@link ModConfigs} に置くこと。
 */
public final class ModConstants {

    private ModConstants() {}

    // =====================================================================
    // アイテム
    // =====================================================================
    // 例) public static final int EXAMPLE_SWORD_DURABILITY = 250;

    // =====================================================================
    // ブロック
    // =====================================================================
    // 例) public static final float EXAMPLE_BLOCK_HARDNESS = 1.5f;

    // =====================================================================
    // エンティティ
    // =====================================================================
    // 例) public static final double EXAMPLE_MOB_MAX_HEALTH = 20.0;
    // 例) public static final double EXAMPLE_MOB_MOVEMENT_SPEED = 0.25;

    // =====================================================================
    // ステータス（仮置き）
    // ※ 本実装（HP/MP/経験値/レベル/ステータスの独自システム）ができたら置き換える
    // =====================================================================
    /** 仮置きの最大 MP。 */
    public static final int PLACEHOLDER_MAX_MP = 20;
    /** 1 レベル上がるごとにもらえるステータスポイント。 */
    public static final int STAT_POINTS_PER_LEVEL = 1;

    // =====================================================================
    // HUD（画面下部の HP / MP / レベル表示）
    // 単位はすべて GUI ピクセル。色は ARGB（先頭 2 桁がアルファ）。
    // =====================================================================
    /** HP/MP バーの横幅（ホットバーと同じ 182）。 */
    public static final int HUD_BAR_WIDTH = 182;
    /** HP/MP バーの高さ（数値を中に表示するので 9 以上推奨）。 */
    public static final int HUD_BAR_HEIGHT = 9;
    /** HP バーと MP バーの間隔。 */
    public static final int HUD_BAR_GAP = 1;
    /** ホットバー上端から MP バー下端までの余白。 */
    public static final int HUD_HOTBAR_MARGIN = 2;
    /** レベルアイコン（丸）の直径。 */
    public static final int HUD_LEVEL_ICON_SIZE = 17;
    /** レベルアイコンと HP バーの間隔。 */
    public static final int HUD_LEVEL_ICON_GAP = 1;
    /** 乗り物のジャンプゲージ表示中に HUD を上へずらす量。 */
    public static final int HUD_JUMP_BAR_OFFSET = 7;
    /** 酸素ゲージの高さ。 */
    public static final int HUD_AIR_BAR_HEIGHT = 5;
    /** ダメージを受けたとき、減った分の「残像」が追いつく速さ（1 秒あたりの割合）。 */
    public static final float HUD_DAMAGE_TRAIL_SPEED = 1.5f;

    public static final int HUD_COLOR_BORDER = 0xFF000000;
    public static final int HUD_COLOR_BACKGROUND = 0xFF2B2B2B;
    public static final int HUD_COLOR_HP = 0xFF3CC83C;
    public static final int HUD_COLOR_HP_LOW = 0xFFE0A030;
    public static final int HUD_COLOR_HP_TRAIL = 0xFFD04040;
    public static final int HUD_COLOR_MP = 0xFF3C78E6;
    public static final int HUD_COLOR_AIR = 0xFF9AE0FF;
    public static final int HUD_COLOR_TEXT = 0xFFFFFFFF;
    public static final int HUD_COLOR_LEVEL_EMPTY = 0xFF7A7A7A;
    public static final int HUD_COLOR_LEVEL_FILL = 0xFFF2C230;
    /** HP がこの割合以下になったら HP バーを警告色にする。 */
    public static final float HUD_HP_LOW_RATIO = 0.25f;

    // =====================================================================
    // 敵の頭上 HP バー
    // 単位はブロック。
    // =====================================================================
    /** バーの横幅。 */
    public static final float ENEMY_BAR_WIDTH = 0.9f;
    /** バーの高さ。 */
    public static final float ENEMY_BAR_HEIGHT = 0.09f;
    /** 枠線の太さ。 */
    public static final float ENEMY_BAR_BORDER = 0.02f;
    /** 当たり判定の上端からバーまでの高さ。 */
    public static final float ENEMY_BAR_Y_OFFSET = 0.35f;
    /** 名札が表示されているときに追加で上へずらす量。 */
    public static final float ENEMY_BAR_NAMETAG_OFFSET = 0.3f;
    public static final int ENEMY_BAR_COLOR_BORDER = 0xC0000000;
    public static final int ENEMY_BAR_COLOR_BACKGROUND = 0xC0404040;
    public static final int ENEMY_BAR_COLOR_FILL = 0xFFD83030;
}
