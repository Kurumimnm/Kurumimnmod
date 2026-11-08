package io.github.kurumimnm.kurumimnmod.registry;

import com.mojang.serialization.MapCodec;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.quest.QuestDefinition;
import io.github.kurumimnm.kurumimnmod.quest.QuestHeightFunction;
import io.github.kurumimnm.kurumimnmod.quest.QuestPlotBiomeSource;
import io.github.kurumimnm.kurumimnmod.quest.QuestPlotFeature;
import io.github.kurumimnm.kurumimnmod.world.town.TownFeature;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.NewDatapackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * ワールド生成・クエストまわりの登録クラス。
 * <ul>
 *   <li>BiomeSource の種類 {@code kurumimnmod:quest_plots}（{@link QuestPlotBiomeSource}・クエストの区画のバイオーム）</li>
 *   <li>地形の部品（density function）の種類 {@code kurumimnmod:quest_height}（{@link QuestHeightFunction}・区画の地面の高さ）</li>
 *   <li>地物（feature）の種類 {@code kurumimnmod:town}（{@link TownFeature}・街）と
 *       {@code kurumimnmod:quest_plot}（{@link QuestPlotFeature}・ゲート・境界の壁・ボス部屋・中間拠点）</li>
 *   <li>データパックのレジストリ {@code kurumimnmod:quest}（クエストの JSON の置き場所）
 *       … {@code data/<名前空間>/kurumimnmod/quest/<名前>.json}</li>
 * </ul>
 * バイオーム・ディメンション・ワールドプリセット自体は JSON（src/main/resources/data）で定義している。
 */
public final class ModWorldgen {

    private ModWorldgen() {}

    /** クエストのレジストリ。 */
    public static final ResourceKey<Registry<QuestDefinition>> QUEST =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "quest"));

    /** クエスト用ディメンション（data/kurumimnmod/dimension/quest.json）。 */
    public static final ResourceKey<Level> QUEST_LEVEL =
            ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "quest"));

    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, Kurumimnmod.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<QuestPlotBiomeSource>> QUEST_PLOTS_BIOME_SOURCE =
            BIOME_SOURCES.register("quest_plots", () -> QuestPlotBiomeSource.CODEC);

    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, Kurumimnmod.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<QuestHeightFunction>> QUEST_HEIGHT =
            DENSITY_FUNCTION_TYPES.register("quest_height", () -> QuestHeightFunction.CODEC);

    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES =
            DeferredRegister.create(Registries.FEATURE_TYPE, Kurumimnmod.MOD_ID);

    /** 街を建てる地物。 */
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<TownFeature>> TOWN =
            FEATURE_TYPES.register("town", () -> TownFeature.CODEC);

    /** クエストの区画の建物（ゲート・境界の壁・ボス部屋・中間拠点）。 */
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<QuestPlotFeature>> QUEST_PLOT =
            FEATURE_TYPES.register("quest_plot", () -> QuestPlotFeature.CODEC);

    public static void register(IEventBus modEventBus) {
        FEATURE_TYPES.register(modEventBus);
        BIOME_SOURCES.register(modEventBus);
        DENSITY_FUNCTION_TYPES.register(modEventBus);
        modEventBus.addListener(ModWorldgen::onNewDatapackRegistry);
    }

    /** クエストのレジストリを登録する。クライアントでも名前・フィールドの一覧を使う（HUD・クエスト画面）ので同期する。 */
    private static void onNewDatapackRegistry(NewDatapackRegistryEvent event) {
        event.worldRegistry(QUEST, QuestDefinition.CODEC, QuestDefinition.CODEC);
    }
}
