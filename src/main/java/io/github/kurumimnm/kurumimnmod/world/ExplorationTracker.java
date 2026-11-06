package io.github.kurumimnm.kurumimnmod.world;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.network.FieldWorldPayload;
import io.github.kurumimnm.kurumimnmod.registry.ModAttachments;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * サーバー側で、プレイヤーがいる「場所」を毎 tick 調べる。
 * <ul>
 *   <li>行ったことのある街・フィールド・エリアを記録する（{@link PlayerExploration}）</li>
 *   <li>ゲートを通れない場所へ入ろうとしたら、直前の位置へ押し戻す（{@link GateRules}）</li>
 *   <li>フィールドボスの実績（kurumimnmod:field_boss/〇〇）を取ったら、そのフィールドを「ボス撃破済み」にする</li>
 * </ul>
 * 普段は当たり判定（{@link Borders}）で壁の手前で止まるので、ここは「ワープなどで当たり判定を通らずに入った」ときの保険。
 * OP・クリエイティブも歩いては通れない。通れるのはスペクテイターと、OP のテレポート（コマンド・地図から）だけ。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class ExplorationTracker {

    private ExplorationTracker() {}

    /** フィールドボスの実績の置き場所（data/kurumimnmod/advancement/field_boss/フィールドID.json）。 */
    public static final String BOSS_ADVANCEMENT_PREFIX = "field_boss/";

    /** プレイヤーごとの「直前にいた場所と位置」（保存しない一時データ）。 */
    private record Track(FieldRegion region, Vec3 pos) {}

    private static final Map<UUID, Track> TRACKS = new HashMap<>();

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Optional<FieldMap> map = FieldLookup.map(player.level());
        if (map.isEmpty()) {
            TRACKS.remove(player.getUUID());
            return;
        }
        Vec3 pos = player.position();
        FieldRegion region = RegionGrid.at(map.get(), pos.x, pos.z);
        Track last = TRACKS.get(player.getUUID());
        if (last == null) {
            TRACKS.put(player.getUUID(), new Track(region, pos));
            visit(player, region);
            return;
        }
        if (region.equals(last.region())) {
            TRACKS.put(player.getUUID(), new Track(region, pos));
            return;
        }

        // 場所が変わった：入れるかどうか
        PlayerExploration exploration = player.getData(ModAttachments.EXPLORATION);
        boolean teleported = pos.distanceToSqr(last.pos()) > ModConstants.GATE_TELEPORT_DISTANCE * ModConstants.GATE_TELEPORT_DISTANCE;
        boolean allowed = Borders.mayEnter(player, last.region(), region)
                || (teleported && isOperator(player));
        if (!allowed) {
            // 壁の手前へ押し戻す
            if (player.isPassenger()) {
                player.stopRiding();
            }
            player.teleportTo(last.pos().x, last.pos().y, last.pos().z);
            player.setDeltaMovement(Vec3.ZERO);
            player.sendOverlayMessage(Component.translatable("message." + Kurumimnmod.MOD_ID + ".gate.blocked"));
            return;
        }
        TRACKS.put(player.getUUID(), new Track(region, pos));
        visit(player, region);
    }

    private static void visit(ServerPlayer player, FieldRegion region) {
        PlayerExploration before = player.getData(ModAttachments.EXPLORATION);
        PlayerExploration after = before.visit(region);
        if (after != before) {
            player.setData(ModAttachments.EXPLORATION, after);
        }
    }

    private static boolean isOperator(Player player) {
        return Commands.LEVEL_GAMEMASTERS.check(player.permissions());
    }

    // =====================================================================
    // ログイン・ディメンション移動・リスポーン
    // =====================================================================

    @SubscribeEvent
    static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TRACKS.remove(player.getUUID());
            syncClearedFromAdvancements(player);
            sendFieldWorld(player);
        }
    }

    @SubscribeEvent
    static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        TRACKS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TRACKS.remove(player.getUUID());
            sendFieldWorld(player);
        }
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TRACKS.remove(player.getUUID()); // リスポーン地点へのワープは壁の判定をしない
            sendFieldWorld(player);
        }
    }

    /** そのプレイヤーの判定をやり直す（コマンドでテレポート・記録を変えたとき用）。 */
    public static void resetTrack(ServerPlayer player) {
        TRACKS.remove(player.getUUID());
    }

    private static void sendFieldWorld(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new FieldWorldPayload(FieldLookup.mapId(player.level())));
    }

    // =====================================================================
    // フィールドボスの実績
    // =====================================================================

    @SubscribeEvent
    static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getAdvancement().id().getNamespace().equals(Kurumimnmod.MOD_ID)
                && event.getAdvancement().id().getPath().startsWith(BOSS_ADVANCEMENT_PREFIX)) {
            syncClearedFromAdvancements(player);
        }
    }

    /** 「ボス撃破済み」のフィールドを、実績の状態に合わせて作り直す。 */
    public static void syncClearedFromAdvancements(ServerPlayer player) {
        Optional<FieldMap> map = FieldLookup.map(player.level());
        if (map.isEmpty()) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        Set<Integer> cleared = new HashSet<>();
        for (FieldMap.Field field : map.get().fields()) {
            AdvancementHolder holder = server.getAdvancements().get(bossAdvancementId(field));
            if (holder != null && player.getAdvancements().getOrStartProgress(holder).isDone()) {
                cleared.add(field.number());
            }
        }
        PlayerExploration exploration = player.getData(ModAttachments.EXPLORATION);
        if (!exploration.clearedFields().equals(cleared)) {
            player.setData(ModAttachments.EXPLORATION, exploration.withCleared(cleared));
        }
    }

    public static Identifier bossAdvancementId(FieldMap.Field field) {
        return Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, BOSS_ADVANCEMENT_PREFIX + field.id());
    }
}
