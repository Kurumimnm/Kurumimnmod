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
    /**
     * 設定用ステッキでカカシを狙える距離（ブロック）。
     * 通常の攻撃の届く距離に関係なく、クロスヘアが合っていればこの距離まで設定画面を開ける
     * （実際には描画距離内＝クライアントに見えているカカシのみ）。
     */
    public static final double SCARECROW_CONFIG_PICK_RANGE = 256.0;
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

    // =====================================================================
    // ワールド：ゲート（赤い壁）
    // =====================================================================
    /** 1 tick でこれ以上（ブロック）動いたら「テレポート」とみなす（OP のテレポートは壁を通す）。 */
    public static final double GATE_TELEPORT_DISTANCE = 8.0;
    /** 壁を描く範囲（プレイヤーからの水平距離、ブロック）。この距離から見え始める。 */
    public static final int GATE_RENDER_RADIUS = 64;
    /** この割合の距離（0〜1）より遠いと、だんだん薄くなる。手前ではくっきり見える。 */
    public static final float GATE_FADE_START = 0.6f;
    /** プレイヤーがこのブロック数だけ動いたら、描く壁を計算し直す（裏で計算する）。 */
    public static final int GATE_RECOMPUTE_STEP = 4;
    /** 壁の高さ：地面（または目線の低いほう）から下へ・上へ（ブロック）。上の端はぼかす。 */
    public static final float GATE_RENDER_BELOW = 8f;
    public static final float GATE_RENDER_ABOVE = 56f;
    /** 壁の色（ARGB）。 */
    public static final int GATE_COLOR = 0xFFFF2A2A;
    /** 膜のいちばん濃いところの不透明度（0〜1）。 */
    public static final float GATE_MAX_ALPHA = 0.45f;
    /** 横の光る線：間隔（ブロック）・太さ（ブロック）・上へ流れる速さ（1 秒あたりブロック）・不透明度。 */
    public static final float GATE_LINE_SPACING = 4f;
    public static final float GATE_LINE_WIDTH = 0.18f;
    public static final float GATE_LINE_SPEED = 1.2f;
    public static final float GATE_LINE_ALPHA = 0.8f;
    /** モブ・アイテムの壁は、狙っているプレイヤー（いなければこの距離以内のいちばん近いプレイヤー）から見た壁で決める。 */
    public static final double GATE_MOB_PLAYER_RANGE = 128.0;
    /** モブの狙いが壁の向こうのプレイヤーになっていないか調べる間隔（tick）。 */
    public static final int GATE_TARGET_CHECK_TICKS = 10;
    /** 壁の内部キャッシュ：覚えておく最大チャンク数（溢れたら作り直す）。 */
    public static final int REGION_CACHE_MAX_CHUNKS = 16384;

    // =====================================================================
    // ワールド：場所の名前の表示
    // =====================================================================
    /** 場所が変わったときの大きな表示：フェードイン・表示・フェードアウトの時間（ミリ秒）。 */
    public static final int LOCATION_TITLE_FADE_IN_MS = 400;
    public static final int LOCATION_TITLE_STAY_MS = 2200;
    public static final int LOCATION_TITLE_FADE_OUT_MS = 800;
    /** 大きな表示の文字の倍率（初めて来た場所は NEW_SCALE）。 */
    public static final float LOCATION_TITLE_SCALE = 2.0f;
    public static final float LOCATION_TITLE_NEW_SCALE = 2.6f;
    /** 大きな表示の上端の位置（画面の高さに対する割合）。 */
    public static final float LOCATION_TITLE_Y = 0.22f;
    /** 同じ境目を行き来したときに表示が連続しないよう、エリアの表示を出さない間隔（ミリ秒）。 */
    public static final int LOCATION_TITLE_COOLDOWN_MS = 3000;
    /** 左上の常時表示の位置（ピクセル）。 */
    public static final int LOCATION_HUD_X = 4;
    public static final int LOCATION_HUD_Y = 4;
    public static final int LOCATION_COLOR_NAME = 0xFFFFFFFF;
    public static final int LOCATION_COLOR_SUB = 0xFFC8C8C8;
    public static final int LOCATION_COLOR_NEW = 0xFFFFD94A;
    public static final int LOCATION_COLOR_BACKGROUND = 0x60000000;

    // =====================================================================
    // ワールド：地図画面（M キー）
    // =====================================================================
    /** 地図の画像の 1 ピクセルが何ブロックか（小さいほど細かいが、作るのに時間がかかる）。 */
    public static final int MAP_BLOCKS_PER_PIXEL = 16;
    /** 地図の外側の余白（ブロック）。 */
    public static final int MAP_MARGIN = 1200;
    /** 拡大率の範囲（画面の 1 ピクセルあたり地図の何ピクセルか）と、ホイール 1 回あたりの倍率。 */
    public static final float MAP_MIN_ZOOM = 0.25f;
    public static final float MAP_MAX_ZOOM = 8f;
    public static final float MAP_ZOOM_STEP = 1.25f;
    /** まだ行っていないエリアの色。 */
    public static final int MAP_COLOR_UNEXPLORED = 0xFF1B1B20;
    /** エリアの境目の線の色。 */
    public static final int MAP_COLOR_AREA_LINE = 0xFFFFFFFF;
    /** 山脈の色（低いところ → 高いところ）。 */
    public static final int MAP_COLOR_MOUNTAIN_LOW = 0xFF7A766E;
    public static final int MAP_COLOR_MOUNTAIN_HIGH = 0xFFF2F2F2;
    /** 街の印の色。 */
    public static final int MAP_COLOR_TOWN = 0xFF1D9E75;
    /** プレイヤーの印の色。 */
    public static final int MAP_COLOR_PLAYER = 0xFFFF4040;
}
