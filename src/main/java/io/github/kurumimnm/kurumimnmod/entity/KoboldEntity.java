package io.github.kurumimnm.kurumimnmod.entity;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.registry.ModItems;
import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * コボルト（犬頭の小柄な人型。敵対モブ）。ボス部屋ではコボルトロードの取り巻き。
 * <p>
 * 【通常攻撃】骨のナイフで刺す（近接、攻撃力 KOBOLD_ATTACK_DAMAGE）。左腕に木の盾（見た目だけ）。
 * 【スキル：飛びつき】相手が離れているとき（KOBOLD_LEAP_MIN〜MAX_RANGE）に使い、一気に距離を詰める。
 *   <ol>
 *     <li>予兆：頭の上に「！」を出して身をかがめる（Config skillTelegraphSeconds）</li>
 *     <li>相手の手前へ向けて跳ぶ（向きは跳んだ瞬間に決まる）。空中で触れた相手にダメージ（1 回だけ）</li>
 *     <li>着地したら少しすきがある</li>
 *   </ol>
 *   ダメージはふつうの攻撃より弱い（距離を詰めるのが目的）。ダメージ・クールダウンは Config [mobs.kobold]。
 * 【出現】クエストのフィールド（placed）・スポーンエッグ・/summon。自然スポーンなし。ドロップなし。
 */
public class KoboldEntity extends SkillMonster {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.kobold.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.kobold.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.kobold.attack");
    private static final RawAnimation WINDUP = RawAnimation.begin().thenPlayAndHold("animation.kobold.windup");
    private static final RawAnimation LEAP = RawAnimation.begin().thenPlay("animation.kobold.leap");
    private static final String ACTION_CONTROLLER = "action";
    /** 空中での横の空気抵抗（バニラの LivingEntity と同じ。1 tick ごとに速さがこの倍になる）。 */
    private static final double AIR_DRAG = 0.91;

    public KoboldEntity(EntityType<? extends KoboldEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, ModConstants.KOBOLD_MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ModConstants.KOBOLD_ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, ModConstants.KOBOLD_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, ModConstants.KOBOLD_FOLLOW_RANGE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LeapSkillGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, ModConstants.KOBOLD_CHASE_SPEED_MODIFIER, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, ModConstants.KOBOLD_WANDER_SPEED_MODIFIER));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // 攻撃されたら近くのコボルトにも知らせる（群れで動く）
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        triggerAnim(ACTION_CONTROLLER, "attack");
        return super.doHurtTarget(level, target);
    }

    // =====================================================================
    // スキル：飛びつき
    // =====================================================================

    private enum Phase { WINDUP, AIR, RECOVER, DONE }

    private class LeapSkillGoal extends Goal {
        private Phase phase = Phase.DONE;
        private int timer;
        private boolean leftGround;
        private boolean hit;

        LeapSkillGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive() || !skillReady() || !onGround()) {
                return false;
            }
            double d = distanceTo(target);
            return d >= ModConstants.KOBOLD_LEAP_MIN_RANGE && d <= ModConstants.KOBOLD_LEAP_MAX_RANGE && hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return phase != Phase.DONE && isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            getNavigation().stop();
            phase = Phase.WINDUP;
            timer = ModConfigs.skillTelegraphTicks();
            setTelegraphing(true);
            triggerAnim(ACTION_CONTROLLER, "windup");
        }

        @Override
        public void stop() {
            setTelegraphing(false);
            phase = Phase.DONE;
            startSkillCooldown(ModConfigs.koboldLeapCooldownTicks());
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            switch (phase) {
                case WINDUP -> {
                    if (target != null) {
                        getLookControl().setLookAt(target, 30f, 30f);
                    }
                    if (--timer <= 0) {
                        setTelegraphing(false);
                        leap(target);
                    }
                }
                case AIR -> {
                    timer++;
                    if (!onGround()) {
                        leftGround = true;
                    }
                    if (!hit) {
                        tryHit();
                    }
                    if ((leftGround && onGround()) || timer > ModConstants.KOBOLD_LEAP_MAX_TICKS) {
                        phase = Phase.RECOVER;
                        timer = ModConstants.KOBOLD_LEAP_RECOVER_TICKS;
                    }
                }
                case RECOVER -> {
                    if (--timer <= 0) {
                        phase = Phase.DONE;
                    }
                }
                default -> { }
            }
        }

        /**
         * 相手の手前へ跳ぶ。横の速さは、空中にいる間（KOBOLD_LEAP_FLIGHT_TICKS）に空気抵抗で減っていく分を見込んで、
         * 進む距離がちょうど「相手までの距離 − KOBOLD_LEAP_STOP_SHORT」になるように決める。
         */
        private void leap(LivingEntity target) {
            double dx = target != null ? target.getX() - getX() : 0;
            double dz = target != null ? target.getZ() - getZ() : 0;
            double dist = Math.sqrt(dx * dx + dz * dz);
            double travel = Math.max(0, dist - ModConstants.KOBOLD_LEAP_STOP_SHORT);
            double dragSum = (1 - Math.pow(AIR_DRAG, ModConstants.KOBOLD_LEAP_FLIGHT_TICKS)) / (1 - AIR_DRAG);
            double h = dist > 1.0E-3 ? travel / dragSum : 0;
            double vx = dist > 1.0E-3 ? dx / dist * h : 0;
            double vz = dist > 1.0E-3 ? dz / dist * h : 0;
            if (dist > 1.0E-3) {
                float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
                setYRot(yaw);
                yBodyRot = yaw;
                yHeadRot = yaw;
            }
            setDeltaMovement(vx, ModConstants.KOBOLD_LEAP_UP_SPEED, vz);
            phase = Phase.AIR;
            timer = 0;
            leftGround = false;
            hit = false;
            triggerAnim(ACTION_CONTROLLER, "leap");
            playSound(SoundEvents.FOX_AGGRO, 1.0f, ModConstants.KOBOLD_VOICE_PITCH);
        }

        /** 空中で触れたプレイヤー・相手にダメージ（1 回の飛びつきで 1 回だけ）。 */
        private void tryHit() {
            if (!(level() instanceof ServerLevel level)) {
                return;
            }
            AABB box = getBoundingBox().inflate(ModConstants.KOBOLD_LEAP_HIT_REACH);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != KoboldEntity.this && e.isAlive() && (e instanceof Player || e == getTarget()))) {
                if (e instanceof Player p && (p.isCreative() || p.isSpectator())) {
                    continue;
                }
                e.hurtServer(level, damageSources().mobAttack(KoboldEntity.this), (float) ModConfigs.koboldLeapDamage());
                hit = true;
            }
        }
    }

    // =====================================================================
    // その他
    // =====================================================================

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.KOBOLD_SPAWN_EGG.get());
    }

    // ---- 鳴き声（仮：キツネの声を少し高くして使う。専用の音は後で差し替え） ----
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FOX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return ModConstants.KOBOLD_VOICE_PITCH + (getRandom().nextFloat() - 0.5f) * 0.1f;
    }

    // =====================================================================
    // GeckoLib
    // =====================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<KoboldEntity>("main", 4,
                test -> test.setAndContinue(test.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<KoboldEntity>(ACTION_CONTROLLER, test -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK)
                .triggerableAnim("windup", WINDUP)
                .triggerableAnim("leap", LEAP));
    }
}
