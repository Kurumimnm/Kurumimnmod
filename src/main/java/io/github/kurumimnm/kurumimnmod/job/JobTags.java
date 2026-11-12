package io.github.kurumimnm.kurumimnmod.job;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * 職業の武器制限に使うアイテムタグ（data/kurumimnmod/tags/item/weapons/*.json）。
 * <pre>
 * kurumimnmod:weapons           … 制限の対象になる武器すべて（下の 5 つをまとめたもの）
 * kurumimnmod:weapons/sword     … 剣（バニラの #minecraft:swords を含む）
 * kurumimnmod:weapons/axe       … 斧（#minecraft:axes）
 * kurumimnmod:weapons/spear     … 槍（#minecraft:spears）
 * kurumimnmod:weapons/gauntlet  … ガントレット（MOD で追加したらここに入れる）
 * kurumimnmod:weapons/bow       … 弓・クロスボウ（狩人系だけが使える）
 * </pre>
 * 杖などの魔法武器は kurumimnmod:weapons に入れていないので、どの職業でも使える。
 */
public final class JobTags {

    private JobTags() {}

    public static final TagKey<Item> WEAPONS = item("weapons");

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, path));
    }
}
