package io.github.kurumimnm.kurumimnmod.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.entity.MinionEntity;
import io.github.kurumimnm.kurumimnmod.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * ミニオンのレンダラー（GeckoLib）。head ボーンを見ている方向へ向ける。
 * モデル・アニメーション・テクスチャは登録名 "minion" から自動で決まる（geckolib/models/entity/minion.geo.json など）。
 */
public class MinionRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<MinionEntity, R> {

    private static final String HEAD_BONE = "head";

    public MinionRenderer(EntityRendererProvider.Context context) {
        super(context, ModEntities.MINION.get());
        this.shadowRadius = ModConstants.MINION_SHADOW_RADIUS;
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> renderPassInfo, BoneSnapshots snapshots) {
        // 頭を見ている方向へ向ける（GeckoLib のボーン回転はバニラと符号が逆なのでマイナスして足す。GriffinRenderer と同じ）
        if (renderPassInfo.renderState() instanceof LivingEntityRenderState living) {
            float yaw = Mth.clamp(living.yRot, -ModConstants.MINION_HEAD_MAX_YAW, ModConstants.MINION_HEAD_MAX_YAW);
            float pitch = Mth.clamp(living.xRot, -ModConstants.MINION_HEAD_MAX_PITCH, ModConstants.MINION_HEAD_MAX_PITCH);
            snapshots.ifPresent(HEAD_BONE, snapshot -> snapshot
                    .setRotY(snapshot.getRotY() - yaw * Mth.DEG_TO_RAD)
                    .setRotX(snapshot.getRotX() - pitch * Mth.DEG_TO_RAD));
        }
    }
}
