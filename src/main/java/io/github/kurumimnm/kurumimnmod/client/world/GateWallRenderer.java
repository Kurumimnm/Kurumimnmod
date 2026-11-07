package io.github.kurumimnm.kurumimnmod.client.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.world.FieldMap;
import io.github.kurumimnm.kurumimnmod.world.FieldRegion;
import io.github.kurumimnm.kurumimnmod.world.GateRules;
import io.github.kurumimnm.kurumimnmod.world.PlayerExploration;
import io.github.kurumimnm.kurumimnmod.world.RegionGrid;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * ボーダー（赤い壁）を描く（クライアント専用）。
 * <p>
 * プレイヤーの周り {@link ModConstants#GATE_RENDER_RADIUS} ブロックの柱ごとの「場所」を調べ、
 * 通れない境目（{@link GateRules#hasWall}）に、地面から高く立ち上がる赤い膜を描く。
 * 膜には上へ流れる光の線が入り、遠くからでも「そこに壁がある」とわかる。手前ははっきり、遠くはだんだん薄く。
 * <p>
 * 境目の計算は重いので裏のスレッドで行い、プレイヤーが {@link ModConstants#GATE_RECOMPUTE_STEP} ブロック動くか、
 * 探索の記録が変わったときだけやり直す。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID, value = Dist.CLIENT)
public final class GateWallRenderer {

    private GateWallRenderer() {}

    private static final Identifier WHITE_TEXTURE =
            Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "textures/misc/white.png");

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Kurumimnmod gate walls");
        thread.setDaemon(true);
        return thread;
    });

    /** 壁の 1 枚（1 ブロック幅）。alongX = true なら X 方向に伸びる（z が一定）。ground は足元の地面の高さ（未計算なら NaN）。 */
    private static final class Segment {
        final int x;
        final int z;
        final boolean alongX;
        float ground = Float.NaN;

        Segment(int x, int z, boolean alongX) {
            this.x = x;
            this.z = z;
            this.alongX = alongX;
        }
    }

    // 計算結果（描画スレッドだけが触る）
    private static List<Segment> segments = List.of();
    private static int cachedX = Integer.MIN_VALUE;
    private static int cachedZ = Integer.MIN_VALUE;
    private static PlayerExploration cachedExploration;
    private static FieldMap cachedMap;
    private static CompletableFuture<List<Segment>> pending;

    @SubscribeEvent
    static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        FieldMap map = ClientFieldWorld.map();
        if (player == null || level == null || map == null) {
            segments = List.of();
            return;
        }
        update(map, ClientFieldWorld.exploration(player), player.getBlockX(), player.getBlockZ());
        if (segments.isEmpty()) {
            return;
        }

        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        float playerY = (float) player.getY();
        double time = System.currentTimeMillis() / 1000.0;
        double radius = ModConstants.GATE_RENDER_RADIUS;
        double fadeStart = radius * ModConstants.GATE_FADE_START;
        float lineOffset = (float) ((time * ModConstants.GATE_LINE_SPEED) % ModConstants.GATE_LINE_SPACING);
        int rgb = ModConstants.GATE_COLOR & 0xFFFFFF;
        List<Segment> drawn = segments;

        PoseStack poseStack = event.getPoseStack();
        event.getSubmitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.text(WHITE_TEXTURE), (pose, consumer) -> {
            for (Segment s : drawn) {
                if (Float.isNaN(s.ground)) {
                    s.ground = groundAt(level, s, playerY);
                }
                double dist = Math.hypot(s.x + 0.5 - player.getX(), s.z + 0.5 - player.getZ());
                double fade = dist <= fadeStart ? 1 : Math.max(0, 1 - (dist - fadeStart) / (radius - fadeStart));
                if (fade <= 0.01) {
                    continue;
                }
                float ax = (float) (s.x - camera.x);
                float az = (float) (s.z - camera.z);
                float bx = s.alongX ? ax + 1 : ax;
                float bz = s.alongX ? az : az + 1;
                float worldBottom = Math.min(s.ground, playerY) - ModConstants.GATE_RENDER_BELOW;
                float worldTop = Math.max(s.ground, playerY) + ModConstants.GATE_RENDER_ABOVE;
                float y0 = (float) (worldBottom - camera.y);
                float y1 = (float) (worldTop - camera.y);
                float yFadeTop = y0 + (y1 - y0) * 0.55f; // ここから上はだんだん透明に

                // 膜（下はくっきり、上へぼかす）
                int solid = argb(ModConstants.GATE_MAX_ALPHA * fade, rgb);
                wall(consumer, pose, ax, az, bx, bz, y0, solid, yFadeTop, solid);
                wall(consumer, pose, ax, az, bx, bz, yFadeTop, solid, y1, rgb);

                // 上へ流れる光の線（ワールド座標で揃えるので、隣の柱と線がつながる）
                float spacing = ModConstants.GATE_LINE_SPACING;
                float first = (float) (Math.floor((worldBottom - lineOffset) / spacing) * spacing + lineOffset);
                for (float wy = first; wy < worldTop; wy += spacing) {
                    if (wy < worldBottom) {
                        continue;
                    }
                    double heightFade = wy < worldBottom + (worldTop - worldBottom) * 0.55f ? 1
                            : Math.max(0, (worldTop - wy) / ((worldTop - worldBottom) * 0.45f));
                    int line = argb(ModConstants.GATE_LINE_ALPHA * fade * heightFade, rgb | 0x404040);
                    float ly = (float) (wy - camera.y);
                    wall(consumer, pose, ax, az, bx, bz, ly, line, ly + ModConstants.GATE_LINE_WIDTH, line);
                }
            }
        });
    }

    /** 必要なら裏で計算し直し、終わっていたら結果を受け取る。 */
    private static void update(FieldMap map, PlayerExploration exploration, int px, int pz) {
        if (pending != null && pending.isDone()) {
            segments = pending.getNow(List.of());
            pending = null;
        }
        int step = ModConstants.GATE_RECOMPUTE_STEP;
        boolean moved = Math.abs(px - cachedX) >= step || Math.abs(pz - cachedZ) >= step;
        if (pending == null && (moved || exploration != cachedExploration || map != cachedMap)) {
            boolean jumped = map != cachedMap || Math.abs(px - cachedX) > ModConstants.GATE_RENDER_RADIUS;
            cachedX = px;
            cachedZ = pz;
            cachedExploration = exploration;
            cachedMap = map;
            if (jumped) {
                segments = List.of(); // 別の場所へワープしたら古い壁は消す
            }
            pending = CompletableFuture.supplyAsync(() -> compute(map, exploration, px, pz), EXECUTOR);
        }
    }

    /** 境目を探す（裏のスレッド）。 */
    private static List<Segment> compute(FieldMap map, PlayerExploration exploration, int px, int pz) {
        int r = ModConstants.GATE_RENDER_RADIUS;
        int size = r * 2 + 2;
        FieldRegion[] regions = new FieldRegion[size * size];
        for (int dz = 0; dz < size; dz++) {
            for (int dx = 0; dx < size; dx++) {
                regions[dz * size + dx] = RegionGrid.at(map, px - r + dx, pz - r + dz);
            }
        }
        List<Segment> result = new ArrayList<>();
        long r2 = (long) (r + 1) * (r + 1);
        for (int dz = 0; dz < size; dz++) {
            for (int dx = 0; dx < size; dx++) {
                long ox = dx - r;
                long oz = dz - r;
                if (ox * ox + oz * oz > r2) {
                    continue;
                }
                FieldRegion here = regions[dz * size + dx];
                if (dx + 1 < size) {
                    FieldRegion east = regions[dz * size + dx + 1];
                    if (!here.sameZone(east) && GateRules.hasWall(exploration, here, east)) {
                        result.add(new Segment(px - r + dx + 1, pz - r + dz, false)); // x が一定の面
                    }
                }
                if (dz + 1 < size) {
                    FieldRegion south = regions[(dz + 1) * size + dx];
                    if (!here.sameZone(south) && GateRules.hasWall(exploration, here, south)) {
                        result.add(new Segment(px - r + dx, pz - r + dz + 1, true)); // z が一定の面
                    }
                }
            }
        }
        return result;
    }

    /** 壁の足元の地面の高さ（境目の両側の高いほう）。チャンクが読み込まれていなければプレイヤーの高さ。 */
    private static float groundAt(ClientLevel level, Segment s, float fallback) {
        int ax = s.x;
        int az = s.z;
        int bx = s.alongX ? s.x : s.x - 1;
        int bz = s.alongX ? s.z - 1 : s.z;
        if (!level.hasChunk(ax >> 4, az >> 4) || !level.hasChunk(bx >> 4, bz >> 4)) {
            return fallback;
        }
        int ha = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ax, az);
        int hb = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
        return Math.max(ha, hb);
    }

    private static int argb(double alpha, int rgb) {
        int a = (int) Math.round(255 * Math.max(0, Math.min(1, alpha)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /** 縦の四角形を 1 枚（裏表）。下端の色 bottomColor、上端の色 topColor。 */
    private static void wall(VertexConsumer c, PoseStack.Pose pose, float ax, float az, float bx, float bz,
                             float bottom, int bottomColor, float top, int topColor) {
        vertex(c, pose, ax, bottom, az, bottomColor);
        vertex(c, pose, bx, bottom, bz, bottomColor);
        vertex(c, pose, bx, top, bz, topColor);
        vertex(c, pose, ax, top, az, topColor);
        // 裏面
        vertex(c, pose, ax, top, az, topColor);
        vertex(c, pose, bx, top, bz, topColor);
        vertex(c, pose, bx, bottom, bz, bottomColor);
        vertex(c, pose, ax, bottom, az, bottomColor);
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z, int color) {
        c.addVertex(pose, x, y, z).setColor(color).setUv(0.5f, 0.5f).setLight(LightCoordsUtil.FULL_BRIGHT);
    }
}
