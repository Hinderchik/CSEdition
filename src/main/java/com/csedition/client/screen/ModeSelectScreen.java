package com.csedition.client.screen;

import com.csedition.client.ClientState;
import com.csedition.client.render.CSRenderUtil;
import com.csedition.config.ModeConfig;
import com.csedition.data.GameMode;
import com.mojang.blaze3d.vertex.PoseStack;
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
 */
public class ModeSelectScreen extends Screen {
    private final Screen parent;
    private final List<GameMode> modes = new ArrayList<>();
    private int scrollOffset = 0;

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
        int cx = this.width / 2;
        int topY = 40;
        int rowH = 36;
        int visibleRows = Math.min(modes.size(), (this.height - 100) / rowH);

        for (int i = 0; i < visibleRows; i++) {
            int idx = i + scrollOffset;
            if (idx >= modes.size()) break;
            GameMode mode = modes.get(idx);
            int y = topY + i * rowH;
            Button btn = Button.builder(
                    Component.literal(mode.getDisplayName() + (mode.isBuiltIn() ? "" : " *")),
                    b -> selectMode(mode)
            ).bounds(cx - 150, y, 300, 30).build();
            this.addRenderableWidget(btn);
        }

        // Кнопка "Назад"
        this.addRenderableWidget(Button.builder(
                Component.literal("Назад"),
                b -> Minecraft.getInstance().setScreen(parent)
        ).bounds(cx - 50, this.height - 30, 100, 20).build());

        // Кнопки прокрутки
        if (scrollOffset > 0) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("^"),
                    b -> { scrollOffset = Math.max(0, scrollOffset - 1); this.init(); }
            ).bounds(cx + 160, topY, 20, 20).build());
        }
        if (scrollOffset + visibleRows < modes.size()) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("v"),
                    b -> { scrollOffset++; this.init(); }
            ).bounds(cx + 160, topY + 25, 20, 20).build());
        }
    }

    private void selectMode(GameMode mode) {
        ClientState.setSelectedModeId(mode.getId());
        Minecraft.getInstance().setScreen(new MapSelectScreen(mode.getId(), this));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Тёмный фон
        g.fill(0, 0, this.width, this.height, 0xC0000000);
        Font font = this.font;

        // Заголовок
        String title = "ВЫБОР РЕЖИМА";
        g.drawString(font, title, this.width / 2 - font.width(title) / 2, 15, CSRenderUtil.CS_YELLOW);

        // Описание под заголовком
        g.drawString(font, "Выберите режим, затем карту", this.width / 2 - 60, 28, 0xFFAAAAAA);

        // Подсказка о выбранном режиме
        String current = "Текущий: " + ClientState.getSelectedModeId();
        g.drawString(font, current, 10, this.height - 15, 0xFF888888);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
