package com.csedition.client.hud;

import com.csedition.config.CrosshairConfig;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Статический рендерер прицела CS2-стиль.
 * Все методы — статические, без состояния, без аллокаций.
 * Использует только g.fill(...) и простую математику.
 */
public final class CrosshairRenderer {
    private CrosshairRenderer() {}

    /**
     * Рисует прицел в точке (cx, cy).
     * @param spread текущее расхождение (в px), может быть дробным для плавной динамики
     */
    public static void draw(GuiGraphics g, int cx, int cy, CrosshairConfig cfg, float spread) {
        int len = cfg.length;
        int thk = cfg.thickness;
        int gap = cfg.gap;
        int off = gap + thk / 2;

        // Сдвиги от центра с учётом spread
        float s = Math.max(0f, spread);
        int leftStart  = cx - off - len;
        int leftEnd    = cx - off;
        int rightStart = cx + off;
        int rightEnd   = cx + off + len;
        int topStart   = cy - off - len;
        int topEnd     = cy - off;
        int botStart   = cy + off;
        int botEnd     = cy + off + len;

        // Цвета
        int col = cfg.color;
        int alpha = cfg.outlineAlpha & 0xFF;
        int outlineCol = (alpha << 24) | (col & 0x00FFFFFF);

        // Outline (чёрный, на 1px больше каждой линии)
        if (cfg.outline) {
            fillThick(g, leftStart - 1, cy, leftEnd + 1, cy, thk + 2, outlineCol);
            fillThick(g, rightStart - 1, cy, rightEnd + 1, cy, thk + 2, outlineCol);
            if (!cfg.tStyle) {
                fillThick(g, cx, topStart - 1, cx, topEnd + 1, thk + 2, outlineCol);
            }
            fillThick(g, cx, botStart - 1, cx, botEnd + 1, thk + 2, outlineCol);
        }

        // Основные линии цветом
        // Left
        g.fill(leftStart, cy - thk / 2, leftEnd, cy + thk / 2, col);
        // Right
        g.fill(rightStart, cy - thk / 2, rightEnd, cy + thk / 2, col);
        // Top (кроме T-style)
        if (!cfg.tStyle) {
            g.fill(cx - thk / 2, topStart, cx + thk / 2, topEnd, col);
        }
        // Bottom
        g.fill(cx - thk / 2, botStart, cx + thk / 2, botEnd, col);

        // Точка
        if (cfg.dot) {
            int ds = cfg.dotSize;
            int dx = cx - ds / 2;
            int dy = cy - ds / 2;
            g.fill(dx, dy, dx + ds, dy + ds, col);
        }
    }

    /** helper: закрашивает прямоугольник толщиной thk вокруг горизонтальной/вертикальной линии */
    private static void fillThick(GuiGraphics g, int x1, int y1, int x2, int y2, int thk, int color) {
        if (x1 == x2) {
            // вертикальная линия
            g.fill(x1 - thk / 2, y1, x2 + thk / 2, y2 + 1, color);
        } else {
            // горизонтальная линия
            g.fill(x1, y1 - thk / 2, x2 + 1, y2 + thk / 2, color);
        }
    }

    /**
     * Рисует превью прицела (статический или динамический режим имитируется).
     * spread передаётся вызывающим; для превью берём cfg.baseSpread + cfg.moveSpread.
     */
    public static void drawPreview(GuiGraphics g, int cx, int cy, CrosshairConfig cfg, float previewSpread) {
        draw(g, cx, cy, cfg, Math.max(0f, previewSpread));
    }
}
