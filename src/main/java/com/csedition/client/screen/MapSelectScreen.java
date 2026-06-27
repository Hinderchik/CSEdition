package com.csedition.client.screen;

import com.csedition.CSEditionMod;
import com.csedition.client.ClientState;
import com.csedition.network.CSPackets;
import com.csedition.network.PacketMapList;
import com.csedition.network.PacketSelectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Экран выбора карты.
 * Стиль CS: тёмный фон, квадратные кнопки по 3 в ряд, прокрутка колесом мыши.
 *
 * Карты приходят с сервера через PacketMapList и хранятся в ClientState.mapList.
 * При клике отправляется PacketSelectMap на сервер.
 *
 * Может фильтровать по modeId (если задан) — показывает только карты выбранного режима.
 */
public class MapSelectScreen extends Screen {
    private static final int COLS = 3;
    private static final int BTN_SIZE = 80;
    private static final int GAP = 12;
    private static final int PADDING = 20;
    private static final int HEADER_HEIGHT = 40;
    private static final int FOOTER_HEIGHT = 30;

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private List<PacketMapList.MapEntry> maps;
    private final String modeId; // null = все карты

    public MapSelectScreen() {
        this(null);
    }

    public MapSelectScreen(String modeId) {
        this(modeId, null);
    }

    public MapSelectScreen(String modeId, Screen parent) {
        super(Component.literal("Выбор карты"));
        this.modeId = modeId;
        // parent не используется, но принимаем для совместимости
    }

    @Override
    protected void init() {
        super.init();
        // Фильтруем карты по режиму, если modeId задан
        List<PacketMapList.MapEntry> all = ClientState.getMapList();
        if (all == null) {
            this.maps = java.util.Collections.emptyList();
        } else if (modeId == null || modeId.isEmpty()) {
            this.maps = all;
        } else {
            this.maps = all.stream()
                    .filter(e -> modeId.equals(e.modeId))
                    .collect(Collectors.toList());
        }
        rebuildWidgets();
    }

    protected void rebuildWidgets() {
        clearWidgets();
        if (maps == null) return;

        int rows = (maps.size() + COLS - 1) / COLS;
        int gridHeight = rows * BTN_SIZE + (rows - 1) * GAP;
        int availableHeight = height - HEADER_HEIGHT - FOOTER_HEIGHT - PADDING * 2;
        maxScroll = Math.max(0, gridHeight - availableHeight);

        int gridWidth = COLS * BTN_SIZE + (COLS - 1) * GAP;
        int startX = (width - gridWidth) / 2;
        int startY = HEADER_HEIGHT + PADDING - scrollOffset;

        for (int i = 0; i < maps.size(); i++) {
            int row = i / COLS;
            int col = i % COLS;
            int x = startX + col * (BTN_SIZE + GAP);
            int y = startY + row * (BTN_SIZE + GAP);
            PacketMapList.MapEntry entry = maps.get(i);
            Button btn = Button.builder(
                    Component.literal(entry.displayName),
                    b -> selectMap(entry.id)
            ).bounds(x, y, BTN_SIZE, BTN_SIZE).build();
            addRenderableWidget(btn);
        }

        // Кнопка закрытия
        int closeY = height - FOOTER_HEIGHT;
        addRenderableWidget(Button.builder(
                Component.literal("Закрыть"),
                b -> Minecraft.getInstance().setScreen(null)
        ).bounds(width / 2 - 50, closeY, 100, 20).build());
    }

    private void selectMap(String mapId) {
        CSEditionMod.LOGGER.info("[CS-Edition] Selecting map: {}", mapId);
        CSPackets.CHANNEL.sendToServer(new PacketSelectMap(mapId));
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Тёмный фон
        g.fill(0, 0, width, height, 0xE0101010);

        // Заголовок
        String title = modeId != null && !modeId.isEmpty()
                ? "ВЫБОР КАРТЫ — " + modeId.toUpperCase()
                : "ВЫБОР КАРТЫ";
        int titleW = font.width(title);
        g.drawString(font, title, (width - titleW) / 2, 12, 0xFFFFAA00);

        // Подсказка о прокрутке
        if (maxScroll > 0) {
            String hint = "Прокрутите для просмотра";
            int hintW = font.width(hint);
            g.drawString(font, hint, (width - hintW) / 2, height - FOOTER_HEIGHT - 14, 0xFF888888);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll <= 0) return false;
        int old = scrollOffset;
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 16));
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
