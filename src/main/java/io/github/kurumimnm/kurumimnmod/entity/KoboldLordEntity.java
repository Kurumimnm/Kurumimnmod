package io.github.kurumimnm.kurumimnmod.entity;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.quest.QuestPlots;
import io.github.kurumimnm.kurumimnmod.registry.ModItems;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * コボルトロード（ボス）。チュートリアルの神殿のボス部屋にいる。
 * <p>
 * 【見た目】コボルトをひと回り大きくした姿。暗い毛色・鉄の兜・マント。右手になた、腰に 2 本目のなた。
 * 【通常攻撃】なたで斬る（近接、攻撃力 KOBOLD_LORD_ATTACK_DAMAGE）。
 * 【スキル：壁蹴り突撃】（ダメージ・範囲・CT は Config [mobs.koboldLord]）
 *   <ol>
 *     <li>予兆：頭の上に「！」</li>
 *     <li>いちばん近い柱の上のほう（{@link QuestPlots.Plot#pillars}）へ跳びつく。柱がなければその場で真上へ跳ぶ</li>
 *     <li>柱にしがみつく間、相手の今いる場所（着地点）の地面に<b>赤い範囲</b>を出す（{@link #getAoeCenter}）</li>
 *     <li>柱を蹴って着地点へ突撃。範囲の中のプレイヤーにダメージ。着地後は少しすき</li>
 *   </ol>
 *   着地点は「しがみついた瞬間の相手の位置」で決まるので、範囲から出ればかわせる。
 * 【二刀流】HP が最大の 3 割（Config koboldLordDualHealthRatio）以下になると、腰のなたを抜いて二刀流になる。
 *   以後の通常攻撃は 2 連撃（2 撃目は KOBOLD_LORD_DUAL_SECOND_HIT_DELAY tick 後）、移動速度 +20%。
 * 【ボス】エンティティタグ kurumimnmod:bosses（経験値のアシストのルールなど）。遠くに離れても消えない。
 */
public class KoboldLordEntity extends SkillMonster {

    /** 二刀流か（見た目の切り替えに使うので同期）。 */
    private static final EntityDataAccessor<Boolean> DATA_DUAL =
            SynchedEntityData.defineId(KoboldLordEntity.class, EntityDataSerializers.BOOLEAN);
    /** 着地点の範囲を表示中か・その中心（クライアントの範囲表示用に同期）。 */
    private static final EntityDataAccessor<Boolean> DATA_AOE =
            SynchedEntityData.defineId(KoboldLordEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Vector3f> DATA_AOE_CENTER =
            SynchedEntityData.defineId(KoboldLordEntity.class, EntityDataSerializers.VECTOR3);

    private static final Identifier DUAL_SPEED_ID = Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "kobold_lord_dual_speed");

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.kobold_lord.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.kobold_lord.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.kobold_lord.attack");
    private static final RawAnimation ATTACK_DUAL = RawAnimation.begin().thenPlay("animation.kobold_lord.attack_dual");
    private static final RawAnimation WINDUP = RawAnimation.begin().thenPlayAndHold("animation.kobold_lord.windup");
    private static final RawAnimation JUMP = RawAnimation.begin().thenPlayAndHold("animation.kobold_lord.jump");
    private static final RawAnimation CLING = RawAnimation.begin().thenPlayAndHold("animation.kobold_lord.cling");
    private static final RawAnimation DIVE = RawAnimation.begin().thenPlay("animation.kobold_lord.dive");
    private static final RawAnimation DRAW = RawAnimation.begin().thenPlay("animation.kobold_lord.draw");
    private static final String ACTION_CONTROLLER = "action";

    /** 二刀流の 2 撃目までの残り tick（0 = 予定なし）。 */
    private int secondHitTicks;

    public KoboldLordEntity(EntityType<? extends KoboldLordEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, ModConstants.KOBOLD_LORD_MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ModConstants.KOBOLD_LORD_ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, ModConstants.KOBOLD_LORD_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, ModConstants.KOBOLD_LORD_FOLLOW_RANGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, ModConstants.KOBOLD_LORD_KNOCKBACK_RESISTANCE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DUAL, false);
        builder.define(DATA_AOE, false);
        builder.define(DATA_AOE_CENTER, new Vector3f());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WallKickGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, ModConstants.KOBOLD_LORD_CHASE_SPEED_MODIFIER, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, ModConstants.KOBOLD_LORD_WANDER_SPEED_MODIFIER));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    // =====================================================================
    // 二刀流
    // =====================================================================

    public boolean isDualWielding() {
        return this.entityData.get(DATA_DUAL);
    }

    /** 二刀流にする（移動速度の補正も付ける。読み込み時にも呼ぶ）。 */
    private void setDualWielding(boolean dual) {
        this.entityData.set(DATA_DUAL, dual);
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (dual) {
                speed.addOrUpdateTransientModifier(new AttributeModifier(DUAL_SPEED_ID,
                        ModConstants.KOBOLD_LORD_DUAL_SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            } else {
                speed.removeModifier(DUAL_SPEED_ID);
            }
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        // HP が 3 割を切ったら二刀流に
        if (!isDualWielding() && getHealth() <= getMaxHealth() * ModConfigs.koboldLordDualHealthRatio()) {
            setDualWielding(true);
            triggerAnim(ACTION_CONTROLLER, "draw");
            playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.7f);
        }
        // 二刀流の 2 撃目
        if (secondHitTicks > 0 && --secondHitTicks == 0) {
            LivingEntity target = getTarget();
            if (target != null && target.isAlive() && isWithinMeleeAttackRange(target)) {
                hitOnce(level, target);
            }
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (isDualWielding()) {
            triggerAnim(ACTION_CONTROLLER, "attack_dual");
            secondHitTicks = ModConstants.KOBOLD_LORD_DUAL_SECOND_HIT_DELAY;
        } else {
            triggerAnim(ACTION_CONTROLLER, "attack");
        }
        return hitOnce(level, target);
    }

    private boolean hitOnce(ServerLevel level, Entity target) {
        return super.doHurtTarget(level, target);
    }

    // =====================================================================
    // 着地点の範囲（クライアントの表示用）
    // =====================================================================

    /** 着地点の範囲を表示中なら、その中心（地面の高さ）。 */
    public @Nullable Vec3 getAoeCenter() {
        if (!this.entityData.get(DATA_AOE)) {
            return null;
        }
        Vector3f v = this.entityData.get(DATA_AOE_CENTER);
        return new Vec3(v.x, v.y, v.z);
    }

    private void showAoe(@Nullable Vec3 center) {
        if (center != null) {
            this.entityData.set(DATA_AOE_CENTER, new Vector3f((float) center.x, (float) center.y, (float) center.z));
        }
        this.entityData.set(DATA_AOE, center != null);
    }

    // =====================================================================
    // スキル：壁蹴り突撃
    // =====================================================================

    private enum Phase { WINDUP, JUMP, CLING, DIVE, RECOVER, DONE }

    private class WallKickGoal extends Goal {
        private Phase phase = Phase.DONE;
        private int timer;
        private Vec3 from = Vec3.ZERO;
        private Vec3 grab = Vec3.ZERO;
        private Vec3 landing = Vec3.ZERO;

        WallKickGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive() || !skillReady() || !onGround()) {
                return false;
            }
            double d = distanceTo(target);
            return d >= ModConstants.KOBOLD_LORD_KICK_MIN_RANGE && d <= ModConstants.KOBOLD_LORD_KICK_MAX_RANGE;
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
            // 途中で止まっても（死んだ・相手が消えたなど）、浮いたまま・範囲が出たままにならないように戻す
            setTelegraphing(false);
            showAoe(null);
            setNoGravity(false);
            resetFallDistance();
            phase = Phase.DONE;
            startSkillCooldown(ModConfigs.koboldLordKickCooldownTicks());
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            resetFallDistance();
            switch (phase) {
                case WINDUP -> {
                    if (target != null) {
                        getLookControl().setLookAt(target, 30f, 30f);
                    }
                    if (--timer <= 0) {
                        setTelegraphing(false);
                        from = position();
                        grab = findGrabPoint();
                        setNoGravity(true);
                        phase = Phase.JUMP;
                        timer = 0;
                        faceTowards(grab);
                        triggerAnim(ACTION_CONTROLLER, "jump");
                        playSound(SoundEvents.FOX_AGGRO, 1.5f, ModConstants.KOBOLD_LORD_VOICE_PITCH);
                    }
                }
                case JUMP -> {
                    // 弧を描いて柱の上のほうへ
                    timer++;
                    double s = Math.min(1.0, timer / (double) ModConstants.KOBOLD_LORD_JUMP_TICKS);
                    Vec3 p = from.lerp(grab, s).add(0, ModConstants.KOBOLD_LORD_JUMP_ARC * 4 * s * (1 - s), 0);
                    moveTowards(p);
                    if (s >= 1.0) {
                        phase = Phase.CLING;
                        timer = ModConstants.KOBOLD_LORD_CLING_TICKS;
                        setDeltaMovement(Vec3.ZERO);
                        // 着地点 = 今の相手の位置（ここで決まる。範囲を出して、よける時間を作る）
                        landing = target != null ? target.position() : position().add(0, -ModConstants.KOBOLD_LORD_FALLBACK_JUMP_HEIGHT, 0);
                        showAoe(landing);
                        setTelegraphing(true);
                        triggerAnim(ACTION_CONTROLLER, "cling");
                    }
                }
                case CLING -> {
                    setDeltaMovement(Vec3.ZERO);
                    faceTowards(landing);
                    if (--timer <= 0) {
                        setTelegraphing(false);
                        from = position();
                        phase = Phase.DIVE;
                        timer = 0;
                        triggerAnim(ACTION_CONTROLLER, "dive");
                        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.6f);
                    }
                }
                case DIVE -> {
                    timer++;
                    double s = Math.min(1.0, timer / (double) ModConstants.KOBOLD_LORD_DIVE_TICKS);
                    moveTowards(from.lerp(landing, s));
                    if (s >= 1.0) {
                        land();
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

        /** 着地：範囲の中のプレイヤーにダメージ、範囲を消して重力を戻す。 */
        private void land() {
            setNoGravity(false);
            setDeltaMovement(Vec3.ZERO);
            showAoe(null);
            if (level() instanceof ServerLevel level) {
                double r = ModConfigs.koboldLordKickRadius();
                for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(r + 1, ModConstants.KOBOLD_LORD_KICK_HIT_HEIGHT, r + 1),
                        p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
                    double dx = p.getX() - landing.x;
                    double dz = p.getZ() - landing.z;
                    if (dx * dx + dz * dz <= r * r && Math.abs(p.getY() - landing.y) <= ModConstants.KOBOLD_LORD_KICK_HIT_HEIGHT) {
                        p.hurtServer(level, damageSources().mobAttack(KoboldLordEntity.this), (float) ModConfigs.koboldLordKickDamage());
                    }
                }
            }
            playSound(SoundEvents.ANVIL_LAND, 0.5f, 0.8f);
            phase = Phase.RECOVER;
            timer = ModConstants.KOBOLD_LORD_RECOVER_TICKS;
        }

        /**
         * 跳びつく所：いちばん近い柱の、ロードに向いた面の上のほう。
         * 面にぴったり付くよう、柱→ロードの向きを東西南北のどれかにそろえる。柱がなければ真上。
         */
        private Vec3 findGrabPoint() {
            QuestPlots.Plot plot = QuestPlots.at(getX(), getZ());
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            if (plot != null) {
                for (BlockPos p : plot.pillars()) {
                    double d = Mth.square(p.getX() + 0.5 - getX()) + Mth.square(p.getZ() + 0.5 - getZ());
                    if (d < bestDist) {
                        bestDist = d;
                        best = p;
                    }
                }
            }
            if (plot == null || best == null) {
                return position().add(0, ModConstants.KOBOLD_LORD_FALLBACK_JUMP_HEIGHT, 0);
            }
            double cx = best.getX() + 0.5;
            double cz = best.getZ() + 0.5;
            double dx = getX() - cx;
            double dz = getZ() - cz;
            double off = ModConstants.QUEST_PILLAR_HALF_WIDTH + 0.5 + getBbWidth() / 2 + 0.05;
            double gx = cx;
            double gz = cz;
            if (Math.abs(dx) >= Math.abs(dz)) {
                gx += Math.signum(dx == 0 ? 1 : dx) * off;
            } else {
                gz += Math.signum(dz) * off;
            }
            double gy = plot.pillarTopY() - ModConstants.KOBOLD_LORD_CLING_BELOW_TOP;
            return new Vec3(gx, gy, gz);
        }

        /** 次の tick で p に着くように速さを決める（重力なしで動かす）。 */
        private void moveTowards(Vec3 p) {
            setDeltaMovement(p.subtract(position()));
        }

        private void faceTowards(Vec3 p) {
            double dx = p.x - getX();
            double dz = p.z - getZ();
            if (dx * dx + dz * dz > 1.0E-4) {
                float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
                setYRot(yaw);
                yBodyRot = yaw;
                yHeadRot = yaw;
            }
        }
    }

    // =====================================================================
    // 保存・その他
    // =====================================================================

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("DualWield", isDualWielding());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setDualWielding(input.getBooleanOr("DualWield", false));
    }

    /** ボスなので、遠くに離れても消えない。 */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.KOBOLD_LORD_SPAWN_EGG.get());
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
        return ModConstants.KOBOLD_LORD_VOICE_PITCH + (getRandom().nextFloat() - 0.5f) * 0.1f;
    }

    // =====================================================================
    // GeckoLib
    // =====================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<KoboldLordEntity>("main", 4,
                test -> test.setAndContinue(test.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<KoboldLordEntity>(ACTION_CONTROLLER, test -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK)
                .triggerableAnim("attack_dual", ATTACK_DUAL)
                .triggerableAnim("windup", WINDUP)
                .triggerableAnim("jump", JUMP)
                .triggerableAnim("cling", CLING)
                .triggerableAnim("dive", DIVE)
                .triggerableAnim("draw", DRAW));
    }
}
