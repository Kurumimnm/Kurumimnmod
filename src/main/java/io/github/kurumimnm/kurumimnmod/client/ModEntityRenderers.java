package io.github.kurumimnm.kurumimnmod.client;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.client.scarecrow.ScarecrowRenderer;
import io.github.kurumimnm.kurumimnmod.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * エンティティレンダラーの登録クラス（クライアント専用）。
 * <p>
 * GeckoLib のエンティティは {@code GeoEntityRenderer} で描画する。
 * エンティティタイプを渡すと、登録名からモデル・アニメーション・テクスチャの
 * パスが自動で決まる（DefaultedEntityGeoModel）。
 * <pre>{@code
 * event.registerEntityRenderer(ModEntities.WALNUT_MOB.get(),
 *         context -> new GeoEntityRenderer<>(context, ModEntities.WALNUT_MOB.get()));
 * }</pre>
 * 影の大きさを変える・発光レイヤーを足すなど、描画を調整したくなったら
 * client/renderer パッケージに専用のレンダラークラスを作る。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID, value = Dist.CLIENT)
public final class ModEntityRenderers {

    private ModEntityRenderers() {}

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // エンティティを追加したらここにレンダラーを登録する（上の Javadoc の例を参照）

        // カカシ（頭上のダメージ数字も描く専用レンダラー）
        event.registerEntityRenderer(ModEntities.SCARECROW.get(), context -> new ScarecrowRenderer<>(context));
    }
}
