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

    // ---- 氷柱（氷柱の杖の弾）。ダメージ・速さなどの調整値は Config の [magic.iceWand] ----
    /** 当たり判定の大きさ（ブロック）。 */
    public static final float ICE_ICICLE_SIZE = 0.5f;
    /** 貫通判定の太さ（ブロック）。見た目の太さに合わせて、敵の当たり判定をこれだけ広げて判定する。 */
    public static final double ICE_ICICLE_HIT_RADIUS = 0.4;
    /** 何にも当たらなかったときに消えるまでの時間（tick）。安全装置。 */
    public static final int ICE_ICICLE_MAX_LIFETIME = 200;
    /** 何ブロック先のプレイヤーまで見えるか（チャンク単位）。 */
    public static final int ICE_ICICLE_TRACKING_RANGE = 8;

    // =====================================================================
    // レベル
    // ※ ステータスの初期値・振り分けの上昇量・効果の数値は Config（ModConfigs の [status]）にある
    // =====================================================================
    /** 初期レベル。 */
    public static final int INITIAL_LEVEL = 1;

    // =====================================================================
    // HUD（画面下部の HP / MP / レベル表示）
    // 単位はすべて GUI ピクセル。色は ARGB（先頭 2 桁がアルファ）。
    // =====================================================================
    /** HP/MP バーの横幅（ホットバーと同じ 182）。 */
    public static final int HUD_BAR_WIDTH = 182;
    /** HP/MP バーの高さ（枠線を除く）。 */
    public static final int HUD_BAR_HEIGHT = 5;
    /** HP/MP バーに重ねる数値の文字サイズ（1.0 = 通常サイズ）。 */
    public static final float HUD_BAR_TEXT_SCALE = 0.65f;
    /** HP バーと MP バーの間隔。 */
    public static final int HUD_BAR_GAP = 1;
    /** ホットバー上端から MP バー下端までの余白。 */
    public static final int HUD_HOTBAR_MARGIN = 2;
    /** 乗り物のジャンプゲージ表示中に HUD を上へずらす量。 */
    public static final int HUD_JUMP_BAR_OFFSET = 7;
    /** 酸素ゲージの高さ。 */
    public static final int HUD_AIR_BAR_HEIGHT = 3;
    /** ダメージを受けたとき、減った分の「残像」が追いつく速さ（1 秒あたりの割合）。 */
    public static final float HUD_DAMAGE_TRAIL_SPEED = 1.5f;

    public static final int HUD_COLOR_BORDER = 0xFF000000;
    public static final int HUD_COLOR_BACKGROUND = 0xFF2B2B2B;
    /** HP 減少時の残像（半透明の白）。 */
    public static final int HUD_COLOR_HP_TRAIL = 0xA0FFFFFF;
    public static final int HUD_COLOR_MP = 0xFF3C78E6;
    public static final int HUD_COLOR_AIR = 0xFF9AE0FF;
    public static final int HUD_COLOR_TEXT = 0xFFFFFFFF;

    // ---- HP の色（割合に応じて 緑 → 黄 → オレンジ → 赤 に変化。プレイヤー・敵共通） ----
    /** 各色になる HP の割合（小さい順）。間の割合はなめらかに混ぜる。 */
    public static final float[] HP_COLOR_STOPS = {0.15f, 0.35f, 0.60f, 1.00f};
    /** HP_COLOR_STOPS に対応する色（赤・オレンジ・黄・緑）。 */
    public static final int[] HP_COLORS = {0xFFD83030, 0xFFF08C28, 0xFFE8D030, 0xFF3CC83C};

    // =====================================================================
    // レベルエンブレム（ホットバーの右に表示する丸いレベル表示）
    // =====================================================================
    /** エンブレムの直径（ホットバーの高さと同じ 22 推奨）。 */
    public static final int LEVEL_EMBLEM_SIZE = 22;
    /** ホットバー右端からの間隔。 */
    public static final int LEVEL_EMBLEM_MARGIN = 6;
    /** レベルアップ演出の長さ（秒）。 */
    public static final float LEVEL_UP_FLASH_SECONDS = 0.8f;

    public static final int LEVEL_COLOR_RIM_DARK = 0xFF4A3205;     // 外枠（濃い金）
    public static final int LEVEL_COLOR_RIM_LIGHT = 0xFFFFE27A;    // 金のリング（上側）
    public static final int LEVEL_COLOR_RIM_SHADE = 0xFFB07A10;    // 金のリング（下側）
    public static final int LEVEL_COLOR_EMPTY_TOP = 0xFF5A5A5A;    // 空の部分（上側・灰色）
    public static final int LEVEL_COLOR_EMPTY_BOTTOM = 0xFF2E2E2E; // 空の部分（下側）
    public static final int LEVEL_COLOR_FILL_TOP = 0xFFFFD84A;     // 経験値（液面側・明るい黄）
    public static final int LEVEL_COLOR_FILL_BOTTOM = 0xFFD07800;  // 経験値（底側・濃い黄）
    public static final int LEVEL_COLOR_SURFACE = 0xFFFFF6C0;      // 液面のハイライト
    public static final int LEVEL_COLOR_TEXT = 0xFFFFFFFF;
    public static final int LEVEL_COLOR_TEXT_OUTLINE = 0xFF3A2400;

    // =====================================================================
    // 武器ダメージ表示（レベルエンブレムの右）
    // =====================================================================
    /** レベルエンブレムとの間隔。 */
    public static final int DAMAGE_DISPLAY_MARGIN = 4;
    /** アイコンと数値の間隔。 */
    public static final int DAMAGE_DISPLAY_ICON_GAP = 2;
    /**
     * 攻撃のクールダウンが溜まっていないときのダメージ倍率の下限（バニラの仕様と同じ 0.2）。
     * バニラは「ダメージ × (0.2 + 溜まり具合² × 0.8)」なので、最小 = 最大 × 0.2 になる。
     */
    public static final float ATTACK_MIN_CHARGE_RATIO = 0.2f;
    public static final int DAMAGE_COLOR_PHYSICAL = 0xFFFFE0C0;  // 物理ダメージの数値（淡いオレンジ）
    public static final int DAMAGE_COLOR_MAGIC = 0xFFE0B8FF;     // 魔法ダメージの数値（淡い紫）

    // =====================================================================
    // 魔法の照準サークル（右クリック長押し中に着弾地点へ表示）
    // 色は ARGB。中心から外周にかけて塗りが薄くなる（ダメージ減衰のイメージ）
    // =====================================================================
    /** 円を何分割して描くか（多いほどなめらか）。 */
    public static final int AOE_SEGMENTS = 48;
    /** 外周の線の太さ（ブロック）。 */
    public static final float AOE_RING_WIDTH = 0.08f;
    /** 地面からの浮かせ量（ちらつき防止）。 */
    public static final float AOE_SURFACE_OFFSET = 0.03f;
    /** 発射できるとき：塗りの中心・外周・線の色。 */
    public static final int AOE_COLOR_FILL_CENTER = 0x6080D8FF;
    public static final int AOE_COLOR_FILL_EDGE = 0x2080D8FF;
    public static final int AOE_COLOR_RING = 0xD0B8ECFF;
    /** MP 不足・クールダウン中：塗りの中心・外周・線の色。 */
    public static final int AOE_COLOR_DISABLED_FILL_CENTER = 0x50FF6060;
    public static final int AOE_COLOR_DISABLED_FILL_EDGE = 0x18FF6060;
    public static final int AOE_COLOR_DISABLED_RING = 0xB0FF8080;

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
    /** 目盛りの間隔（0.1 = HP 10% ごと）。 */
    public static final float ENEMY_BAR_TICK_STEP = 0.1f;
    /** 目盛り線の太さ。 */
    public static final float ENEMY_BAR_TICK_WIDTH = 0.012f;
    public static final int ENEMY_BAR_COLOR_TICK = 0x90000000;
    // ※ HP 部分の色は HP_COLORS（割合で変化）を使用
}
