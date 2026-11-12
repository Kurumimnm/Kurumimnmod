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
 * ここに置くのは「クライアントからの操作をサーバーへ送る」ものと、
 * 演出・画面表示のためにサーバーからクライアントへ一時的に送るもの（カカシのダメージ数字・設定画面）。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class ModNetwork {

    private ModNetwork() {}

    /** 通信の互換バージョン（パケットの中身を変えたら上げる）。 */
    private static final String VERSION = "6";

    @SubscribeEvent
    static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(AllocateStatsPayload.TYPE, AllocateStatsPayload.STREAM_CODEC, AllocateStatsPayload::handle);

        // ---- カカシ ----
        // クライアント → サーバー（設定画面の取得・保存・削除）
        registrar.playToServer(RequestScarecrowSettingsPayload.TYPE, RequestScarecrowSettingsPayload.STREAM_CODEC,
                RequestScarecrowSettingsPayload::handle);
        registrar.playToServer(UpdateScarecrowSettingsPayload.TYPE, UpdateScarecrowSettingsPayload.STREAM_CODEC,
                UpdateScarecrowSettingsPayload::handle);
        registrar.playToServer(DeleteScarecrowPayload.TYPE, DeleteScarecrowPayload.STREAM_CODEC,
                DeleteScarecrowPayload::handle);
        // サーバー → クライアント（受け取ったときの処理は client/scarecrow/ScarecrowClientPayloads で登録。
        // 専用サーバーでクライアント専用クラスを読み込まないよう、ここでは形式だけ登録する）
        registrar.playToClient(OpenScarecrowSettingsPayload.TYPE, OpenScarecrowSettingsPayload.STREAM_CODEC);
        registrar.playToClient(ScarecrowDamagePayload.TYPE, ScarecrowDamagePayload.STREAM_CODEC);


        registrar.playToServer(CastWandPayload.TYPE, CastWandPayload.STREAM_CODEC, CastWandPayload::handle);

        // ---- クエスト ----
        // クライアント → サーバー（クエスト画面の操作・ゲートの行き先）
        registrar.playToServer(QuestActionPayload.TYPE, QuestActionPayload.STREAM_CODEC, QuestActionPayload::handle);
        registrar.playToServer(ChooseGatePayload.TYPE, ChooseGatePayload.STREAM_CODEC, ChooseGatePayload::handle);
        // サーバー → クライアント（受け取ったときの処理は client/quest/ClientQuest で登録）
        registrar.playToClient(OpenGatePayload.TYPE, OpenGatePayload.STREAM_CODEC);
        // 最初の武器の選択（サーバー → クライアントで画面を開き、選んだらクライアント → サーバー）
        registrar.playToClient(OpenWeaponSelectPayload.TYPE, OpenWeaponSelectPayload.STREAM_CODEC);
        registrar.playToServer(ChooseWeaponPayload.TYPE, ChooseWeaponPayload.STREAM_CODEC, ChooseWeaponPayload::handle);

        // ---- 職業 ----
        registrar.playToServer(SelectJobPayload.TYPE, SelectJobPayload.STREAM_CODEC, SelectJobPayload::handle);
        // サーバー → クライアント（受け取ったときの処理は client/job/ClientJob で登録）
        registrar.playToClient(OpenJobSelectPayload.TYPE, OpenJobSelectPayload.STREAM_CODEC);
    }
}
