package io.github.kurumimnm.kurumimnmod.magic;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 「魔法ダメージを与えるアイテム（魔法武器）」であることを表すインターフェース。
 * <p>
 * このインターフェースを実装したアイテムをメインハンドに持つと、HUD のダメージ表示が
 * 物理ダメージではなく、ここで返す魔法ダメージになる（{@code client/hud/WeaponDamageHud}）。
 * <p>
 * 実装例（氷柱の杖：中心ダメージが最大、範囲の端が最小。知力の倍率をかける）:
 * <pre>{@code
 * public class IceWandItem extends Item implements MagicWand, MagicDamageSource {
 *     @Override
 *     public DamageRange magicDamage(Player player, ItemStack stack) {
 *         float max = (float) (ModConfigs.iceWandDamage() * StatManager.magicMultiplier(player));
 *         return new DamageRange(max * (float) ModConfigs.iceWandEdgeDamageRatio(), max);
 *     }
 * }
 * }</pre>
 * クライアント側の HUD からも呼ばれるので、サーバー専用の処理（ServerPlayer へのキャスト等）は書かないこと。
 */
public interface MagicDamageSource {

    /**
     * いま攻撃したときの魔法ダメージ（ステータスの倍率など込みの最終値）。
     *
     * @param player 持っているプレイヤー
     * @param stack  メインハンドのアイテム
     */
    DamageRange magicDamage(Player player, ItemStack stack);

    /**
     * ダメージの幅。最小と最大が同じなら 1 つの数値として表示される。
     *
     * @param min 最小ダメージ（範囲の端・溜め不足など）
     * @param max 最大ダメージ
     */
    record DamageRange(float min, float max) {
        /** 固定ダメージ（最小 = 最大）。 */
        public static DamageRange of(float value) {
            return new DamageRange(value, value);
        }
    }
}
