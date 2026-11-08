package io.github.kurumimnm.kurumimnmod.world;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.registry.ModWorldgen;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * このMODのワールド（現世の始まりの街のまわり・クエスト用ディメンション）では、指定したモブ以外は勝手にわかない。
 * <ul>
 *   <li>対象：クエスト用ディメンション全体と、バイオームタグ {@code kurumimnmod:controlled_spawns} のバイオーム</li>
 *   <li>わいてよいモブ：エンティティタグ {@code kurumimnmod:allowed_natural_spawns}（今は空）。
 *       クエストのフィールドのモブは {@code QuestManager} が TRIGGERED で出すので止めない</li>
 *   <li>止める理由：自然スポーン・地形生成時の動物・巡回隊・行商人や襲撃・ゾンビの増援</li>
 * </ul>
 * スポーンエッグ・コマンド・スポナー・繁殖など、プレイヤーや MOD が意図して出すものは止めない。
 * バイオームのスポーン表も空にしてある（データ JSON）。ふつうのワールド（バニラの地形）には何もしない。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class ModSpawnRules {

    private ModSpawnRules() {}

    /** このMODのバイオーム（スポーンを管理する所）。 */
    public static final TagKey<Biome> CONTROLLED = TagKey.create(Registries.BIOME,
            Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "controlled_spawns"));

    /** このMODのワールドでも自然にわいてよいモブ。 */
    public static final TagKey<EntityType<?>> ALLOWED = TagKey.create(Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "allowed_natural_spawns"));

    /** 止めるスポーンの理由。 */
    private static final Set<EntitySpawnReason> BLOCKED = EnumSet.of(
            EntitySpawnReason.NATURAL,          // 自然スポーン・ファントム
            EntitySpawnReason.CHUNK_GENERATION, // 地形生成時の動物
            EntitySpawnReason.PATROL,           // ピリジャーの巡回隊
            EntitySpawnReason.EVENT,            // 行商人・襲撃など
            EntitySpawnReason.REINFORCEMENT);   // ゾンビの増援

    @SubscribeEvent
    static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!BLOCKED.contains(event.getSpawnType()) || event.getEntity().typeHolder().is(ALLOWED)
                || !(event.getLevel().getLevel() instanceof ServerLevel level)) {
            return;
        }
        boolean controlled = level.dimension().equals(ModWorldgen.QUEST_LEVEL)
                || level.getBiome(BlockPos.containing(event.getX(), event.getY(), event.getZ())).is(CONTROLLED);
        if (controlled) {
            event.setSpawnCancelled(true);
        }
    }
}
