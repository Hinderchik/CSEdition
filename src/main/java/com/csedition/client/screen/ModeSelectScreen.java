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
 * Экран выбора режима в стиле CS Mobile / BLOCKPOST mobile.
 *
 * Визуальный стиль:
 *   - Сетка карточек 2 колонки (адаптивно по высоте окна)
 *   - Каждая карточка: верхняя цветная полоса (по типу режима),
 *     крупное название, описание, бейджи свойств, нижняя строка со статами
 *   - Hover: подсветка рамки + подсказка "НАЖМИТЕ"
 *   - Selected: внутреннее свечение цветом режима
 *   - Скролл: колесо, стрелки, draggable scrollbar справа
 *
 * Цвета режимов:
 *   classic     → оранжевый
 *   deathmatch  → красный
 *   gungame     → жёлтый
 *   pistol_only → зелёный
 *   custom      → голубой
 */
public class ModeSelectScreen extends Screen {
    private final Screen parent;
    private final List<GameMode> modes = new ArrayList<>();

    // ===== Layout =====
    private static final int COLS = 2;
    private static final int CARD_W = 270;
    private static final int CARD_H = 175;
    private static final int GAP_X = 18;
    private static final int GAP_Y = 16;
    private static final int TOP_PAD = 105;
    private static final int BOTTOM_PAD = 75;

    // ===== Scroll =====
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int visibleRows = 0;

    // ===== Hover tracking (для подсказки "НАЖМИТЕ") =====
    private int hoveredIndex = -1;

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
        computeLayout();

        int gridWidth = COLS * CARD_W + (COLS - 1) * GAP_X;
        int startX = (this.width - gridWidth) / 2;
        int startY = TOP_PAD - scrollOffset;

        // Карточки — без Button (свой рендер и своя обработка кликов)
        // Только стрелки прокрутки и кнопка "Назад" — Button
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
        int totalRows = (modes.size() + COLS - 1) / COLS;
        maxScroll = Math.max(0, (totalRows - visibleRows) * (CARD_H + GAP_Y));
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    private void selectMode(GameMode mode) {
        ClientState.setSelectedModeId(mode.getId());
        Minecraft.getInstance().setScreen(new MapSelectScreen(mode.getId(), this));
    }

    // ====================== Цвета режимов ======================

    private static int colorForMode(String modeId) {
        if (modeId == null) return 0xFF66AAFF;
        return switch (modeId.toLowerCase()) {
            case "classic" -> CSRenderUtil.CS_ORANGE;
            case "deathmatch" -> CSRenderUtil.CS_RED;
            case "gungame" -> CSRenderUtil.CS_YELLOW;
            case "pistol_only" -> CSRenderUtil.CS_GREEN;
            default -> 0xFF66AAFF;
        };
    }

