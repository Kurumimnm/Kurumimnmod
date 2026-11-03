package io.github.kurumimnm.kurumimnmod;

import io.github.kurumimnm.kurumimnmod.config.ModClientConfigs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * クライアント専用のエントリーポイント。
 * <p>
 * dist = Dist.CLIENT なので専用サーバーではロードされない。
 * レンダラー登録（GeckoLib のエンティティレンダラー等）は今後ここ、
 * もしくは client パッケージ配下に追加していく。
 */
@Mod(value = Kurumimnmod.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID, value = Dist.CLIENT)
public final class KurumimnmodClient {

    public KurumimnmodClient(ModContainer container) {
        // MOD 一覧画面の「設定」ボタンから Config を GUI で編集できるようにする。
        // 項目名の翻訳は lang ファイルの "kurumimnmod.configuration.*" に定義する。
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        // クライアント専用の設定（HUD・敵 HP バーの表示設定）
        ModClientConfigs.register(container);

        // HUD（ModHudLayers / EnemyHealthBarRenderer）とキー割り当て（ModKeyMappings）は
        // @EventBusSubscriber で自動登録される。
    }

    /** クライアント側のセットアップ処理。 */
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        Kurumimnmod.LOGGER.info("[{}] client setup", Kurumimnmod.MOD_ID);
    }
}
