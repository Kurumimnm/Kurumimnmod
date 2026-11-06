package io.github.kurumimnm.kurumimnmod.world;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * このMODのワールドでは、バニラの生き物が勝手にわかないようにする。
 * <p>
 * バイオームのスポーン表（gameplay/natural_mob_spawns）は空にしてあるが、それとは別の仕組みでわくもの
 * （ファントム・ピリジャーの巡回隊・行商人・ゾンビの増援など）もここで止める。
 * スポーンエッグ・コマンド・スポナー・繁殖など、プレイヤーや MOD が意図して出すものは止めない。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class FieldWorldSpawns {

    private FieldWorldSpawns() {}

    /** 止めるスポーンの理由。 */
    private static final Set<EntitySpawnReason> BLOCKED = EnumSet.of(
            EntitySpawnReason.NATURAL,          // 自然スポーン・ファントム
            EntitySpawnReason.CHUNK_GENERATION, // 地形生成時の動物
            EntitySpawnReason.PATROL,           // ピリジャーの巡回隊
            EntitySpawnReason.EVENT,            // 行商人・襲撃など
            EntitySpawnReason.REINFORCEMENT);   // ゾンビの増援

    @SubscribeEvent
    static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (BLOCKED.contains(event.getSpawnType())
                && event.getLevel().getLevel() instanceof ServerLevel level
                && FieldLookup.map(level).isPresent()) {
            event.setSpawnCancelled(true);
        }
    }
}
