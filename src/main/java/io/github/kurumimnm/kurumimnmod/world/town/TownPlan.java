package io.github.kurumimnm.kurumimnmod.world.town;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1 つの街の「設計図」。置くブロックの一覧を、チャンクごとに分けて持つ。
 * <p>
 * 街はいくつものチャンクにまたがるので、ワールド生成ではチャンクごとに
 * 「そのチャンクに入るブロックだけ」を置く（{@link TownFeature}）。
 * 設計図は街ごとに 1 回だけ作って使い回す（同じ街なら毎回同じ設計図になる）。
 */
public final class TownPlan {

    /** 1 個のブロック。 */
    public record Entry(int x, int y, int z, BlockState state) {}

    /** 片付けをする柱（地面より上の木・草などを取り除く）。 */
    public record Column(int x, int z) {}

    private final int groundY;
    private final Map<Long, List<Entry>> entriesByChunk;
    private final Map<Long, List<Column>> clearByChunk;

    private TownPlan(int groundY, Map<Long, List<Entry>> entriesByChunk, Map<Long, List<Column>> clearByChunk) {
        this.groundY = groundY;
        this.entriesByChunk = entriesByChunk;
        this.clearByChunk = clearByChunk;
    }

    /** 街の地面の高さ（地面の一番上のブロックの Y）。 */
    public int groundY() {
        return groundY;
    }

    public List<Entry> entries(ChunkPos chunk) {
        return entriesByChunk.getOrDefault(chunk.pack(), List.of());
    }

    public List<Column> clearColumns(ChunkPos chunk) {
        return clearByChunk.getOrDefault(chunk.pack(), List.of());
    }

    /** 設計図を組み立てる道具。後から置いたブロックが前のブロックを上書きする。 */
    public static final class Builder {
        private final int groundY;
        private final Map<Long, BlockState> blocks = new LinkedHashMap<>();
        private final Map<Long, List<Column>> clear = new HashMap<>();

        public Builder(int groundY) {
            this.groundY = groundY;
        }

        public int groundY() {
            return groundY;
        }

        public void set(int x, int y, int z, BlockState state) {
            blocks.put(BlockPos.asLong(x, y, z), state);
        }

        /** 直方体を埋める（両端を含む）。 */
        public void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
            for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
                for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                    for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                        set(x, y, z, state);
                    }
                }
            }
        }

        /** その柱の、地面より上を片付ける。 */
        public void clearColumn(int x, int z) {
            clear.computeIfAbsent(ChunkPos.pack(x >> 4, z >> 4), k -> new ArrayList<>()).add(new Column(x, z));
        }

        public TownPlan build() {
            Map<Long, List<Entry>> byChunk = new HashMap<>();
            for (Map.Entry<Long, BlockState> e : blocks.entrySet()) {
                int x = BlockPos.getX(e.getKey());
                int y = BlockPos.getY(e.getKey());
                int z = BlockPos.getZ(e.getKey());
                byChunk.computeIfAbsent(ChunkPos.pack(x >> 4, z >> 4), k -> new ArrayList<>())
                        .add(new Entry(x, y, z, e.getValue()));
            }
            return new TownPlan(groundY, byChunk, clear);
        }
    }
}
