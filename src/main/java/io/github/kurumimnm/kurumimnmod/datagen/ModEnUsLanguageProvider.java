package io.github.kurumimnm.kurumimnmod.datagen;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
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

        // ---- ブロック ----

        // ---- エンティティ ----

        // ---- キー割り当て ----
        add("key.category." + MOD_ID + ".main", "Kurumimnmod");
        add("key." + MOD_ID + ".open_status", "Open Status Screen");

        // ---- ステータス画面 ----
        add("screen." + MOD_ID + ".status", "Status");
        add("screen." + MOD_ID + ".status.level", "Level %s");
        add("screen." + MOD_ID + ".status.next_level", "Next level: %s%%");
        add("screen." + MOD_ID + ".status.hp", "HP %s / %s");
        add("screen." + MOD_ID + ".status.mp", "MP %s / %s");
        add("screen." + MOD_ID + ".status.points", "Unspent Points: %s");

        // ---- ステータス（StatType） ----
        addStat(StatType.STR, "STR", "Strength: increases physical attack power.");
        addStat(StatType.VIT, "VIT", "Vitality: increases max HP.");
        addStat(StatType.INT, "INT", "Intelligence: increases max MP and magic power.");
        addStat(StatType.DEX, "DEX", "Dexterity: increases accuracy and critical rate.");
        addStat(StatType.AGI, "AGI", "Agility: increases movement and attack speed.");

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
        add(MOD_ID + ".configuration.enemyHealthBar", "Enemy Health Bar");
        add(MOD_ID + ".configuration.enabled", "Enabled");
        add(MOD_ID + ".configuration.range", "Display Range");
        add(MOD_ID + ".configuration.hostileOnly", "Hostile Mobs Only");
    }

    /** ステータスの略称と説明文をまとめて追加する。 */
    private void addStat(StatType type, String name, String description) {
        add(type.nameKey(), name);
        add(type.descriptionKey(), description);
    }
}
