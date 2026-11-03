package io.github.kurumimnm.kurumimnmod.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * ゲーム中に変更・調整できる設定値（Config）をまとめるクラス。
 * <p>
 * 【使い分け】
 * <ul>
 *   <li>スポーン確率・ダメージ倍率など「ワールド/サーバーごとに調整したい実行時の値」 → このクラス（SYNCED）</li>
 *   <li>耐久値・攻撃力など「アイテム登録時に確定する値」 → {@link ModConstants}
 *       （Config はレジストリ登録より後に読み込まれるため、登録時の値には使えない）</li>
 * </ul>
 * 【NeoForge 26.x の Config 種別】
 * LOCAL（旧 COMMON）/ CLIENT / SYNCED（旧 SERVER）/ STARTUP
 * SYNCED はサーバー起動時に読み込まれ、接続したクライアントへ同期される。
 * 出力ファイル: config/kurumimnmod-synced.toml
 */
public final class ModConfigs {

    private ModConfigs() {}

    // =====================================================================
    // SYNCED（ゲームプレイ関連・サーバー → クライアント同期）
    // =====================================================================
    private static final ModConfigSpec.Builder SYNCED_BUILDER = new ModConfigSpec.Builder();

    // ---- general セクション ----
    static {
        SYNCED_BUILDER.push("general");
    }

    /** デバッグ用の詳細ログを出すかどうか。 */
    public static final ModConfigSpec.BooleanValue DEBUG_LOGGING = SYNCED_BUILDER
            .comment("Whether to output verbose debug logs for Kurumimnmod.")
            .define("debugLogging", false);

    static {
        SYNCED_BUILDER.pop();
    }

    // ---- player セクション（プレイヤーの仕様） ----
    static {
        SYNCED_BUILDER.push("player");
    }

    /**
     * 空腹システムを無効化するか。
     * true のとき満腹度は常に最大で固定され、空腹による自然回復・餓死も発生しない。
     */
    public static final ModConfigSpec.BooleanValue DISABLE_HUNGER = SYNCED_BUILDER
            .comment("If true, the hunger system is disabled: food level stays full,",
                    "and there is no natural regeneration or starvation from hunger.")
            .define("disableHunger", true);

    static {
        SYNCED_BUILDER.pop();
    }

    /** Config がまだ読み込まれていない（ワールド起動前など）ときはデフォルト値を返す安全な取得。 */
    public static boolean isHungerDisabled() {
        return SYNCED_SPEC.isLoaded() ? DISABLE_HUNGER.get() : DISABLE_HUNGER.getDefault();
    }

    // ---- level セクション（レベル・経験値） ----
    static {
        SYNCED_BUILDER.push("level");
    }

    /** バニラの経験値（オーブ）を無効化するか。 */
    public static final ModConfigSpec.BooleanValue DISABLE_VANILLA_EXPERIENCE = SYNCED_BUILDER
            .comment("If true, vanilla experience orbs are never spawned (mobs, mining, smelting, fishing, breeding, trading, etc.).")
            .define("disableVanillaExperience", true);

    /** Lv1 → 2 に必要な経験値。必要経験値(Lv) = baseExp + growth × (Lv - 1) ^ exponent */
    public static final ModConfigSpec.DoubleValue LEVEL_BASE_EXP = SYNCED_BUILDER
            .comment("Exp required to go from level 1 to 2.",
                    "Required exp to level up = baseExp + growth * (level - 1) ^ exponent.")
            .defineInRange("baseExp", 12.0, 1.0, 1_000_000.0);

    /** 1 レベルごとの必要経験値の増え方の大きさ。 */
    public static final ModConfigSpec.DoubleValue LEVEL_GROWTH = SYNCED_BUILDER
            .comment("How much the required exp grows per level (multiplier of (level - 1) ^ exponent).")
            .defineInRange("growth", 4.0, 0.0, 1_000_000.0);

