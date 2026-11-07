package io.github.kurumimnm.kurumimnmod.client.world;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.world.FieldMap;
import io.github.kurumimnm.kurumimnmod.world.FieldRegion;
import io.github.kurumimnm.kurumimnmod.world.PlayerExploration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * 地図画面の画像（クライアント専用）。
 * <p>
 * 地図（{@link FieldMap}）から、上から見た地形の色（フィールドの map_color ＋ 高さの陰影、山脈は高さで灰〜白）を計算して作る。
 * 実際のチャンクを読み込まなくても描けるので、遠くの場所も表示できる。
 * <ul>
 *   <li>行ったことのないエリアは暗く塗りつぶす（エリア単位で解放）</li>
 *   <li>行ったことのあるエリアの境目は白い線</li>
 * </ul>
 * 1 ピクセル = {@link ModConstants#MAP_BLOCKS_PER_PIXEL} ブロック。画像を作るのは重いので、別スレッドで 1 回だけ作る。
 */
public final class WorldMapImage {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "world_map");

    /** 街のマスの「場所」の番号（-1 − 街の並び順）。 */
    private static final int NO_REGION = Integer.MIN_VALUE;

    /** 計算済みの地形（地図ごと）。 */
    private record Base(FieldMap map, int width, int height, int originX, int originZ,
                        int[] keys, int[] colors, Map<Integer, String> townByKey) {}

    private static @Nullable CompletableFuture<Base> building;
    private static @Nullable Base base;
    private static @Nullable DynamicTexture texture;
    private static @Nullable PlayerExploration shownExploration;

    private WorldMapImage() {}

    static void clear() {
        building = null;
        base = null;
        shownExploration = null;
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(TEXTURE);
            texture = null;
        }
    }

    /** 画像の準備ができていれば true（まだなら作り始める）。 */
    static boolean ready(FieldMap map, PlayerExploration exploration) {
        if (base == null || base.map() != map) {
            if (building == null) {
                building = CompletableFuture.supplyAsync(() -> build(map), Util.backgroundExecutor());
            }
            if (!building.isDone()) {
                return false;
            }
            base = building.join();
            building = null;
            shownExploration = null;
        }
        if (exploration != shownExploration) {
            upload(exploration);
            shownExploration = exploration;
        }
        return true;
    }

    static int width() {
        return base == null ? 1 : base.width();
    }

    static int height() {
        return base == null ? 1 : base.height();
    }

    /** 画像の左上のブロック座標。 */
    static int originX() {
        return base == null ? 0 : base.originX();
    }

    static int originZ() {
        return base == null ? 0 : base.originZ();
    }

    // =====================================================================
    // 地形の計算（別スレッド）
    // =====================================================================

    private static Base build(FieldMap map) {
        int bpp = ModConstants.MAP_BLOCKS_PER_PIXEL;
        int margin = ModConstants.MAP_MARGIN;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (FieldMap.Town town : map.towns()) {
            minX = Math.min(minX, town.x());
            maxX = Math.max(maxX, town.x());
            minZ = Math.min(minZ, town.z());
            maxZ = Math.max(maxZ, town.z());
        }
        for (FieldMap.Field field : map.fields()) {
            for (FieldMap.AreaSeed seed : field.areas()) {
                minX = Math.min(minX, seed.x());
                maxX = Math.max(maxX, seed.x());
                minZ = Math.min(minZ, seed.z());
                maxZ = Math.max(maxZ, seed.z());
            }
        }
        int originX = minX - margin;
        int originZ = minZ - margin;
        int width = (maxX + margin - originX) / bpp + 1;
        int height = (maxZ + margin - originZ) / bpp + 1;

        Map<String, Integer> townKeys = new HashMap<>();
        Map<Integer, String> townByKey = new HashMap<>();
        for (int i = 0; i < map.towns().size(); i++) {
            townKeys.put(map.towns().get(i).id(), -1 - i);
            townByKey.put(-1 - i, map.towns().get(i).id());
        }

        int[] keys = new int[width * height];
        int[] colors = new int[width * height];
        double[] heights = new double[width * height];
        for (int pz = 0; pz < height; pz++) {
            for (int px = 0; px < width; px++) {
                double x = originX + (px + 0.5) * bpp;
                double z = originZ + (pz + 0.5) * bpp;
                int i = pz * width + px;
                FieldRegion region = map.regionAt(x, z);
                keys[i] = region.isTown() ? townKeys.getOrDefault(region.town(), NO_REGION) : region.areaKey();
                heights[i] = map.terrainHeight(x, z);
                colors[i] = baseColor(map, region, x, z, heights[i]);
            }
        }
        // 陰影（北西から光が当たる）
        for (int pz = 1; pz < height; pz++) {
            for (int px = 1; px < width; px++) {
                int i = pz * width + px;
                double slope = heights[i] - heights[i - width - 1];
                float shade = (float) Math.max(0.6, Math.min(1.35, 1 + slope * 0.04));
                colors[i] = scale(colors[i], shade);
            }
        }
        return new Base(map, width, height, originX, originZ, keys, colors, townByKey);
    }

    private static int baseColor(FieldMap map, FieldRegion region, double x, double z, double height) {
        if (region.isTown()) {
            return 0xFFD8C8A0;
        }
        io.github.kurumimnm.kurumimnmod.world.terrain.TerrainBake bake = map.bake();
        if (bake != null && bake.covers(x, z)) {
            // 地形 v2：水・高さで色を決める
            double water = bake.waterLevel(x, z);
            if (!Double.isNaN(water) && water > height) {
                return water <= bake.seaLevel() + 0.5 ? ModConstants.MAP_COLOR_SEA : ModConstants.MAP_COLOR_WATER;
            }
            int fieldColor = 0xFF000000 | map.field(region.field()).map(FieldMap.Field::mapColor).orElse(0x7FB238);
            if (height > ModConstants.MAP_ROCK_FROM) {
                float t = (float) Math.max(0, Math.min(1, (height - ModConstants.MAP_ROCK_FROM) / (ModConstants.MAP_ROCK_TO - ModConstants.MAP_ROCK_FROM)));
                return lerp(fieldColor, t > 0.6f ? ModConstants.MAP_COLOR_MOUNTAIN_HIGH : ModConstants.MAP_COLOR_MOUNTAIN_LOW, Math.min(1, t * 1.4f));
            }
            return fieldColor;
        }
        if (map.mountains().isPresent() && map.mountainPosition(x, z) > map.mountains().get().biomeThreshold()) {
            float t = (float) Math.max(0, Math.min(1, (height - map.terrain().baseHeight())
                    / Math.max(1, map.terrain().maxHeight() - map.terrain().baseHeight())));
            return lerp(ModConstants.MAP_COLOR_MOUNTAIN_LOW, ModConstants.MAP_COLOR_MOUNTAIN_HIGH, t);
        }
        return 0xFF000000 | map.field(region.field()).map(FieldMap.Field::mapColor).orElse(0x7FB238);
    }

    // =====================================================================
    // 行ったことのある場所だけ見せて、テクスチャにする（メインスレッド）
    // =====================================================================

    private static void upload(PlayerExploration exploration) {
        Base b = base;
        if (b == null) {
            return;
        }
        int w = b.width();
        int h = b.height();
        boolean[] visible = new boolean[w * h];
        for (int i = 0; i < visible.length; i++) {
            visible[i] = isVisible(b, b.keys()[i], exploration);
        }
        NativeImage image = new NativeImage(w, h, false);
        for (int pz = 0; pz < h; pz++) {
            for (int px = 0; px < w; px++) {
                int i = pz * w + px;
                int color;
                if (!visible[i]) {
                    color = ModConstants.MAP_COLOR_UNEXPLORED;
                } else if ((px + 1 < w && b.keys()[i + 1] != b.keys()[i])
                        || (pz + 1 < h && b.keys()[i + w] != b.keys()[i])
                        || (px > 0 && b.keys()[i - 1] != b.keys()[i] && !visible[i - 1])
                        || (pz > 0 && b.keys()[i - w] != b.keys()[i] && !visible[i - w])) {
                    color = ModConstants.MAP_COLOR_AREA_LINE; // エリアの境目
                } else {
                    color = b.colors()[i];
                }
                image.setPixel(px, pz, color);
            }
        }
        if (texture == null) {
            texture = new DynamicTexture(() -> "Kurumimnmod world map", image);
            Minecraft.getInstance().getTextureManager().register(TEXTURE, texture);
        } else {
            texture.setPixels(image);
        }
        texture.upload();
    }

    private static boolean isVisible(Base b, int key, PlayerExploration exploration) {
        if (key == NO_REGION) {
            return false;
        }
        if (key < 0) {
            String town = b.townByKey().get(key);
            return town != null && exploration.towns().contains(town);
        }
        return exploration.areas().contains(key);
    }

    /** ブロック座標 (x, z) が地図で見えているか（行ったことのある場所か）。 */
    static boolean isVisibleAt(FieldMap map, PlayerExploration exploration, double x, double z) {
        FieldRegion region = map.regionAt(x, z);
        return exploration.visited(region);
    }

    private static int scale(int argb, float f) {
        int r = Math.min(255, Math.round(((argb >> 16) & 0xFF) * f));
        int g = Math.min(255, Math.round(((argb >> 8) & 0xFF) * f));
        int b = Math.min(255, Math.round((argb & 0xFF) * f));
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static int lerp(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
