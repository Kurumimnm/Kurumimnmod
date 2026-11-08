package io.github.kurumimnm.kurumimnmod.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import java.util.ArrayList;
import java.util.List;
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

        /** 入口（南の端の真ん中。転移して来る場所）の地面の上。神殿は広間の南の壁ぎわ。 */
        public BlockPos entrance() {
            if (isTemple()) {
                return new BlockPos(centerX(), terrain.base() + 1,
                        centerZ() + terrain.hallLength() / 2 - ModConstants.QUEST_TEMPLE_ENTRANCE_INSET);
            }
            return new BlockPos(centerX(), terrain.base() + 1, centerZ() + half() - ModConstants.QUEST_GATE_INSET);
        }

        /** 次へ進むゲート（北の端の真ん中）の地面の上。闘技場は真ん中の北寄り、神殿は広間の北の壁ぎわ。 */
        public BlockPos exit() {
            if (isTemple()) {
                return new BlockPos(centerX(), terrain.base() + 1,
                        centerZ() - terrain.hallLength() / 2 + ModConstants.QUEST_TEMPLE_GATE_INSET);
            }
            int inset = isArena() ? half() / 2 : ModConstants.QUEST_GATE_INSET;
            return new BlockPos(centerX(), terrain.base() + 1, centerZ() - half() + inset);
        }

        /** ボス部屋（闘技場・神殿）か。 */
        public boolean isArena() {
            return terrain.isBossRoom();
        }

        /** 神殿（天井のある長方形のボス部屋）か。 */
        public boolean isTemple() {
            return terrain.style().equals(QuestDefinition.Terrain.TEMPLE);
        }

        /** 神殿の広間の内側（壁から margin ブロック以上内側）か。 */
        public boolean inHall(double x, double z, double margin) {
            return Math.abs(x - centerX()) <= terrain.hallWidth() / 2.0 - margin
                    && Math.abs(z - centerZ()) <= terrain.hallLength() / 2.0 - margin;
        }

        /** 神殿の天井（いちばん下の段）の Y。 */
        public int ceilingY() {
            return terrain.base() + 1 + terrain.pillarHeight();
        }

        /** 闘技場の床（円）の半径。 */
        public int arenaRadius() {
            return half() - ModConstants.QUEST_ARENA_MARGIN;
        }

        /**
         * ボス部屋の柱の位置（柱の中心の、床の 1 つ上のブロック）。ボス部屋でなければ空。
         * 柱は {@link QuestPlotFeature} が建て、ボスの AI（柱に飛び乗るスキルなど）もここから位置を引く。
         */
        public List<BlockPos> pillars() {
            int n = terrain.pillars();
            if (!isArena() || n <= 0) {
                return List.of();
            }
            if (isTemple()) {
                // 左右 2 列（西・東の壁から QUEST_TEMPLE_PILLAR_INSET の所）に半分ずつ、南北に等間隔
                List<BlockPos> list = new ArrayList<>(n);
                int west = n - n / 2;
                int east = n / 2;
                int xOff = terrain.hallWidth() / 2 - ModConstants.QUEST_TEMPLE_PILLAR_INSET;
                addRow(list, centerX() - xOff, west);
                addRow(list, centerX() + xOff, east);
                return list;
            }
            double ring = arenaRadius() * ModConstants.QUEST_PILLAR_RING_RATIO;
            double start = Math.toRadians(ModConstants.QUEST_PILLAR_START_ANGLE);
            List<BlockPos> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                double a = start + Math.PI * 2 * i / n;
                list.add(new BlockPos(centerX() + (int) Math.round(Math.cos(a) * ring), terrain.base() + 1,
                        centerZ() + (int) Math.round(Math.sin(a) * ring)));
            }
            return list;
        }

        /** 神殿の柱の 1 列（南北に等間隔。両端の壁との間も柱どうしの間と同じにする）。 */
        private void addRow(List<BlockPos> list, int x, int count) {
            int length = terrain.hallLength();
            for (int i = 0; i < count; i++) {
                int z = centerZ() - length / 2 + (int) Math.round(length * (i + 1) / (double) (count + 1));
                list.add(new BlockPos(x, terrain.base() + 1, z));
            }
        }

        /** 柱のてっぺんの Y（柱の上に立ったときの足もと。神殿では天井の下面と同じ高さ）。 */
        public int pillarTopY() {
            return terrain.base() + 1 + terrain.pillarHeight();
        }

        /** (x, z) が柱から margin ブロック以内か。 */
        public boolean nearPillar(double x, double z, double margin) {
            double reach = ModConstants.QUEST_PILLAR_HALF_WIDTH + margin;
            for (BlockPos p : pillars()) {
                if (Math.abs(x - p.getX()) <= reach && Math.abs(z - p.getZ()) <= reach) {
                    return true;
                }
            }
            return false;
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
