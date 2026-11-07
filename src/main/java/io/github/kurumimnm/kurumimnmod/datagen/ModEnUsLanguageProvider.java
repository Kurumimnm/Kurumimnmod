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
        add("commands." + MOD_ID + ".area.info", "Field %s: %s - Area %s (Recommended Lv %s)");
        add("commands." + MOD_ID + ".area.not_field_world", "This world does not use the Kurumimnmod field map.");
        add("commands." + MOD_ID + ".area.mountains", "(Inside the mountain range)");
        add("commands." + MOD_ID + ".towns.entry", "Town %s: X %s / Z %s");

        // ---- ワールド：アストラリアのフィールド・街・エリアの名前（設計書 第1部。仮名） ----
        addBiome("young_leaf_plains", "Young Leaf Plains"); // FD01 若葉の草原
        addBiome("misty_marsh_canyon", "Misty Marsh Canyon"); // FD02 霧沼の大峡谷
        addBiome("ash_wastes", "Ash Wastes"); // FD03 灰の荒野
        addBiome("great_forest", "Great Forest"); // FD04 大樹海
        addBiome("sea_breeze_hills", "Sea Breeze Hills"); // FD05 海風の丘陵
        addBiome("great_desert", "Great Desert"); // FD06 大砂漠
        addBiome("crimson_volcano", "Crimson Volcanic Belt"); // FD07 紅蓮火山帯
        addBiome("machine_wastes", "Machine Wastes"); // FD08 機械の荒地・錆の谷
        addBiome("zenith_mountains", "Zenith Mountains"); // FD09 天頂山脈・浮遊島
        addBiome("northern_peaks", "Northern Peaks"); // FD10 北嶺山脈・永久氷河
        addBiome("black_rock_plateau", "Black Rock Plateau"); // FD11 黒岩台地・大裂溝
        addBiome("starfall_highlands", "Starfall Highlands"); // FD12 星降りの高原
        addBiome("scarlet_plateau", "Scarlet Plateau"); // SCARLET 緋色台地
        addBiome("eldion", "Old Capital Eldion"); // E00 旧都エルディオン
        addBiome("astral_sea", "Astral Sea"); // SEA アストラリアの海
        add("town." + MOD_ID + ".fastoria", "Fastoria"); // C01 ファストリア村
        add("town." + MOD_ID + ".secantis", "Secantis"); // C02 セカンティス
        add("town." + MOD_ID + ".thirdiel", "Thirdiel"); // C03 サーディエル
        add("town." + MOD_ID + ".fourthria", "Fourthria"); // C04 フォースリア
        add("town." + MOD_ID + ".fifterion", "Fifterion"); // C05 フィフテリオン
        add("town." + MOD_ID + ".sixtera", "Sixtera"); // C06 シクステラ
        add("town." + MOD_ID + ".sevantia", "Sevantia"); // C07 セヴァンティア
        add("town." + MOD_ID + ".eightval", "Eightval"); // C08 エイトヴァル
        add("town." + MOD_ID + ".ninelis", "Ninelis"); // C09 ナイネリス
        add("town." + MOD_ID + ".tenzaria", "Tenzaria"); // C10 テンザリア
        add("town." + MOD_ID + ".elevana", "Elevana"); // C11 イレヴァーナ
        add("town." + MOD_ID + ".mill_post", "Mill Post"); // V01 ミル宿場
        add("town." + MOD_ID + ".bridgewarden", "Bridgewarden Village"); // V02 橋守の村
        add("town." + MOD_ID + ".watermill", "Watermill Village"); // V03 水車の村
        add("town." + MOD_ID + ".red_clay_post", "Red Clay Post"); // V04 赤土の宿場
        add("town." + MOD_ID + ".caravanserai", "Caravanserai"); // V05 隊商宿
        add("town." + MOD_ID + ".east_ash_watch", "East Ash Watch"); // V06 東灰原の監視村
        add("town." + MOD_ID + ".west_ash_pilgrim", "West Ash Pilgrim Lodge"); // V07 西灰原の巡礼小屋
        add("town." + MOD_ID + ".woodcutter", "Woodcutter Village"); // V08 樵の村
        add("town." + MOD_ID + ".rift_north_hut", "Rift North Lodge"); // V09 裂溝北の山小屋
        add("town." + MOD_ID + ".hot_spring", "Hot Spring Village"); // V10 湯けむりの村
        add("town." + MOD_ID + ".beach_fishing", "Beach Fishing Village"); // V11 浜の漁村
        add("town." + MOD_ID + ".starfall", "Starfall Village"); // V12 星降りの村
        add("town." + MOD_ID + ".ash_wall_fort", "Ash Wall Watchfort"); // F01 灰壁監視砦
        add("town." + MOD_ID + ".north_ridge_fort", "North Ridge Fort"); // F02 北嶺砦
        add("town." + MOD_ID + ".crimson_fort", "Crimson Fort"); // F03 紅蓮砦
        add("town." + MOD_ID + ".sand_sea_fort", "Sand Sea Fort"); // F04 砂海砦
        add("town." + MOD_ID + ".rift_lift_base", "Rift Lift Base"); // F05 裂溝昇降拠点
        add("town." + MOD_ID + ".tidewatch_fort", "Tidewatch Fort"); // F06 潮見砦
        add("town." + MOD_ID + ".sky_observatory", "Firmament Observatory"); // F07 天穹観測基地
        add("town." + MOD_ID + ".rust_valley_fort", "Rust Valley Fort"); // F08 錆谷砦
        addArea("young_leaf_plains", 1, "Mill Post Town"); // FD01-1 ミル宿場
        addArea("young_leaf_plains", 2, "Young Leaf Meadow"); // FD01-2 若葉の草原
        addArea("young_leaf_plains", 3, "Eastern Hills"); // FD01-3 東の丘陵
        addArea("young_leaf_plains", 4, "Before the Scarlet Gate"); // FD01-4 緋の関所前
        addArea("young_leaf_plains", 5, "Mossy Cave"); // FD01-5 苔むした洞窟
        addArea("misty_marsh_canyon", 1, "Bridgewarden Village"); // FD02-1 橋守の村
        addArea("misty_marsh_canyon", 2, "East Cliff Lookout"); // FD02-2 東崖の見晴らし台
        addArea("misty_marsh_canyon", 3, "Misty Marsh Bridge"); // FD02-3 霧沼大橋
        addArea("misty_marsh_canyon", 4, "West Cliff Old Fort"); // FD02-4 西崖の古砦
        addArea("misty_marsh_canyon", 5, "Waterfall Plateau"); // FD02-5 滝口の台地
        addArea("misty_marsh_canyon", 6, "Crimson River Falls"); // FD02-6 紅河の落ち口
        addArea("misty_marsh_canyon", 7, "Plunge Pool Marsh"); // FD02-7 滝つぼの沼
        addArea("misty_marsh_canyon", 8, "Great Marsh Under the Bridge"); // FD02-8 橋下の大沼
        addArea("misty_marsh_canyon", 9, "Poison Mist Colony"); // FD02-9 毒霧の群生地
        addArea("misty_marsh_canyon", 10, "Sunken Ancient Machine"); // FD02-10 沈んだ古代機械
        addArea("misty_marsh_canyon", 11, "Crimson River Plunge Pool"); // FD02-11 紅河の滝つぼ
        addArea("misty_marsh_canyon", 12, "Estuary Wetland"); // FD02-12 河口の湿原
        addArea("ash_wastes", 1, "Ash Wall Watchfort"); // FD03-1 灰壁監視砦
        addArea("ash_wastes", 2, "Ruined Fort of the Ash Fields"); // FD03-2 灰原の廃砦
        addArea("ash_wastes", 3, "Western Pilgrim Road"); // FD03-3 西の巡礼路
        addArea("ash_wastes", 4, "Edge of the Northern Battlefield"); // FD03-4 北の戦場跡外縁
        addArea("ash_wastes", 5, "Eastern Wreckage Fields"); // FD03-5 東の残骸原
        addArea("ash_wastes", 6, "Southeastern Ash Hills"); // FD03-6 南東の灰丘
        addArea("ash_wastes", 7, "Ancient Exhaust Duct (West)"); // FD03-7 古代排気路（西）
        addArea("ash_wastes", 8, "Ancient Exhaust Duct (East)"); // FD03-8 古代排気路（東）
        addArea("great_forest", 1, "Thirdiel Treetop Town"); // FD04-1 サーディエル樹上街
        addArea("great_forest", 2, "Canopy Corridor"); // FD04-2 天蓋の回廊
        addArea("great_forest", 3, "Giant Tree Lookout"); // FD04-3 巨木の見張り台
        addArea("great_forest", 4, "Ancient Treetops"); // FD04-4 古樹の梢
        addArea("great_forest", 5, "Jade River Ferry"); // FD04-5 翠河の渡し
        addArea("great_forest", 6, "Root Labyrinth Entrance"); // FD04-6 樹根の迷宮入口
        addArea("great_forest", 7, "Moss Wetland"); // FD04-7 苔の湿地
        addArea("great_forest", 8, "Decayed Spirit Shrine"); // FD04-8 朽ちた精霊廟
        addArea("great_forest", 9, "Before the Green Cradle"); // FD04-9 緑の揺籃前
        addArea("great_forest", 10, "Foot of the Ancient Trees"); // FD04-10 古樹の根元
        addArea("great_forest", 11, "Woodcutter Clearing"); // FD04-11 樵の切り開き
        addArea("great_forest", 12, "Western Mist Woods"); // FD04-12 西の霧林
        addArea("sea_breeze_hills", 1, "Elevana Harbor"); // FD05-1 イレヴァーナ港
        addArea("sea_breeze_hills", 2, "Tidewatch Cape"); // FD05-2 潮見の岬
        addArea("sea_breeze_hills", 3, "Windmill Hill"); // FD05-3 風車の丘
        addArea("sea_breeze_hills", 4, "White Sand Beach"); // FD05-4 白砂の浜
        addArea("sea_breeze_hills", 5, "Seabird Cliffs"); // FD05-5 海鳥の断崖
        addArea("sea_breeze_hills", 6, "Murmuring Cove"); // FD05-6 潮騒の入り江
        addArea("sea_breeze_hills", 7, "Hillside Pastures"); // FD05-7 丘陵の牧草地
        addArea("sea_breeze_hills", 8, "Shipwreck Graveyard"); // FD05-8 沈没船の墓場
        addArea("sea_breeze_hills", 9, "Coral Shallows"); // FD05-9 珊瑚の浅瀬
        addArea("sea_breeze_hills", 10, "Before the Blue Cradle"); // FD05-10 青の揺り籠前
        addArea("great_desert", 1, "Caravanserai"); // FD06-1 隊商宿
        addArea("great_desert", 2, "Outskirts of Tenzaria"); // FD06-2 テンザリア外縁
        addArea("great_desert", 3, "Red Dunes"); // FD06-3 赤い砂丘
        addArea("great_desert", 4, "Valley of the Royal Tombs"); // FD06-4 王墓の谷
        addArea("great_desert", 5, "Great Scorching Sand Sea"); // FD06-5 灼熱の大砂海
        addArea("great_desert", 6, "Mirage Tower"); // FD06-6 蜃気楼の塔
        addArea("great_desert", 7, "Sand Sea Fort"); // FD06-7 砂海砦
        addArea("great_desert", 8, "Eastern Salt Flats"); // FD06-8 東の岩塩平原
        addArea("great_desert", 9, "The Buried Eye"); // FD06-9 埋もれた眼
        addArea("great_desert", 10, "Under-Sand Corridor"); // FD06-10 砂下の回廊
        addArea("great_desert", 11, "Royal Tomb Depths"); // FD06-11 王墓の地下
        addArea("great_desert", 12, "Bottom of the Quicksand"); // FD06-12 流砂の底
        addArea("crimson_volcano", 1, "Hot Spring Village"); // FD07-1 湯けむりの村
        addArea("crimson_volcano", 2, "Before the Crimson Heart"); // FD07-2 紅の心臓前
        addArea("crimson_volcano", 3, "Ashfall Slopes"); // FD07-3 灰降る斜面
        addArea("crimson_volcano", 4, "Crimson Fort"); // FD07-4 紅蓮砦
        addArea("crimson_volcano", 5, "Obsidian Temple"); // FD07-5 黒曜の神殿
        addArea("crimson_volcano", 6, "The Great Crater"); // FD07-6 大火口
        addArea("crimson_volcano", 7, "Lava Mine"); // FD07-7 溶岩坑道
        addArea("crimson_volcano", 8, "Red-Hot Lava Tube"); // FD07-8 赤熱の溶岩洞
        addArea("crimson_volcano", 9, "Fumarole Maze"); // FD07-9 噴気孔の迷路
        addArea("crimson_volcano", 10, "Crater Underground Lake"); // FD07-10 火口の地下湖
        addArea("machine_wastes", 1, "Outskirts of Fifterion"); // FD08-1 フィフテリオン外縁
        addArea("machine_wastes", 2, "Silent Battery"); // FD08-2 沈黙の砲台
        addArea("machine_wastes", 3, "Rust Valley Fort"); // FD08-3 錆谷砦
        addArea("machine_wastes", 4, "Rust Valley"); // FD08-4 錆の谷
        addArea("machine_wastes", 5, "Gear Mesas"); // FD08-5 歯車のメサ群
        addArea("machine_wastes", 6, "Ancient Railway Terminus"); // FD08-6 古代軌道の終着駅
        addArea("machine_wastes", 7, "Seventh Manufactory"); // FD08-7 第七製造工廠
        addArea("machine_wastes", 8, "Old Mine Shafts"); // FD08-8 旧坑道群
        addArea("machine_wastes", 9, "Rusted Factory Depths"); // FD08-9 錆の工場跡・地下
        addArea("machine_wastes", 10, "Eastern Abandoned Mine"); // FD08-10 東の廃鉱
        addArea("zenith_mountains", 1, "Sevantia"); // FD09-1 セヴァンティア
        addArea("zenith_mountains", 2, "Floating Island Ruins"); // FD09-2 浮遊島遺跡群
        addArea("zenith_mountains", 3, "Stargazing Tower"); // FD09-3 星見の塔
        addArea("zenith_mountains", 4, "Isles Above the Clouds"); // FD09-4 雲海の浮き島
        addArea("zenith_mountains", 5, "Firmament Nest"); // FD09-5 天穹の巣
        addArea("zenith_mountains", 6, "Sixtera Castle Gate"); // FD09-6 シクステラ城門
        addArea("zenith_mountains", 7, "Great Wind Cave"); // FD09-7 風穴の大洞
        addArea("zenith_mountains", 8, "Firmament Observatory"); // FD09-8 天穹観測基地
        addArea("zenith_mountains", 9, "Sky Ridge Suspension Bridge"); // FD09-9 天嶺道の吊り橋
        addArea("zenith_mountains", 10, "Eastern Snow Ridge"); // FD09-10 東の雪稜
        addArea("zenith_mountains", 11, "Eagle Ledge"); // FD09-11 鷲の岩棚
        addArea("zenith_mountains", 12, "Shoulder of the Zenith Peak"); // FD09-12 天頂峰の肩
        addArea("northern_peaks", 1, "Rift North Lodge"); // FD10-1 裂溝北の山小屋
        addArea("northern_peaks", 2, "Outskirts of Fourthria"); // FD10-2 フォースリア外縁
        addArea("northern_peaks", 3, "North Ridge Fort"); // FD10-3 北嶺砦
        addArea("northern_peaks", 4, "Eternal Glacier"); // FD10-4 永久氷河
        addArea("northern_peaks", 5, "Before the White Coffin"); // FD10-5 白の棺前
        addArea("northern_peaks", 6, "Frozen Battleship"); // FD10-6 凍れる戦艦
        addArea("northern_peaks", 7, "Western Snowfield"); // FD10-7 西の雪原
        addArea("northern_peaks", 8, "Ice Crystal Cave"); // FD10-8 氷晶洞
        addArea("northern_peaks", 9, "Bottom of the Crevasse"); // FD10-9 クレバスの底
        addArea("northern_peaks", 10, "Subglacial Waterway"); // FD10-10 氷河下の水脈
        addArea("northern_peaks", 11, "Ice Harbor Cave Gate"); // FD10-11 氷港の洞門
        addArea("black_rock_plateau", 1, "Ninelis"); // FD11-1 ナイネリス
        addArea("black_rock_plateau", 2, "Rift Lift Base"); // FD11-2 裂溝昇降拠点
        addArea("black_rock_plateau", 3, "Inverted Tower"); // FD11-3 逆さ塔
        addArea("black_rock_plateau", 4, "Great Rift Bridge"); // FD11-4 裂溝大橋
        addArea("black_rock_plateau", 5, "Basalt Column Plateau"); // FD11-5 玄武岩の柱状台地
        addArea("black_rock_plateau", 6, "Falling Star Plunge Pool"); // FD11-6 落星の滝つぼ
        addArea("black_rock_plateau", 7, "The Black Well"); // FD11-7 黒の井戸
        addArea("black_rock_plateau", 8, "Abyssal Corridor"); // FD11-8 深淵回廊
        addArea("black_rock_plateau", 9, "Western Rift Floor"); // FD11-9 裂溝の西底
        addArea("black_rock_plateau", 10, "Bottom of the Abyss"); // FD11-10 深淵の底
        addArea("starfall_highlands", 1, "Starfall Village"); // FD12-1 星降りの村
        addArea("starfall_highlands", 2, "Great Meteor Crater"); // FD12-2 大きな隕石孔
        addArea("starfall_highlands", 3, "Northern Star Lookout"); // FD12-3 北の星見台
        addArea("starfall_highlands", 4, "Southern Cliffs"); // FD12-4 南の断崖
        addArea("starfall_highlands", 5, "Eastern Meteoric Iron Fields"); // FD12-5 東の隕鉄原
        addArea("starfall_highlands", 6, "Hollow Beneath the Crater"); // FD12-6 隕石孔の下の空洞
        addArea("starfall_highlands", 7, "Star Shard Vein"); // FD12-7 星の欠片の鉱脈
        addArea("scarlet_plateau", 1, "Outskirts of Secantis"); // SCARLET-1 セカンティス外縁
        addArea("scarlet_plateau", 2, "The Scarlet Capital"); // SCARLET-2 緋の都
        addArea("scarlet_plateau", 3, "Red Clay Hills"); // SCARLET-3 赤土の丘
        addArea("scarlet_plateau", 4, "Red Clay Post Town"); // SCARLET-4 赤土の宿場
        addArea("eldion", 1, "Inside the Veil"); // E00-1 帳の内側
        addArea("eldion", 2, "The Central Tower"); // E00-2 中央塔
        addArea("eldion", 3, "The Great Crater"); // E00-3 大クレーター
        addArea("eldion", 4, "Sunken Streets"); // E00-4 沈んだ街並み
        addArea("eldion", 5, "Western Ruins"); // E00-5 西の廃墟
        add("hud." + MOD_ID + ".location.area", "Area %s - Recommended Lv %s");
        add("hud." + MOD_ID + ".location.area_named", "Area %s %s - Recommended Lv %s");
        add("commands." + MOD_ID + ".flag.granted", "Granted progress flag %2$s to %1$s player(s).");
        add("commands." + MOD_ID + ".flag.revoked", "Revoked progress flag %2$s from %1$s player(s).");

        // ---- ワールド：街の種類 ----
        add("town_type." + MOD_ID + ".hidden_village", "Hidden Village");
        add("town_type." + MOD_ID + ".village", "Village");
        add("town_type." + MOD_ID + ".post_town", "Post Town");
        add("town_type." + MOD_ID + ".mining_town", "Mining Town");
        add("town_type." + MOD_ID + ".town", "Town");
        add("town_type." + MOD_ID + ".port_town", "Port Town");
        add("town_type." + MOD_ID + ".castle_town", "Castle Town");
        add("town_type." + MOD_ID + ".temple_city", "Temple City");
        add("town_type." + MOD_ID + ".city", "City");
        add("town_type." + MOD_ID + ".frontline_city", "Frontline City");

        // ---- ワールド：場所の表示・ゲート・地図 ----
        add("hud." + MOD_ID + ".location.new", "- New Location -");
        add("message." + MOD_ID + ".gate.blocked", "You cannot go further yet. Advance the story to open the way.");
        add("commands." + MOD_ID + ".explore.all", "Marked every town and area as visited.");
        add("commands." + MOD_ID + ".explore.reset", "Cleared the exploration record.");
        add("key." + MOD_ID + ".open_map", "Open Map");
        add("screen." + MOD_ID + ".map", "Map");
        add("screen." + MOD_ID + ".map.loading", "Drawing the map...");
        add("screen." + MOD_ID + ".map.hover", "%s (%s)");
        add("screen." + MOD_ID + ".map.unexplored", "Unexplored");
        add("screen." + MOD_ID + ".map.coords", "X %s / Z %s");
        add("screen." + MOD_ID + ".map.help", "Drag: Move   Wheel: Zoom   M: Close");
        add("screen." + MOD_ID + ".map.teleport.title", "Teleport?");
        add("screen." + MOD_ID + ".map.teleport.message", "Teleport to %s (X %s / Z %s)?");
        add("screen." + MOD_ID + ".map.help_op", "Drag: Move   Wheel: Zoom   Right-click: Teleport (OP)   M: Close");

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

    /** エリアの名前を追加する（area.kurumimnmod.<フィールド>.<番号>）。 */
    private void addArea(String field, int area, String name) {
        add("area." + MOD_ID + "." + field + "." + area, name);
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
