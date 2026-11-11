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
    private static final String SCARECROW_SCREEN = "screen." + MOD_ID + ".scarecrow";

    public ModEnUsLanguageProvider(PackOutput output) {
        super(output, MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        // ---- クリエイティブタブ ----
        add("itemGroup." + MOD_ID + ".main", "Kurumimnmod");

        // ---- アイテム ----
        addItem(ModItems.SCARECROW_SUMMON_STICK, "Scarecrow Summoning Stick");
        add("item." + MOD_ID + ".scarecrow_summon_stick.desc", "Right-click a block to summon a scarecrow.");
        add("item." + MOD_ID + ".scarecrow_summon_stick.op_only", "Operators only");
        addItem(ModItems.SCARECROW_CONFIG_STICK, "Scarecrow Settings Stick");
        add("item." + MOD_ID + ".scarecrow_config_stick.desc", "Aim at a scarecrow and left-click to open its settings.");
        addItem(ModItems.ICE_WAND, "Icicle Wand");
        addItem(ModItems.GRIFFIN_SPAWN_EGG, "Griffin Spawn Egg");
        addItem(ModItems.MINION_SPAWN_EGG, "Minion Spawn Egg");
        addItem(ModItems.SLIME_SPAWN_EGG, "Slime Spawn Egg");
        addItem(ModItems.GAROU_SPAWN_EGG, "Garou Spawn Egg");
        addItem(ModItems.KOBOLD_SPAWN_EGG, "Kobold Spawn Egg");
        addItem(ModItems.KOBOLD_LORD_SPAWN_EGG, "Kobold Lord Spawn Egg");

        // ---- ブロック ----

        // ---- エンティティ ----
        addEntityType(ModEntities.SCARECROW, "Scarecrow");
        addEntityType(ModEntities.GRIFFIN, "Griffin");
        addEntityType(ModEntities.MINION, "Minion");
        addEntityType(ModEntities.SLIME, "Slime");
        addEntityType(ModEntities.GAROU, "Garou");
        addEntityType(ModEntities.KOBOLD, "Kobold");
        addEntityType(ModEntities.KOBOLD_LORD, "Kobold Lord");


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
        addStat(StatType.DEFENSE, "Defense", "Reduces the damage you take from enemies by a flat amount (applied after armor). If your Defense is higher than the damage, you take only 1.");
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

        // ---- カカシ（メッセージ） ----
        add("message." + MOD_ID + ".scarecrow.op_only", "Only operators can do this.");
        add("message." + MOD_ID + ".scarecrow.no_space", "Not enough space to summon a scarecrow here.");
        add("message." + MOD_ID + ".scarecrow.not_found", "The scarecrow could not be found.");
        add("message." + MOD_ID + ".scarecrow.saved", "Scarecrow settings saved.");
        add("message." + MOD_ID + ".scarecrow.deleted", "Scarecrow removed.");
        add("message." + MOD_ID + ".scarecrow.damage_taken", "Scarecrow hit you: %s damage (%s before reduction)");

        // ---- カカシの設定画面 ----
        add(SCARECROW_SCREEN, "Scarecrow Settings");
        add(SCARECROW_SCREEN + ".section.defense", "When Hit");
        add(SCARECROW_SCREEN + ".section.counter", "Counterattack");
        add(SCARECROW_SCREEN + ".max_health", "Max HP");
        add(SCARECROW_SCREEN + ".max_health.desc", "Max HP of the scarecrow (1 - 1024).");
        add(SCARECROW_SCREEN + ".infinite_health", "Infinite HP");
        add(SCARECROW_SCREEN + ".infinite_health.desc", "If checked, HP never decreases (damage is still measured and shown). If unchecked, the hit that empties its HP is shown in red and HP is fully restored.");
        add(SCARECROW_SCREEN + ".defense", "Defense");
        add(SCARECROW_SCREEN + ".defense.desc", "Reduces damage from attackers by a flat amount, using the same formula as the Defense stat of players (0 - 1000000).");
        add(SCARECROW_SCREEN + ".armor", "Armor");
        add(SCARECROW_SCREEN + ".armor.desc", "Vanilla armor points (0 - 30).");
        add(SCARECROW_SCREEN + ".magic_resistance", "Magic Resist (%)");
        add(SCARECROW_SCREEN + ".magic_resistance.desc", "Reduces magic damage by this percentage (0 - 100).");
        add(SCARECROW_SCREEN + ".knockback_resistance", "Knockback Resist (%)");
        add(SCARECROW_SCREEN + ".knockback_resistance.desc", "At 100 the scarecrow is never knocked back (0 - 100). When knocked back, it slides back to where it was summoned.");
        add(SCARECROW_SCREEN + ".no_knockback", "No Knockback");
        add(SCARECROW_SCREEN + ".no_knockback.desc", "If checked, the scarecrow is never knocked back and stays where it was summoned (Knockback Resist is ignored).");
        add(SCARECROW_SCREEN + ".counter_enabled", "Counterattack");
        add(SCARECROW_SCREEN + ".counter_enabled.desc", "If checked, the scarecrow attacks the nearest player in reach (survival / adventure only).");
        add(SCARECROW_SCREEN + ".counter_damage", "Attack Damage");
        add(SCARECROW_SCREEN + ".counter_damage.desc", "Damage before the armor and Defense of the player (0 - 1000000).");
        add(SCARECROW_SCREEN + ".counter_interval", "Interval (sec)");
        add(SCARECROW_SCREEN + ".counter_interval.desc", "Seconds between attacks (0.1 - 60).");
        add(SCARECROW_SCREEN + ".counter_range", "Reach (blocks)");
        add(SCARECROW_SCREEN + ".counter_range.desc", "How far the scarecrow can attack (0.5 - 16 blocks).");
        add(SCARECROW_SCREEN + ".reset", "Defaults");
        add(SCARECROW_SCREEN + ".reset.desc", "Fill in the default values from the config. Not applied until you press Save.");
        add(SCARECROW_SCREEN + ".delete", "Remove");
        add(SCARECROW_SCREEN + ".delete.desc", "Remove this scarecrow (operators only).");
        add(SCARECROW_SCREEN + ".delete.confirm", "Really?");
        add(SCARECROW_SCREEN + ".cancel", "Cancel");
        add(SCARECROW_SCREEN + ".save", "Save");

        // ---- ワールド・ブロック・キー ----
        add("generator." + MOD_ID + ".field_world", "Kurumimnmod World");
        add("block." + MOD_ID + ".quest_gate", "Quest Gate");
        add("key." + MOD_ID + ".open_quest", "Open Quests");

        // ---- バイオーム ----
        add("biome." + MOD_ID + ".home_plains", "Starting Plains");
        add("biome." + MOD_ID + ".quest_meadow", "Meadow");
        add("biome." + MOD_ID + ".quest_forest", "Forest");
        add("biome." + MOD_ID + ".quest_desert", "Desert");
        add("biome." + MOD_ID + ".quest_snow", "Snowfield");
        add("biome." + MOD_ID + ".quest_badlands", "Badlands");
        add("biome." + MOD_ID + ".quest_arena", "Boss Arena");
        add("biome." + MOD_ID + ".quest_hub", "Waypoint");
        add("biome." + MOD_ID + ".quest_void", "Void");

        // ---- クエスト（名前・説明・フィールド名） ----
        add("quest." + MOD_ID + ".tutorial", "Tutorial");
        add("quest." + MOD_ID + ".tutorial.description", "Learn how to fight on the meadows outside town. Defeat every monster in both fields to clear it. Clearing it lets you choose a job.");
        add("quest." + MOD_ID + ".tutorial.field.1", "Meadow of Beginnings");
        add("quest." + MOD_ID + ".tutorial.field.2", "Monster Meadow");
        add("quest." + MOD_ID + ".kobold_meadow", "The Kobold Meadows");
        add("quest." + MOD_ID + ".kobold_meadow.description", "Meadows overrun by kobolds. Clear out every monster in each field, then defeat the Kobold Lord waiting in the temple at the end.");
        add("quest." + MOD_ID + ".kobold_meadow.field.1", "Meadow of Beginnings");
        add("quest." + MOD_ID + ".kobold_meadow.field.2", "Howling Meadow");
        add("quest." + MOD_ID + ".kobold_meadow.field.3", "Kobold Territory");
        add("quest." + MOD_ID + ".kobold_meadow.field.boss", "Arena of the Kobold Lord");
        add("quest." + MOD_ID + ".first_trial", "The First Trial");
        add("quest." + MOD_ID + ".first_trial.description", "Cross the meadow and the forest beyond the town, and defeat the beast lurking deep inside.");
        add("quest." + MOD_ID + ".first_trial.field.1", "Meadow Path");
        add("quest." + MOD_ID + ".first_trial.field.2", "Sunlit Woods");
        add("quest." + MOD_ID + ".first_trial.field.boss", "Arena of the Beast");
        add("quest." + MOD_ID + ".forked_road", "The Forked Road");
        add("quest." + MOD_ID + ".forked_road.description", "Past the meadow the road splits into desert and snowfield. Only those who conquer both may challenge the master of the arena.");
        add("quest." + MOD_ID + ".forked_road.field.1", "Meadow of Departure");
        add("quest." + MOD_ID + ".forked_road.field.2a", "Scorching Dunes");
        add("quest." + MOD_ID + ".forked_road.field.2b", "Silver Peaks");
        add("quest." + MOD_ID + ".forked_road.field.boss", "Arena of the Master");

        // ---- 左上の表示 ----
        add("hud." + MOD_ID + ".quest.field", "Field %s: %s");
        add("hud." + MOD_ID + ".quest.hub", "Waypoint");
        add("hud." + MOD_ID + ".quest.goal", "Goal: %s");
        add("hud." + MOD_ID + ".quest.goal.kill", "Defeat monsters %s / %s");
        add("hud." + MOD_ID + ".quest.goal.reach", "Reach the gate");
        add("hud." + MOD_ID + ".quest.goal.boss", "Defeat the boss");
        add("hud." + MOD_ID + ".quest.goal.wipe", "Defeat all monsters %s / %s");
        add("hud." + MOD_ID + ".quest.goal.cleared", "Cleared! The gate is open");
        add("hud." + MOD_ID + ".quest.goal.hub", "Choose the next field at the departure gate");
        add("hud." + MOD_ID + ".quest.goal.travel", "Travel to the field from the quest screen (J)");
        add("hud." + MOD_ID + ".quest.goal.done", "Quest complete! Return through the gate");

        // ---- クエスト画面・ゲート画面 ----
        add("screen." + MOD_ID + ".quest", "Quests");
        add("screen." + MOD_ID + ".quest.close", "Close");
        add("screen." + MOD_ID + ".quest.travel", "To the Field");
        add("screen." + MOD_ID + ".quest.travel.tooltip", "Travel to the quest (the first field, or the waypoint once you have cleared a field)");
        add("screen." + MOD_ID + ".quest.abandon", "Abandon");
        add("screen." + MOD_ID + ".quest.abandon.confirm", "Really abandon?");
        add("screen." + MOD_ID + ".quest.abandon.tooltip", "Give up the quest and return to where you accepted it (press again to confirm)");
        add("screen." + MOD_ID + ".quest.accept", "Accept");
        add("screen." + MOD_ID + ".quest.completed_mark", "%s ✔");
        add("screen." + MOD_ID + ".quest.list", "Quest List");
        add("screen." + MOD_ID + ".quest.list.empty", "No quests available");
        add("screen." + MOD_ID + ".quest.current", "Current Quest");
        add("screen." + MOD_ID + ".quest.fields", "Fields");
        add("screen." + MOD_ID + ".quest.field.cleared", "✔ %s");
        add("screen." + MOD_ID + ".quest.field.here", "▶ %s");
        add("screen." + MOD_ID + ".quest.field.open", "· %s");
        add("screen." + MOD_ID + ".quest.field.unknown", "? Field %s: ???");
        add("screen." + MOD_ID + ".gate", "Gate");
        add("screen." + MOD_ID + ".gate.title", "The gate is open. Choose where to go");
        add("screen." + MOD_ID + ".gate.title.hub", "Departure Gate: choose where to go");
        add("screen." + MOD_ID + ".gate.hub", "To the Waypoint");
        add("screen." + MOD_ID + ".gate.finish", "Complete the quest (return to where you accepted it)");
        add("screen." + MOD_ID + ".gate.locked", "%s (locked)");
        add("screen." + MOD_ID + ".gate.locked.tooltip", "Another field must be cleared first");
        add("screen." + MOD_ID + ".gate.stay", "Not yet");
        add("screen." + MOD_ID + ".status.money", "Money %sG");

        // ---- メッセージ・コマンド ----
        add("message." + MOD_ID + ".quest.already", "You already have a quest (you can abandon it from the quest screen)");
        add("message." + MOD_ID + ".quest.unknown", "Quest %s was not found");
        add("message." + MOD_ID + ".quest.accepted", "Accepted the quest \"%s\"");
        add("message." + MOD_ID + ".quest.none", "You have no quest");
        add("message." + MOD_ID + ".quest.abandoned", "Abandoned the quest \"%s\"");
        add("message." + MOD_ID + ".quest.completed", "Quest \"%s\" complete! Reward: %sG");
        add("message." + MOD_ID + ".quest.entered", "Entered %s");
        add("message." + MOD_ID + ".quest.field_title", "Field %s: %s");
        add("message." + MOD_ID + ".quest.hub", "Arrived at the waypoint");
        add("message." + MOD_ID + ".quest.not_your_gate", "You cannot use this gate");
        add("message." + MOD_ID + ".quest.gate_closed", "The gate is still closed (the clear condition is not met)");
        add("message." + MOD_ID + ".quest.locked", "You cannot enter that field yet");
        add("message." + MOD_ID + ".quest.field_cleared", "Field cleared! The gate is open");
        add("message." + MOD_ID + ".quest.goal_cleared", "Boss defeated! Complete the quest through the gate");
        add("commands." + MOD_ID + ".quest.entry", "%s: %s");
        add("commands." + MOD_ID + ".quest.not_in_field", "You are not in a field");
        add("commands." + MOD_ID + ".quest.no_field", "Quest %s has no field %s");
        add("commands." + MOD_ID + ".quest.preview", "Created plot %s: center X %s Z %s (size %s), entrance %s %s %s");
        add("commands." + MOD_ID + ".money.get", "Money: %sG");
        // ---- 職業 ----
        add("message.kurumimnmod.job.unlocked", "Jobs unlocked!");
        add("message.kurumimnmod.job.selected", "You became a %s!");
        add("screen.kurumimnmod.job", "Choose a Job");
        add("screen.kurumimnmod.job.title", "Choose your job");
        add("screen.kurumimnmod.job.note", "You cannot change your job later");
        add("screen.kurumimnmod.job.chosen", "> %s <");
        add("screen.kurumimnmod.job.confirm", "Become a %s");
        add("screen.kurumimnmod.job.later", "Choose later");
        add("screen.kurumimnmod.job.close", "Close");
        add("screen.kurumimnmod.job.empty.1", "No jobs are available yet");
        add("screen.kurumimnmod.job.empty.2", "Once jobs are added, this screen opens when you log in");
        add("commands.kurumimnmod.job.unlock", "Unlocked jobs for %s player(s)");
        add("commands.kurumimnmod.job.reset", "Reset the job of %s player(s)");

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
        add(MOD_ID + ".configuration.baseExp", "Exp Required for Level 1");
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
        add(MOD_ID + ".configuration.enemy", "Enemy Stats");
        add(MOD_ID + ".configuration.enemyDefenseRatio", "Enemy VIT/MDF Reduction Ratio");
        add(MOD_ID + ".configuration.blockedDamage", "Damage when Defense beats Attack");
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
        add(MOD_ID + ".configuration.mobs", "Mobs");
        add(MOD_ID + ".configuration.griffin", "Griffin");
        add(MOD_ID + ".configuration.armoredChance", "Armored Spawn Chance");
        add(MOD_ID + ".configuration.normalDefense", "Defense (No Armor)");
        add(MOD_ID + ".configuration.skillTelegraphSeconds", "Skill Warning Time (seconds)");
        add(MOD_ID + ".configuration.garou", "Garou");
        add(MOD_ID + ".configuration.garouBiteDamage", "Lunging Bite Damage (per bite)");
        add(MOD_ID + ".configuration.garouBiteDistance", "Lunging Bite Distance (blocks per bite)");
        add(MOD_ID + ".configuration.garouBiteCooldown", "Lunging Bite Cooldown (seconds)");
        add(MOD_ID + ".configuration.kobold", "Kobold");
        add(MOD_ID + ".configuration.koboldLeapDamage", "Leap Damage");
        add(MOD_ID + ".configuration.koboldLeapCooldown", "Leap Cooldown (seconds)");
        add(MOD_ID + ".configuration.koboldLord", "Kobold Lord");
        add(MOD_ID + ".configuration.koboldLordKickDamage", "Wall-Kick Dive Damage");
        add(MOD_ID + ".configuration.koboldLordKickRadius", "Wall-Kick Dive Radius (blocks)");
        add(MOD_ID + ".configuration.koboldLordKickCooldown", "Wall-Kick Dive Cooldown (seconds)");
        add(MOD_ID + ".configuration.koboldLordDualHealthRatio", "Dual-Wield Health Ratio");
        add(MOD_ID + ".configuration.koboldLordKickKnockback", "Wall-Kick Dive Shockwave Knockback");
        add(MOD_ID + ".configuration.koboldLordRoarRadius", "War Roar Radius (blocks)");
        add(MOD_ID + ".configuration.koboldLordRoarKnockback", "War Roar Knockback");
        add(MOD_ID + ".configuration.armoredDefense", "Defense (Armored)");

        // ---- Config 画面（カカシ・ダメージ数字） ----
        add(MOD_ID + ".configuration.scarecrow", "Scarecrow");
        add(MOD_ID + ".configuration.scarecrowMaxHealth", "Initial Max HP");
        add(MOD_ID + ".configuration.scarecrowInfiniteHealth", "Initial Infinite HP");
        add(MOD_ID + ".configuration.scarecrowDefense", "Initial Defense");
        add(MOD_ID + ".configuration.scarecrowArmor", "Initial Armor");
        add(MOD_ID + ".configuration.scarecrowMagicResistance", "Initial Magic Resistance (%)");
        add(MOD_ID + ".configuration.scarecrowKnockbackResistance", "Initial Knockback Resistance (%)");
        add(MOD_ID + ".configuration.scarecrowKnockbackImmune", "Initial No Knockback");
        add(MOD_ID + ".configuration.scarecrowCounterEnabled", "Initial Counterattack");
        add(MOD_ID + ".configuration.scarecrowCounterDamage", "Initial Counterattack Damage");
        add(MOD_ID + ".configuration.scarecrowCounterInterval", "Initial Counterattack Interval (Seconds)");
        add(MOD_ID + ".configuration.scarecrowCounterRange", "Initial Counterattack Reach (Blocks)");
        add(MOD_ID + ".configuration.scarecrowRegenDelay", "HP Restore Delay (Seconds)");
        add(MOD_ID + ".configuration.scarecrowReturnDelay", "Return Delay (Seconds)");
        add(MOD_ID + ".configuration.scarecrowReturnSpeed", "Return Speed (Blocks per Tick)");
        add(MOD_ID + ".configuration.damageNumbers", "Damage Numbers");
        add(MOD_ID + ".configuration.showDamageNumbers", "Show Damage Numbers");
        add(MOD_ID + ".configuration.damageNumberDuration", "Display Time (Seconds)");
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
