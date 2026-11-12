package io.github.kurumimnm.kurumimnmod.quest;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.registry.ModWorldgen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * クエスト用ディメンションの中だけのモブのルール（バニラのモブをクエストで使うため）。
 * <ul>
 *   <li><b>地形を壊さない</b>：クリーパーの爆発・エンダーマンのブロック運びなど、モブによる破壊（mobGriefing）を止める。
 *       爆発は念のため、壊れるブロックの一覧も空にする（爆発のダメージ・吹き飛ばしはそのまま）</li>
 *   <li><b>エンダーマンは区画の外へ出ない</b>：ワープ先がいる区画の「戦える場所」（縁の崖・壁の外を除く）の外なら取り消す</li>
 *   <li><b>クモは明るくても襲ってくる</b>：クエストは昼で止めることが多いので、ふつうの目標・近接攻撃を足す
 *       （バニラのクモは明るいと攻撃をやめる）</li>
 *   <li>アンデッドが日光で燃えないのは、クエスト用のタイムライン（{@link QuestEnvironment}）の設定</li>
 * </ul>
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class QuestMobRules {

    private QuestMobRules() {}

    private static boolean inQuest(Level level) {
        return level.dimension().equals(ModWorldgen.QUEST_LEVEL);
    }

    // ---- 地形を壊さない ----
    @SubscribeEvent
    static void onMobGriefing(EntityMobGriefingEvent event) {
        Entity entity = event.getEntity();
        if (entity != null && inQuest(entity.level())) {
            event.setCanGrief(false);
        }
    }

    @SubscribeEvent
    static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (inQuest(event.getLevel())) {
            event.getAffectedBlocks().clear();
        }
    }

    // ---- エンダーマンは区画の外へ出ない ----
    @SubscribeEvent
    static void onEnderTeleport(EntityTeleportEvent.EnderEntity event) {
        Entity entity = event.getEntityLiving();
        if (!inQuest(entity.level())) {
            return;
        }
        QuestPlots.Plot plot = QuestPlots.at(event.getPrevX(), event.getPrevZ());
        if (plot == null || !plot.playable(event.getTargetX(), event.getTargetZ(), ModConstants.QUEST_ENDER_TELEPORT_MARGIN)) {
            event.setCanceled(true);
        }
    }

    // ---- クモは明るくても襲ってくる ----
    @SubscribeEvent
    static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !inQuest(event.getLevel())) {
            return;
        }
        if (event.getEntity() instanceof Spider spider) {
            // 読み込み直すたびに新しいモブとして作られるので、入るたびに足す
            spider.targetSelector.addGoal(ModConstants.QUEST_SPIDER_TARGET_PRIORITY,
                    new NearestAttackableTargetGoal<>(spider, Player.class, true));
            spider.goalSelector.addGoal(ModConstants.QUEST_SPIDER_ATTACK_PRIORITY,
                    new MeleeAttackGoal(spider, ModConstants.QUEST_SPIDER_CHASE_SPEED_MODIFIER, true));
        }
    }
}
