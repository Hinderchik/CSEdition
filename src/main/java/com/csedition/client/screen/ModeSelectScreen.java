package com.csedition.client.screen;

import com.csedition.client.ClientState;
import com.csedition.client.render.CSRenderUtil;
import com.csedition.config.ModeConfig;
import com.csedition.data.GameMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран выбора режима. Открывается по клавише M в лобби.
 * После выбора режима открывает MapSelectScreen с фильтром по режиму.
 *
 * Стиль CS: тёмный фон, оранжевый заголовок, аккуратные кнопки без наложений.
 */
public class ModeSelectScreen extends Screen {
    private final Screen parent;
    private final List<GameMode> modes = new ArrayList<>();
    private int scrollOffset = 0;

    private static final int ROW_H = 32;
    private static final int TOP_Y = 50;
    private static final int BOTTOM_PAD = 50;
    private static final int BTN_W = 280;

    public ModeSelectScreen(Screen parent) {
        super(Component.literal("Выбор режима"));
        this.parent = parent;
        refreshModes();
    }

    private void refreshModes() {
        modes.clear();
        modes.addAll(ModeConfig.getModes().values());
    }

    @Override
    protected void init() {
        super.init();
        rebuildWidgets();
    }

    protected void rebuildWidgets() {
        clearWidgets();

        int cx = this.width / 2;
        int visibleRows = Math.max(1, (this.height - TOP_Y - BOTTOM_PAD) / ROW_H);

        // Кнопки режимов
        for (int i = 0; i < visibleRows; i++) {
            int idx = i + scrollOffset;
            if (idx >= modes.size()) break;
            GameMode mode = modes.get(idx);
            int y = TOP_Y + i * ROW_H;
            Button btn = Button.builder(
                    Component.literal(mode.getDisplayName() + (mode.isBuiltIn() ? "" : " *")),
                    b -> selectMode(mode)
            ).bounds(cx - BTN_W / 2, y, BTN_W, ROW_H - 4).build();
            this.addRenderableWidget(btn);
        }

        // Кнопки прокрутки (справа от списка)
        int scrollX = cx + BTN_W / 2 + 8;
        if (scrollOffset > 0) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("\u25B2"),
                    b -> { scrollOffset = Math.max(0, scrollOffset - 1); rebuildWidgets(); }
            ).bounds(scrollX, TOP_Y, 24, 24).build());
        }
        if (scrollOffset + visibleRows < modes.size()) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("\u25BC"),
                    b -> { scrollOffset++; rebuildWidgets(); }
            ).bounds(scrollX, TOP_Y + 28, 24, 24).build());
        }

        // Кнопка "Назад"
        this.addRenderableWidget(Button.builder(
                Component.literal("Назад"),
                b -> Minecraft.getInstance().setScreen(parent)
        ).bounds(cx - 50, this.height - 30, 100, 20).build());
    }

    private void selectMode(GameMode mode) {
        ClientState.setSelectedModeId(mode.getId());
        Minecraft.getInstance().setScreen(new MapSelectScreen(mode.getId(), this));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Тёмный фон
        g.fill(0, 0, this.width, this.height, 0xE0101010);

        Font font = this.font;

        // Заголовок
        String title = "ВЫБОР РЕЖИМА";
        int titleW = font.width(title);
        g.drawString(font, title, this.width / 2 - titleW / 2, 15, CSRenderUtil.CS_YELLOW);

        // Подсказка
        g.drawString(font, "Выберите режим, затем карту", this.width / 2 - 60, 32, 0xFFAAAAAA);

        // Индикатор прокрутки
        if (!modes.isEmpty()) {
            int visibleRows = Math.max(1, (this.height - TOP_Y - BOTTOM_PAD) / ROW_H);
            String scrollInfo = (scrollOffset + 1) + "-" + Math.min(scrollOffset + visibleRows, modes.size()) + " / " + modes.size();
            int infoW = font.width(scrollInfo);
            g.drawString(font, scrollInfo, this.width / 2 - infoW / 2, this.height - 45, 0xFF888888);
        }

        // Текущий режим
        String current = "Текущий: " + ClientState.getSelectedModeId();
        g.drawString(font, current, 10, this.height - 15, 0xFF888888);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int visibleRows = Math.max(1, (this.height - TOP_Y - BOTTOM_PAD) / ROW_H);
        int maxScroll = Math.max(0, modes.size() - visibleRows);
        int old = scrollOffset;
        if (delta > 0) scrollOffset = Math.max(0, scrollOffset - 1);
        else if (delta < 0) scrollOffset = Math.min(maxScroll, scrollOffset + 1);
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
