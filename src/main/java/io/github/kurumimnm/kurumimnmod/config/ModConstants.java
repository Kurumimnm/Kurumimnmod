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

    // ---- カカシ（ステータス実験用の的） ----
    // ※ 召喚直後の初期設定（HP・防御力・反撃など）は Config の [scarecrow]。ここは登録時の値と入力の上限。
    /** 当たり判定の幅・高さ（ブロック）。モデル（scarecrow.geo.json）の大きさに合わせてある。 */
    public static final float SCARECROW_WIDTH = 0.8f;
    public static final float SCARECROW_HEIGHT = 2.2f;
    /** クライアントへ同期する距離（チャンク）。 */
    public static final int SCARECROW_TRACKING_RANGE = 10;
    /** 属性登録時の最大 HP（召喚時に設定値で上書きされる）。 */
    public static final double SCARECROW_BASE_HEALTH = 100.0;
    /** 設定画面で入力できる値の範囲（サーバー側でもこの範囲に丸める）。 */
    public static final double SCARECROW_MIN_HEALTH = 1.0;
    public static final double SCARECROW_MAX_HEALTH_LIMIT = 1024.0;   // バニラの最大 HP 属性の上限
    public static final int SCARECROW_DEFENSE_LIMIT = 1_000_000;
    public static final double SCARECROW_ARMOR_LIMIT = 30.0;          // バニラの防具ポイント属性の上限
    public static final double SCARECROW_COUNTER_DAMAGE_LIMIT = 1_000_000.0;
    public static final double SCARECROW_COUNTER_INTERVAL_MIN = 0.1;
    public static final double SCARECROW_COUNTER_INTERVAL_MAX = 60.0;
    public static final double SCARECROW_COUNTER_RANGE_MIN = 0.5;
    public static final double SCARECROW_COUNTER_RANGE_MAX = 16.0;
    /** 設定画面を開く・保存・削除できる距離（ブロック。不正なパケット対策）。 */
    public static final double SCARECROW_INTERACT_DISTANCE = 16.0;
    /** 召喚位置からこれ以上離れていたら戻る（ブロック）。 */
    public static final double SCARECROW_HOME_EPSILON = 0.02;
    /** 召喚時の向きを丸める角度（度。アーマースタンドと同じ 22.5°刻み）。 */
    public static final float SCARECROW_YAW_STEP = 22.5f;

    // ---- ダメージ数字（カカシの頭上に浮かぶ数値） ----
    /** 数字が出る高さ（当たり判定の上端からのブロック数）。 */
    public static final float DAMAGE_NUMBER_Y_OFFSET = 0.25f;
    /** 消えるまでに浮き上がる高さ（ブロック）。 */
    public static final float DAMAGE_NUMBER_RISE = 0.9f;
    /** 出現位置の左右のばらつき（ブロック。連続ヒットで数字が重ならないように）。 */
    public static final float DAMAGE_NUMBER_SPREAD = 0.5f;
    /** 文字の大きさ（バニラの名札 = 0.025）。 */
    public static final float DAMAGE_NUMBER_SCALE = 0.03f;
    /** クリティカル時の文字の大きさ（倍率）。 */
    public static final float DAMAGE_NUMBER_CRIT_SCALE = 1.5f;
    /** 出現直後に少し大きく表示する演出の倍率と時間（割合）。 */
    public static final float DAMAGE_NUMBER_POP_SCALE = 1.4f;
    public static final float DAMAGE_NUMBER_POP_TIME = 0.12f;
    /** 1 体あたりに同時に表示する数字の最大数（古いものから消える）。 */
    public static final int DAMAGE_NUMBER_MAX_PER_ENTITY = 24;
    public static final int DAMAGE_NUMBER_COLOR_NORMAL = 0xFFFFFFFF;  // 通常（白）
    public static final int DAMAGE_NUMBER_COLOR_CRIT = 0xFFFFC830;    // クリティカル（金）
    public static final int DAMAGE_NUMBER_COLOR_MAGIC = 0xFF6EC8FF;   // 魔法（水色）
    public static final int DAMAGE_NUMBER_COLOR_OTHER = 0xFFB0B0B0;   // 攻撃者なし（炎・毒など。灰）
    public static final int DAMAGE_NUMBER_COLOR_BREAK = 0xFFFF5050;   // HP を削り切った一撃（赤）

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