    /** 必要経験値の伸び方（1.0 = 一定ペース、大きいほど高レベルで上がりにくくなる）。 */
    public static final ModConfigSpec.DoubleValue LEVEL_EXPONENT = SYNCED_BUILDER
            .comment("Curve of the required exp growth. 1.0 = linear, larger values make high levels harder to reach.")
            .defineInRange("exponent", 1.5, 0.0, 5.0);

    /** 最大レベル（0 = 上限なし）。 */
    public static final ModConfigSpec.IntValue MAX_LEVEL = SYNCED_BUILDER
            .comment("Maximum level. 0 = no limit.")
            .defineInRange("maxLevel", 0, 0, 1_000_000);

    /** キルしたプレイヤーがもらえる経験値の倍率。 */
    public static final ModConfigSpec.DoubleValue KILLER_EXP_RATIO = SYNCED_BUILDER
            .comment("Multiplier of the mob's exp given to the player who killed it.")
            .defineInRange("killerExpRatio", 1.0, 0.0, 100.0);

    /** アシストしたプレイヤーがもらえる経験値の倍率。 */
    public static final ModConfigSpec.DoubleValue ASSIST_EXP_RATIO = SYNCED_BUILDER
            .comment("Multiplier of the mob's exp given to each assisting player",
                    "(damaged / debuffed the mob, or buffed a player who damaged it).")
            .defineInRange("assistExpRatio", 0.5, 0.0, 100.0);

    /** ボスのアシストでもらえる経験値の倍率。 */
    public static final ModConfigSpec.DoubleValue BOSS_ASSIST_EXP_RATIO = SYNCED_BUILDER
            .comment("Multiplier of a boss's exp given to each assisting player.",
                    "For bosses, any contribution before the boss dies counts (no time limit).")
            .defineInRange("bossAssistExpRatio", 1.0, 0.0, 100.0);

    /** アシストが有効な時間（秒）。関わるたびに数え直し。ボスには適用しない。 */
    public static final ModConfigSpec.IntValue ASSIST_WINDOW_SECONDS = SYNCED_BUILDER
            .comment("How long (seconds) a contribution counts as an assist. Refreshed on every new contribution.",
                    "Not applied to bosses.")
            .defineInRange("assistWindowSeconds", 60, 1, 3600);

    /** 経験値獲得時にアクションバーへ「+100 EXP」を表示するか。 */
    public static final ModConfigSpec.BooleanValue SHOW_EXP_MESSAGES = SYNCED_BUILDER
            .comment("Whether to show \"+N EXP\" on the action bar when gaining exp from mobs.")
            .define("showExpMessages", true);

    static {
        SYNCED_BUILDER.pop();
    }

    // ---- 安全な取得（読み込み前はデフォルト値） ----
    private static <T> T value(ModConfigSpec.ConfigValue<T> config) {
        return SYNCED_SPEC.isLoaded() ? config.get() : config.getDefault();
    }

    public static boolean isDebugLogging() { return value(DEBUG_LOGGING); }
    public static boolean isVanillaExperienceDisabled() { return value(DISABLE_VANILLA_EXPERIENCE); }
    public static double levelBaseExp() { return value(LEVEL_BASE_EXP); }
    public static double levelGrowth() { return value(LEVEL_GROWTH); }
    public static double levelExponent() { return value(LEVEL_EXPONENT); }
    public static int maxLevel() { return value(MAX_LEVEL); }
    public static double killerExpRatio() { return value(KILLER_EXP_RATIO); }
    public static double assistExpRatio() { return value(ASSIST_EXP_RATIO); }
    public static double bossAssistExpRatio() { return value(BOSS_ASSIST_EXP_RATIO); }
    public static int assistWindowSeconds() { return value(ASSIST_WINDOW_SECONDS); }
    public static boolean showExpMessages() { return value(SHOW_EXP_MESSAGES); }

    // ※ 機能追加時はここにセクション（例: "mobs", "items"）を追加していく。

    public static final ModConfigSpec SYNCED_SPEC = SYNCED_BUILDER.build();

    /** メインクラスから呼び出して Config を FML に登録する。 */
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SYNCED, SYNCED_SPEC);
    }
}
