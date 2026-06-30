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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Экран выбора карты в стиле CS Mobile / BLOCKPOST mobile.
 *
 * Визуальный стиль:
 *   - Горизонтальные карточки 2xN (адаптивно по высоте)
 *   - Каждая карточка: слева процедурный мини-превью (зоны T/CT, сетка, брекеты),
 *     справа инфо: имя, ID, бейдж режима
 *   - Hover: подсветка рамки цветом режима + "PRESS TO DEPLOY"
 *   - Скролл: колесо, стрелки, draggable scrollbar
 *
 * Карты приходят с сервера через PacketMapList и хранятся в ClientState.mapList.
 * При клике отправляется PacketSelectMap на сервер.
 */
public class MapSelectScreen extends Screen {
    // ===== Layout =====
    private static final int COLS = 2;
    private static final int CARD_W = 350;
    private static final int CARD_H = 120;
    private static final int GAP_X = 16;
    private static final int GAP_Y = 14;
    private static final int TOP_PAD = 105;
    private static final int BOTTOM_PAD = 75;

    // ===== Превью =====
    private static final int PREVIEW_W = 110;
    private static final int PREVIEW_H = 96;

    // ===== Scroll =====
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int visibleRows = 0;
    private int hoveredIndex = -1;

    private List<PacketMapList.MapEntry> maps;
    private final String modeId;
    private final Screen parent;

    public MapSelectScreen() {
        this(null, null);
    }

    public MapSelectScreen(String modeId) {
        this(modeId, null);
    }

    public MapSelectScreen(String modeId, Screen parent) {
        super(Component.literal("Выбор карты"));
        this.modeId = modeId;
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        loadMaps();
        rebuildWidgets();
    }

