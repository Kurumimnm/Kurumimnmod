package io.github.kurumimnm.kurumimnmod.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * クライアント側だけで使う設定（表示まわり）。
 * <p>
 * プレイヤーごとに好みで変えてよい値（HUD の表示設定など）はここに置く。
 * 出力ファイル: config/kurumimnmod-client.toml
 */
public final class ModClientConfigs {

    private ModClientConfigs() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ---- enemyHealthBar セクション（敵の頭上 HP バー） ----
    static {
        BUILDER.push("enemyHealthBar");
    }

    /** 敵の頭上 HP バーを表示するか。 */
    public static final ModConfigSpec.BooleanValue ENEMY_BAR_ENABLED = BUILDER
            .comment("Whether to show health bars above enemies.")
            .define("enabled", true);

    /** 何ブロック以内の敵に HP バーを表示するか。 */
    public static final ModConfigSpec.DoubleValue ENEMY_BAR_RANGE = BUILDER
            .comment("Maximum distance (in blocks) at which enemy health bars are shown.")
            .defineInRange("range", 16.0, 1.0, 64.0);

    /** true: 敵対モブだけ / false: プレイヤー以外のすべての生き物に表示。 */
    public static final ModConfigSpec.BooleanValue ENEMY_BAR_HOSTILE_ONLY = BUILDER
            .comment("If true, only hostile mobs show a health bar. If false, all living entities except players do.")
            .define("hostileOnly", true);

    static {
        BUILDER.pop();
    }

    // ---- damageNumbers セクション（カカシの頭上に出るダメージ数字） ----
    static {
        BUILDER.push("damageNumbers");
    }

    /** カカシに与えたダメージを数字で表示するか。 */
    public static final ModConfigSpec.BooleanValue SHOW_DAMAGE_NUMBERS = BUILDER
            .comment("Whether to show floating damage numbers above scarecrows.")
            .define("showDamageNumbers", true);

    /** 数字が消えるまでの時間（秒）。 */
    public static final ModConfigSpec.DoubleValue DAMAGE_NUMBER_DURATION = BUILDER
            .comment("How long (seconds) each damage number stays on screen.")
            .defineInRange("damageNumberDuration", 1.2, 0.2, 10.0);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** クライアントのエントリーポイントから呼び出して登録する。 */
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC);
    }
}
