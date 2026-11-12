package io.github.kurumimnm.kurumimnmod.entity;

import io.github.kurumimnm.kurumimnmod.registry.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * 魔法の矢（仮）。見た目・飛び方はバニラの矢と全く同じ（独自のモデル・テクスチャは今はなし）。
 * <p>
 * このクラスは「矢がバニラの矢（物理）か魔法の矢か」をサーバー側で見分けるためだけに存在する
 * （{@code StatEvents#onIncomingDamage} が {@code instanceof MagicArrowEntity} で判定し、
 * 物理攻撃ではなく魔法攻撃として扱う＝知力の倍率がかかり、攻撃力の倍率はかからない）。
 * ダメージの計算自体はバニラのまま（防具無視などの特別な処理はしていない）。
 */
public class MagicArrowEntity extends Arrow {

    public MagicArrowEntity(Level level, LivingEntity shooter, ItemStack pickupItemStack, @Nullable ItemStack firedFromWeapon) {
        super(level, shooter, pickupItemStack, firedFromWeapon);
    }

    /** 拾ったときに戻ってくるアイテムを魔法の矢にする（素の弓矢ではなく）。 */
    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(ModItems.MAGIC_ARROW.get());
    }
}
