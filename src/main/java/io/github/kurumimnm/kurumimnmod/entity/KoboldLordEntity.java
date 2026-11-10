package io.github.kurumimnm.kurumimnmod.entity;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.enemy.EnemyMeleeAttackGoal;
import io.github.kurumimnm.kurumimnmod.quest.QuestPlots;
import io.github.kurumimnm.kurumimnmod.registry.ModItems;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * コボルトロード（ボス）。チュートリアルの神殿のボス部屋にいる。
 * <p>
 * 【見た目】幾多の戦いを生き延びた歴戦の首領。猫背の巨体に傷だらけの毛皮、爪痕で潰れた右目と赤く光る左目、
 *   片方が折れた角付きのへこんだ兜、トゲ付きの肩当て、仕留めた獲物の頭骨の首飾り、裂けて血の染みたマント。
 *   右手に刃こぼれした血まみれの大なた、腰に 2 本目のなた。
 * 【大きさ】属性 scale の基本値 KOBOLD_LORD_SCALE（1.6 倍。高さ約 3.8 ブロック）。
 * 【ステータス】ボス。VIT（防御力）10（data_maps/entity_type/enemy_stats.json、{@link io.github.kurumimnm.kurumimnmod.enemy.EnemyStats}）。
 *   画面上部に赤い 10 区切りのボスバー（近くのプレイヤーだけ）。
 * 【通常攻撃】大なたを振り下ろす（近接、攻撃力 KOBOLD_LORD_ATTACK_DAMAGE）。当たると斬撃と火花。
 * 【スキル：壁蹴り突撃】（ダメージ・範囲・CT は Config [mobs.koboldLord]）
 *   <ol>
 *     <li>予兆：頭の上に「！」</li>
 *     <li>いちばん近い柱の上のほう（{@link QuestPlots.Plot#pillars}）へ跳びつく。柱がなければその場で真上へ跳ぶ</li>
 *     <li>柱にしがみつく間、相手の今いる場所（着地点）の地面に<b>赤い範囲</b>を出す（{@link #getAoeCenter}）</li>
 *     <li>柱を蹴って着地点へ突撃（煙の軌跡）。着地で爆発と広がる衝撃波、範囲の中のプレイヤーにダメージ＋吹き飛ばし。着地後は少しすき</li>
 *   </ol>
 *   着地点は「しがみついた瞬間の相手の位置」で決まるので、範囲から出ればかわせる。
 * 【二刀流（第 2 形態）】HP が最大の 3 割（Config koboldLordDualHealthRatio）以下になると、地上で腰のなたを抜き、
 *   雄たけびを上げて周りのプレイヤーを押し返す（ダメージなし。範囲・強さは Config）。
 *   以後の通常攻撃は 2 連撃（2 撃目は KOBOLD_LORD_DUAL_SECOND_HIT_DELAY tick 後）、移動速度 +20%、口から煙を吐く。
 * 【ボス】エンティティタグ kurumimnmod:bosses（経験値のアシストのルールなど）。遠くに離れても消えない。
 */
public class KoboldLordEntity extends SkillMonster {

    /** 二刀流か（見た目の切り替えに使うので同期）。 */
    private static final EntityDataAccessor<Boolean> DATA_DUAL =
            SynchedEntityData.defineId(KoboldLordEntity.class, EntityDataSerializers.BOOLEAN);
    /** 着地点の範囲を表示中か・その中心（クライアントの範囲表示用に同期）。 */
    private static final EntityDataAccessor<Boolean> DATA_AOE =
            SynchedEntityData.defineId(KoboldLordEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Vector3fc> DATA_AOE_CENTER =
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
    private static final RawAnimation LAND = RawAnimation.begin().thenPlay("animation.kobold_lord.land");
    private static final String ACTION_CONTROLLER = "action";

    /** 二刀流の 2 撃目までの残り tick（0 = 予定なし）。 */
    private int secondHitTicks;
    /** 第 2 形態への移行の演出の残り tick（0 = 移行中でない）。 */
    private int phaseShiftTicks;

    /** ボスバー（赤・10 区切り）。近くのプレイヤーにだけ見せる（{@link #updateBossBarPlayers}）。 */
    private final ServerBossEvent bossEvent =
            new ServerBossEvent(UUID.randomUUID(), getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    public KoboldLordEntity(EntityType<? extends KoboldLordEntity> type, Level level) {
        super(type, level);
        // 大きさの属性（scale）を当たり判定に反映する
        refreshDimensions();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, ModConstants.KOBOLD_LORD_MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ModConstants.KOBOLD_LORD_ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, ModConstants.KOBOLD_LORD_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, ModConstants.KOBOLD_LORD_FOLLOW_RANGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, ModConstants.KOBOLD_LORD_KNOCKBACK_RESISTANCE)
                // ボスらしい巨体：見た目・当たり判定・目の高さをまとめて拡大（GeckoLib も描画時にこの倍率をかける）
                .add(Attributes.SCALE, ModConstants.KOBOLD_LORD_SCALE)
                .add(Attributes.STEP_HEIGHT, ModConstants.KOBOLD_LORD_STEP_HEIGHT);
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
        this.goalSelector.addGoal(0, new PhaseShiftGoal());
        this.goalSelector.addGoal(1, new WallKickGoal());
        this.goalSelector.addGoal(2, new EnemyMeleeAttackGoal(this, ModConstants.KOBOLD_LORD_CHASE_SPEED_MODIFIER, true));
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
        // ボスバー
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (tickCount % ModConstants.KOBOLD_LORD_BOSS_BAR_UPDATE_INTERVAL == 0) {
            updateBossBarPlayers(level);
        }
        // HP が 3 割を切ったら二刀流に（壁蹴り突撃の空中では待つ。地上に降りてから）
        if (!isDualWielding() && onGround() && !isNoGravity()
                && getHealth() <= getMaxHealth() * ModConfigs.koboldLordDualHealthRatio()) {
            setDualWielding(true);
            phaseShiftTicks = ModConstants.KOBOLD_LORD_PHASE_SHIFT_TICKS;
            triggerAnim(ACTION_CONTROLLER, "draw");
            playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.7f);
        }
        if (phaseShiftTicks > 0) {
            phaseShiftTicks--;
            if (phaseShiftTicks == ModConstants.KOBOLD_LORD_PHASE_SHIFT_TICKS - ModConstants.KOBOLD_LORD_ROAR_DELAY_TICKS) {
                roar(level);
            }
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
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            // 斬撃・火花・重い打撃音
            double y = target.getY(0.5);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), y, target.getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.CRIT, target.getX(), y, target.getZ(),
                    ModConstants.KOBOLD_LORD_HIT_SPARKS, 0.3, 0.3, 0.3, 0.4);
            playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.6f);
        }
        return hit;
    }

    // =====================================================================
    // 第 2 形態への移行（なたを抜く → 雄たけび）
    // =====================================================================

    /** 移行の演出の間は、その場で動かない（近接攻撃・壁蹴り突撃もしない）。 */
    private class PhaseShiftGoal extends Goal {
        PhaseShiftGoal() {
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return phaseShiftTicks > 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            getNavigation().stop();
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30f, 30f);
            }
        }
    }

    /** 雄たけび：大きな咆哮・煙の輪・周りのプレイヤーを押し返す（ダメージなし）。 */
    private void roar(ServerLevel level) {
        playSound(SoundEvents.RAVAGER_ROAR, 2.0f, 0.75f);
        double r = ModConfigs.koboldLordRoarRadius();
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getEyeY(), getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getEyeY(), getZ(), 20, 0.5, 0.4, 0.5, 0.15);
        particleRing(level, ParticleTypes.POOF, position(), Math.max(1.0, r * 0.5), 0.2);
        particleRing(level, groundDust(level, position()), position(), Math.max(1.0, r), 0.1);
        double strength = ModConfigs.koboldLordRoarKnockback();
        if (r > 0 && strength > 0) {
            for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(r),
                    p -> p.isAlive() && !p.isCreative() && !p.isSpectator() && distanceToSqr(p) <= r * r)) {
                launch(p, position(), strength);
            }
        }
    }

    // =====================================================================
    // 演出の共通処理
    // =====================================================================

    /** 地面に円を描くようにパーティクルを出す。 */
    private static void particleRing(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, double yOffset) {
        int n = Math.max(8, (int) Math.ceil(Math.PI * 2 * radius * ModConstants.KOBOLD_LORD_RING_PARTICLES_PER_BLOCK));
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            level.sendParticles(particle, center.x + Math.cos(a) * radius, center.y + yOffset, center.z + Math.sin(a) * radius,
                    1, 0, 0, 0, 0);
        }
    }

    /** 足もとのブロックの土ぼこり（メイスの叩きつけと同じ、地面から立ちのぼる破片）。 */
    private static ParticleOptions groundDust(ServerLevel level, Vec3 pos) {
        return new BlockParticleOption(ParticleTypes.DUST_PILLAR, groundBlock(level, pos));
    }

    /** 足もとのブロックの破片。 */
    private static ParticleOptions blockDebris(ServerLevel level, Vec3 pos) {
        return new BlockParticleOption(ParticleTypes.BLOCK, groundBlock(level, pos));
    }

    private static BlockState groundBlock(ServerLevel level, Vec3 pos) {
        BlockState state = level.getBlockState(BlockPos.containing(pos.x, pos.y - 0.5, pos.z));
        return state.isAir() ? Blocks.STONE.defaultBlockState() : state;
    }

    /** プレイヤーを中心から外へ吹き飛ばす（少し浮かせる）。ノックバック耐性が効く。 */
    private void launch(Player player, Vec3 from, double strength) {
        player.knockback(strength, from.x - player.getX(), from.z - player.getZ(), damageSources().mobAttack(this), 0f);
        player.setDeltaMovement(player.getDeltaMovement().add(0, ModConstants.KOBOLD_LORD_KNOCKBACK_LIFT, 0));
        // プレイヤーの動きはクライアント側で計算されるので、新しい速度を本人へ送る
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
        }
    }

    // =====================================================================
    // ボスバー
    // =====================================================================

    /** ボスバーを、近く（KOBOLD_LORD_BOSS_BAR_RANGE）にいるプレイヤーにだけ見せる。 */
    private void updateBossBarPlayers(ServerLevel level) {
        double range = ModConstants.KOBOLD_LORD_BOSS_BAR_RANGE;
        Set<ServerPlayer> near = new HashSet<>(level.getPlayers(p -> p.isAlive() && distanceToSqr(p) <= range * range));
        for (ServerPlayer p : List.copyOf(bossEvent.getPlayers())) {
            if (!near.contains(p)) {
                bossEvent.removePlayer(p);
            }
        }
        for (ServerPlayer p : near) {
            bossEvent.addPlayer(p);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    // =====================================================================
    // 第 2 形態：口から煙（クライアント）
    // =====================================================================

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() && isAlive() && isDualWielding()
                && tickCount % ModConstants.KOBOLD_LORD_RAGE_SMOKE_INTERVAL == 0) {
            float yaw = yHeadRot * Mth.DEG_TO_RAD;
            double fx = -Mth.sin(yaw);
            double fz = Mth.cos(yaw);
            double scale = getScale();
            level().addParticle(ParticleTypes.SMOKE, getX() + fx * 0.8 * scale, getY() + 1.65 * scale, getZ() + fz * 0.8 * scale,
                    fx * 0.02, 0.03, fz * 0.02);
        }
    }

    // =====================================================================
    // 着地点の範囲（クライアントの表示用）
    // =====================================================================

    /** 着地点の範囲を表示中なら、その中心（地面の高さ）。 */
    public @Nullable Vec3 getAoeCenter() {
        if (!this.entityData.get(DATA_AOE)) {
            return null;
        }
        Vector3fc v = this.entityData.get(DATA_AOE_CENTER);
        return new Vec3(v.x(), v.y(), v.z());
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
        /** しがみつく柱のブロックの位置（柱がなければ null）。 */
        private @Nullable BlockPos grabPillar;
        private Vec3 landing = Vec3.ZERO;
        /** 着地後に広がる衝撃波の、次に出す輪の番号。 */
        private int nextRing;

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
            playSound(SoundEvents.HOGLIN_ANGRY, 1.5f, 0.7f);
            if (level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.2, getZ(), 12, 0.6, 0.1, 0.6, 0.02);
            }
        }

        @Override
        public void stop() {
            // 途中で止まっても（死んだ・相手が消えたなど）、浮いたまま・範囲が出たままにならないように戻す
            setTelegraphing(false);
            showAoe(null);
            setNoGravity(false);
            resetFallDistance();
            phase = Phase.DONE;
            startSkillCooldown("kick", ModConfigs.koboldLordKickCooldownTicks());
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
                        playSound(SoundEvents.GOAT_LONG_JUMP, 1.5f, 0.7f);
                        if (level() instanceof ServerLevel level) {
                            // 踏み切りの土煙と破片
                            level.sendParticles(blockDebris(level, from), from.x, from.y + 0.1, from.z,
                                    ModConstants.KOBOLD_LORD_LEAP_DEBRIS, 0.5, 0.1, 0.5, 0.2);
                            level.sendParticles(ParticleTypes.POOF, from.x, from.y + 0.2, from.z, 10, 0.5, 0.1, 0.5, 0.05);
                        }
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
                        // 柱に爪を立てた衝撃で、柱の破片がこぼれる
                        playSound(SoundEvents.STONE_BREAK, 1.0f, 0.6f);
                        if (level() instanceof ServerLevel level) {
                            Vec3 wall = position().add(0, getBbHeight() * 0.4, 0);
                            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, grabBlock(level)),
                                    wall.x, wall.y, wall.z, ModConstants.KOBOLD_LORD_LEAP_DEBRIS, 0.4, 0.6, 0.4, 0.1);
                        }
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
                        playSound(SoundEvents.ENDER_DRAGON_FLAP, 1.5f, 1.3f);
                    }
                }
                case DIVE -> {
                    timer++;
                    double s = Math.min(1.0, timer / (double) ModConstants.KOBOLD_LORD_DIVE_TICKS);
                    moveTowards(from.lerp(landing, s));
                    if (level() instanceof ServerLevel level) {
                        // 突撃の軌跡
                        double mid = getBbHeight() * 0.5;
                        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + mid, getZ(),
                                ModConstants.KOBOLD_LORD_DIVE_TRAIL_PARTICLES, 0.4, mid * 0.5, 0.4, 0.0);
                        level.sendParticles(ParticleTypes.CRIT, getX(), getY() + mid, getZ(), 2, 0.4, mid * 0.5, 0.4, 0.1);
                    }
                    if (s >= 1.0) {
                        land();
                    }
                }
                case RECOVER -> {
                    // 衝撃波の輪を、内側から外側へ順に広げる
                    int elapsed = ModConstants.KOBOLD_LORD_RECOVER_TICKS - timer;
                    if (nextRing < ModConstants.KOBOLD_LORD_SHOCKWAVE_RINGS
                            && elapsed % ModConstants.KOBOLD_LORD_SHOCKWAVE_RING_INTERVAL == 0
                            && level() instanceof ServerLevel level) {
                        shockwaveRing(level, nextRing++);
                    }
                    if (--timer <= 0) {
                        phase = Phase.DONE;
                    }
                }
                default -> { }
            }
        }

        /** 着地：爆発と衝撃波。範囲の中のプレイヤーにダメージ＋吹き飛ばし、範囲を消して重力を戻す。 */
        private void land() {
            setNoGravity(false);
            setDeltaMovement(Vec3.ZERO);
            showAoe(null);
            triggerAnim(ACTION_CONTROLLER, "land");
            if (level() instanceof ServerLevel level) {
                double r = ModConfigs.koboldLordKickRadius();
                double knockback = ModConfigs.koboldLordKickKnockback();
                for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(r + 1, ModConstants.KOBOLD_LORD_KICK_HIT_HEIGHT, r + 1),
                        p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
                    double dx = p.getX() - landing.x;
                    double dz = p.getZ() - landing.z;
                    if (dx * dx + dz * dz <= r * r && Math.abs(p.getY() - landing.y) <= ModConstants.KOBOLD_LORD_KICK_HIT_HEIGHT) {
                        p.hurtServer(level, damageSources().mobAttack(KoboldLordEntity.this), skillDamage("kick", ModConfigs.koboldLordKickDamage()));
                        if (knockback > 0) {
                            launch(p, landing, knockback);
                        }
                    }
                }
                // 爆発・飛び散る地面の破片・最初の衝撃波の輪
                level.sendParticles(ParticleTypes.EXPLOSION, landing.x, landing.y + 0.5, landing.z, 1, 0, 0, 0, 0);
                level.sendParticles(blockDebris(level, landing), landing.x, landing.y + 0.2, landing.z,
                        ModConstants.KOBOLD_LORD_LANDING_DEBRIS, r * 0.4, 0.2, r * 0.4, 0.3);
                nextRing = 0;
                shockwaveRing(level, nextRing++);
            }
            playSound(SoundEvents.MACE_SMASH_GROUND_HEAVY, 2.0f, 0.8f);
            playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.0f, 1.3f);
            playSound(SoundEvents.ANVIL_LAND, 0.5f, 0.8f);
            phase = Phase.RECOVER;
            timer = ModConstants.KOBOLD_LORD_RECOVER_TICKS;
        }

        /** 着地の衝撃波の i 番目の輪（i = 0 がいちばん内側。最後の輪が着地点の範囲の端）。 */
        private void shockwaveRing(ServerLevel level, int i) {
            double radius = ModConfigs.koboldLordKickRadius() * (i + 1) / ModConstants.KOBOLD_LORD_SHOCKWAVE_RINGS;
            particleRing(level, groundDust(level, landing), landing, radius, 0.1);
            particleRing(level, ParticleTypes.POOF, landing, radius, 0.3);
        }

        /** しがみついている柱のブロック（破片の見た目用）。柱がなければ足もとのブロック。 */
        private BlockState grabBlock(ServerLevel level) {
            if (grabPillar != null) {
                BlockState state = level.getBlockState(grabPillar);
                if (!state.isAir()) {
                    return state;
                }
            }
            return groundBlock(level, from);
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
                grabPillar = null;
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
            grabPillar = BlockPos.containing(cx, gy + 1, cz);
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
        if (hasCustomName()) {
            bossEvent.setName(getDisplayName());
        }
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
                .triggerableAnim("draw", DRAW)
                .triggerableAnim("land", LAND));
    }
}
