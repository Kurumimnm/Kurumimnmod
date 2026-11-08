package io.github.kurumimnm.kurumimnmod.quest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * クエストのゲート（フィールドの出口・中間拠点の出発ゲート）。壊せない・光る。
 * 右クリックすると、フィールドをクリアしていれば行き先を選ぶ画面が開く（{@link QuestManager#useGate}）。
 */
public class QuestGateBlock extends Block {

    public QuestGateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer) {
            QuestManager.useGate(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
