package io.github.kurumimnm.kurumimnmod.client.hud;

import io.github.kurumimnm.kurumimnmod.client.status.ClientStatus;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.magic.MagicDamageSource;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * レベルエンブレムの右に「いま持っている武器で攻撃したときのダメージ」を表示する。
 * <pre>
 *   (Lv) ⚔ 1.4~7      ← 物理（剣・素手など）。攻撃のクールダウンで変わるので 最小~最大
 *   (Lv) ✦ 6~12       ← 魔法武器（{@link MagicDamageSource} を実装したアイテム）
 * </pre>
 * 【物理ダメージの計算】
 * <ul>
 *   <li>最大 = プレイヤーの攻撃力属性（武器の攻撃力・力のポーション等の効果込み）× ステータス「攻撃力」の倍率</li>
 *   <li>最小 = 最大 × {@link ModConstants#ATTACK_MIN_CHARGE_RATIO}（クールダウンが溜まっていないとき。バニラと同じ）</li>
 *   <li>エンチャント・クリティカル・相手による増減は含めない</li>
 * </ul>
 */
final class WeaponDamageHud {

    private WeaponDamageHud() {}

    // ---- アイコン（9x9 のドット絵。文字ごとに色を割り当てる。'.' は透明） ----
    private static final String[] SWORD_ICON = {
            ".......OO",
            "......OWO",
            ".....OWO.",
            "....OWO..",
            ".G.OWO...",
            "..GWO....",
            "..BGG....",
            ".B.......",
            "B........",
    };
    private static final String[] MAGIC_ICON = {
            "....P....",
            "....P....",
            "...PLP...",
            "..PLWLP..",
            "PPLWWWLPP",
            "..PLWLP..",
            "...PLP...",
            "....P....",
            "....P....",
    };

    private static int iconColor(char c) {
        return switch (c) {
            case 'O' -> 0xFF303030; // 刃のふち
            case 'W' -> 0xFFE8E8F0; // 刃
            case 'G' -> 0xFFD4A020; // つば（金）
            case 'B' -> 0xFF7A4A20; // 柄（茶）
            case 'P' -> 0xFF8A4FD8; // 魔法の星（紫）
            case 'L' -> 0xFFC8A0FF; // 魔法の星（明るい紫）
            default -> 0;
        };
    }

    static void render(GuiGraphicsExtractor g, Minecraft mc, int screenW, int screenH) {
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();

        // ---- ダメージを求める ----
        float min;
        float max;
        boolean magic = stack.getItem() instanceof MagicDamageSource;
        if (magic) {
            MagicDamageSource.DamageRange range = ((MagicDamageSource) stack.getItem()).magicDamage(player, stack);
            min = Math.min(range.min(), range.max());
            max = Math.max(range.min(), range.max());
        } else {
            max = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * ClientStatus.attackMultiplier();
            min = max * ModConstants.ATTACK_MIN_CHARGE_RATIO;
        }
        String text = format(min).equals(format(max)) ? format(max) : format(min) + "~" + format(max);

        // ---- 描画（エンブレムの縦中央に揃える） ----
        Font font = mc.font;
        int size = ModConstants.LEVEL_EMBLEM_SIZE;
        int centerY = screenH - size / 2;
        int x = LevelEmblemHud.right(mc, screenW) + ModConstants.DAMAGE_DISPLAY_MARGIN;

        String[] icon = magic ? MAGIC_ICON : SWORD_ICON;
        drawIcon(g, icon, x, centerY - icon.length / 2);
        x += icon[0].length() + ModConstants.DAMAGE_DISPLAY_ICON_GAP;

        int color = magic ? ModConstants.DAMAGE_COLOR_MAGIC : ModConstants.DAMAGE_COLOR_PHYSICAL;
        // 数字のグリフは高さ 7px
        g.text(font, text, x, centerY - 3, color, true);
    }

    /** ドット絵を 1px ずつ描く（同じ色が横に続く部分はまとめて塗る）。 */
    private static void drawIcon(GuiGraphicsExtractor g, String[] rows, int x, int y) {
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            int col = 0;
            while (col < line.length()) {
                char c = line.charAt(col);
                int end = col + 1;
                while (end < line.length() && line.charAt(end) == c) {
                    end++;
                }
                int color = iconColor(c);
                if (color != 0) {
                    g.fill(x + col, y + row, x + end, y + row + 1, color);
                }
                col = end;
            }
        }
    }

    /** 小数第 1 位まで（.0 は省略）。 */
    private static String format(float value) {
        double rounded = Math.round(value * 10) / 10.0;
        return rounded == Math.rint(rounded)
                ? String.valueOf((long) rounded)
                : String.format(Locale.ROOT, "%.1f", rounded);
    }
}
