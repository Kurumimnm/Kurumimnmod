package io.github.kurumimnm.kurumimnmod.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.entity.SlimeEntity;
import io.github.kurumimnm.kurumimnmod.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * スライム（MOD 独自）のレンダラー（GeckoLib）。
 * モデル・アニメーション・テクスチャは登録名 "slime" から自動で決まる（geckolib/models/entity/slime.geo.json など）。
 */
public class SlimeRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<SlimeEntity, R> {

    public SlimeRenderer(EntityRendererProvider.Context context) {
        super(context, ModEntities.SLIME.get());
        this.shadowRadius = ModConstants.SLIME_SHADOW_RADIUS;
    }
}
