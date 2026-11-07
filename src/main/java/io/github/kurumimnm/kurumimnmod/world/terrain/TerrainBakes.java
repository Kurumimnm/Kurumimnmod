package io.github.kurumimnm.kurumimnmod.world.terrain;

import com.mojang.logging.LogUtils;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.registry.ModWorldgen;
import io.github.kurumimnm.kurumimnmod.world.FieldMap;
import io.github.kurumimnm.kurumimnmod.world.RoadSettings;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

/**
 * ワールドを開くときに地形 v2 を用意する。
 * <p>
 * サーバーが地形を作り始める前（{@link ServerAboutToStartEvent}）に、設計図（地図 JSON の design）がある地図ごとに
 * 保存済みの結果を読む。なければ（新しいワールド・設計図やシードが変わった）そのシード値で計算して保存する。
 * 結果は地図（{@link FieldMap#attachBake}）に持たせ、地形・バイオーム・水・道の生成がそれを使う。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class TerrainBakes {

    private TerrainBakes() {}

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    static void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        long seed = server.getWorldGenSettings().options().seed();
        Registry<FieldMap> maps = server.registryAccess().lookupOrThrow(ModWorldgen.FIELD_MAP);
        for (Map.Entry<ResourceKey<FieldMap>, FieldMap> entry : maps.entrySet()) {
            FieldMap map = entry.getValue();
            if (map.design().isEmpty()) {
                continue;
            }
            Identifier id = entry.getKey().identifier();
            Path dir = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(Kurumimnmod.MOD_ID).resolve("terrain");
            Path file = dir.resolve(id.getPath() + ".bin.gz");
            TerrainDesign design = map.design().get();
            TerrainBake bake = null;
            try {
                bake = TerrainBake.load(file, design, seed);
            } catch (Exception e) {
                LOGGER.warn("[地形v2] 保存データを読めなかったので計算し直します: {}", e.toString());
            }
            if (bake == null) {
                long start = System.currentTimeMillis();
                LOGGER.info("[地形v2] {} を計算します（シード {}）", id, seed);
                bake = TerrainBaker.bake(design, seed, map::v1BaseHeight, towns(map, design), roads(map, design), LOGGER);
                LOGGER.info("[地形v2] {} の計算が終わりました: {} 秒", id, (System.currentTimeMillis() - start) / 1000.0);
                try {
                    bake.save(file);
                    if (ModConstants.TERRAIN_WRITE_PREVIEW) {
                        bake.writePreview(dir.resolve(id.getPath() + "_preview.png"));
                    }
                } catch (Exception e) {
                    LOGGER.error("[地形v2] 保存できませんでした", e);
                }
            } else {
                LOGGER.info("[地形v2] {} の保存データを読みました", id);
            }
            map.attachBake(bake);
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        Registry<FieldMap> maps = event.getServer().registryAccess().lookupOrThrow(ModWorldgen.FIELD_MAP);
        for (FieldMap map : maps) {
            map.attachBake(null);
        }
    }

    /** 範囲の中の街（川がよけて流れる平らな台にする）。 */
    private static List<TerrainBaker.TownPad> towns(FieldMap map, TerrainDesign design) {
        List<TerrainBaker.TownPad> pads = new ArrayList<>();
        for (FieldMap.Town town : map.towns()) {
            if (inner(design, town.x(), town.z())) {
                pads.add(new TerrainBaker.TownPad(town.x(), town.z(), town.radius()));
            }
        }
        return pads;
    }

    /** 範囲の中で道を引く区間。範囲の外へ出る道は、今までの地形の道と縁でつながる所までにする。 */
    private static List<TerrainBaker.RoadRequest> roads(FieldMap map, TerrainDesign design) {
        List<TerrainBaker.RoadRequest> requests = new ArrayList<>();
        if (map.roads().isEmpty()) {
            return requests;
        }
        for (RoadSettings.Route route : map.roads().get().routes()) {
            var from = map.town(route.from());
            var to = map.town(route.to());
            if (from.isEmpty() || to.isEmpty()) {
                continue;
            }
            List<double[]> pts = new ArrayList<>();
            pts.add(new double[]{from.get().x(), from.get().z()});
            for (FieldMap.Point via : route.via()) {
                pts.add(new double[]{via.x(), via.z()});
            }
            pts.add(new double[]{to.get().x(), to.get().z()});
            boolean fromIn = inner(design, pts.get(0)[0], pts.get(0)[1]);
            boolean toIn = inner(design, pts.get(pts.size() - 1)[0], pts.get(pts.size() - 1)[1]);
            if (fromIn && toIn) {
                requests.add(new TerrainBaker.RoadRequest(pts.get(0)[0], pts.get(0)[1], pts.get(pts.size() - 1)[0], pts.get(pts.size() - 1)[1]));
            } else if (fromIn || toIn) {
                List<double[]> walk = new ArrayList<>(pts);
                if (!fromIn) {
                    java.util.Collections.reverse(walk);
                }
                double[] exit = exitPoint(design, walk);
                double[] joined = map.nearestV1Road(exit[0], exit[1], ModConstants.TERRAIN_ROAD_JOIN_SEARCH);
                requests.add(new TerrainBaker.RoadRequest(walk.get(0)[0], walk.get(0)[1], joined[0], joined[1]));
            }
        }
        return requests;
    }

    /** 縁のつなぎ目の真ん中より内側か（そこから内は v2 の道）。 */
    private static boolean inner(TerrainDesign design, double x, double z) {
        TerrainDesign.Region r = design.region();
        double inset = r.blend() * 0.5;
        return x > r.x0() + inset && z > r.z0() + inset && x < r.x0() + r.size() - inset && z < r.z0() + r.size() - inset;
    }

    /** 折れ線を内側から外へたどり、内側の最後の点を返す。 */
    private static double[] exitPoint(TerrainDesign design, List<double[]> pts) {
        double[] last = pts.get(0);
        for (int k = 0; k + 1 < pts.size(); k++) {
            double[] a = pts.get(k);
            double[] b = pts.get(k + 1);
            double len = Math.hypot(b[0] - a[0], b[1] - a[1]);
            for (double s = 0; s <= len; s += 4) {
                double x = a[0] + (b[0] - a[0]) * s / len;
                double z = a[1] + (b[1] - a[1]) * s / len;
                if (!inner(design, x, z)) {
                    return last;
                }
                last = new double[]{x, z};
            }
        }
        return last;
    }
}
