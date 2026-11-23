package com.csedition.client.screen;

import com.csedition.client.render.CSRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

/**
 * Кнопка в стиле CS2 для главного меню.
 * Плоский фон, 1px обводка, ховер/акцент состояния.
 */
public class CsButton extends Button {

    private final boolean accent;
    private boolean hovered = false;

    public CsButton(int x, int y, int width, int height, Component message,
                     OnPress onPress, boolean accent) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.accent = accent;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hovered = isMouseOver(mouseX, mouseY);

        int bgColor, borderColor, textColor;
        int offsetX = 0, offsetY = 0;

        if (accent) {
            // Акцентная кнопка (ИГРАТЬ)
            if (hovered) {
                bgColor = 0xFFFFD166;
            } else {
                bgColor = CSRenderUtil.COLOR_ACCENT; // 0xFFE8B84B
            }
            borderColor = CSRenderUtil.COLOR_ACCENT;
            textColor = 0xFF0B0E12;
        } else {
            // Обычная кнопка
            if (hovered) {
                bgColor = 0xCC1A1F26;
                borderColor = CSRenderUtil.COLOR_ACCENT; // 0xFFE8B84B
            } else {
                bgColor = 0x99101418;
                borderColor = CSRenderUtil.COLOR_BORDER; // 0xFF2A2F36
            }
            textColor = CSRenderUtil.COLOR_TEXT_WHITE; // 0xFFE6EAF0
        }

        // Нажатие — смещение текста
        if (isMouseOver(mouseX, mouseY) && Minecraft.getInstance().mouseHandler.isLeftPressed()) {
            offsetY = 1;
        }

        // Фон
        g.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
        // Обводка 1px
        g.fill(getX(), getY(), getX() + width, getY() + 1, borderColor);
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor);
        g.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);
        g.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor);

        // Текст
        var font = Minecraft.getInstance().font;
        int textX = getX() + (width - font.width(getMessage())) / 2 + offsetX;
        int textY = getY() + (height - 8) / 2 + offsetY;
        g.drawString(font, getMessage(), textX, textY, textColor, false);
    }
}