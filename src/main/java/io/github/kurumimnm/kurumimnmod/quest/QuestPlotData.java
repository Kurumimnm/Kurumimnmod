package io.github.kurumimnm.kurumimnmod.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * 割り当てた区画の一覧（ワールドの data/kurumimnmod_quest_plots.dat に保存）。
 * 読み込んだとき・割り当てたときに {@link QuestPlots} へ写す（生成中に引けるように）。
 */
public final class QuestPlotData extends SavedData {

    public static final Codec<QuestPlotData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("next_index").forGetter(d -> d.nextIndex),
            QuestPlots.Plot.CODEC.listOf().fieldOf("plots").forGetter(d -> d.plots)
    ).apply(i, QuestPlotData::new));

    public static final SavedDataType<QuestPlotData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "quest_plots"), QuestPlotData::new, CODEC);

    private int nextIndex;
    private final List<QuestPlots.Plot> plots;

    public QuestPlotData() {
        this(1, List.of());
    }

    private QuestPlotData(int nextIndex, List<QuestPlots.Plot> plots) {
        this.nextIndex = nextIndex;
        this.plots = new ArrayList<>(plots);
    }

    /** サーバーの区画データ（オーバーワールドに保存）。 */
    public static QuestPlotData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** 保存されている区画を {@link QuestPlots} に写す（サーバー起動時）。 */
    void publish() {
        QuestPlots.clear();
        plots.forEach(QuestPlots::put);
    }

    public Optional<QuestPlots.Plot> plot(int index) {
        return plots.stream().filter(p -> p.index() == index).findFirst();
    }

    /** 新しい区画を割り当てる（seed = 地形の乱数）。 */
    public QuestPlots.Plot allocate(Identifier quest, String fieldId, QuestDefinition.Field field, long seed) {
        int size = Mth.clamp(field.size(), ModConstants.QUEST_FIELD_MIN_SIZE, ModConstants.QUEST_FIELD_MAX_SIZE);
        QuestPlots.Plot plot = new QuestPlots.Plot(nextIndex++, quest, fieldId, seed, size, field.biome(), field.terrain());
        plots.add(plot);
        QuestPlots.put(plot);
        setDirty();
        return plot;
    }
}
