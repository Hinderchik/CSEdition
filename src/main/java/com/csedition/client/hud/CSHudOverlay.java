package com.csedition.client.hud;

import com.csedition.client.ClientState;
import com.csedition.client.render.CSRenderUtil;
import com.csedition.data.GamePhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Кастомный HUD в стиле CS Mobile / BLOCKPOST mobile.
 *
 * В лобби (LOBBY) — стандартный ванильный HUD.
 * В катке — кастомный:
 *   - Хотбар перенесён вправо, вертикально (9 слотов в столбик)
 *   - HP и броня — компактные горизонтальные полоски внизу слева
 *   - Деньги — справа сверху
 *   - Фаза/таймер — сверху по центру на тёмной подложке
 *   - Кастомный ammo HUD убран (используется ванильное отображение через NBT)
 *
 * Рисуется каждый кадр без кэш-чеков (бывший кэш вызывал мерцание).
 */
@OnlyIn(Dist.CLIENT)
public class CSHudOverlay {

    // ===== Layout хотбара =====
    private static final int SLOT_SIZE = 36;
    private static final int SLOT_GAP = 4;
    private static final int HOTBAR_RIGHT_MARGIN = 8;

    @SubscribeEvent
    public void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (ClientState.getPhase() == GamePhase.LOBBY) return;

