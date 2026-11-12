package io.github.kurumimnm.kurumimnmod.job;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 職業 1 つの設計（データパックのレジストリ {@code kurumimnmod:job}）。
 * <b>数値はすべてこの JSON で調整する</b>（コードの変更は不要）。どの項目も省略可能。
 * <pre>
 * data/&lt;名前空間&gt;/kurumimnmod/job/&lt;名前&gt;.json
 * {
 *   "sort": 10,                       … 選択画面の並び順（小さいほど上）
 *   "icon": "minecraft:iron_sword",   … 選択画面のボタンに出すアイテム
 *   "selectable": true,               … 最初の選択画面に出すか（Lv50 の派生職・上位職は false）
 *   "stats": {                        … ステータスの倍率（省略 = 1.0）
 *     "hp": 1.5, "mp": 1.0, "attack": 1.2, "defense": 1.15, "intelligence": 1.0
 *   },
 *   "per_level": {                    … レベルが 1 上がるごとの上昇量（省略 = 0）
 *     "hp": 5, "mp": 2,
 *     "crit_chance": 0.001            … クリティカル率（0.001 = +0.1%）
 *   },
 *   "weapons": ["kurumimnmod:weapons/sword"],  … 使える武器のタグ（省略 = 制限なし）
 *   "arrow_intelligence": 1.0,        … 矢のダメージに知力の倍率をどれだけ足すか（魔弓士用。省略 = 0）
 *                                         矢のダメージ ×＝ 1 ＋ (知力の魔法倍率 − 1) × この値
 *   "promotion": {                    … 分岐（このレベルになると選択画面が開く）
 *     "level": 50,
 *     "options": ["kurumimnmod:magic_swordsman", "kurumimnmod:swordsman"]  … 自分自身＝そのまま続ける
 *   }
 * }
 * </pre>
 * 【計算式（A 方式）】最終値 ＝（初期値 ＋ 振り分け ＋ 職業のレベル上昇の合計）× 倍率
 * <p>
 * 名前・説明は lang の {@code job.<名前空間>.<名前>}／{@code job.<名前空間>.<名前>.description}。
 */
public record JobDefinition(
        int sort,
        Optional<Identifier> icon,
        boolean selectable,
        Multipliers stats,
        LevelGains perLevel,
        List<TagKey<Item>> weapons,
        double arrowIntelligence,
        Optional<Promotion> promotion) {

    public static final Codec<JobDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("sort", 0).forGetter(JobDefinition::sort),
            Identifier.CODEC.optionalFieldOf("icon").forGetter(JobDefinition::icon),
            Codec.BOOL.optionalFieldOf("selectable", true).forGetter(JobDefinition::selectable),
            Multipliers.CODEC.optionalFieldOf("stats", Multipliers.NONE).forGetter(JobDefinition::stats),
            LevelGains.CODEC.optionalFieldOf("per_level", LevelGains.NONE).forGetter(JobDefinition::perLevel),
            TagKey.codec(Registries.ITEM).listOf().optionalFieldOf("weapons", List.of()).forGetter(JobDefinition::weapons),
            Codec.DOUBLE.optionalFieldOf("arrow_intelligence", 0.0).forGetter(JobDefinition::arrowIntelligence),
            Promotion.CODEC.optionalFieldOf("promotion").forGetter(JobDefinition::promotion)
    ).apply(i, JobDefinition::new));

    public static String nameKey(Identifier id) {
        return "job." + id.getNamespace() + "." + id.getPath();
    }

    public static String descriptionKey(Identifier id) {
        return nameKey(id) + ".description";
    }

    /**
     * この武器を使えるか。武器（タグ {@code kurumimnmod:weapons}）でないアイテム・素手は常に使える。
     * weapons が空なら制限なし。
     */
    public boolean canUseWeapon(ItemStack stack) {
        if (weapons.isEmpty() || stack.isEmpty() || !stack.is(JobTags.WEAPONS)) {
            return true;
        }
        for (TagKey<Item> tag : weapons) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    // =====================================================================
    // 中身の型
    // =====================================================================

    /** ステータスの倍率（1.0 = 変化なし）。 */
    public record Multipliers(double hp, double mp, double attack, double defense, double intelligence) {

        public static final Multipliers NONE = new Multipliers(1, 1, 1, 1, 1);

        public static final Codec<Multipliers> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("hp", 1.0).forGetter(Multipliers::hp),
                Codec.DOUBLE.optionalFieldOf("mp", 1.0).forGetter(Multipliers::mp),
                Codec.DOUBLE.optionalFieldOf("attack", 1.0).forGetter(Multipliers::attack),
                Codec.DOUBLE.optionalFieldOf("defense", 1.0).forGetter(Multipliers::defense),
                Codec.DOUBLE.optionalFieldOf("intelligence", 1.0).forGetter(Multipliers::intelligence)
        ).apply(i, Multipliers::new));
    }

    /** レベルが 1 上がるごとの上昇量。 */
    public record LevelGains(double hp, double mp, double critChance) {

        public static final LevelGains NONE = new LevelGains(0, 0, 0);

        public static final Codec<LevelGains> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("hp", 0.0).forGetter(LevelGains::hp),
                Codec.DOUBLE.optionalFieldOf("mp", 0.0).forGetter(LevelGains::mp),
                Codec.DOUBLE.optionalFieldOf("crit_chance", 0.0).forGetter(LevelGains::critChance)
        ).apply(i, LevelGains::new));
    }

    /** 分岐：level になったら options から 1 つ選ぶ（自分自身を入れると「そのまま続ける」）。 */
    public record Promotion(int level, List<Identifier> options) {

        public static final Codec<Promotion> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("level").forGetter(Promotion::level),
                Identifier.CODEC.listOf().fieldOf("options").forGetter(Promotion::options)
        ).apply(i, Promotion::new));
    }
}
