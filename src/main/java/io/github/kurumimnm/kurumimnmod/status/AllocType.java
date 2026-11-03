package io.github.kurumimnm.kurumimnmod.status;

import com.mojang.serialization.Codec;
import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * ステータスポイントを振り分けられる項目（ステータス付与画面に並ぶもの）。
 * <pre>
 * 項目     1 ポイントごとの効果（量は Config [status.perPoint]）
 * 体力     最大 HP +10
 * 魔力     最大 MP +10
 * 攻撃力   攻撃力 +1
 * 防御力   防御力 +1
 * 知力     知力 +1
 * </pre>
 * 表示名は lang の {@code stat.kurumimnmod.alloc.<id>}、説明文は {@code ...desc}（%s に上昇量が入る）。
 */
public enum AllocType implements StringRepresentable {
    VITALITY("vitality"),
    MAGIC("magic"),
    ATTACK("attack"),
    DEFENSE("defense"),
    INTELLIGENCE("intelligence");

    /** セーブデータ用（"vitality" などの文字列で保存。並び順を変えても壊れない）。 */
    public static final Codec<AllocType> CODEC = StringRepresentable.fromEnum(AllocType::values);
    /** 通信用（番号で送る）。 */
    public static final StreamCodec<ByteBuf, AllocType> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);

    private final String id;

    AllocType(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public String nameKey() {
        return "stat." + Kurumimnmod.MOD_ID + ".alloc." + id;
    }

    public String descriptionKey() {
        return nameKey() + ".desc";
    }
}
