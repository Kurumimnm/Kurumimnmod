package io.github.kurumimnm.kurumimnmod.status;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.level.LevelManager;
import io.github.kurumimnm.kurumimnmod.registry.ModAttachments;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * ステータスの読み書き・計算の窓口（サーバー・クライアント共通）。
 * <p>
 * 【計算式】（数値はすべて Config の [status] で変更可能）
 * <pre>
 * 最大 HP   = 初期HP + 体力 × 10
 * 最大 MP   = 初期MP + 魔力 × 10
 * 攻撃力    = 初期値 + 攻撃力ポイント × 1   → 与ダメージ ×(1 + (攻撃力 - 1) × 10%)
 * 防御力    = 初期値 + 防御力ポイント × 1   → 敵からの被ダメージ −(防御力 - 1) × 0.5
 * 知力      = 初期値 + 知力ポイント × 1   → 魔法ダメージ ×(1 + (知力 - 1) × 10%)（攻撃力の倍率はかからない）
 * 技量      = 初期値                        → クリ率 5% + (技量 - 1) × 0.5%、クリ倍率 1.5 + (技量 - 1) × 0.01
 * ラック    = 初期値                        → バニラの幸運 +(ラック - 1)
 * 抵抗力    = 初期値
 * </pre>
 * Config は SYNCED なのでクライアントにも同じ値が届き、画面表示もこのクラスで計算できる。
 */
public final class StatManager {

    private StatManager() {}

    // =====================================================================
    // 振り分けデータ
    // =====================================================================

    public static PlayerStats get(Player player) {
        return player.getData(ModAttachments.PLAYER_STATS);
    }

    /** レベルアップで獲得したポイントの合計。 */
    public static int earnedPoints(Player player) {
        int levelsGained = LevelManager.get(player).level() - ModConstants.INITIAL_LEVEL;
        return Math.max(0, levelsGained * ModConfigs.pointsPerLevel());
    }

    /** まだ振り分けていないポイント。 */
    public static int unspentPoints(Player player) {
        return Math.max(0, earnedPoints(player) - get(player).totalPoints());
    }

    /**
     * ポイントを振り分ける（サーバー側）。不正な値（負の数・所持ポイント超過）は全体を拒否する。
     *
     * @return 振り分けに成功したら true
     */
    public static boolean allocate(ServerPlayer player, Map<AllocType, Integer> add) {
        int total = 0;
        for (int n : add.values()) {
            if (n < 0) {
                return false;
            }
            total += n;
        }
        if (total <= 0 || total > unspentPoints(player)) {
            return false;
        }
        float oldMaxHealth = player.getMaxHealth();
        player.setData(ModAttachments.PLAYER_STATS, get(player).plus(add));
        applyAttributes(player);
        // 最大 HP が増えた分だけ現在 HP も回復させる（振った直後にバーが減って見えないように）
        float gained = player.getMaxHealth() - oldMaxHealth;
        if (gained > 0) {
            player.heal(gained);
        }
        if (ModConfigs.isDebugLogging()) {
            Kurumimnmod.LOGGER.info("[{}] {} allocated {}", Kurumimnmod.MOD_ID, player.getName().getString(), add);
        }
        return true;
    }

    /** 振り分けをすべて取り消す（コマンド用）。ポイントは全額戻る。 */
    public static void reset(ServerPlayer player) {
        player.setData(ModAttachments.PLAYER_STATS, PlayerStats.DEFAULT);
        applyAttributes(player);
    }

    // =====================================================================
    // ステータス値の計算
    // =====================================================================

    /** ステータスの最終的な値。 */
    public static double value(Player player, StatType type) {
        PlayerStats s = get(player);
        return switch (type) {
            case ATTACK -> cfg(ModConfigs.INITIAL_ATTACK) + s.points(AllocType.ATTACK) * cfg(ModConfigs.ATTACK_PER_POINT);
            case DEFENSE -> cfg(ModConfigs.INITIAL_DEFENSE) + s.points(AllocType.DEFENSE) * cfg(ModConfigs.DEFENSE_PER_POINT);
            case INTELLIGENCE -> cfg(ModConfigs.INITIAL_INTELLIGENCE) + s.points(AllocType.INTELLIGENCE) * cfg(ModConfigs.INTELLIGENCE_PER_POINT);
            case DEXTERITY -> cfg(ModConfigs.INITIAL_DEXTERITY);
            case LUCK -> cfg(ModConfigs.INITIAL_LUCK);
            case RESISTANCE -> cfg(ModConfigs.INITIAL_RESISTANCE);
            case ATTACK_SPEED -> ModConfigs.get(ModConfigs.INITIAL_ATTACK_SPEED);
            case MOVE_SPEED -> ModConfigs.get(ModConfigs.INITIAL_MOVE_SPEED);
            case JUMP_POWER -> ModConfigs.get(ModConfigs.INITIAL_JUMP_POWER);
        };
    }