        NamedGuiOverlay overlay = event.getOverlay();
        String id = overlay.id().toString();
        // Отменяем всё — хотбар/health/armor/effects рисуем сами
        switch (id) {
            case "minecraft:player_health":
            case "minecraft:food_level":
            case "minecraft:air_level":
            case "minecraft:armor_level":
            case "minecraft:hotbar":
            case "minecraft:experience_bar":
            case "minecraft:jump_bar":
            case "minecraft:mount_health":
                event.setCanceled(true);
                break;
            default:
                break;
        }
    }

    @SubscribeEvent
    public void onRenderPost(RenderGuiOverlayEvent.Post event) {
        if (ClientState.getPhase() == GamePhase.LOBBY) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.hideGui) return;

        GuiGraphics g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        drawHealthArmor(g, w, h, mc.player);
        drawMoney(g, w, h);
        drawPhaseTimer(g, w, h);
        drawHotbar(g, w, h, mc.player);
        drawRoundEndScreen(g, w, h);
    }

    private void drawRoundEndScreen(GuiGraphics g, int w, int h) {
        if (!ClientState.shouldShowRoundEnd()) return;
        g.fill(0, 0, w, h, 0x80000000);
        Font font = Minecraft.getInstance().font;
        String winner = ClientState.getRoundEndWinner();
        String reason = ClientState.getRoundEndReason();
        int round = ClientState.getRoundEndRound();
        int topKills = ClientState.getRoundEndTopKills();

        String title;
        int titleColor;
        if ("TARGET_KILLS".equals(reason)) {
            title = "МАТЧ ОКОНЧЕН";
            titleColor = 0xFFFFAA00;
        } else {
            title = "РАУНД " + round + " ОКОНЧЕН";
            titleColor = 0xFFFFFFFF;
        }
        int titleX = w / 2 - font.width(title) / 2;
        g.drawString(font, title, titleX, h / 2 - 40, titleColor);

        String winnerText = "Победитель: " + winner;
        int winnerX = w / 2 - font.width(winnerText) / 2;
        g.drawString(font, winnerText, winnerX, h / 2 - 20, 0xFF55FF55);

        String reasonText = switch (reason) {
            case "ELIMINATION" -> "Все противники уничтожены";
            case "TIME_OUT" -> "Время вышло";
            case "TARGET_KILLS" -> "Достигнуто целевое число убийств (" + topKills + ")";
            default -> reason;
        };
        int reasonX = w / 2 - font.width(reasonText) / 2;
        g.drawString(font, reasonText, reasonX, h / 2, 0xFFCCCCCC);
    }

    // ====================== HP / Броня ======================

    /**
     * Компактные полоски HP/брони в стиле CS:
     * - Над полоской — подпись (HP / AP) и числовое значение
     * - Полоска с сегментами по 25%
     * - Градиент от яркого к тёмному
     * - Тонкая цветная линия сверху для свечения
     */
    private void drawHealthArmor(GuiGraphics g, int w, int h, Player p) {
        Font font = Minecraft.getInstance().font;
        int barW = 180;
        int barH = 7;
        int x = 12;
        int y = h - 56; // над нижним краем

        // HP
        float maxHp = p.getMaxHealth();
        float health = p.getHealth();
        float healthRatio = maxHp > 0 ? Math.max(0, health / maxHp) : 0;
        drawCsBar(g, font, x, y, barW, barH,
                healthRatio, CSRenderUtil.CS_RED,
                "HP", (int) health, (int) maxHp);

        // Броня
        int armor = p.getArmorValue();
        float armorRatio = armor > 0 ? Math.min(1f, armor / 20f) : 0;
        int y2 = y + barH + 14;
        drawCsBar(g, font, x, y2, barW, barH,
                armorRatio, CSRenderUtil.CS_BLUE,
                "AP", armor, 20);
    }

    /**
     * Рисует одну полоску в CS-стиле.
     */
    private void drawCsBar(GuiGraphics g, Font font, int x, int y, int w, int h,
                           float ratio, int color, String label,
                           int value, int maxValue) {
        ratio = Math.max(0, Math.min(1, ratio));
        // Рамка
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        // Тёмный фон
        int bgR = (int)((color >> 16) & 0xFF) / 6;
        int bgG = (int)((color >> 8) & 0xFF) / 6;
        int bgB = (int)(color & 0xFF) / 6;
        int bgColor = 0xFF000000 | (bgR << 16) | (bgG << 8) | bgB;
        g.fill(x, y, x + w, y + h, bgColor);

        // Заливка с градиентом
        int fillW = (int) (w * ratio);
        if (fillW > 0) {
            int dark = CSRenderUtil.darken(color, 0.55f);
            CSRenderUtil.hGradient(g, x, y, fillW, h, color, dark);
            // Свечение сверху
            g.fill(x, y, x + fillW, y + 1, CSRenderUtil.lighten(color, 0.35f));
        }
        // Сегментация каждые 25%
        for (int i = 1; i < 4; i++) {
            int sx = x + (w * i / 4);
            g.fill(sx, y, sx + 1, y + h, 0x66000000);
        }

        // Подпись слева сверху
        g.drawString(font, label, x, y - 9, color);
        // Число справа сверху
        String num = value + " / " + maxValue;
        int numW = font.width(num);
        g.drawString(font, num, x + w - numW, y - 9, 0xFFDDDDDD);
    }

    // ====================== Деньги ======================

    private void drawMoney(GuiGraphics g, int w, int h) {
        int money = ClientState.getMoney();
        Font font = Minecraft.getInstance().font;
        String text = "$" + money;

        // Подложка
        int textW = font.width(text);
        int padX = 8, padY = 4;
        int boxX = w - textW - padX * 2 - 8;
        int boxY = 8;
        g.fill(boxX, boxY, boxX + textW + padX * 2, boxY + 18, 0xCC000000);
        // Оранжевая полоска слева
        g.fill(boxX, boxY, boxX + 2, boxY + 18, CSRenderUtil.CS_ORANGE);
        // Угловые акценты
        g.fill(boxX, boxY, boxX + 5, boxY + 1, CSRenderUtil.CS_ORANGE);
        g.fill(boxX, boxY, boxX + 1, boxY + 5, CSRenderUtil.CS_ORANGE);

        g.drawString(font, text, boxX + padX, boxY + 5, CSRenderUtil.CS_GREEN);
    }

    // ====================== Фаза / Таймер ======================

    private void drawPhaseTimer(GuiGraphics g, int w, int h) {
        GamePhase phase = ClientState.getPhase();
        int ticks = ClientState.getPhaseTicks();

        Font font = Minecraft.getInstance().font;
        String phaseName = switch (phase) {
            case LOBBY -> "ЛОББИ";
            case BUY_TIME -> "ЗАКУП";
            case FIGHTING -> "БОЙ";
            case ROUND_END -> "КОНЕЦ РАУНДА";
        };
        String text = phaseName + "  " + (ticks / 20) + "с";
        int textW = font.width(text);
        int cx = w / 2;

        int bgX1 = cx - textW / 2 - 10;
        int bgX2 = cx + textW / 2 + 10;
        g.fill(bgX1, 4, bgX2, 22, 0xCC000000);
        g.fill(bgX1, 18, bgX2, 19, CSRenderUtil.CS_ORANGE);
        // Угловые акценты
        g.fill(bgX1, 4, bgX1 + 6, 5, CSRenderUtil.CS_ORANGE);
        g.fill(bgX1, 4, bgX1 + 1, 10, CSRenderUtil.CS_ORANGE);
        g.fill(bgX2 - 6, 4, bgX2, 5, CSRenderUtil.CS_ORANGE);
        g.fill(bgX2 - 1, 4, bgX2, 10, CSRenderUtil.CS_ORANGE);

        g.drawString(font, text, cx - textW / 2, 9, CSRenderUtil.CS_YELLOW);
    }

    // ====================== Хотбар (вертикальный, справа) ======================

    /**
     * Вертикальный хотбар в правой части экрана.
     * 9 слотов в столбик, выбранный подсвечен оранжевым.
     * Слева от слотов — номер (1..9) мелким шрифтом.
     */
    private void drawHotbar(GuiGraphics g, int w, int h, Player p) {
        Font font = Minecraft.getInstance().font;
        int slotSize = SLOT_SIZE;
        int slotGap = SLOT_GAP;
        int totalH = 9 * slotSize + 8 * slotGap;
        int hotbarX = w - slotSize - HOTBAR_RIGHT_MARGIN;
        int hotbarY = (h - totalH) / 2;

        int selected = p.getInventory().selected;

        for (int i = 0; i < 9; i++) {
            int slotY = hotbarY + i * (slotSize + slotGap);
            ItemStack stack = p.getInventory().getItem(i);
            boolean isSelected = (i == selected);

            drawHotbarSlot(g, font, hotbarX, slotY, slotSize, isSelected);

            // Предмет
            if (!stack.isEmpty()) {
                int itemX = hotbarX + (slotSize - 16) / 2;
                int itemY = slotY + (slotSize - 16) / 2;
                g.renderItem(stack, itemX, itemY);
                // Каунтер если стак > 1
                if (stack.getCount() > 1) {
                    String count = String.valueOf(stack.getCount());
                    g.drawString(font, count,
                            hotbarX + slotSize - font.width(count) - 2,
                            slotY + slotSize - 8,
                            0xFFFFFFFF);
                }
            }

            // Номер слота слева
            String num = String.valueOf(i + 1);
            int numColor = isSelected ? CSRenderUtil.CS_ORANGE : 0xFF888888;
            g.drawString(font, num,
                    hotbarX - font.width(num) - 4,
                    slotY + slotSize / 2 - 4,
                    numColor);
        }

        // Обводка всего хотбара
        int outlineX1 = hotbarX - 12;
        int outlineY1 = hotbarY - 4;
        int outlineX2 = hotbarX + slotSize + 4;
        int outlineY2 = hotbarY + totalH + 4;
        g.renderOutline(outlineX1, outlineY1,
                outlineX2 - outlineX1, outlineY2 - outlineY1,
                CSRenderUtil.CS_BORDER);
    }

    private void drawHotbarSlot(GuiGraphics g, Font font, int x, int y, int size, boolean selected) {
        // Фон с градиентом
        int bgTop = selected ? 0xEE3A2A1A : 0xEE0E0E0E;
        int bgBot = selected ? 0xEE1A0A00 : 0xCC050505;
        CSRenderUtil.vGradient(g, x, y, size, size, bgTop, bgBot);

        // Рамка
        int borderColor = selected ? CSRenderUtil.CS_ORANGE : CSRenderUtil.CS_BORDER;
        g.renderOutline(x, y, size, size, borderColor);

        // Для выбранного — угловые акценты и внутренняя подсветка
        if (selected) {
            // Угловые акценты
            g.fill(x, y, x + 10, y + 1, borderColor);
            g.fill(x, y, x + 1, y + 10, borderColor);
            g.fill(x + size - 10, y, x + size, y + 1, borderColor);
            g.fill(x + size - 1, y, x + size, y + 10, borderColor);
            g.fill(x, y + size - 1, x + 10, y + size, borderColor);
            g.fill(x, y + size - 10, x + 1, y + size, borderColor);
            g.fill(x + size - 10, y + size - 1, x + size, y + size, borderColor);
            g.fill(x + size - 1, y + size - 10, x + size, y + size, borderColor);
            // Внутренняя подсветка
            g.renderOutline(x + 2, y + 2, size - 4, size - 4,
                    CSRenderUtil.withAlpha(borderColor, 120));
        }
    }
}
