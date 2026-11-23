package com.csedition.client.screen;

import com.csedition.config.CrosshairConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * Экран настроек прицела CS2-стиль.
 * Две колонки: слева настройки со скроллом, справа превью.
 * Поддерживает возврат в главное меню (fromMenu=true) или в игру (fromMenu=false).
 */
public class CrosshairConfigScreen extends Screen {

    private static final int LEFT_W = 300;
    private static final int RIGHT_X = 320;
    private static final int TOP_PAD = 50;
    private static final int BOT_PAD = 50;
    private static final int PAD = 12;

    private CrosshairConfig config;
    private CrosshairConfig backup;
    private CrosshairConfig temp;
    private boolean dynamicMode;

    // Скролл
    private int scrollOffset = 0;
    private int maxScroll = 0;

    // Слайдеры
    private FloatSlider lengthSlider, thicknessSlider, gapSlider, dotSizeSlider, outlineAlphaSlider;
    private FloatSlider baseSpreadSlider, moveSpreadSlider, sprintSpreadSlider, airSpreadSlider;
    private FloatSlider shotSpreadSlider, shotRecoverySlider, lerpSpeedSlider;
    private EditBox colorHexEdit;
    private Button btnStatic, btnDynamic;

    // Откуда открыт экран: true = из главного меню, false = из игры (F8)
    private final boolean fromMenu;

    public CrosshairConfigScreen(boolean fromMenu) {
        super(Component.literal("Настройки прицела"));
        this.fromMenu = fromMenu;
    }

    @Override
    protected void init() {
        super.init();
        config = com.csedition.client.hud.CrosshairOverlay.getConfig();
        if (config == null) config = new CrosshairConfig();
        backup = config.copy();
        temp = config.copy();
        dynamicMode = temp.dynamic;
        buildWidgets();
        computeLayout();
    }

