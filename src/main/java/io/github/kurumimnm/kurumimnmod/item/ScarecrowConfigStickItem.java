package io.github.kurumimnm.kurumimnmod.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * カカシ設定用ステッキ。カカシを狙って左クリックすると設定画面が開く（誰でも使用可能）。
 * <p>
 * 左クリックの処理はクライアント側の client/scarecrow/ScarecrowInputHandler で行う
 * （カカシを攻撃する代わりに、サーバーへ設定の取得をリクエストする）。
 * カカシ以外を狙っているときは普通の棒と同じ。
 */
public class ScarecrowConfigStickItem extends Item {

    public ScarecrowConfigStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.kurumimnmod.scarecrow_config_stick.desc").withStyle(ChatFormatting.GRAY));
    }
}
