package com.csedition.client.screen;

import com.csedition.client.ClientState;
import com.csedition.client.render.CSRenderUtil;
import com.csedition.config.ModeConfig;
import com.csedition.network.CSPackets;
import com.csedition.network.PacketMapList;
import com.csedition.network.PacketSelectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI выбора карты в лобби.
 * Стиль CS: тёмные панели, оранжевые акценты, угловые элементы.
 * Всё процедурно — никаких PNG.
 *
 * Если передан modeId — показывает только карты для этого режима.
 * Если modeId пустой — показывает все карты.
 */
public class MapSelectScreen extends Screen {
    private static final int BTN_W = 240;
    private static final int BTN_H = 36;
    private static final int PADDING = 8;

    private final Screen parent;
    private final String modeId;
    private final List<PacketMapList.MapEntry> filteredMaps = new ArrayList<>();

    public MapSelectScreen() {
        this(null, "");
    }

    public MapSelectScreen(Screen parent, String modeId) {
        super(Component.literal("Select Map"));
        this.parent = parent;
        this.modeId = modeId == null ? "" : modeId;
        refreshFiltered();
    }

    private void refreshFiltered() {
        filteredMaps.clear();
        for (PacketMapList.MapEntry entry : ClientState.getMapList()) {
            // Фильтруем по modeId: пустой modeId у карты = подходит для всех режимов
            if (entry.modeId == null || entry.modeId.isEmpty() || entry.modeId.equals(this.modeId)) {
                filteredMaps.add(entry);
            }
        }
    }

    @Override
    protected void init() {
        refreshFiltered();
        int cx = this.width / 2;
        int topY = 60;
        int rowH = BTN_H + PADDING;
        int visibleRows = Math.min(filteredMaps.size(), (this.height - 120) / rowH);

        for (int i = 0; i < visibleRows; i++) {
            PacketMapList.MapEntry entry = filteredMaps.get(i);
            int y = topY + i * rowH;
            Button btn = Button.builder(
                    Component.literal(entry.displayName),
                    b -> selectMap(entry)
            ).bounds(cx - BTN_W / 2, y, BTN_W, BTN_H).build();
            this.addRenderableWidget(btn);
        }

        // Кнопка "Назад" к выбору режима
        if (parent != null) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("< Back to Modes"),
                    b -> Minecraft.getInstance().setScreen(parent)
            ).bounds(10, this.height - 30, 120, 20).build());
        }
    }

    private void selectMap(PacketMapList.MapEntry entry) {
        CSPackets.CHANNEL.sendToServer(new PacketSelectMap(entry.id));
        this.onClose();
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

        // Подзаголовок с режимом
        if (!modeId.isEmpty()) {
            var mode = ModeConfig.getMode(modeId);
            String modeName = mode != null ? mode.getDisplayName() : modeId;
            g.drawCenteredString(this.font, "Mode: " + modeName,
                    this.width / 2, 52, CSRenderUtil.CS_YELLOW);
        }

        // Сообщение если нет карт
        if (filteredMaps.isEmpty()) {
            g.drawCenteredString(this.font, "No maps for this mode",
                    this.width / 2, this.height / 2, 0xFF888888);
            g.drawCenteredString(this.font, "Use /cs addmap to create one",
                    this.width / 2, this.height / 2 + 15, 0xFF666666);
        }

        g.drawCenteredString(this.font, "Press ESC to close",
                this.width / 2, this.height - 8, 0xFF888888);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
