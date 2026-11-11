package io.github.kurumimnm.kurumimnmod.job;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * 職業 1 つの設計（データパックのレジストリ {@code kurumimnmod:job}）。
 * <pre>
 * data/&lt;名前空間&gt;/kurumimnmod/job/&lt;名前&gt;.json
 * {
 *   "sort": 10,                      … 選択画面の並び順（小さいほど上）
 *   "icon": "minecraft:iron_sword"   … 選択画面のボタンに出すアイテム（省略可）
 * }
 * </pre>
 * 名前・説明は lang の {@code job.<名前空間>.<名前>}／{@code job.<名前空間>.<名前>.description}。
 * レベルアップ時の上がり方・ステータスの倍率・使える武器などは、職業を追加するときにこのレコードへ足す。
 *
 * @param sort 並び順
 * @param icon ボタンのアイコン（アイテム）
 */
public record JobDefinition(int sort, Optional<Identifier> icon) {

    public static final Codec<JobDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("sort", 0).forGetter(JobDefinition::sort),
            Identifier.CODEC.optionalFieldOf("icon").forGetter(JobDefinition::icon)
    ).apply(i, JobDefinition::new));

    public static String nameKey(Identifier id) {
        return "job." + id.getNamespace() + "." + id.getPath();
    }

    public static String descriptionKey(Identifier id) {
        return nameKey(id) + ".description";
    }
}
