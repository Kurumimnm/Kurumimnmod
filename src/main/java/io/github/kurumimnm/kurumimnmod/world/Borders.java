package io.github.kurumimnm.kurumimnmod.world;

import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * ボーダー（赤い壁）を「越えられない壁」にするためのルール。サーバー・クライアント共通。
 * <ul>
 *   <li><b>プレイヤー</b>：{@link GateRules#canEnter} で入れない場所には入れない。OP・クリエイティブも同じ（スペクテイターだけ素通り）</li>
 *   <li><b>プレイヤーが乗っている乗り物・プレイヤーのペット・プレイヤーの飛び道具</b>：そのプレイヤーと同じルール</li>
 *   <li><b>それ以外（モブ・アイテムなど）</b>：壁（{@link GateRules#hasWall}）がある境目は越えられず、壁越しに攻撃もできない。
 *       壁がなければいつも通り動ける。壁は人によって違うので、モブが狙っているプレイヤー、
 *       いなければ近く（{@link ModConstants#GATE_MOB_PLAYER_RANGE} ブロック以内）のいちばん近いプレイヤーから見た壁で決める。
 *       近くにプレイヤーがいなければ越えられない</li>
 *   <li><b>街は安全地帯</b>：モブ・アイテムなどは、壁が消えた街にも外から入れない（街の中のプレイヤーを狙わない・攻撃しない）</li>
 * </ul>
 * 場所は柱（1 ブロック）単位で決める（{@link RegionGrid}）。
 */
public final class Borders {

    private Borders() {}

    /** その座標を含む柱の場所。フィールドマップのワールドでなければ null。 */
    public static @Nullable FieldRegion regionAt(Level level, double x, double z) {
        FieldMap map = FieldLookup.mapOrNull(level);
        return map == null ? null : RegionGrid.at(map, x, z);
    }

    /**
     * そのエンティティのルールを決めるプレイヤー。
     *
     * @param player     そのプレイヤー
     * @param controlled true = プレイヤー本人・乗っている乗り物・ペット・プレイヤーの飛び道具（そのプレイヤーとして入れるかで決める）。
     *                   false = モブ・アイテムなど（そのプレイヤーから見て壁があるかで決める）
     */
    public record Rule(Player player, boolean controlled) {}

    /** そのエンティティのルール。決められるプレイヤーがいなければ null（＝ほかの場所へは出られない）。 */
    public static @Nullable Rule ruleFor(Entity entity) {
        Player controller = controller(entity);
        if (controller != null) {
            return new Rule(controller, true);
        }
        Entity actor = entity;
        if (entity instanceof Projectile projectile && projectile.getOwner() != null) {
            actor = projectile.getOwner(); // モブの矢などは撃ったモブと同じ
        }
        if (actor instanceof Mob mob && mob.getTarget() instanceof Player target) {
            return new Rule(target, false);
        }
        Player nearest = actor.level().getNearestPlayer(actor, ModConstants.GATE_MOB_PLAYER_RANGE);
        return nearest == null ? null : new Rule(nearest, false);
    }

    /** そのエンティティを動かしているプレイヤー（本人・乗っている人・飼い主・飛び道具を撃った人）。いなければ null。 */
    public static @Nullable Player controller(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        for (Entity passenger : entity.getPassengers()) {
            if (passenger instanceof Player player) {
                return player;
            }
        }
        if (entity instanceof OwnableEntity ownable && ownable.getOwner() instanceof Player owner) {
            return owner;
        }
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof Player owner) {
            return owner;
        }
        return null;
    }

    /** entity が from（今いる場所）から to へ入れるか。 */
    public static boolean mayEnter(Entity entity, FieldRegion from, FieldRegion to) {
        if (from.sameZone(to)) {
            return true;
        }
        // 街は安全地帯：プレイヤーが動かしていないもの（モブ・アイテムなど）は、壁が消えた街にも入れない
        if (to.isTown() && controller(entity) == null) {
            return false;
        }
        Rule rule = ruleFor(entity);
        if (rule == null) {
            return false;
        }
        if (rule.player().isSpectator()) {
            return rule.controlled();
        }
        PlayerExploration exploration = rule.player().getData(ModAttachments.EXPLORATION);
        return rule.controlled()
                ? GateRules.canEnter(exploration, from, to)
                : !GateRules.hasWall(exploration, from, to);
    }

    /** entity が、今いる場所から座標 (x, z) の場所へ手を出せないか（壊す・置く・叩く・使う）。 */
    public static boolean blocked(Entity entity, double x, double z) {
        FieldMap map = FieldLookup.mapOrNull(entity.level());
        if (map == null) {
            return false;
        }
        FieldRegion from = RegionGrid.at(map, entity.getX(), entity.getZ());
        FieldRegion to = RegionGrid.at(map, x, z);
        return !mayEnter(entity, from, to);
    }

    /** a と b が別の場所（街／フィールド）にいるか。 */
    public static boolean differentZone(Entity a, Entity b) {
        FieldMap map = FieldLookup.mapOrNull(a.level());
        if (map == null) {
            return false;
        }
        return !RegionGrid.at(map, a.getX(), a.getZ()).sameZone(RegionGrid.at(map, b.getX(), b.getZ()));
    }

    /** entity と player の間に、player から見て壁があるか（違う場所にいて、その境目に壁がある）。 */
    public static boolean wallBetween(Entity entity, Player player) {
        if (player.isSpectator()) {
            return false;
        }
        FieldMap map = FieldLookup.mapOrNull(entity.level());
        if (map == null) {
            return false;
        }
        FieldRegion a = RegionGrid.at(map, entity.getX(), entity.getZ());
        FieldRegion b = RegionGrid.at(map, player.getX(), player.getZ());
        return !a.sameZone(b) && GateRules.hasWall(player.getData(ModAttachments.EXPLORATION), a, b);
    }

    /** モブ entity がプレイヤーを狙えないか（壁の向こう、または街の中＝安全地帯にいる）。 */
    public static boolean cannotTarget(Entity entity, Player player) {
        if (wallBetween(entity, player)) {
            return true;
        }
        FieldMap map = FieldLookup.mapOrNull(entity.level());
        if (map == null || controller(entity) != null) {
            return false;
        }
        FieldRegion a = RegionGrid.at(map, entity.getX(), entity.getZ());
        FieldRegion b = RegionGrid.at(map, player.getX(), player.getZ());
        return b.isTown() && !a.sameZone(b);
    }

    /** attacker が victim を傷つけられないか（壁越しの攻撃）。狙われたプレイヤーから見て壁があっても止める。 */
    public static boolean attackBlocked(Entity attacker, LivingEntity victim) {
        if (attacker == victim) {
            return false;
        }
        FieldMap map = FieldLookup.mapOrNull(attacker.level());
        if (map == null) {
            return false;
        }
        FieldRegion from = RegionGrid.at(map, attacker.getX(), attacker.getZ());
        FieldRegion to = RegionGrid.at(map, victim.getX(), victim.getZ());
        if (from.sameZone(to)) {
            return false;
        }
        if (victim instanceof Player player && !player.isSpectator()
                && GateRules.hasWall(player.getData(ModAttachments.EXPLORATION), from, to)) {
            return true;
        }
        return !mayEnter(attacker, from, to);
    }

    // =====================================================================
    // 当たり判定（Entity の衝突計算に混ぜる。EntityCollisionMixin から呼ぶ）
    // =====================================================================

    /** 当たり判定を調べる範囲が広すぎるとき（高速移動など）に調べる最大の柱数（片側）。 */
    private static final int MAX_COLUMNS = 24;

    /**
     * source が box の範囲で動くときにぶつかる「見えない壁」。
     * 入れない場所の柱を、box の上下より少し大きい箱で塞ぐ（ジャンプ・飛行でも越えられない）。
     */
    public static List<VoxelShape> wallShapes(Entity source, Level level, AABB box) {
        FieldMap map = FieldLookup.mapOrNull(level);
        if (map == null) {
            return List.of();
        }
        if (source instanceof Player player && player.isSpectator()) {
            return List.of();
        }
        int x0 = Mth.floor(box.minX);
        int x1 = Mth.floor(box.maxX - 1.0E-7);
        int z0 = Mth.floor(box.minZ);
        int z1 = Mth.floor(box.maxZ - 1.0E-7);
        int cx = Mth.floor(source.getX());
        int cz = Mth.floor(source.getZ());
        x0 = Math.max(x0, cx - MAX_COLUMNS);
        x1 = Math.min(x1, cx + MAX_COLUMNS);
        z0 = Math.max(z0, cz - MAX_COLUMNS);
        z1 = Math.min(z1, cz + MAX_COLUMNS);

        FieldRegion from = RegionGrid.at(map, cx, cz);
        List<VoxelShape> walls = null;
        FieldRegion lastTo = null;
        boolean lastAllowed = true;
        for (int z = z0; z <= z1; z++) {
            for (int x = x0; x <= x1; x++) {
                FieldRegion to = RegionGrid.at(map, x, z);
                if (to == from || to.sameZone(from)) {
                    continue;
                }
                if (!to.equals(lastTo)) {
                    lastTo = to;
                    lastAllowed = mayEnter(source, from, to);
                }
                if (lastAllowed) {
                    continue;
                }
                if (walls == null) {
                    walls = new ArrayList<>();
                }
                walls.add(Shapes.create(x, box.minY - 2.0, z, x + 1, box.maxY + 2.0, z + 1));
            }
        }
        return walls == null ? List.of() : walls;
    }
}
