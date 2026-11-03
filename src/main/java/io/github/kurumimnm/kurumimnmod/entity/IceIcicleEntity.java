package io.github.kurumimnm.kurumimnmod.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.magic.MagicTargeting;
import io.github.kurumimnm.kurumimnmod.registry.ModEntities;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 氷柱の杖が撃ち出す大きな氷柱。
 * <p>
 * 【動き】
 * <ul>
 *   <li>重力なし・一定速度でまっすぐ飛ぶ</li>
 *   <li>敵に当たっても消えずに貫通する（通過した敵には中心と同じダメージ。1 体につき 1 回）</li>
 *   <li>地面・壁に当たるか、狙った地点（最大距離）まで飛ぶと着弾する</li>
 * </ul>
 * 【着弾】半径 {@code radius} 内の敵全員にダメージ。中心ほど高く、端で {@code edgeDamageRatio} 倍まで下がる。
 * 貫通時にダメージを与えた敵には、着弾ダメージは与えない（二重ヒットなし）。
 * 【鈍足】ダメージが入った敵（貫通・着弾とも）に鈍足を付ける（時間・強さは Config）。
 * <p>
 * ダメージは「間接魔法」扱い（kurumimnmod:magic_attack タグ）なので、
 * 知力の倍率は StatEvents で自動的にかかる。ここでは知力を考慮しない基本値を渡す。
 * 数値はすべて Config の [magic.iceWand]。
 */
