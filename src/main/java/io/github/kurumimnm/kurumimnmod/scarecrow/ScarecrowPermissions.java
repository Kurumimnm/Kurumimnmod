package io.github.kurumimnm.kurumimnmod.scarecrow;

import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.entity.ScarecrowEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;

/**
 * カカシの操作権限・操作対象のチェック（サーバー側）。
 * <ul>
 *   <li>召喚・削除 … OP（権限レベル 2 = ゲームマスター以上）のみ</li>
 *   <li>設定の閲覧・変更 … 誰でも可</li>
 * </ul>
 */
public final class ScarecrowPermissions {

    private ScarecrowPermissions() {}

    /** 召喚・削除ができるか（OP 権限レベル 2 以上。シングルプレイではチート ON のとき）。 */
    public static boolean canManage(ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /**
     * クライアントから送られてきたエンティティ ID のカカシを取得する。
     * 存在しない・カカシではない・遠すぎる（{@link ModConstants#SCARECROW_INTERACT_DISTANCE} より先）場合は null。
     */
    public static ScarecrowEntity findTarget(ServerPlayer player, int entityId) {
        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof ScarecrowEntity scarecrow) || !scarecrow.isAlive()) {
            return null;
        }
        double max = ModConstants.SCARECROW_INTERACT_DISTANCE;
        return player.distanceToSqr(scarecrow) <= max * max ? scarecrow : null;
    }
}
