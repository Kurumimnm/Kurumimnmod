package io.github.kurumimnm.kurumimnmod.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * プレイヤーの探索の記録（プレイヤーに付けるデータ。保存され、本人のクライアントに同期される）。
 *
 * @param towns         行ったことのある街の id
 * @param fields        行ったことのあるフィールドの番号
 * @param areas         行ったことのあるエリア（フィールド番号 × 100 ＋ エリア番号）。地図はこの単位で解放される
 * @param flags         持っている進行フラグ（gate など。実績 kurumimnmod:progress/〇〇 と連動）
 */
public record PlayerExploration(Set<String> towns, Set<Integer> fields, Set<Integer> areas, Set<String> flags) {

    public static final PlayerExploration EMPTY = new PlayerExploration(Set.of(), Set.of(), Set.of(), Set.of());

    public PlayerExploration {
        towns = Set.copyOf(towns);
        fields = Set.copyOf(fields);
        areas = Set.copyOf(areas);
        flags = Set.copyOf(flags);
    }

    public static final Codec<PlayerExploration> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf).optionalFieldOf("towns", Set.of()).forGetter(PlayerExploration::towns),
            Codec.INT.listOf().xmap(Set::copyOf, List::copyOf).optionalFieldOf("fields", Set.of()).forGetter(PlayerExploration::fields),
            Codec.INT.listOf().xmap(Set::copyOf, List::copyOf).optionalFieldOf("areas", Set.of()).forGetter(PlayerExploration::areas),
            Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf).optionalFieldOf("flags", Set.of()).forGetter(PlayerExploration::flags)
    ).apply(i, PlayerExploration::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerExploration> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.collection(HashSet::new)), e -> new HashSet<>(e.towns()),
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.collection(HashSet::new)), e -> new HashSet<>(e.fields()),
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.collection(HashSet::new)), e -> new HashSet<>(e.areas()),
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.collection(HashSet::new)), e -> new HashSet<>(e.flags()),
            PlayerExploration::new);

    public boolean visited(FieldRegion region) {
        return region.isTown() ? towns.contains(region.town()) : areas.contains(region.areaKey());
    }

    /** その場所を訪れた記録を足したもの（変化がなければ自分自身）。 */
    public PlayerExploration visit(FieldRegion region) {
        if (region.isTown()) {
            if (towns.contains(region.town())) {
                return this;
            }
            Set<String> newTowns = new HashSet<>(towns);
            newTowns.add(region.town());
            return new PlayerExploration(newTowns, fields, areas, flags);
        }
        if (fields.contains(region.field()) && areas.contains(region.areaKey())) {
            return this;
        }
        Set<Integer> newFields = new HashSet<>(fields);
        newFields.add(region.field());
        Set<Integer> newAreas = new HashSet<>(areas);
        newAreas.add(region.areaKey());
        return new PlayerExploration(towns, newFields, newAreas, flags);
    }

    public PlayerExploration withFlags(Set<String> newFlags) {
        return new PlayerExploration(towns, fields, areas, newFlags);
    }

    public PlayerExploration withVisited(Set<String> newTowns, Set<Integer> newFields, Set<Integer> newAreas) {
        return new PlayerExploration(newTowns, newFields, newAreas, flags);
    }
}
