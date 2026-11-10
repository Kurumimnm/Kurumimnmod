package io.github.kurumimnm.kurumimnmod.enemy;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * 通常攻撃の CT（クールタイム）を敵ステータスから読む近接攻撃 AI。
 * <p>
 * バニラの {@link MeleeAttackGoal} は攻撃間隔が 1 秒固定なので、それを
 * {@link EnemyStats#attackCooldownTicks}（JSON の attack_cooldown、未設定なら 1 秒）に置き換えたもの。
 * それ以外（追いかけ方・攻撃が届く距離）はバニラと同じ。
 */
public class EnemyMeleeAttackGoal extends MeleeAttackGoal {

    /** バニラの攻撃間隔（20 tick = 1 秒）。JSON で未設定のときに使う。 */
    public static final int DEFAULT_COOLDOWN_TICKS = 20;

    /** 次に攻撃できるワールドの時刻（tick）。 */
    private long nextAttackAt;

    public EnemyMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(mob, speedModifier, followingTargetEvenIfNotSeen);
    }

    private int cooldownTicks() {
        return EnemyStats.attackCooldownTicks(mob, DEFAULT_COOLDOWN_TICKS);
    }

    @Override
    protected void resetAttackCooldown() {
        nextAttackAt = mob.level().getGameTime() + cooldownTicks();
    }

    @Override
    protected boolean isTimeToAttack() {
        return mob.level().getGameTime() >= nextAttackAt;
    }

    @Override
    protected int getTicksUntilNextAttack() {
        return (int) Math.max(0, nextAttackAt - mob.level().getGameTime());
    }

    @Override
    protected int getAttackInterval() {
        return cooldownTicks();
    }
}
