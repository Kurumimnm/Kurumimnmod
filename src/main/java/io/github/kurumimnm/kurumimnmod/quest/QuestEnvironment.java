package io.github.kurumimnm.kurumimnmod.quest;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.clock.WorldClock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 時間と天気の固定。
 * <ul>
 *   <li><b>クエスト中の時間</b>：クエスト用ディメンションは専用の時計 {@code kurumimnmod:quest}（data/kurumimnmod/world_clock/quest.json）
 *       を使う（dimension_type/quest.json の default_clock）。現世の時計（minecraft:overworld）とは別なので、現世の昼夜は今まで通り進む。
 *       この時計は止めておき、フィールドに入ったときにクエストの JSON の time の時刻に合わせる（{@link #setQuestTime}）。
 *       空の色・明るさなどは専用のタイムライン kurumimnmod:quest_day / quest_moon（バニラの day / moon の写し）で決まる。
 *       quest_day には「アンデッドが日光で燃える」（gameplay/monsters_burn）を入れていないので、クエスト中は昼でも燃えない</li>
 *   <li><b>天気</b>：天気はサーバー全体で 1 つ。ずっと晴れにする（Config world.fixWeatherClear）</li>
 * </ul>
 * ※ 時計はクエスト用ディメンション全体で 1 つなので、マルチプレイで別々の時間のクエストを同時に受けると、
 *    最後にフィールドに入った人のクエストの時間になる。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class QuestEnvironment {

    private QuestEnvironment() {}

    /** クエスト用ディメンションの時計。 */
    public static final ResourceKey<WorldClock> QUEST_CLOCK =
            ResourceKey.create(Registries.WORLD_CLOCK, Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "quest"));

    private static Optional<? extends Holder<WorldClock>> questClock(MinecraftServer server) {
        return server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).get(QUEST_CLOCK);
    }

    /** クエストの時計をその時刻にして止める（Config freezeQuestTime が false なら時刻だけ合わせて進める）。 */
    public static void setQuestTime(MinecraftServer server, int time) {
        questClock(server).ifPresentOrElse(clock -> {
            server.clockManager().setTotalTicks(clock, time);
            server.clockManager().setPaused(clock, ModConfigs.freezeQuestTime());
        }, () -> Kurumimnmod.LOGGER.warn("World clock {} is missing", QUEST_CLOCK.identifier()));
    }

    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        questClock(server).ifPresent(clock -> server.clockManager().setPaused(clock, ModConfigs.freezeQuestTime()));
        keepClear(server);
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % ModConstants.WORLD_ENVIRONMENT_CHECK_TICKS == 0) {
            keepClear(server);
        }
    }

    /** 晴れにして、晴れの残り時間を戻す（残りが 0 になると雨が始まるので、定期的に戻し続ける）。 */
    private static void keepClear(MinecraftServer server) {
        if (ModConfigs.fixWeatherClear()) {
            server.setWeatherParameters(ModConstants.WORLD_CLEAR_WEATHER_TICKS, 0, false, false);
        }
    }
}
