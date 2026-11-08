package io.github.kurumimnm.kurumimnmod.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * クエスト用ディメンションの「区画」。
 * <p>
 * フィールド 1 つ＝区画 1 つ（正方形）。区画は {@link ModConstants#QUEST_PLOT_SPACING} ブロックおきの格子に並び、
 * 区画と区画の間は何もない（落ちると奈落）。クエストを受けてフィールドに初めて入るたびに新しい区画を割り当て、
 * そのとき決めた乱数（seed）で地形を作るので、<b>同じフィールドでも受けるたびに地形が変わる</b>。
 * <ul>
 *   <li>格子 (0, 0)：中間拠点（全クエスト共通。地形は固定）</li>
 *   <li>格子 (gx, gz≧1)：フィールド。番号 index（1 から）を順に並べる</li>
 * </ul>
 * 区画の一覧は {@link QuestPlotData}（ワールドに保存）が持ち、ここには生成中に引けるよう写しを置く。
 * 地形（{@link QuestHeightFunction}）・バイオーム（{@link QuestPlotBiomeSource}）・建物（{@link QuestPlotFeature}）は
 * 生成のスレッドから呼ばれるので、ここは状態を変えない読み取りだけにしている。
 */
public final class QuestPlots {

    private QuestPlots() {}

    /** 何もない所の「地面の高さ」（これより上はすべて空気になる）。 */
    public static final double VOID = -1000;

    /** 中間拠点のバイオーム。 */
    public static final Identifier HUB_BIOME = Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "quest_hub");

    /**
     * 区画 1 つ。
     *
     * @param index   番号（1 から。格子の位置が決まる）
     * @param quest   クエストの ID
     * @param field   フィールドの ID
     * @param seed    地形の乱数
     * @param size    一辺（ブロック）
     * @param biome   バイオーム
     * @param terrain 地形の作り方
     */
    public record Plot(int index, Identifier quest, String field, long seed, int size, Identifier biome,
                       QuestDefinition.Terrain terrain) {

        public static final Codec<Plot> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("index").forGetter(Plot::index),
                Identifier.CODEC.fieldOf("quest").forGetter(Plot::quest),
                Codec.STRING.fieldOf("field").forGetter(Plot::field),
                Codec.LONG.fieldOf("seed").forGetter(Plot::seed),
                Codec.INT.fieldOf("size").forGetter(Plot::size),
                Identifier.CODEC.fieldOf("biome").forGetter(Plot::biome),
                QuestDefinition.Terrain.CODEC.fieldOf("terrain").forGetter(Plot::terrain)
        ).apply(i, Plot::new));

        public int centerX() {
            return gridX(index) * ModConstants.QUEST_PLOT_SPACING;
        }

        public int centerZ() {
            return gridZ(index) * ModConstants.QUEST_PLOT_SPACING;
        }

        public int half() {
            return size / 2;
        }

        /** 区画の中（正方形の内側）か。 */
        public boolean contains(double x, double z) {
            return Math.abs(x - centerX()) < half() && Math.abs(z - centerZ()) < half();
        }

        /** 入口（南の端の真ん中。転移して来る場所）の地面の上。 */
        public BlockPos entrance() {
            return new BlockPos(centerX(), terrain.base() + 1, centerZ() + half() - ModConstants.QUEST_GATE_INSET);
        }

        /** 次へ進むゲート（北の端の真ん中）の地面の上。ボス部屋は真ん中の北寄り。 */
        public BlockPos exit() {
            int inset = isArena() ? half() / 2 : ModConstants.QUEST_GATE_INSET;
            return new BlockPos(centerX(), terrain.base() + 1, centerZ() - half() + inset);
        }

        public boolean isArena() {
            return terrain.style().equals(QuestDefinition.Terrain.ARENA);
        }
    }

    // =====================================================================
    // 格子
    // =====================================================================

    static int gridX(int index) {
        return (index - 1) % ModConstants.QUEST_PLOTS_PER_ROW;
    }

    static int gridZ(int index) {
        return (index - 1) / ModConstants.QUEST_PLOTS_PER_ROW + 1;
    }

    private static long key(int gx, int gz) {
        return ((long) gx << 32) ^ (gz & 0xFFFFFFFFL);
    }

    /** 生成中に引ける区画の写し（格子の位置 → 区画）。 */
    private static final Map<Long, Plot> BY_GRID = new ConcurrentHashMap<>();

    static void put(Plot plot) {
        BY_GRID.put(key(gridX(plot.index()), gridZ(plot.index())), plot);
    }

    static void clear() {
        BY_GRID.clear();
    }

    /** その地点がある区画（なければ null。中間拠点も null）。 */
    public static @Nullable Plot at(double x, double z) {
        int spacing = ModConstants.QUEST_PLOT_SPACING;
        int gx = Math.floorDiv((int) Math.floor(x) + spacing / 2, spacing);
        int gz = Math.floorDiv((int) Math.floor(z) + spacing / 2, spacing);
        if (gz < 1) {
            return null;
        }
        Plot plot = BY_GRID.get(key(gx, gz));
        return plot != null && plot.contains(x, z) ? plot : null;
    }

    // =====================================================================
    // 中間拠点（格子 (0, 0)）
    // =====================================================================

    /** 中間拠点の中（円形の島）か。 */
    public static boolean inHub(double x, double z) {
        double r = ModConstants.QUEST_HUB_RADIUS;
        return x * x + z * z <= r * r;
    }

    /** 中間拠点に着く場所（広場の南）。 */
    public static BlockPos hubArrival() {
        return new BlockPos(0, ModConstants.QUEST_HUB_GROUND_Y + 1, 14);
    }

    /** 中間拠点の出発ゲート（広場の北）。 */
    public static BlockPos hubGate() {
        return new BlockPos(0, ModConstants.QUEST_HUB_GROUND_Y + 1, -14);
    }

    // =====================================================================
    // 地形・バイオーム
    // =====================================================================

    /** 地面の高さ（その柱でいちばん上の地面の Y）。何もない所は {@link #VOID}。 */
    public static double height(double x, double z) {
        if (inHub(x, z)) {
            return ModConstants.QUEST_HUB_GROUND_Y;
        }
        Plot plot = at(x, z);
        return plot == null ? VOID : QuestTerrain.height(plot, x - plot.centerX(), z - plot.centerZ());
    }

    /** バイオーム（null = 何もない所）。 */
    public static @Nullable Identifier biome(double x, double z) {
        if (inHub(x, z)) {
            return HUB_BIOME;
        }
        Plot plot = at(x, z);
        return plot == null ? null : plot.biome();
    }
}
