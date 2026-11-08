package io.github.kurumimnm.kurumimnmod.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;

/**
 * クエスト 1 つの設計（データパックのレジストリ {@code kurumimnmod:quest}）。
 * <pre>
 * data/&lt;名前空間&gt;/kurumimnmod/quest/&lt;名前&gt;.json
 * {
 *   "start": ["1"],                 … 最初に入れるフィールド（ふつうは 1 つ）
 *   "goal": "boss",                 … このフィールドをクリアするとクエスト達成
 *   "sort": 10,                     … クエスト一覧の並び順
 *   "reward_money": 100,            … 達成したときにもらえるお金（$）
 *   "fields": {
 *     "1":    { "number": 1, "size": 192, "biome": "kurumimnmod:quest_meadow",
 *               "terrain": { "style": "hills", "base": 70, "amplitude": 10, "scale": 90 },
 *               "clear": { "type": "kill", "count": 8 }, "next": ["boss"],
 *               "mobs": [{ "type": "minecraft:zombie", "weight": 3 }], "max_mobs": 8 },
 *     "boss": { "number": 2, "size": 96, "biome": "kurumimnmod:quest_arena", "terrain": { "style": "arena" },
 *               "clear": { "type": "boss" }, "boss": "minecraft:ravager", "requires": ["1"] }
 *   }
 * }
 * </pre>
 * 名前・説明は lang の {@code quest.<名前空間>.<名前>}／{@code ...description}／{@code ...field.<フィールドID>}。
 *
 * @param start       最初に入れるフィールド
 * @param goal        クリアするとクエスト達成になるフィールド（ふつうはボス部屋）
 * @param fields      フィールド（ID → 設定）
 * @param sort        一覧の並び順（小さいほど上）
 * @param rewardMoney 達成したときにもらえるお金
 */
public record QuestDefinition(List<String> start, String goal, Map<String, Field> fields, int sort, int rewardMoney) {

