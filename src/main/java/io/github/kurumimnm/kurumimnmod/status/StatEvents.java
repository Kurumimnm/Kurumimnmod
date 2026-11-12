package io.github.kurumimnm.kurumimnmod.status;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import io.github.kurumimnm.kurumimnmod.registry.ModTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import io.github.kurumimnm.kurumimnmod.entity.MagicArrowEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * ステータスの効果を実際のゲームに反映するイベント処理（サーバー側）。
 * <ol>
 *   <li>属性の反映（最大 HP など）… ログイン・リスポーン・ディメンション移動時と、毎秒の確認</li>
 *   <li>MP の自然回復 … 毎秒</li>
 *   <li>攻撃力・知力 … プレイヤーが与えるダメージに倍率をかける（魔法ダメージは知力、それ以外は攻撃力）</li>
 *   <li>防御力 … 敵から受けるダメージを固定値で減らす（防具の計算のあと。ダメージ ≦ 防御力なら 1。敵の VIT と同じ式）</li>
 *   <li>技量 … クリティカルを技量の確率判定に置き換える（バニラの落下クリティカルは無効）</li>
 *   <li>矢 … 引き絞り切った矢にも同じ会心判定（バニラの「会心の矢」の乱数ボーナスは無効）。
 *       魔法の矢（{@link MagicArrowEntity}）は物理ではなく魔法攻撃として扱う（知力の倍率。誰が撃っても同じ）</li>
 * </ol>
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class StatEvents {

    private StatEvents() {}

    // =====================================================================
    // 1. 属性の反映
    // =====================================================================

    @SubscribeEvent
    static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StatManager.applyAttributes(player);
        }
    }

    /** リスポーン後は新しいプレイヤーになるので反映し直し、HP を（増えた最大 HP まで）満タンにする。 */
    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StatManager.applyAttributes(player);
            if (!event.isEndConquered()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    @SubscribeEvent
    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StatManager.applyAttributes(player);
        }
    }

    // =====================================================================
    // 2. 毎秒の処理（属性の再確認・MP 回復）
    // =====================================================================

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        // Config が変更された場合もここで反映される（値が同じなら何もしない）
        StatManager.applyAttributes(player);

        // ---- MP 自然回復 ----
        if (player.isAlive()) {
            float max = StatManager.maxMana(player);
            float current = StatManager.mana(player);
            if (current < max) {
                StatManager.setMana(player, current + (float) (max * ModConfigs.mpRegenPerSecond()));
            }
        }
    }

    // =====================================================================
    // 3. 攻撃力・知力（与ダメージ倍率）
    // =====================================================================

    /**
     * プレイヤーが原因のダメージに倍率をかける。
     * <ul>
     *   <li>魔法ダメージ（ダメージタイプタグ {@code kurumimnmod:magic_attack}。魔法の杖など）→ 知力の倍率</li>
     *   <li>それ以外（近接・弓矢など）→ 攻撃力の倍率</li>
     * </ul>
     * 防具などで軽減される前の値にかけるので、バニラの武器ダメージと同じ扱いになる。
     */
    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        Player attacker = realPlayer(event.getSource());
        if (attacker == null || attacker == victim) {
            return;
        }
        boolean magic = event.getSource().is(ModTags.DamageTypes.MAGIC_ATTACK)
                || event.getSource().getDirectEntity() instanceof MagicArrowEntity;
        float multiplier = magic ? StatManager.magicMultiplier(attacker) : StatManager.attackMultiplier(attacker);
        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow) {
            multiplier *= arrowCritMultiplier(attacker, arrow, victim);
        }
        if (multiplier != 1f) {
            event.setAmount(event.getAmount() * multiplier);
        }
    }

    // =====================================================================
    // 4. 防御力（被ダメージ固定値軽減）
    // =====================================================================

    /**
     * 敵（攻撃したエンティティがいるダメージ）から受けるダメージを減らす。
     * 防具・エンチャント・耐性エフェクトの計算が終わったあとの値から引く。
     * 受けるダメージが防御力以下なら被ダメ 1（Config blockedDamage）、超えたら 防御力 × 0.5 を引く。
     * 落下・溶岩・溺れなど、攻撃者のいないダメージは対象外。/kill や奈落も対象外。
     * <p>
     * 対象：プレイヤー（防御力ステータス）と、{@link DefenseHolder} を実装した Mob（グリフォンなど）。
     * カカシは計測表示のため ScarecrowEvents で同じ式を使って別に処理している。
     */
    @SubscribeEvent
    static void onDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        double defense;
        if (entity instanceof ServerPlayer player) {
            defense = StatManager.value(player, StatType.DEFENSE);
        } else if (entity instanceof DefenseHolder holder && !entity.level().isClientSide()) {
            defense = holder.getDefense();
        } else {
            return;
        }
        DamageSource source = event.getSource();
        if (source.getEntity() == null || source.getEntity() == entity
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        event.setNewDamage(StatManager.applyDefenseStat(event.getNewDamage(), defense));
    }

    // =====================================================================
    // 5. 技量（クリティカル）
    // =====================================================================

    /**
     * クリティカル判定を技量の確率判定だけにする（バニラの落下中クリティカルは無効）。
     * <ul>
     *   <li>攻撃のクールダウンが 9 割以上たまっているときだけ判定（バニラと同じ条件）</li>
     *   <li>乱数はサーバー側だけで判定する。クライアント側は常にクリティカルなしにして、
     *       演出（パーティクル）はサーバーから届いたものだけを表示する</li>
     * </ul>
     */
    @SubscribeEvent
    static void onCriticalHit(CriticalHitEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player instanceof FakePlayer) {
            event.setCriticalHit(false);
            return;
        }
        boolean fullStrength = player.getAttackStrengthScale(0.5f) > 0.9f;
        boolean crit = fullStrength && player.getRandom().nextDouble() < StatManager.critChance(player);
        event.setCriticalHit(crit);
        if (crit) {
            event.setDamageMultiplier(StatManager.critMultiplier(player));
        }
    }

    // =====================================================================
    // 矢（会心・魔弓士の知力）
    // =====================================================================

    /** 矢が「引き絞り切って撃たれた」ことを覚えておく印（エンティティの保存データ）。 */
    private static final String FULL_CHARGE_KEY = Kurumimnmod.MOD_ID + ":full_charge";

    /**
     * プレイヤーが撃った矢が出たとき：バニラの会心の矢（乱数でダメージが増える）を止め、
     * 引き絞り切ったかどうかだけ覚えておく（会心はステータスの判定に一本化）。
     */
    @SubscribeEvent
    static void onArrowJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || event.loadedFromDisk()
                || !(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof Player)) {
            return;
        }
        if (arrow.isCritArrow()) {
            arrow.getPersistentData().putBoolean(FULL_CHARGE_KEY, true);
            arrow.setCritArrow(false);
        }
    }

    /** 矢の会心によるダメージ倍率。引き絞り切った矢だけ、会心率で判定して会心ダメージ倍率（近接と同じ値）。 */
    private static float arrowCritMultiplier(Player attacker, AbstractArrow arrow, LivingEntity victim) {
        float multiplier = 1f;
        boolean full = arrow.getPersistentData().getBooleanOr(FULL_CHARGE_KEY, false);
        if (full && attacker.getRandom().nextDouble() < StatManager.critChance(attacker)) {
            multiplier *= StatManager.critMultiplier(attacker);
            if (victim.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY(0.5), victim.getZ(),
                        12, victim.getBbWidth() / 2, victim.getBbHeight() / 4, victim.getBbWidth() / 2, 0.2);
            }
        }
        return multiplier;
    }

    // =====================================================================
    // ユーティリティ
    // =====================================================================

    /** ダメージの原因が本物のプレイヤーなら返す（矢などは撃った人）。 */
    private static Player realPlayer(DamageSource source) {
        return source.getEntity() instanceof Player player && !(player instanceof FakePlayer) ? player : null;
    }
}
