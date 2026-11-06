package io.github.kurumimnm.kurumimnmod.world;

import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.Mth;

/**
 * 「1 ブロックの柱ごとの場所（街・フィールド・エリア）」のキャッシュ。
 * <p>
 * ボーダーの当たり判定は、すべての生き物が動くたびに何度も呼ばれる。毎回 {@link FieldMap#regionAt} を計算すると重いので、
 * チャンク（16×16 列）単位でまとめて計算して覚えておく。
 * <p>
 * 柱の場所は「柱の中心 (x+0.5, z+0.5)」で決める。当たり判定・壁の表示・入った判定のすべてをこの柱単位で揃えるので、
 * 壁はブロックの境目にきっちり立つ。
 * <p>
 * 地図は 1 つのワールドに 1 つなので、キャッシュは地図ごと（同じ地図の中身かどうかは参照で比べる）に最大数個だけ持つ。
 */
public final class RegionGrid {

    private RegionGrid() {}

    private record Entry(FieldMap map, ConcurrentHashMap<Long, FieldRegion[]> chunks) {}

    /** 地図ごとのキャッシュ（サーバーとクライアントで地図の実体が別なので、普通は 1〜2 個）。 */
    private static volatile Entry[] entries = new Entry[0];

    /** ブロックの柱 (blockX, blockZ) の場所。 */
    public static FieldRegion at(FieldMap map, int blockX, int blockZ) {
        FieldRegion[] chunk = chunk(map, blockX >> 4, blockZ >> 4);
        return chunk[((blockZ & 15) << 4) | (blockX & 15)];
    }

    /** 座標 (x, z) を含む柱の場所。 */
    public static FieldRegion at(FieldMap map, double x, double z) {
        return at(map, Mth.floor(x), Mth.floor(z));
    }

    private static FieldRegion[] chunk(FieldMap map, int chunkX, int chunkZ) {
        ConcurrentHashMap<Long, FieldRegion[]> chunks = chunks(map);
        long key = ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
        FieldRegion[] cached = chunks.get(key);
        if (cached != null) {
            return cached;
        }
        FieldRegion[] computed = new FieldRegion[256];
        int x0 = chunkX << 4;
        int z0 = chunkZ << 4;
        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                computed[(dz << 4) | dx] = map.regionAt(x0 + dx + 0.5, z0 + dz + 0.5);
            }
        }
        if (chunks.size() >= ModConstants.REGION_CACHE_MAX_CHUNKS) {
            chunks.clear(); // 溢れたら全部捨てて作り直す（めったに起きない）
        }
        chunks.put(key, computed);
        return computed;
    }

    private static ConcurrentHashMap<Long, FieldRegion[]> chunks(FieldMap map) {
        Entry[] current = entries;
        for (Entry entry : current) {
            if (entry.map() == map) {
                return entry.chunks();
            }
        }
        synchronized (RegionGrid.class) {
            current = entries;
            for (Entry entry : current) {
                if (entry.map() == map) {
                    return entry.chunks();
                }
            }
            // 新しい地図。古いものは最大 3 個まで残す（ワールドを開き直すと地図の実体が変わる）
            int keep = Math.min(current.length, 3);
            Entry[] next = new Entry[keep + 1];
            System.arraycopy(current, current.length - keep, next, 0, keep);
            Entry created = new Entry(map, new ConcurrentHashMap<>());
            next[keep] = created;
            entries = next;
            return created.chunks();
        }
    }
}
