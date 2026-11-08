package io.github.kurumimnm.kurumimnmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.entity.KoboldLordEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * ボスのスキルの着地点の範囲（地面の赤い円。クライアント専用）。
 * <p>
 * 今はコボルトロードの壁蹴り突撃だけ。サーバーが同期した中心（{@link KoboldLordEntity#getAoeCenter}）に、
 * Config の半径（koboldLordKickRadius）の円を描く。描き方は氷柱の杖の照準サークル（AoeIndicatorRenderer）と同じ。
 * 色は ModConstants の BOSS_AOE_*、分割数・線の太さは AOE_*。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID, value = Dist.CLIENT)
public final class BossAoeRenderer {

    private BossAoeRenderer() {}

    private static final Identifier WHITE_TEXTURE =
            Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "textures/misc/white.png");

    @SubscribeEvent
    static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof KoboldLordEntity lord) {
                Vec3 center = lord.getAoeCenter();
                if (center != null) {
                    drawCircle(event, camera, center, (float) ModConfigs.koboldLordKickRadius());
                }
            }
        }
    }

    private static void drawCircle(SubmitCustomGeometryEvent event, Vec3 camera, Vec3 pos, float radius) {
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(pos.x - camera.x, pos.y - camera.y + ModConstants.AOE_SURFACE_OFFSET, pos.z - camera.z);
        int fillCenter = ModConstants.BOSS_AOE_COLOR_FILL_CENTER;
        int fillEdge = ModConstants.BOSS_AOE_COLOR_FILL_EDGE;
        int ring = ModConstants.BOSS_AOE_COLOR_RING;
        event.getSubmitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.text(WHITE_TEXTURE), (pose, consumer) -> {
            int segments = ModConstants.AOE_SEGMENTS;
            float inner = Math.max(0f, radius - ModConstants.AOE_RING_WIDTH);
            for (int i = 0; i < segments; i++) {
                double a0 = Math.PI * 2 * i / segments;
                double a1 = Math.PI * 2 * (i + 1) / segments;
                float c0 = (float) Math.cos(a0);
                float s0 = (float) Math.sin(a0);
                float c1 = (float) Math.cos(a1);
                float s1 = (float) Math.sin(a1);
                quad(consumer, pose, 0f, 0f, fillCenter, c0 * inner, s0 * inner, fillEdge,
                        c1 * inner, s1 * inner, fillEdge, 0f, 0f, fillCenter);
                quad(consumer, pose, c0 * inner, s0 * inner, ring, c0 * radius, s0 * radius, ring,
                        c1 * radius, s1 * radius, ring, c1 * inner, s1 * inner, ring);
            }
        });
        poseStack.popPose();
    }

    /** 水平な四角形（両面）。 */
    private static void quad(VertexConsumer c, PoseStack.Pose pose,
                             float x0, float z0, int col0, float x1, float z1, int col1,
                             float x2, float z2, int col2, float x3, float z3, int col3) {
        vertex(c, pose, x0, z0, col0);
        vertex(c, pose, x1, z1, col1);
        vertex(c, pose, x2, z2, col2);
        vertex(c, pose, x3, z3, col3);
        vertex(c, pose, x3, z3, col3);
        vertex(c, pose, x2, z2, col2);
        vertex(c, pose, x1, z1, col1);
        vertex(c, pose, x0, z0, col0);
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose pose, float x, float z, int color) {
        c.addVertex(pose, x, 0f, z).setColor(color).setUv(0.5f, 0.5f).setLight(LightCoordsUtil.FULL_BRIGHT);
    }
}
