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
 * Хотбар — ВЕРТИКАЛЬНЫЙ, 3 слота, справа снизу (как мобильный FPS).
 *
 * Оптимизация FPS:
 *   1. Дедуп по фрейму (millis, не nanos — стабильнее на Windows).
 *   2. Кэш renderItem: рендерим ItemStack только если он изменился.
 *      g.renderItem — самая дорогая операция, особенно для TaCZ-моделей.
 *   3. Используем fill вместо gradient где возможно (Tesselator дорогой).
 *   4. Кэш значений HP/AP/money/фазы — пропускаем fill и текст если не изменились.
 *   5. Никаких outline для не-выбранных слотов — только выбранный с рамкой.
 */
@OnlyIn(Dist.CLIENT)
public class CSHudOverlay {

    private static final int HOTBAR_SLOTS = 3;
    private static final int SLOT_GAP = 4;

    /** Дедуп по фрейму — millis стабильнее nanos на разных ОС. */
    private long lastDrawMillis = -1;

    /** Кэш ItemStack для renderItem — рендерим только при изменениях. */
    private final ItemStack[] lastRenderedItem = new ItemStack[HOTBAR_SLOTS];

    /** Кэш значений — пропускаем перерисовку если не изменились. */
    private float lastHealthRatio = -1f;
    private int lastArmorValue = -1;
    private int lastMoney = Integer.MIN_VALUE;
    private int lastPhaseTicks = Integer.MIN_VALUE;
    private GamePhase lastPhase = null;

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

        long now = System.currentTimeMillis();
        if (now == lastDrawMillis) return;
        lastDrawMillis = now;

        GuiGraphics g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        Layout layout = Layout.forScreen(w, h);

