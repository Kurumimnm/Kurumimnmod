package io.github.kurumimnm.kurumimnmod.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * スライム（MOD 独自。敵対モブ）。バニラのスライムとは別物で、<b>分裂しない</b>・大きさは 1 種類。
 * <p>
 * 【移動】バニラのスライムと同じく、地面にいる間は止まって力をため、跳んでいる間だけ前へ進む
 *        （{@link HopMoveControl}）。経路探索はふつうの地上ナビなので、段差や回り道もできる。
 * 【攻撃】跳ねて近づき、体当たりする（近接。攻撃の間隔はバニラの 1 秒）。
 * 【能力値】ModConstants の SLIME_*。
 * 【出現】クエストのフィールド（placed）・スポーンエッグ・/summon。自然スポーンなし。ドロップなし。
 * 【見た目】GeckoLib（slime.geo.json）。アニメーション：idle（ぷるぷる）／ jump（空中で縦に伸びる）。
 */
public class SlimeEntity extends Monster implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.slime.idle");
    private static final RawAnimation JUMP = RawAnimation.begin().thenLoop("animation.slime.jump");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SlimeEntity(EntityType<? extends SlimeEntity> type, Level level) {
        super(type, level);
        this.moveControl = new HopMoveControl(this);
    }

    /** 属性（数値は ModConstants の SLIME_*）。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, ModConstants.SLIME_MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ModConstants.SLIME_ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, ModConstants.SLIME_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, ModConstants.SLIME_FOLLOW_RANGE);
    }

    // =====================================================================
    // AI
    // =====================================================================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, ModConstants.SLIME_CHASE_SPEED_MODIFIER, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, ModConstants.SLIME_WANDER_SPEED_MODIFIER));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 次に跳ぶまでの間（tick）。追いかけている間は短くする。 */
    int nextJumpDelay() {
        int delay = ModConstants.SLIME_JUMP_DELAY_MIN
                + getRandom().nextInt(ModConstants.SLIME_JUMP_DELAY_MAX - ModConstants.SLIME_JUMP_DELAY_MIN + 1);
        return getTarget() != null ? Math.max(1, delay / 3) : delay;
    }

    // =====================================================================
    // その他
    // =====================================================================

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.SLIME_SPAWN_EGG.get());
    }

    // ---- 音（バニラの小さいスライムの音） ----
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }

    /** 跳んだときの音。 */
    void playJumpSound() {
        playSound(SoundEvents.SLIME_JUMP_SMALL, getSoundVolume(), getVoicePitch());
    }

    // =====================================================================
    // GeckoLib
    // =====================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 空中 → jump / 地面 → idle
        controllers.add(new AnimationController<SlimeEntity>("main", 2,
                test -> test.setAndContinue(test.animatable().onGround() ? IDLE : JUMP)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // =====================================================================
    // 移動
    // =====================================================================

    /**
     * 跳ねて進む移動制御（バニラの SlimeMoveControl と同じ考え方）。
     * 地面にいる間は止まり、間（{@link #nextJumpDelay}）が来たら跳ぶ。前へ進むのは跳んでいる間だけ。
     */
    static class HopMoveControl extends MoveControl {
        private final SlimeEntity slime;
        private int jumpDelay;

        HopMoveControl(SlimeEntity slime) {
            super(slime);
            this.slime = slime;
        }

        @Override
        public void tick() {
            if (this.operation != MoveControl.Operation.MOVE_TO) {
                this.mob.setZza(0.0f);
                return;
            }
            this.operation = MoveControl.Operation.WAIT;
            // 行き先の方へ向く
            double dx = this.wantedX - this.mob.getX();
            double dz = this.wantedZ - this.mob.getZ();
            if (dx * dx + dz * dz > 1.0E-4) {
                float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0f;
                this.mob.setYRot(this.rotlerp(this.mob.getYRot(), yaw, 90.0f));
            }
            this.mob.yBodyRot = this.mob.getYRot();
            this.mob.yHeadRot = this.mob.getYRot();
            float speed = (float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
            if (this.mob.onGround()) {
                if (this.jumpDelay-- <= 0) {
                    this.jumpDelay = this.slime.nextJumpDelay();
                    this.mob.setSpeed(speed);
                    this.slime.getJumpControl().jump();
                    this.slime.playJumpSound();
                } else {
                    // 力をためている間は止まる
                    this.mob.setXxa(0.0f);
                    this.mob.setZza(0.0f);
                    this.mob.setSpeed(0.0f);
                }
            } else {
                this.mob.setSpeed(speed);
            }
        }
    }
}
