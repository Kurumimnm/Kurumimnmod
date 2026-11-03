package io.github.kurumimnm.kurumimnmod.client.scarecrow;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.entity.ScarecrowEntity;
import io.github.kurumimnm.kurumimnmod.registry.ModEntities;
import io.github.kurumimnm.kurumimnmod.scarecrow.DamageKind;
import io.github.kurumimnm.kurumimnmod.scarecrow.ScarecrowEvents;
import java.util.Deque;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;

/**
 * カカシのレンダラー（GeckoLib）＋頭上のダメージ数字。
 * <p>
 * GeckoLib のレンダラーには RenderLivingEvent が来ないため、{@link #submit} を拡張して
 * モデルの描画のあとにダメージ数字を描く。
 * <ul>
 *   <li>数字はカメラの方を向く（名札と同じビルボード）</li>
 *   <li>出現時に少し大きく → 上へ浮かびながら最後の 3 割でフェードアウト</li>
 *   <li>色・大きさは {@link DamageKind}（クリティカルは金色で大きく）</li>
 * </ul>
 * 位置・大きさ・色は ModConstants の DAMAGE_NUMBER_*、表示時間は ModClientConfigs。
 * <p>
 * R は「EntityRenderState かつ GeoRenderState」（IceIcicleRenderer と同じ書き方）。
 */
public class ScarecrowRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<ScarecrowEntity, R> {

    /** RenderState に書き込むエンティティ ID（数字の検索に使う）。 */
    private static final ContextKey<Integer> ENTITY_ID =
            new ContextKey<>(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "scarecrow_entity_id"));

    /** 消え始める時点（表示時間に対する割合）。 */
    private static final float FADE_START = 0.7f;

    public ScarecrowRenderer(EntityRendererProvider.Context context) {
        super(context, ModEntities.SCARECROW.get());
    }

    @Override
    public void extractRenderState(ScarecrowEntity entity, R state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.setRenderData(ENTITY_ID, entity.getId());
    }

    @Override
    public void submit(R state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        Integer id = state.getRenderData(ENTITY_ID);
        Deque<DamageNumbers.Entry> entries = id == null ? null : DamageNumbers.get(id);
        if (entries != null) {
            submitDamageNumbers(state, entries, poseStack, collector, camera);
        }
    }

    // =====================================================================
    // ダメージ数字
    // =====================================================================

    private void submitDamageNumbers(R state, Deque<DamageNumbers.Entry> entries, PoseStack poseStack,
                                     SubmitNodeCollector collector, CameraRenderState camera) {
        Font font = getFont();
        long now = System.currentTimeMillis();
        float duration = DamageNumbers.durationMillis();
        float baseY = state.boundingBoxHeight + ModConstants.DAMAGE_NUMBER_Y_OFFSET;

        for (DamageNumbers.Entry entry : entries) {
            float t = Mth.clamp((now - entry.startMillis()) / duration, 0f, 1f);
            float alpha = t < FADE_START ? 1f : 1f - (t - FADE_START) / (1f - FADE_START);
            int a = Math.round(alpha * 255);
            if (a < 8) {
                continue; // ほぼ透明（アルファが小さすぎる文字は正しく描かれないので出さない）
            }

            // ---- 大きさ：クリティカルは大きく、出現直後は少し大きく ----
            float scale = ModConstants.DAMAGE_NUMBER_SCALE;
            if (entry.kind() == DamageKind.CRITICAL || entry.kind() == DamageKind.BREAK) {
                scale *= ModConstants.DAMAGE_NUMBER_CRIT_SCALE;
            }
            float pop = ModConstants.DAMAGE_NUMBER_POP_TIME;
            if (t < pop) {
                scale *= Mth.lerp(t / pop, ModConstants.DAMAGE_NUMBER_POP_SCALE, 1f);
            }

            // ---- 位置：上へ浮かぶ（最初は速く、だんだん遅く） ----
            float rise = 1f - (1f - t) * (1f - t);
            float y = baseY + ModConstants.DAMAGE_NUMBER_RISE * rise;

            Component text = Component.literal(ScarecrowEvents.format(entry.amount()));
            if (entry.kind() == DamageKind.CRITICAL || entry.kind() == DamageKind.BREAK) {
                text = text.copy().withStyle(ChatFormatting.BOLD);
            }
            FormattedCharSequence sequence = text.getVisualOrderText();
            int color = (a << 24) | (entry.kind().color() & 0xFFFFFF);

            poseStack.pushPose();
            poseStack.translate(0f, y, 0f);
            // カメラの方を向ける（名札と同じ）。左右のばらつきは画面上の横方向にずらす
            poseStack.rotate(camera.orientation);
            poseStack.translate(entry.offsetX(), 0f, 0f);
            poseStack.scale(scale, -scale, scale);
            collector.submitText(poseStack, -font.width(sequence) / 2f, -font.lineHeight / 2f, sequence,
                    true, Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT, color, 0, 0);
            poseStack.popPose();
        }
    }
}
