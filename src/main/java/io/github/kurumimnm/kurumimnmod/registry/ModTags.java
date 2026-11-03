package io.github.kurumimnm.kurumimnmod.registry;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;

/**
 * MOD 独自のタグ。
 * <p>
 * タグの中身は JSON（データパック）で定義する。
 */
public final class ModTags {

    private ModTags() {}

    /** エンティティタイプのタグ。 */
    public static final class Entities {

        private Entities() {}

        /**
         * ボス（kurumimnmod:bosses）。
         * <p>
         * ここに入れた Mob はボスとして扱われる。判定は {@link #isBoss(Entity)} を使うこと。
         * <ul>
         *   <li>経験値：アシストも bossAssistExpRatio 倍（既定 100%）、倒されるまでに 1 度でも関われば対象</li>
         *   <li>今後：ボス専用 HP バー・UI・BGM もこのタグで判定する予定</li>
         * </ul>
         * <pre>
         * data/kurumimnmod/tags/entity_type/bosses.json
         * { "values": [ "kurumimnmod:walnut_king" ] }
         * </pre>
         * MOD のボスを追加したら、datagen（タグ Provider）でこの JSON を生成する予定。
         */
        public static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "bosses"));

        /** ボスかどうか。 */
        public static boolean isBoss(Entity entity) {
            return entity.getType().builtInRegistryHolder().is(BOSSES);
        }
    }

    /** ダメージタイプのタグ。 */
    public static final class DamageTypes {

        private DamageTypes() {}

        /**
         * 魔法攻撃（kurumimnmod:magic_attack）。
         * <p>
         * このタグのダメージは、攻撃力ではなく知力の倍率で計算される（StatEvents）。
         * 中身は data/kurumimnmod/tags/damage_type/magic_attack.json
         * （初期値: minecraft:magic, minecraft:indirect_magic）。
         * ※ NeoForge の neoforge:is_magic は茨・毒・ウィザーも含むので使わない。
         */
        public static final TagKey<DamageType> MAGIC_ATTACK = TagKey.create(Registries.DAMAGE_TYPE,
                Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "magic_attack"));
    }
}