    /** 最大 HP（ステータスから計算した値。実際の最大 HP は属性に反映済み）。 */
    public static double maxHealth(Player player) {
        return cfg(ModConfigs.INITIAL_HP) + get(player).points(AllocType.VITALITY) * cfg(ModConfigs.VITALITY_HP_PER_POINT);
    }

    /** 最大 MP。 */
    public static float maxMana(Player player) {
        return cfg(ModConfigs.INITIAL_MP) + get(player).points(AllocType.MAGIC) * cfg(ModConfigs.MAGIC_MP_PER_POINT);
    }

    /** 振り分け 1 ポイントあたりの上昇量（付与画面の説明文用）。 */
    public static int perPoint(AllocType type) {
        return switch (type) {
            case VITALITY -> cfg(ModConfigs.VITALITY_HP_PER_POINT);
            case MAGIC -> cfg(ModConfigs.MAGIC_MP_PER_POINT);
            case ATTACK -> cfg(ModConfigs.ATTACK_PER_POINT);
            case DEFENSE -> cfg(ModConfigs.DEFENSE_PER_POINT);
            case INTELLIGENCE -> cfg(ModConfigs.INTELLIGENCE_PER_POINT);
        };
    }

    // ---- 効果の計算（StatEvents から使う） ----

    /** 攻撃力による与ダメージ倍率。 */
    public static float attackMultiplier(Player player) {
        double bonus = (value(player, StatType.ATTACK) - 1) * ModConfigs.attackDamageBonusPerPoint();
        return (float) Math.max(0, 1 + bonus);
    }

    /** 知力による魔法ダメージ倍率（魔法ダメージには攻撃力の倍率はかからない）。 */
    public static float magicMultiplier(Player player) {
        double bonus = (value(player, StatType.INTELLIGENCE) - 1) * ModConfigs.intelligenceMagicBonusPerPoint();
        return (float) Math.max(0, 1 + bonus);
    }

    /** 防御力による被ダメージ軽減量（固定値）。 */
    public static float defenseReduction(Player player) {
        return defenseReduction(value(player, StatType.DEFENSE));
    }

    /**
     * 防御力の値から被ダメージ軽減量（固定値）を求める。プレイヤー・Mob・カカシで共通の式。
     * 防御力 1 で軽減なし、以降 1 ごとに Config の defenseReduction ずつ増える。
     */
    public static float defenseReduction(double defense) {
        return (float) Math.max(0, (defense - 1) * ModConfigs.defenseReductionPerPoint());
    }

    /**
     * 軽減量を引いたダメージ（最低ダメージ保証つき）。軽減量が 0 以下なら元のダメージをそのまま返す。
     */
    public static float applyDefense(float damage, float reduction) {
        if (reduction <= 0 || damage <= 0) {
            return damage;
        }
        float minDamage = (float) Math.min(ModConfigs.defenseMinDamage(), damage);
        return Math.max(minDamage, damage - reduction);
    }

    /**
     * 防御力ステータスでの軽減（プレイヤー・{@link DefenseHolder} の Mob・カカシで共通）。
     * <ul>
     *   <li>防御力 ＞ 受けるダメージ → 被ダメは Config blockedDamage（既定 1）</li>
     *   <li>それ以外 → 受けるダメージ − (防御力 − 1) × defenseReduction（従来どおり）</li>
     * </ul>
     */
    public static float applyDefenseStat(float damage, double defense) {
        if (damage <= 0) {
            return damage;
        }
        if (defense > damage) {
            return Math.min(damage, (float) ModConfigs.blockedDamage());
        }
        return applyDefense(damage, defenseReduction(defense));
    }

    /**
     * 敵の防御力（VIT・MDF）での軽減。
     * <ul>
     *   <li>D ≦ VIT → 被ダメは Config blockedDamage（既定 1）</li>
     *   <li>D ＞ VIT → D − VIT × enemyDefenseRatio（既定 0.5）</li>
     * </ul>
     * 例（既定値・VIT 9）：D10 → 5.5、D20 → 15.5。VIT 10 に D10 → 1。
     */
    public static float applyEnemyDefense(float damage, int defense) {
        if (defense <= 0 || damage <= 0) {
            return damage;
        }
        float blocked = Math.min(damage, (float) ModConfigs.blockedDamage());
        if (damage <= defense) {
            return blocked;
        }
        return Math.max(blocked, damage - (float) (defense * ModConfigs.enemyDefenseRatio()));
    }

