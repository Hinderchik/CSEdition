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
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Кастомный HUD в стиле CS Mobile / BLOCKPOST mobile.
 *
 * Хотбар — ВЕРТИКАЛЬНЫЙ, 3 слота, справа снизу (как мобильный FPS).
 *
 * Оптимизация FPS:
 *   1. Рисуем только в Post одного конкретного overlay (minecraft:hotbar) —
 *      это гарантирует ровно 1 draw/кадр без агрессивного millis-дедупа.
 *   2. Кэш значений HP/AP/money/фазы — пропускаем fill и текст если не изменились.
 *   3. Используем fill вместо gradient где возможно (Tesselator дорогой).
 *   4. Никаких outline для не-выбранных слотов — только выбранный с рамкой.
 *   5. renderItem вызывается каждый кадр (кэш renderItem сломан: каждый кадр
 *      frame buffer очищается, skip = предмет исчезает).
 */
@OnlyIn(Dist.CLIENT)
public class CSHudOverlay {

    private static final int HOTBAR_SLOTS = 3;
    private static final int SLOT_GAP = 4;

    /** Кэш значений — пропускаем перерисовку блоков если не изменились. */
    private float lastHealthRatio = -1f;
    private int lastArmorValue = -1;
    private int lastHpInt = -1;
    private int lastMoney = Integer.MIN_VALUE;
    private int lastPhaseTicks = Integer.MIN_VALUE;
    private GamePhase lastPhase = null;

    /** Кэш количества предметов в слоте — пропускаем только текст числа. */
    private final int[] lastItemCount = new int[HOTBAR_SLOTS];

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

    /**
     * Public render method called by the custom overlay
     * (registered via RegisterGuiOverlaysEvent) AND by the old
     * RenderGameOverlayEvent.Post fallback (below).
     *
     * A frame counter prevents double-rendering when both paths fire
     * in the same frame — only the first call draws, subsequent calls
     * in the same frame are skipped.
     */
    private long lastFrameDrawn = -1;

    public void render(GuiGraphics g) {
        if (ClientState.getPhase() == GamePhase.LOBBY) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.hideGui) return;

        // Prevent double-render if both paths fire same frame
        long frame = mc.getFrameTime(); // nanos
        if (frame == lastFrameDrawn) return;
        lastFrameDrawn = frame;

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        Layout layout = Layout.forScreen(w, h);

