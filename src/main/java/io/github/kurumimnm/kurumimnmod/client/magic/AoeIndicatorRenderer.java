package io.github.kurumimnm.kurumimnmod.client.magic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.magic.MagicTargeting;
import io.github.kurumimnm.kurumimnmod.magic.MagicWand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Quaternionf;

/**
 * 魔法の杖の照準サークル（クライアント専用）。
 * <p>
 * 杖を右クリック長押ししている間、着弾予定地点に範囲の円を表示する。
 * <ul>
 *   <li>中心ほど濃く、外周ほど薄い塗り（中心ほどダメージが高いことを表す）＋外周の線</li>
 *   <li>当たったブロックの面に沿って表示（壁を狙えば壁に、地面なら地面に貼り付く）</li>
 *   <li>MP 不足・クールダウン中は赤色になる</li>
 * </ul>
 * 位置の計算はサーバーの発射処理と同じ {@link MagicTargeting#find} を使う。
 * 色・太さなどは {@link ModConstants} の AOE_*。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID, value = Dist.CLIENT)
public final class AoeIndicatorRenderer {

    private AoeIndicatorRenderer() {}

    /** 単色の図形を描くための真っ白なテクスチャ（頂点カラーで色を付ける）。 */
    private static final Identifier WHITE_TEXTURE =
            Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "textures/misc/white.png");

    @SubscribeEvent
    static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isUsingItem()) {
            return;
        }
        ItemStack stack = player.getUseItem();
        if (!(stack.getItem() instanceof MagicWand wand) || wand.aoeRadius() <= 0) {
            return;
        }

        MagicTargeting.Target target = MagicTargeting.find(player, wand.aimRange());
        float radius = (float) wand.aoeRadius();
        boolean ready = wand.canCast(player, stack);
        int fillCenter = ready ? ModConstants.AOE_COLOR_FILL_CENTER : ModConstants.AOE_COLOR_DISABLED_FILL_CENTER;
        int fillEdge = ready ? ModConstants.AOE_COLOR_FILL_EDGE : ModConstants.AOE_COLOR_DISABLED_FILL_EDGE;
        int ringColor = ready ? ModConstants.AOE_COLOR_RING : ModConstants.AOE_COLOR_DISABLED_RING;

        // PoseStack はカメラ基準なので、着弾地点 − カメラ位置 へ移動する
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        Vec3 pos = target.position();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(pos.x - camera.x, pos.y - camera.y, pos.z - camera.z);
        // 円は XZ 平面（上向き）で作るので、当たった面の向きに回転させる。何もない方向なら水平のまま
        Direction face = target.face();
        if (face != null && face != Direction.UP) {
            poseStack.rotate(new Quaternionf().rotationTo(0f, 1f, 0f, face.getStepX(), face.getStepY(), face.getStepZ()));
        }
        poseStack.translate(0f, ModConstants.AOE_SURFACE_OFFSET, 0f);

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
                // 塗り（中心 → 内側の縁。色は中心から外へ薄くなる）
                quad(consumer, pose,
                        0f, 0f, fillCenter,
                        c0 * inner, s0 * inner, fillEdge,
                        c1 * inner, s1 * inner, fillEdge,
                        0f, 0f, fillCenter);
                // 外周の線
                quad(consumer, pose,
                        c0 * inner, s0 * inner, ringColor,
                        c0 * radius, s0 * radius, ringColor,
                        c1 * radius, s1 * radius, ringColor,
                        c1 * inner, s1 * inner, ringColor);
            }
        });
        poseStack.popPose();
    }

    /** 水平な四角形を 1 枚追加する（x, z と色を 4 頂点分。裏からも見えるよう両面分出す）。 */
    private static void quad(VertexConsumer c, PoseStack.Pose pose,
                             float x0, float z0, int col0, float x1, float z1, int col1,
                             float x2, float z2, int col2, float x3, float z3, int col3) {
        vertex(c, pose, x0, z0, col0);
        vertex(c, pose, x1, z1, col1);
        vertex(c, pose, x2, z2, col2);
        vertex(c, pose, x3, z3, col3);
        // 裏面
        vertex(c, pose, x3, z3, col3);
        vertex(c, pose, x2, z2, col2);
        vertex(c, pose, x1, z1, col1);
        vertex(c, pose, x0, z0, col0);
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose pose, float x, float z, int color) {
        // テクスチャは全面白なので UV はどこでもよい（中央を指定）
        c.addVertex(pose, x, 0f, z).setColor(color).setUv(0.5f, 0.5f).setLight(LightCoordsUtil.FULL_BRIGHT);
    }
}
