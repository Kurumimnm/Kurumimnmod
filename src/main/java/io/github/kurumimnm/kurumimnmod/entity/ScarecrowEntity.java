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
import io.github.kurumimnm.kurumimnmod.network.ScarecrowDamagePayload;
import io.github.kurumimnm.kurumimnmod.registry.ModEntities;
import io.github.kurumimnm.kurumimnmod.registry.ModItems;
import io.github.kurumimnm.kurumimnmod.scarecrow.DamageKind;
import io.github.kurumimnm.kurumimnmod.scarecrow.ScarecrowSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * カカシ（ステータス実験用の的）。
 * <p>
 * 【召喚・削除】召喚用ステッキでブロックを右クリック（OP のみ）。削除は設定画面の「削除」ボタン（OP のみ）。
 * 【設定】設定用ステッキで狙って左クリック → 設定画面（誰でも可）。設定はカカシ 1 体ごとに保存される
 *        （{@link ScarecrowSettings}）。
 * 【被弾】受けたダメージ（防具・防御力・魔法耐性で軽減した最終値）を頭上に数字で表示する。
 *        ダメージの計算・表示は scarecrow/ScarecrowEvents。
 *        HP 無限なら HP は減らない。有限なら削り切ったときに全回復し、しばらく攻撃されなくても全回復する。
 * 【移動】AI なし・重力なし。押されても動かない。ノックバックでずれたら、少し待ってから召喚位置へ滑って戻る。
 *        向きは召喚時の向きで固定。
 * 【反撃】ON のとき、射程内で一番近いプレイヤー（サバイバル／アドベンチャー）を一定間隔で攻撃する。
 *        プレイヤーの防具・防御力ステータスで軽減され、受けたダメージはアクションバーに表示される。
 */
