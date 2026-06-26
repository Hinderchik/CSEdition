package com.csedition.client.hud;

import com.csedition.client.ClientState;
import com.csedition.client.render.CSRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Кастомный HUD в стиле CS.
 * Рисуется поверх стандартного (который отключён миксином).
 * Всё процедурно через CSRenderUtil — никаких PNG.
 *
 * Расположение:
 *   - HP/Броня: левый нижний угол (как в CS)
 *   - Деньги: правый верхний угол
 *   - Патроны: правый нижний угол (компактно)
 *   - Таймер фазы: верх по центру
 *   - Прицел: центр экрана
 */
@OnlyIn(Dist.CLIENT)
public class CSHudOverlay {

    // Кэш для оптимизации — не пересчитываем каждый кадр
    private int cachedMoney = -1;
    private int cachedPhase = -1;
    private int cachedPhaseTicks = -1;

    @SubscribeEvent
    public void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        NamedGuiOverlay overlay = event.getOverlay();
        String id = overlay.id().toString();
        if (id.equals("minecraft:player_health")
                || id.equals("minecraft:food_level")
                || id.equals("minecraft:air_level")
                || id.equals("minecraft:armor_level")
                || id.equals("minecraft:hotbar")
                || id.equals("minecraft:experience_bar")) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderPost(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.hideGui) return;

        GuiGraphics g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        drawHealthArmor(g, w, h, mc.player);
        drawMoney(g, w, h);
        drawAmmo(g, w, h, mc.player);
        drawPhaseTimer(g, w, h);
        drawCrosshair(g, w, h);
    }

    /**
     * HP и броня — левый нижний угол (как в CS).
     */
    private void drawHealthArmor(GuiGraphics g, int w, int h, Player p) {
        int barWidth = 160;
        int barHeight = 8;
        int x = 12;
        int y = h - 36;

        // HP — красная полоска
        float healthRatio = Math.max(0, p.getHealth() / p.getMaxHealth());
        CSRenderUtil.healthBar(g, x, y, barWidth, barHeight, healthRatio, CSRenderUtil.CS_RED);
        String hpText = String.valueOf((int) p.getHealth());
        g.drawString(Minecraft.getInstance().font, hpText, x + barWidth + 4, y, 0xFFFFFFFF);

        // Броня — синяя полоска
        int y2 = y + barHeight + 3;
        float armorRatio = Math.min(1f, p.getArmorValue() / 20f);
        CSRenderUtil.healthBar(g, x, y2, barWidth, barHeight, armorRatio, CSRenderUtil.CS_BLUE);
        String armorText = String.valueOf(p.getArmorValue());
        g.drawString(Minecraft.getInstance().font, armorText, x + barWidth + 4, y2, 0xFFFFFFFF);
    }

    /**
     * Деньги — правый верхний угол.
     */
    private void drawMoney(GuiGraphics g, int w, int h) {
        int money = ClientState.getMoney();
        String text = "$" + money;
        var font = Minecraft.getInstance().font;
        int x = w - font.width(text) - 12;
        int y = 10;
        g.drawString(font, text, x, y, CSRenderUtil.CS_GREEN);
    }

    /**
     * Патроны — правый нижний угол, компактно.
     */
    private void drawAmmo(GuiGraphics g, int w, int h, Player p) {
        ItemStack held = p.getMainHandItem();
        int ammoInMag = 0;
        int ammoReserve = 0;
        if (held.hasTag()) {
            var tag = held.getTag();
            if (tag != null) {
                if (tag.contains("AmmoCount")) ammoInMag = tag.getInt("AmmoCount");
                if (tag.contains("AmmoCountMax")) ammoReserve = tag.getInt("AmmoCountMax");
            }
        }
        // Компактный формат: "30 / 90"
        String ammo = ammoInMag + " / " + ammoReserve;
        var font = Minecraft.getInstance().font;
        int x = w - font.width(ammo) - 12;
        int y = h - 18;
        g.drawString(font, ammo, x, y, 0xFFFFFFFF);
    }

    /**
     * Таймер фазы — верх по центру.
     */
    private void drawPhaseTimer(GuiGraphics g, int w, int h) {
        int phase = ClientState.getPhase().ordinal();
        int ticks = ClientState.getPhaseTicks();
        // Кэш: не пересчитываем каждый кадр
        if (phase == cachedPhase && ticks == cachedPhaseTicks) {
            // Можно пропустить, но для плавности всё равно рисуем
        }
        cachedPhase = phase;
        cachedPhaseTicks = ticks;

        int seconds = ticks / 20;
        String text = ClientState.getPhase().name() + "  " + seconds + "s";
        var font = Minecraft.getInstance().font;
        int x = w / 2 - font.width(text) / 2;
        int y = 8;
        g.drawString(font, text, x, y, CSRenderUtil.CS_YELLOW);
    }

    /**
     * Прицел — центр экрана.
     */
    private void drawCrosshair(GuiGraphics g, int w, int h) {
        int cx = w / 2;
        int cy = h / 2;
        int size = 5;
        int gap = 2;
        int color = 0xFF55FF55;
        // 4 линии прицела
        g.fill(cx - size - gap, cy, cx - gap, cy + 1, color);
        g.fill(cx + gap + 1, cy, cx + size + gap, cy + 1, color);
        g.fill(cx, cy - size - gap, cx + 1, cy - gap, color);
        g.fill(cx, cy + gap + 1, cx + 1, cy + size + gap, color);
    }
}
