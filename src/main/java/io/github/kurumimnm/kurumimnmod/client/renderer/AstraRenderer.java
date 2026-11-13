package io.github.kurumimnm.kurumimnmod.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.entity.AstraEntity;
import io.github.kurumimnm.kurumimnmod.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * アストラのレンダラー（GeckoLib）。
 * <ul>
 *   <li>モデルを {@link ModConstants#ASTRA_RENDER_SCALE} 倍に拡大して描く（当たり判定の大きさとは別）</li>
 *   <li>半透明で描く（膜の透け感・テクスチャの透明な穴）</li>
 *   <li>発光レイヤー：astra_glowmask.png（目・口の中・膜のふちと筋・爪・星の粒）</li>
 *   <li>影は出さない（空に浮かんでいるため）</li>
 *   <li>画面外判定を見た目全体の範囲に広げる（{@link AstraEntity#cullingBox}）</li>
 * </ul>
 */
public class AstraRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<AstraEntity, R> {

    public AstraRenderer(EntityRendererProvider.Context context) {
        super(context, ModEntities.ASTRA.get());
        withScale(ModConstants.ASTRA_RENDER_SCALE);
        withRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 0f;
    }

    /** 半透明で描く（背面は描かない。キューブは閉じているので見た目は変わらず、重なりの崩れが減る）。 */
    @Override
    public @Nullable RenderType getRenderType(R renderState, Identifier texture) {
        return RenderTypes.entityTranslucentCull(texture);
    }

    /** 画面外判定に使う範囲。当たり判定ではなく、膜・触腕の先まで含めた見た目全体で判定する。 */
    @Override
    protected AABB getBoundingBoxForCulling(AstraEntity entity, float partialTick) {
        return AstraEntity.cullingBox(entity);
    }
}