        drawHealthArmor(g, w, h, mc.player, layout);
        drawMoney(g, w, h, layout);
        drawPhaseTimer(g, w, h, layout);
        drawHotbar(g, w, h, mc.player, layout);
    }

    private void drawHealthArmor(GuiGraphics g, int w, int h, Player p, Layout layout) {
        Font font = Minecraft.getInstance().font;
        int x = layout.padLeft;
        int barW = layout.hpBarW;
        int barH = layout.hpBarH;

        float maxHp = p.getMaxHealth();
        float health = p.getHealth();
        float healthRatio = maxHp > 0 ? Math.max(0, health / maxHp) : 0;
        int armor = p.getArmorValue();

        boolean healthChanged = healthRatio != lastHealthRatio;
        boolean armorChanged = armor != lastArmorValue;

        if (healthChanged) {
            drawCsBar(g, x, layout.hpY, barW, barH, healthRatio, CSRenderUtil.CS_RED, "HP");
            lastHealthRatio = healthRatio;
        }
        if (armorChanged) {
            float armorRatio = armor > 0 ? Math.min(1f, armor / 20f) : 0;
            drawCsBar(g, x, layout.apY, barW, barH, armorRatio, CSRenderUtil.CS_BLUE, "AP");
            lastArmorValue = armor;
        }

        if (healthChanged) {
            g.drawString(font, String.valueOf((int) health), x + barW + 4, layout.hpY, 0xFFFFFFFF);
        }
        if (armorChanged) {
            g.drawString(font, String.valueOf(armor), x + barW + 4, layout.apY, 0xFFFFFFFF);
        }
    }

    private void drawCsBar(GuiGraphics g, int x, int y, int w, int h,
                           float ratio, int color, String label) {
        ratio = Math.max(0, Math.min(1, ratio));
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        g.fill(x, y, x + w, y + h, 0xFF1A0A0A);
        int fillW = (int) (w * ratio);
        if (fillW > 0) {
            g.fill(x, y, x + fillW, y + h, color);
        }
        for (int i = 1; i < 4; i++) {
            int sx = x + (w * i / 4);
            g.fill(sx, y, sx + 1, y + h, 0x66000000);
        }
        g.drawString(Minecraft.getInstance().font, label, x, y - 9, color);
    }

    private void drawMoney(GuiGraphics g, int w, int h, Layout layout) {
        int money = ClientState.getMoney();
        if (money == lastMoney) return;
        lastMoney = money;

        Font font = Minecraft.getInstance().font;
        String text = "$" + money;
        int padX = layout.scale(8);
        int boxY = layout.scale(8);
        int boxH = layout.scale(18);
        int textW = font.width(text);
        int boxX = w - textW - padX * 2 - layout.scale(8);

        g.fill(boxX, boxY, boxX + textW + padX * 2, boxY + boxH, 0xCC000000);
        g.fill(boxX, boxY, boxX + 2, boxY + boxH, CSRenderUtil.CS_ORANGE);
        g.drawString(font, text, boxX + padX, boxY + layout.scale(5), CSRenderUtil.CS_GREEN);
    }

    private void drawPhaseTimer(GuiGraphics g, int w, int h, Layout layout) {
        GamePhase phase = ClientState.getPhase();
        int ticks = ClientState.getPhaseTicks();
        if (phase == lastPhase && ticks == lastPhaseTicks) return;
        lastPhase = phase;
        lastPhaseTicks = ticks;

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
        g.drawString(font, text, cx - textW / 2, layout.scale(9), CSRenderUtil.CS_YELLOW);
    }

    /**
     * ВЕРТИКАЛЬНЫЙ хотбар — 3 слота столбиком, справа снизу.
     * renderItem с кэшем: ItemStack рисуется только если изменился.
     */
    private void drawHotbar(GuiGraphics g, int w, int h, Player p, Layout layout) {
        Font font = Minecraft.getInstance().font;
        int slotSize = layout.slotSize;
        int slotGap = SLOT_GAP;
        int totalH = HOTBAR_SLOTS * slotSize + (HOTBAR_SLOTS - 1) * slotGap;
        int hotbarX = w - slotSize - layout.scale(8);
        int hotbarY = h - totalH - layout.scale(8);

        int selected = p.getInventory().selected;

        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            int slotY = hotbarY + i * (slotSize + slotGap);
            ItemStack stack = p.getInventory().getItem(i);
            boolean isSelected = (i == selected);

            // Фон слота — простой fill (без gradient — Tesselator дорогой)
            int bgColor = isSelected ? 0xEE3A2A1A : 0xEE0E0E0E;
            g.fill(hotbarX, slotY, hotbarX + slotSize, slotY + slotSize, bgColor);

            // Рамка: выбранный — оранжевая outline, остальные — серая из 4 fill'ов
            if (isSelected) {
                g.renderOutline(hotbarX, slotY, slotSize, slotSize, CSRenderUtil.CS_ORANGE);
            } else {
                int border = 0xFF3A3A3A;
                g.fill(hotbarX, slotY, hotbarX + slotSize, slotY + 1, border);
                g.fill(hotbarX, slotY + slotSize - 1, hotbarX + slotSize, slotY + slotSize, border);
                g.fill(hotbarX, slotY, hotbarX + 1, slotY + slotSize, border);
                g.fill(hotbarX + slotSize - 1, slotY, hotbarX + slotSize, slotY + slotSize, border);
            }

            // КЭШ renderItem — рендерим только если ItemStack изменился
            ItemStack lastStack = lastRenderedItem[i];
            if (!ItemStack.matches(lastStack, stack)) {
                if (!stack.isEmpty()) {
                    int itemX = hotbarX + (slotSize - 16) / 2;
                    int itemY = slotY + (slotSize - 16) / 2;
                    g.renderItem(stack, itemX, itemY);
                    if (stack.getCount() > 1) {
                        String count = String.valueOf(stack.getCount());
                        g.drawString(font, count,
                                hotbarX + slotSize - font.width(count) - 2,
                                slotY + slotSize - 8,
                                0xFFFFFFFF);
                    }
                }
                lastRenderedItem[i] = stack.copy();
            }

            // Номер слота слева — всегда (дёшево)
            String num = String.valueOf(i + 1);
            int numColor = isSelected ? CSRenderUtil.CS_ORANGE : 0xFF888888;
            g.drawString(font, num,
                    hotbarX - font.width(num) - 4,
                    slotY + slotSize / 2 - 4,
                    numColor);
        }
    }

    private record Layout(
            int scale,
            int padLeft,
            int hpBarW,
            int hpBarH,
            int hpY,
            int apY,
            int slotSize
    ) {
        int scale(int base) {
            return Math.max(1, base * scale / 3);
        }

        static Layout forScreen(int w, int h) {
            int minDim = Math.min(w, h);
            int scale = Math.max(1, Math.min(6, minDim / 180));
            int padLeft = Math.max(8, scale * 6);
            int hpBarW = Math.max(120, scale * 90);
            int hpBarH = Math.max(4, scale * 3);
            int slotSize = Math.max(28, scale * 14);
            int hpY = h - slotSize - scale * 32;
            int apY = hpY + hpBarH + scale * 6;
            return new Layout(scale, padLeft, hpBarW, hpBarH, hpY, apY, slotSize);
        }
    }
}