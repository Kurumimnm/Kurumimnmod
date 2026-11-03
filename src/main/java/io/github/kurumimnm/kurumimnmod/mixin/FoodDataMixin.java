package io.github.kurumimnm.kurumimnmod.mixin;

import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 空腹システムの無効化。
 * <p>
 * バニラの {@code FoodData#tick} は「空腹度の消費」「満腹時の自然回復」「餓死ダメージ」を
 * まとめて処理しているので、Config で無効化されている間はこの処理を丸ごとスキップする。
 * 満腹度は常に最大に固定しておく（ダッシュ可否などバニラの判定が満腹度を見ているため）。
 * <p>
 * ※ 満腹度が常に最大なので、通常の食べ物は食べられなくなる
 *   （金のリンゴなど「いつでも食べられる」物は食べられる）。
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    // 引数（ServerPlayer）は使わないので省略している（Mixin は末尾の引数を省略できる）
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void kurumimnmod$disableHunger(CallbackInfo ci) {
        if (ModConfigs.isHungerDisabled()) {
            FoodData self = (FoodData) (Object) this;
            self.setFoodLevel(20);
            self.setSaturation(0f);
            ci.cancel();
        }
    }
}
