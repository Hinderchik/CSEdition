package com.csedition.client.screen;

import com.csedition.client.ClientState;
import com.csedition.client.render.CSRenderUtil;
import com.csedition.network.CSPackets;
import com.csedition.network.PacketMapList;
import com.csedition.network.PacketSelectMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * GUI выбора карты в лобби.
 * Стиль CS: тёмные панели, оранжевые акценты, угловые элементы.
 * Всё процедурно — никаких PNG.
 */
public class MapSelectScreen extends Screen {
    private static final int BTN_W = 240;
    private static final int BTN_H = 36;
    private static final int PADDING = 8;

    public MapSelectScreen() {
        super(Component.literal("Select Map"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Тёмный фон со сканлайнами
        g.fill(0, 0, this.width, this.height, 0xDD050505);
        CSRenderUtil.scanlines(g, 0, 0, this.width, this.height, 3);

        // Заголовок
        String title = "SELECT MAP";
        int titleW = this.font.width(title) + 40;
        CSRenderUtil.csPanel(g, this.width / 2 - titleW / 2, 16, titleW, 32, null, this.font);
        CSRenderUtil.cornerAccents(g, this.width / 2 - titleW / 2, 16, titleW, 32, 8, CSRenderUtil.CS_ORANGE);
        g.drawCenteredString(this.font, title, this.width / 2, 28, CSRenderUtil.CS_ORANGE);

        List<PacketMapList.MapEntry> maps = ClientState.getMapList();
        int totalH = maps.size() * (BTN_H + PADDING);
        int startY = (this.height - totalH) / 2 + 20;
        int x = (this.width - BTN_W) / 2;

        for (int i = 0; i < maps.size(); i++) {
            int y = startY + i * (BTN_H + PADDING);
            boolean hovered = mouseX >= x && mouseX <= x + BTN_W && mouseY >= y && mouseY <= y + BTN_H;
            CSRenderUtil.csButton(g, x, y, BTN_W, BTN_H, maps.get(i).displayName, hovered, this.font);
        }

        g.drawCenteredString(this.font, "Press ESC to close",
                this.width / 2, this.height - 20, 0xFF888888);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<PacketMapList.MapEntry> maps = ClientState.getMapList();
        int totalH = maps.size() * (BTN_H + PADDING);
        int startY = (this.height - totalH) / 2 + 20;
        int x = (this.width - BTN_W) / 2;

        for (int i = 0; i < maps.size(); i++) {
            int y = startY + i * (BTN_H + PADDING);
            if (mouseX >= x && mouseX <= x + BTN_W && mouseY >= y && mouseY <= y + BTN_H) {
                CSPackets.CHANNEL.sendToServer(new PacketSelectMap(maps.get(i).id));
                this.onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
