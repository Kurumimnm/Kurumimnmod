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


    // ---- グリフォン（中立モブ。普段は地上を歩き、戦闘・落下・水中では飛ぶ） ----
    // ※ 鎧付き個体の出現率・防御力は Config の [mobs.griffin]。ここは登録時の値と AI の調整値。
    /** 当たり判定の幅・高さ（ブロック）。モデル（griffin.geo.json）の胴体に合わせてある。 */
    public static final float GRIFFIN_WIDTH = 1.3f;
    public static final float GRIFFIN_HEIGHT = 1.9f;
    /** 目の高さ（ブロック）。頭の位置に合わせる。 */
    public static final float GRIFFIN_EYE_HEIGHT = 1.75f;
    /** 何チャンク先のプレイヤーまで見えるか。 */
    public static final int GRIFFIN_TRACKING_RANGE = 10;
    /** 影の半径（ブロック）。 */
    public static final float GRIFFIN_SHADOW_RADIUS = 0.8f;

    /** 最大 HP。 */
    public static final double GRIFFIN_MAX_HEALTH = 40.0;
    /** 地上の移動速度（バニラの属性値。ウマ ≒ 0.225、オオカミ = 0.3）。 */
    public static final double GRIFFIN_MOVEMENT_SPEED = 0.25;
    /** 飛行速度（バニラの属性値。ミツバチ = 0.6）。実際の速さは GRIFFIN_AIR_ACCELERATION も影響する。 */
    public static final double GRIFFIN_FLYING_SPEED = 0.6;
    /** 空中での加速の倍率。大きいほど速く飛ぶ（巡航 ≒ 飛行速度² × この値 / 0.09 ブロック/tick）。 */
    public static final float GRIFFIN_AIR_ACCELERATION = 0.1f;
    /** 攻撃力（防具・防御力で軽減される前）。 */
    public static final double GRIFFIN_ATTACK_DAMAGE = 6.0;
    /** 索敵範囲（ブロック）。仲間を呼ぶ範囲（水平）もこの値。 */
    public static final double GRIFFIN_FOLLOW_RANGE = 32.0;
    /** ノックバック耐性（0.0〜1.0）。 */
    public static final double GRIFFIN_KNOCKBACK_RESISTANCE = 0.3;

    /** 怒りが続く時間（秒）の範囲。攻撃した相手をこの時間追いかける。 */
    public static final int GRIFFIN_ANGER_MIN_SECONDS = 20;
    public static final int GRIFFIN_ANGER_MAX_SECONDS = 39;
    /** 攻撃中の移動速度の倍率。 */
    public static final double GRIFFIN_ATTACK_SPEED_MODIFIER = 1.3;
    /** 地上をうろつくときの移動速度の倍率。 */
    public static final double GRIFFIN_WANDER_SPEED_MODIFIER = 0.8;
    /** 空中をうろつくときの移動速度の倍率。 */
    public static final double GRIFFIN_AIR_WANDER_SPEED_MODIFIER = 1.0;
    /** 何もしていないときに気まぐれで飛び立つ確率（毎 tick 1 / この値。1200 で平均 1 分に 1 回）。 */
    public static final int GRIFFIN_IDLE_TAKEOFF_CHANCE = 1200;
    /** 気まぐれで飛ぶ時間（tick）の範囲。終わると着地する。 */
    public static final int GRIFFIN_IDLE_FLIGHT_MIN_TICKS = 200;
    public static final int GRIFFIN_IDLE_FLIGHT_MAX_TICKS = 500;
    /** 飛び立ったあと、最低これだけは飛び続ける（tick）。すぐ着地してバタつくのを防ぐ。 */
    public static final int GRIFFIN_MIN_FLIGHT_TICKS = 40;
    /** 着地しようとしてもこれだけ着地できなければ、羽ばたきをやめて降りる（tick）。 */
    public static final int GRIFFIN_LANDING_TIMEOUT_TICKS = 200;
    /** この距離（ブロック）以上落ちたら翼を広げて飛ぶ（崖から落ちたとき）。落下ダメージは受けない。 */
    public static final double GRIFFIN_FALL_TAKEOFF_DISTANCE = 3.0;
    /** 飛び立つときの上向きの勢い（ブロック/tick）。 */
    public static final double GRIFFIN_TAKEOFF_BOOST = 0.45;
    /** 空中でうろつく範囲（水平・ブロック）。 */
    public static final int GRIFFIN_AIR_WANDER_RANGE = 12;
    /** 空中でうろつく高さ（地面からのブロック数）の範囲。 */
    public static final int GRIFFIN_AIR_WANDER_MIN_HEIGHT = 4;
    public static final int GRIFFIN_AIR_WANDER_MAX_HEIGHT = 10;
    /** 頭がプレイヤーの方を向く角度の上限（度）。左右・上下。 */
    public static final float GRIFFIN_HEAD_MAX_YAW = 50f;
    public static final float GRIFFIN_HEAD_MAX_PITCH = 35f;
    /** 鳴き声の高さ（オウムの声を低くして使う）。 */
    public static final float GRIFFIN_VOICE_PITCH = 0.55f;

    // ---- ミニオン（小鬼。敵対・近接。チュートリアルの雑魚） ----
    public static final double MINION_MAX_HEALTH = 15.0;
    public static final double MINION_ATTACK_DAMAGE = 3.0;
    public static final double MINION_MOVEMENT_SPEED = 0.25;     // ゾンビ 0.23
    public static final double MINION_FOLLOW_RANGE = 24.0;
    /** 追いかけるときの速さの倍率（MeleeAttackGoal）。 */
    public static final double MINION_CHASE_SPEED_MODIFIER = 1.1;
    public static final double MINION_WANDER_SPEED_MODIFIER = 0.8;
    /** 当たり判定（ブロック）。子どもくらいの背丈。 */
    public static final float MINION_WIDTH = 0.6f;
    public static final float MINION_HEIGHT = 1.25f;
    public static final float MINION_EYE_HEIGHT = 1.05f;
    public static final int MINION_TRACKING_RANGE = 8;
    public static final float MINION_SHADOW_RADIUS = 0.4f;
    public static final float MINION_HEAD_MAX_YAW = 60f;
    public static final float MINION_HEAD_MAX_PITCH = 40f;
    /** 鳴き声の高さ（仮：ピグリンの声を高くして使う）。 */
    public static final float MINION_VOICE_PITCH = 1.5f;

    // ---- スライム（MOD 独自。分裂しない。跳ねて近づき体当たり） ----
    public static final double SLIME_MAX_HEALTH = 10.0;
    public static final double SLIME_ATTACK_DAMAGE = 2.0;
    public static final double SLIME_MOVEMENT_SPEED = 0.3;      // 跳んでいる間だけ前へ進む
    public static final double SLIME_FOLLOW_RANGE = 16.0;
    public static final double SLIME_CHASE_SPEED_MODIFIER = 1.0;
    public static final double SLIME_WANDER_SPEED_MODIFIER = 0.8;
    /** 次に跳ぶまでの間（tick）。追いかけている間はこれの 1/3。 */
    public static final int SLIME_JUMP_DELAY_MIN = 10;
    public static final int SLIME_JUMP_DELAY_MAX = 30;
    public static final float SLIME_WIDTH = 0.75f;
    public static final float SLIME_HEIGHT = 0.625f;
    public static final float SLIME_EYE_HEIGHT = 0.4f;
    public static final int SLIME_TRACKING_RANGE = 8;
    public static final float SLIME_SHADOW_RADIUS = 0.4f;

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
    // 現世（Kurumimnmod ワールド）：始まりの街
    // =====================================================================
    /** 平らな地形の地面の一番上の Y（data/kurumimnmod/worldgen/world_preset/field_world.json の層と合わせる）。 */
    public static final int START_TOWN_GROUND_Y = 64;
    /** 初めてワールドに入ったときのスポーン地点（広場の噴水の南）。 */
    public static final int START_TOWN_SPAWN_X = 0;
    public static final int START_TOWN_SPAWN_Z = 6;

    // =====================================================================
    // クエスト：区画（フィールド）
    // =====================================================================
    /** 区画（フィールド 1 つ）の格子の間隔（ブロック）。フィールドの一辺の最大より十分大きくする。 */
    public static final int QUEST_PLOT_SPACING = 4096;
    /** 格子の横 1 列に並べる区画の数（いっぱいになったら次の列へ）。 */
    public static final int QUEST_PLOTS_PER_ROW = 64;
    /** フィールドの一辺の最小・最大（ブロック）。クエストの JSON の size はこの範囲。 */
    public static final int QUEST_FIELD_MIN_SIZE = 48;
    public static final int QUEST_FIELD_MAX_SIZE = 2048;
    /** 縁の崖の幅と高さ（ブロック）。外は奈落で、いちばん外側の 1 列に見えない壁を置く。 */
    public static final int QUEST_RIM_WIDTH = 24;
    public static final double QUEST_RIM_HEIGHT = 36;
    /** 入口・ゲートを縁からどれだけ内側に置くか（ブロック）。 */
    public static final int QUEST_GATE_INSET = 16;
    /** 入口・ゲートのまわりを平らにする半径と、なめらかにつなぐ幅（ブロック）。 */
    public static final double QUEST_FLAT_RADIUS = 7;
    public static final double QUEST_FLAT_BLEND = 14;
    /** ゲートの画面を使える距離（ブロック）。 */
    public static final double QUEST_GATE_USE_RANGE = 8;
    /** ボス部屋：壁から区画の縁までの間と、壁の高さ（ブロック）。 */
    public static final int QUEST_ARENA_MARGIN = 6;
    public static final int QUEST_ARENA_WALL_HEIGHT = 8;

    // ---- フィールドのモブ（クエストの JSON の mobs・max_mobs） ----
    /** モブをわかせる間隔（tick）。1 回に 1 体。 */
    public static final int QUEST_SPAWN_INTERVAL_TICKS = 40;
    /** 数を数える範囲（プレイヤーからの横の距離。この中のモンスターが max_mobs 未満ならわかせる）。 */
    public static final double QUEST_SPAWN_COUNT_RADIUS = 48;
    /** わかせる場所のプレイヤーからの距離。 */
    public static final double QUEST_SPAWN_MIN_DISTANCE = 18;
    public static final double QUEST_SPAWN_MAX_DISTANCE = 36;
    /** 入口のまわりのこの半径にはわかせない。 */
    public static final double QUEST_SPAWN_SAFE_RADIUS = 20;

    // ---- 中間拠点（全クエスト共通。クエスト用ディメンションの (0, 0)） ----
    /** 島の半径・地面の高さ。 */
    public static final int QUEST_HUB_RADIUS = 56;
    public static final int QUEST_HUB_GROUND_Y = 80;
    /** 中間拠点の街の範囲。 */
    public static final int QUEST_HUB_TOWN_RADIUS = 44;

    // ---- クエストの表示（左上） ----
    public static final int QUEST_HUD_X = 4;
    public static final int QUEST_HUD_Y = 4;
    public static final int QUEST_COLOR_TITLE = 0xFFFFD94A;
    public static final int QUEST_COLOR_FIELD = 0xFFFFFFFF;
    public static final int QUEST_COLOR_GOAL = 0xFFC8C8C8;
    public static final int QUEST_COLOR_CLEARED = 0xFF7CFC7C;
    public static final int QUEST_COLOR_BACKGROUND = 0x60000000;
}
