package io.github.kurumimnm.kurumimnmod.enemy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jspecify.annotations.Nullable;

/**
 * 敵のステータス（データマップ {@code kurumimnmod:enemy_stats}）。
 * <p>
 * 敵ごとの数値はコードに直書きせず、データパックの JSON で設定する（サーバー管理者も上書き可）。
 * <b>どの項目も省略可能</b>。省略した項目はデフォルト値（下表）になる。
 * HP・D・移動速度などは、省略するとエンティティのコード側の初期値（ModConstants）のまま。
 * <pre>
 * data/kurumimnmod/data_maps/entity_type/enemy_stats.json
 * {
 *   "values": {
 *     "kurumimnmod:kobold": {
 *       "hp": 30,               // 体力
 *       "damage": 4,            // 通常攻撃のダメージ（D）
 *       "vit": 3,               // 防御力（物理）
 *       "mdf": 0,               // 魔法防御
 *       "mnd": 2,               // 精神力（デバフ耐性）
 *       "attack_cooldown": 1.0, // 通常攻撃の CT（秒）
 *       "skills": {             // スキルごとのダメージ・CT（秒）
 *         "leap": { "damage": 2, "cooldown": 5.0 }
 *       },
 *       "elements": { "ice": 1.5, "fire": 0.5 },  // 属性ごとの被ダメ倍率（1.5 = 弱点）
 *       "flinch_resist": 5      // このダメージ未満の攻撃ではノックバックしない
 *     }
 *   }
 * }
 * </pre>
 *
 * <h2>各ステータスの効果（処理は {@link EnemyStatEvents}）</h2>
 * <table>
 *   <tr><th>項目</th><th>既定値</th><th>効果</th></tr>
 *   <tr><td>hp</td><td>コードの値</td><td>最大 HP（攻撃力 1 のプレイヤーなら hp 回殴ると倒れる）</td></tr>
 *   <tr><td>damage</td><td>コードの値</td><td>通常攻撃のダメージ（バニラの攻撃力属性）</td></tr>
 *   <tr><td>vit</td><td>0</td><td>物理の被ダメ：D ≦ VIT なら 1、超えたら D − VIT × 軽減率（Config）</td></tr>
 *   <tr><td>mdf</td><td>0</td><td>魔法の被ダメ：VIT と同じ式（魔法ダメージは VIT ではなく MDF で判定）</td></tr>
 *   <tr><td>mnd</td><td>0</td><td>デバフ：付与してきたプレイヤーの知力 ≦ MND なら無効</td></tr>
 *   <tr><td>attack_cooldown</td><td>1.0 秒</td><td>通常攻撃の CT（バニラの近接攻撃と同じ 1 秒）</td></tr>
 *   <tr><td>skills.&lt;id&gt;.damage / cooldown</td><td>コードの値</td><td>スキルのダメージ・CT（秒）</td></tr>
 *   <tr><td>elements.&lt;属性&gt;</td><td>1.0</td><td>属性ダメージの倍率（ダメージタイプタグ kurumimnmod:element/&lt;属性&gt;）</td></tr>
 *   <tr><td>flinch_resist</td><td>0</td><td>1 回のダメージがこの値未満ならノックバックしない</td></tr>
 *   <tr><td>knockback_resist</td><td>コードの値</td><td>バニラのノックバック耐性（0.0〜1.0）</td></tr>
 *   <tr><td>move_speed</td><td>コードの値</td><td>移動速度（バニラの属性値。ゾンビ 0.23）</td></tr>
 *   <tr><td>follow_range</td><td>コードの値</td><td>索敵範囲（ブロック）</td></tr>
 * </table>
 * 撃破経験値は別のデータマップ（{@code kurumimnmod:experience}）で設定する。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public record EnemyStats(
        Optional<Double> hp,
        Optional<Double> damage,
        int vit,
        int mdf,
        int mnd,
        Optional<Double> attackCooldown,
        Map<String, SkillStats> skills,
        Map<String, Double> elements,
        double flinchResist,
        Optional<Double> knockbackResist,
        Optional<Double> moveSpeed,
        Optional<Double> followRange) {

    /** 設定されていない敵（すべてデフォルト値）。 */
    public static final EnemyStats NONE = new EnemyStats(Optional.empty(), Optional.empty(), 0, 0, 0,
            Optional.empty(), Map.of(), Map.of(), 0.0, Optional.empty(), Optional.empty(), Optional.empty());

    /**
     * スキル 1 つ分の設定。どちらも省略するとコード側の値（Config）を使う。
     *
     * @param damage   スキルのダメージ（D）
     * @param cooldown スキルの CT（秒）
     */
    public record SkillStats(Optional<Double> damage, Optional<Double> cooldown) {
        public static final Codec<SkillStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("damage").forGetter(SkillStats::damage),
                Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("cooldown").forGetter(SkillStats::cooldown)
        ).apply(instance, SkillStats::new));
    }

    /** 属性名（タグ kurumimnmod:element/&lt;属性名&gt; のパスとして使える文字だけ）。 */
    private static final Codec<String> ELEMENT_NAME = Codec.STRING.validate(name -> Identifier.isValidPath(name)
            ? DataResult.success(name)
            : DataResult.error(() -> "Invalid element name: " + name));

    private static final Codec<Double> NON_NEGATIVE = Codec.doubleRange(0, Double.MAX_VALUE);

    public static final Codec<EnemyStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(1, Double.MAX_VALUE).optionalFieldOf("hp").forGetter(EnemyStats::hp),
            NON_NEGATIVE.optionalFieldOf("damage").forGetter(EnemyStats::damage),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("vit", 0).forGetter(EnemyStats::vit),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("mdf", 0).forGetter(EnemyStats::mdf),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("mnd", 0).forGetter(EnemyStats::mnd),
            NON_NEGATIVE.optionalFieldOf("attack_cooldown").forGetter(EnemyStats::attackCooldown),
            Codec.unboundedMap(Codec.STRING, SkillStats.CODEC).optionalFieldOf("skills", Map.of()).forGetter(EnemyStats::skills),
            Codec.unboundedMap(ELEMENT_NAME, NON_NEGATIVE).optionalFieldOf("elements", Map.of()).forGetter(EnemyStats::elements),
            NON_NEGATIVE.optionalFieldOf("flinch_resist", 0.0).forGetter(EnemyStats::flinchResist),
            Codec.doubleRange(0, 1).optionalFieldOf("knockback_resist").forGetter(EnemyStats::knockbackResist),
            NON_NEGATIVE.optionalFieldOf("move_speed").forGetter(EnemyStats::moveSpeed),
            NON_NEGATIVE.optionalFieldOf("follow_range").forGetter(EnemyStats::followRange)
    ).apply(instance, EnemyStats::new));

    /** データマップの種類（ID: kurumimnmod:enemy_stats）。 */
    public static final DataMapType<EntityType<?>, EnemyStats> DATA_MAP = DataMapType.builder(
            Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "enemy_stats"), Registries.ENTITY_TYPE, CODEC).build();

    // =====================================================================
    // 取得
    // =====================================================================

    /** エンティティの種類から設定を取得する（未設定なら null）。 */
    public static @Nullable EnemyStats get(EntityType<?> type) {
        return type.builtInRegistryHolder().getData(DATA_MAP);
    }

    /** エンティティから設定を取得する（未設定なら {@link #NONE}）。 */
    public static EnemyStats of(Entity entity) {
        @Nullable EnemyStats stats = get(entity.getType());
        return stats == null ? NONE : stats;
    }

    /** 属性ダメージの倍率（設定がなければ 1.0）。 */
    public double elementMultiplier(String element) {
        return elements.getOrDefault(element, 1.0);
    }

    // =====================================================================
    // CT・スキル（敵のコードから呼ぶ）
    // =====================================================================

    /** 秒 → tick（最低 1 tick）。 */
    private static int toTicks(double seconds) {
        return Math.max(1, (int) Math.round(seconds * 20));
    }

    /** 通常攻撃の CT（tick）。JSON で未設定なら fallbackTicks。 */
    public static int attackCooldownTicks(Entity entity, int fallbackTicks) {
        return of(entity).attackCooldown.map(EnemyStats::toTicks).orElse(fallbackTicks);
    }

    /** スキルの CT（tick）。JSON で未設定なら fallbackTicks（Config の値など）。 */
    public static int skillCooldownTicks(Entity entity, String skillId, int fallbackTicks) {
        @Nullable SkillStats skill = of(entity).skills.get(skillId);
        return skill == null ? fallbackTicks : skill.cooldown().map(EnemyStats::toTicks).orElse(fallbackTicks);
    }

    /** スキルのダメージ。JSON で未設定なら fallback（Config の値など）。 */
    public static float skillDamage(Entity entity, String skillId, double fallback) {
        @Nullable SkillStats skill = of(entity).skills.get(skillId);
        return (float) (skill == null ? fallback : skill.damage().orElse(fallback));
    }

    @SubscribeEvent
    static void onRegisterDataMapTypes(RegisterDataMapTypesEvent event) {
        event.register(DATA_MAP);
    }
}
