package io.github.kurumimnm.kurumimnmod.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
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

/**
 * ミニオン（小鬼。敵対モブ）。チュートリアルの最初の相手。
 * <p>
 * 【行動】プレイヤーを見つけると走って近づき、手の棍棒で殴る（ゾンビと同じ近接。攻撃の間隔はバニラの 1 秒）。
 *        攻撃されたら仲間を呼ばずに反撃する。普段はうろつく。
 * 【能力値】HP・攻撃力・速さは ModConstants の MINION_*（登録時に決まるので Config ではなく定数）。
 * 【出現】クエストのフィールド（placed）・スポーンエッグ・/summon。自然スポーンなし。ドロップなし。
 * 【見た目】GeckoLib（minion.geo.json）。アニメーション：idle / walk / attack（棍棒を振り下ろす）。
 */
public class MinionEntity extends Monster implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.minion.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.minion.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.minion.attack");
    /** 攻撃のアニメーションを流すコントローラー名（サーバーから triggerAnim で再生する）。 */
    private static final String ACTION_CONTROLLER = "action";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MinionEntity(EntityType<? extends MinionEntity> type, Level level) {
        super(type, level);
    }

    /** 属性（数値は ModConstants の MINION_*）。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, ModConstants.MINION_MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ModConstants.MINION_ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, ModConstants.MINION_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, ModConstants.MINION_FOLLOW_RANGE);
    }

    // =====================================================================
    // AI
    // =====================================================================

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, ModConstants.MINION_CHASE_SPEED_MODIFIER, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, ModConstants.MINION_WANDER_SPEED_MODIFIER));
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
    // その他
    // =====================================================================

    /** クリエイティブのピックブロック（ホイールクリック）でスポーンエッグを取れるようにする。 */
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.MINION_SPAWN_EGG.get());
    }

    // ---- 鳴き声（仮：ピグリンの声を高くして使う。専用の音は後で差し替え） ----
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PIGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PIGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIGLIN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return ModConstants.MINION_VOICE_PITCH + (getRandom().nextFloat() - 0.5f) * 0.15f;
    }

    // =====================================================================
    // GeckoLib
    // =====================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<MinionEntity>("main", 4,
                test -> test.setAndContinue(test.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<MinionEntity>(ACTION_CONTROLLER, test -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
