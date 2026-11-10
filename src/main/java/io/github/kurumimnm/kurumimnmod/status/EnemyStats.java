package io.github.kurumimnm.kurumimnmod.status;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import io.github.kurumimnm.kurumimnmod.registry.ModTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jspecify.annotations.Nullable;

/**
 * 敵（モブ・ボス）専用のステータス（データマップ）。Notion「敵情報」の敵ステータス。
 * <p>
 * 値はコードに直書きせず、データパックの JSON で敵ごとに設定する。書いていない項目は 0（＝効果なし）。
 * <pre>
 * data/kurumimnmod/data_maps/entity_type/enemy_stats.json
 * {
 *   "values": {
 *     "kurumimnmod:kobold_lord": { "vitality": 10 }
 *   }
 * }
 * </pre>
 * 今あるステータス：
 * <ul>
 *   <li><b>VIT（vitality・防御力）</b>：プレイヤーから受けるダメージを減らす（減算型）。
 *     <ul>
 *       <li>ダメージ ≦ VIT → 最低ダメージ（Config vitMinDamage、初期値 1）</li>
 *       <li>ダメージ ＞ VIT → max(最低ダメージ, ダメージ − VIT)</li>
 *     </ul>
 *     防具などの計算が終わったあとの値にかける。魔法ダメージにもかけるかは Config vitAppliesToMagic。</li>
 * </ul>
 * MND・RES など他の敵ステータスは、ここに項目（optionalFieldOf）を足していく。
 *
 * @param vitality VIT（防御力）。0 なら軽減なし
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public record EnemyStats(int vitality) {

    /** 何も設定されていない敵。 */
    public static final EnemyStats NONE = new EnemyStats(0);

    public static final Codec<EnemyStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("vitality", 0).forGetter(EnemyStats::vitality)
    ).apply(instance, EnemyStats::new));

    /** データマップの種類（ID: kurumimnmod:enemy_stats）。 */
    public static final DataMapType<EntityType<?>, EnemyStats> DATA_MAP = DataMapType.builder(
            Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "enemy_stats"), Registries.ENTITY_TYPE, CODEC).build();

    /** エンティティの種類から敵ステータスを取得する（未設定なら {@link #NONE}）。 */
    public static EnemyStats of(EntityType<?> type) {
        @Nullable EnemyStats data = type.builtInRegistryHolder().getData(DATA_MAP);
        return data == null ? NONE : data;
    }

    /**
     * VIT による軽減（減算型）。
     *
     * @param damage    軽減前のダメージ
     * @param vitality  VIT
     * @param minDamage 最低ダメージ（元のダメージがこれより小さければ元のまま）
     */
    public static float applyVitality(float damage, int vitality, float minDamage) {
        if (vitality <= 0 || damage <= 0) {
            return damage;
        }
        float floor = Math.min(minDamage, damage);
        if (damage <= vitality) {
            return floor;
        }
        return Math.max(floor, damage - vitality);
    }

    @SubscribeEvent
    static void onRegisterDataMapTypes(RegisterDataMapTypesEvent event) {
        event.register(DATA_MAP);
    }

    /** プレイヤーの攻撃を、敵の VIT で減らす（防具などの計算のあと）。 */
    @SubscribeEvent
    static void onDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || victim instanceof Player) {
            return;
        }
        int vit = of(victim.getType()).vitality();
        if (vit <= 0) {
            return;
        }
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player player) || player instanceof FakePlayer
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        if (source.is(ModTags.DamageTypes.MAGIC_ATTACK) && !ModConfigs.enemyVitAppliesToMagic()) {
            return;
        }
        event.setNewDamage(applyVitality(event.getNewDamage(), vit, (float) ModConfigs.enemyVitMinDamage()));
    }
}
