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

    // ※ 機能追加時はここにセクション（例: "mobs", "items"）を追加していく。

    public static final ModConfigSpec SYNCED_SPEC = SYNCED_BUILDER.build();

    /** メインクラスから呼び出して Config を FML に登録する。 */
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SYNCED, SYNCED_SPEC);
    }
}
