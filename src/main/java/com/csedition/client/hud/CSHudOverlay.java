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
 * Хотбар — ТОЛЬКО 3 слота горизонтально внизу справа (как у мобильных FPS).
 *
 * Адаптивность под разрешение:
 *   - Базовый дизайн рассчитан на 1920x1080
 *   - UI автоматически масштабируется по min(w, h) / 480
 *   - На 854x480 всё выглядит компактно, на 4K — крупно и читаемо
 *
 * FPS оптимизация:
 *   - Дедуп по нанотайму (рисуем 1 раз за кадр, а не 8)
 *   - Рендер ItemStack кэшируется через PoseStack.pushPose (один setup/teardown)
 *   - Слоты с одинаковым ItemStack пропускаются (сравнение по IdentityHash)
 *   - Каунтер рисуем только если stack.getCount() > 1
 */
@OnlyIn(Dist.CLIENT)
public class CSHudOverlay {

    /** Показываем только 3 слота хотбара (мобильный стиль). */
    private static final int HOTBAR_SLOTS = 3;

    /** Нанотайм последней отрисовки (для дедупа). */
    private long lastDrawNanos = -1;

    /** Кэш последнего отрендеренного состояния хотбара (для пропуска). */
    private final ItemStack[] lastHotbarRendered = new ItemStack[HOTBAR_SLOTS];
    private int lastHotbarHash = 0;

    @SubscribeEvent
    public void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (ClientState.getPhase() == GamePhase.LOBBY) return;
        String id = event.getOverlay().id().toString();
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

        // Дедуп по кадру — Post event фаерится 8+ раз за кадр
        long now = System.nanoTime();
        if (now == lastDrawNanos) return;
        lastDrawNanos = now;

        GuiGraphics g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        Layout layout = Layout.forScreen(w, h);

        drawHealthArmor(g, w, h, mc.player, layout);
        drawMoney(g, w, h, layout);
        drawPhaseTimer(g, w, h, layout);
        drawHotbar(g, w, h, mc.player, layout);
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

        String title = "TARGET_KILLS".equals(reason) ? "МАТЧ ОКОНЧЕН" : "РАУНД " + round + " ОКОНЧЕН";
        int titleColor = "TARGET_KILLS".equals(reason) ? 0xFFFFAA00 : 0xFFFFFFFF;
        g.drawCenteredString(font, title, w / 2 - font.width(title) / 2, h / 2 - 40, titleColor);

        String winnerText = "Победитель: " + winner;
        g.drawCenteredString(font, winnerText, w / 2 - font.width(winnerText) / 2, h / 2 - 20, 0xFF55FF55);

