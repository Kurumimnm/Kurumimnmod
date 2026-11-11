package io.github.kurumimnm.kurumimnmod.job;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NewDatapackRegistryEvent;

/**
 * 職業のレジストリ {@code kurumimnmod:job}（データパック。JSON を足すだけで職業が増える）。
 * クライアントでも選択画面に一覧を出すので同期する。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID)
public final class JobRegistry {

    private JobRegistry() {}

    public static final ResourceKey<Registry<JobDefinition>> JOB =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "job"));

    @SubscribeEvent
    static void onNewDatapackRegistry(NewDatapackRegistryEvent event) {
        event.worldRegistry(JOB, JobDefinition.CODEC, JobDefinition.CODEC);
    }
}
