package io.github.kurumimnm.kurumimnmod.magic;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 魔法の「狙った場所」を求める共通処理（サーバー・クライアント共通）。
 * <p>
 * クライアントの照準サークル表示と、サーバーの発射処理で同じ計算を使うので、
 * 「サークルが出ている場所」と「実際に飛んでいく場所」が一致する。
 */
public final class MagicTargeting {

    private MagicTargeting() {}

    /**
     * 狙った場所。
     *
     * @param position 着弾予定地点
     * @param face     当たったブロックの面（何もない方向を狙ったときは null）
     */
    public record Target(Vec3 position, @Nullable Direction face) {
    }

    /**
     * 視線の先で最初に当たるブロックの位置を求める。何にも当たらなければ最大距離の地点。
     * 液体は通り抜ける（水面ではなく水底を狙う）。
     */
    public static Target find(Player player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(range));
        BlockHitResult hit = player.level().clip(
                new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return new Target(end, null);
        }
        return new Target(hit.getLocation(), hit.getDirection());
    }

    /**
     * 頭上の発射地点。天井が低くて頭上が埋まっている場合は目の位置から撃つ。
     *
     * @param height 頭のてっぺんからの高さ（ブロック）
     */
    public static Vec3 overheadOrigin(Player player, double height) {
        Vec3 eye = player.getEyePosition();
        Vec3 origin = player.position().add(0, player.getBbHeight() + height, 0);
        BlockHitResult hit = player.level().clip(
                new ClipContext(eye, origin, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? origin : eye;
    }
}
