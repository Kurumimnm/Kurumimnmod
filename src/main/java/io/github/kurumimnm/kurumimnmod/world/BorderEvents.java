package io.github.kurumimnm.kurumimnmod.world;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * ボーダー（赤い壁）越しには何もできないようにする。サーバー・クライアント両方で動く。
 * <ul>
 *   <li>壁の向こうのブロックを壊す・置く・使う（チェストを開けるなど）</li>
 *   <li>壁の向こうのエンティティを叩く・使う。壁越しにダメージを与える／受ける</li>
 *   <li>矢・雪玉などの飛び道具が壁を抜ける（抜けたところで消える）</li>
 *   <li>爆発が壁の向こうを壊す・傷つける</li>
 *   <li>エンダーマン・エンダーパール・コーラスフルーツで壁の向こうへワープする</li>
 *   <li>水・溶岩が壁の向こうへ流れる</li>
 * </ul>
 * 歩いて越えられないこと自体は当たり判定（{@link Borders#wallShapes}）で止めている。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class BorderEvents {

    private BorderEvents() {}

    // ---- ブロック ----

    @SubscribeEvent
    static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (blockedAt(event.getEntity(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onBreak(BreakBlockEvent event) {
        if (blockedAt(event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getPos();
        BlockPos placed = event.getFace() == null ? pos : pos.relative(event.getFace());
        if (blockedAt(event.getEntity(), pos) || blockedAt(event.getEntity(), placed)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() != null && blockedAt(event.getEntity(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (!(event.getLevel() instanceof Level level)) {
            return;
        }
        BlockPos from = event.getLiquidPos();
        BlockPos to = event.getPos();
        if ((from.getX() != to.getX() || from.getZ() != to.getZ()) && differentZone(level, from, to)) {
            event.setCanceled(true);
        }
    }

    // ---- エンティティ ----

    @SubscribeEvent
    static void onAttack(AttackEntityEvent event) {
        if (Borders.blocked(event.getEntity(), event.getTarget().getX(), event.getTarget().getZ())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (Borders.blocked(event.getEntity(), event.getTarget().getX(), event.getTarget().getZ())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        Entity direct = event.getSource().getDirectEntity();
        if ((attacker != null && Borders.attackBlocked(attacker, event.getEntity()))
                || (direct != null && direct != attacker && Borders.attackBlocked(direct, event.getEntity()))) {
            event.setCanceled(true);
        }
    }

    // ---- 飛び道具 ----

    /** 飛び道具は普通の当たり判定を通らずに動くので、1 tick ごとに「壁を抜けたか」を調べて消す。 */
    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Projectile projectile) || projectile.level().isClientSide()) {
            return;
        }
        FieldRegion before = Borders.regionAt(projectile.level(), projectile.xo, projectile.zo);
        if (before == null) {
            return;
        }
        FieldRegion now = Borders.regionAt(projectile.level(), projectile.getX(), projectile.getZ());
        if (now != null && !Borders.mayEnter(projectile, before, now)) {
            projectile.discard();
        }
    }

    @SubscribeEvent
    static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        HitResult hit = event.getRayTraceResult();
        if (hit.getType() != HitResult.Type.MISS) {
            Vec3 at = hit.getLocation();
            if (Borders.blocked(projectile, at.x, at.z)) {
                event.setCanceled(true);
                if (!projectile.level().isClientSide()) {
                    projectile.discard();
                }
            }
        }
    }

    // ---- 爆発 ----

    @SubscribeEvent
    static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        Vec3 center = event.getExplosion().center();
        FieldRegion origin = Borders.regionAt(level, center.x, center.z);
        if (origin == null) {
            return;
        }
        event.getAffectedBlocks().removeIf(pos -> !sameZone(level, origin, pos.getX() + 0.5, pos.getZ() + 0.5));
        event.getAffectedEntities().removeIf(entity -> !sameZone(level, origin, entity.getX(), entity.getZ()));
    }

    // ---- ワープ ----

    @SubscribeEvent
    static void onEnderTeleport(EntityTeleportEvent.EnderEntity event) {
        cancelTeleportIfBlocked(event);
    }

    @SubscribeEvent
    static void onPearlTeleport(EntityTeleportEvent.EnderPearl event) {
        cancelTeleportIfBlocked(event);
    }

    @SubscribeEvent
    static void onItemTeleport(EntityTeleportEvent.ItemConsumption event) {
        cancelTeleportIfBlocked(event);
    }

    private static <T extends EntityTeleportEvent & net.neoforged.bus.api.ICancellableEvent> void cancelTeleportIfBlocked(T event) {
        if (Borders.blocked(event.getEntity(), event.getTargetX(), event.getTargetZ())) {
            event.setCanceled(true);
        }
    }

    // ---- 共通 ----

    private static boolean blockedAt(Entity entity, BlockPos pos) {
        return Borders.blocked(entity, pos.getX() + 0.5, pos.getZ() + 0.5);
    }

    private static boolean differentZone(Level level, BlockPos a, BlockPos b) {
        FieldRegion ra = Borders.regionAt(level, a.getX() + 0.5, a.getZ() + 0.5);
        FieldRegion rb = Borders.regionAt(level, b.getX() + 0.5, b.getZ() + 0.5);
        return ra != null && rb != null && !ra.sameZone(rb);
    }

    private static boolean sameZone(Level level, FieldRegion origin, double x, double z) {
        FieldRegion region = Borders.regionAt(level, x, z);
        return region == null || region.sameZone(origin);
    }
}
