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
import net.minecraft.world.phys.Vec3;

/**
 * ガロウ（4 つ足の狼。敵対モブ）。チュートリアルの中ボス格。
 * <p>
 * 【通常攻撃】噛みつき（近接、攻撃力 GAROU_ATTACK_DAMAGE）。
 * 【スキル：噛みつき】相手が少し離れているとき（GAROU_BITE_MIN〜MAX_RANGE）に使う。
 *   <ol>
 *     <li>予兆：頭の上に「！」を出して身をかがめる（Config skillTelegraphSeconds）</li>
 *     <li>相手の方へ 3 ブロック突進しながら噛みつく（1 回目）</li>
 *     <li>少し間をおいて、もう一度相手の方へ突進して噛みつく（2 回目）</li>
 *   </ol>
 *   突進の向きは各突進の始めに決まるので、横へよければかわせる。1 回の突進で同じ相手には 1 回だけ当たる。
 *   ダメージ・距離・クールダウンは Config [mobs.garou]。
 * 【出現】クエストのフィールド（placed）・スポーンエッグ・/summon。自然スポーンなし。ドロップなし。
 */
public class GarouEntity extends SkillMonster {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.garou.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.garou.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.garou.attack");
    private static final RawAnimation WINDUP = RawAnimation.begin().thenPlayAndHold("animation.garou.windup");
    private static final RawAnimation SNAP = RawAnimation.begin().thenPlay("animation.garou.snap");
    private static final String ACTION_CONTROLLER = "action";
    /** 噛みつきの回数。 */
    private static final int BITES = 2;

    public GarouEntity(EntityType<? extends GarouEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, ModConstants.GAROU_MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ModConstants.GAROU_ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, ModConstants.GAROU_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, ModConstants.GAROU_FOLLOW_RANGE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BiteSkillGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, ModConstants.GAROU_CHASE_SPEED_MODIFIER, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, ModConstants.GAROU_WANDER_SPEED_MODIFIER));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        triggerAnim(ACTION_CONTROLLER, "attack");
        return super.doHurtTarget(level, target);
    }

    // =====================================================================
    // スキル：噛みつき
    // =====================================================================

    private enum Phase { WINDUP, LUNGE, PAUSE, RECOVER, DONE }

    private class BiteSkillGoal extends Goal {
        private Phase phase = Phase.DONE;
        private int timer;
        private int bitesDone;
        private boolean hitThisLunge;
        private Vec3 direction = Vec3.ZERO;

        BiteSkillGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive() || !skillReady() || !onGround()) {
                return false;
            }
            double d = distanceTo(target);
            return d >= ModConstants.GAROU_BITE_MIN_RANGE && d <= ModConstants.GAROU_BITE_MAX_RANGE && hasLineOfSight(target);
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
            bitesDone = 0;
            setTelegraphing(true);
            triggerAnim(ACTION_CONTROLLER, "windup");
        }

        @Override
        public void stop() {
            setTelegraphing(false);
            phase = Phase.DONE;
            startSkillCooldown(ModConfigs.garouBiteCooldownTicks());
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
                        startLunge(target);
                    }
                }
                case LUNGE -> {
                    // 毎 tick 速さを決め直す（地面の摩擦で止まらないように）。距離 = 速さ × tick 数
                    double speed = ModConfigs.garouBiteDistance() / ModConstants.GAROU_BITE_LUNGE_TICKS;
                    setDeltaMovement(direction.x * speed, getDeltaMovement().y, direction.z * speed);
                    if (!hitThisLunge) {
                        tryBite();
                    }
                    if (--timer <= 0) {
                        bitesDone++;
                        phase = bitesDone >= BITES ? Phase.RECOVER : Phase.PAUSE;
                        timer = bitesDone >= BITES ? ModConstants.GAROU_BITE_RECOVER_TICKS : ModConstants.GAROU_BITE_PAUSE_TICKS;
                        setDeltaMovement(getDeltaMovement().multiply(0.3, 1, 0.3));
                    }
                }
                case PAUSE -> {
                    if (target != null) {
                        getLookControl().setLookAt(target, 30f, 30f);
                    }
                    if (--timer <= 0) {
                        startLunge(target);
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

        /** 突進を始める：今の相手の位置へ向きを決める（相手がいなければ前へ）。 */
        private void startLunge(LivingEntity target) {
            Vec3 dir = target != null
                    ? new Vec3(target.getX() - getX(), 0, target.getZ() - getZ())
                    : Vec3.directionFromRotation(0, getYRot());
            direction = dir.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, getYRot()) : dir.normalize();
            float yaw = (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90f;
            setYRot(yaw);
            yBodyRot = yaw;
            yHeadRot = yaw;
            hitThisLunge = false;
            phase = Phase.LUNGE;
            timer = ModConstants.GAROU_BITE_LUNGE_TICKS;
            triggerAnim(ACTION_CONTROLLER, "snap");
            playSound(SoundEvents.FOX_BITE, 1.0f, ModConstants.GAROU_VOICE_PITCH);
        }

        /** 口の前（体の当たり判定を前へずらして広げた範囲）にいるプレイヤー・相手に噛みつく。 */
        private void tryBite() {
            if (!(level() instanceof ServerLevel level)) {
                return;
            }
            double reach = ModConstants.GAROU_BITE_HIT_REACH;
            AABB box = getBoundingBox().move(direction.scale(reach)).inflate(reach * 0.5, 0, reach * 0.5);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != GarouEntity.this && e.isAlive() && (e instanceof Player || e == getTarget()))) {
                if (e instanceof Player p && (p.isCreative() || p.isSpectator())) {
                    continue;
                }
                e.hurtServer(level, damageSources().mobAttack(GarouEntity.this), (float) ModConfigs.garouBiteDamage());
                hitThisLunge = true;
            }
        }
    }

    // =====================================================================
    // その他
    // =====================================================================

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.GAROU_SPAWN_EGG.get());
    }

    // ---- 鳴き声（仮：キツネの声を低くして使う。専用の音は後で差し替え） ----
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FOX_AGGRO;
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
        return ModConstants.GAROU_VOICE_PITCH + (getRandom().nextFloat() - 0.5f) * 0.1f;
    }

    // =====================================================================
    // GeckoLib
    // =====================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GarouEntity>("main", 4,
                test -> test.setAndContinue(test.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<GarouEntity>(ACTION_CONTROLLER, test -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK)
                .triggerableAnim("windup", WINDUP)
                .triggerableAnim("snap", SNAP));
    }
}
