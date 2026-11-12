package io.github.kurumimnm.kurumimnmod.quest;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.network.OpenGatePayload;
import io.github.kurumimnm.kurumimnmod.registry.ModAttachments;
import io.github.kurumimnm.kurumimnmod.registry.ModWorldgen;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * クエストの進行（サーバー側）。
 * <ul>
 *   <li>受注：受けた場所を覚えて、最初のフィールドへ転移（今は無条件。受付の仕組みは後で作る）</li>
 *   <li>フィールド：初めて入るときに区画を割り当てる（地形はそのとき決まる）。クリア条件を満たすとゲートが開く</li>
 *   <li>ゲート：クリア後に右クリックすると行き先（次のフィールド・中間拠点・達成して帰る）を選ぶ画面を出す</li>
 *   <li>中間拠点：全クエスト共通。出発ゲートから、今入れるフィールドへ行ける</li>
 *   <li>辞退：受けた場所へ戻る。達成：お金をもらって受けた場所へ戻る</li>
 *   <li>モブ：フィールドの設定（mobs）のモブだけを、プレイヤーのまわりにわかせる</li>
 *   <li>決まった数のモブ（placed）：初めて入ったときに区画の中へ置く（わき直さない）。クリア条件 wipe なら全部倒すとクリア</li>
 * </ul>
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class QuestManager {

    private QuestManager() {}

    private static final String MSG = "message." + Kurumimnmod.MOD_ID + ".quest.";

    // =====================================================================
    // 状態の読み書き
    // =====================================================================

    public static PlayerQuest get(ServerPlayer player) {
        return player.getData(ModAttachments.QUEST);
    }

    private static void set(ServerPlayer player, PlayerQuest quest) {
        player.setData(ModAttachments.QUEST, quest);
    }

    public static Optional<Registry<QuestDefinition>> registry(MinecraftServer server) {
        return server.registryAccess().lookup(ModWorldgen.QUEST);
    }

    public static Optional<QuestDefinition> definition(MinecraftServer server, Identifier id) {
        return registry(server).flatMap(r -> r.getOptional(id));
    }

    private static Optional<QuestDefinition> current(ServerPlayer player) {
        return get(player).quest().flatMap(id -> definition(player.level().getServer(), id));
    }

    private static Optional<ServerLevel> questLevel(MinecraftServer server) {
        return Optional.ofNullable(server.getLevel(ModWorldgen.QUEST_LEVEL));
    }

    public static boolean inQuestLevel(Entity entity) {
        return entity.level().dimension().equals(ModWorldgen.QUEST_LEVEL);
    }

    // =====================================================================
    // 受注・辞退・達成
    // =====================================================================

    /** クエストを受けて、最初のフィールドへ転移する。 */
    public static boolean accept(ServerPlayer player, Identifier id) {
        PlayerQuest q = get(player);
        if (q.active()) {
            player.sendSystemMessage(Component.translatable(MSG + "already"));
            return false;
        }
        Optional<QuestDefinition> def = definition(player.level().getServer(), id);
        if (def.isEmpty() || def.get().start().isEmpty()) {
            player.sendSystemMessage(Component.translatable(MSG + "unknown", id.toString()));
            return false;
        }
        // 受けた場所（クエスト用ディメンションの中で受けた場合は、元の世界のスポーン地点にする）
        GlobalPos from = inQuestLevel(player) ? worldSpawn(player.level().getServer()) : GlobalPos.of(player.level().dimension(), player.blockPosition());
        set(player, q.start(id, from));
        player.sendSystemMessage(Component.translatable(MSG + "accepted", Component.translatable(QuestDefinition.nameKey(id))));
        enterField(player, def.get().start().get(0));
        return true;
    }

    /** クエストを辞退して、受けた場所へ戻る。 */
    public static void abandon(ServerPlayer player) {
        PlayerQuest q = get(player);
        if (!q.active()) {
            player.sendSystemMessage(Component.translatable(MSG + "none"));
            return;
        }
        Component name = Component.translatable(QuestDefinition.nameKey(q.quest().get()));
        GlobalPos back = q.origin().orElseGet(() -> worldSpawn(player.level().getServer()));
        set(player, q.finish(false));
        teleportHome(player, back);
        player.sendSystemMessage(Component.translatable(MSG + "abandoned", name));
    }

    /** クエスト達成：報酬を受け取り、受けた場所へ戻る。 */
    private static void complete(ServerPlayer player, QuestDefinition def) {
        PlayerQuest q = get(player);
        Component name = Component.translatable(QuestDefinition.nameKey(q.quest().get()));
        GlobalPos back = q.origin().orElseGet(() -> worldSpawn(player.level().getServer()));
        if (def.rewardMoney() > 0) {
            Money.add(player, def.rewardMoney());
        }
        set(player, q.finish(true));
        teleportHome(player, back);
        player.sendSystemMessage(Component.translatable(MSG + "completed", name, def.rewardMoney()));
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
        // ほかの仕組み（職業の解放など）へ知らせる
        NeoForge.EVENT_BUS.post(new QuestCompletedEvent(player, q.quest().get(), def));
    }

    /**
     * クエストの場所へ移動する（クエスト画面の「フィールドへ」）。
     * まだどこもクリアしていなければ最初のフィールド、それ以外は中間拠点へ。
     */
    public static void travel(ServerPlayer player) {
        PlayerQuest q = get(player);
        Optional<QuestDefinition> def = current(player);
        if (def.isEmpty()) {
            player.sendSystemMessage(Component.translatable(MSG + "none"));
            return;
        }
        if (q.cleared().isEmpty()) {
            enterField(player, def.get().start().get(0));
        } else {
            goHub(player);
        }
    }

    // =====================================================================
    // フィールド・中間拠点への移動
    // =====================================================================

    /** フィールドに入る（初めてなら区画を割り当てる）。入口に転移し、ボス部屋ならボスを出す。 */
    public static void enterField(ServerPlayer player, String fieldId) {
        MinecraftServer server = player.level().getServer();
        PlayerQuest q = get(player);
        Optional<QuestDefinition> def = current(player);
        Optional<ServerLevel> level = questLevel(server);
        if (def.isEmpty() || level.isEmpty()) {
            return;
        }
        Optional<QuestDefinition.Field> field = def.get().field(fieldId);
        if (field.isEmpty()) {
            return;
        }
        Identifier questId = q.quest().get();
        QuestPlotData data = QuestPlotData.get(server);
        QuestPlots.Plot plot = Optional.ofNullable(q.plots().get(fieldId)).flatMap(data::plot)
                .orElseGet(() -> data.allocate(questId, fieldId, field.get(), player.getRandom().nextLong() ^ System.nanoTime()));
        boolean firstTime = !q.plots().containsKey(fieldId);
        q = q.withPlot(fieldId, plot.index()).withLocation(fieldId);
        if (!q.location().equals(get(player).location()) || firstTime) {
            q = q.withKills(0);
        }
        set(player, q);
        // クエスト中の時間：クエストの時刻で止める（QuestEnvironment）
        QuestEnvironment.setQuestTime(server, def.get().time());
        BlockPos in = plot.entrance();
        teleport(player, level.get(), in, 180f);
        player.sendSystemMessage(Component.translatable(MSG + "entered", fieldTitle(questId, field.get(), fieldId)));
        // 決まった数のモブ：初めてなら置く一覧を作る（実際に置くのはチャンクが読み込まれてから。onServerTick）
        if (!field.get().placed().isEmpty() && !q.cleared().contains(fieldId)) {
            startPlacement(player, fieldId, field.get());
        }
        syncPlacedKills(player, fieldId, field.get());
        // ボス部屋：まだクリアしていなくて、ボスがいなければ出す
        if (field.get().clear().type().equals(QuestDefinition.Clear.BOSS) && !q.cleared().contains(fieldId)) {
            spawnBoss(player, level.get(), plot, field.get());
        }
    }

    /** 中間拠点へ移動する。 */
    public static void goHub(ServerPlayer player) {
        Optional<ServerLevel> level = questLevel(player.level().getServer());
        if (level.isEmpty() || !get(player).active()) {
            return;
        }
        set(player, get(player).withLocation(PlayerQuest.HUB));
        teleport(player, level.get(), QuestPlots.hubArrival(), 180f);
        player.sendSystemMessage(Component.translatable(MSG + "hub"));
    }

    private static Component fieldTitle(Identifier questId, QuestDefinition.Field field, String fieldId) {
        return Component.translatable(MSG + "field_title", field.number(),
                Component.translatable(QuestDefinition.fieldKey(questId, fieldId)));
    }

    // =====================================================================
    // ゲート
    // =====================================================================

    /** 行き先の種類（ゲートの画面の選択肢）。 */
    public static final byte OPTION_FIELD = 0;
    public static final byte OPTION_HUB = 1;
    public static final byte OPTION_FINISH = 2;

    /** ゲート（{@link QuestGateBlock}）を右クリックしたとき。 */
    public static void useGate(ServerPlayer player, BlockPos pos) {
        if (!inQuestLevel(player)) {
            return;
        }
        PlayerQuest q = get(player);
        Optional<QuestDefinition> def = current(player);
        if (def.isEmpty()) {
            player.sendSystemMessage(Component.translatable(MSG + "none"));
            return;
        }
        if (QuestPlots.inHub(pos.getX(), pos.getZ())) {
            PacketDistributor.sendToPlayer(player, new OpenGatePayload("", options(def.get(), q, null)));
            return;
        }
        QuestPlots.Plot plot = QuestPlots.at(pos.getX(), pos.getZ());
        if (plot == null || !Integer.valueOf(plot.index()).equals(q.plots().get(plot.field()))) {
            player.sendSystemMessage(Component.translatable(MSG + "not_your_gate"));
            return;
        }
        QuestDefinition.Field field = def.get().field(plot.field()).orElse(null);
        if (field == null) {
            return;
        }
        if (!q.cleared().contains(plot.field())) {
            if (field.clear().type().equals(QuestDefinition.Clear.REACH)) {
                clearField(player, plot.field());
                q = get(player);
            } else {
                player.sendSystemMessage(Component.translatable(MSG + "gate_closed"));
                return;
            }
        }
        PacketDistributor.sendToPlayer(player, new OpenGatePayload(plot.field(), options(def.get(), q, plot.field())));
    }

    /** ゲートの選択肢。from = 今いるフィールド（null = 中間拠点）。 */
    private static List<OpenGatePayload.Option> options(QuestDefinition def, PlayerQuest q, String from) {
        List<OpenGatePayload.Option> list = new ArrayList<>();
        Set<String> cleared = q.cleared();
        if (from != null) {
            QuestDefinition.Field field = def.field(from).orElse(null);
            if (field != null) {
                for (String next : field.next()) {
                    if (!cleared.contains(next)) {
                        list.add(new OpenGatePayload.Option(OPTION_FIELD, next, def.available(next, cleared)));
                    }
                }
            }
        } else {
            for (String id : def.fieldOrder()) {
                if (def.available(id, cleared)) {
                    list.add(new OpenGatePayload.Option(OPTION_FIELD, id, true));
                } else if (!cleared.contains(id) && reachable(def, id, cleared)) {
                    list.add(new OpenGatePayload.Option(OPTION_FIELD, id, false)); // 道はあるが条件が足りない
                }
            }
        }
        if (cleared.contains(def.goal())) {
            list.add(new OpenGatePayload.Option(OPTION_FINISH, "", true));
        }
        if (from != null) {
            list.add(new OpenGatePayload.Option(OPTION_HUB, "", true));
        }
        return list;
    }

    /** 道筋（start または クリアしたフィールドの next）には入っているか。 */
    private static boolean reachable(QuestDefinition def, String id, Set<String> cleared) {
        if (def.start().contains(id)) {
            return true;
        }
        for (String c : cleared) {
            if (def.field(c).map(f -> f.next().contains(id)).orElse(false)) {
                return true;
            }
        }
        return false;
    }

    /** ゲートの画面で行き先を選んだとき（クライアントから）。条件はここで確かめ直す。 */
    public static void chooseGate(ServerPlayer player, byte kind, String fieldId) {
        if (!inQuestLevel(player) || !nearGate(player)) {
            return;
        }
        PlayerQuest q = get(player);
        Optional<QuestDefinition> def = current(player);
        if (def.isEmpty()) {
            return;
        }
        switch (kind) {
            case OPTION_FIELD -> {
                if (def.get().available(fieldId, q.cleared())) {
                    enterField(player, fieldId);
                } else {
                    player.sendSystemMessage(Component.translatable(MSG + "locked"));
                }
            }
            case OPTION_HUB -> {
                if (!q.cleared().isEmpty()) {
                    goHub(player);
                }
            }
            case OPTION_FINISH -> {
                if (q.cleared().contains(def.get().goal())) {
                    complete(player, def.get());
                }
            }
            default -> { }
        }
    }

    /** ゲートの近くにいるか（離れた所から画面を使えないように）。 */
    private static boolean nearGate(ServerPlayer player) {
        double range = ModConstants.QUEST_GATE_USE_RANGE;
        BlockPos gate;
        if (QuestPlots.inHub(player.getX(), player.getZ())) {
            gate = QuestPlots.hubGate();
        } else {
            QuestPlots.Plot plot = QuestPlots.at(player.getX(), player.getZ());
            if (plot == null) {
                return false;
            }
            gate = plot.exit();
        }
        return player.distanceToSqr(gate.getX() + 0.5, gate.getY(), gate.getZ() + 0.5) <= range * range;
    }

    // =====================================================================
    // クリア
    // =====================================================================

    /** フィールドをクリアにする（ゲートが開く）。 */
    public static void clearField(ServerPlayer player, String fieldId) {
        PlayerQuest q = get(player);
        Optional<QuestDefinition> def = current(player);
        if (def.isEmpty() || q.cleared().contains(fieldId)) {
            return;
        }
        set(player, q.withCleared(fieldId));
        boolean goal = def.get().goal().equals(fieldId);
        player.sendSystemMessage(Component.translatable(MSG + (goal ? "goal_cleared" : "field_cleared")));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    @SubscribeEvent
    static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || !level.dimension().equals(ModWorldgen.QUEST_LEVEL)) {
            return;
        }
        // ボス：出した本人のフィールドをクリア（だれが倒しても）
        UUID id = victim.getUUID();
        for (ServerPlayer player : level.players()) {
            PlayerQuest q = get(player);
            if (q.boss().isPresent() && q.boss().get().equals(id)) {
                set(player, q.withBoss(Optional.empty()));
                if (q.inField()) {
                    clearField(player, q.location());
                }
            }
        }
        // 置いたモブ（placed）：置いた本人の数を減らす（だれが・何で倒しても）
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            PlayerQuest pq = get(player);
            for (var entry : pq.placements().entrySet()) {
                if (entry.getValue().alive().contains(id)) {
                    set(player, pq.withPlacement(entry.getKey(), entry.getValue().died(id)));
                    afterPlacementChanged(player, entry.getKey());
                    break;
                }
            }
        }
        // 倒した数（クリア条件 kill）
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || !(victim instanceof Enemy)) {
            return;
        }
        PlayerQuest q = get(killer);
        if (!q.inField() || q.cleared().contains(q.location())) {
            return;
        }
        Optional<QuestDefinition.Field> field = current(killer).flatMap(d -> d.field(q.location()));
        QuestPlots.Plot plot = QuestPlots.at(victim.getX(), victim.getZ());
        if (field.isEmpty() || plot == null || !Integer.valueOf(plot.index()).equals(q.plots().get(q.location()))) {
            return;
        }
        QuestDefinition.Clear clear = field.get().clear();
        if (clear.type().equals(QuestDefinition.Clear.KILL)) {
            int kills = q.kills() + 1;
            set(killer, q.withKills(kills));
            if (kills >= clear.count()) {
                clearField(killer, q.location());
            }
        }
    }

    // =====================================================================
    // モブ・ボス
    // =====================================================================

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        Optional<ServerLevel> level = questLevel(server);
        if (level.isEmpty()) {
            return;
        }
        if (server.getTickCount() % ModConstants.QUEST_PLACE_INTERVAL_TICKS == 0) {
            for (ServerPlayer player : level.get().players()) {
                tickPlacement(player, level.get());
            }
        }
        if (server.getTickCount() % ModConstants.QUEST_SPAWN_INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerPlayer player : level.get().players()) {
            PlayerQuest q = get(player);
            if (!q.inField() || player.isSpectator()) {
                continue;
            }
            QuestPlots.Plot plot = QuestPlots.at(player.getX(), player.getZ());
            Optional<QuestDefinition.Field> field = current(player).flatMap(d -> d.field(q.location()));
            if (plot != null && field.isPresent() && !field.get().mobs().isEmpty()) {
                spawnMobs(player, level.get(), plot, field.get());
            }
        }
    }

    /** プレイヤーのまわりに、フィールドのモブを（上限まで）1 体わかせる。 */
    private static void spawnMobs(ServerPlayer player, ServerLevel level, QuestPlots.Plot plot, QuestDefinition.Field field) {
        double r = ModConstants.QUEST_SPAWN_COUNT_RADIUS;
        AABB box = player.getBoundingBox().inflate(r, 32, r);
        int count = level.getEntitiesOfClass(Mob.class, box, m -> m instanceof Enemy && m.isAlive()).size();
        if (count >= field.maxMobs()) {
            return;
        }
        // 重み付きで種類を選ぶ
        int total = field.mobs().stream().mapToInt(m -> Math.max(0, m.weight())).sum();
        if (total <= 0) {
            return;
        }
        int pick = player.getRandom().nextInt(total);
        QuestDefinition.Mob chosen = field.mobs().get(0);
        for (QuestDefinition.Mob m : field.mobs()) {
            pick -= Math.max(0, m.weight());
            if (pick < 0) {
                chosen = m;
                break;
            }
        }
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(chosen.type());
        if (type.isEmpty()) {
            return;
        }
        // 場所：プレイヤーから少し離れた、区画の中の地面の上（入口のそばは避ける）
        double angle = player.getRandom().nextDouble() * Math.PI * 2;
        double dist = ModConstants.QUEST_SPAWN_MIN_DISTANCE
                + player.getRandom().nextDouble() * (ModConstants.QUEST_SPAWN_MAX_DISTANCE - ModConstants.QUEST_SPAWN_MIN_DISTANCE);
        int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
        int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
        int inset = plot.terrain().rimWidth();
        if (Math.abs(x - plot.centerX()) > plot.half() - inset || Math.abs(z - plot.centerZ()) > plot.half() - inset) {
            return;
        }
        BlockPos in = plot.entrance();
        if (Math.hypot(x - in.getX(), z - in.getZ()) < ModConstants.QUEST_SPAWN_SAFE_RADIUS) {
            return;
        }
        BlockPos pos = new BlockPos(x, 0, z);
        if (!level.isLoaded(pos)) {
            return;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        type.get().spawn(level, new BlockPos(x, y, z), EntitySpawnReason.TRIGGERED);
    }

    // ---- 決まった数のモブ（placed） ----

    /** 置くモブの一覧を作る（そのフィールドで初めてのときだけ）。種類はばらばらの順に混ぜる。 */
    private static void startPlacement(ServerPlayer player, String fieldId, QuestDefinition.Field field) {
        PlayerQuest q = get(player);
        if (q.placement(fieldId).isPresent()) {
            return;
        }
        List<Identifier> list = new ArrayList<>();
        for (QuestDefinition.Placed p : field.placed()) {
            if (BuiltInRegistries.ENTITY_TYPE.getOptional(p.type()).isEmpty()) {
                Kurumimnmod.LOGGER.warn("Quest field '{}': unknown entity type '{}' in placed (skipped)", fieldId, p.type());
                continue;
            }
            for (int i = 0; i < p.count(); i++) {
                list.add(p.type());
            }
        }
        // 混ぜる（同じ種類がかたまって置かれないように）
        for (int i = list.size() - 1; i > 0; i--) {
            int j = player.getRandom().nextInt(i + 1);
            Identifier t = list.get(i);
            list.set(i, list.get(j));
            list.set(j, t);
        }
        set(player, q.withPlacement(fieldId, new PlayerQuest.Placement(List.copyOf(list), List.of())));
        afterPlacementChanged(player, fieldId);
    }

    /** 置くモブの数が変わったあと：HUD の数を直し、wipe なら全部倒したかを確かめる。 */
    private static void afterPlacementChanged(ServerPlayer player, String fieldId) {
        Optional<QuestDefinition.Field> field = current(player).flatMap(d -> d.field(fieldId));
        if (field.isEmpty()) {
            return;
        }
        syncPlacedKills(player, fieldId, field.get());
        PlayerQuest q = get(player);
        boolean done = q.placement(fieldId).map(PlayerQuest.Placement::done).orElse(false);
        if (done && field.get().clear().type().equals(QuestDefinition.Clear.WIPE)) {
            clearField(player, fieldId);
        }
    }

    /** 今いるフィールドが placed を使うなら、HUD 用の「倒した数」を「合計 − 残り」にする。 */
    private static void syncPlacedKills(ServerPlayer player, String fieldId, QuestDefinition.Field field) {
        PlayerQuest q = get(player);
        if (!fieldId.equals(q.location()) || field.placed().isEmpty()) {
            return;
        }
        int remaining = q.placement(fieldId).map(PlayerQuest.Placement::remaining).orElse(field.placedTotal());
        int kills = Math.max(0, field.placedTotal() - remaining);
        if (kills != q.kills()) {
            set(player, q.withKills(kills));
        }
    }

    /** まだ置いていないモブを、読み込まれたチャンクに置く（1 回に {@link ModConstants#QUEST_PLACE_PER_TICK} 体まで）。 */
    private static void tickPlacement(ServerPlayer player, ServerLevel level) {
        PlayerQuest q = get(player);
        if (!q.inField() || player.isSpectator()) {
            return;
        }
        String fieldId = q.location();
        Optional<PlayerQuest.Placement> placement = q.placement(fieldId);
        if (placement.isEmpty() || placement.get().pending().isEmpty()) {
            return;
        }
        Optional<QuestPlots.Plot> plot = Optional.ofNullable(q.plots().get(fieldId))
                .flatMap(QuestPlotData.get(level.getServer())::plot);
        if (plot.isEmpty()) {
            return;
        }
        PlayerQuest.Placement state = placement.get();
        int placed = 0;
        int index = 0;
        while (index < state.pending().size() && placed < ModConstants.QUEST_PLACE_PER_TICK) {
            Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(state.pending().get(index));
            Optional<BlockPos> pos = type.isPresent() ? placePosition(player, level, plot.get()) : Optional.empty();
            Entity e = pos.isPresent() ? type.get().spawn(level, pos.get(), EntitySpawnReason.TRIGGERED) : null;
            if (e == null) {
                index++; // 今回は置けなかった（次の回にもう一度）
                continue;
            }
            if (e instanceof Mob mob) {
                mob.setPersistenceRequired(); // 離れても消えない（倒すまで残る）
            }
            state = state.placed(index, e.getUUID());
            placed++;
        }
        if (placed > 0) {
            set(player, get(player).withPlacement(fieldId, state));
            afterPlacementChanged(player, fieldId);
        }
    }

    /**
     * 置く場所を探す：区画の中（縁の崖・ボス部屋の壁・柱から離れた所）で、入口から離れていて、チャンクが読み込まれている地面の上。
     * 見つからなければ empty（次の回にもう一度）。
     */
    private static Optional<BlockPos> placePosition(ServerPlayer player, ServerLevel level, QuestPlots.Plot plot) {
        int margin = ModConstants.QUEST_PLACE_EDGE_MARGIN;
        BlockPos in = plot.entrance();
        for (int attempt = 0; attempt < ModConstants.QUEST_PLACE_ATTEMPTS; attempt++) {
            double u;
            double v;
            if (plot.isTemple()) {
                double rx = Math.max(1, plot.terrain().hallWidth() / 2.0 - margin);
                double rz = Math.max(1, plot.terrain().hallLength() / 2.0 - margin);
                u = (player.getRandom().nextDouble() * 2 - 1) * rx;
                v = (player.getRandom().nextDouble() * 2 - 1) * rz;
            } else if (plot.isArena()) {
                double r = Math.max(1, plot.arenaRadius() - margin) * Math.sqrt(player.getRandom().nextDouble());
                double a = player.getRandom().nextDouble() * Math.PI * 2;
                u = Math.cos(a) * r;
                v = Math.sin(a) * r;
            } else {
                double reach = Math.max(1, plot.half() - plot.terrain().rimWidth() - margin);
                u = (player.getRandom().nextDouble() * 2 - 1) * reach;
                v = (player.getRandom().nextDouble() * 2 - 1) * reach;
            }
            int x = (int) Math.floor(plot.centerX() + u);
            int z = (int) Math.floor(plot.centerZ() + v);
            if (Math.hypot(x - in.getX(), z - in.getZ()) < ModConstants.QUEST_PLACE_SAFE_RADIUS
                    || plot.nearPillar(x, z, margin)) {
                continue;
            }
            BlockPos column = new BlockPos(x, 0, z);
            if (!level.isLoaded(column)) {
                continue;
            }
            // 神殿は天井があるので、高さの地図ではなく床の上に置く
            int y = plot.isArena() ? plot.terrain().base() + 1 : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            return Optional.of(new BlockPos(x, y, z));
        }
        return Optional.empty();
    }

    /** ボス部屋のボスを出す（まだいなければ）。 */
    private static void spawnBoss(ServerPlayer player, ServerLevel level, QuestPlots.Plot plot, QuestDefinition.Field field) {
        PlayerQuest q = get(player);
        if (q.boss().isPresent() && level.getEntity(q.boss().get()) != null) {
            return;
        }
        Optional<EntityType<?>> type = field.boss().flatMap(BuiltInRegistries.ENTITY_TYPE::getOptional);
        if (type.isEmpty()) {
            return;
        }
        BlockPos pos = new BlockPos(plot.centerX(), plot.terrain().base() + 1, plot.centerZ());
        level.getChunk(pos);
        Entity boss = type.get().spawn(level, pos, EntitySpawnReason.TRIGGERED);
        if (boss instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        if (boss != null) {
            set(player, q.withBoss(Optional.of(boss.getUUID())));
        }
    }

    // =====================================================================
    // 転移
    // =====================================================================

    private static void teleport(ServerPlayer player, ServerLevel level, BlockPos pos, float yaw) {
        level.getChunk(pos); // 先に作っておく
        player.teleportTo(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), yaw, 0f, true);
    }

    /** 元の世界（受けた場所）へ戻る。 */
    private static void teleportHome(ServerPlayer player, GlobalPos to) {
        MinecraftServer server = player.level().getServer();
        ServerLevel level = server.getLevel(to.dimension());
        if (level == null) {
            to = worldSpawn(server);
            level = server.overworld();
        }
        teleport(player, level, to.pos(), player.getYRot());
    }

    /** ワールドのスポーン地点。 */
    private static GlobalPos worldSpawn(MinecraftServer server) {
        LevelData.RespawnData data = server.getRespawnData();
        return data.globalPos();
    }

    // =====================================================================
    // サーバーの起動・停止・ログイン
    // =====================================================================

    /** 区画の一覧を読み込む（オーバーワールドを読み込んだとき＝ほかのディメンションの生成より前）。 */
    @SubscribeEvent
    static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            level.getDataStorage().computeIfAbsent(QuestPlotData.TYPE).publish();
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        QuestPlots.clear();
    }

    /** クエストを受けていないのにクエスト用ディメンションにいたら、スポーン地点へ戻す。 */
    @SubscribeEvent
    static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && inQuestLevel(player) && !get(player).active()) {
            teleportHome(player, worldSpawn(player.level().getServer()));
        }
    }

    /** 死んで元の世界で生き返ったら、場所を「クエストの外」にする（クエストは続く。クエスト画面から戻れる）。 */
    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !inQuestLevel(player) && get(player).active()) {
            set(player, get(player).withLocation(PlayerQuest.NOWHERE));
        }
    }
}
