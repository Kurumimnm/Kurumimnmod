package io.github.kurumimnm.kurumimnmod.client.hud;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * バニラ HUD の差し替え登録。
 * <ul>
 *   <li>ハート（PLAYER_HEALTH）      → HP/MP バー＋レベルアイコン（{@link PlayerStatusHud}）</li>
 *   <li>空腹ゲージ（FOOD_LEVEL）      → 非表示</li>
 *   <li>水中の泡（AIR_LEVEL）         → 酸素ゲージ（{@link AirGaugeHud}）</li>
 *   <li>経験値バー（CONTEXTUAL_INFO_BAR*） → 非表示（乗り物のジャンプゲージだけは残す）</li>
 *   <li>経験値レベルの数字（EXPERIENCE_LEVEL） → 非表示（レベルアイコンに統合）</li>
 * </ul>
 * 防具ゲージ・乗り物の体力はバニラのまま、HUD の上に積まれて表示される。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID, value = Dist.CLIENT)
public final class ModHudLayers {

    private ModHudLayers() {}

    /** 何も描かないレイヤー。 */
    private static final GuiLayer NONE = (g, deltaTracker) -> {};

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.replaceLayer(VanillaGuiLayers.PLAYER_HEALTH, PlayerStatusHud::render);
        event.replaceLayer(VanillaGuiLayers.FOOD_LEVEL, NONE);
        event.replaceLayer(VanillaGuiLayers.AIR_LEVEL, AirGaugeHud::render);
        event.replaceLayer(VanillaGuiLayers.EXPERIENCE_LEVEL, NONE);

        // 経験値バー／ロケーターバーの枠は消すが、馬などのジャンプゲージは必要なので残す
        event.wrapLayer(VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND, old -> (g, deltaTracker) -> {
            if (PlayerStatusHud.isJumpBarShown(Minecraft.getInstance())) {
                old.render(g, deltaTracker);
            }
        });
        event.wrapLayer(VanillaGuiLayers.CONTEXTUAL_INFO_BAR, old -> (g, deltaTracker) -> {
            if (PlayerStatusHud.isJumpBarShown(Minecraft.getInstance())) {
                old.render(g, deltaTracker);
            }
        });
    }
}
