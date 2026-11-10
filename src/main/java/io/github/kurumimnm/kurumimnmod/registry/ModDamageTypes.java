package io.github.kurumimnm.kurumimnmod.registry;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * MOD 独自のダメージタイプ。
 * <p>
 * 中身は JSON（data/kurumimnmod/damage_type/）で定義する。属性ダメージは
 * タグ kurumimnmod:element/&lt;属性名&gt; に入れると、敵ステータスの属性耐性・弱点の対象になる。
 */
public final class ModDamageTypes {

    private ModDamageTypes() {}

    /**
     * 氷の魔法（氷柱の杖）。バニラの間接魔法と同じ扱い（防具無視・死亡メッセージも同じ）で、
     * 属性タグ kurumimnmod:element/ice と魔法攻撃タグ kurumimnmod:magic_attack に入っている。
     */
    public static final ResourceKey<DamageType> ICE_MAGIC = key("ice_magic");

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, name));
    }

    /** ダメージソースを作る（direct = 直接の原因（弾など）、causing = 攻撃した人）。 */
    public static DamageSource source(Level level, ResourceKey<DamageType> type,
                                      @Nullable Entity direct, @Nullable Entity causing) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type),
                direct, causing);
    }
}
