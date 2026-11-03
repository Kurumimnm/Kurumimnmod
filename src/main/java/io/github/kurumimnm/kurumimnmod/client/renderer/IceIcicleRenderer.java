package io.github.kurumimnm.kurumimnmod.client.renderer;

import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
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
 * <p>
 * R は「EntityRenderState かつ GeoRenderState」（GeckoLib が実行時に EntityRenderState へ
 * GeoRenderState を合成するため。コンパイル時はジェネリクスで両方を満たすことを示す）。
 */
public class IceIcicleRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<IceIcicleEntity, R> {

    public IceIcicleRenderer(EntityRendererProvider.Context context) {
        super(context, ModEntities.ICE_ICICLE.get());
    }

    @Override
    protected void applyRotations(RenderPassInfo<R> renderPassInfo, PoseStack poseStack, float nativeScale) {
        Vec3 velocity = renderPassInfo.getOrDefaultGeckolibData(DataTickets.VELOCITY, Vec3.ZERO);
        if (velocity.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 dir = velocity.normalize();
        poseStack.rotate(new Quaternionf().rotationTo(0f, -1f, 0f, (float) dir.x, (float) dir.y, (float) dir.z));
    }
}
