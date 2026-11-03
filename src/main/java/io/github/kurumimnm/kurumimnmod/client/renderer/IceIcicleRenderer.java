package io.github.kurumimnm.kurumimnmod.client.renderer;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kurumimnm.kurumimnmod.entity.IceIcicleEntity;
import io.github.kurumimnm.kurumimnmod.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * 氷柱のレンダラー（GeckoLib）。
 * <p>
 * モデル（ice_icicle.geo.json）は「先端が真下（-Y）」の向きで作ってあるので、
 * 先端が飛んでいく方向を向くように回転させる。向きは速度から求める
 * （速度が 0 = 着弾直後は回転させない）。
 * 回転の中心はエンティティの位置（モデルの原点）。
 */
public class IceIcicleRenderer extends GeoEntityRenderer<IceIcicleEntity, EntityRenderState> {

    public IceIcicleRenderer(EntityRendererProvider.Context context) {
        super(context, ModEntities.ICE_ICICLE.get());
    }

    @Override
    protected void applyRotations(RenderPassInfo<EntityRenderState> renderPassInfo, PoseStack poseStack, float nativeScale) {
        Vec3 velocity = renderPassInfo.renderState().getOrDefaultGeckolibData(DataTickets.VELOCITY, Vec3.ZERO);
        if (velocity.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 dir = velocity.normalize();
        poseStack.mulPose(new Quaternionf().rotationTo(0f, -1f, 0f, (float) dir.x, (float) dir.y, (float) dir.z));
    }
}