    private void buildWidgets() {
        clearWidgets();
        int y = TOP_PAD + 8;

        // --- Кнопка "Назад" в правом верхнем углу ---
        int bw = 80, bh = 20;
        this.addRenderableWidget(Button.builder(
                Component.literal("Назад"),
                b -> this.onClose())
                .bounds(this.width - bw - 8, 8, bw, bh).build());

        // --- Режим ---
        this.addRenderableWidget(btnStatic = Button.builder(
                Component.literal("[Статический]"),
                b -> setMode(false))
                .bounds(PAD, y, 140, 24).build());
        this.addRenderableWidget(btnDynamic = Button.builder(
                Component.literal("[Динамический]"),
                b -> setMode(true))
                .bounds(PAD + 148, y, 140, 24).build());
        y += 28;

        y += 6;
        y += 22;

        lengthSlider = mkFloat(PAD, y, 200, 20, "Длина", temp.length, 0, 32, v -> temp.length = Math.round(v));
        y += 24;
        thicknessSlider = mkFloat(PAD, y, 200, 20, "Толщина", temp.thickness, 1, 8, v -> temp.thickness = Math.round(v));
        y += 24;
        gapSlider = mkFloat(PAD, y, 200, 20, "Зазор", temp.gap, 0, 32, v -> temp.gap = Math.round(v));
        y += 32;

        addToggle(y++, "Точка в центре", temp.dot, v -> { temp.dot = v; rebuildToggles(); });
        if (temp.dot) {
            dotSizeSlider = mkFloat(PAD, y, 200, 20, "Размер точки", temp.dotSize, 1, 8, v -> temp.dotSize = Math.round(v));
            y += 24;
        }
        addToggle(y++, "T-образный (без верхней линии)", temp.tStyle, v -> { temp.tStyle = v; });
        y += 24;
        addToggle(y++, "Чёрная обводка 1px", temp.outline, v -> { temp.outline = v; rebuildOutline(); });
        if (temp.outline) {
            outlineAlphaSlider = mkFloat(PAD, y, 200, 20, "Прозр. обводки", temp.outlineAlpha, 0, 255, v -> temp.outlineAlpha = Math.round(v));
            y += 24;
        }
        y += 10;

        // --- Цвет ---
        y += 22;
        int[] presets = {0xFF00FF00, 0xFFFFFF00, 0xFF00FFFF, 0xFFFFFFFF, 0xFFFF00FF, 0xFFFF0000, 0xFF8800FF};
        String[] names = {"Зелёный", "Жёлтый", "Голубой", "Белый", "Розовый", "Красный", "Фиолет."};
        for (int i = 0; i < presets.length; i++) {
            final int c = presets[i];
            this.addRenderableWidget(Button.builder(
                    Component.literal(names[i]),
                    b -> { temp.color = c; updateColorHex(); })
                    .bounds(PAD + i % 4 * 72, y, 68, 20).build());
            if ((i + 1) % 4 == 0) y += 24;
        }
        y += 8;
        colorHexEdit = new EditBox(this.font, PAD, y, 150, 20, Component.literal("Цвет hex"));
        colorHexEdit.setValue("#" + Integer.toHexString(temp.color & 0xFFFFFF));
        colorHexEdit.setResponder(s -> {
            try {
                String h = s.startsWith("#") ? s.substring(1) : s;
                temp.color = 0xFF000000 | Integer.parseUnsignedInt(h.trim(), 16);
            } catch (Exception ignored) {}
        });
        this.addRenderableWidget(colorHexEdit);
        y += 28;

        // --- Динамика ---
        y += 22;
        baseSpreadSlider = mkFloat(PAD, y, 200, 20, "Base Spread", temp.baseSpread, 0f, 32f, v -> temp.baseSpread = v);
        y += 24;
        moveSpreadSlider = mkFloat(PAD, y, 200, 20, "Move Spread", temp.moveSpread, 0f, 32f, v -> temp.moveSpread = v);
        y += 24;
        sprintSpreadSlider = mkFloat(PAD, y, 200, 20, "Sprint Spread", temp.sprintSpread, 0f, 32f, v -> temp.sprintSpread = v);
        y += 24;
        airSpreadSlider = mkFloat(PAD, y, 200, 20, "Air Spread", temp.airSpread, 0f, 32f, v -> temp.airSpread = v);
        y += 24;
        shotSpreadSlider = mkFloat(PAD, y, 200, 20, "Shot Spread", temp.shotSpread, 0f, 32f, v -> temp.shotSpread = v);
        y += 24;
        shotRecoverySlider = mkFloat(PAD, y, 200, 20, "Recovery", temp.shotRecovery, 0.05f, 1f, v -> temp.shotRecovery = v);
        y += 24;
        lerpSpeedSlider = mkFloat(PAD, y, 200, 20, "Lerp Speed", temp.lerpSpeed, 0.05f, 1f, v -> temp.lerpSpeed = v);
        y += 32;

        // --- Поведение ---
        y += 22;
        addToggle(y++, "Скрывать при зуме снайперки", temp.hideOnSniperAds, v -> { temp.hideOnSniperAds = v; });
        addToggle(y++, "Скрывать в наблюдателе", temp.hideInSpectator, v -> { temp.hideInSpectator = v; });
        addToggle(y++, "Уважать F1 (hideGui)", temp.hideWithHud, v -> { temp.hideWithHud = v; });
        y += 40;

        // --- Кнопки внизу ---
        int botY = this.height - BOT_PAD + 4;
        this.addRenderableWidget(Button.builder(Component.literal("\u21A9 Reset"), b -> resetAll())
                .bounds(PAD, botY, 80, 26).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> cancelAll())
                .bounds(PAD + 88, botY, 80, 26).build());
        this.addRenderableWidget(Button.builder(Component.literal("\u2713 Save"), b -> saveAll())
                .bounds(PAD + 176, botY, 80, 26).build());
    }

    private void setMode(boolean dyn) {
        dynamicMode = dyn;
        temp.dynamic = dyn;
        btnStatic.setMessage(Component.literal(dyn ? "[Статический]" : "\u00A7e[Статический]"));
        btnDynamic.setMessage(Component.literal(dyn ? "\u00A7b[Динамический]" : "[Динамический]"));
        refreshSliderStates();
    }

    private void refreshSliderStates() {
        boolean d = dynamicMode;
        if (moveSpreadSlider != null) moveSpreadSlider.active = d;
        if (sprintSpreadSlider != null) sprintSpreadSlider.active = d;
        if (airSpreadSlider != null) airSpreadSlider.active = d;
        if (shotSpreadSlider != null) shotSpreadSlider.active = d;
        if (shotRecoverySlider != null) shotRecoverySlider.active = d;
        if (lerpSpeedSlider != null) lerpSpeedSlider.active = d;
    }

    private void rebuildToggles() {}
    private void rebuildOutline() {}

    private void updateColorHex() {
        if (colorHexEdit != null) colorHexEdit.setValue("#" + Integer.toHexString(temp.color & 0xFFFFFF));
    }

    private void resetAll() {
        temp = new CrosshairConfig();
        dynamicMode = true;
        temp.dynamic = true;
        clearWidgets();
        buildWidgets();
    }

    private void cancelAll() {
        temp = backup.copy();
        dynamicMode = backup.dynamic;
        clearWidgets();
        buildWidgets();
    }

    private void saveAll() {
        temp.sanitize();
        CrosshairConfig.save(temp);
        com.csedition.client.hud.CrosshairOverlay.setConfig(temp);
        this.onClose(); // закрыть экран после сохранения
    }

    private void computeLayout() {
        int contentH = 700;
        int availH = this.height - TOP_PAD - BOT_PAD - 40;
        maxScroll = Math.max(0, contentH - availH);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    @Override
    public void resize(net.minecraft.client.Minecraft mc, int width, int height) {
        super.resize(mc, width, height);
        computeLayout();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Фон
        g.fill(0, 0, this.width, this.height, 0xDD050505);

        // Левая панель
        g.fill(0, 0, LEFT_W, this.height, 0xEE0A0A0A);
        g.renderOutline(LEFT_W, 0, 1, this.height, 0xFF2A2F36);

        g.drawCenteredString(font, "\u00A7e\u00A7lНАСТРОЙКИ ПРИЦЕЛА", LEFT_W / 2, 10, 0xFFFF8C00);
        g.drawCenteredString(font, "CS Edition", LEFT_W / 2, 28, 0xFF666666);

        // Правая панель — превью
        int px = RIGHT_X;
        int py = TOP_PAD;
        int pw = this.width - px - PAD;
        int ph = this.height - py - BOT_PAD;
        g.fill(px, py, px + pw, py + ph, 0xDD080808);
        g.renderOutline(px, py, pw, ph, 0xFF2A2F36);

        // Сетка фона
        for (int gx = px; gx < px + pw; gx += 24) {
            g.fill(gx, py, gx + 1, py + ph, 0x1AFFFFFF);
        }
        for (int gy = py; gy < py + ph; gy += 24) {
            g.fill(px, gy, px + pw, gy + 1, 0x1AFFFFFF);
        }

        // Превью прицела
        int pcx = px + pw / 2;
        int pcy = py + ph / 2 - 8;
        float previewSpread;
        if (dynamicMode) {
            previewSpread = com.csedition.client.hud.CrosshairPreviewUtil.getDemoSpread(temp);
        } else {
            previewSpread = temp.baseSpread;
        }
        com.csedition.client.hud.CrosshairRenderer.drawPreview(g, pcx, pcy, temp, previewSpread);

        String modeText = dynamicMode ? "\u00A7bДИНАМИЧЕСКИЙ" : "\u00A7eСТАТИЧЕСКИЙ";
        g.drawCenteredString(font, modeText, pcx, pcy + 36, 0xFFFFFFFF);
        String sp = String.format("spread: %.2f", previewSpread);
        g.drawCenteredString(font, sp, pcx, pcy + 52, 0xFF888888);

        // Отрисовка секций и лейблов поверх виджетов (здесь g доступен)
        int y = TOP_PAD + 8;
        y += 34;
        drawLabel(g, y, "Режим: статический — прицел не меняется.\nДинамический — расходится при движении и выстреле.");
        y += 12;
        drawSectionHeader(g, y, "\u00A77ГЕОМЕТРИЯ");
        y += 8;
        drawSectionHeader(g, y, "\u00A77ЦВЕТ");
        y += 8;
        drawSectionHeader(g, y, "\u00A77ДИНАМИКА");
        y += 8;
        drawSectionHeader(g, y, "\u00A77ПОВЕДЕНИЕ");
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll <= 0) return false;
        int step = 30;
        int old = scrollOffset;
        if (delta > 0) scrollOffset = Math.max(0, scrollOffset - step);
        else if (delta < 0) scrollOffset = Math.min(maxScroll, scrollOffset + step);
        return old != scrollOffset;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true; // Esc закрывает экран
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (this.minecraft == null) return;
        if (fromMenu) {
            this.minecraft.setScreen(new com.csedition.client.screen.CsMainMenuScreen());
        } else {
            this.minecraft.setScreen(null); // обратно в игру
        }
    }

    // ====================== Вспомогательные методы отрисовки ======================

    private void drawSectionHeader(GuiGraphics g, int y, String text) {
        int x = PAD;
        int w = LEFT_W - PAD * 2 - 8;
        g.fill(x, y, x + w, y + 18, 0x332A2F36);
        g.renderOutline(x, y, w, 18, 0xFF2A2F36);
        g.drawString(font, text, x + 6, y + 4, 0xFFAAAAAA);
    }

    private void drawLabel(GuiGraphics g, int y, String text) {
        g.drawString(font, text, PAD, y, 0xFF888888);
    }

    private void addToggle(int y, String text, boolean initial, java.util.function.Consumer<Boolean> onChange) {
        boolean[] state = {initial};
        this.addRenderableWidget(Button.builder(
                Component.literal(state[0] ? "[\u2713] " + text : "[ ] " + text),
                b -> {
                    state[0] = !state[0];
                    onChange.accept(state[0]);
                    b.setMessage(Component.literal(state[0] ? "[\u2713] " + text : "[ ] " + text));
                })
                .bounds(PAD, y, LEFT_W - PAD * 2, 18).build());
    }

    private FloatSlider mkFloat(int x, int y, int w, int h, String label, float value, float min, float max,
                                java.util.function.Consumer<Float> setter) {
        FloatSlider s = new FloatSlider(x, y, w, h, label, value, min, max, setter);
        this.addRenderableWidget(s);
        return s;
    }
}