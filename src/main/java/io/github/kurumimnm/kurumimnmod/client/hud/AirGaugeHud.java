package io.github.kurumimnm.kurumimnmod.client.hud;

import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

/**
 * 酸素ゲージ（バニラの泡アイコンの代わり）。
 * <p>
 * 水中にいるとき、または酸素が減っているときだけ、HP バーの右上に細いゲージで表示する。
 */
public final class AirGaugeHud {

    private AirGaugeHud() {}

    public static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!PlayerStatusHud.shouldRender(mc)) {
            return;
        }
        LocalPlayer player = mc.player;
        int maxAir = player.getMaxAirSupply();
        int air = Math.clamp(player.getAirSupply(), 0, maxAir);
        boolean underwater = player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
        if (!underwater && air >= maxAir) {
            return;
        }

        var hud = mc.gui.hud;
        // HP バー右半分の上（右側の積み上げ位置）に表示する
        int w = ModConstants.HUD_BAR_WIDTH / 2 - ModConstants.HUD_LEVEL_ICON_SIZE / 2 - 4;
        int h = ModConstants.HUD_AIR_BAR_HEIGHT;
        int x = g.guiWidth() / 2 + ModConstants.HUD_BAR_WIDTH / 2 - w;
        int y = g.guiHeight() - hud.rightHeight + (9 - h);

        HudDraw.bar(g, x, y, w, h, air / (float) Math.max(1, maxAir), ModConstants.HUD_COLOR_AIR, 0, 0);
        hud.rightHeight += 10;
    }
}
