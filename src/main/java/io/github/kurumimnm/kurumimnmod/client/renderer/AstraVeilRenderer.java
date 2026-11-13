package io.github.kurumimnm.kurumimnmod.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.entity.AstraVeilEntity;
import io.github.kurumimnm.kurumimnmod.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * アストラ・ヴェイルのレンダラー（GeckoLib）。
 * <ul>
 *   <li>モデルを {@link ModConstants#ASTRA_VEIL_RENDER_SCALE} 倍に拡大して描く（当たり判定の大きさとは別）</li>
 *   <li>半透明で描く（幕の透け感・テクスチャの透明な穴）</li>
 *   <li>発光レイヤー：astra_veil_glowmask.png（目・顔の継ぎ目・幕のふち・葉脈・星の粒）</li>
 *   <li>影は出さない（空に浮かんでいるため）</li>
 *   <li>画面外判定を見た目全体の範囲に広げる（{@link AstraVeilEntity#cullingBox}）</li>
 * </ul>
 */
public class AstraVeilRenderer<R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<AstraVeilEntity, R> {

    public AstraVeilRenderer(EntityRendererProvider.Context context) {
        super(context, ModEntities.ASTRA_VEIL.get());
        withScale(ModConstants.ASTRA_VEIL_RENDER_SCALE);
        withRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 0f;
    }

    /** 半透明で描く（背面は描かない。キューブは閉じているので見た目は変わらず、重なりの崩れが減る）。 */
    @Override
    public @Nullable RenderType getRenderType(R renderState, Identifier texture) {
        return RenderTypes.entityTranslucentCull(texture);
    }

    /**
     * 画面外判定に使う範囲。巨体の一部（幕の先など）が画面に入っていれば描く。
     * （バージョンによってエンティティ側・レンダラー側のどちらで判定するかが違うので両方に置き、@Override は付けない）
     */
    public AABB getBoundingBoxForCulling(AstraVeilEntity entity) {
        return AstraVeilEntity.cullingBox(entity);
    }
}
