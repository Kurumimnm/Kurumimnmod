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

        // ---- ブロック ----

        // ---- エンティティ ----
        addEntityType(ModEntities.SCARECROW, "Scarecrow");


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

        // ---- ワールド（ワールドの種類・コマンド） ----
        add("generator." + MOD_ID + ".field_world", "Kurumimnmod World");
        add("commands." + MOD_ID + ".area.info", "Field %s: %s - Area %s (Danger %s)");
        add("commands." + MOD_ID + ".area.not_field_world", "This world does not use the Kurumimnmod field map.");
        add("commands." + MOD_ID + ".area.mountains", "(Inside the mountain range)");
        add("commands." + MOD_ID + ".towns.entry", "Town %s: X %s / Z %s");

        // ---- ワールド：街の名前（1〜20 は名前が決まるまで仮） ----
        add("town." + MOD_ID + ".mediana", "Mediana");
        for (int n = 1; n <= 20; n++) {
            add("town." + MOD_ID + ".town_" + n, "Town " + n + " (tentative)");
        }

        // ---- ワールド：場所の表示・ゲート・地図 ----
        add("hud." + MOD_ID + ".location.town", "Town");
        add("hud." + MOD_ID + ".location.area", "Area %s - Danger %s");
        add("hud." + MOD_ID + ".location.new", "- New Location -");
        add("message." + MOD_ID + ".gate.blocked", "Defeat the boss of this field to go further.");
        add("commands." + MOD_ID + ".gate.cleared", "Marked %s field(s) as boss defeated.");
        add("commands." + MOD_ID + ".gate.reset", "Reset all field boss victories.");
        add("commands." + MOD_ID + ".explore.all", "Marked every town and area as visited.");
        add("commands." + MOD_ID + ".explore.reset", "Cleared the exploration record.");
        add("key." + MOD_ID + ".open_map", "Open Map");
        add("screen." + MOD_ID + ".map", "Map");
        add("screen." + MOD_ID + ".map.loading", "Drawing the map...");
        add("screen." + MOD_ID + ".map.hover", "%s (%s)");
        add("screen." + MOD_ID + ".map.unexplored", "Unexplored");
        add("screen." + MOD_ID + ".map.coords", "X %s / Z %s");
        add("screen." + MOD_ID + ".map.help", "Drag: Move   Wheel: Zoom   M: Close");
        add("screen." + MOD_ID + ".map.help_op", "Drag: Move   Wheel: Zoom   Right-click: Teleport (OP)   M: Close");

        // ---- フィールド（バイオーム名）。番号は設計時のフィールド番号 ----
        addBiome("cradle_basin", "Cradle Basin"); // 1
        addBiome("young_leaf_road", "Young Leaf Road"); // 2
        addBiome("whispering_forest", "Whispering Forest"); // 3
        addBiome("windsong_hills", "Windsong Hills"); // 4
        addBiome("mossy_battlefield", "Mossy Battlefield"); // 5
        addBiome("sunlit_flower_field", "Sunlit Flower Field"); // 6
        addBiome("misty_lakeside", "Misty Lakeside"); // 7
        addBiome("gray_rock_hills", "Gray Rock Hills"); // 8
        addBiome("dragon_claw_gorge", "Dragon Claw Gorge"); // 9
        addBiome("red_earth_wasteland", "Red Earth Wasteland"); // 10
        addBiome("old_highway_ruins", "Old Highway Ruins"); // 11
        addBiome("millennium_tree_forest", "Millennium Tree Forest"); // 12
        addBiome("silver_snowfield", "Silver Snowfield"); // 13
        addBiome("scorched_lava_plateau", "Scorched Lava Plateau"); // 14
        addBiome("sandstorm_desert", "Sandstorm Desert"); // 15
        addBiome("twilight_marsh", "Twilight Marsh"); // 16
        addBiome("crystal_caverns", "Crystal Caverns"); // 17
        addBiome("storm_cliffs", "Storm Cliffs"); // 18
        addBiome("sunken_capital", "Sunken Capital"); // 19
        addBiome("poison_mushroom_sea", "Poison Mushroom Sea"); // 20
        addBiome("dragon_bone_valley", "Dragon Bone Valley"); // 21
        addBiome("floating_rock_highlands", "Floating Rock Highlands"); // 22
        addBiome("cursed_black_forest", "Cursed Black Forest"); // 23
        addBiome("stargazing_plateau", "Stargazing Plateau"); // 24
        addBiome("iron_vein_mines", "Iron Vein Mines"); // 25
        addBiome("rusted_ancient_ruins", "Rusted Ancient Ruins"); // 26
        addBiome("great_beast_plains", "Great Beast Plains"); // 27
        addBiome("waterfall_gorge", "Waterfall Gorge"); // 28
        addBiome("dragon_peaks", "Dragon Peaks"); // 29
        addBiome("kingdom_of_the_dead", "Kingdom of the Dead"); // 30
        addBiome("sky_temples", "Sky Temples"); // 31
        addBiome("eastern_rough_coast", "Eastern Rough Coast"); // 32
        addBiome("calamity_scar", "Calamity Scar"); // 33
        addBiome("timeless_forest", "Timeless Forest"); // 34
        addBiome("mana_storm_wastes", "Mana Storm Wastes"); // 35
        addBiome("obsidian_volcano", "Obsidian Volcano"); // 36
        addBiome("great_ancient_ruins", "Great Ancient Ruins"); // 37
        addBiome("world_tree_roots", "World Tree Roots"); // 38
        addBiome("permafrost_ice_castle", "Permafrost Ice Castle"); // 39
        addBiome("laboratory_valley", "Laboratory Valley"); // 40
        addBiome("black_sand_desert", "Black Sand Desert"); // 41
        addBiome("acid_marsh", "Acid Marsh"); // 42
        addBiome("eternal_night_forest", "Eternal Night Forest"); // 43
        addBiome("thunder_highlands", "Thunder Highlands"); // 44
        addBiome("corroded_land", "Corroded Land"); // 45
        addBiome("giant_bone_wastes", "Giant Bone Wastes"); // 46
        addBiome("maelstrom_coast", "Maelstrom Coast"); // 47
        addBiome("red_moon_plains", "Red Moon Plains"); // 48
        addBiome("breathing_earth", "Breathing Earth"); // 49
        addBiome("calamity_slumber", "Land of the Sleeping Calamity"); // 50
        addBiome("sky_wall_range", "Sky Wall Range"); // 山脈（メディアナを囲む山・大陸を南北に分ける山）

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

    /** このMODのバイオーム（フィールド）の名前を追加する。 */
    private void addBiome(String id, String name) {
        add("biome." + MOD_ID + "." + id, name);
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
