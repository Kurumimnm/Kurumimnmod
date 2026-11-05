package io.github.kurumimnm.kurumimnmod.client.world;

import io.github.kurumimnm.kurumimnmod.Kurumimnmod;
import io.github.kurumimnm.kurumimnmod.client.ModKeyMappings;
import io.github.kurumimnm.kurumimnmod.config.ModConstants;
import io.github.kurumimnm.kurumimnmod.network.MapTeleportPayload;
import io.github.kurumimnm.kurumimnmod.world.FieldMap;
import io.github.kurumimnm.kurumimnmod.world.FieldRegion;
import io.github.kurumimnm.kurumimnmod.world.PlayerExploration;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * 地図画面（M キー・クライアント専用）。
 * <ul>
 *   <li>行ったことのあるエリア・街だけが見える。エリアの境目は白い線</li>
 *   <li>ドラッグで移動、ホイールで拡大縮小。マウスを乗せた場所の名前を表示</li>
 *   <li>OP は右クリックでその場所へテレポート</li>
 * </ul>
 */
public final class WorldMapScreen extends Screen {

    private static final String KEY = "screen." + Kurumimnmod.MOD_ID + ".map";
    private static final int MARGIN = 8;
    private static final int FOOTER = 14;

    private final FieldMap map;
    /** 画面の中央に来るブロック座標と、拡大率（画面 1 ピクセルあたり地図画像の何ピクセルか）。 */
    private double centerX;
    private double centerZ;
    private float zoom = 1f;
    private boolean dragged;

    public WorldMapScreen(FieldMap map, double startX, double startZ) {
        super(Component.translatable(KEY));
        this.map = map;
        this.centerX = startX;
        this.centerZ = startZ;
    }

    private PlayerExploration exploration() {
        LocalPlayer player = this.minecraft.player;
        return player == null ? PlayerExploration.EMPTY : ClientFieldWorld.exploration(player);
    }

    private boolean isOperator() {
        LocalPlayer player = this.minecraft.player;
        return player != null && Commands.LEVEL_GAMEMASTERS.check(player.permissions());
    }

    // ---- 座標の変換（画面 ⇔ ブロック） ----

    private double blocksPerScreenPixel() {
        return ModConstants.MAP_BLOCKS_PER_PIXEL / (double) zoom;
    }

    private double toBlockX(double screenX) {
        return centerX + (screenX - this.width / 2.0) * blocksPerScreenPixel();
    }

    private double toBlockZ(double screenY) {
        return centerZ + (screenY - mapCenterY()) * blocksPerScreenPixel();
    }

    private double toScreenX(double blockX) {
        return this.width / 2.0 + (blockX - centerX) / blocksPerScreenPixel();
    }

    private double toScreenY(double blockZ) {
        return mapCenterY() + (blockZ - centerZ) / blocksPerScreenPixel();
    }

    private double mapCenterY() {
        return (MARGIN + (this.height - FOOTER)) / 2.0;
    }

    // ---- 描画 ----

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(g, mouseX, mouseY, partialTicks);
        int left = MARGIN;
        int top = MARGIN;
        int right = this.width - MARGIN;
        int bottom = this.height - FOOTER;
        g.fill(left - 1, top - 1, right + 1, bottom + 1, 0xFF000000);
        g.fill(left, top, right, bottom, ModConstants.MAP_COLOR_UNEXPLORED);

        PlayerExploration exploration = exploration();
        if (!WorldMapImage.ready(map, exploration)) {
            g.centeredText(this.font, Component.translatable(KEY + ".loading"), this.width / 2, this.height / 2, 0xFFFFFFFF);
            return;
        }

        g.enableScissor(left, top, right, bottom);
        // 地図の画像
        int bpp = ModConstants.MAP_BLOCKS_PER_PIXEL;
        int x0 = (int) Math.floor(toScreenX(WorldMapImage.originX()));
        int y0 = (int) Math.floor(toScreenY(WorldMapImage.originZ()));
        int x1 = (int) Math.ceil(toScreenX(WorldMapImage.originX() + (double) WorldMapImage.width() * bpp));
        int y1 = (int) Math.ceil(toScreenY(WorldMapImage.originZ() + (double) WorldMapImage.height() * bpp));
        g.blit(WorldMapImage.TEXTURE, x0, y0, x1, y1, 0f, 1f, 0f, 1f);

