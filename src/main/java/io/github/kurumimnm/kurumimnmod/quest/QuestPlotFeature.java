package io.github.kurumimnm.kurumimnmod.quest;

import com.mojang.serialization.MapCodec;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.registry.ModBlocks;
import io.github.kurumimnm.kurumimnmod.world.town.TownFeature;
import io.github.kurumimnm.kurumimnmod.world.town.TownSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * クエスト用ディメンションの建物を置く地物（クエストのバイオームの top_layer_modification の最後）。
 * <ul>
 *   <li>フィールド：縁の見えない壁（バリアブロック）、入口の足場、出口のゲート（{@link QuestGateBlock}）</li>
 *   <li>ボス部屋：石の床と円形の壁、かがり火</li>
 *   <li>中間拠点：街（{@link io.github.kurumimnm.kurumimnmod.world.town.TownPlanner} の hub）、出発ゲート、島の縁の見えない壁</li>
 * </ul>
 * <pre>{@code data/kurumimnmod/worldgen/feature/quest_plot.json: { "type": "kurumimnmod:quest_plot" } }</pre>
 */
public final class QuestPlotFeature implements Feature {

    public static final QuestPlotFeature INSTANCE = new QuestPlotFeature();
    public static final MapCodec<QuestPlotFeature> CODEC = MapCodec.unit(INSTANCE);

    /** 中間拠点の街。 */
    public static final TownSpec HUB_TOWN = new TownSpec("quest_hub", 0, 0, ModConstants.QUEST_HUB_TOWN_RADIUS, "hub",
            "medieval", ModConstants.QUEST_HUB_GROUND_Y);

    private QuestPlotFeature() {}

