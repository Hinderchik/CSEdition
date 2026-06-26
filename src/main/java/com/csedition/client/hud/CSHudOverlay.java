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
 */
@OnlyIn(Dist.CLIENT)
public class CSHudOverlay {

    @SubscribeEvent
    public void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        NamedGuiOverlay overlay = event.getOverlay();
        String id = overlay.id().toString();
        if (id.equals("minecraft:player_health")
                || id.equals("minecraft:food_level")
                || id.equals("minecraft:air_level")
                || id.equals("minecraft:armor_level")) {
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

    private void drawHealthArmor(GuiGraphics g, int w, int h, Player p) {
        int barWidth = 220;
        int barHeight = 10;
        int x = w / 2 - barWidth / 2;
        int y = 12;

        // Панель-фон с угловыми акцентами
        CSRenderUtil.csPanel(g, x - 8, y - 4, barWidth + 16, barHeight * 2 + 12, null, Minecraft.getInstance().font);
        CSRenderUtil.cornerAccents(g, x - 8, y - 4, barWidth + 16, barHeight * 2 + 12, 6, CSRenderUtil.CS_ORANGE);

        // HP — красная полоска с градиентом и сегментацией
        float healthRatio = Math.max(0, p.getHealth() / p.getMaxHealth());
        CSRenderUtil.healthBar(g, x, y, barWidth, barHeight, healthRatio, CSRenderUtil.CS_RED);
        String hpText = String.valueOf((int) p.getHealth());
        g.drawString(Minecraft.getInstance().font, hpText, x + barWidth + 6, y + 1, 0xFFFFFFFF);

        // Броня — синяя полоска
        int y2 = y + barHeight + 4;
        float armorRatio = Math.min(1f, p.getArmorValue() / 20f);
        CSRenderUtil.healthBar(g, x, y2, barWidth, barHeight, armorRatio, CSRenderUtil.CS_BLUE);
        String armorText = String.valueOf(p.getArmorValue());
        g.drawString(Minecraft.getInstance().font, armorText, x + barWidth + 6, y2 + 1, 0xFFFFFFFF);
    }

    private void drawMoney(GuiGraphics g, int w, int h) {
        String money = "$" + ClientState.getMoney();
        var font = Minecraft.getInstance().font;
        int x = w - font.width(money) - 16;
        int y = 12;
        // Панель
        CSRenderUtil.csPanel(g, x - 6, y - 4, font.width(money) + 12, 18, null, font);
        CSRenderUtil.cornerAccents(g, x - 6, y - 4, font.width(money) + 12, 18, 4, CSRenderUtil.CS_GREEN);
        g.drawString(font, money, x, y + 2, CSRenderUtil.CS_GREEN);
    }

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
        String ammo = ammoInMag + " / " + ammoReserve;
        var font = Minecraft.getInstance().font;
        int x = w - font.width(ammo) - 16;
        int y = h - 24;
        CSRenderUtil.csPanel(g, x - 6, y - 4, font.width(ammo) + 12, 18, null, font);
        CSRenderUtil.cornerAccents(g, x - 6, y - 4, font.width(ammo) + 12, 18, 4, CSRenderUtil.CS_ORANGE);
        g.drawString(font, ammo, x, y + 2, 0xFFFFFFFF);
    }

    private void drawPhaseTimer(GuiGraphics g, int w, int h) {
        int seconds = ClientState.getPhaseTicks() / 20;
        String text = ClientState.getPhase().name() + "  " + seconds + "s";
        var font = Minecraft.getInstance().font;
        int x = w / 2 - font.width(text) / 2;
        int y = 36;
        // Свечение под текстом
        CSRenderUtil.glow(g, w / 2, y + 4, 60, CSRenderUtil.withAlpha(CSRenderUtil.CS_YELLOW, 40));
        g.drawString(font, text, x, y, CSRenderUtil.CS_YELLOW);
    }

    private void drawCrosshair(GuiGraphics g, int w, int h) {
        int cx = w / 2;
        int cy = h / 2;
        int size = 6;
        int gap = 2;
        int color = 0xFF55FF55;
        // 4 линии прицела
        g.fill(cx - size - gap, cy, cx - gap, cy + 1, color);
        g.fill(cx + gap + 1, cy, cx + size + gap, cy + 1, color);
        g.fill(cx, cy - size - gap, cx + 1, cy - gap, color);
        g.fill(cx, cy + gap + 1, cx + 1, cy + size + gap, color);
        // Центральная точка
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, color);
    }
}