    private static String formatTime(int seconds) {
        if (seconds <= 0) return "∞";
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d", m, s);
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static int badgeWidth(Font font, String text) {
        return font.width(text) + 14;
    }

    // ====================== Бейдж ======================

    private void drawBadge(GuiGraphics g, Font font, int x, int y, String text, int color) {
        int w = badgeWidth(font, text);
        int h = 13;
        // Затемнённый фон
        g.fill(x, y, x + w, y + h, withAlpha(color, 50));
        // Рамка
        g.renderOutline(x, y, w, h, color);
        // Угловые акценты
        g.fill(x, y, x + 4, y + 1, color);
        g.fill(x, y, x + 1, y + 4, color);
        g.fill(x + w - 4, y, x + w, y + 1, color);
        g.fill(x + w - 1, y, x + w, y + 4, color);
        // Текст
        g.drawCenteredString(font, text, x + w / 2, y + 3, color);
    }

    // ====================== Карточка режима ======================

    private void drawModeCard(GuiGraphics g, GameMode mode, int x, int y, boolean hovered, boolean selected) {
        int accent = colorForMode(mode.getId());

        // Тень
        g.fill(x + 3, y + 4, x + CARD_W + 3, y + CARD_H + 4, 0x66000000);
        // Фон — вертикальный градиент
        int bgTop = hovered ? 0xFF1C1C1C : (selected ? 0xFF181410 : 0xFF101010);
        int bgBot = hovered ? 0xFF0A0808 : (selected ? 0xFF0A0805 : 0xFF060606);
        CSRenderUtil.vGradient(g, x, y, CARD_W, CARD_H, bgTop, bgBot);
        // Сканлайны (очень слабые, чтобы добавить текстуры)
        CSRenderUtil.scanlines(g, x, y, CARD_W, CARD_H, 4);

        // Рамка (толще на hover/selected)
        int borderColor = (hovered || selected) ? accent : CSRenderUtil.CS_BORDER;
        g.renderOutline(x, y, CARD_W, CARD_H, borderColor);

        // Верхняя цветная полоса
        g.fill(x, y, x + CARD_W, y + 3, accent);

        // Угловые акценты (тактический стиль)
        g.fill(x, y, x + 14, y + 1, accent);
        g.fill(x, y, x + 1, y + 14, accent);
        g.fill(x + CARD_W - 14, y, x + CARD_W, y + 1, accent);
        g.fill(x + CARD_W - 1, y, x + CARD_W, y + 14, accent);
        // Нижние углы — более тонкие акценты
        g.fill(x, y + CARD_H - 1, x + 8, y + CARD_H, accent);
        g.fill(x + CARD_W - 8, y + CARD_H - 1, x + CARD_W, y + CARD_H, accent);

        // Внутренняя подсветка при selected
        if (selected) {
            g.renderOutline(x + 3, y + 3, CARD_W - 6, CARD_H - 6, withAlpha(accent, 180));
        }

        Font font = this.font;

        // Название режима
        String name = mode.getDisplayName().toUpperCase();
        g.drawCenteredString(font, name, x + CARD_W / 2, y + 14, accent);

        // Сепаратор под названием
        int sepW = 50;
        int sepX = x + CARD_W / 2 - sepW / 2;
        g.fill(sepX, y + 28, sepX + sepW, y + 29, 0xFF555555);
        // Декоративные точки по краям сепаратора
        g.fill(sepX - 4, y + 28, sepX - 3, y + 29, accent);
        g.fill(sepX + sepW + 3, y + 28, sepX + sepW + 4, y + 29, accent);

        // Описание (с троеточием если не влезает)
        String desc = mode.getDescription();
        int maxDescW = CARD_W - 20;
        if (font.width(desc) > maxDescW) {
            while (desc.length() > 4 && font.width(desc + "...") > maxDescW) {
                desc = desc.substring(0, desc.length() - 1);
            }
            desc = desc + "...";
        }
        g.drawCenteredString(font, desc, x + CARD_W / 2, y + 36, 0xFFAAAAAA);

        // ===== Бейджи свойств =====
        List<String> badgeTexts = new ArrayList<>();
        List<Integer> badgeColors = new ArrayList<>();
        if (mode.getRoundTimeSeconds() > 0) {
            String t = "TIME " + formatTime(mode.getRoundTimeSeconds());
            badgeTexts.add(t);
            badgeColors.add(0xFF5599CC);
        }
        if (mode.isAllowBuy()) {
            badgeTexts.add("BUY");
            badgeColors.add(CSRenderUtil.CS_GREEN);
        }
        if (mode.getBuyTimeSeconds() > 0 && mode.isAllowBuy()) {
            String t = "BUY " + mode.getBuyTimeSeconds() + "s";
            badgeTexts.add(t);
            badgeColors.add(0xFFCC88FF);
        }
        if (mode.isRespawn()) {
            badgeTexts.add("RESPAWN");
            badgeColors.add(CSRenderUtil.CS_RED);
        }
        if (!badgeTexts.isEmpty()) {
            int totalW = 0;
            for (String s : badgeTexts) totalW += badgeWidth(font, s) + 5;
            totalW -= 5;
            int badgeX = x + (CARD_W - totalW) / 2;
            int badgeY = y + 62;
            for (int i = 0; i < badgeTexts.size(); i++) {
                drawBadge(g, font, badgeX, badgeY, badgeTexts.get(i), badgeColors.get(i));
                badgeX += badgeWidth(font, badgeTexts.get(i)) + 5;
            }
        }

        // ===== Нижняя статистика =====
        int statsY = y + CARD_H - 32;
        // Разделитель
        g.fill(x + 14, statsY - 6, x + CARD_W - 14, statsY - 5, 0xFF333333);

        int colW = (CARD_W - 28) / 3;
        // Значения
        g.drawCenteredString(font, "$" + mode.getStartMoney(),
                x + 14 + colW / 2, statsY, CSRenderUtil.CS_GREEN);
        g.drawCenteredString(font, "+$" + mode.getKillReward(),
                x + 14 + colW + colW / 2, statsY, 0xFFCCCCCC);
        g.drawCenteredString(font, "+$" + mode.getRoundWinReward(),
                x + 14 + 2 * colW + colW / 2, statsY, CSRenderUtil.CS_ORANGE);
        // Подписи
        int labelsY = statsY + 10;
        g.drawCenteredString(font, "START", x + 14 + colW / 2, labelsY, 0xFF666666);
        g.drawCenteredString(font, "KILL", x + 14 + colW + colW / 2, labelsY, 0xFF666666);
        g.drawCenteredString(font, "WIN", x + 14 + 2 * colW + colW / 2, labelsY, 0xFF666666);

        // Подсказка "НАЖМИТЕ" при наведении
        if (hovered) {
            String hint = "▸ НАЖМИТЕ";
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
        // Лёгкий виньет по краям (радиальная имитация)
        CSRenderUtil.vGradient(g, 0, 60, this.width, 80,
                CSRenderUtil.withAlpha(0xFF1A0A00, 60), 0x00000000);
        CSRenderUtil.vGradient(g, 0, this.height - 130, this.width, 80,
                0x00000000, CSRenderUtil.withAlpha(0xFF000000, 100));

        // Верхняя панель
        g.fill(0, 0, this.width, 60, 0xEE0A0A0A);
        g.fill(0, 58, this.width, 60, CSRenderUtil.CS_ORANGE);
        // Нижняя панель
        g.fill(0, this.height - 50, this.width, this.height, 0xEE0A0A0A);
        g.fill(0, this.height - 50, this.width, this.height - 49, CSRenderUtil.CS_ORANGE);

        Font font = this.font;

        // Заголовок
        g.drawCenteredString(font, "ВЫБОР РЕЖИМА", this.width / 2, 12, CSRenderUtil.CS_ORANGE);
        g.drawCenteredString(font, "Выберите режим → затем карту", this.width / 2, 32, 0xFFAAAAAA);
        CSRenderUtil.cornerAccents(g, 0, 0, this.width, 60, 16, CSRenderUtil.CS_ORANGE);

        // Карточки
        int gridWidth = COLS * CARD_W + (COLS - 1) * GAP_X;
        int startX = (this.width - gridWidth) / 2;
        int startY = TOP_PAD - scrollOffset;
        hoveredIndex = -1;
        for (int i = 0; i < modes.size(); i++) {
            int row = i / COLS;
            int col = i % COLS;
            int x = startX + col * (CARD_W + GAP_X);
            int y = startY + row * (CARD_H + GAP_Y);
            GameMode mode = modes.get(i);
            boolean hov = mouseX >= x && mouseX <= x + CARD_W
                       && mouseY >= y && mouseY <= y + CARD_H;
            boolean sel = mode.getId().equalsIgnoreCase(ClientState.getSelectedModeId());
            if (hov) hoveredIndex = i;
            // Пропуск невидимых
            if (y + CARD_H < TOP_PAD || y > this.height - BOTTOM_PAD) continue;
            drawModeCard(g, mode, x, y, hov, sel);
        }

        // Scrollbar справа (после стрелок)
        if (maxScroll > 0) {
            int sbX = startX + gridWidth + 44;
            int sbY = TOP_PAD;
            int sbH = this.height - TOP_PAD - BOTTOM_PAD;
            // Дорожка
            g.fill(sbX, sbY, sbX + 4, sbY + sbH, 0xFF1A1A1A);
            g.renderOutline(sbX, sbY, 4, sbH, 0xFF333333);
            // Ползунок
            int totalRows = (modes.size() + COLS - 1) / COLS;
            int thumbH = Math.max(24, sbH * visibleRows / totalRows);
            int thumbY = sbY + (sbH - thumbH) * scrollOffset / maxScroll;
            g.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, CSRenderUtil.CS_ORANGE);
            g.fill(sbX, thumbY, sbX + 4, thumbY + 2, 0xFFFFAA66);
        }

        // Пустое состояние
        if (modes.isEmpty()) {
            g.drawCenteredString(font, "Нет доступных режимов",
                    this.width / 2, this.height / 2, 0xFFFF6666);
        } else {
            // Подсказка снизу
            g.drawCenteredString(font, "ENTER — Выбрать  |  ESC — Назад  |  Колесо — Скролл",
                    this.width / 2, this.height - 30, 0xFF888888);
            // Счётчик
            String count = modes.size() + " режим" + pluralRu(modes.size());
            g.drawString(font, count, 12, this.height - 30, 0xFF666666);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private static String pluralRu(int n) {
        int mod10 = n % 10;
        int mod100 = n % 100;
        if (mod10 == 1 && mod100 != 11) return "";
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return "а";
        return "ов";
    }

    // ====================== Input ======================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int gridWidth = COLS * CARD_W + (COLS - 1) * GAP_X;
        int startX = (this.width - gridWidth) / 2;
        int startY = TOP_PAD - scrollOffset;

        // Карточки
        for (int i = 0; i < modes.size(); i++) {
            int row = i / COLS;
            int col = i % COLS;
            int x = startX + col * (CARD_W + GAP_X);
            int y = startY + row * (CARD_H + GAP_Y);
            if (mouseX >= x && mouseX <= x + CARD_W
                    && mouseY >= y && mouseY <= y + CARD_H) {
                selectMode(modes.get(i));
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
                int totalRows = (modes.size() + COLS - 1) / COLS;
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
        // ENTER на наведённой карточке
        if (keyCode == 257 || keyCode == 335) { // ENTER / NUMPAD_ENTER
            if (hoveredIndex >= 0 && hoveredIndex < modes.size()) {
                selectMode(modes.get(hoveredIndex));
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
