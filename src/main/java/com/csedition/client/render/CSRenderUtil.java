package com.csedition.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/**
 * Утилита процедурной отрисовки элементов в стиле CS.
 * Никаких PNG — всё рисуется через Tesselator/вершины.
 *
 * Эффекты:
 *   - Градиентные полоски (HP, броня, патроны)
 *   - Кнопки с объёмной тенью и подсветкой
 *   - Сетка с угловыми акцентами
 *   - Сканлайны (тёмные горизонтальные линии)
 *   - Свечение (glow) для активных элементов
 */
public final class CSRenderUtil {
    private CSRenderUtil() {}

    // ====================== Цвета CS ======================
    public static final int CS_ORANGE = 0xFFFF8C00;
    public static final int CS_ORANGE_DARK = 0xFFCC6600;
    public static final int CS_RED = 0xFFFF2222;
    public static final int CS_BLUE = 0xFF2266FF;
    public static final int CS_GREEN = 0xFF55FF55;
    public static final int CS_YELLOW = 0xFFFFFF55;
    public static final int CS_DARK_BG = 0xEE0A0A0A;
    public static final int CS_PANEL = 0xCC1A1A1A;
    public static final int CS_PANEL_LIGHT = 0xCC2A2A2A;
    public static final int CS_BORDER = 0xFF3A3A3A;
    public static final int CS_BORDER_ACCENT = 0xFFFF8C00;

    // ====================== Градиент ======================

    /**
     * Рисует горизонтальный градиент (слева направо).
     */
    public static void hGradient(GuiGraphics g, int x, int y, int w, int h, int colorLeft, int colorRight) {
        Matrix4f m = g.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int a1 = (colorLeft >>> 24) & 0xFF;
        int r1 = (colorLeft >>> 16) & 0xFF;
        int gg1 = (colorLeft >>> 8) & 0xFF;
        int b1 = colorLeft & 0xFF;
        int a2 = (colorRight >>> 24) & 0xFF;
        int r2 = (colorRight >>> 16) & 0xFF;
        int gg2 = (colorRight >>> 8) & 0xFF;
        int b2 = colorRight & 0xFF;
        buf.vertex(m, x, y + h, 0).color(r1, gg1, b1, a1).endVertex();
        buf.vertex(m, x + w, y + h, 0).color(r2, gg2, b2, a2).endVertex();
        buf.vertex(m, x + w, y, 0).color(r2, gg2, b2, a2).endVertex();
        buf.vertex(m, x, y, 0).color(r1, gg1, b1, a1).endVertex();
        Tesselator.getInstance().end();
        RenderSystem.disableBlend();
    }

    /**
     * Рисует вертикальный градиент (сверху вниз).
     */
    public static void vGradient(GuiGraphics g, int x, int y, int w, int h, int colorTop, int colorBottom) {
        Matrix4f m = g.pose().last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int a1 = (colorTop >>> 24) & 0xFF;
        int r1 = (colorTop >>> 16) & 0xFF;
        int gg1 = (colorTop >>> 8) & 0xFF;
        int b1 = colorTop & 0xFF;
        int a2 = (colorBottom >>> 24) & 0xFF;
        int r2 = (colorBottom >>> 16) & 0xFF;
        int gg2 = (colorBottom >>> 8) & 0xFF;
        int b2 = colorBottom & 0xFF;
        buf.vertex(m, x, y, 0).color(r1, gg1, b1, a1).endVertex();
        buf.vertex(m, x + w, y, 0).color(r1, gg1, b1, a1).endVertex();
        buf.vertex(m, x + w, y + h, 0).color(r2, gg2, b2, a2).endVertex();
        buf.vertex(m, x, y + h, 0).color(r2, gg2, b2, a2).endVertex();
        Tesselator.getInstance().end();
        RenderSystem.disableBlend();
    }

    // ====================== Полоска HP/Броня ======================

    /**
     * Полоска здоровья/брони в стиле CS: тёмный фон, цветная заливка,
     * сегментация (маленькие штрихи), свечение сверху.
     */
    public static void healthBar(GuiGraphics g, int x, int y, int w, int h, float ratio, int fillColor) {
        ratio = Math.max(0, Math.min(1, ratio));
        // Тёмный фон с рамкой
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        g.fill(x, y, x + w, y + h, 0xFF1A0000);
        // Заливка
        int fillW = (int)(w * ratio);
        if (fillW > 0) {
            // Градиент от яркого к тёмному
            int dark = darken(fillColor, 0.5f);
            hGradient(g, x, y, fillW, h, fillColor, dark);
            // Свечение сверху (1px)
            g.fill(x, y, x + fillW, y + 1, lighten(fillColor, 0.3f));
        }
        // Сегментация (каждые 10% — тонкая вертикальная линия)
        for (int i = 1; i < 10; i++) {
            int sx = x + (w * i / 10);
            g.fill(sx, y, sx + 1, y + h, 0x55000000);
        }
    }

