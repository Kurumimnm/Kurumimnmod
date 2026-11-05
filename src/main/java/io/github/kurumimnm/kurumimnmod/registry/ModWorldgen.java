package io.github.kurumimnm.kurumimnmod.registry;

import com.mojang.serialization.MapCodec;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.world.FieldMap;
import io.github.kurumimnm.kurumimnmod.world.FieldMapBiomeSource;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewDatapackRegistryEvent;

/**
 * ワールド生成まわりの登録クラス。
 * <ul>
 *   <li>BiomeSource の種類 {@code kurumimnmod:field_map}（{@link FieldMapBiomeSource}）</li>
 *   <li>データパックのレジストリ {@code kurumimnmod:field_map}（地図 JSON の置き場所）
 *       … {@code data/<名前空間>/kurumimnmod/field_map/<名前>.json}</li>
 * </ul>
 * バイオーム・地形・ワールドプリセット自体は JSON（src/main/resources/data）で定義している。
 */
public final class ModWorldgen {

    private ModWorldgen() {}

    /** 地図（FieldMap）のレジストリ。 */
    public static final ResourceKey<Registry<FieldMap>> FIELD_MAP =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "field_map"));

    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, Kurumimnmod.MOD_ID);

    /** フィールドマップに従ってバイオームを決める BiomeSource。 */
    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<FieldMapBiomeSource>> FIELD_MAP_BIOME_SOURCE =
            BIOME_SOURCES.register("field_map", () -> FieldMapBiomeSource.CODEC);

    public static void register(IEventBus modEventBus) {
        BIOME_SOURCES.register(modEventBus);
        modEventBus.addListener(ModWorldgen::onNewDatapackRegistry);
    }

    /**
     * 地図のレジストリを「ワールド用のデータパックレジストリ」として登録する。
     * サーバー側だけで使うので、クライアントへの同期はしない。
     */
    private static void onNewDatapackRegistry(NewDatapackRegistryEvent event) {
        event.worldRegistry(FIELD_MAP, FieldMap.DIRECT_CODEC);
    }
}