    public static final Codec<QuestDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().fieldOf("start").forGetter(QuestDefinition::start),
            Codec.STRING.fieldOf("goal").forGetter(QuestDefinition::goal),
            Codec.unboundedMap(Codec.STRING, Field.CODEC).fieldOf("fields").forGetter(QuestDefinition::fields),
            Codec.INT.optionalFieldOf("sort", 0).forGetter(QuestDefinition::sort),
            Codec.INT.optionalFieldOf("reward_money", 0).forGetter(QuestDefinition::rewardMoney)
    ).apply(i, QuestDefinition::new));

    public Optional<Field> field(String id) {
        return Optional.ofNullable(fields.get(id));
    }

    /** フィールド番号の順に並べた ID の一覧（画面の表示用）。 */
    public List<String> fieldOrder() {
        List<String> ids = new ArrayList<>(fields.keySet());
        ids.sort(Comparator.comparingInt((String id) -> fields.get(id).number()).thenComparing(id -> id));
        return ids;
    }

    /**
     * そのフィールドに今入れるか。条件：まだクリアしていない・必要なフィールド（requires）をすべてクリア済み・
     * 最初のフィールドか、クリアしたフィールドの「次」に入っている。
     */
    public boolean available(String id, Set<String> cleared) {
        Field f = fields.get(id);
        if (f == null || cleared.contains(id) || !cleared.containsAll(f.requires())) {
            return false;
        }
        if (start.contains(id)) {
            return true;
        }
        for (String c : cleared) {
            Field done = fields.get(c);
            if (done != null && done.next().contains(id)) {
                return true;
            }
        }
        return false;
    }

    /** 道筋としては次に来るが、条件（requires）がまだ足りないフィールドか（「条件あり」として表示する）。 */
    public boolean lockedByRequirement(String id, Set<String> cleared) {
        Field f = fields.get(id);
        return f != null && !cleared.contains(id) && !cleared.containsAll(f.requires());
    }

    // ---- 名前（lang のキー） ----

    public static String nameKey(Identifier id) {
        return "quest." + id.getNamespace() + "." + id.getPath();
    }

    public static String descriptionKey(Identifier id) {
        return nameKey(id) + ".description";
    }

    public static String fieldKey(Identifier id, String fieldId) {
        return nameKey(id) + ".field." + fieldId;
    }

    // =====================================================================

    /**
     * フィールド 1 つの設定。
     *
     * @param number  表示する番号（「フィールド 1」）
     * @param size    正方形の一辺（ブロック）。{@link ModConstants#QUEST_FIELD_MIN_SIZE}〜{@link ModConstants#QUEST_FIELD_MAX_SIZE}
     * @param biome   バイオーム（地表のブロック・草木・空の色）
     * @param terrain 地形の作り方
     * @param clear   クリア条件
     * @param next    クリアしたあとゲートから進めるフィールド
     * @param requires このフィールドに入るのに、先にクリアしておくフィールド（ボスと戦う条件など）
     * @param mobs    わくモブ（重み付き）
     * @param maxMobs フィールドにいるモブの上限（プレイヤー 1 人のまわり）
     * @param boss    ボス（ボス部屋に入ったときに 1 体出す）
     */
    public record Field(int number, int size, Identifier biome, Terrain terrain, Clear clear, List<String> next,
                        List<String> requires, List<Mob> mobs, int maxMobs, Optional<Identifier> boss) {

        public static final Codec<Field> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("number").forGetter(Field::number),
                Codec.intRange(ModConstants.QUEST_FIELD_MIN_SIZE, ModConstants.QUEST_FIELD_MAX_SIZE).fieldOf("size").forGetter(Field::size),
                Identifier.CODEC.fieldOf("biome").forGetter(Field::biome),
                Terrain.CODEC.optionalFieldOf("terrain", Terrain.DEFAULT).forGetter(Field::terrain),
                Clear.CODEC.optionalFieldOf("clear", Clear.REACH_GATE).forGetter(Field::clear),
                Codec.STRING.listOf().optionalFieldOf("next", List.of()).forGetter(Field::next),
                Codec.STRING.listOf().optionalFieldOf("requires", List.of()).forGetter(Field::requires),
                Mob.CODEC.listOf().optionalFieldOf("mobs", List.of()).forGetter(Field::mobs),
                Codec.INT.optionalFieldOf("max_mobs", 8).forGetter(Field::maxMobs),
                Identifier.CODEC.optionalFieldOf("boss").forGetter(Field::boss)
        ).apply(i, Field::new));

        public boolean isArena() {
            return terrain.style().equals(Terrain.ARENA);
        }
    }

    /**
     * 地形の作り方。
     *
     * @param style     plains（なだらか）／hills（丘）／mountains（険しい山）／dunes（砂丘）／arena（ボス部屋：平らな円形の闘技場）
     * @param base      地面の基準の高さ（Y）。入口とゲートのまわりはこの高さで平らになる
     * @param amplitude 起伏の大きさ（ブロック）
     * @param scale     起伏の横の大きさ（ブロック。大きいほどゆったり）
     */
    public record Terrain(String style, int base, double amplitude, double scale) {
        public static final String ARENA = "arena";
        public static final Terrain DEFAULT = new Terrain("hills", 70, 10, 90);

        public static final Codec<Terrain> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("style", "hills").forGetter(Terrain::style),
                Codec.INT.optionalFieldOf("base", 70).forGetter(Terrain::base),
                Codec.DOUBLE.optionalFieldOf("amplitude", 10.0).forGetter(Terrain::amplitude),
                Codec.DOUBLE.optionalFieldOf("scale", 90.0).forGetter(Terrain::scale)
        ).apply(i, Terrain::new));
    }

    /**
     * クリア条件（仮。種類は今後増やす）。
     *
     * @param type  reach（ゲートにたどり着く）／kill（このフィールドでモンスターを count 体倒す）／boss（ボスを倒す）
     * @param count 倒す数（kill のとき）
     */
    public record Clear(String type, int count) {
        public static final String REACH = "reach";
        public static final String KILL = "kill";
        public static final String BOSS = "boss";
        public static final Clear REACH_GATE = new Clear(REACH, 0);

        public static final Codec<Clear> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("type").forGetter(Clear::type),
                Codec.INT.optionalFieldOf("count", 0).forGetter(Clear::count)
        ).apply(i, Clear::new));
    }

    /** わくモブ（重みが大きいほど出やすい）。 */
    public record Mob(Identifier type, int weight) {
        public static final Codec<Mob> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("type").forGetter(Mob::type),
                Codec.INT.optionalFieldOf("weight", 1).forGetter(Mob::weight)
        ).apply(i, Mob::new));
    }

    /** レジストリの名前（{@code kurumimnmod:quest}）で使う ID。 */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, path);
    }
}
