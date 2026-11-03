package io.github.kurumimnm.kurumimnmod.status;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
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
 *   <li>攻撃力 … プレイヤーが与えるダメージに倍率をかける</li>
 *   <li>防御力 … 敵から受けるダメージを固定値で減らす（防具の計算のあと）</li>
 *   <li>技量 … クリティカルを技量の確率判定に置き換える（バニラの落下クリティカルは無効）</li>
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
    // 3. 攻撃力（与ダメージ倍率）
    // =====================================================================

    /**
     * プレイヤーが原因のダメージ（近接・弓矢など）に攻撃力の倍率をかける。
     * 防具などで軽減される前の値にかけるので、バニラの武器ダメージと同じ扱いになる。
     * TODO: 魔法を実装したら、魔法ダメージは攻撃力ではなく知力で計算するよう分ける
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
        float multiplier = StatManager.attackMultiplier(attacker);
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
     * 落下・溶岩・溺れなど、攻撃者のいないダメージは対象外。/kill や奈落も対象外。
     */
    @SubscribeEvent
    static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.getEntity() == null || source.getEntity() == player
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        float reduction = StatManager.defenseReduction(player);
        if (reduction <= 0 || event.getNewDamage() <= 0) {
            return;
        }
        float minDamage = (float) Math.min(ModConfigs.defenseMinDamage(), event.getNewDamage());
        event.setNewDamage(Math.max(minDamage, event.getNewDamage() - reduction));
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
    // ユーティリティ
    // =====================================================================

    /** ダメージの原因が本物のプレイヤーなら返す（矢などは撃った人）。 */
    private static Player realPlayer(DamageSource source) {
        return source.getEntity() instanceof Player player && !(player instanceof FakePlayer) ? player : null;
    }
}
