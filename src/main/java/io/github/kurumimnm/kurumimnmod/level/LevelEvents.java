package io.github.kurumimnm.kurumimnmod.level;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import io.github.kurumimnm.kurumimnmod.registry.ModAttachments;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import org.jspecify.annotations.Nullable;

/**
 * レベルシステムのゲーム内イベント処理（サーバー側）。
 * <ol>
 *   <li>バニラの経験値オーブを出さない</li>
 *   <li>ダメージ・デバフ・バフで「誰が関わったか」を記録する</li>
 *   <li>Mob が倒されたら、キルした人に 100%・アシストした人に 50% の経験値を配る</li>
 * </ol>
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class LevelEvents {

    private LevelEvents() {}

    // =====================================================================
    // 1. バニラの経験値を無効化
    // =====================================================================

    /**
     * 経験値オーブがワールドに出現するのを止める。
     * Mob・採掘・かまど・釣り・繁殖・交易・エンチャント瓶など、
     * オーブを出すものはすべてこの 1 か所で無効化できる。
     */
    @SubscribeEvent
    static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ExperienceOrb && ModConfigs.isVanillaExperienceDisabled()) {
            event.setCanceled(true);
        }
    }

    // =====================================================================
    // 2. 関わったプレイヤーの記録
    // =====================================================================

    /** ダメージを与えたプレイヤーを記録する（1 ダメージでも可。矢・トライデント等は撃った人）。 */
    @SubscribeEvent
    static void onLivingDamage(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || event.getInflictedDamage() <= 0) {
            return;
        }
        ServerPlayer attacker = asRealPlayer(event.getSource().getEntity());
        if (attacker != null && attacker != victim) {
            victim.getData(ModAttachments.KILL_CONTRIBUTION).recordDamage(attacker.getUUID(), victim.level().getGameTime());
        }
    }

    /**
     * エフェクトが付与されたとき:
     * <ul>
     *   <li>プレイヤーが敵にデバフ（有害なエフェクト）を付けた → 敵側に記録</li>
     *   <li>プレイヤーが他のプレイヤーにバフ（有益なエフェクト）を付けた → 受け取った側に「支援者」として記録</li>
     * </ul>
     * スプラッシュ／残留ポーション・効能付きの矢などは、投げた（撃った）人が付与者になる。
     */
    @SubscribeEvent
    static void onEffectAdded(MobEffectEvent.Added event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        ServerPlayer source = asRealPlayer(event.getEffectSource());
        if (source == null || source == target) {
            return; // 自分自身へのエフェクトは対象外
        }
        long now = target.level().getGameTime();
        MobEffectCategory category = event.getEffectInstance().getEffect().value().getCategory();

        if (category == MobEffectCategory.HARMFUL && !(target instanceof Player)) {
            target.getData(ModAttachments.KILL_CONTRIBUTION).recordDebuff(source.getUUID(), now);
        } else if (category == MobEffectCategory.BENEFICIAL && target instanceof ServerPlayer) {
            target.getData(ModAttachments.KILL_CONTRIBUTION).recordSupport(source.getUUID(), now, assistWindowTicks());
        }
    }

    // =====================================================================
    // 3. 経験値の配布
    // =====================================================================

    /**
     * Mob が倒されたときに経験値を配る。
     * 他の MOD が死亡をキャンセルした場合に配らないよう、優先度を最低にしている。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide() || level.getServer() == null) {
            return;
        }
        int baseExp = MobExperience.of(victim.getType());
        if (baseExp <= 0) {
            return; // 経験値が設定されていない Mob
        }

        // ---- キルした人（直接倒した人。落下死などの場合は直前に攻撃していた人） ----
        ServerPlayer killer = asRealPlayer(event.getSource().getEntity());
        if (killer == null) {
            killer = asRealPlayer(victim.getKillCredit());
        }

        // ---- アシストした人 ----
        long now = level.getGameTime();
        long window = assistWindowTicks();
        Set<UUID> assists = new HashSet<>();
        KillContribution contribution = victim.getExistingDataOrNull(ModAttachments.KILL_CONTRIBUTION);
        if (contribution != null) {
            Set<UUID> damagers = contribution.recentDamagers(now, window);
            assists.addAll(damagers);
            assists.addAll(contribution.recentDebuffers(now, window));
            // ダメージを与えた人（＋キルした人）にバフをくれた人もアシスト
            Set<UUID> attackers = new HashSet<>(damagers);
            if (killer != null) {
                attackers.add(killer.getUUID());
            }
            for (UUID attackerId : attackers) {
                ServerPlayer attacker = level.getServer().getPlayerList().getPlayer(attackerId);
                KillContribution support = attacker == null ? null : attacker.getExistingDataOrNull(ModAttachments.KILL_CONTRIBUTION);
                if (support != null) {
                    assists.addAll(support.recentSupporters(now, window));
                }
            }
        }
        if (killer != null) {
            assists.remove(killer.getUUID()); // キルした人は 100% のみ（重複して受け取らない）
            LevelManager.addExp(killer, scale(baseExp, ModConfigs.killerExpRatio()), LevelManager.Reason.KILL);
        }

        long assistExp = scale(baseExp, ModConfigs.assistExpRatio());
        for (UUID id : assists) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player != null) { // ログアウト中のプレイヤーには配らない
                LevelManager.addExp(player, assistExp, LevelManager.Reason.ASSIST);
            }
        }
    }

    // =====================================================================
    // ユーティリティ
    // =====================================================================

    /** 本物のプレイヤー（FakePlayer＝機械などの疑似プレイヤーは除く）なら返す。 */
    private static @Nullable ServerPlayer asRealPlayer(@Nullable Entity entity) {
        return entity instanceof ServerPlayer player && !(entity instanceof FakePlayer) ? player : null;
    }

    /** 基本経験値 × 倍率（小数点以下切り捨て。ただし 0 にはせず最低 1）。 */
    private static long scale(int baseExp, double ratio) {
        if (ratio <= 0) {
            return 0;
        }
        return Math.max(1L, (long) Math.floor(baseExp * ratio));
    }

    private static long assistWindowTicks() {
        return ModConfigs.assistWindowSeconds() * 20L;
    }
}
