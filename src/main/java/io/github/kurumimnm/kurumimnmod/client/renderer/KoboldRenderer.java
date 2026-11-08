package io.github.kurumimnm.kurumimnmod.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.entity.KoboldEntity;
import io.github.kurumimnm.kurumimnmod.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * KoboldEntity のレンダラー（GeckoLib）。
 * <ul>
 *   <li>スキルの予兆中だけ頭の上の「！」（alert ボーン）を表示する（{@link TelegraphBones}）</li>
 *   <li>head ボーンを見ている方向へ向ける</li>
 * </ul>
 * モデル・アニメーション・テクスチャは登録名 "kobold" から自動で決まる（geckolib/models/entity/kobold.geo.json など）。
 */
public class KoboldRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<KoboldEntity, R> {

    private static final String HEAD_BONE = "head";

    public KoboldRenderer(EntityRendererProvider.Context context) {
        super(context, ModEntities.KOBOLD.get());
        this.shadowRadius = ModConstants.KOBOLD_SHADOW_RADIUS;
    }

    @Override
    public void extractRenderState(KoboldEntity entity, R state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        TelegraphBones.extract(entity, state);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> renderPassInfo, BoneSnapshots snapshots) {
        TelegraphBones.apply(renderPassInfo, snapshots);
        // 頭を見ている方向へ向ける（GriffinRenderer と同じ）
        if (renderPassInfo.renderState() instanceof LivingEntityRenderState living) {
            float yaw = Mth.clamp(living.yRot, -ModConstants.KOBOLD_HEAD_MAX_YAW, ModConstants.KOBOLD_HEAD_MAX_YAW);
            float pitch = Mth.clamp(living.xRot, -ModConstants.KOBOLD_HEAD_MAX_PITCH, ModConstants.KOBOLD_HEAD_MAX_PITCH);
            snapshots.ifPresent(HEAD_BONE, snapshot -> snapshot
                    .setRotY(snapshot.getRotY() - yaw * Mth.DEG_TO_RAD)
                    .setRotX(snapshot.getRotX() - pitch * Mth.DEG_TO_RAD));
        }
    }
}
