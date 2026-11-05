package io.github.kurumimnm.kurumimnmod.client.world;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.network.FieldWorldPayload;
import io.github.kurumimnm.kurumimnmod.registry.ModAttachments;
import io.github.kurumimnm.kurumimnmod.registry.ModWorldgen;
import io.github.kurumimnm.kurumimnmod.world.FieldMap;
import io.github.kurumimnm.kurumimnmod.world.FieldRegion;
import io.github.kurumimnm.kurumimnmod.world.GateRules;
import io.github.kurumimnm.kurumimnmod.world.PlayerExploration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * クライアント側の「フィールドマップのワールド」の状態（クライアント専用）。
 * <ul>
 *   <li>今いるディメンションの地図（サーバーから名前が届き、中身は同期済みのレジストリから取り出す）</li>
 *   <li>今いる場所。変わったら場所の名前を表示する（{@link LocationHud}）</li>
 *   <li>ゲートの壁の手前で止める（サーバーでも同じ判定をして押し戻すが、カクつかないようにこちらでも止める）</li>
 * </ul>
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID, value = Dist.CLIENT)
public final class ClientFieldWorld {

    private ClientFieldWorld() {}

    private static @Nullable Identifier mapId;
    private static @Nullable FieldMap map;
    private static @Nullable FieldRegion region;
    private static @Nullable Vec3 lastPos;

    @SubscribeEvent
    static void onRegisterClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(FieldWorldPayload.TYPE, (payload, context) -> {
            mapId = payload.map().orElse(null);
            map = null;
            region = null;
            lastPos = null;
            LocationHud.reset();
        });
    }

    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        mapId = null;
        map = null;
        region = null;
        lastPos = null;
        LocationHud.reset();
        WorldMapImage.clear();
    }

    /** 今いるディメンションの地図（フィールドマップのワールドでなければ null）。 */
    public static @Nullable FieldMap map() {
        if (map == null && mapId != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                map = mc.level.registryAccess().lookup(ModWorldgen.FIELD_MAP)
                        .flatMap(registry -> registry.getOptional(mapId))
                        .orElse(null);
            }
        }
        return map;
    }

    /** 今いる場所（不明なら null）。 */
    public static @Nullable FieldRegion region() {
        return region;
    }

    public static PlayerExploration exploration(LocalPlayer player) {
        return player.getData(ModAttachments.EXPLORATION);
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        FieldMap fieldMap = map();
        if (fieldMap == null) {
            return;
        }
        Vec3 pos = player.position();
        FieldRegion now = fieldMap.regionAt(pos.x, pos.z);
        if (region == null || lastPos == null) {
            region = now;
            lastPos = pos;
            LocationHud.onEnter(fieldMap, null, now, !exploration(player).visited(now));
            return;
        }
        if (now.equals(region)) {
            lastPos = pos;
            return;
        }
        PlayerExploration exploration = exploration(player);
        boolean teleported = pos.distanceToSqr(lastPos) > 64;
        boolean allowed = teleported || player.isCreative() || player.isSpectator()
                || GateRules.canEnter(exploration, region, now);
        if (!allowed) {
            // 壁の手前で止める
            player.setPos(lastPos.x, lastPos.y, lastPos.z);
            player.setDeltaMovement(Vec3.ZERO);
            return;
        }
        FieldRegion previous = region;
        region = now;
        lastPos = pos;
        LocationHud.onEnter(fieldMap, previous, now, !exploration.visited(now));
    }
}
