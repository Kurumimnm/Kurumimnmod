package io.github.kurumimnm.kurumimnmod.world;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.storage.LevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * 「Kurumimnmod ワールド」（平らな現世）の初期スポーン地点を、始まりの街の広場（噴水の南）にする。
 * 平らな地形のバイオームが {@code kurumimnmod:home_plains} のときだけ（ふつうのワールドには何もしない）。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class StartTownSpawn {

    private StartTownSpawn() {}

    @SubscribeEvent
    static void onCreateSpawnPosition(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(level.getChunkSource().getGenerator() instanceof FlatLevelSource flat)
                || !flat.settings().getBiome().is(ModSpawnRules.CONTROLLED)) {
            return;
        }
        BlockPos pos = new BlockPos(ModConstants.START_TOWN_SPAWN_X, ModConstants.START_TOWN_GROUND_Y + 1,
                ModConstants.START_TOWN_SPAWN_Z);
        level.setRespawnData(LevelData.RespawnData.of(level.dimension(), pos, 180f, 0f));
        event.setCanceled(true);
    }
}