public class IceIcicleEntity extends Projectile implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ice_icicle.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ---- サーバー側だけで使う値（保存しない。ワールド再読み込み時はその場で着弾して消える） ----
    /** 着弾地点までの距離（ここまで飛んだら着弾）。 */
    private double maxDistance;
    /** これまでに飛んだ距離。 */
    private double traveled;
    /** 貫通でダメージを与えたエンティティ（着弾ダメージの二重ヒット防止）。 */
    private final Set<Integer> piercedIds = new HashSet<>();

    public IceIcicleEntity(EntityType<? extends IceIcicleEntity> type, Level level) {
        super(type, level);
    }

    /**
     * プレイヤーの頭上から、狙った場所に向けて氷柱を撃ち出す（サーバー側）。
     */
    public static void launch(Player owner) {
        Level level = owner.level();
        Vec3 origin = MagicTargeting.overheadOrigin(owner, ModConfigs.iceWandSpawnHeight());
        Vec3 target = MagicTargeting.find(owner, ModConfigs.iceWandRange()).position();
        Vec3 toTarget = target.subtract(origin);
        double distance = toTarget.length();
        Vec3 direction = distance < 1.0E-4 ? owner.getViewVector(1.0f) : toTarget.scale(1.0 / distance);

        IceIcicleEntity icicle = new IceIcicleEntity(ModEntities.ICE_ICICLE.get(), level);
        icicle.setOwner(owner);
        icicle.setPos(origin);
        icicle.setDeltaMovement(direction.scale(ModConfigs.iceWandSpeed()));
        icicle.maxDistance = distance;
        level.addFreshEntity(icicle);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // 同期するデータはなし（向きは速度から求める）
    }

    // =====================================================================
    // 移動・当たり判定
    // =====================================================================

    @Override
    public void tick() {
        super.tick();
        Vec3 pos = position();
        Vec3 velocity = getDeltaMovement();
        double speed = velocity.length();
        boolean server = level() instanceof ServerLevel;

        if (speed < 1.0E-4 || (server && tickCount > ModConstants.ICE_ICICLE_MAX_LIFETIME)) {
            if (server) {
                discard();
            }
            return;
        }

        Vec3 next = pos.add(velocity);
        boolean landed = false;

        // 狙った地点（最大距離）に届いたら着弾
        if (server && traveled + speed >= maxDistance) {
            next = pos.add(velocity.scale(Math.max(0, maxDistance - traveled) / speed));
            landed = true;
        }

        // 地面・壁に当たったら着弾
        BlockHitResult blockHit = level().clip(
                new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) {
            next = blockHit.getLocation();
            landed = true;
        }

        if (level() instanceof ServerLevel serverLevel) {
            pierceEntities(serverLevel, pos, next);
            traveled += pos.distanceTo(next);
        } else {
            spawnTrailParticles(pos, next);
        }

        setPos(next);

        if (landed) {
            if (level() instanceof ServerLevel serverLevel) {
                impact(serverLevel, next, velocity.scale(1.0 / speed));
                discard();
            } else {
                // クライアントはサーバーからの削除を待つ間、その場で止めておく
                setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    /** 飛んでいる途中で通過した敵にダメージを与える（1 体につき 1 回）。 */
    private void pierceEntities(ServerLevel level, Vec3 from, Vec3 to) {
        double r = ModConstants.ICE_ICICLE_HIT_RADIUS;
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(from, to).inflate(r), this::canDamage);
        float damage = (float) ModConfigs.iceWandDamage();
        for (LivingEntity target : candidates) {
            if (piercedIds.contains(target.getId())) {
                continue;
            }
            AABB box = target.getBoundingBox().inflate(r);
            if (box.contains(from) || box.clip(from, to).isPresent()) {
                piercedIds.add(target.getId());
                hit(level, target, damage);
            }
        }
    }

    // =====================================================================
    // 着弾
    // =====================================================================

    /**
     * 着弾：範囲内の敵全員に、中心からの距離に応じたダメージを与える。
     *
     * @param direction 飛んできた向き（壁越しの判定を避けるため、少し手前を中心にする）
     */
    private void impact(ServerLevel level, Vec3 center, Vec3 direction) {
        double radius = ModConfigs.iceWandRadius();
        double maxDamage = ModConfigs.iceWandDamage();
        double edgeRatio = ModConfigs.iceWandEdgeDamageRatio();
        // 着弾面にめり込んだ位置だと視線チェックが壁に当たるので、少しだけ手前に戻す
        Vec3 eye = center.subtract(direction.scale(0.1));

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius), this::canDamage);
        for (LivingEntity target : targets) {
            if (piercedIds.contains(target.getId())) {
                continue;
            }
            // 中心から当たり判定の一番近い点までの距離（大きい敵ほど当たりやすい）
            double distance = distanceToBox(center, target.getBoundingBox());
            if (distance > radius || !hasLineOfSight(level, eye, target)) {
                continue;
            }
            // 中心 = 100%、端 = edgeRatio に直線的に下がる
            double ratio = Mth.lerp(distance / radius, 1.0, edgeRatio);
            hit(level, target, (float) (maxDamage * ratio));
        }

        spawnImpactEffects(level, center, radius);
    }

    /**
     * ダメージを与え、入ったら鈍足を付ける。
     * 鈍足の付与者を撃った人にしておくと、アシスト判定（デバフで関わった）にも数えられる。
     */
    private void hit(ServerLevel level, LivingEntity target, float damage) {
        if (!target.hurtServer(level, damageSource(), damage)) {
            return; // 無敵時間中・無効化されたときは鈍足も付けない
        }
        int duration = ModConfigs.iceWandSlownessDuration();
        if (duration > 0 && target.isAlive()) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration,
                    ModConfigs.iceWandSlownessAmplifier()), getOwner());
        }
    }

    /** 点から AABB までの最短距離（中にいれば 0）。 */
    private static double distanceToBox(Vec3 p, AABB box) {
        double dx = Math.max(Math.max(box.minX - p.x, 0), p.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - p.y, 0), p.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - p.z, 0), p.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** 壁越しの敵には当てない（着弾点から敵の中心までブロックがないか）。 */
    private boolean hasLineOfSight(ServerLevel level, Vec3 from, LivingEntity target) {
        Vec3 to = target.getBoundingBox().getCenter();
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                .getType() == HitResult.Type.MISS;
    }

    /** ダメージを与えてよい相手か（撃った本人・その仲間やペット・同じ乗り物の相手は除く）。 */
    private boolean canDamage(LivingEntity target) {
        if (!target.isAlive() || target.isSpectator()) {
            return false;
        }
        Entity owner = getOwner();
        if (owner == null) {
            return true;
        }
        return target != owner
                && !owner.isPassengerOfSameVehicle(target)
                && !target.isAlliedTo(owner)
                && !owner.isAlliedTo(target);
    }

    /** 間接魔法ダメージ（直接の原因 = 氷柱、攻撃者 = 撃った人）。 */
    private DamageSource damageSource() {
        return level().damageSources().indirectMagic(this, getOwner());
    }

    // =====================================================================
    // 演出
    // =====================================================================

    /** 飛んでいる間の雪の軌跡（クライアント側）。 */
    private void spawnTrailParticles(Vec3 from, Vec3 to) {
        for (int i = 0; i < 3; i++) {
            Vec3 p = from.lerp(to, random.nextDouble());
            level().addParticle(ParticleTypes.SNOWFLAKE,
                    p.x + (random.nextDouble() - 0.5) * 0.4,
                    p.y + (random.nextDouble() - 0.5) * 0.4,
                    p.z + (random.nextDouble() - 0.5) * 0.4,
                    0, 0, 0);
        }
    }

    /** 着弾時の演出：氷が砕ける破片＋範囲の外周に雪の輪。 */
    private void spawnImpactEffects(ServerLevel level, Vec3 center, double radius) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
                center.x, center.y + 0.2, center.z, 60, 0.6, 0.4, 0.6, 0.15);
        level.sendParticles(ParticleTypes.SNOWFLAKE,
                center.x, center.y + 0.3, center.z, 30, radius * 0.4, 0.2, radius * 0.4, 0.02);
        int points = Math.max(16, (int) (radius * 12));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    center.x + Math.cos(angle) * radius, center.y + 0.15, center.z + Math.sin(angle) * radius,
                    1, 0, 0, 0, 0);
        }
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 0.7f);
    }

    // =====================================================================
    // その他
    // =====================================================================

    /** 攻撃して壊したりはできない。 */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    // ---- GeckoLib ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
