package io.github.kurumimnm.kurumimnmod.world;

import io.github.kurumimnm.kurumimnmod.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
 *   <li><b>プレイヤーが乗っている乗り物・プレイヤーのペット</b>：そのプレイヤーと同じルール</li>
 *   <li><b>それ以外（モブ・アイテム・飛び道具など）</b>：自分のいる「場所」（街／フィールド）から出られない。
 *       同じフィールドのエリア同士は自由に動ける</li>
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
     * 本人がプレイヤーならその人、乗っているプレイヤーがいればその人、飼い主がプレイヤーならその人。いなければ null。
     */
    public static @Nullable Player governingPlayer(Entity entity) {
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
        Player player = governingPlayer(entity);
        if (player == null) {
            return false; // モブ・アイテムなどは自分の場所から出られない
        }
        if (player.isSpectator()) {
            return true;
        }
        return GateRules.canEnter(player.getData(ModAttachments.EXPLORATION), from, to);
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

    /** attacker が victim を傷つけられないか（壁越しの攻撃）。 */
    public static boolean attackBlocked(Entity attacker, LivingEntity victim) {
        return attacker != victim && blocked(attacker, victim.getX(), victim.getZ());
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
