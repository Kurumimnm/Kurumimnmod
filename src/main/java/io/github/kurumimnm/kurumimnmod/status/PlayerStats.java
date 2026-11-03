package io.github.kurumimnm.kurumimnmod.status;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * プレイヤーが振り分けたステータスポイント（プレイヤーにアタッチして保存・同期するデータ）。
 * <p>
 * 不変（record）。変更は {@link StatManager} 経由で新しいインスタンスを setData する。
 * 未使用ポイントは保存せず「レベルから計算した合計 − 振り分け済み」で求める
 * （Config でポイント数を変えても整合が取れるようにするため）。
 *
 * @param allocated 項目ごとの振り分け済みポイント（0 の項目は省略されることがある）
 */
public record PlayerStats(Map<AllocType, Integer> allocated) {

    public static final PlayerStats DEFAULT = new PlayerStats(Map.of());

    /** 項目 → ポイント数 の Map（通信用）。振り分けパケットでも共用する。 */
    public static final StreamCodec<ByteBuf, Map<AllocType, Integer>> POINTS_STREAM_CODEC =
            ByteBufCodecs.<ByteBuf, AllocType, Integer, Map<AllocType, Integer>>map(HashMap::new, AllocType.STREAM_CODEC, ByteBufCodecs.VAR_INT);

    /** セーブデータ（NBT）用。 */
    public static final MapCodec<PlayerStats> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.unboundedMap(AllocType.CODEC, Codec.INT).optionalFieldOf("allocated", Map.of()).forGetter(PlayerStats::allocated)
    ).apply(instance, PlayerStats::new));

    /** サーバー → クライアント同期用。 */
    public static final StreamCodec<ByteBuf, PlayerStats> STREAM_CODEC =
            POINTS_STREAM_CODEC.map(PlayerStats::new, PlayerStats::allocated);

    public PlayerStats {
        allocated = Map.copyOf(allocated);
    }

    /** 項目に振り分けたポイント数。 */
    public int points(AllocType type) {
        return allocated.getOrDefault(type, 0);
    }

    /** 振り分け済みポイントの合計。 */
    public int totalPoints() {
        int sum = 0;
        for (int v : allocated.values()) {
            sum += v;
        }
        return sum;
    }

    /** ポイントを加算した新しいインスタンスを返す。 */
    public PlayerStats plus(Map<AllocType, Integer> add) {
        Map<AllocType, Integer> map = new EnumMap<>(AllocType.class);
        map.putAll(allocated);
        add.forEach((type, n) -> map.merge(type, n, Integer::sum));
        return new PlayerStats(map);
    }
}