        drawHealthArmor(g, w, h, mc.player, layout);
        drawMoney(g, w, h, layout);
        drawPhaseTimer(g, w, h, layout);
        drawHotbar(g, w, h, mc.player, layout);
    }

    /**
     * FALLBACK render path — uses the OLD deprecated RenderGameOverlayEvent
     * which fires reliably in ALL Forge 1.20.x versions regardless of
     * whether the custom overlay registration succeeded.
     * Fires every frame on ElementType.HOTBAR (which we also cancel in Pre
     * via the new API, but Post still fires reliably here).
     *
     * The frame counter in render() prevents double-drawing if both
     * the custom overlay AND this fallback fire in the same frame.
     */
    @SubscribeEvent
    public void onRenderGameOverlayPost(net.minecraftforge.client.event.RenderGameOverlayEvent.Post event) {
        if (event.getType() != net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.HOTBAR) return;
        render(event.getGuiGraphics());
    }

    /**
     * Cancel vanilla hotbar in the OLD API too — belt and suspenders.
     * If the new RenderGuiOverlayEvent.Pre handler doesn't fire for some
     * reason, this ensures the vanilla hotbar is still hidden.
     */
    @SubscribeEvent
    public void onRenderGameOverlayPre(net.minecraftforge.client.event.RenderGameOverlayEvent.Pre event) {
        if (ClientState.getPhase() == GamePhase.LOBBY) return;
        switch (event.getType()) {
            case HOTBAR:
            case PLAYER_HEALTH:
            case FOOD:
            case AIR:
            case ARMOR:
            case EXPERIENCE_BAR:
            case JUMP_BAR:
            case MOUNT_HEALTH:
                event.setCanceled(true);
                break;
            default:
                break;
        }
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
        float armorRatio = armor > 0 ? Math.min(1f, armor / 20f) : 0;

        // Фон и заливка рисуем КАЖДЫЙ кадр (frame buffer очищается каждый кадр).
        // Кэш только для текста — текст "20" / "15" не меняется каждый кадр.
        drawCsBar(g, x, layout.hpY, barW, barH, healthRatio, CSRenderUtil.CS_RED, "HP");
        drawCsBar(g, x, layout.apY, barW, barH, armorRatio, CSRenderUtil.CS_BLUE, "AP");

        int hpInt = (int) health;
        if (hpInt != lastHpInt) {
            g.drawString(font, String.valueOf(hpInt), x + barW + 4, layout.hpY, 0xFFFFFFFF);
            lastHpInt = hpInt;
        }
        if (armor != lastArmorValue) {
            g.drawString(font, String.valueOf(armor), x + barW + 4, layout.apY, 0xFFFFFFFF);
            lastArmorValue = armor;
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
        Font font = Minecraft.getInstance().font;
        String text = "$" + money;
        int padX = layout.scale(8);
        int boxY = layout.scale(8);
        int boxH = layout.scale(18);
        int textW = font.width(text);
        int boxX = w - textW - padX * 2 - layout.scale(8);

        // Фон рисуем КАЖДЫЙ кадр (frame buffer очищается).
        // Текст — только при изменении суммы.
        g.fill(boxX, boxY, boxX + textW + padX * 2, boxY + boxH, 0xCC000000);
        g.fill(boxX, boxY, boxX + 2, boxY + boxH, CSRenderUtil.CS_ORANGE);

        if (money != lastMoney) {
            g.drawString(font, text, boxX + padX, boxY + layout.scale(5), CSRenderUtil.CS_GREEN);
            lastMoney = money;
        }
    }

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

        // Фон рисуем КАЖДЫЙ кадр (frame buffer очищается).
        // Текст (фаза + секунды) — только при изменении значений.
        g.fill(bgX1, bgY1, bgX2, bgY2, 0xCC000000);
        g.fill(bgX1, bgY2 - 4, bgX2, bgY2 - 3, CSRenderUtil.CS_ORANGE);

        if (phase != lastPhase || ticks != lastPhaseTicks) {
            // Затираем старый текст перед рисованием нового (на случай изменения длины)
            g.fill(bgX1 + 1, bgY1 + 1, bgX2 - 1, bgY2 - 5, 0xCC000000);
            g.drawString(font, text, cx - textW / 2, layout.scale(9), CSRenderUtil.CS_YELLOW);
            lastPhase = phase;
            lastPhaseTicks = ticks;
        }
    }

    /**
     * ВЕРТИКАЛЬНЫЙ хотбар — 3 слота столбиком, справа снизу.
     * Фон, рамка и предмет рисуются КАЖДЫЙ кадр (кэш renderItem сломан —
     * frame buffer очищается каждый кадр, skip = предмет исчезает).
     * Кэшируем только текст количества, если оно не изменилось.
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

            // Фон слота — каждый кадр (frame buffer очищается)
            int bgColor = isSelected ? 0xEE3A2A1A : 0xEE0E0E0E;
            g.fill(hotbarX, slotY, hotbarX + slotSize, slotY + slotSize, bgColor);

            // Рамка — каждый кадр
            if (isSelected) {
                g.renderOutline(hotbarX, slotY, slotSize, slotSize, CSRenderUtil.CS_ORANGE);
            } else {
                int border = 0xFF3A3A3A;
                g.fill(hotbarX, slotY, hotbarX + slotSize, slotY + 1, border);
                g.fill(hotbarX, slotY + slotSize - 1, hotbarX + slotSize, slotY + slotSize, border);
                g.fill(hotbarX, slotY, hotbarX + 1, slotY + slotSize, border);
                g.fill(hotbarX + slotSize - 1, slotY, hotbarX + slotSize, slotY + slotSize, border);
            }

            // Предмет — каждый кадр, БЕЗ кэша
            if (!stack.isEmpty()) {
                int itemX = hotbarX + (slotSize - 16) / 2;
                int itemY = slotY + (slotSize - 16) / 2;
                g.renderItem(stack, itemX, itemY);
            }

            // Количество — каждый кадр, БЕЗ кэша (дёшево, string уже в Font кэше)
            if (stack.getCount() > 1) {
                String count = String.valueOf(stack.getCount());
                g.drawString(font, count,
                        hotbarX + slotSize - font.width(count) - 2,
                        slotY + slotSize - 8,
                        0xFFFFFFFF);
            }

            // Номер слота — каждый кадр
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
