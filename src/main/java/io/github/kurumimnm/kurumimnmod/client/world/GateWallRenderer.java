package io.github.kurumimnm.kurumimnmod.client.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.world.FieldMap;
import io.github.kurumimnm.kurumimnmod.world.FieldRegion;
import io.github.kurumimnm.kurumimnmod.world.GateRules;
import io.github.kurumimnm.kurumimnmod.world.PlayerExploration;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * ゲートの赤い壁を描く（クライアント専用）。
 * <p>
 * プレイヤーの周り（{@link ModConstants#GATE_RENDER_RADIUS} ブロック）の 1 ブロックごとの「場所」を調べ、
 * 隣り合うマスの場所が違い、そこを通れない（{@link GateRules#hasWall}）境目に、縦の赤い膜を描く。
 * 遠いほど・上下の端ほど薄くなり、ゆっくり明滅する。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID, value = Dist.CLIENT)
public final class GateWallRenderer {

    private GateWallRenderer() {}

    private static final Identifier WHITE_TEXTURE =
            Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "textures/misc/white.png");

    /** 壁の 1 枚（1 ブロック幅）。alongX = true なら X 方向に伸びる（z が一定）。 */
    private record Segment(int x, int z, boolean alongX) {}

    // 計算結果のキャッシュ（プレイヤーが別のブロックへ移るか、探索の記録が変わったら作り直す）
    private static List<Segment> segments = List.of();
    private static int cachedX = Integer.MIN_VALUE;
    private static int cachedZ = Integer.MIN_VALUE;
    private static PlayerExploration cachedExploration;
    private static FieldMap cachedMap;

    @SubscribeEvent
    static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        FieldMap map = ClientFieldWorld.map();
        if (player == null || map == null) {
            return;
        }
        PlayerExploration exploration = ClientFieldWorld.exploration(player);
        int px = player.getBlockX();
        int pz = player.getBlockZ();
        if (px != cachedX || pz != cachedZ || exploration != cachedExploration || map != cachedMap) {
            segments = compute(map, exploration, px, pz);
            cachedX = px;
            cachedZ = pz;
            cachedExploration = exploration;
            cachedMap = map;
        }
        if (segments.isEmpty()) {
            return;
        }

        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        double baseY = player.getY();
        float y0 = (float) (baseY - ModConstants.GATE_RENDER_BELOW - camera.y);
        float y1 = (float) (baseY + ModConstants.GATE_RENDER_ABOVE - camera.y);
        float yMid0 = y0 + (y1 - y0) * 0.2f;
        float yMid1 = y0 + (y1 - y0) * 0.7f;
        double time = System.currentTimeMillis() / 1000.0;
        double radius = ModConstants.GATE_RENDER_RADIUS;

        PoseStack poseStack = event.getPoseStack();
        event.getSubmitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.text(WHITE_TEXTURE), (pose, consumer) -> {
            for (Segment s : segments) {
                float ax = (float) (s.x() - camera.x);
                float az = (float) (s.z() - camera.z);
                float bx = s.alongX() ? ax + 1 : ax;
                float bz = s.alongX() ? az : az + 1;
                // 遠いほど薄く、ゆっくり明滅する
                double dist = Math.hypot(s.x() + 0.5 - player.getX(), s.z() + 0.5 - player.getZ());
                double fade = Math.max(0, 1 - dist / radius);
                double shimmer = 0.75 + 0.25 * Math.sin(time * ModConstants.GATE_STRIPE_SPEED * Math.PI * 2
                        + (s.x() + s.z()) / ModConstants.GATE_STRIPE_SPACING);
                int alpha = (int) Math.round(255 * ModConstants.GATE_MAX_ALPHA * fade * shimmer);
                if (alpha <= 2) {
                    continue;
                }
                int solid = (alpha << 24) | (ModConstants.GATE_COLOR & 0xFFFFFF);
                int clear = ModConstants.GATE_COLOR & 0xFFFFFF;
                // 下（透明→濃い）・中（濃い）・上（濃い→透明）の 3 段に分けて、上下の端をぼかす
                wall(consumer, pose, ax, az, bx, bz, y0, clear, yMid0, solid);
                wall(consumer, pose, ax, az, bx, bz, yMid0, solid, yMid1, solid);
                wall(consumer, pose, ax, az, bx, bz, yMid1, solid, y1, clear);
            }
        });
    }

    /** 境目を探す。 */
    private static List<Segment> compute(FieldMap map, PlayerExploration exploration, int px, int pz) {
        int r = ModConstants.GATE_RENDER_RADIUS;
        int size = r * 2 + 2;
        FieldRegion[] regions = new FieldRegion[size * size];
        for (int dz = 0; dz < size; dz++) {
            for (int dx = 0; dx < size; dx++) {
                regions[dz * size + dx] = map.regionAt(px - r + dx + 0.5, pz - r + dz + 0.5);
            }
        }
        List<Segment> result = new ArrayList<>();
        for (int dz = 0; dz < size; dz++) {
            for (int dx = 0; dx < size; dx++) {
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