        // 街の印と名前（行ったことのある街だけ）
        for (FieldMap.Town town : map.towns()) {
            if (!exploration.towns().contains(town.id())) {
                continue;
            }
            int tx = (int) Math.round(toScreenX(town.x()));
            int ty = (int) Math.round(toScreenY(town.z()));
            g.fill(tx - 3, ty - 3, tx + 3, ty + 3, 0xFF000000);
            g.fill(tx - 2, ty - 2, tx + 2, ty + 2, ModConstants.MAP_COLOR_TOWN);
            g.centeredText(this.font, LocationHud.name(map, FieldRegion.town(town.id())), tx, ty + 5, 0xFFFFFFFF);
        }

        // プレイヤーの位置
        LocalPlayer player = this.minecraft.player;
        if (player != null) {
            int ppx = (int) Math.round(toScreenX(player.getX()));
            int ppy = (int) Math.round(toScreenY(player.getZ()));
            g.fill(ppx - 3, ppy - 3, ppx + 3, ppy + 3, 0xFFFFFFFF);
            g.fill(ppx - 2, ppy - 2, ppx + 2, ppy + 2, ModConstants.MAP_COLOR_PLAYER);
            // 向いている方向
            double yaw = Math.toRadians(player.getYRot());
            for (int step = 4; step <= 9; step++) {
                int lx = (int) Math.round(ppx - Math.sin(yaw) * step);
                int ly = (int) Math.round(ppy + Math.cos(yaw) * step);
                g.fill(lx, ly, lx + 1, ly + 1, ModConstants.MAP_COLOR_PLAYER);
            }
        }
        g.disableScissor();

        // マウスを乗せた場所の名前
        if (mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom) {
            double bx = toBlockX(mouseX);
            double bz = toBlockZ(mouseY);
            FieldRegion region = map.regionAt(bx, bz);
            Component label = exploration.visited(region)
                    ? Component.translatable(KEY + ".hover", LocationHud.name(map, region), LocationHud.sub(map, region))
                    : Component.translatable(KEY + ".unexplored");
            g.text(this.font, label, mouseX + 10, mouseY - 4, 0xFFFFFFFF, true);
            g.text(this.font, Component.translatable(KEY + ".coords", (int) bx, (int) bz), left + 4, bottom - 12, 0xFFC8C8C8, true);
        }

        // 操作説明
        Component help = Component.translatable(isOperator() ? KEY + ".help_op" : KEY + ".help");
        g.centeredText(this.font, help, this.width / 2, this.height - FOOTER + 3, 0xFFC8C8C8);
    }

    // ---- 操作 ----

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        dragged = false;
        super.mouseClicked(event, doubleClick);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (event.button() == 0) {
            centerX -= dx * blocksPerScreenPixel();
            centerZ -= dy * blocksPerScreenPixel();
            dragged = true;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        // 右クリック（ドラッグしていない）で、OP ならその場所へテレポート
        if (event.button() == 1 && !dragged && isOperator()) {
            int bx = (int) Math.floor(toBlockX(event.x()));
            int bz = (int) Math.floor(toBlockZ(event.y()));
            ClientPacketDistributor.sendToServer(new MapTeleportPayload(bx, bz));
            this.onClose();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // マウスの位置を中心に拡大縮小する
        double beforeX = toBlockX(mouseX);
        double beforeZ = toBlockZ(mouseY);
        float factor = scrollY > 0 ? ModConstants.MAP_ZOOM_STEP : 1f / ModConstants.MAP_ZOOM_STEP;
        zoom = Math.max(ModConstants.MAP_MIN_ZOOM, Math.min(ModConstants.MAP_MAX_ZOOM, zoom * factor));
        centerX += beforeX - toBlockX(mouseX);
        centerZ += beforeZ - toBlockZ(mouseY);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        KeyMapping open = ModKeyMappings.OPEN_MAP;
        if (!open.isUnbound() && open.matches(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
