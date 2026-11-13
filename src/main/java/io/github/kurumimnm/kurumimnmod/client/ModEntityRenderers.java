package io.github.kurumimnm.kurumimnmod.client;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.client.scarecrow.ScarecrowRenderer;
import io.github.kurumimnm.kurumimnmod.client.renderer.AstraVeilRenderer;
import io.github.kurumimnm.kurumimnmod.client.renderer.GarouRenderer;
import io.github.kurumimnm.kurumimnmod.client.renderer.GriffinRenderer;
import io.github.kurumimnm.kurumimnmod.client.renderer.KoboldLordRenderer;
import io.github.kurumimnm.kurumimnmod.client.renderer.KoboldRenderer;
import io.github.kurumimnm.kurumimnmod.client.renderer.MinionRenderer;
import io.github.kurumimnm.kurumimnmod.client.renderer.SlimeRenderer;
import io.github.kurumimnm.kurumimnmod.client.renderer.IceIcicleRenderer;
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

        // グリフォン（鎧の表示切り替え・頭の向きを扱う専用レンダラー）
        event.registerEntityRenderer(ModEntities.GRIFFIN.get(), context -> new GriffinRenderer<>(context));
        event.registerEntityRenderer(ModEntities.MINION.get(), context -> new MinionRenderer<>(context));
        event.registerEntityRenderer(ModEntities.SLIME.get(), context -> new SlimeRenderer<>(context));
        event.registerEntityRenderer(ModEntities.GAROU.get(), context -> new GarouRenderer<>(context));
        event.registerEntityRenderer(ModEntities.KOBOLD.get(), context -> new KoboldRenderer<>(context));
        event.registerEntityRenderer(ModEntities.KOBOLD_LORD.get(), context -> new KoboldLordRenderer<>(context));

        // アストラ・ヴェイル（10 倍に拡大・半透明・発光レイヤー・画面外判定の拡大を行う専用レンダラー）
        event.registerEntityRenderer(ModEntities.ASTRA_VEIL.get(), context -> new AstraVeilRenderer<>(context));


        // 氷柱の杖の氷柱（飛ぶ方向に向けて回転させる専用レンダラー）
        event.registerEntityRenderer(ModEntities.ICE_ICICLE.get(), context -> new IceIcicleRenderer<>(context));
    }
}
