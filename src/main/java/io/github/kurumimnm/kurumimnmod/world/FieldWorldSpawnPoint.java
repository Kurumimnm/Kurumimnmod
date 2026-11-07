package io.github.kurumimnm.kurumimnmod.world;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * 新しいワールドの初期スポーン地点を「始まりの街」（地図の番号 1 の街。アストラリアではファストリア村）にする。
 * バニラの探し方だと (0, 0) 付近（旧都）になってしまうため。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class FieldWorldSpawnPoint {

    private FieldWorldSpawnPoint() {}

    /** 始まりの街の番号。 */
    public static final int START_TOWN_NUMBER = 1;

    @SubscribeEvent
    static void onCreateSpawnPosition(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Optional<FieldMap> map = FieldLookup.map(level);
        if (map.isEmpty()) {
            return;
        }
        Optional<FieldMap.Town> start = map.get().town(START_TOWN_NUMBER);
        if (start.isEmpty()) {
            return;
        }
        FieldMap.Town town = start.get();
        int y = (int) Math.ceil(map.get().terrainHeight(town.x() + 0.5, town.z() + 0.5)) + 1;
        level.setRespawnData(LevelData.RespawnData.of(level.dimension(), new BlockPos(town.x(), y, town.z()), 0, 0));
        event.setCanceled(true);
    }
}