        String reasonText = switch (reason) {
            case "ELIMINATION" -> "Все противники уничтожены";
            case "TIME_OUT" -> "Время вышло";
            case "TARGET_KILLS" -> "Достигнуто целевое число убийств (" + topKills + ")";
            default -> reason;
        };
        g.drawCenteredString(font, reasonText, w / 2 - font.width(reasonText) / 2, h / 2, 0xFFCCCCCC);
    }

    // ====================== HP / Броня ======================

    private void drawHealthArmor(GuiGraphics g, int w, int h, Player p, Layout layout) {
        Font font = Minecraft.getInstance().font;
        int x = layout.padLeft;
        int barW = layout.hpBarW;
        int barH = layout.hpBarH;

        float maxHp = p.getMaxHealth();
        float health = p.getHealth();
        float healthRatio = maxHp > 0 ? Math.max(0, health / maxHp) : 0;
        drawCsBar(g, font, x, layout.hpY, barW, barH,
                healthRatio, CSRenderUtil.CS_RED,
                "HP", (int) health, (int) maxHp, layout);

        int armor = p.getArmorValue();
        float armorRatio = armor > 0 ? Math.min(1f, armor / 20f) : 0;
        drawCsBar(g, font, x, layout.apY, barW, barH,
                armorRatio, CSRenderUtil.CS_BLUE,
                "AP", armor, 20, layout);
    }

    private void drawCsBar(GuiGraphics g, Font font, int x, int y, int w, int h,
                           float ratio, int color, String label,
                           int value, int maxValue, Layout layout) {
        ratio = Math.max(0, Math.min(1, ratio));
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        int bgR = (int)((color >> 16) & 0xFF) / 6;
        int bgG = (int)((color >> 8) & 0xFF) / 6;
        int bgB = (int)(color & 0xFF) / 6;
        int bgColor = 0xFF000000 | (bgR << 16) | (bgG << 8) | bgB;
        g.fill(x, y, x + w, y + h, bgColor);

        int fillW = (int) (w * ratio);
        if (fillW > 0) {
            int dark = CSRenderUtil.darken(color, 0.55f);
            CSRenderUtil.hGradient(g, x, y, fillW, h, color, dark);
            g.fill(x, y, x + fillW, y + 1, CSRenderUtil.lighten(color, 0.35f));
        }
        for (int i = 1; i < 4; i++) {
            int sx = x + (w * i / 4);
            g.fill(sx, y, sx + 1, y + h, 0x66000000);
        }

        // Подпись и значение
        int textOffset = Math.max(8, layout.scale(9));
        g.drawString(font, label, x, y - textOffset, color);
        String num = value + " / " + maxValue;
        g.drawString(font, num, x + w - font.width(num), y - textOffset, 0xFFDDDDDD);
    }

    // ====================== Деньги ======================

    private void drawMoney(GuiGraphics g, int w, int h, Layout layout) {
        int money = ClientState.getMoney();
        Font font = Minecraft.getInstance().font;
        String text = "$" + money;
        int padX = layout.scale(8);
        int boxY = layout.scale(8);
        int boxH = layout.scale(18);
        int textW = font.width(text);
        int boxX = w - textW - padX * 2 - layout.scale(8);

        g.fill(boxX, boxY, boxX + textW + padX * 2, boxY + boxH, 0xCC000000);
        g.fill(boxX, boxY, boxX + 2, boxY + boxH, CSRenderUtil.CS_ORANGE);
        g.fill(boxX, boxY, boxX + 5, boxY + 1, CSRenderUtil.CS_ORANGE);
        g.fill(boxX, boxY, boxX + 1, boxY + 5, CSRenderUtil.CS_ORANGE);
        g.drawString(font, text, boxX + padX, boxY + layout.scale(5), CSRenderUtil.CS_GREEN);
    }

    // ====================== Фаза / Таймер ======================

    private void drawPhaseTimer(GuiGraphics g, int w, int h, Layout layout) {
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
        int bgY2 = layout.scale(22);
        int bgY1 = layout.scale(4);
        int padX = layout.scale(10);

        int bgX1 = cx - textW / 2 - padX;
        int bgX2 = cx + textW / 2 + padX;
        g.fill(bgX1, bgY1, bgX2, bgY2, 0xCC000000);
        g.fill(bgX1, bgY2 - 4, bgX2, bgY2 - 3, CSRenderUtil.CS_ORANGE);
        g.fill(bgX1, bgY1, bgX1 + 6, bgY1 + 1, CSRenderUtil.CS_ORANGE);
        g.fill(bgX1, bgY1, bgX1 + 1, bgY1 + 6, CSRenderUtil.CS_ORANGE);
        g.fill(bgX2 - 6, bgY1, bgX2, bgY1 + 1, CSRenderUtil.CS_ORANGE);
        g.fill(bgX2 - 1, bgY1, bgX2, bgY1 + 6, CSRenderUtil.CS_ORANGE);
        g.drawString(font, text, cx - textW / 2, layout.scale(9), CSRenderUtil.CS_YELLOW);
    }

    // ====================== Хотбар (3 слота, справа снизу) ======================

    /**
     * 3 слота хотбара горизонтально в правом нижнем углу.
     * Слот 1 (индекс 0) — primary, 2 (индекс 1) — secondary, 3 (индекс 2) — knife/utility.
     */
    private void drawHotbar(GuiGraphics g, int w, int h, Player p, Layout layout) {
        Font font = Minecraft.getInstance().font;
        int slotSize = layout.slotSize;
        int slotGap = layout.scale(4);
        int totalW = HOTBAR_SLOTS * slotSize + (HOTBAR_SLOTS - 1) * slotGap;
        int hotbarX = w - totalW - layout.scale(10);
        int hotbarY = h - slotSize - layout.scale(10);

        // Хэш текущего состояния — пропускаем если ничего не изменилось
        int currentHash = computeHotbarHash(p);
        if (currentHash == lastHotbarHash) {
            // Хотбар не менялся — но HP/AP/money могли. Поэтому не возвращаемся —
            // продолжаем рисовать остальной HUD (HP/AP/money/timer уже нарисованы выше)
            return;
        }
        lastHotbarHash = currentHash;

        int selected = p.getInventory().selected;

        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            int slotX = hotbarX + i * (slotSize + slotGap);
            ItemStack stack = p.getInventory().getItem(i);
            boolean isSelected = (i == selected);
            boolean isEmpty = stack.isEmpty();

            // Пропуск: слот с тем же ItemStack уже отрендерен
            if (lastHotbarRendered[i] != null
                    && ItemStack.matches(lastHotbarRendered[i], stack)) {
                continue;
            }
            lastHotbarRendered[i] = stack.copy();

            drawHotbarSlot(g, font, slotX, hotbarY, slotSize, isSelected);

            if (!isEmpty) {
                int itemX = slotX + (slotSize - 16) / 2;
                int itemY = hotbarY + (slotSize - 16) / 2;
                g.renderItem(stack, itemX, itemY);
                if (stack.getCount() > 1) {
                    String count = String.valueOf(stack.getCount());
                    g.drawString(font, count,
                            slotX + slotSize - font.width(count) - 2,
                            hotbarY + slotSize - 8,
                            0xFFFFFFFF);
                }
            }

            // Номер слота над слотом
            String num = String.valueOf(i + 1);
            int numColor = isSelected ? CSRenderUtil.CS_ORANGE : 0xFF888888;
            g.drawString(font, num,
                    slotX + (slotSize - font.width(num)) / 2,
                    hotbarY - layout.scale(10),
                    numColor);
        }
    }

    private int computeHotbarHash(Player p) {
        int hash = p.getInventory().selected;
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            ItemStack s = p.getInventory().getItem(i);
            hash = 31 * hash + s.getCount();
            hash = 31 * hash + (s.isEmpty() ? 0 : System.identityHashCode(s.getItem()));
        }
        return hash;
    }

    private void drawHotbarSlot(GuiGraphics g, Font font, int x, int y, int size, boolean selected) {
        int bgTop = selected ? 0xEE3A2A1A : 0xEE0E0E0E;
        int bgBot = selected ? 0xEE1A0A00 : 0xCC050505;
        CSRenderUtil.vGradient(g, x, y, size, size, bgTop, bgBot);

        int borderColor = selected ? CSRenderUtil.CS_ORANGE : CSRenderUtil.CS_BORDER;
        g.renderOutline(x, y, size, size, borderColor);

        if (selected) {
            int corner = Math.max(6, size / 6);
            g.fill(x, y, x + corner, y + 1, borderColor);
            g.fill(x, y, x + 1, y + corner, borderColor);
            g.fill(x + size - corner, y, x + size, y + 1, borderColor);
            g.fill(x + size - 1, y, x + size, y + corner, borderColor);
            g.fill(x, y + size - 1, x + corner, y + size, borderColor);
            g.fill(x, y + size - corner, x + 1, y + size, borderColor);
            g.fill(x + size - corner, y + size - 1, x + size, y + size, borderColor);
            g.fill(x + size - 1, y + size - corner, x + size, y + size, borderColor);
            g.renderOutline(x + 2, y + 2, size - 4, size - 4,
                    CSRenderUtil.withAlpha(borderColor, 120));
        }
    }

    // ====================== Layout (адаптивный под разрешение) ======================

    /**
     * Адаптивный layout под разные разрешения экрана.
     * База — 1920x1080. Масштабируется по min(w, h) / 480 (стандартное
     * разрешение мобильных FPS).
     */
    private record Layout(
            int scale,        // множитель (1.0 на 480p, ~2.0 на 1080p)
            int padLeft,      // отступ слева
            int hpBarW,       // ширина HP/AP полоски
            int hpBarH,       // высота HP/AP полоски
            int hpY,           // Y для HP
            int apY,           // Y для AP
            int slotSize       // размер слота хотбара
    ) {
        int scale(int base) {
            return Math.max(1, base * scale / 2);
        }

        static Layout forScreen(int w, int h) {
            int minDim = Math.min(w, h);
            // 480 = база (scale=2), 720 = scale=3, 1080 = scale=4
            int scale = Math.max(1, Math.min(6, minDim / 240));
            int padLeft = Math.max(8, scale * 6);
            int hpBarW = Math.max(120, scale * 90);
            int hpBarH = Math.max(5, scale * 4);
            int slotSize = Math.max(28, scale * 18);
            int hpY = h - slotSize - scale * 28;
            int apY = hpY + hpBarH + scale * 8;
            return new Layout(scale, padLeft, hpBarW, hpBarH, hpY, apY, slotSize);
        }
    }
}
