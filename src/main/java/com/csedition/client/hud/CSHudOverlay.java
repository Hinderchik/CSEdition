package com.csedition.client.hud;

import com.csedition.client.ClientState;
import com.csedition.client.SlotLayout;
import com.csedition.data.GamePhase;
import com.csedition.data.Team;
import com.csedition.match.MatchManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * CS2-style HUD. Рендерится в RenderGuiEvent.Post (одна регистрация через getInstance).
 * Не зависит от GUI scale — все размеры в gui-пикселях, Minecraft сам масштабирует.
 */
@OnlyIn(Dist.CLIENT)
public class CSHudOverlay {

    private static final CSHudOverlay INSTANCE = new CSHudOverlay();

    // --- Константы стиля ---
    private static final int PAD            = 6;
    private static final int BAR_W          = 96;
    private static final int BAR_H          = 5;
    private static final int SLOT           = 22;
    private static final int SLOT_GAP       = 2;

    // Цвета CS2
    private static final int COLOR_T   = 0xFFD9A33A;
    private static final int COLOR_CT  = 0xFF5B8FD6;
    private static final int COLOR_HP_DARK = 0xFF101418;
    private static final int COLOR_BORDER = 0xFF2A2F36;
    private static final int COLOR_MONEY_SYMBOL = 0xFF9AA4AF;
    private static final int COLOR_MONEY_AMOUNT = 0xFF7ED957;
    private static final int COLOR_TEXT_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GREY = 0xFF9AA4AF;
    private static final int COLOR_ACCENT = 0xFFE8B84B;
    private static final int COLOR_PANEL_BG = 0x99101418;
    private static final int FLASH_COLOR = 0xFFE8B84B;
    private static final long FLASH_DURATION_MS = 200L;

    private static final int HP_ICON_SIZE  = 8;
    private static final int ARMOR_ICON_SIZE = 8;
    private static final int MONEY_HEIGHT = 14;
    private static final int TIMER_BAR_H  = 16;
    private static final int SCORE_BAR_H  = 14;

    // Flash state
    private int lastMoney = Integer.MIN_VALUE;
    private long moneyFlashUntil = 0;

    // Phase/timer string caches
    private GamePhase lastPhaseCached = null;
    private int lastTicksCache = Integer.MIN_VALUE;
    private String cachedTimeStr = "";

    public CSHudOverlay() {
        // Single registration — caller is responsible for registering via getInstance()
    }

