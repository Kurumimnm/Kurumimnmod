package io.github.kurumimnm.kurumimnmod.registry;

import com.mojang.serialization.MapCodec;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.world.FieldMap;
import io.github.kurumimnm.kurumimnmod.world.FieldMapBiomeSource;
import io.github.kurumimnm.kurumimnmod.world.FieldRoadFeature;
import io.github.kurumimnm.kurumimnmod.world.TerrainHeightFunction;
import io.github.kurumimnm.kurumimnmod.world.town.TownFeature;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewDatapackRegistryEvent;

/**
 * ワールド生成まわりの登録クラス。
 * <ul>
 *   <li>BiomeSource の種類 {@code kurumimnmod:field_map}（{@link FieldMapBiomeSource}）</li>
 *   <li>地形の部品（density function）の種類 {@code kurumimnmod:terrain_height}（{@link TerrainHeightFunction}）</li>
 *   <li>地物（feature）の種類 {@code kurumimnmod:field_roads}（{@link FieldRoadFeature}・街道）</li>
 *   <li>地物（feature）の種類 {@code kurumimnmod:field_towns}（{@link TownFeature}・街）</li>
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

    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, Kurumimnmod.MOD_ID);

    /** 地面の高さ（このMODの地形はすべてこれで作る）。 */
    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<TerrainHeightFunction>> TERRAIN_HEIGHT =
            DENSITY_FUNCTION_TYPES.register("terrain_height", () -> TerrainHeightFunction.CODEC);

    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES =
            DeferredRegister.create(Registries.FEATURE_TYPE, Kurumimnmod.MOD_ID);

    /** 街道を敷く地物。 */
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<FieldRoadFeature>> FIELD_ROADS =
            FEATURE_TYPES.register("field_roads", () -> FieldRoadFeature.CODEC);

    /** 街を建てる地物。 */
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<TownFeature>> FIELD_TOWNS =
            FEATURE_TYPES.register("field_towns", () -> TownFeature.CODEC);

    public static void register(IEventBus modEventBus) {
        FEATURE_TYPES.register(modEventBus);
        BIOME_SOURCES.register(modEventBus);
        DENSITY_FUNCTION_TYPES.register(modEventBus);
        modEventBus.addListener(ModWorldgen::onNewDatapackRegistry);
    }

    /**
     * 地図のレジストリを「ワールド用のデータパックレジストリ」として登録する。
     * クライアントでも地図を使う（ゲートの壁・場所の名前・地図画面）ので、クライアントへ同期する。
     */
    private static void onNewDatapackRegistry(NewDatapackRegistryEvent event) {
        event.worldRegistry(FIELD_MAP, FieldMap.DIRECT_CODEC, FieldMap.DIRECT_CODEC);
    }
}
