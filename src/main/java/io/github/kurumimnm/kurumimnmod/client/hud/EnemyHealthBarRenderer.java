package io.github.kurumimnm.kurumimnmod.client.hud;

import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModClientConfigs;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 敵の頭上に HP バー（数値なし）を表示する。
 * <p>
 * 26.x の描画は「エンティティ → RenderState（描画用スナップショット）→ 描画」の 2 段階なので、
 * <ol>
 *   <li>{@link RegisterRenderStateModifiersEvent} で HP の割合などを RenderState に書き込み</li>
 *   <li>{@link RenderLivingEvent.Post} でその値を読み出してバーを描く</li>
 * </ol>
 * という流れにしている。表示距離・対象は {@link ModClientConfigs} で変更可能。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID, value = Dist.CLIENT)
public final class EnemyHealthBarRenderer {

    private EnemyHealthBarRenderer() {}

    /** HP の割合（0〜1）。バーを出さないエンティティは -1。 */
    private static final ContextKey<Float> HEALTH_RATIO =
            new ContextKey<>(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "health_ratio"));
    /** バーを表示する高さ（エンティティの足元からのブロック数）。 */
    private static final ContextKey<Float> BAR_HEIGHT =
            new ContextKey<>(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "health_bar_height"));

    // ---- 1. RenderState に HP 情報を書き込む ----
    @SubscribeEvent
    static void onRegisterRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(
                new TypeToken<LivingEntityRenderer<? extends LivingEntity, LivingEntityRenderState, ?>>() {},
                (entity, state) -> {
                    // RenderState は使い回されるので、対象外のときも必ず値を上書きしておく（-1 = 表示しない）
                    if (!shouldShowBar(entity)) {
                        state.setRenderData(HEALTH_RATIO, -1f);
                        return;
                    }
                    state.setRenderData(HEALTH_RATIO, Math.clamp(entity.getHealth() / Math.max(1f, entity.getMaxHealth()), 0f, 1f));
                    float y = entity.getBbHeight() + ModConstants.ENEMY_BAR_Y_OFFSET;
                    if (entity.shouldShowName()) {
                        y += ModConstants.ENEMY_BAR_NAMETAG_OFFSET;
                    }
                    state.setRenderData(BAR_HEIGHT, y);
                });
    }

    /** HP バーを出す対象か。 */
    private static boolean shouldShowBar(LivingEntity entity) {
        if (!ModClientConfigs.ENEMY_BAR_ENABLED.get()) {
            return false;
        }
        if (entity instanceof Player || entity.isInvisible() || !entity.isAlive()) {
            return false;
        }
        return !ModClientConfigs.ENEMY_BAR_HOSTILE_ONLY.get() || entity instanceof Enemy;
    }

    // ---- 2. 描画 ----
    @SubscribeEvent
    static void onRenderLivingPost(RenderLivingEvent.Post<?, ?, ?> event) {
        LivingEntityRenderState state = event.getRenderState();
        Float ratio = state.getRenderData(HEALTH_RATIO);
        Float barY = state.getRenderData(BAR_HEIGHT);
        if (ratio == null || barY == null || ratio < 0f) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0f, barY, 0f);

        // この時点の PoseStack は「カメラからエンティティへの相対位置」だけを持っているので、
        // 平行移動成分 = エンティティ − カメラ。これを使ってバーをカメラの方へ向ける（ビルボード）
        Vector3f rel = poseStack.last().pose().getTranslation(new Vector3f());
        double range = ModClientConfigs.ENEMY_BAR_RANGE.get();
        if (rel.lengthSquared() > range * range) {
            poseStack.popPose();
            return;
        }
        float dx = -rel.x;
        float dy = -rel.y;
        float dz = -rel.z;
        float yaw = (float) Math.atan2(dx, dz);
        float pitch = (float) -Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
        poseStack.mulPose(new Quaternionf().rotateY(yaw).rotateX(pitch));

        float w = ModConstants.ENEMY_BAR_WIDTH;
        float h = ModConstants.ENEMY_BAR_HEIGHT;
        float b = ModConstants.ENEMY_BAR_BORDER;
        float left = -w / 2f;
        float fillRight = left + w * ratio;

        event.getSubmitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.textBackground(), (pose, consumer) -> {
            // 奥から順に：枠 → 背景 → HP（z を少しずつ手前にして重なりのちらつきを防ぐ）
            quad(consumer, pose, left - b, -b, left + w + b, h + b, 0.000f, ModConstants.ENEMY_BAR_COLOR_BORDER);
            quad(consumer, pose, left, 0f, left + w, h, 0.002f, ModConstants.ENEMY_BAR_COLOR_BACKGROUND);
            if (ratio > 0f) {
                quad(consumer, pose, left, 0f, fillRight, h, 0.004f, ModConstants.ENEMY_BAR_COLOR_FILL);
            }
        });
        poseStack.popPose();
    }

    /** 四角形を 1 枚追加する（裏面からも見えるよう両面分の頂点を出す）。 */
    private static void quad(VertexConsumer c, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float z, int color) {
        int light = LightCoordsUtil.FULL_BRIGHT;
        // 表
        c.addVertex(pose, x0, y1, z).setColor(color).setLight(light);
        c.addVertex(pose, x0, y0, z).setColor(color).setLight(light);
        c.addVertex(pose, x1, y0, z).setColor(color).setLight(light);
        c.addVertex(pose, x1, y1, z).setColor(color).setLight(light);
        // 裏
        c.addVertex(pose, x1, y1, z).setColor(color).setLight(light);
        c.addVertex(pose, x1, y0, z).setColor(color).setLight(light);
        c.addVertex(pose, x0, y0, z).setColor(color).setLight(light);
        c.addVertex(pose, x0, y1, z).setColor(color).setLight(light);
    }
}
