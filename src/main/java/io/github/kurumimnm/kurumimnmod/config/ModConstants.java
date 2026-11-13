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

    // ---- ガロウ（4 つ足の狼。敵対。スキル：噛みつき。ダメージ・距離・CT は Config [mobs.garou]） ----
    public static final double GAROU_MAX_HEALTH = 20.0;
    public static final double GAROU_ATTACK_DAMAGE = 10.0;
    public static final double GAROU_MOVEMENT_SPEED = 0.3;      // オオカミ 0.3
    public static final double GAROU_FOLLOW_RANGE = 32.0;
    public static final double GAROU_CHASE_SPEED_MODIFIER = 1.2;
    public static final double GAROU_WANDER_SPEED_MODIFIER = 0.8;
    public static final float GAROU_WIDTH = 0.9f;
    public static final float GAROU_HEIGHT = 1.25f;
    public static final float GAROU_EYE_HEIGHT = 1.0f;
    public static final int GAROU_TRACKING_RANGE = 10;
    public static final float GAROU_SHADOW_RADIUS = 0.6f;
    public static final float GAROU_HEAD_MAX_YAW = 45f;
    public static final float GAROU_HEAD_MAX_PITCH = 30f;
    public static final float GAROU_VOICE_PITCH = 0.6f;
    /** 噛みつきを使う、相手との距離（ブロック）。近すぎず遠すぎない所で使う。 */
    public static final double GAROU_BITE_MIN_RANGE = 2.5;
    public static final double GAROU_BITE_MAX_RANGE = 8.0;
    /** 噛みつき 1 回の突進の長さ（tick）と、1 回目と 2 回目の間・終わったあとのすき（tick）。 */
    public static final int GAROU_BITE_LUNGE_TICKS = 6;
    public static final int GAROU_BITE_PAUSE_TICKS = 5;
    public static final int GAROU_BITE_RECOVER_TICKS = 12;
    /** 噛みつきの当たり判定：体の当たり判定を前へ・横へ広げる量（ブロック）。 */
    public static final double GAROU_BITE_HIT_REACH = 0.8;

    // ---- コボルト（犬頭の小柄な人型。敵対。スキル：飛びつき。ダメージ・CT は Config [mobs.kobold]） ----
    public static final double KOBOLD_MAX_HEALTH = 20.0;
    public static final double KOBOLD_ATTACK_DAMAGE = 5.0;
    public static final double KOBOLD_MOVEMENT_SPEED = 0.26;
    public static final double KOBOLD_FOLLOW_RANGE = 28.0;
    public static final double KOBOLD_CHASE_SPEED_MODIFIER = 1.1;
    public static final double KOBOLD_WANDER_SPEED_MODIFIER = 0.8;
    public static final float KOBOLD_WIDTH = 0.6f;
    public static final float KOBOLD_HEIGHT = 1.65f;
    public static final float KOBOLD_EYE_HEIGHT = 1.4f;
    public static final int KOBOLD_TRACKING_RANGE = 8;
    public static final float KOBOLD_SHADOW_RADIUS = 0.45f;
    public static final float KOBOLD_HEAD_MAX_YAW = 60f;
    public static final float KOBOLD_HEAD_MAX_PITCH = 40f;
    public static final float KOBOLD_VOICE_PITCH = 1.25f;
    /** 飛びつきを使う、相手との距離（ブロック）。離れた相手との距離を詰めるスキル。 */
    public static final double KOBOLD_LEAP_MIN_RANGE = 4.0;
    public static final double KOBOLD_LEAP_MAX_RANGE = 10.0;
    /** 飛びつきの上向きの速さと、空中にいる時間の見込み（tick。横の速さの計算に使う）。 */
    public static final double KOBOLD_LEAP_UP_SPEED = 0.55;
    public static final int KOBOLD_LEAP_FLIGHT_TICKS = 14;
    /** 相手の手前で着地するように、距離からこれだけ引く（ブロック）。 */
    public static final double KOBOLD_LEAP_STOP_SHORT = 1.0;
    /** 飛びついてから地面に着くまでの最大の時間（tick。安全装置）と、着地後のすき（tick）。 */
    public static final int KOBOLD_LEAP_MAX_TICKS = 40;
    public static final int KOBOLD_LEAP_RECOVER_TICKS = 8;
    /** 飛びつきの当たり判定：体の当たり判定を広げる量（ブロック）。 */
    public static final double KOBOLD_LEAP_HIT_REACH = 0.4;

    // ---- コボルトロード（ボス。スキル：壁蹴り突撃、HP 3 割で二刀流。ダメージ・CT は Config [mobs.koboldLord]） ----
    public static final double KOBOLD_LORD_MAX_HEALTH = 50.0;
    public static final double KOBOLD_LORD_ATTACK_DAMAGE = 7.0;
    public static final double KOBOLD_LORD_MOVEMENT_SPEED = 0.27;
    public static final double KOBOLD_LORD_FOLLOW_RANGE = 40.0;
    public static final double KOBOLD_LORD_KNOCKBACK_RESISTANCE = 0.6;
    public static final double KOBOLD_LORD_CHASE_SPEED_MODIFIER = 1.1;
    public static final double KOBOLD_LORD_WANDER_SPEED_MODIFIER = 0.7;
    /**
     * 大きさの倍率（バニラの属性 scale の基本値）。モデルの見た目・当たり判定・目の高さがまとめてこの倍率になる。
     * 1.6 で 高さ約 3.8 ブロック・幅 1.6 ブロック（アイアンゴーレム 2.7、ウォーデン 2.9）。
     */
    public static final double KOBOLD_LORD_SCALE = 1.6;
    /** 段差を自動で上がれる高さ（ブロック。大きいので 1 ブロックの段差で引っかからないように）。 */
    public static final double KOBOLD_LORD_STEP_HEIGHT = 1.0;
    /** 当たり判定（ブロック。倍率 KOBOLD_LORD_SCALE をかける前）。モデルは角の先まで約 2.4 ブロック、肩当てまで含めた幅は約 1.4 ブロック。 */
    public static final float KOBOLD_LORD_WIDTH = 1.0f;
    public static final float KOBOLD_LORD_HEIGHT = 2.4f;
    public static final float KOBOLD_LORD_EYE_HEIGHT = 1.9f;
    public static final int KOBOLD_LORD_TRACKING_RANGE = 10;
    public static final float KOBOLD_LORD_SHADOW_RADIUS = 0.85f;
    public static final float KOBOLD_LORD_HEAD_MAX_YAW = 50f;
    public static final float KOBOLD_LORD_HEAD_MAX_PITCH = 35f;
    public static final float KOBOLD_LORD_VOICE_PITCH = 0.8f;
    /** 壁蹴り突撃を使う、相手との距離（ブロック）。 */
    public static final double KOBOLD_LORD_KICK_MIN_RANGE = 4.0;
    public static final double KOBOLD_LORD_KICK_MAX_RANGE = 24.0;
    /** 柱に飛びつく高さ：柱のてっぺんからこれだけ下（ブロック。体が大きいので天井にめり込まない高さ）。 */
    public static final double KOBOLD_LORD_CLING_BELOW_TOP = 5.0;
    /** 柱が近くにないとき（クエストの外など）は、その場で真上にこれだけ跳んでから突撃する（ブロック）。 */
    public static final double KOBOLD_LORD_FALLBACK_JUMP_HEIGHT = 6.0;
    /** 各段階の長さ（tick）：柱へ跳ぶ・柱にしがみつく（この間に着地点の範囲が出る）・突撃・着地後のすき。 */
    public static final int KOBOLD_LORD_JUMP_TICKS = 14;
    public static final int KOBOLD_LORD_CLING_TICKS = 16;
    public static final int KOBOLD_LORD_DIVE_TICKS = 8;
    public static final int KOBOLD_LORD_RECOVER_TICKS = 24;
    /** 柱へ跳ぶときの弧の高さ（ブロック。まっすぐより少し上を通る）。 */
    public static final double KOBOLD_LORD_JUMP_ARC = 2.0;
    /** 突撃の当たり判定：着地点からの縦の幅（ブロック。横は Config の半径）。 */
    public static final double KOBOLD_LORD_KICK_HIT_HEIGHT = 2.5;
    /** 二刀流：2 撃目までの間（tick）と、移動速度の上がり方（割合）。 */
    public static final int KOBOLD_LORD_DUAL_SECOND_HIT_DELAY = 6;
    public static final double KOBOLD_LORD_DUAL_SPEED_BONUS = 0.2;

    // ---- コボルトロード：ボスバー ----
    /** ボスバーを表示する距離（ブロック）。近くの別のクエスト区画にいるプレイヤーには出さない。 */
    public static final double KOBOLD_LORD_BOSS_BAR_RANGE = 40.0;
    /** ボスバーを見せるプレイヤーを更新する間隔（tick）。 */
    public static final int KOBOLD_LORD_BOSS_BAR_UPDATE_INTERVAL = 10;

    // ---- コボルトロード：第 2 形態への移行（二刀流＋雄たけび。範囲・強さは Config） ----
    /** 移行の演出の長さ（tick。アニメ draw = 1.5 秒）。この間は動かない。 */
    public static final int KOBOLD_LORD_PHASE_SHIFT_TICKS = 30;
    /** 移行が始まってから雄たけびまで（tick。なたを抜き終わったところ）。 */
    public static final int KOBOLD_LORD_ROAR_DELAY_TICKS = 12;
    /** 雄たけび・衝撃波で吹き飛ばすときの上向きの速さ。 */
    public static final double KOBOLD_LORD_KNOCKBACK_LIFT = 0.45;
    /** 第 2 形態の間、口から煙を出す間隔（tick。クライアント）。 */
    public static final int KOBOLD_LORD_RAGE_SMOKE_INTERVAL = 3;

    // ---- コボルトロード：攻撃の演出（パーティクルの数など。見た目だけ） ----
    /** 円形のパーティクル（衝撃波・雄たけび）の密度（円周 1 ブロックあたりの数）。 */
    public static final double KOBOLD_LORD_RING_PARTICLES_PER_BLOCK = 3.0;
    /** 着地の衝撃波：広がる輪の数と、輪どうしの間隔（tick）。 */
    public static final int KOBOLD_LORD_SHOCKWAVE_RINGS = 3;
    public static final int KOBOLD_LORD_SHOCKWAVE_RING_INTERVAL = 2;
    /** 着地点に飛び散るブロックの破片の数。 */
    public static final int KOBOLD_LORD_LANDING_DEBRIS = 40;
    /** 突撃中の軌跡（1 tick あたりの煙の数）。 */
    public static final int KOBOLD_LORD_DIVE_TRAIL_PARTICLES = 4;
    /** 跳び上がるとき・柱にしがみついたときに飛び散る破片の数。 */
    public static final int KOBOLD_LORD_LEAP_DEBRIS = 20;
    /** 通常攻撃が当たったときの火花（クリティカルのパーティクル）の数。 */
    public static final int KOBOLD_LORD_HIT_SPARKS = 10;

    // ---- アストラ・ヴェイル（星幽の巡界者。フィールドにランダムエンカウントする超大型ボス） ----
    // ※ 今は見た目のみ。攻撃・遭遇（3 分で帰る）・空の演出は未実装。HP などは Mod 独自ステータスが決まったら差し替える
    /** 最大 HP（仮）。 */
    public static final double ASTRA_VEIL_MAX_HEALTH = 1000.0;
    /** ノックバック耐性（1.0 = まったく押されない）。 */
    public static final double ASTRA_VEIL_KNOCKBACK_RESISTANCE = 1.0;
    /** 描画の倍率。モデル（geo.json）は約 1/10 の大きさで作ってあり、ここで拡大する（翼幅 約 100 ブロック）。 */
    public static final float ASTRA_VEIL_RENDER_SCALE = 10.0f;
    /**
     * 当たり判定（ブロック）。巨体全体ではなく「顔」のまわりだけ（エンティティの位置 = 顔の下端）。
     * 幕・触腕は見た目だけで当たり判定はない（部位ごとの判定は攻撃を作るときに検討）。
     */
    public static final float ASTRA_VEIL_WIDTH = 10.0f;
    public static final float ASTRA_VEIL_HEIGHT = 24.0f;
    public static final float ASTRA_VEIL_EYE_HEIGHT = 16.0f;
    /** クライアントへ同期する距離（チャンク）。遠くからでも見えるように大きめ。 */
    public static final int ASTRA_VEIL_TRACKING_RANGE = 16;
    /** 描画する最大距離（ブロック）。巨体なのでバニラの判定より遠くまで描く。 */
    public static final double ASTRA_VEIL_RENDER_DISTANCE = 256.0;
    /** 描画の判定に使う範囲（ブロック。エンティティの位置からの横・下・上）。翼・触腕の先まで含める。 */
    public static final double ASTRA_VEIL_CULL_HALF_WIDTH = 72.0;
    public static final double ASTRA_VEIL_CULL_BELOW = 64.0;
    public static final double ASTRA_VEIL_CULL_ABOVE = 56.0;
    /** スポーンエッグ・/summon で出したとき、置いた場所からこれだけ上に浮かべる（ブロック。触腕の先が地面に届くくらい）。 */
    public static final double ASTRA_VEIL_SPAWN_HOVER_HEIGHT = 60.0;

    // ---- ボスの着地点の範囲表示（クライアント） ----
    public static final int BOSS_AOE_COLOR_FILL_CENTER = 0x80FF3020;
    public static final int BOSS_AOE_COLOR_FILL_EDGE = 0x30FF3020;
    public static final int BOSS_AOE_COLOR_RING = 0xE0FF4030;

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
    /** 初期レベル（新規プレイヤーは Lv0 から始まる）。 */
    public static final int INITIAL_LEVEL = 0;

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
    /** ボス部屋の柱（クエストの JSON の terrain.pillars / pillar_height）。本数・高さの上限と、高さの初期値（ブロック）。 */
    public static final int QUEST_PILLAR_MAX_COUNT = 16;
    public static final int QUEST_PILLAR_MAX_HEIGHT = 32;
    public static final int QUEST_PILLAR_DEFAULT_HEIGHT = 10;
    /** 神殿（terrain.style = temple）の広間の大きさの初期値（内側。幅 = 東西、奥行き = 南北）。 */
    public static final int QUEST_TEMPLE_DEFAULT_WIDTH = 40;
    public static final int QUEST_TEMPLE_DEFAULT_LENGTH = 64;
    /** 神殿の壁の厚さ（ブロック）。 */
    public static final int QUEST_TEMPLE_WALL_THICKNESS = 2;
    /** 神殿の柱の列を壁からどれだけ内側に立てるか（柱の中心まで、ブロック）。 */
    public static final int QUEST_TEMPLE_PILLAR_INSET = 7;
    /** 神殿の入口・ゲートを南北の壁からどれだけ内側に置くか（ブロック）。 */
    public static final int QUEST_TEMPLE_ENTRANCE_INSET = 6;
    public static final int QUEST_TEMPLE_GATE_INSET = 8;
    /** 神殿の明かり（見えない光源ブロック）を置く間隔と、床からの高さ（ブロック）。 */
    public static final int QUEST_TEMPLE_LIGHT_SPACING = 7;
    public static final int QUEST_TEMPLE_LIGHT_HEIGHT = 3;
    /** 柱の太さ（中心からの半分の幅。1 なら 3×3）。 */
    public static final int QUEST_PILLAR_HALF_WIDTH = 1;
    /** 柱を立てる円の半径（闘技場の半径に対する割合）と、1 本目の向き（度。0 = 東、45 = 南東）。 */
    public static final double QUEST_PILLAR_RING_RATIO = 0.55;
    public static final double QUEST_PILLAR_START_ANGLE = 45;

    // ---- 決まった数を置くモブ（クエストの JSON の placed） ----
    /** placed の 1 項目の数の上限。 */
    public static final int QUEST_PLACED_MAX_PER_ENTRY = 200;
    /** 置く処理の間隔（tick）と、1 回に置く最大の数（読み込まれたチャンクにだけ置き、残りは次の回へ）。 */
    public static final int QUEST_PLACE_INTERVAL_TICKS = 10;
    public static final int QUEST_PLACE_PER_TICK = 8;
    /** 1 体の場所を探す回数（読み込まれていない・入口に近いなどで外れたら次の回へ回す）。 */
    public static final int QUEST_PLACE_ATTEMPTS = 6;
    /** 入口のまわりのこの半径には置かない（入った瞬間に囲まれないように）。 */
    public static final double QUEST_PLACE_SAFE_RADIUS = 16;
    /** 縁の崖・ボス部屋の壁・柱からこれだけ離して置く（ブロック）。 */
    public static final int QUEST_PLACE_EDGE_MARGIN = 3;

    // ---- フィールドのモブ（クエストの JSON の mobs・max_mobs） ----
    /** モブをわかせる間隔（tick）。1 回に 1 体。 */
    public static final int QUEST_SPAWN_INTERVAL_TICKS = 40;
    /** クエスト中のエンダーマンのワープ先：戦える場所の端からこれだけ内側に限る（ブロック）。 */
    public static final double QUEST_ENDER_TELEPORT_MARGIN = 2.0;
    /** クエスト中のクモ：明るくても襲ってくるように足す目標・攻撃の優先度と、追いかける速さの倍率。 */
    public static final int QUEST_SPIDER_TARGET_PRIORITY = 2;
    public static final int QUEST_SPIDER_ATTACK_PRIORITY = 3;
    public static final double QUEST_SPIDER_CHASE_SPEED_MODIFIER = 1.0;
    /** クエスト中の時間の初期値（tick。クエストの JSON の time を省略したとき。6000 = 昼）。 */
    public static final int QUEST_DEFAULT_TIME = 6000;
    /** 天気・クエストの時計を確かめ直す間隔（tick）。 */
    public static final int WORLD_ENVIRONMENT_CHECK_TICKS = 100;
    /** 天気を晴れに固定するとき、晴れの残り時間をこの長さに戻し続ける（tick。0 になると雨が始まるため）。 */
    public static final int WORLD_CLEAR_WEATHER_TICKS = 24000;
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
