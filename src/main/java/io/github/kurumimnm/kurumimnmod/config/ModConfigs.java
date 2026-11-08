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

    // ---- status セクション（ステータス：初期値・振り分け・効果） ----
    static {
        SYNCED_BUILDER.push("status");
    }

    /** 1 レベル上がるごとにもらえるステータスポイント。 */
    public static final ModConfigSpec.IntValue POINTS_PER_LEVEL = SYNCED_BUILDER
            .comment("Stat points gained per level up.")
            .defineInRange("pointsPerLevel", 1, 0, 1000);

    // ---- 初期ステータス ----
    static {
        SYNCED_BUILDER.push("initial");
    }

    public static final ModConfigSpec.IntValue INITIAL_HP = SYNCED_BUILDER
            .comment("Initial max HP (vanilla = 20).")
            .defineInRange("hp", 20, 1, 1_000_000);
    public static final ModConfigSpec.IntValue INITIAL_MP = SYNCED_BUILDER
            .comment("Initial max MP.")
            .defineInRange("mp", 20, 0, 1_000_000);
    public static final ModConfigSpec.IntValue INITIAL_ATTACK = SYNCED_BUILDER
            .comment("Initial attack.")
            .defineInRange("attack", 1, 0, 1_000_000);
    public static final ModConfigSpec.IntValue INITIAL_DEFENSE = SYNCED_BUILDER
            .comment("Initial defense.")
            .defineInRange("defense", 1, 0, 1_000_000);
    public static final ModConfigSpec.IntValue INITIAL_INTELLIGENCE = SYNCED_BUILDER
            .comment("Initial intelligence.")
            .defineInRange("intelligence", 1, 0, 1_000_000);
    public static final ModConfigSpec.IntValue INITIAL_DEXTERITY = SYNCED_BUILDER
            .comment("Initial dexterity.")
            .defineInRange("dexterity", 1, 0, 1_000_000);
    public static final ModConfigSpec.IntValue INITIAL_LUCK = SYNCED_BUILDER
            .comment("Initial luck.")
            .defineInRange("luck", 1, 0, 1_000_000);
    public static final ModConfigSpec.IntValue INITIAL_RESISTANCE = SYNCED_BUILDER
            .comment("Initial resistance.")
            .defineInRange("resistance", 1, 0, 1_000_000);
    public static final ModConfigSpec.DoubleValue INITIAL_ATTACK_SPEED = SYNCED_BUILDER
            .comment("Attack speed multiplier (1.0 = vanilla).")
            .defineInRange("attackSpeed", 1.0, 0.1, 10.0);
    public static final ModConfigSpec.DoubleValue INITIAL_MOVE_SPEED = SYNCED_BUILDER
            .comment("Movement speed multiplier (1.0 = vanilla).")
            .defineInRange("moveSpeed", 1.0, 0.1, 10.0);
    public static final ModConfigSpec.DoubleValue INITIAL_JUMP_POWER = SYNCED_BUILDER
            .comment("Jump strength multiplier (1.0 = vanilla).")
            .defineInRange("jumpPower", 1.0, 0.1, 10.0);

    static {
        SYNCED_BUILDER.pop();
    }

    // ---- 振り分け 1 ポイントあたりの上昇量 ----
    static {
        SYNCED_BUILDER.push("perPoint");
    }

    public static final ModConfigSpec.IntValue VITALITY_HP_PER_POINT = SYNCED_BUILDER
            .comment("Max HP gained per point in Vitality.")
            .defineInRange("vitalityHp", 10, 0, 1_000_000);
    public static final ModConfigSpec.IntValue MAGIC_MP_PER_POINT = SYNCED_BUILDER
            .comment("Max MP gained per point in Magic.")
            .defineInRange("magicMp", 10, 0, 1_000_000);
    public static final ModConfigSpec.IntValue ATTACK_PER_POINT = SYNCED_BUILDER
            .comment("Attack gained per point in Attack.")
            .defineInRange("attack", 1, 0, 1_000_000);
    public static final ModConfigSpec.IntValue DEFENSE_PER_POINT = SYNCED_BUILDER
            .comment("Defense gained per point in Defense.")
            .defineInRange("defense", 1, 0, 1_000_000);
    public static final ModConfigSpec.IntValue INTELLIGENCE_PER_POINT = SYNCED_BUILDER
            .comment("Intelligence gained per point in Intelligence.")
            .defineInRange("intelligence", 1, 0, 1_000_000);

    static {
        SYNCED_BUILDER.pop();
    }

    // ---- 効果（ステータス値 → 実際の数値） ----
    static {
        SYNCED_BUILDER.push("effects");
    }

    /** 攻撃力 1 ごとのダメージ倍率の上昇（0.1 = +10%）。ダメージ = 元のダメージ ×(1 + (攻撃力 - 1) × この値) */
    public static final ModConfigSpec.DoubleValue ATTACK_DAMAGE_BONUS_PER_POINT = SYNCED_BUILDER
            .comment("Damage multiplier added per Attack above 1 (0.1 = +10%).",
                    "Damage = original damage * (1 + (attack - 1) * this).")
            .defineInRange("attackDamageBonus", 0.1, 0.0, 100.0);

    /** 防御力 1 ごとに、敵から受けるダメージを減らす量。軽減量 = (防御力 - 1) × この値 */
    public static final ModConfigSpec.DoubleValue DEFENSE_REDUCTION_PER_POINT = SYNCED_BUILDER
            .comment("Damage from enemies reduced per Defense above 1 (flat).",
                    "Reduction = (defense - 1) * this. Applied after armor.")
            .defineInRange("defenseReduction", 0.5, 0.0, 1000.0);

    /** 防御力で軽減したあとの最低ダメージ（0 = 完全に防げる）。 */
    public static final ModConfigSpec.DoubleValue DEFENSE_MIN_DAMAGE = SYNCED_BUILDER
            .comment("Minimum damage after the Defense reduction (0 = can be fully blocked).")
            .defineInRange("defenseMinDamage", 0.0, 0.0, 1000.0);

    /** 技量 1 のときのクリティカル率（0.05 = 5%）。 */
    public static final ModConfigSpec.DoubleValue CRIT_CHANCE_BASE = SYNCED_BUILDER
            .comment("Critical hit chance at Dexterity 1 (0.05 = 5%).")
            .defineInRange("critChanceBase", 0.05, 0.0, 1.0);
    /** 技量 1 ごとのクリティカル率の上昇。 */
    public static final ModConfigSpec.DoubleValue CRIT_CHANCE_PER_POINT = SYNCED_BUILDER
            .comment("Critical hit chance added per Dexterity above 1 (0.005 = +0.5%).")
            .defineInRange("critChancePerPoint", 0.005, 0.0, 1.0);
    /** 技量 1 のときのクリティカル倍率。 */
    public static final ModConfigSpec.DoubleValue CRIT_DAMAGE_BASE = SYNCED_BUILDER
            .comment("Critical damage multiplier at Dexterity 1 (vanilla = 1.5).")
            .defineInRange("critDamageBase", 1.5, 1.0, 100.0);
    /** 技量 1 ごとのクリティカル倍率の上昇。 */
    public static final ModConfigSpec.DoubleValue CRIT_DAMAGE_PER_POINT = SYNCED_BUILDER
            .comment("Critical damage multiplier added per Dexterity above 1 (0.01 = +1%).")
            .defineInRange("critDamagePerPoint", 0.01, 0.0, 100.0);

    /** ラック 1 ごとにバニラの「幸運」属性に加える量（ラック 1 = 幸運 0）。 */
    public static final ModConfigSpec.DoubleValue LUCK_ATTRIBUTE_PER_POINT = SYNCED_BUILDER
            .comment("Vanilla luck attribute added per Luck above 1 (affects fishing and chest loot).")
            .defineInRange("luckAttributePerPoint", 1.0, 0.0, 1024.0);

    /** MP の自然回復量（1 秒あたり、最大 MP に対する割合。0.01 = 1%）。 */
    public static final ModConfigSpec.DoubleValue MP_REGEN_PER_SECOND = SYNCED_BUILDER
            .comment("MP regenerated per second, as a ratio of max MP (0.01 = 1%).")
            .defineInRange("mpRegenPerSecond", 0.01, 0.0, 1.0);

    /** 知力 1 ごとの魔法ダメージ倍率の上昇（0.1 = +10%）。魔法ダメージ = 元のダメージ ×(1 + (知力 - 1) × この値) */
    public static final ModConfigSpec.DoubleValue INTELLIGENCE_MAGIC_BONUS_PER_POINT = SYNCED_BUILDER
            .comment("Magic damage multiplier added per Intelligence above 1 (0.1 = +10%).",
                    "Magic damage = original damage * (1 + (intelligence - 1) * this).",
                    "Magic damage is NOT affected by Attack.")
            .defineInRange("intelligenceMagicBonus", 0.1, 0.0, 100.0);

    static {
        SYNCED_BUILDER.pop();
        SYNCED_BUILDER.pop(); // status
    }

    // ---- scarecrow セクション（カカシ：召喚直後の初期設定と挙動） ----
    // ※ ここは「召喚した直後の初期値」。召喚後はカカシごとに設定画面（設定用ステッキ）で変更でき、
    //    変更した値はカカシ本体に保存される（Config を変えても既存のカカシには反映されない）。
    static {
        SYNCED_BUILDER.push("scarecrow");
    }

    public static final ModConfigSpec.DoubleValue SCARECROW_MAX_HEALTH = SYNCED_BUILDER
            .comment("Initial max HP of a newly summoned scarecrow (vanilla attribute limit = 1024).")
            .defineInRange("scarecrowMaxHealth", 100.0, ModConstants.SCARECROW_MIN_HEALTH, ModConstants.SCARECROW_MAX_HEALTH_LIMIT);
    public static final ModConfigSpec.BooleanValue SCARECROW_INFINITE_HEALTH = SYNCED_BUILDER
            .comment("If true, newly summoned scarecrows never lose HP (damage is still measured and shown).")
            .define("scarecrowInfiniteHealth", true);
    public static final ModConfigSpec.IntValue SCARECROW_DEFENSE = SYNCED_BUILDER
            .comment("Initial Defense of a scarecrow. Works the same as the player's Defense stat",
                    "(reduction = (defense - 1) * status.effects.defenseReduction).")
            .defineInRange("scarecrowDefense", 1, 0, ModConstants.SCARECROW_DEFENSE_LIMIT);
    public static final ModConfigSpec.DoubleValue SCARECROW_ARMOR = SYNCED_BUILDER
            .comment("Initial armor points of a scarecrow (vanilla armor attribute, max 30).")
            .defineInRange("scarecrowArmor", 0.0, 0.0, ModConstants.SCARECROW_ARMOR_LIMIT);
    public static final ModConfigSpec.DoubleValue SCARECROW_MAGIC_RESISTANCE = SYNCED_BUILDER
            .comment("Initial magic resistance of a scarecrow in percent (magic damage is reduced by this %).")
            .defineInRange("scarecrowMagicResistance", 0.0, 0.0, 100.0);
    public static final ModConfigSpec.DoubleValue SCARECROW_KNOCKBACK_RESISTANCE = SYNCED_BUILDER
            .comment("Initial knockback resistance of a scarecrow in percent (100 = never knocked back).")
            .defineInRange("scarecrowKnockbackResistance", 0.0, 0.0, 100.0);
    public static final ModConfigSpec.BooleanValue SCARECROW_NO_KNOCKBACK = SYNCED_BUILDER
            .comment("If true, newly summoned scarecrows are never knocked back (knockback resistance is ignored).",
                    "(Renamed from scarecrowNoKnockback so that the old default value false left in existing files is not used.)")
            .define("scarecrowKnockbackImmune", true);
    public static final ModConfigSpec.BooleanValue SCARECROW_COUNTER_ENABLED = SYNCED_BUILDER
            .comment("If true, newly summoned scarecrows attack nearby players.")
            .define("scarecrowCounterEnabled", false);
    public static final ModConfigSpec.DoubleValue SCARECROW_COUNTER_DAMAGE = SYNCED_BUILDER
            .comment("Initial damage of the scarecrow's counterattack (before the player's armor / Defense).")
            .defineInRange("scarecrowCounterDamage", 4.0, 0.0, ModConstants.SCARECROW_COUNTER_DAMAGE_LIMIT);
    public static final ModConfigSpec.DoubleValue SCARECROW_COUNTER_INTERVAL = SYNCED_BUILDER
            .comment("Initial interval (seconds) between the scarecrow's counterattacks.")
            .defineInRange("scarecrowCounterInterval", 2.0, ModConstants.SCARECROW_COUNTER_INTERVAL_MIN, ModConstants.SCARECROW_COUNTER_INTERVAL_MAX);
    public static final ModConfigSpec.DoubleValue SCARECROW_COUNTER_RANGE = SYNCED_BUILDER
            .comment("Initial reach (blocks) of the scarecrow's counterattack.")
            .defineInRange("scarecrowCounterRange", 3.0, ModConstants.SCARECROW_COUNTER_RANGE_MIN, ModConstants.SCARECROW_COUNTER_RANGE_MAX);
    public static final ModConfigSpec.DoubleValue SCARECROW_REGEN_DELAY = SYNCED_BUILDER
            .comment("Seconds without taking damage before a scarecrow's HP is fully restored (finite HP only).")
            .defineInRange("scarecrowRegenDelay", 5.0, 0.0, 600.0);
    public static final ModConfigSpec.DoubleValue SCARECROW_RETURN_DELAY = SYNCED_BUILDER
            .comment("Seconds after being knocked back before a scarecrow starts returning to where it was summoned.")
            .defineInRange("scarecrowReturnDelay", 0.75, 0.0, 60.0);
    public static final ModConfigSpec.DoubleValue SCARECROW_RETURN_SPEED = SYNCED_BUILDER
            .comment("Speed (blocks per tick) at which a scarecrow slides back to where it was summoned.")
            .defineInRange("scarecrowReturnSpeed", 0.15, 0.01, 10.0);

    static {
        SYNCED_BUILDER.pop();
    }

    // ---- mobs セクション（MOD の Mob） ----
    static {
        SYNCED_BUILDER.push("mobs");
        SYNCED_BUILDER.push("griffin");
    }

    // ---- グリフォン ----
    /** 出現時に鎧（古い鉄の鎧）を着ている確率（0.0〜1.0）。スポーンエッグ・/summon でも判定する。 */
    public static final ModConfigSpec.DoubleValue GRIFFIN_ARMORED_CHANCE = SYNCED_BUILDER
            .comment("Chance (0.0 - 1.0) that a griffin spawns wearing old iron armor.",
                    "Armored griffins have the same stats except for a higher Defense.")
            .defineInRange("armoredChance", 0.2, 0.0, 1.0);
    /** 鎧なしの個体の防御力（プレイヤーの防御力ステータスと同じ式の固定値軽減。1 = 軽減なし）。 */
    public static final ModConfigSpec.IntValue GRIFFIN_DEFENSE = SYNCED_BUILDER
            .comment("Defense of a normal griffin. Works the same as the player's Defense stat (1 = no reduction).")
            .defineInRange("normalDefense", 1, 1, 1_000_000);
    /** 鎧付きの個体の防御力。 */
    public static final ModConfigSpec.IntValue GRIFFIN_ARMORED_DEFENSE = SYNCED_BUILDER
            .comment("Defense of an armored griffin. Works the same as the player's Defense stat (1 = no reduction).")
            .defineInRange("armoredDefense", 5, 1, 1_000_000);

    static {
        SYNCED_BUILDER.pop(); // griffin
    }

    // ---- スキルを使うモブ共通 ----
    /** スキルの前に頭の上に「！」を出す時間（秒）。この間は動かず、終わるとスキルを出す。 */
    public static final ModConfigSpec.DoubleValue SKILL_TELEGRAPH_SECONDS = SYNCED_BUILDER
            .comment("Seconds a mob shows the \"!\" warning above its head before using a skill.")
            .defineInRange("skillTelegraphSeconds", 0.5, 0.0, 5.0);

    // ---- ガロウ（4 つ足の狼。スキル：噛みつき） ----
    static {
        SYNCED_BUILDER.push("garou");
    }

    /** 噛みつき 1 回のダメージ（2 回噛む）。 */
    public static final ModConfigSpec.DoubleValue GAROU_BITE_DAMAGE = SYNCED_BUILDER
            .comment("Damage of each bite of the Garou's lunging bite skill (it bites twice).")
            .defineInRange("garouBiteDamage", 10.0, 0.0, 1000.0);
    /** 噛みつき 1 回で前へ進む距離（ブロック）。 */
    public static final ModConfigSpec.DoubleValue GAROU_BITE_DISTANCE = SYNCED_BUILDER
            .comment("Blocks the Garou lunges forward with each bite.")
            .defineInRange("garouBiteDistance", 3.0, 0.0, 16.0);
    /** 噛みつきのクールダウン（秒）。 */
    public static final ModConfigSpec.DoubleValue GAROU_BITE_COOLDOWN = SYNCED_BUILDER
            .comment("Cooldown of the lunging bite skill (seconds).")
            .defineInRange("garouBiteCooldown", 10.0, 0.0, 600.0);

    // ---- コボルト（犬頭の小柄な人型。スキル：飛びつき） ----
    static {
        SYNCED_BUILDER.pop(); // garou
        SYNCED_BUILDER.push("kobold");
    }

    /** 飛びつきのダメージ（ふつうの攻撃より弱い。距離を詰めるためのスキル）。 */
    public static final ModConfigSpec.DoubleValue KOBOLD_LEAP_DAMAGE = SYNCED_BUILDER
            .comment("Damage of the Kobold's leap attack (meant to close the distance).")
            .defineInRange("koboldLeapDamage", 2.0, 0.0, 1000.0);
    /** 飛びつきのクールダウン（秒）。 */
    public static final ModConfigSpec.DoubleValue KOBOLD_LEAP_COOLDOWN = SYNCED_BUILDER
            .comment("Cooldown of the leap attack (seconds).")
            .defineInRange("koboldLeapCooldown", 10.0, 0.0, 600.0);

    // ---- コボルトロード（ボス） ----
    static {
        SYNCED_BUILDER.pop(); // kobold
        SYNCED_BUILDER.push("koboldLord");
    }

    /** 壁蹴り突撃のダメージ（着地点の範囲の中にいるプレイヤーに当たる）。 */
    public static final ModConfigSpec.DoubleValue KOBOLD_LORD_KICK_DAMAGE = SYNCED_BUILDER
            .comment("Damage of the Kobold Lord's wall-kick dive (hits players inside the marked landing area).")
            .defineInRange("koboldLordKickDamage", 15.0, 0.0, 1000.0);
    /** 壁蹴り突撃の着地点の範囲（半径・ブロック）。地面に赤い円で表示する。 */
    public static final ModConfigSpec.DoubleValue KOBOLD_LORD_KICK_RADIUS = SYNCED_BUILDER
            .comment("Radius (blocks) of the wall-kick dive's landing area, shown as a red circle on the ground.")
            .defineInRange("koboldLordKickRadius", 2.5, 0.5, 16.0);
    /** 壁蹴り突撃のクールダウン（秒）。 */
    public static final ModConfigSpec.DoubleValue KOBOLD_LORD_KICK_COOLDOWN = SYNCED_BUILDER
            .comment("Cooldown of the wall-kick dive (seconds).")
            .defineInRange("koboldLordKickCooldown", 20.0, 0.0, 600.0);
    /** 二刀流になる HP の割合（最大 HP に対して。0.3 = 3 割以下）。 */
    public static final ModConfigSpec.DoubleValue KOBOLD_LORD_DUAL_HEALTH_RATIO = SYNCED_BUILDER
            .comment("The Kobold Lord draws its second machete when its health falls to this ratio of max health.")
            .defineInRange("koboldLordDualHealthRatio", 0.3, 0.0, 1.0);

    static {
        SYNCED_BUILDER.pop(); // koboldLord
        SYNCED_BUILDER.pop(); // mobs
    }

    // ---- magic セクション（魔法の杖） ----
    static {
        SYNCED_BUILDER.push("magic");
        SYNCED_BUILDER.push("iceWand");
    }

    // ---- 氷柱の杖（頭上から大きな氷柱を 1 本撃ち出す） ----
    /** 1 回の発射に必要な MP。 */
    public static final ModConfigSpec.DoubleValue ICE_WAND_MP_COST = SYNCED_BUILDER
            .comment("MP consumed per cast.")
            .defineInRange("mpCost", 4.0, 0.0, 1_000_000.0);
    /** 着弾地点の中心での最大ダメージ（知力の倍率がかかる前の値）。 */
    public static final ModConfigSpec.DoubleValue ICE_WAND_DAMAGE = SYNCED_BUILDER
            .comment("Damage at the center of the impact (before the Intelligence multiplier).",
                    "Enemies pierced on the way also take this damage.")
            .defineInRange("damage", 7.0, 0.0, 1_000_000.0);
    /** 着弾範囲（サークル）の半径（ブロック）。 */
    public static final ModConfigSpec.DoubleValue ICE_WAND_RADIUS = SYNCED_BUILDER
            .comment("Radius (blocks) of the impact area.")
            .defineInRange("radius", 3.0, 0.1, 32.0);
    /** 範囲の端でのダメージの割合（0.5 = 中心の 50%）。中心から端まで直線的に下がる。 */
    public static final ModConfigSpec.DoubleValue ICE_WAND_EDGE_DAMAGE_RATIO = SYNCED_BUILDER
            .comment("Damage ratio at the edge of the impact area (0.5 = 50% of the center damage).",
                    "Damage decreases linearly from the center to the edge.")
            .defineInRange("edgeDamageRatio", 0.5, 0.0, 1.0);
    /** 狙える最大距離（ブロック）。何もない方向を狙うと、この距離で着弾する。 */
    public static final ModConfigSpec.DoubleValue ICE_WAND_RANGE = SYNCED_BUILDER
            .comment("Max aiming distance (blocks). Aiming at nothing makes the icicle land at this distance.")
            .defineInRange("castRange", 32.0, 1.0, 128.0);
    /** 氷柱の飛ぶ速さ（ブロック / tick）。 */
    public static final ModConfigSpec.DoubleValue ICE_WAND_SPEED = SYNCED_BUILDER
            .comment("Icicle speed (blocks per tick).")
            .defineInRange("projectileSpeed", 1.5, 0.1, 10.0);
    /** 頭のてっぺんから、氷柱が出現する位置までの高さ（ブロック）。 */
    public static final ModConfigSpec.DoubleValue ICE_WAND_SPAWN_HEIGHT = SYNCED_BUILDER
            .comment("Height (blocks) above the player's head where the icicle appears.")
            .defineInRange("spawnHeight", 1.0, 0.0, 8.0);
    /** 発射後のクールダウン（tick。20 = 1 秒）。 */
    public static final ModConfigSpec.IntValue ICE_WAND_COOLDOWN = SYNCED_BUILDER
            .comment("Cooldown after casting (ticks, 20 = 1 second).")
            .defineInRange("cooldownTicks", 10, 0, 1200);

    /** 当たった敵に付ける鈍足の時間（tick。0 = 付けない）。 */
    public static final ModConfigSpec.IntValue ICE_WAND_SLOWNESS_DURATION = SYNCED_BUILDER
            .comment("Duration (ticks) of Slowness applied to enemies hit (pierce and impact). 0 = no Slowness.")
            .defineInRange("slownessDuration", 60, 0, 12000);
    /** 鈍足の強さ（0 = 鈍足 I、1 = 鈍足 II …）。 */
    public static final ModConfigSpec.IntValue ICE_WAND_SLOWNESS_AMPLIFIER = SYNCED_BUILDER
            .comment("Slowness level - 1 (0 = Slowness I, 1 = Slowness II, ...).")
            .defineInRange("slownessAmplifier", 0, 0, 9);

    static {
        SYNCED_BUILDER.pop(); // iceWand
        SYNCED_BUILDER.pop(); // magic
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
    public static int pointsPerLevel() { return value(POINTS_PER_LEVEL); }
    public static double attackDamageBonusPerPoint() { return value(ATTACK_DAMAGE_BONUS_PER_POINT); }
    public static double defenseReductionPerPoint() { return value(DEFENSE_REDUCTION_PER_POINT); }
    public static double defenseMinDamage() { return value(DEFENSE_MIN_DAMAGE); }
    public static double critChanceBase() { return value(CRIT_CHANCE_BASE); }
    public static double critChancePerPoint() { return value(CRIT_CHANCE_PER_POINT); }
    public static double critDamageBase() { return value(CRIT_DAMAGE_BASE); }
    public static double critDamagePerPoint() { return value(CRIT_DAMAGE_PER_POINT); }
    public static double luckAttributePerPoint() { return value(LUCK_ATTRIBUTE_PER_POINT); }
    public static double mpRegenPerSecond() { return value(MP_REGEN_PER_SECOND); }
    public static double scarecrowRegenDelay() { return value(SCARECROW_REGEN_DELAY); }
    public static double scarecrowReturnDelay() { return value(SCARECROW_RETURN_DELAY); }
    public static double scarecrowReturnSpeed() { return value(SCARECROW_RETURN_SPEED); }
    public static double intelligenceMagicBonusPerPoint() { return value(INTELLIGENCE_MAGIC_BONUS_PER_POINT); }
    public static double iceWandMpCost() { return value(ICE_WAND_MP_COST); }
    public static double iceWandDamage() { return value(ICE_WAND_DAMAGE); }
    public static double iceWandRadius() { return value(ICE_WAND_RADIUS); }
    public static double iceWandEdgeDamageRatio() { return value(ICE_WAND_EDGE_DAMAGE_RATIO); }
    public static double iceWandRange() { return value(ICE_WAND_RANGE); }
    public static double iceWandSpeed() { return value(ICE_WAND_SPEED); }
    public static double iceWandSpawnHeight() { return value(ICE_WAND_SPAWN_HEIGHT); }
    public static int iceWandCooldown() { return value(ICE_WAND_COOLDOWN); }
    public static int iceWandSlownessDuration() { return value(ICE_WAND_SLOWNESS_DURATION); }
    public static int iceWandSlownessAmplifier() { return value(ICE_WAND_SLOWNESS_AMPLIFIER); }
    public static double griffinArmoredChance() { return value(GRIFFIN_ARMORED_CHANCE); }
    public static int griffinDefense() { return value(GRIFFIN_DEFENSE); }
    public static int griffinArmoredDefense() { return value(GRIFFIN_ARMORED_DEFENSE); }
    /** 予兆の時間（tick）。 */
    public static int skillTelegraphTicks() { return (int) Math.round(value(SKILL_TELEGRAPH_SECONDS) * 20); }
    public static double garouBiteDamage() { return value(GAROU_BITE_DAMAGE); }
    public static double garouBiteDistance() { return value(GAROU_BITE_DISTANCE); }
    public static int garouBiteCooldownTicks() { return (int) Math.round(value(GAROU_BITE_COOLDOWN) * 20); }
    public static double koboldLeapDamage() { return value(KOBOLD_LEAP_DAMAGE); }
    public static int koboldLeapCooldownTicks() { return (int) Math.round(value(KOBOLD_LEAP_COOLDOWN) * 20); }
    public static double koboldLordKickDamage() { return value(KOBOLD_LORD_KICK_DAMAGE); }
    public static double koboldLordKickRadius() { return value(KOBOLD_LORD_KICK_RADIUS); }
    public static int koboldLordKickCooldownTicks() { return (int) Math.round(value(KOBOLD_LORD_KICK_COOLDOWN) * 20); }
    public static double koboldLordDualHealthRatio() { return value(KOBOLD_LORD_DUAL_HEALTH_RATIO); }

    /** Config 値（int / double）を安全に取得する（StatType 等から使う）。 */
    public static <T> T get(ModConfigSpec.ConfigValue<T> config) { return value(config); }

    // ※ 機能追加時は上にセクション（例: "mobs", "items"）を追加していく。

    public static final ModConfigSpec SYNCED_SPEC = SYNCED_BUILDER.build();

    /** メインクラスから呼び出して Config を FML に登録する。 */
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SYNCED, SYNCED_SPEC);
    }
}
