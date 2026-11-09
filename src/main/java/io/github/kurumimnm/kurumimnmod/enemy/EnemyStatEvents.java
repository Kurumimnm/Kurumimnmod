package io.github.kurumimnm.kurumimnmod.enemy;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.registry.ModTags;
import io.github.kurumimnm.kurumimnmod.status.StatManager;
import io.github.kurumimnm.kurumimnmod.status.StatType;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import org.jspecify.annotations.Nullable;

/**
 * 敵のステータス（{@link EnemyStats}）を実際のゲームに反映するイベント処理（サーバー側）。
 * <ol>
 *   <li>HP・D などの属性 … ワールドに入ったとき（スポーン・チャンク読み込み）に反映</li>
 *   <li>被ダメージ … 属性の倍率 → VIT（魔法は MDF）の軽減</li>
 *   <li>怯み耐性 … 1 回のダメージが flinch_resist 未満ならノックバックしない</li>
 *   <li>精神力（MND）… デバフを付けたプレイヤーの知力 ≦ MND なら無効</li>
 * </ol>
 * CT（通常攻撃・スキル）は敵のコードから {@link EnemyStats#attackCooldownTicks} などを呼んで使う。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class EnemyStatEvents {

    private EnemyStatEvents() {}

    /** 怯まなかった（ノックバックしない）ダメージを受けたワールド時刻。サーバースレッドからのみ使う。 */
    private static final Map<LivingEntity, Long> NO_FLINCH_AT = new WeakHashMap<>();

    // =====================================================================
    // 1. 属性（HP・D・移動速度など）
    // =====================================================================

    /**
     * JSON で設定された項目だけ、属性の基本値を上書きする（未設定の項目はコードの初期値のまま）。
     * HP が満タンだった場合は、新しい最大 HP まで回復させる（新しくスポーンした敵が満タンで出るように）。
     */
    @SubscribeEvent
    static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity entity)
                || entity instanceof Player) {
            return;
        }
        @Nullable EnemyStats stats = EnemyStats.get(entity.getType());
        if (stats == null) {
            return;
        }
        float oldMax = entity.getMaxHealth();
        boolean wasFull = entity.getHealth() >= oldMax;

        setBase(entity, Attributes.MAX_HEALTH, stats.hp());
        setBase(entity, Attributes.ATTACK_DAMAGE, stats.damage());
        setBase(entity, Attributes.KNOCKBACK_RESISTANCE, stats.knockbackResist());
        setBase(entity, Attributes.MOVEMENT_SPEED, stats.moveSpeed());
        setBase(entity, Attributes.FOLLOW_RANGE, stats.followRange());

        if (wasFull && entity.getMaxHealth() != oldMax) {
            entity.setHealth(entity.getMaxHealth());
        }
    }

    private static void setBase(LivingEntity entity, Holder<Attribute> attribute, Optional<Double> value) {
        if (value.isEmpty()) {
            return;
        }
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && instance.getBaseValue() != value.get()) {
            instance.setBaseValue(value.get());
        }
    }

    // =====================================================================
    // 2. 被ダメージ（属性 → VIT / MDF）・3. 怯み耐性
    // =====================================================================

    /**
     * 敵が受けるダメージを計算する。防具などの軽減のあとの値に対して、
     * <ol>
     *   <li>属性の倍率（elements。炎・溶岩など攻撃者のいないダメージにもかかる）</li>
     *   <li>VIT（魔法ダメージ＝タグ kurumimnmod:magic_attack は MDF）。攻撃者のいるダメージのみ</li>
     * </ol>
     * /kill・奈落は対象外。
     */
    @SubscribeEvent
    static void onDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || entity instanceof Player) {
            return;
        }
        @Nullable EnemyStats stats = EnemyStats.get(entity.getType());
        DamageSource source = event.getSource();
        if (stats == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        float damage = event.getNewDamage();

        // ---- 属性耐性・弱点 ----
        for (Map.Entry<String, Double> element : stats.elements().entrySet()) {
            if (source.is(ModTags.DamageTypes.element(element.getKey()))) {
                damage *= element.getValue().floatValue();
            }
        }

        // ---- VIT / MDF ----
        Entity attacker = source.getEntity();
        if (attacker != null && attacker != entity) {
            int defense = source.is(ModTags.DamageTypes.MAGIC_ATTACK) ? stats.mdf() : stats.vit();
            damage = StatManager.applyEnemyDefense(damage, defense);
        }
        event.setNewDamage(damage);

        // ---- 怯み耐性（このあとのノックバックを止める印をつける） ----
        if (stats.flinchResist() > 0 && damage < stats.flinchResist()) {
            NO_FLINCH_AT.put(entity, entity.level().getGameTime());
        } else {
            NO_FLINCH_AT.remove(entity);
        }
    }

    /** 怯み耐性：同じ tick に「怯まない」ダメージを受けていたらノックバックを止める。 */
    @SubscribeEvent
    static void onKnockBack(LivingKnockBackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }
        Long at = NO_FLINCH_AT.get(entity);
        if (at != null && at == entity.level().getGameTime()) {
            event.setCanceled(true);
        }
    }

    // =====================================================================
    // 4. 精神力（MND）
    // =====================================================================

    /**
     * デバフ（有害なエフェクト）を付けようとしたプレイヤーの知力が MND 以下なら無効にする。
     * 矢・魔法の弾などは撃った人で判定。付与者がプレイヤーでない（または不明な）ときは通常どおり。
     */
    @SubscribeEvent
    static void onEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || entity instanceof Player) {
            return;
        }
        @Nullable EnemyStats stats = EnemyStats.get(entity.getType());
        if (stats == null || stats.mnd() <= 0
                || event.getEffectInstance().getEffect().value().getCategory() != MobEffectCategory.HARMFUL) {
            return;
        }
        Entity source = event.getEffectSource();
        if (source instanceof Projectile projectile) {
            source = projectile.getOwner();
        }
        if (source instanceof Player player && !(player instanceof FakePlayer)
                && StatManager.value(player, StatType.INTELLIGENCE) <= stats.mnd()) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