    public static CSHudOverlay getInstance() { return INSTANCE; }

    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        if (!shouldRender()) return;
        doRender(event.getGuiGraphics());
    }

    private boolean shouldRender() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        if (mc.options.hideGui) return false;
        // НЕ проверяем mc.screen — HUD остаётся видимым в инвентаре, меню закупа и т.д.
        return ClientState.getPhase() != GamePhase.LOBBY;
    }

    private void doRender(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        Font font = mc.font;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        drawHpArmor(g, p, w, h);
        drawMoney(g, w);
        drawTimerScore(g, w, h);
        drawHotbar(g, p, w, h, font);
    }

    // ====================== HP & Armor (bottom-left, CS2 style) ======================

    private void drawHpArmor(GuiGraphics g, Player p, int screenW, int screenH) {
        Font font = Minecraft.getInstance().font;
        float maxHp = Math.max(1f, p.getMaxHealth());
        float hpRatio = Math.min(1f, p.getHealth() / maxHp);
        int hpInt = (int) p.getHealth();
        int armor = p.getArmorValue();

        // Индикатор цвета HP: при <=25% красноватый оттенок
        int hpColorBase = hpInt <= (int)(maxHp * 0.25) ? 0xFFD33A3A : interpolateHpColor(hpRatio);
        int hpTextColor = hpInt <= (int)(maxHp * 0.25) ? 0xFFD33A3A : 0xFFFFFFFF;

        // Пульсация прозрачности при низком HP
        int hpAlpha = 0xFF;
        if (hpInt <= (int)(maxHp * 0.25)) {
            int pulse = (int)(System.currentTimeMillis() / 250) % 2;
            hpAlpha = pulse == 0 ? 0xCC : 0xFF;
            hpColorBase = hpColorBase & 0x00FFFFFF | (hpAlpha << 24);
        }

        int barX = PAD;
        // Начинаем с низа экрана и идём вверх: сначала HP, потом броня
        int currentY = screenH - PAD - BAR_H;

        // ---- HP row ----
        // Иконка-крестик 8x8 слева
        int iconX = barX;
        int iconY = currentY - HP_ICON_SIZE - 1;
        drawCrossIcon(g, iconX, iconY, HP_ICON_SIZE, 0xFFFF6B6B);

        // Полоска HP
        drawBar(g, barX + HP_ICON_SIZE + 2, currentY, BAR_W, BAR_H, hpRatio, hpColorBase);

        // Число HP справа от полоски
        String hpStr = String.valueOf(hpInt);
        int hpStrW = font.width(hpStr);
        g.drawString(font, hpStr, barX + HP_ICON_SIZE + 2 + BAR_W + 4, currentY, hpTextColor);

        currentY -= BAR_H + 3;

        // ---- Armor row (пропускаем если 0) ----
        if (armor > 0) {
            int armorColor = 0xFFBFC7D5;
            float armorRatio = Math.min(1f, armor / 20f);
            drawBar(g, barX + ARMOR_ICON_SIZE + 2, currentY, BAR_W, BAR_H, armorRatio, armorColor);
            String armorStr = String.valueOf(armor);
            g.drawString(font, armorStr, barX + ARMOR_ICON_SIZE + 2 + BAR_W + 4, currentY, armorColor);
            // Иконка-ромб (щит) для брони
            int armIconY = currentY - ARMOR_ICON_SIZE - 1;
            drawShieldIcon(g, barX, armIconY, ARMOR_ICON_SIZE, armorColor);
            currentY -= BAR_H + 3;
        }
    }

    private static int interpolateHpColor(float ratio) {
        // От красного (0) к жёлтому (0.5) к зелёному (1.0)
        int r, gg, b;
        if (ratio < 0.5f) {
            float t = ratio / 0.5f;
            r = 255;
            gg = (int)(255 * (1 - t) + 180 * t);
            b = (int)(58 * (1 - t) + 111 * t);
        } else {
            float t = (ratio - 0.5f) / 0.5f;
            r = (int)(255 * (1 - t) + 111 * t);
            gg = (int)(180 * (1 - t) + 211 * t);
            b = (int)(111 * (1 - t) + 111 * t);
        }
        return (0xFF << 24) | (r << 16) | (gg << 8) | b;
    }

    private static void drawBar(GuiGraphics g, int x, int y, int w, int h, float ratio, int color) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, COLOR_BORDER);
        g.fill(x, y, x + w, y + h, COLOR_PANEL_BG);
        int fill = Math.max(1, (int)(w * ratio));
        g.fill(x, y, x + fill, y + h, color);
    }

    private static void drawCrossIcon(GuiGraphics g, int x, int y, int size, int color) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        int t = 2;
        g.fill(cx - t/2, y, cx + t/2, y + size, color);
        g.fill(x, cy - t/2, x + size, cy + t/2, color);
    }

    private static void drawShieldIcon(GuiGraphics g, int x, int y, int size, int color) {
        int cx = x + size / 2;
        g.fill(cx - 1, y, cx + 1, y + size, color);
        g.fill(x, y + 1, x + size, y + 2, color);
        g.fill(x, y + size - 2, x + size, y + size - 1, color);
    }

    // ====================== Money (top-right, CS2 style) ======================

    private void drawMoney(GuiGraphics g, int screenW) {
        Font font = Minecraft.getInstance().font;
        int money = ClientState.getMoney();
        long now = System.currentTimeMillis();

        boolean flashing = now < moneyFlashUntil;
        int moneyColor = flashing ? FLASH_COLOR : COLOR_MONEY_AMOUNT;

        String text = "$ " + money;
        int textW = font.width(text);
        int boxW = textW + PAD * 2;
        int boxX = screenW - PAD - boxW;
        int boxY = PAD;

        // Фон панели
        g.fill(boxX, boxY, boxX + boxW, boxY + MONEY_HEIGHT, COLOR_PANEL_BG);
        g.renderOutline(boxX, boxY, boxW, MONEY_HEIGHT, COLOR_BORDER);

        // Текст
        g.drawString(font, text, boxX + PAD, boxY + (MONEY_HEIGHT - 8) / 2, moneyColor);

        // Обновляем состояние флеша
        if (money != lastMoney) {
            moneyFlashUntil = now + FLASH_DURATION_MS;
            lastMoney = money;
        }
    }

    // ====================== Timer & Score (top-center, CS2 style) ======================

    private void drawTimerScore(GuiGraphics g, int screenW, int screenH) {
        GamePhase phase = ClientState.getPhase();
        if (phase == GamePhase.LOBBY) return;

        Font font = Minecraft.getInstance().font;
        int ticks = ClientState.getPhaseTicks();

        // Кэш строки времени — пересчитываем только при изменении фазы или секунд
        if (phase != lastPhaseCached || ticks / 20 != lastTicksCache) {
            cachedTimeStr = buildTimeStr(phase, ticks);
            lastPhaseCached = phase;
            lastTicksCache = ticks / 20;
        }

        int timerColor = getTimeColor(ticks);
        int topY = PAD;

        // Единый блок по центру: таймер сверху, счёт снизу
        int panelPad = PAD;
        int timePanelW = font.width(cachedTimeStr) + panelPad * 2;
        int panelX = screenW / 2 - timePanelW / 2;

        // Таймер панель
        g.fill(panelX, topY, panelX + timePanelW, topY + TIMER_BAR_H, COLOR_PANEL_BG);
        g.renderOutline(panelX, topY, timePanelW, TIMER_BAR_H, COLOR_BORDER);
        int timeTextX = panelX + (timePanelW - font.width(cachedTimeStr)) / 2;
        g.drawString(font, cachedTimeStr, timeTextX, topY + 2, timerColor);

        // Счёт под таймером
        MatchManager mm = MatchManager.getInstance();
        if (mm != null) {
            int tScore = mm.getRoundsWon(Team.T);
            int ctScore = mm.getRoundsWon(Team.CT);
            String scoreText = tScore + " : " + ctScore;
            int scoreY = topY + TIMER_BAR_H + 2;
            int scorePanelW = font.width(scoreText) + panelPad * 2;
            int scorePanelX = screenW / 2 - scorePanelW / 2;
            g.fill(scorePanelX, scoreY, scorePanelX + scorePanelW, scoreY + SCORE_BAR_H, COLOR_PANEL_BG);
            g.renderOutline(scorePanelX, scoreY, scorePanelW, SCORE_BAR_H, COLOR_BORDER);
            int sx = scorePanelX + (scorePanelW - font.width(scoreText)) / 2;
            // T часть — оранжевым, CT часть — синим
            int leftW = font.width(tScore + " ");
            g.drawString(font, tScore + " ", sx, scoreY + 1, COLOR_T);
            g.drawString(font, " : ", sx + leftW, scoreY + 1, COLOR_GREY);
            g.drawString(font, "" + ctScore, sx + leftW + font.width(" : "), scoreY + 1, COLOR_CT);
        }
    }

    private String buildTimeStr(GamePhase phase, int ticks) {
        int seconds = ticks / 20;
        return switch (phase) {
            case BUY_TIME -> "BUY " + seconds;
            case ROUND_END -> "ROUND END";
            default -> seconds / 60 + ":" + String.format("%02d", seconds % 60);
        };
    }

    private static int getTimeColor(int ticks) {
        int seconds = ticks / 20;
        if (seconds < 10) return 0xFFD33A3A;
        if (seconds <= 30) return 0xFFE8B84B;
        return 0xFFFFFFFF;
    }

    // ====================== Hotbar (bottom-right, vertical, CS2 style) ======================

    private void drawHotbar(GuiGraphics g, Player p, int screenW, int screenH, Font font) {
        int slotSize = SLOT;
        int gap = SLOT_GAP;
        int slotsCount = SlotLayout.COUNT;
        int totalH = slotsCount * slotSize + (slotsCount - 1) * gap;
        int hotbarX = screenW - slotSize - PAD;
        int hotbarY = screenH - totalH - PAD;

        int selectedSlot = p.getInventory().selected;

        for (int i = 0; i < slotsCount; i++) {
            int slotY = hotbarY + i * (slotSize + gap);
            ItemStack stack = p.getInventory().getItem(i);
            boolean isSelected = (i == selectedSlot);

            // Фон слота
            int bgColor = isSelected ? 0xCC1A1F26 : 0x99101418;
            g.fill(hotbarX, slotY, hotbarX + slotSize, slotY + slotSize, bgColor);

            // Обводка
            int borderColor = isSelected ? COLOR_ACCENT : COLOR_BORDER;
            if (isSelected) {
                // Лёгкое свечение — двойной контур
                g.fill(hotbarX - 1, slotY - 1, hotbarX + slotSize + 1, slotY, 0x44E8B84B);
                g.fill(hotbarX - 1, slotY + slotSize, hotbarX + slotSize + 1, slotY + slotSize + 1, 0x44E8B84B);
                g.fill(hotbarX - 1, slotY, hotbarX, slotY + slotSize, 0x44E8B84B);
                g.fill(hotbarX + slotSize, slotY, hotbarX + slotSize + 1, slotY + slotSize, 0x44E8B84B);
            }
            g.fill(hotbarX, slotY, hotbarX + slotSize, slotY + 1, borderColor);
            g.fill(hotbarX, slotY + slotSize - 1, hotbarX + slotSize, slotY + slotSize, borderColor);
            g.fill(hotbarX, slotY, hotbarX + 1, slotY + slotSize, borderColor);
            g.fill(hotbarX + slotSize - 1, slotY, hotbarX + slotSize, slotY + slotSize, borderColor);

            // Номер слота (левый верхний угол)
            String num = String.valueOf(i + 1);
            g.drawString(font, num, hotbarX + 2, slotY + 2, COLOR_GREY);

            // Предмет
            if (!stack.isEmpty()) {
                int iconX = hotbarX + (slotSize - 16) / 2;
                int iconY = slotY + (slotSize - 16) / 2;
                g.renderItem(stack, iconX, iconY);
                g.renderItemDecorations(font, stack, iconX, iconY);
                // Количество
                if (stack.getCount() > 1) {
                    String count = String.valueOf(stack.getCount());
                    int cw = font.width(count);
                    g.drawString(font, count, iconX + 16 - cw - 1, iconY + 16 - 8, 0xFFFFFFFF);
                }
            }
        }

        // Подписи слотов под хотбаром (только для выбранного)
        int labelY = hotbarY - 6;
        for (int i = 0; i < slotsCount; i++) {
            if (i == selectedSlot) {
                String slotName = getSlotName(i);
                int lw = font.width(slotName);
                g.drawString(font, slotName, hotbarX + (slotSize - lw) / 2, labelY, COLOR_ACCENT);
            }
        }
    }

    private static String getSlotName(int slotIndex) {
        return switch (slotIndex) {
            case 0 -> "PRIMARY";
            case 1 -> "SECONDARY";
            case 2 -> "KNIFE";
            case 3 -> "UTILITY";
            default -> "";
        };
    }
}