    private void loadMaps() {
        List<PacketMapList.MapEntry> all = ClientState.getMapList();
        if (all == null) {
            this.maps = Collections.emptyList();
        } else if (modeId == null || modeId.isEmpty() || MapData.MODE_ANY.equalsIgnoreCase(modeId)) {
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
    }

    protected void rebuildWidgets() {
        clearWidgets();
        if (maps == null) return;
        computeLayout();

        int gridWidth = COLS * CARD_W + (COLS - 1) * GAP_X;
        int startX = (this.width - gridWidth) / 2;

        // Стрелки прокрутки
        int scrollX = startX + gridWidth + 10;
        if (scrollOffset > 0) {
            this.addRenderableWidget(Button.builder(Component.literal("\u25B2"), b -> {
                scrollOffset = Math.max(0, scrollOffset - (CARD_H + GAP_Y));
                rebuildWidgets();
            }).bounds(scrollX, TOP_PAD, 26, 26).build());
        }
        if (scrollOffset < maxScroll) {
            this.addRenderableWidget(Button.builder(Component.literal("\u25BC"), b -> {
                scrollOffset = Math.min(maxScroll, scrollOffset + (CARD_H + GAP_Y));
                rebuildWidgets();
            }).bounds(scrollX, TOP_PAD + 30, 26, 26).build());
        }

        // Назад
        this.addRenderableWidget(Button.builder(
                Component.literal("НАЗАД"),
                b -> Minecraft.getInstance().setScreen(parent)
        ).bounds(this.width / 2 - 60, this.height - 36, 120, 24).build());
    }

    private void computeLayout() {
        visibleRows = Math.max(1, (this.height - TOP_PAD - BOTTOM_PAD) / (CARD_H + GAP_Y));
        int totalRows = (maps.size() + COLS - 1) / COLS;
        maxScroll = Math.max(0, (totalRows - visibleRows) * (CARD_H + GAP_Y));
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    private void selectMap(String mapId) {
        CSEditionMod.LOGGER.info("[CS-Edition] Selecting map: {}", mapId);
        CSPackets.CHANNEL.sendToServer(new PacketSelectMap(mapId));
        Minecraft.getInstance().setScreen(null);
    }

    // ====================== Утилиты ======================

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static int colorForMode(String modeId) {
        if (modeId == null || modeId.isEmpty()) return CSRenderUtil.CS_ORANGE;
        return switch (modeId.toLowerCase()) {
            case "classic" -> CSRenderUtil.CS_ORANGE;
            case "deathmatch" -> CSRenderUtil.CS_RED;
            case "gungame" -> CSRenderUtil.CS_YELLOW;
            case "pistol_only" -> CSRenderUtil.CS_GREEN;
            case "any" -> 0xFFAAAAAA;
            default -> 0xFF66AAFF;
        };
    }

    private static int badgeWidth(Font font, String text) {
        return font.width(text) + 14;
    }

    private void drawBadge(GuiGraphics g, Font font, int x, int y, String text, int color) {
        int w = badgeWidth(font, text);
        int h = 14;
        g.fill(x, y, x + w, y + h, withAlpha(color, 50));
        g.renderOutline(x, y, w, h, color);
        g.fill(x, y, x + 4, y + 1, color);
        g.fill(x, y, x + 1, y + 4, color);
        g.fill(x + w - 4, y, x + w, y + 1, color);
        g.fill(x + w - 1, y, x + w, y + 4, color);
        g.drawCenteredString(font, text, x + w / 2, y + 3, color);
    }

    // ====================== Мини-превью карты ======================

    /**
     * Процедурный мини-превью: сетка + зоны T/CT + центральный маркер + угловые брекеты.
     * Никаких PNG — всё рисуется вершинами и fill'ами.
     */
    private void drawMapPreview(GuiGraphics g, int x, int y, int w, int h, int modeColor, String mapId) {
        // Фон — диагональный градиент (имитация "топ-даун" карты)
        CSRenderUtil.vGradient(g, x, y, w, h, withAlpha(modeColor, 35), 0xFF050505);
        // Лёгкий шум сканлайнами
        CSRenderUtil.scanlines(g, x, y, w, h, 4);

        // Сетка
        int gridStep = 10;
        for (int gx = x; gx < x + w; gx += gridStep) {
            g.fill(gx, y, gx + 1, y + h, 0x33000000);
        }
        for (int gy = y; gy < y + h; gy += gridStep) {
            g.fill(x, gy, x + w, gy + 1, 0x33000000);
        }

        // Зона T (слева, красная подсветка)
        int tZoneW = w / 3;
        CSRenderUtil.vGradient(g, x, y, tZoneW, h, withAlpha(CSRenderUtil.CS_RED, 70), 0x00000000);
        // Зона CT (справа, синяя подсветка)
        int ctZoneW = w / 3;
        int ctX = x + w - ctZoneW;
        CSRenderUtil.vGradient(g, ctX, y, ctZoneW, h, withAlpha(CSRenderUtil.CS_BLUE, 70), 0x00000000);
        // Центральная граница зон
        g.fill(x + w / 2, y, x + w / 2 + 1, y + h, withAlpha(modeColor, 140));

        // Метки T / CT
        Font font = this.font;
        g.drawString(font, "T", x + 4, y + 3, CSRenderUtil.CS_RED);
        g.drawString(font, "CT", x + w - font.width("CT") - 3, y + 3, CSRenderUtil.CS_BLUE);

        // Центральный маркер (точка спавна / объекта)
        int cx = x + w / 2;
        int cy = y + h / 2;
        g.fill(cx - 1, cy - 4, cx + 1, cy + 4, modeColor);
        g.fill(cx - 4, cy - 1, cx + 4, cy + 1, modeColor);
        g.fill(cx, cy, cx + 1, cy + 1, 0xFFFFFFFF);

        // Имитация маршрутов (зигзаг линия в центре)
        int pathY = y + h - 14;
        int segW = 6;
        for (int px = x + 6; px < x + w - 6; px += segW * 2) {
            g.fill(px, pathY, px + segW, pathY + 1, withAlpha(modeColor, 100));
        }

        // Угловые брекеты (тактический стиль)
        int bSize = 7;
        // TL
        g.fill(x, y, x + bSize, y + 1, modeColor);
        g.fill(x, y, x + 1, y + bSize, modeColor);
        // TR
        g.fill(x + w - bSize, y, x + w, y + 1, modeColor);
        g.fill(x + w - 1, y, x + w, y + bSize, modeColor);
        // BL
        g.fill(x, y + h - 1, x + bSize, y + h, modeColor);
        g.fill(x, y + h - bSize, x + 1, y + h, modeColor);
        // BR
        g.fill(x + w - bSize, y + h - 1, x + w, y + h, modeColor);
        g.fill(x + w - 1, y + h - bSize, x + w, y + h, modeColor);

        // Имя карты внизу (мелким, полупрозрачно)
        String label = mapId == null ? "" : mapId;
        if (label.length() > 12) label = label.substring(0, 12) + "...";
        g.drawString(font, label, x + 5, y + h - 9, 0xAAFFFFFF);
    }

    // ====================== Карточка карты ======================

    private void drawMapCard(GuiGraphics g, PacketMapList.MapEntry entry, int x, int y,
                              boolean hovered, boolean selected) {
        int accent = colorForMode(entry.modeId);

        // Тень
        g.fill(x + 3, y + 4, x + CARD_W + 3, y + CARD_H + 4, 0x66000000);

        // Фон — вертикальный градиент
        int bgTop = hovered ? 0xFF1C1C1C : (selected ? 0xFF181410 : 0xFF101010);
        int bgBot = hovered ? 0xFF0A0808 : (selected ? 0xFF0A0805 : 0xFF060606);
        CSRenderUtil.vGradient(g, x, y, CARD_W, CARD_H, bgTop, bgBot);
        CSRenderUtil.scanlines(g, x, y, CARD_W, CARD_H, 4);

        // Рамка
        int borderColor = (hovered || selected) ? accent : CSRenderUtil.CS_BORDER;
        g.renderOutline(x, y, CARD_W, CARD_H, borderColor);

        // Верхняя цветная полоса
        g.fill(x, y, x + CARD_W, y + 3, accent);

        // Угловые акценты
        g.fill(x, y, x + 14, y + 1, accent);
        g.fill(x, y, x + 1, y + 14, accent);
        g.fill(x + CARD_W - 14, y, x + CARD_W, y + 1, accent);
        g.fill(x + CARD_W - 1, y, x + CARD_W, y + 14, accent);
        g.fill(x, y + CARD_H - 1, x + 8, y + CARD_H, accent);
        g.fill(x + CARD_W - 8, y + CARD_H - 1, x + CARD_W, y + CARD_H, accent);

        // Внутренняя подсветка при selected
        if (selected) {
            g.renderOutline(x + 3, y + 3, CARD_W - 6, CARD_H - 6, withAlpha(accent, 180));
        }

        // ===== Превью слева =====
        int previewX = x + 8;
        int previewY = y + 8;
        // Рамка превью
        g.renderOutline(previewX - 1, previewY - 1, PREVIEW_W + 2, PREVIEW_H + 2, accent);
        drawMapPreview(g, previewX, previewY, PREVIEW_W, PREVIEW_H, accent, entry.id);

        // ===== Инфо справа =====
        Font font = this.font;
        int infoX = previewX + PREVIEW_W + 14;
        int infoW = x + CARD_W - infoX - 12;

        // Название карты (крупно)
        String name = entry.displayName == null ? entry.id : entry.displayName;
        if (font.width(name) > infoW) {
            while (name.length() > 4 && font.width(name + "...") > infoW) {
                name = name.substring(0, name.length() - 1);
            }
            name = name + "...";
        }
        g.drawString(font, name, infoX, y + 14, accent);

        // Сепаратор
        g.fill(infoX, y + 30, infoX + Math.min(60, infoW), y + 31, 0xFF555555);

        // ID карты (мелко, серым)
        String idText = "ID: " + entry.id;
        if (font.width(idText) > infoW) {
            while (idText.length() > 8 && font.width(idText + "...") > infoW) {
                idText = idText.substring(0, idText.length() - 1);
            }
            idText = idText + "...";
        }
        g.drawString(font, idText, infoX, y + 38, 0xFF888888);

        // Бейдж режима
        String modeText = (entry.modeId == null || entry.modeId.isEmpty()
                || MapData.MODE_ANY.equalsIgnoreCase(entry.modeId))
                ? "ALL MODES" : entry.modeId.toUpperCase();
        drawBadge(g, font, infoX, y + 55, modeText, accent);

        // Нижняя строка статуса
        int bottomY = y + CARD_H - 22;
        g.fill(infoX, bottomY - 6, infoX + infoW, bottomY - 5, 0xFF333333);
        g.drawString(font, "▸ READY TO DEPLOY", infoX, bottomY, 0xFF666666);

        // Hover-подсказка
        if (hovered) {
            String hint = "▸ DEPLOY";
            int hw = font.width(hint);
            g.drawString(font, hint, x + CARD_W - hw - 10, y + CARD_H - 12, accent);
        }
    }

    // ====================== Render ======================

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Фон
        g.fill(0, 0, this.width, this.height, 0xFF030303);
        CSRenderUtil.scanlines(g, 0, 0, this.width, this.height, 3);
        CSRenderUtil.vGradient(g, 0, 60, this.width, 80,
                CSRenderUtil.withAlpha(0xFF1A0A00, 60), 0x00000000);
        CSRenderUtil.vGradient(g, 0, this.height - 130, this.width, 80,
                0x00000000, CSRenderUtil.withAlpha(0xFF000000, 100));

        // Верх / Низ панели
        g.fill(0, 0, this.width, 60, 0xEE0A0A0A);
        g.fill(0, 58, this.width, 60, CSRenderUtil.CS_ORANGE);
        g.fill(0, this.height - 50, this.width, this.height, 0xEE0A0A0A);
        g.fill(0, this.height - 50, this.width, this.height - 49, CSRenderUtil.CS_ORANGE);

        Font font = this.font;

        // Заголовок
        String title = (modeId != null && !modeId.isEmpty()
                && !MapData.MODE_ANY.equalsIgnoreCase(modeId))
                ? "ВЫБОР КАРТЫ — " + modeId.toUpperCase()
                : "ВЫБОР КАРТЫ";
        g.drawCenteredString(font, title, this.width / 2, 12, CSRenderUtil.CS_ORANGE);
        g.drawCenteredString(font, "Нажмите на карту для запуска матча", this.width / 2, 32, 0xFFAAAAAA);
        CSRenderUtil.cornerAccents(g, 0, 0, this.width, 60, 16, CSRenderUtil.CS_ORANGE);

        // Карточки
        int gridWidth = COLS * CARD_W + (COLS - 1) * GAP_X;
        int startX = (this.width - gridWidth) / 2;
        int startY = TOP_PAD - scrollOffset;
        hoveredIndex = -1;

        if (maps == null || maps.isEmpty()) {
            g.drawCenteredString(font, "Нет карт для этого режима",
                    this.width / 2, this.height / 2 - 10, 0xFFFF6666);
            g.drawCenteredString(font, "Добавьте карту: /cs map add <id> <name> [mode]",
                    this.width / 2, this.height / 2 + 10, 0xFF888888);
        } else {
            for (int i = 0; i < maps.size(); i++) {
                int row = i / COLS;
                int col = i % COLS;
                int x = startX + col * (CARD_W + GAP_X);
                int y = startY + row * (CARD_H + GAP_Y);
                PacketMapList.MapEntry entry = maps.get(i);
                boolean hov = mouseX >= x && mouseX <= x + CARD_W
                           && mouseY >= y && mouseY <= y + CARD_H;
                boolean sel = entry.id.equalsIgnoreCase(ClientState.getCurrentMapId());
                if (hov) hoveredIndex = i;
                if (y + CARD_H < TOP_PAD || y > this.height - BOTTOM_PAD) continue;
                drawMapCard(g, entry, x, y, hov, sel);
            }

            // Scrollbar
            if (maxScroll > 0) {
                int sbX = startX + gridWidth + 44;
                int sbY = TOP_PAD;
                int sbH = this.height - TOP_PAD - BOTTOM_PAD;
                g.fill(sbX, sbY, sbX + 4, sbY + sbH, 0xFF1A1A1A);
                g.renderOutline(sbX, sbY, 4, sbH, 0xFF333333);
                int totalRows = (maps.size() + COLS - 1) / COLS;
                int thumbH = Math.max(24, sbH * visibleRows / totalRows);
                int thumbY = sbY + (sbH - thumbH) * scrollOffset / maxScroll;
                g.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, CSRenderUtil.CS_ORANGE);
                g.fill(sbX, thumbY, sbX + 4, thumbY + 2, 0xFFFFAA66);
            }

            // Подсказка
            g.drawCenteredString(font, "ENTER — Запустить  |  ESC — Назад  |  Колесо — Скролл",
                    this.width / 2, this.height - 30, 0xFF888888);
            String count = maps.size() + " карт" + pluralRu(maps.size());
            g.drawString(font, count, 12, this.height - 30, 0xFF666666);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private static String pluralRu(int n) {
        int mod10 = n % 10;
        int mod100 = n % 100;
        if (mod10 == 1 && mod100 != 11) return "а";
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return "ы";
        return "";
    }

    // ====================== Input ======================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (maps == null || maps.isEmpty()) return super.mouseClicked(mouseX, mouseY, button);

        int gridWidth = COLS * CARD_W + (COLS - 1) * GAP_X;
        int startX = (this.width - gridWidth) / 2;
        int startY = TOP_PAD - scrollOffset;

        // Карточки
        for (int i = 0; i < maps.size(); i++) {
            int row = i / COLS;
            int col = i % COLS;
            int x = startX + col * (CARD_W + GAP_X);
            int y = startY + row * (CARD_H + GAP_Y);
            if (mouseX >= x && mouseX <= x + CARD_W
                    && mouseY >= y && mouseY <= y + CARD_H) {
                selectMap(maps.get(i).id);
                return true;
            }
        }

        // Scrollbar drag
        if (maxScroll > 0) {
            int sbX = startX + gridWidth + 44;
            int sbY = TOP_PAD;
            int sbH = this.height - TOP_PAD - BOTTOM_PAD;
            if (mouseX >= sbX - 4 && mouseX <= sbX + 8
                    && mouseY >= sbY && mouseY <= sbY + sbH) {
                int totalRows = (maps.size() + COLS - 1) / COLS;
                int thumbH = Math.max(24, sbH * visibleRows / totalRows);
                int newThumbCenter = (int) (mouseY - TOP_PAD);
                int newScroll = (int) ((double) (newThumbCenter - thumbH / 2) / (sbH - thumbH) * maxScroll);
                scrollOffset = Math.max(0, Math.min(maxScroll, newScroll));
                rebuildWidgets();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll <= 0) return false;
        int step = (CARD_H + GAP_Y);
        int old = scrollOffset;
        if (delta > 0) scrollOffset = Math.max(0, scrollOffset - step);
        else if (delta < 0) scrollOffset = Math.min(maxScroll, scrollOffset + step);
        if (old != scrollOffset) {
            rebuildWidgets();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // ENTER / NUMPAD_ENTER
            if (maps != null && hoveredIndex >= 0 && hoveredIndex < maps.size()) {
                selectMap(maps.get(hoveredIndex).id);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
