package com.csedition.client.screen;

import com.csedition.CSEditionMod;
import com.csedition.client.ClientState;
import com.csedition.client.render.CSRenderUtil;
import com.csedition.data.MapData;
import com.csedition.network.CSPackets;
import com.csedition.network.PacketMapList;
import com.csedition.network.PacketSelectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Экран выбора карты в стиле CS.
 *
 * Карты приходят с сервера через PacketMapList и хранятся в ClientState.mapList.
 * При клике отправляется PacketSelectMap на сервер.
 *
 * Фильтрация по modeId:
 *   - modeId == null или "any" → показываем все карты
 *   - modeId == "classic" → показываем карты с modeId="classic" ИЛИ modeId="any" ИЛИ пустым
 *
 * Стиль: тёмный фон с сканлайнами, оранжевые акценты, угловые рамки, плавная прокрутка.
 */
public class MapSelectScreen extends Screen {
    private static final int COLS = 3;
    private static final int BTN_W = 130;
    private static final int BTN_H = 80;
    private static final int GAP_X = 14;
    private static final int GAP_Y = 12;
    private static final int TOP_Y = 70;
    private static final int BOTTOM_PAD = 60;

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private List<PacketMapList.MapEntry> maps;
    private final String modeId;

    public MapSelectScreen() {
        this(null, null);
    }

    public MapSelectScreen(String modeId) {
        this(modeId, null);
    }

    public MapSelectScreen(String modeId, Screen parent) {
        super(Component.literal("Выбор карты"));
        this.modeId = modeId;
    }

    @Override
    protected void init() {
        super.init();
        List<PacketMapList.MapEntry> all = ClientState.getMapList();
        if (all == null) {
            this.maps = Collections.emptyList();
        } else if (modeId == null || modeId.isEmpty() || MapData.MODE_ANY.equalsIgnoreCase(modeId)) {
            // null/empty/any → все карты
            this.maps = all;
        } else {
            // Фильтр: карта подходит если её modeId совпадает ИЛИ она "any" ИЛИ пустая
            this.maps = all.stream()
                    .filter(e -> e.modeId == null
                            || e.modeId.isEmpty()
                            || MapData.MODE_ANY.equalsIgnoreCase(e.modeId)
                            || modeId.equals(e.modeId))
                    .collect(Collectors.toList());
        }
        rebuildWidgets();
    }

    protected void rebuildWidgets() {
        clearWidgets();
        if (maps == null) return;

        int rows = (maps.size() + COLS - 1) / COLS;
        int gridHeight = rows * BTN_H + (rows - 1) * GAP_Y;
        int availableHeight = this.height - TOP_Y - BOTTOM_PAD;
        maxScroll = Math.max(0, gridHeight - availableHeight);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        int gridWidth = COLS * BTN_W + (COLS - 1) * GAP_X;
        int startX = (this.width - gridWidth) / 2;
        int startY = TOP_Y - scrollOffset;

        for (int i = 0; i < maps.size(); i++) {
            int row = i / COLS;
            int col = i % COLS;
            int x = startX + col * (BTN_W + GAP_X);
            int y = startY + row * (BTN_H + GAP_Y);
            PacketMapList.MapEntry entry = maps.get(i);
            Button btn = Button.builder(
                    Component.literal(entry.displayName),
                    b -> selectMap(entry.id)
            ).bounds(x, y, BTN_W, BTN_H).build();
            this.addRenderableWidget(btn);
        }

        // Кнопки прокрутки (справа от сетки)
        int scrollX = startX + gridWidth + 10;
        if (scrollOffset > 0) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("\u25B2"),
                    b -> { scrollOffset = Math.max(0, scrollOffset - 50); rebuildWidgets(); }
            ).bounds(scrollX, TOP_Y, 28, 28).build());
        }
        if (scrollOffset < maxScroll) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("\u25BC"),
                    b -> { scrollOffset = Math.min(maxScroll, scrollOffset + 50); rebuildWidgets(); }
            ).bounds(scrollX, TOP_Y + 32, 28, 28).build());
        }

        // Кнопка "Назад"
        this.addRenderableWidget(Button.builder(
                Component.literal("Назад"),
                b -> Minecraft.getInstance().setScreen(null)
        ).bounds(this.width / 2 - 50, this.height - 32, 100, 22).build());
    }

    private void selectMap(String mapId) {
        CSEditionMod.LOGGER.info("[CS-Edition] Selecting map: {}", mapId);
        CSPackets.CHANNEL.sendToServer(new PacketSelectMap(mapId));
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Тёмный фон
        g.fill(0, 0, this.width, this.height, 0xFF050505);

        // Сканлайны (эффект CRT)
        CSRenderUtil.scanlines(g, 0, 0, this.width, this.height, 3);

        // Верхняя панель с заголовком
        g.fill(0, 0, this.width, 55, 0xCC0A0A0A);
        g.fill(0, 53, this.width, 55, CSRenderUtil.CS_ORANGE);

        Font font = this.font;

        // Заголовок
        String title = modeId != null && !modeId.isEmpty() && !MapData.MODE_ANY.equalsIgnoreCase(modeId)
                ? "ВЫБОР КАРТЫ — " + modeId.toUpperCase()
                : "ВЫБОР КАРТЫ";
        int titleW = font.width(title);
        g.drawString(font, title, this.width / 2 - titleW / 2, 12, CSRenderUtil.CS_ORANGE);

        // Подзаголовок
        String subtitle = "Нажмите на карту для запуска матча";
        int subW = font.width(subtitle);
        g.drawString(font, subtitle, this.width / 2 - subW / 2, 30, 0xFFAAAAAA);

        // Угловые акценты на верхней панели
        CSRenderUtil.cornerAccents(g, 0, 0, this.width, 55, 12, CSRenderUtil.CS_ORANGE);

        // Контент
        if (maps.isEmpty()) {
            String empty = "Нет карт для этого режима";
            int w = font.width(empty);
            g.drawString(font, empty, this.width / 2 - w / 2, this.height / 2 - 10, 0xFFFF6666);
            String hint = "Добавьте карту через /cs map add <id> <name> [mode]";
            int hw = font.width(hint);
            g.drawString(font, hint, this.width / 2 - hw / 2, this.height / 2 + 10, 0xFF888888);
        } else {
            // Индикатор прокрутки
            int rows = (maps.size() + COLS - 1) / COLS;
            int visibleRows = Math.max(1, (this.height - TOP_Y - BOTTOM_PAD) / (BTN_H + GAP_Y));
            int firstVisible = scrollOffset / (BTN_H + GAP_Y) + 1;
            int lastVisible = Math.min(rows, firstVisible + visibleRows - 1);
            String scrollInfo = "Карты " + firstVisible + "-" + lastVisible + " из " + rows + " (" + maps.size() + " всего)";
            int infoW = font.width(scrollInfo);
            g.drawString(font, scrollInfo, this.width / 2 - infoW / 2, this.height - 50, 0xFF888888);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll <= 0) return false;
        int old = scrollOffset;
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 25));
        if (old != scrollOffset) {
            rebuildWidgets();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
