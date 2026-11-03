package io.github.kurumimnm.kurumimnmod.item;

import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.entity.ScarecrowEntity;
import io.github.kurumimnm.kurumimnmod.scarecrow.ScarecrowPermissions;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * カカシ召喚用ステッキ。ブロックを右クリックすると、その面の上（横）にカカシを召喚する。
 * <ul>
 *   <li>OP（権限レベル 2 以上）のみ使用可能。それ以外のプレイヤーにはメッセージを出して何もしない</li>
 *   <li>カカシはプレイヤーの方を向く（22.5°刻み）</li>
 *   <li>置く場所がブロックと重なる場合は召喚しない</li>
 *   <li>使ってもなくならない（何体でも置ける）</li>
 * </ul>
 */
public class ScarecrowSummonStickItem extends Item {

    public ScarecrowSummonStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player)) {
            // クライアント側：判定はサーバーに任せ、腕だけ振る
            return InteractionResult.SUCCESS;
        }
        if (!ScarecrowPermissions.canManage(player)) {
            player.sendOverlayMessage(Component.translatable("message.kurumimnmod.scarecrow.op_only")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        // クリックした面の隣のブロックの、足元中央に置く
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Vec3 spawnPos = Vec3.atBottomCenterOf(pos);
        // プレイヤーの方を向かせる（アーマースタンドと同じく一定の角度で丸める）
        float step = ModConstants.SCARECROW_YAW_STEP;
        float yaw = Mth.wrapDegrees(Math.round((player.getYRot() + 180f) / step) * step);

        ScarecrowEntity scarecrow = ScarecrowEntity.summon(level, spawnPos, yaw);
        if (scarecrow == null) {
            player.sendOverlayMessage(Component.translatable("message.kurumimnmod.scarecrow.no_space")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        level.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z, SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 1f, 0.8f);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, spawnPos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.kurumimnmod.scarecrow_summon_stick.desc").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.kurumimnmod.scarecrow_summon_stick.op_only").withStyle(ChatFormatting.DARK_GRAY));
    }
}
