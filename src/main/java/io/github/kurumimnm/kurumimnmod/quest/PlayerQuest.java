package io.github.kurumimnm.kurumimnmod.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * プレイヤーのクエストの状態（データアタッチメント。保存・死んでも消えない・本人に同期）。
 *
 * @param quest     受けているクエスト（なし = empty）
 * @param location  今いる場所：{@link #NOWHERE}（クエストの外）／{@link #HUB}（中間拠点）／フィールドの ID
 * @param cleared   クリアしたフィールド
 * @param kills     今のフィールドで倒したモンスターの数（クリア条件 kill 用）
 * @param plots     このクエストで割り当てた区画（フィールドの ID → 区画の番号）。同じクエストの間は同じ地形
 * @param origin    クエストを受けた場所（辞退・達成したら戻る）
 * @param boss      出したボス（倒したらクリア）
 * @param completed 達成したクエスト
 */
public record PlayerQuest(Optional<Identifier> quest, String location, Set<String> cleared, int kills,
                          Map<String, Integer> plots, Optional<GlobalPos> origin, Optional<UUID> boss,
                          Set<Identifier> completed) {

    public static final String NOWHERE = "";
    public static final String HUB = "@hub";

    public static final PlayerQuest EMPTY = new PlayerQuest(Optional.empty(), NOWHERE, Set.of(), 0, Map.of(),
            Optional.empty(), Optional.empty(), Set.of());

    public static final Codec<PlayerQuest> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.optionalFieldOf("quest").forGetter(PlayerQuest::quest),
            Codec.STRING.optionalFieldOf("location", NOWHERE).forGetter(PlayerQuest::location),
            Codec.STRING.listOf().xmap(l -> (Set<String>) new HashSet<>(l), s -> List.copyOf(s))
                    .optionalFieldOf("cleared", Set.of()).forGetter(PlayerQuest::cleared),
            Codec.INT.optionalFieldOf("kills", 0).forGetter(PlayerQuest::kills),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("plots", Map.of()).forGetter(PlayerQuest::plots),
            GlobalPos.CODEC.optionalFieldOf("origin").forGetter(PlayerQuest::origin),
            UUIDUtil.CODEC.optionalFieldOf("boss").forGetter(PlayerQuest::boss),
            Identifier.CODEC.listOf().xmap(l -> (Set<Identifier>) new HashSet<>(l), s -> List.copyOf(s))
                    .optionalFieldOf("completed", Set.of()).forGetter(PlayerQuest::completed)
    ).apply(i, PlayerQuest::new));

    /** 本人のクライアントへ送る内容（HUD・クエスト画面に要るものだけ。区画・受けた場所・ボスは送らない）。 */
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerQuest> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), PlayerQuest::quest,
            ByteBufCodecs.STRING_UTF8, PlayerQuest::location,
            ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.STRING_UTF8), q -> new HashSet<>(q.cleared()),
            ByteBufCodecs.VAR_INT, PlayerQuest::kills,
            ByteBufCodecs.collection(HashSet::new, Identifier.STREAM_CODEC), q -> new HashSet<>(q.completed()),
            (quest, location, cleared, kills, completed) -> new PlayerQuest(quest, location, cleared, kills, Map.of(),
                    Optional.empty(), Optional.empty(), completed));

    public boolean active() {
        return quest.isPresent();
    }

    public boolean inHub() {
        return HUB.equals(location);
    }

    /** フィールドの中にいるか（中間拠点・クエストの外ではない）。 */
    public boolean inField() {
        return active() && !location.isEmpty() && !inHub();
    }

    // ---- 書き換えた写しを作る ----

    public PlayerQuest withLocation(String newLocation) {
        return new PlayerQuest(quest, newLocation, cleared, kills, plots, origin, boss, completed);
    }

    public PlayerQuest withKills(int newKills) {
        return new PlayerQuest(quest, location, cleared, newKills, plots, origin, boss, completed);
    }

    public PlayerQuest withCleared(String field) {
        Set<String> set = new HashSet<>(cleared);
        set.add(field);
        return new PlayerQuest(quest, location, Set.copyOf(set), kills, plots, origin, boss, completed);
    }

    public PlayerQuest withPlot(String field, int plot) {
        Map<String, Integer> map = new HashMap<>(plots);
        map.put(field, plot);
        return new PlayerQuest(quest, location, cleared, kills, Map.copyOf(map), origin, boss, completed);
    }

    public PlayerQuest withBoss(Optional<UUID> newBoss) {
        return new PlayerQuest(quest, location, cleared, kills, plots, origin, newBoss, completed);
    }

    /** クエストを受けた状態（場所はまだクエストの外）。 */
    public PlayerQuest start(Identifier id, GlobalPos from) {
        return new PlayerQuest(Optional.of(id), NOWHERE, Set.of(), 0, Map.of(), Optional.of(from), Optional.empty(), completed);
    }

    /** クエストを終えた状態（達成なら completed に足す）。 */
    public PlayerQuest finish(boolean success) {
        Set<Identifier> done = new HashSet<>(completed);
        if (success) {
            quest.ifPresent(done::add);
        }
        return new PlayerQuest(Optional.empty(), NOWHERE, Set.of(), 0, Map.of(), Optional.empty(), Optional.empty(), Set.copyOf(done));
    }
}