    @Override
    public MapCodec<QuestPlotFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        ChunkPos chunk = ChunkPos.containing(origin);
        Writer w = new Writer(level, chunk);
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        int hubReach = ModConstants.QUEST_HUB_RADIUS + 2;
        if (Math.abs(minX + 8) <= hubReach + 8 && Math.abs(minZ + 8) <= hubReach + 8) {
            hub(w);
            return true;
        }
        QuestPlots.Plot plot = QuestPlots.at(minX + 8, minZ + 8);
        if (plot == null) {
            // チャンクの真ん中が区画の外でも、端が区画にかかっていることがある
            plot = QuestPlots.at(minX, minZ);
            if (plot == null) plot = QuestPlots.at(minX + 15, minZ + 15);
            if (plot == null) plot = QuestPlots.at(minX, minZ + 15);
            if (plot == null) plot = QuestPlots.at(minX + 15, minZ);
            if (plot == null) {
                return false;
            }
        }
        field(w, plot);
        return true;
    }

    // =====================================================================
    // フィールド
    // =====================================================================

    private static void field(Writer w, QuestPlots.Plot plot) {
        int half = plot.half();
        int cx = plot.centerX();
        int cz = plot.centerZ();
        BlockState barrier = Blocks.BARRIER.defaultBlockState();
        int top = w.level.getMaxY();
        // 縁の見えない壁（区画のいちばん外側の 1 列。地面の上から世界の上まで）
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = w.minX + dx;
                int z = w.minZ + dz;
                int u = x - cx;
                int v = z - cz;
                if (!plot.contains(x, z)) {
                    continue;
                }
                boolean border = Math.abs(u) >= half - 1 || Math.abs(v) >= half - 1;
                if (border) {
                    int ground = (int) Math.floor(QuestPlots.height(x + 0.5, z + 0.5));
                    for (int y = ground + 1; y <= top; y++) {
                        w.set(x, y, z, barrier);
                    }
                }
            }
        }
        // ボス部屋
        if (plot.isArena()) {
            arena(w, plot);
        }
        // 入口の足場
        BlockPos in = plot.entrance();
        disc(w, in.getX(), in.getY() - 1, in.getZ(), 3, Blocks.POLISHED_ANDESITE.defaultBlockState());
        // 出口のゲート
        gate(w, plot.exit());
    }

    /** ボス部屋：石の床（円）と、まわりの壁、かがり火。 */
    private static void arena(Writer w, QuestPlots.Plot plot) {
        int cx = plot.centerX();
        int cz = plot.centerZ();
        int y = plot.terrain().base();
        int r = plot.half() - ModConstants.QUEST_ARENA_MARGIN;
        BlockState floorA = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState floorB = Blocks.POLISHED_ANDESITE.defaultBlockState();
        BlockState wall = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = w.minX + dx;
                int z = w.minZ + dz;
                double d = Math.hypot(x - cx, z - cz);
                if (d <= r) {
                    // 床：同心円の模様
                    w.set(x, y, z, ((int) d / 4) % 2 == 0 ? floorA : floorB);
                } else if (d <= r + 2) {
                    for (int h = 1; h <= ModConstants.QUEST_ARENA_WALL_HEIGHT; h++) {
                        w.set(x, y + h, z, wall);
                    }
                }
            }
        }
        // かがり火（東西南北の壁ぎわ）
        BlockState fire = Blocks.CAMPFIRE.defaultBlockState().trySetValue(CampfireBlock.LIT, true);
        int[][] points = {{r - 3, 0}, {-r + 3, 0}, {0, r - 3}, {0, -r + 3}};
        for (int[] p : points) {
            w.set(cx + p[0], y + 1, cz + p[1], fire);
        }
    }

    // =====================================================================
    // 中間拠点
    // =====================================================================

    private static void hub(Writer w) {
        TownFeature.place(w.level, w.chunk, HUB_TOWN);
        BlockState barrier = Blocks.BARRIER.defaultBlockState();
        int r = ModConstants.QUEST_HUB_RADIUS;
        int top = w.level.getMaxY();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = w.minX + dx;
                int z = w.minZ + dz;
                double d = Math.hypot(x + 0.5, z + 0.5);
                if (d <= r && d > r - 1.5) {
                    for (int y = ModConstants.QUEST_HUB_GROUND_Y + 1; y <= top; y++) {
                        w.set(x, y, z, barrier);
                    }
                }
            }
        }
        gate(w, QuestPlots.hubGate());
    }

    // =====================================================================
    // 部品
    // =====================================================================

    /** ゲート：黒い石の台座、真ん中にゲートのブロック、石のアーチ。pos = ゲートのブロックの位置。 */
    private static void gate(Writer w, BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        disc(w, x, y - 1, z, 3, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
        for (int yy = y; yy <= y + 6; yy++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    w.set(x + dx, yy, z + dz, Blocks.AIR.defaultBlockState());
                }
            }
        }
        w.set(x, y, z, ModBlocks.QUEST_GATE.get().defaultBlockState());
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
        for (int yy = y; yy <= y + 4; yy++) {
            w.set(x - 3, yy, z, yy == y + 4 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : stone);
            w.set(x + 3, yy, z, yy == y + 4 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : stone);
        }
        for (int dx = -3; dx <= 3; dx++) {
            w.set(x + dx, y + 5, z, stone);
        }
        w.set(x, y + 4, z, Blocks.LANTERN.defaultBlockState().trySetValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
    }

    private static void disc(Writer w, int cx, int y, int cz, int r, BlockState state) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz <= r * r + 1) {
                    w.set(cx + dx, y, cz + dz, state);
                }
            }
        }
    }

    /** 今のチャンクの中だけにブロックを置く（ほかのチャンクの分は、そのチャンクの生成のときに置く）。 */
    private static final class Writer {
        private final WorldGenLevel level;
        private final ChunkPos chunk;
        private final int minX;
        private final int minZ;
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        Writer(WorldGenLevel level, ChunkPos chunk) {
            this.level = level;
            this.chunk = chunk;
            this.minX = chunk.getMinBlockX();
            this.minZ = chunk.getMinBlockZ();
        }

        void set(int x, int y, int z, BlockState state) {
            if (x < minX || x > minX + 15 || z < minZ || z > minZ + 15 || y < level.getMinY() || y > level.getMaxY()) {
                return;
            }
            pos.set(x, y, z);
            level.setBlock(pos, state, 2);
        }
    }
}
