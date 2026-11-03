package io.github.kurumimnm.kurumimnmod.datagen;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.registry.ModEntities;
import io.github.kurumimnm.kurumimnmod.registry.ModItems;
import io.github.kurumimnm.kurumimnmod.status.AllocType;
import io.github.kurumimnm.kurumimnmod.status.StatType;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/**
 * 英語の言語ファイル（en_us.json）を生成する Provider。
 * <p>
 * 書き方:
 * <pre>{@code
 * addItem(ModItems.WALNUT, "Walnut");
 * addBlock(ModBlocks.WALNUT_PLANKS, "Walnut Planks");
 * addEntityType(ModEntities.WALNUT_MOB, "Walnut Mob");
 * }</pre>
 * 日本語は src/main/resources/assets/kurumimnmod/lang/ja_jp.json に同じキーで手書きする。
 */
public class ModEnUsLanguageProvider extends LanguageProvider {

    private static final String MOD_ID = Kurumimnmod.MOD_ID;

    public ModEnUsLanguageProvider(PackOutput output) {
        super(output, MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // ---- クリエイティブタブ ----
        add("itemGroup." + MOD_ID + ".main", "Kurumimnmod");

        // ---- アイテム ----
        addItem(ModItems.ICE_WAND, "Icicle Wand");

        // ---- ブロック ----

        // ---- エンティティ ----
        addEntityType(ModEntities.ICE_ICICLE, "Icicle");

        // ---- 魔法の杖（ツールチップ・メッセージ） ----
        add("tooltip." + MOD_ID + ".wand.mp_cost", "MP Cost: %s");
        add("tooltip." + MOD_ID + ".wand.damage", "Damage: %s (center) - %s (edge)");
        add("tooltip." + MOD_ID + ".wand.radius", "Radius: %s blocks");
        add("tooltip." + MOD_ID + ".wand.pierce", "Pierces enemies and shatters on the ground or walls");
        add("tooltip." + MOD_ID + ".wand.slowness", "Inflicts Slowness %s for %ss on hit");
        add("tooltip." + MOD_ID + ".wand.controls", "Hold Right-Click: Aim / Left-Click: Cast");
        add("message." + MOD_ID + ".not_enough_mp", "Not enough MP");

        // ---- キー割り当て ----
        add("key.category." + MOD_ID + ".main", "Kurumimnmod");
        add("key." + MOD_ID + ".open_status", "Open Status Screen");
        add("key." + MOD_ID + ".open_allocation", "Open Stat Allocation Screen");

        // ---- ステータス画面 ----
        add("screen." + MOD_ID + ".status", "Status");
        add("screen." + MOD_ID + ".status.level", "Level %s");
        add("screen." + MOD_ID + ".status.exp", "EXP %s / %s (%s%%)");
        add("screen." + MOD_ID + ".status.hp", "HP %s / %s");
        add("screen." + MOD_ID + ".status.mp", "MP %s / %s");
        add("screen." + MOD_ID + ".status.next", "To next level: %s EXP");
        add("screen." + MOD_ID + ".status.passives", "Passives");
        add("screen." + MOD_ID + ".status.passives.none", "None");

        // ---- ステータス付与画面 ----
        add("screen." + MOD_ID + ".allocation", "Allocate Stats");
        add("screen." + MOD_ID + ".allocation.tab_points", "Allocate Stats (%s)");
        add("screen." + MOD_ID + ".allocation.points", "Available Points: %s");
        add("screen." + MOD_ID + ".allocation.reset", "Reset");
        add("screen." + MOD_ID + ".allocation.confirm", "Confirm");

        // ---- レベル・経験値（メッセージ） ----
        add("message." + MOD_ID + ".exp_gain", "+%s EXP");
        add("message." + MOD_ID + ".exp_gain.assist", "+%s EXP (Assist)");
        add("message." + MOD_ID + ".level_up", "Level up! You are now level %s.");

        // ---- コマンド ----
        add("commands." + MOD_ID + ".exp.add.single", "Gave %s exp to %s");
        add("commands." + MOD_ID + ".exp.add.multiple", "Gave %s exp to %s players");
        add("commands." + MOD_ID + ".level.set.single", "Set level to %s for %s");
        add("commands." + MOD_ID + ".level.set.multiple", "Set level to %s for %s players");
        add("commands." + MOD_ID + ".level.query", "%s: Level %s (%s / %s exp)");
        add("commands." + MOD_ID + ".stat.reset.single", "Reset stat allocation of %s");
        add("commands." + MOD_ID + ".stat.reset.multiple", "Reset stat allocation of %s players");

        // ---- ステータス（StatType：ステータス画面に表示する値） ----
        addStat(StatType.ATTACK, "Attack", "Increases the damage you deal to enemies.");
        add(StatType.ATTACK.nameKey() + ".effect", "Current: damage dealt x%s (except magic)");
        addStat(StatType.DEFENSE, "Defense", "Reduces the damage you take from enemies by a flat amount (applied after armor).");
        add(StatType.DEFENSE.nameKey() + ".effect", "Current: damage taken -%s");
        addStat(StatType.INTELLIGENCE, "Intelligence", "Increases the damage of your magic attacks.");
        add(StatType.INTELLIGENCE.nameKey() + ".effect", "Current: magic damage x%s");
        addStat(StatType.DEXTERITY, "Dexterity", "Increases your critical hit chance and critical damage.");
        add(StatType.DEXTERITY.nameKey() + ".effect", "Current: crit chance %s%% / crit damage x%s");
        addStat(StatType.LUCK, "Luck", "Your fortune. Affects things that depend on luck, such as fishing and chest loot.");
        addStat(StatType.RESISTANCE, "Resistance", "Resistance to debuffs and environmental hazards. Shortens debuffs and can nullify weak ones.");
        addStat(StatType.ATTACK_SPEED, "Attack Speed", "Attack speed multiplier.");
        addStat(StatType.MOVE_SPEED, "Movement Speed", "Movement speed multiplier.");
        addStat(StatType.JUMP_POWER, "Jump Power", "Jump strength multiplier.");

        // ---- 振り分け項目（AllocType：%s に 1 ポイントあたりの上昇量が入る） ----
        addAlloc(AllocType.VITALITY, "Vitality", "Each point increases max HP by %s.");
        addAlloc(AllocType.MAGIC, "Magic", "Each point increases max MP by %s.");
        addAlloc(AllocType.ATTACK, "Attack", "Each point increases Attack by %s.");
        addAlloc(AllocType.DEFENSE, "Defense", "Each point increases Defense by %s.");
        addAlloc(AllocType.INTELLIGENCE, "Intelligence", "Each point increases Intelligence by %s.");

        // ---- Config 画面 ----
        add(MOD_ID + ".configuration.title", "Kurumimnmod Configs");
        add(MOD_ID + ".configuration.section." + MOD_ID + ".synced.toml", "Kurumimnmod Configs");
        add(MOD_ID + ".configuration.section." + MOD_ID + ".synced.toml.title", "Kurumimnmod Configs");
        add(MOD_ID + ".configuration.section." + MOD_ID + ".client.toml", "Kurumimnmod Client Configs");
        add(MOD_ID + ".configuration.section." + MOD_ID + ".client.toml.title", "Kurumimnmod Client Configs");
        add(MOD_ID + ".configuration.general", "General");
        add(MOD_ID + ".configuration.debugLogging", "Debug Logging");
        add(MOD_ID + ".configuration.player", "Player");
        add(MOD_ID + ".configuration.disableHunger", "Disable Hunger");
        add(MOD_ID + ".configuration.level", "Level");
        add(MOD_ID + ".configuration.disableVanillaExperience", "Disable Vanilla Experience");
        add(MOD_ID + ".configuration.baseExp", "Exp Required for Level 2");
        add(MOD_ID + ".configuration.growth", "Required Exp Growth");
        add(MOD_ID + ".configuration.exponent", "Required Exp Curve");
        add(MOD_ID + ".configuration.maxLevel", "Max Level (0 = No Limit)");
        add(MOD_ID + ".configuration.killerExpRatio", "Killer Exp Multiplier");
        add(MOD_ID + ".configuration.assistExpRatio", "Assist Exp Multiplier");
        add(MOD_ID + ".configuration.bossAssistExpRatio", "Boss Assist Exp Multiplier");
        add(MOD_ID + ".configuration.assistWindowSeconds", "Assist Duration (Seconds)");
        add(MOD_ID + ".configuration.showExpMessages", "Show Exp Gain Messages");
        add(MOD_ID + ".configuration.status", "Status");
        add(MOD_ID + ".configuration.pointsPerLevel", "Stat Points per Level");
        add(MOD_ID + ".configuration.initial", "Initial Stats");
        add(MOD_ID + ".configuration.hp", "Max HP");
        add(MOD_ID + ".configuration.mp", "Max MP");
        add(MOD_ID + ".configuration.attack", "Attack");
        add(MOD_ID + ".configuration.defense", "Defense");
        add(MOD_ID + ".configuration.intelligence", "Intelligence");
        add(MOD_ID + ".configuration.dexterity", "Dexterity");
        add(MOD_ID + ".configuration.luck", "Luck");
        add(MOD_ID + ".configuration.resistance", "Resistance");
        add(MOD_ID + ".configuration.attackSpeed", "Attack Speed Multiplier");
        add(MOD_ID + ".configuration.moveSpeed", "Movement Speed Multiplier");
        add(MOD_ID + ".configuration.jumpPower", "Jump Power Multiplier");
        add(MOD_ID + ".configuration.perPoint", "Gain per Allocated Point");
        add(MOD_ID + ".configuration.vitalityHp", "Max HP per Vitality");
        add(MOD_ID + ".configuration.magicMp", "Max MP per Magic");
        add(MOD_ID + ".configuration.effects", "Stat Effects");
        add(MOD_ID + ".configuration.attackDamageBonus", "Damage Bonus per Attack");
        add(MOD_ID + ".configuration.defenseReduction", "Damage Reduction per Defense");
        add(MOD_ID + ".configuration.defenseMinDamage", "Minimum Damage after Defense");
        add(MOD_ID + ".configuration.critChanceBase", "Base Crit Chance");
        add(MOD_ID + ".configuration.critChancePerPoint", "Crit Chance per Dexterity");
        add(MOD_ID + ".configuration.critDamageBase", "Base Crit Damage");
        add(MOD_ID + ".configuration.critDamagePerPoint", "Crit Damage per Dexterity");
        add(MOD_ID + ".configuration.luckAttributePerPoint", "Vanilla Luck per Luck");
        add(MOD_ID + ".configuration.mpRegenPerSecond", "MP Regen per Second (Ratio)");
        add(MOD_ID + ".configuration.intelligenceMagicBonus", "Magic Damage Bonus per Intelligence");
        add(MOD_ID + ".configuration.magic", "Magic");
        add(MOD_ID + ".configuration.iceWand", "Icicle Wand");
        add(MOD_ID + ".configuration.mpCost", "MP Cost");
        add(MOD_ID + ".configuration.damage", "Damage (Center)");
        add(MOD_ID + ".configuration.radius", "Impact Radius");
        add(MOD_ID + ".configuration.edgeDamageRatio", "Damage Ratio at Edge");
        add(MOD_ID + ".configuration.castRange", "Max Aim Distance");
        add(MOD_ID + ".configuration.projectileSpeed", "Projectile Speed (Blocks/Tick)");
        add(MOD_ID + ".configuration.spawnHeight", "Spawn Height above Head");
        add(MOD_ID + ".configuration.cooldownTicks", "Cooldown (Ticks)");
        add(MOD_ID + ".configuration.slownessDuration", "Slowness Duration (Ticks)");
        add(MOD_ID + ".configuration.slownessAmplifier", "Slowness Level (0 = I)");
        add(MOD_ID + ".configuration.enemyHealthBar", "Enemy Health Bar");
        add(MOD_ID + ".configuration.enabled", "Enabled");
        add(MOD_ID + ".configuration.range", "Display Range");
        add(MOD_ID + ".configuration.hostileOnly", "Hostile Mobs Only");
    }

    /** ステータスの名前と説明文をまとめて追加する。 */
    private void addStat(StatType type, String name, String description) {
        add(type.nameKey(), name);
        add(type.descriptionKey(), description);
    }

    /** 振り分け項目の名前と説明文をまとめて追加する。 */
    private void addAlloc(AllocType type, String name, String description) {
        add(type.nameKey(), name);
        add(type.descriptionKey(), description);
    }
}
