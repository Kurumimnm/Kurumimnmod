package io.github.kurumimnm.kurumimnmod.client.job;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.job.JobDefinition;
import io.github.kurumimnm.kurumimnmod.job.JobRegistry;
import io.github.kurumimnm.kurumimnmod.job.PlayerJob;
import io.github.kurumimnm.kurumimnmod.network.OpenJobSelectPayload;
import io.github.kurumimnm.kurumimnmod.registry.ModAttachments;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** 職業のクライアント側（選択画面を開くパケットの受け取り・職業の一覧）。 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID, value = Dist.CLIENT)
public final class ClientJob {

    private ClientJob() {}

    @SubscribeEvent
    static void onRegisterClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(OpenJobSelectPayload.TYPE, (payload, context) ->
                Minecraft.getInstance().gui.setScreen(new JobSelectScreen()));
    }

    /** 自分の職業の状態。 */
    public static PlayerJob state() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? PlayerJob.EMPTY : mc.player.getData(ModAttachments.JOB);
    }

    /** 職業の一覧（並び順 sort → ID）。 */
    public static List<Map.Entry<Identifier, JobDefinition>> all() {
        List<Map.Entry<Identifier, JobDefinition>> list = new ArrayList<>();
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.registryAccess().lookup(JobRegistry.JOB)
                    .ifPresent(r -> r.keySet().forEach(id -> list.add(Map.entry(id, r.getValue(id)))));
        }
        list.sort(Comparator.comparingInt((Map.Entry<Identifier, JobDefinition> e) -> e.getValue().sort())
                .thenComparing(e -> e.getKey().toString()));
        return list;
    }
}