    /** 技量によるクリティカル率（0.0〜1.0）。 */
    public static double critChance(Player player) {
        double dex = value(player, StatType.DEXTERITY) - 1;
        return Math.clamp(ModConfigs.critChanceBase() + dex * ModConfigs.critChancePerPoint(), 0.0, 1.0);
    }

    /** 技量によるクリティカル倍率。 */
    public static float critMultiplier(Player player) {
        double dex = value(player, StatType.DEXTERITY) - 1;
        return (float) Math.max(1.0, ModConfigs.critDamageBase() + dex * ModConfigs.critDamagePerPoint());
    }

    private static int cfg(net.neoforged.neoforge.common.ModConfigSpec.IntValue value) {
        return ModConfigs.get(value);
    }

    // =====================================================================
    // MP（現在値）
    // =====================================================================

    /** 現在の MP。未設定（-1）は満タン扱い。 */
    public static float mana(Player player) {
        float max = maxMana(player);
        float current = player.getData(ModAttachments.PLAYER_MANA);
        return current < 0 ? max : Math.min(current, max);
    }

    /** MP を設定する（サーバー側）。満タンになったら「未設定（満タン扱い）」に戻して同期を減らす。 */
    public static void setMana(ServerPlayer player, float mana) {
        float max = maxMana(player);
        float value = mana >= max ? -1f : Math.max(0f, mana);
        if (player.getData(ModAttachments.PLAYER_MANA) != value) {
            player.setData(ModAttachments.PLAYER_MANA, value);
        }
    }

    /** MP を消費する（魔法などから使う予定）。足りなければ消費せず false。 */
    public static boolean tryConsumeMana(ServerPlayer player, float amount) {
        float current = mana(player);
        if (current < amount) {
            return false;
        }
        setMana(player, current - amount);
        return true;
    }

    // =====================================================================
    // バニラの属性への反映（最大 HP・幸運・攻撃速度・移動速度・ジャンプ力）
    // =====================================================================

    private static final Identifier MAX_HEALTH_ID = id("stat_max_health");
    private static final Identifier LUCK_ID = id("stat_luck");
    private static final Identifier ATTACK_SPEED_ID = id("stat_attack_speed");
    private static final Identifier MOVE_SPEED_ID = id("stat_move_speed");
    private static final Identifier JUMP_ID = id("stat_jump_power");

    /**
     * ステータスをバニラの属性に反映する（サーバー側）。
     * 値が変わっていなければ何もしないので、毎秒呼んでも軽い（Config の変更もこれで反映される）。
     * <p>
     * 永続（permanent）の修飾子を使うのは、ログイン時に「HP の読み込み → 最大 HP の反映」の順になって
     * HP が 20 に切り詰められるのを防ぐため（永続の修飾子はセーブデータに保存され、HP より先に読み込まれる）。
     */
    public static void applyAttributes(ServerPlayer player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            setModifier(player, Attributes.MAX_HEALTH, MAX_HEALTH_ID,
                    maxHealth(player) - health.getBaseValue(), AttributeModifier.Operation.ADD_VALUE);
        }
        setModifier(player, Attributes.LUCK, LUCK_ID,
                (value(player, StatType.LUCK) - 1) * ModConfigs.luckAttributePerPoint(), AttributeModifier.Operation.ADD_VALUE);
        setModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID,
                value(player, StatType.ATTACK_SPEED) - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player, Attributes.MOVEMENT_SPEED, MOVE_SPEED_ID,
                value(player, StatType.MOVE_SPEED) - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(player, Attributes.JUMP_STRENGTH, JUMP_ID,
                value(player, StatType.JUMP_POWER) - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        // 最大 HP が下がった（Config 変更・リセット）ときは現在 HP を合わせる
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /** 修飾子を設定する。値が 0 なら削除、同じ値なら何もしない。 */
    private static void setModifier(Player player, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation op) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (amount == 0) {
            if (existing != null) {
                instance.removeModifier(id);
            }
            return;
        }
        if (existing != null && existing.amount() == amount && existing.operation() == op) {
            return;
        }
        instance.addOrReplacePermanentModifier(new AttributeModifier(id, amount, op));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, path);
    }
}