public class ScarecrowEntity extends Mob implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.scarecrow.idle");
    private static final RawAnimation HIT = RawAnimation.begin().thenPlay("animation.scarecrow.hit");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.scarecrow.attack");
    /** 被弾・攻撃のアニメーションを流すコントローラー名（サーバーから triggerAnim で再生する）。 */
    private static final String ACTION_CONTROLLER = "action";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ---- 保存する値 ----
    private ScarecrowSettings settings = ScarecrowSettings.FALLBACK;
    /** 召喚された位置（ここへ戻る）。古いデータなどで無い場合は最初の tick の位置にする。 */
    private Vec3 home;
    /** 召喚時の向き（ずっとこの向きを向く）。 */
    private float homeYaw;

    // ---- サーバー側だけで使う一時的な値（保存しない） ----
    /** 最後にダメージを受けたゲーム時刻（戻り始め・自動回復の判定用）。 */
    private long lastHurtTime = Long.MIN_VALUE / 2;
    /** クリティカルが出たゲーム時刻（同じ tick のダメージをクリティカル表示にする）。 */
    private long lastCritTime = Long.MIN_VALUE / 2;
    /** 次に反撃できるまでの tick。 */
    private int counterCooldown;

    public ScarecrowEntity(EntityType<? extends ScarecrowEntity> type, Level level) {
        super(type, level);
        // 杭で立っているイメージなので重力なし（ノックバックで浮いてもその場で止まり、召喚位置へ戻る）
        setNoGravity(true);
    }

    /** 属性（最大 HP 等は召喚時・設定変更時に設定値で上書きする）。 */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, ModConstants.SCARECROW_BASE_HEALTH);
    }

    /**
     * カカシを召喚する（サーバー側）。召喚位置がブロックと重なっている場合は召喚せず null を返す。
     *
     * @param pos 足元の位置
     * @param yaw 向き（度）
     */
    public static ScarecrowEntity summon(ServerLevel level, Vec3 pos, float yaw) {
        ScarecrowEntity scarecrow = new ScarecrowEntity(ModEntities.SCARECROW.get(), level);
        scarecrow.snapTo(pos.x, pos.y, pos.z, yaw, 0f);
        scarecrow.home = pos;
        scarecrow.homeYaw = yaw;
        scarecrow.lockRotation();
        scarecrow.setSettings(ScarecrowSettings.fromConfig());
        scarecrow.setPersistenceRequired();
        if (!level.noCollision(scarecrow)) {
            return null;
        }
        level.addFreshEntity(scarecrow);
        return scarecrow;
    }

    // =====================================================================
    // 設定
    // =====================================================================

    public ScarecrowSettings getSettings() {
        return settings;
    }

    /** 設定を変更し、属性へ反映して HP を全回復する。 */
    public void setSettings(ScarecrowSettings newSettings) {
        this.settings = newSettings.sanitized();
        applySettingsToAttributes();
        setHealth(getMaxHealth());
        this.counterCooldown = this.settings.counterIntervalTicks();
    }

    /** 最大 HP・防具ポイント・ノックバック耐性をバニラの属性に反映する。 */
    private void applySettingsToAttributes() {
        setBase(getAttribute(Attributes.MAX_HEALTH), settings.maxHealth());
        setBase(getAttribute(Attributes.ARMOR), settings.armor());
        setBase(getAttribute(Attributes.KNOCKBACK_RESISTANCE), settings.knockbackResistance() / 100.0);
    }

    private static void setBase(AttributeInstance attribute, double value) {
        if (attribute != null && attribute.getBaseValue() != value) {
            attribute.setBaseValue(value);
        }
    }

    // =====================================================================
    // 被弾（ScarecrowEvents から呼ばれる）
    // =====================================================================

    /** クリティカル判定が出たことを記録する（この tick のダメージをクリティカル表示にする）。 */
    public void markCritical() {
        this.lastCritTime = level().getGameTime();
    }

    /** この tick にクリティカルが出ていたか。 */
    public boolean wasCriticalThisTick() {
        return this.lastCritTime == level().getGameTime();
    }

    /**
     * 受けたダメージを記録し、近くのプレイヤーへ数字の表示を送る。
     *
     * @param amount 軽減後の最終ダメージ
     */
    public void recordHit(float amount, DamageKind kind) {
        this.lastHurtTime = level().getGameTime();
        PacketDistributor.sendToPlayersTrackingEntity(this, new ScarecrowDamagePayload(getId(), amount, kind.ordinal()));
        triggerAnim(ACTION_CONTROLLER, "hit");
    }

    /** HP を全回復する（削り切ったとき・自動回復）。 */
    public void restoreHealth() {
        setHealth(getMaxHealth());
    }

    /** 足場の中・水没・落下・圧死などの環境ダメージは受けない（実験の邪魔になるため）。 */
    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.FALL) || source.is(DamageTypes.CRAMMING)) {
            return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        }
        return super.isInvulnerableTo(level, source);
    }

    // =====================================================================
    // 毎 tick の処理
    // =====================================================================

    @Override
    public void tick() {
        super.tick();
        lockRotation();
        if (level() instanceof ServerLevel serverLevel) {
            returnHome();
            regenerate();
            counterAttack(serverLevel);
        }
    }

    /** 向きを召喚時の向きに固定する（AI がないので体・頭が勝手に回らないように）。 */
    private void lockRotation() {
        setYRot(homeYaw);
        setXRot(0f);
        this.yRotO = homeYaw;
        this.yBodyRot = homeYaw;
        this.yBodyRotO = homeYaw;
        this.yHeadRot = homeYaw;
        this.yHeadRotO = homeYaw;
    }

    /** ノックバック等で召喚位置からずれたら、少し待ってから滑るように戻る。 */
    private void returnHome() {
        if (home == null) {
            // /summon などステッキ以外で出た場合：今の位置・向きを召喚位置にし、設定は Config の初期値にする
            home = position();
            homeYaw = getYRot();
            if (settings == ScarecrowSettings.FALLBACK) {
                setSettings(ScarecrowSettings.fromConfig());
            }
            return;
        }
        Vec3 toHome = home.subtract(position());
        double distance = toHome.length();
        if (distance <= ModConstants.SCARECROW_HOME_EPSILON) {
            return;
        }
        long sinceHurt = level().getGameTime() - lastHurtTime;
        if (sinceHurt < Math.round(ModConfigs.scarecrowReturnDelay() * 20)) {
            // 飛ばされている間は水平方向だけ動かす（上へ浮き続けないように）
            Vec3 motion = getDeltaMovement();
            setDeltaMovement(motion.x, 0, motion.z);
            return;
        }
        double step = Math.min(distance, ModConfigs.scarecrowReturnSpeed());
        setDeltaMovement(Vec3.ZERO);
        setPos(position().add(toHome.scale(step / distance)));
    }

    /** 有限 HP のとき、一定時間攻撃されなければ全回復する。 */
    private void regenerate() {
        if (settings.infiniteHealth() || getHealth() >= getMaxHealth()) {
            return;
        }
        long sinceHurt = level().getGameTime() - lastHurtTime;
        if (sinceHurt >= Math.round(ModConfigs.scarecrowRegenDelay() * 20)) {
            restoreHealth();
        }
    }

    /** 反撃：射程内で一番近いプレイヤーを一定間隔で攻撃する。 */
    private void counterAttack(ServerLevel level) {
        if (!settings.counterEnabled()) {
            return;
        }
        if (counterCooldown > 0) {
            counterCooldown--;
            return;
        }
        double range = settings.counterRange();
        Player target = level.getNearestPlayer(getX(), getY() + getBbHeight() / 2, getZ(), range,
                e -> e instanceof Player p && p.isAlive() && !p.isCreative() && !p.isSpectator());
        if (target == null || !hasLineOfSight(target)) {
            return; // 相手がいないときはクールダウンを消費しない（近づいた瞬間に攻撃できる）
        }
        target.hurtServer(level, damageSources().mobAttack(this), settings.counterDamage());
        triggerAnim(ACTION_CONTROLLER, "attack");
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 0.8f, 0.8f);
        counterCooldown = settings.counterIntervalTicks();
    }

    // =====================================================================
    // 保存
    // =====================================================================

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("ScarecrowSettings", ScarecrowSettings.CODEC, settings);
        if (home != null) {
            output.store("Home", Vec3.CODEC, home);
        }
        output.putFloat("HomeYaw", homeYaw);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.settings = input.read("ScarecrowSettings", ScarecrowSettings.CODEC).orElse(ScarecrowSettings.FALLBACK).sanitized();
        this.home = input.read("Home", Vec3.CODEC).orElse(null);
        this.homeYaw = input.getFloatOr("HomeYaw", getYRot());
        applySettingsToAttributes();
    }

    // =====================================================================
    // バニラの挙動の無効化（動かない・消えない・物を拾わない など）
    // =====================================================================

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
        // 他のエンティティに押されても動かない
    }

    @Override
    protected void doPush(Entity entity) {
        // 他のエンティティを押さない
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    protected boolean shouldDropLoot(ServerLevel level) {
        return false;
    }

    /** クリエイティブのピックブロック（ホイールクリック）で召喚用ステッキを取れるようにする。 */
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.SCARECROW_SUMMON_STICK.get());
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ARMOR_STAND_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ARMOR_STAND_BREAK;
    }

    // =====================================================================
    // GeckoLib
    // =====================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 待機：ゆらゆら揺れ続ける
        controllers.add(new AnimationController<>("main", test -> test.setAndContinue(IDLE)));
        // 被弾・反撃：サーバーから triggerAnim で再生する（普段は何もしない）
        controllers.add(new AnimationController<ScarecrowEntity>(ACTION_CONTROLLER, test -> PlayState.STOP)
                .triggerableAnim("hit", HIT)
                .triggerableAnim("attack", ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
