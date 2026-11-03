package io.github.kurumimnm.kurumimnmod.item;

import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import io.github.kurumimnm.kurumimnmod.entity.IceIcicleEntity;
import io.github.kurumimnm.kurumimnmod.magic.MagicDamageSource;
import io.github.kurumimnm.kurumimnmod.magic.MagicWand;
import io.github.kurumimnm.kurumimnmod.status.StatManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * 氷柱の杖。頭上から大きな氷柱を 1 本、狙った場所へ撃ち出す。
 * <ul>
 *   <li>右クリック長押し … 着弾地点にサークルを表示（照準。描画は client/magic/AoeIndicatorRenderer）</li>
 *   <li>左クリック … 発射（照準なしでも発射できる）</li>
 *   <li>必要 MP・ダメージ・範囲・クールダウンなどは Config の [magic.iceWand]</li>
 * </ul>
 * 氷柱の動き・ダメージ処理は {@link IceIcicleEntity}。
 */
public class IceWandItem extends Item implements MagicWand, MagicDamageSource {

    /** 照準を出し続けられる時間（実質無制限。弓などと同じ値）。 */
    private static final int AIM_DURATION = 72000;

    public IceWandItem(Properties properties) {
        super(properties);
    }

    // =====================================================================
    // HUD のダメージ表示（client/hud/WeaponDamageHud）
    // =====================================================================

    /** 中心ダメージが最大、範囲の端が最小。どちらも知力の倍率込み。 */
    @Override
    public DamageRange magicDamage(Player player, ItemStack stack) {
        float max = (float) (ModConfigs.iceWandDamage() * StatManager.magicMultiplier(player));
        return new DamageRange(max * (float) ModConfigs.iceWandEdgeDamageRatio(), max);
    }

    // =====================================================================
    // 右クリック：照準（長押しの間「使用中」になり、その間サークルを表示する）
    // =====================================================================

    /** メインハンドのときだけ構えられる（発射もメインハンドの杖で行うため）。 */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return AIM_DURATION;
    }

    /** 構えのモーション（弓と同じく腕を前に出す）。 */
    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    // =====================================================================
    // 左クリック：発射
    // =====================================================================

    @Override
    public void cast(ServerPlayer player, ItemStack stack) {
        if (player.isSpectator() || player.getCooldowns().isOnCooldown(stack)) {
            return;
        }
        // MP 消費（クリエイティブでは消費しない）
        if (!player.getAbilities().instabuild
                && !StatManager.tryConsumeMana(player, manaCost())) {
            player.sendSystemMessage(Component.translatable("message.kurumimnmod.not_enough_mp")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        IceIcicleEntity.launch(player);

        player.getCooldowns().addCooldown(stack, ModConfigs.iceWandCooldown());
        player.swing(InteractionHand.MAIN_HAND, stack.getAttackAnimation(), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    /** 1 回に必要な MP。 */
    public static float manaCost() {
        return (float) ModConfigs.iceWandMpCost();
    }

    @Override
    public double aimRange() {
        return ModConfigs.iceWandRange();
    }

    @Override
    public double aoeRadius() {
        return ModConfigs.iceWandRadius();
    }

    @Override
    public boolean canCast(Player player, ItemStack stack) {
        if (player.getCooldowns().isOnCooldown(stack)) {
            return false;
        }
        return player.getAbilities().instabuild || StatManager.mana(player) >= manaCost();
    }
}
