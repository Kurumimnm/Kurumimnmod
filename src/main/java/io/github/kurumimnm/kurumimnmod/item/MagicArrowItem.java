package io.github.kurumimnm.kurumimnmod.item;

import io.github.kurumimnm.kurumimnmod.entity.MagicArrowEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * 魔法の矢（仮）。弓・クロスボウにつがえて撃つと魔法攻撃になる（物理の攻撃力ではなく知力の倍率がかかる）。
 * <p>
 * 誰が使えるかは弓・クロスボウ自体の職業制限（タグ kurumimnmod:weapons/bow）に従うだけで、
 * この矢そのものに職業の制限はない（弓を持てる職業なら誰でも使える）。
 * 入手方法（クラフトレシピ）・見た目（専用テクスチャ）は未定。今はバニラの矢と同じ見た目で、
 * コマンド（/give）かクリエイティブ・MOD タブからのみ入手できる。
 */
public class MagicArrowItem extends ArrowItem {

    public MagicArrowItem(Properties properties) {
        super(properties);
    }

    public AbstractArrow createArrowTmpDisabled(Level level, ItemStack ammo, ItemStack weapon, @Nullable LivingEntity shooter) {
        return new MagicArrowEntity(level, shooter, ammo, weapon);
    }
}
