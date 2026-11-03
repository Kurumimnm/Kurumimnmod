package io.github.kurumimnm.kurumimnmod.network;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 独自パケット（ペイロード）の登録。
 * <p>
 * プレイヤーデータの同期はデータアタッチメントの sync 機能で自動化しているので、
 * ここに置くのは「クライアントからの操作をサーバーへ送る」ものだけ。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class ModNetwork {

    private ModNetwork() {}

    /** 通信の互換バージョン（パケットの中身を変えたら上げる）。 */
    private static final String VERSION = "1";

    @SubscribeEvent
    static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(AllocateStatsPayload.TYPE, AllocateStatsPayload.STREAM_CODEC, AllocateStatsPayload::handle);
    }
}