    // ====================== Кнопка CS ======================

    /**
     * Кнопка в стиле CS: тёмная панель, оранжевая рамка при наведении,
     * объёмная тень снизу, угловые акценты.
     */
    public static void csButton(GuiGraphics g, int x, int y, int w, int h, String text, boolean hovered, net.minecraft.client.gui.Font font) {
        // Тень снизу
        g.fill(x + 2, y + h, x + w + 2, y + h + 2, 0xAA000000);
        // Фон — градиент
        int bgTop = hovered ? 0xEE3A2A1A : CS_PANEL;
        int bgBot = hovered ? 0xEE1A0A00 : 0xCC0A0A0A;
        vGradient(g, x, y, w, h, bgTop, bgBot);
        // Рамка
        int borderColor = hovered ? CS_BORDER_ACCENT : CS_BORDER;
        // Угловые акценты (ярче)
        g.fill(x, y, x + 8, y + 1, borderColor);
        g.fill(x + w - 8, y, x + w, y + 1, borderColor);
        g.fill(x, y + h - 1, x + 8, y + h, borderColor);
        g.fill(x + w - 8, y + h - 1, x + w, y + h, borderColor);
        // Боковые стороны
        g.fill(x, y, x + 1, y + h, borderColor);
        g.fill(x + w - 1, y, x + w, y + h, borderColor);
        // Текст
        int textColor = hovered ? CS_ORANGE : 0xFFDDDDDD;
        g.drawCenteredString(font, text, x + w / 2, y + (h - 8) / 2, textColor);
    }

    // ====================== Панель ======================

    /**
     * Панель с заголовком в стиле CS.
     */
    public static void csPanel(GuiGraphics g, int x, int y, int w, int h, String title, net.minecraft.client.gui.Font font) {
        // Тень
        g.fill(x + 3, y + 3, x + w + 3, y + h + 3, 0x88000000);
        // Фон
        g.fill(x, y, x + w, y + h, CS_DARK_BG);
        // Верхняя полоса (оранжевая)
        g.fill(x, y, x + w, y + 2, CS_ORANGE);
        // Заголовок
        if (title != null) {
            g.drawString(font, title, x + 6, y + 6, CS_ORANGE);
        }
        // Рамка
        g.renderOutline(x, y, w, h, CS_BORDER);
    }

    // ====================== Сканлайны ======================

    /**
     * Рисует горизонтальные тёмные линии (эффект CRT-монитора).
     */
    public static void scanlines(GuiGraphics g, int x, int y, int w, int h, int spacing) {
        for (int sy = y; sy < y + h; sy += spacing) {
            g.fill(x, sy, x + w, sy + 1, 0x33000000);
        }
    }

    // ====================== Угловые акценты ======================

    /**
     * Рисует угловые L-образные акценты (как в HUD CS).
     */
    public static void cornerAccents(GuiGraphics g, int x, int y, int w, int h, int size, int color) {
        // TL
        g.fill(x, y, x + size, y + 1, color);
        g.fill(x, y, x + 1, y + size, color);
        // TR
        g.fill(x + w - size, y, x + w, y + 1, color);
        g.fill(x + w - 1, y, x + w, y + size, color);
        // BL
        g.fill(x, y + h - 1, x + size, y + h, color);
        g.fill(x, y + h - size, x + 1, y + h, color);
        // BR
        g.fill(x + w - size, y + h - 1, x + w, y + h, color);
        g.fill(x + w - 1, y + h - size, x + w, y + h, color);
    }

    // ====================== Свечение ======================

    /**
     * Рисует мягкое свечение вокруг точки (имитация glow).
     */
    public static void glow(GuiGraphics g, int cx, int cy, int radius, int color) {
        for (int r = radius; r > 0; r -= 2) {
            int a = ((color >>> 24) & 0xFF) * (radius - r) / radius / 4;
            int c = (a << 24) | (color & 0x00FFFFFF);
            g.fill(cx - r, cy - r, cx + r, cy + r, c);
        }
    }

    // ====================== Утилиты цвета ======================

    public static int darken(int color, float factor) {
        int a = (color >>> 24) & 0xFF;
        int r = (int)(((color >>> 16) & 0xFF) * factor);
        int gg = (int)(((color >>> 8) & 0xFF) * factor);
        int b = (int)((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    public static int lighten(int color, float factor) {
        int a = (color >>> 24) & 0xFF;
        int r = Math.min(255, (int)(((color >>> 16) & 0xFF) + (255 - ((color >>> 16) & 0xFF)) * factor));
        int gg = Math.min(255, (int)(((color >>> 8) & 0xFF) + (255 - ((color >>> 8) & 0xFF)) * factor));
        int b = Math.min(255, (int)((color & 0xFF) + (255 - (color & 0xFF)) * factor));
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}
