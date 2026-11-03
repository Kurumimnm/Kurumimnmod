package io.github.kurumimnm.kurumimnmod.client.magic;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.client.status.ClientStatus;
import io.github.kurumimnm.kurumimnmod.config.ModConfigs;
import io.github.kurumimnm.kurumimnmod.item.IceWandItem;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * 魔法の杖のツールチップ（必要 MP・威力・範囲・操作方法）。
 * <p>
 * 威力は「知力の倍率をかけた現在の値」を表示する。数値は Config から読むので、Config を変えれば表示も変わる。
 * 文言は lang の {@code tooltip.kurumimnmod.wand.*}。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID, value = Dist.CLIENT)
public final class WandTooltips {

    private WandTooltips() {}

    private static final String KEY = "tooltip." + Kurumimnmod.MOD_ID + ".wand.";

    @SubscribeEvent
    static void onItemTooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof IceWandItem)) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        double damage = ModConfigs.iceWandDamage() * ClientStatus.magicMultiplier();
        tooltip.add(Component.translatable(KEY + "mp_cost", format(IceWandItem.manaCost()))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(KEY + "damage", format(damage),
                format(damage * ModConfigs.iceWandEdgeDamageRatio())).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(KEY + "radius", format(ModConfigs.iceWandRadius()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(KEY + "pierce").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(KEY + "controls").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** 小数第 1 位まで（.0 は省略）。 */
    private static String format(double value) {
        double rounded = Math.round(value * 10) / 10.0;
        return rounded == Math.rint(rounded)
                ? String.valueOf((long) rounded)
                : String.format(Locale.ROOT, "%.1f", rounded);
    }
}
