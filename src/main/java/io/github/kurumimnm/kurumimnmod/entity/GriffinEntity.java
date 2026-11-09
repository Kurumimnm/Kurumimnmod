package io.github.kurumimnm.kurumimnmod.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.enemy.EnemyMeleeAttackGoal;
import io.github.kurumimnm.kurumimnmod.registry.ModItems;
import io.github.kurumimnm.kurumimnmod.status.DefenseHolder;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * グリフォン（中立モブ）。
 * <p>
 * 【性格】普段はおとなしい。攻撃されると相手を追いかけて攻撃し、近くの仲間も一緒に襲ってくる
 *        （{@link HurtByTargetGoal#setAlertOthers}。呼ぶ範囲は索敵範囲 GRIFFIN_FOLLOW_RANGE）。
 *        怒りは一定時間（GRIFFIN_ANGER_*_SECONDS）で収まる。騎乗はできない。
 * 【移動】普段は 4 本足で歩く（地上ナビ）。次のときは翼を広げて飛ぶ（飛行ナビ）：
 *        <ul>
 *          <li>戦っているとき（攻撃対象がいる）</li>
 *          <li>水・溶岩に入ったとき</li>
 *          <li>崖などから GRIFFIN_FALL_TAKEOFF_DISTANCE 以上落ちたとき（落下ダメージなし）</li>
 *          <li>ときどき気まぐれで（GRIFFIN_IDLE_TAKEOFF_CHANCE）。しばらく飛んだら着地する</li>
 *        </ul>
 *        地上 / 飛行で MoveControl と PathNavigation を差し替える（{@link #applyMovementMode}）。
 * 【鎧】出現時に一定確率（Config の armoredChance）で古い鉄の鎧を着ている。
 *       能力値は同じで、防御力（MOD の防御力ステータスと同じ固定値軽減）だけが高い。
 *       見た目はモデルの armor_* ボーンを表示するかどうかで切り替える（client/renderer/GriffinRenderer）。
 * 【出現】今はスポーンエッグ・/summon のみ（自然スポーンなし）。ドロップなし。
 * <p>
 * /summon で鎧の有無を指定する例：{@code /summon kurumimnmod:griffin ~ ~ ~ {Armored:1b}}
 */
public class GriffinEntity extends PathfinderMob implements NeutralMob, GeoEntity, DefenseHolder {

    // =====================================================================
    // 同期データ・定数
    // =====================================================================

    /** 鎧を着ているか（見た目の切り替えに使うのでクライアントへ同期）。 */
    private static final EntityDataAccessor<Boolean> DATA_ARMORED =
            SynchedEntityData.defineId(GriffinEntity.class, EntityDataSerializers.BOOLEAN);
    /** 飛行中か（アニメーションの切り替えに使うのでクライアントへ同期）。 */
    private static final EntityDataAccessor<Boolean> DATA_FLYING =
            SynchedEntityData.defineId(GriffinEntity.class, EntityDataSerializers.BOOLEAN);

    private static final UniformInt PERSISTENT_ANGER_TIME =
            TimeUtil.rangeOfSeconds(ModConstants.GRIFFIN_ANGER_MIN_SECONDS, ModConstants.GRIFFIN_ANGER_MAX_SECONDS);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.griffin.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.griffin.walk");
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.griffin.fly");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.griffin.attack");
    /** 攻撃のアニメーションを流すコントローラー名（サーバーから triggerAnim で再生する）。 */
    private static final String ACTION_CONTROLLER = "action";
    /** 羽ばたきの音を鳴らす間隔（tick）。fly アニメーションの 1 周（1 秒）に合わせる。 */
    private static final int FLAP_SOUND_INTERVAL = 20;
    /** 着地点を探すときに試す位置の数。 */
    private static final int LANDING_SEARCH_TRIES = 10;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ---- 地上 / 飛行の移動制御（applyMovementMode で差し替える） ----
    private final PathNavigation groundNavigation;
    private final PathNavigation flyingNavigation;
    @SuppressWarnings("rawtypes")
    private final MoveControl groundMoveControl;
    @SuppressWarnings("rawtypes")
    private final MoveControl flyingMoveControl;

    // ---- 怒り（NeutralMob） ----
    private long persistentAngerEndTime = NO_ANGER_END_TIME;
    @Nullable
    private EntityReference<LivingEntity> persistentAngerTarget;

    // ---- 飛行の状態（サーバー側だけで使う。保存しない） ----
    /** 気まぐれで飛んでいる残り時間（tick）。0 になったら着地を目指す。 */
    private int idleFlightTicks;
    /** 飛び立ってからの経過 tick。 */
    private int flightTicks;
    /** 着地を目指し始めてからの経過 tick。 */
    private int landingTicks;

    public GriffinEntity(EntityType<? extends GriffinEntity> type, Level level) {
        super(type, level);
        // Mob のコンストラクタで作られた地上用をとっておき、飛行用を別に作る
        this.groundNavigation = this.navigation;
        this.groundMoveControl = this.moveControl;
        FlyingPathNavigation flying = new FlyingPathNavigation(this, level);
        flying.setCanOpenDoors(false);
        flying.setCanFloat(true);
        this.flyingNavigation = flying;
        this.flyingMoveControl = new FlyingMoveControl<>(this, 20, true);
    }

    /** 属性（数値は ModConstants の GRIFFIN_*）。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, ModConstants.GRIFFIN_MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, ModConstants.GRIFFIN_MOVEMENT_SPEED)
                .add(Attributes.FLYING_SPEED, ModConstants.GRIFFIN_FLYING_SPEED)
                .add(Attributes.ATTACK_DAMAGE, ModConstants.GRIFFIN_ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, ModConstants.GRIFFIN_FOLLOW_RANGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, ModConstants.GRIFFIN_KNOCKBACK_RESISTANCE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ARMORED, false);
        builder.define(DATA_FLYING, false);
    }

    // =====================================================================
    // AI
    // =====================================================================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new EnemyMeleeAttackGoal(this, ModConstants.GRIFFIN_ATTACK_SPEED_MODIFIER, true));
        this.goalSelector.addGoal(3, new GroundWanderGoal());
        this.goalSelector.addGoal(3, new AirWanderGoal());
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // 攻撃されたら反撃し、近くのグリフォンにも知らせて集団で襲う
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        // 怒っている相手（プレイヤー）を狙う
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(3, new ResetUniversalAngerTargetGoal<>(this, true));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        updatePersistentAnger(level, true);
        updateFlight();
    }

    /** 飛び立つ・着地するの判定（サーバー側、毎 tick）。 */
    private void updateFlight() {
        LivingEntity target = getTarget();
        boolean fighting = target != null && target.isAlive();
        boolean inFluid = isInWater() || isInLava();

        if (!isFlying()) {
            boolean falling = !onGround() && this.fallDistance > ModConstants.GRIFFIN_FALL_TAKEOFF_DISTANCE;
            if (fighting || inFluid || falling) {
                takeOff(0);
            } else if (onGround() && getRandom().nextInt(ModConstants.GRIFFIN_IDLE_TAKEOFF_CHANCE) == 0) {
                takeOff(ModConstants.GRIFFIN_IDLE_FLIGHT_MIN_TICKS + getRandom().nextInt(
                        ModConstants.GRIFFIN_IDLE_FLIGHT_MAX_TICKS - ModConstants.GRIFFIN_IDLE_FLIGHT_MIN_TICKS + 1));
            }
            return;
        }

        this.flightTicks++;
        if (this.idleFlightTicks > 0) {
            this.idleFlightTicks--;
        }
        if (wantsToStayAirborne()) {
            this.landingTicks = 0;
            if (!isNoGravity()) {
                setNoGravity(true); // 着地をあきらめて降りている途中で、また飛ぶ理由ができた
            }
            return;
        }
        // ---- 着地 ----
        if (onGround()) {
            setFlying(false);
            return;
        }
        this.landingTicks++;
        boolean overSolidGround = isSolidGround(getBlockX(), getBlockZ());
        if (isNoGravity() && overSolidGround && this.navigation.isDone()
                && getY() - groundHeight(getBlockX(), getBlockZ()) < 2.0) {
            // 着地点のすぐ上まで来た：羽ばたくのをやめて足を下ろす
            setNoGravity(false);
        } else if (isNoGravity() && overSolidGround && this.landingTicks > ModConstants.GRIFFIN_LANDING_TIMEOUT_TICKS) {
            // 着地点まで行けないなど：その場で羽ばたくのをやめて下りる（落下ダメージは受けない）
            this.navigation.stop();
            setNoGravity(false);
        }
    }

    /** その位置の地面の高さ（一番上のブロックの 1 つ上の Y）。 */
    private int groundHeight(int x, int z) {
        return level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    /** その位置の一番上が水・溶岩などの液体でなく、降りられる地面か。 */
    private boolean isSolidGround(int x, int z) {
        BlockPos top = new BlockPos(x, groundHeight(x, z) - 1, z);
        return level().getFluidState(top).isEmpty() && top.getY() > level().getMinY();
    }

    /** 空中にいる理由があるか（戦闘中・水の上・気まぐれ飛行中・飛び立った直後）。 */
    private boolean wantsToStayAirborne() {
        LivingEntity target = getTarget();
        return (target != null && target.isAlive())
                || isInWater() || isInLava()
                || this.idleFlightTicks > 0
                || this.flightTicks < ModConstants.GRIFFIN_MIN_FLIGHT_TICKS;
    }

    /**
     * 飛び立つ。
     *
     * @param idleTicks 気まぐれで飛ぶ時間（tick）。戦闘・落下などで飛ぶときは 0
     */
    private void takeOff(int idleTicks) {
        this.idleFlightTicks = idleTicks;
        this.flightTicks = 0;
        this.landingTicks = 0;
        boolean wasOnGround = onGround();
        setFlying(true);
        if (wasOnGround) {
            setDeltaMovement(getDeltaMovement().add(0, ModConstants.GRIFFIN_TAKEOFF_BOOST, 0));
        }
    }

    // =====================================================================
    // 地上 / 飛行の切り替え
    // =====================================================================

    public boolean isFlying() {
        return this.entityData.get(DATA_FLYING);
    }

    private void setFlying(boolean flying) {
        this.entityData.set(DATA_FLYING, flying);
        applyMovementMode(flying);
    }

    /** MoveControl と PathNavigation を地上用 / 飛行用に差し替える（重力も切り替える）。 */
    @SuppressWarnings("unchecked")
    private void applyMovementMode(boolean flying) {
        PathNavigation next = flying ? this.flyingNavigation : this.groundNavigation;
        if (this.navigation != next) {
            this.navigation.stop();
            this.navigation = next;
        }
        this.moveControl = flying ? this.flyingMoveControl : this.groundMoveControl;
        setNoGravity(flying);
        if (!flying) {
            setYya(0f);
        }
    }

    /** 飛行中は鳥のように上下にも自由に動く（ミツバチ・オウムと同じ扱い）。 */
    @Override
    protected boolean omnidirectionalAirMover() {
        return isFlying();
    }

    /** 飛行中の速さ。バニラ（0.02 固定）だと遅すぎるので、飛行速度属性に比例させる。 */
    @Override
    protected float getFlyingSpeed() {
        return isFlying() ? getSpeed() * ModConstants.GRIFFIN_AIR_ACCELERATION : super.getFlyingSpeed();
    }

    /** 翼があるので落下ダメージは受けない。 */
    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    // =====================================================================
    // 鎧・防御力
    // =====================================================================

    public boolean isArmored() {
        return this.entityData.get(DATA_ARMORED);
    }

    public void setArmored(boolean armored) {
        this.entityData.set(DATA_ARMORED, armored);
    }

    /** 防御力（StatEvents がプレイヤーの防御力と同じ式で被ダメージを減らす）。 */
    @Override
    public int getDefense() {
        return isArmored() ? ModConfigs.griffinArmoredDefense() : ModConfigs.griffinDefense();
    }

    /** 出現時（スポーンエッグ・/summon）に鎧を着ているかを決める。 */
    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        setArmored(getRandom().nextDouble() < ModConfigs.griffinArmoredChance());
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    // =====================================================================
    // 攻撃
    // =====================================================================

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        triggerAnim(ACTION_CONTROLLER, "attack");
        return super.doHurtTarget(level, target);
    }

    // =====================================================================
    // 怒り（NeutralMob）
    // =====================================================================

    @Override
    public void startPersistentAngerTimer() {
        setTimeToRemainAngry(PERSISTENT_ANGER_TIME.sample(getRandom()));
    }

    @Override
    public long getPersistentAngerEndTime() {
        return this.persistentAngerEndTime;
    }

    @Override
    public void setPersistentAngerEndTime(long endTime) {
        this.persistentAngerEndTime = endTime;
    }

    @Override
    public @Nullable EntityReference<LivingEntity> getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> target) {
        this.persistentAngerTarget = target;
    }

    // =====================================================================
    // 毎 tick（クライアント：羽ばたきの音）
    // =====================================================================

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() && isFlying() && !isSilent() && this.tickCount % FLAP_SOUND_INTERVAL == 0) {
            level().playLocalSound(getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_FLAP, getSoundSource(),
                    0.6f, 0.9f + getRandom().nextFloat() * 0.2f, false);
        }
    }

    // =====================================================================
    // 保存
    // =====================================================================

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        addPersistentAngerSaveData(output);
        output.putBoolean("Armored", isArmored());
        output.putBoolean("Flying", isFlying());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        readPersistentAngerSaveData(level(), input);
        setArmored(input.getBooleanOr("Armored", false));
        setFlying(input.getBooleanOr("Flying", false));
    }

    // =====================================================================
    // その他
    // =====================================================================

    /** スポーンエッグでしか出ない貴重な Mob なので、遠くに離れても消えない。 */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /** クリエイティブのピックブロック（ホイールクリック）でスポーンエッグを取れるようにする。 */
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.GRIFFIN_SPAWN_EGG.get());
    }

    // ---- 鳴き声（仮：オウムの声を低くして使う。専用の音は後で差し替え） ----
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return ModConstants.GRIFFIN_VOICE_PITCH + (getRandom().nextFloat() - 0.5f) * 0.1f;
    }

    // =====================================================================
    // GeckoLib
    // =====================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 飛行中 → fly / 歩いている → walk / 止まっている → idle（5 tick かけてなめらかに切り替え）
        controllers.add(new AnimationController<GriffinEntity>("main", 5, test -> {
            if (test.animatable().isFlying()) {
                return test.setAndContinue(FLY);
            }
            return test.setAndContinue(test.isMoving() ? WALK : IDLE);
        }));
        // 攻撃：サーバーから triggerAnim で再生する（普段は何もしない）
        controllers.add(new AnimationController<GriffinEntity>(ACTION_CONTROLLER, test -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // =====================================================================
    // Goal
    // =====================================================================

    /** 地上をうろつく（飛行中は使わない）。 */
    private class GroundWanderGoal extends RandomStrollGoal {
        GroundWanderGoal() {
            super(GriffinEntity.this, ModConstants.GRIFFIN_WANDER_SPEED_MODIFIER);
        }

        @Override
        public boolean canUse() {
            return !isFlying() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !isFlying() && super.canContinueToUse();
        }
    }

    /**
     * 空中をうろつく / 着地点へ降りる（飛行中で、攻撃対象がいないとき）。
     * 飛び続ける理由があれば空中のランダムな位置へ、なければ真下付近の地面へ向かう。
     */
    private class AirWanderGoal extends Goal {
        AirWanderGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return isFlying() && getTarget() == null && isNoGravity() && navigation.isDone();
        }

        @Override
        public boolean canContinueToUse() {
            return isFlying() && getTarget() == null && isNoGravity() && !navigation.isDone();
        }

        @Override
        public void start() {
            int range = ModConstants.GRIFFIN_AIR_WANDER_RANGE;
            if (!wantsToStayAirborne()) {
                // ---- 着地：近くから順に、液体でない地面を探す（水の上なら陸地まで飛んでいく） ----
                for (int i = 0; i < LANDING_SEARCH_TRIES; i++) {
                    int spread = i < LANDING_SEARCH_TRIES / 2 ? range / 3 : range;
                    int x = getBlockX() + getRandom().nextInt(spread * 2 + 1) - spread;
                    int z = getBlockZ() + getRandom().nextInt(spread * 2 + 1) - spread;
                    if (isSolidGround(x, z)) {
                        navigation.moveTo(x + 0.5, groundHeight(x, z), z + 0.5, ModConstants.GRIFFIN_AIR_WANDER_SPEED_MODIFIER);
                        return;
                    }
                }
            }
            // ---- 空中をうろつく（着地点が見つからなかったときも、場所を変えてまた探す） ----
            int x = getBlockX() + getRandom().nextInt(range * 2 + 1) - range;
            int z = getBlockZ() + getRandom().nextInt(range * 2 + 1) - range;
            double y = groundHeight(x, z) + ModConstants.GRIFFIN_AIR_WANDER_MIN_HEIGHT + getRandom().nextInt(
                    ModConstants.GRIFFIN_AIR_WANDER_MAX_HEIGHT - ModConstants.GRIFFIN_AIR_WANDER_MIN_HEIGHT + 1);
            y = Math.min(y, level().getMaxY() - 2); // 建築限界より上に行かないように
            navigation.moveTo(x + 0.5, y, z + 0.5, ModConstants.GRIFFIN_AIR_WANDER_SPEED_MODIFIER);
        }
    }
}
