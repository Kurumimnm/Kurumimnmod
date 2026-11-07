package io.github.kurumimnm.kurumimnmod.client.world;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.world.FieldMap;
import io.github.kurumimnm.kurumimnmod.world.FieldRegion;
import java.util.Optional;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.jspecify.annotations.Nullable;

/**
 * 場所の名前の表示（クライアント専用）。
 * <ul>
 *   <li>画面左上：今いる場所の名前を常に表示（街の名前／フィールド名＋エリア・危険度）</li>
 *   <li>画面中央上：街・フィールド・エリアが変わったときに大きく表示してフェードアウト。
 *       初めて来た場所は「NEW」付きで大きめに表示し、音を鳴らす</li>
 * </ul>
 * 位置・色・時間は {@link ModConstants} の LOCATION_*。
 */
@EventBusSubscriber(modid = Kurumimnmod.MOD_ID, value = Dist.CLIENT)
public final class LocationHud {

    private LocationHud() {}

    private static final String KEY = "hud." + Kurumimnmod.MOD_ID + ".location";

    /** 左上の表示内容。 */
    private static @Nullable Component currentName;
    private static @Nullable Component currentSub;

    /** 中央の大きな表示。 */
    private static @Nullable Component titleName;
    private static @Nullable Component titleSub;
    private static boolean titleNew;
    private static long titleStartMs;
    /** 同じフィールドの中でエリアの表示を最後に出した時刻（行き来したときに連続表示しないため）。 */
    private static long lastAreaTitleMs;

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Identifier.fromNamespaceAndPath(Kurumimnmod.MOD_ID, "location"), LocationHud::render);
    }

    static void reset() {
        currentName = null;
        currentSub = null;
        titleName = null;
        titleSub = null;
    }

    /** 場所の表示名（街の名前、またはフィールド名）。 */
    public static Component name(FieldMap map, FieldRegion region) {
        if (region.isTown()) {
            return Component.translatable("town." + Kurumimnmod.MOD_ID + "." + region.town());
        }
        Optional<FieldMap.Field> field = map.field(region.field());
        return field.flatMap(f -> f.biome().unwrapKey())
                .map(key -> (Component) Component.translatable(key.identifier().toLanguageKey("biome")))
                .orElse(Component.literal("?"));
    }

    /** 場所の補足（フィールドなら「エリア N 名前 ・ 推奨Lv L」、街なら街の種類）。 */
    public static Component sub(FieldMap map, FieldRegion region) {
        if (region.isTown()) {
            String type = map.town(region.town()).map(FieldMap.Town::type).orElse("town");
            return Component.translatable("town_type." + Kurumimnmod.MOD_ID + "." + type);
        }
        Optional<FieldMap.Field> field = map.field(region.field());
        int level = field.map(FieldMap.Field::tier).orElse(1);
        String areaKey = "area." + Kurumimnmod.MOD_ID + "." + field.map(FieldMap.Field::id).orElse("") + "." + region.area();
        if (net.minecraft.locale.Language.getInstance().has(areaKey)) {
            // 「エリア3 東の丘陵 ・ 推奨Lv 1」
            return Component.translatable(KEY + ".area_named", region.area(), Component.translatable(areaKey), level);
        }
        return Component.translatable(KEY + ".area", region.area(), level);
    }

    /** 場所が変わったとき（ClientFieldWorld から呼ばれる）。previous = null は最初の 1 回。 */
    static void onEnter(FieldMap map, @Nullable FieldRegion previous, FieldRegion now, boolean isNew) {
        currentName = name(map, now);
        currentSub = sub(map, now);

        long nowMs = System.currentTimeMillis();
        boolean zoneChanged = previous == null || !previous.sameZone(now);
        if (!zoneChanged) {
            // 同じフィールドの中でエリアだけ変わった：初めてのエリアか、しばらくぶりなら表示
            if (!isNew && nowMs - lastAreaTitleMs < ModConstants.LOCATION_TITLE_COOLDOWN_MS) {
                return;
            }
            lastAreaTitleMs = nowMs;
        }
        titleName = currentName;
        titleSub = currentSub;
        titleNew = isNew;
        titleStartMs = nowMs;
        if (isNew) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.playSound(SoundEvents.PLAYER_LEVELUP, 0.5f, 1.4f);
            }
        }
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        if (ClientFieldWorld.map() == null || currentName == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        // ---- 左上：今いる場所 ----
        int x = ModConstants.LOCATION_HUD_X;
        int y = ModConstants.LOCATION_HUD_Y;
        int width = Math.max(font.width(currentName), currentSub == null ? 0 : font.width(currentSub));
        g.fill(x - 2, y - 2, x + width + 2, y + 20, ModConstants.LOCATION_COLOR_BACKGROUND);
        g.text(font, currentName, x, y, ModConstants.LOCATION_COLOR_NAME, true);
        if (currentSub != null) {
            g.text(font, currentSub, x, y + 10, ModConstants.LOCATION_COLOR_SUB, true);
        }

        // ---- 中央上：場所が変わったときの大きな表示 ----
        if (titleName == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - titleStartMs;
        int fadeIn = ModConstants.LOCATION_TITLE_FADE_IN_MS;
        int stay = ModConstants.LOCATION_TITLE_STAY_MS;
        int fadeOut = ModConstants.LOCATION_TITLE_FADE_OUT_MS;
        if (elapsed > fadeIn + stay + fadeOut) {
            titleName = null;
            return;
        }
        float alpha;
        if (elapsed < fadeIn) {
            alpha = elapsed / (float) fadeIn;
        } else if (elapsed < fadeIn + stay) {
            alpha = 1f;
        } else {
            alpha = 1f - (elapsed - fadeIn - stay) / (float) fadeOut;
        }
        int a = Math.max(4, Math.min(255, Math.round(alpha * 255))); // 透明度 0 だと不透明で描かれるので下限を付ける
        int screenW = g.guiWidth();
        int top = Math.round(g.guiHeight() * ModConstants.LOCATION_TITLE_Y);
        float scale = titleNew ? ModConstants.LOCATION_TITLE_NEW_SCALE : ModConstants.LOCATION_TITLE_SCALE;

        int line = top;
        if (titleNew) {
            Component newLabel = Component.translatable(KEY + ".new");
            g.centeredText(font, newLabel, screenW / 2, line, withAlpha(ModConstants.LOCATION_COLOR_NEW, a));
            line += 12;
        }
        g.pose().pushMatrix();
        g.pose().translate(screenW / 2f, line);
        g.pose().scale(scale, scale);
        g.centeredText(font, titleName, 0, 0, withAlpha(ModConstants.LOCATION_COLOR_NAME, a));
        g.pose().popMatrix();
        line += Math.round(10 * scale) + 4;
        if (titleSub != null) {
            g.centeredText(font, titleSub, screenW / 2, line, withAlpha(ModConstants.LOCATION_COLOR_SUB, a));
        }
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }
}
