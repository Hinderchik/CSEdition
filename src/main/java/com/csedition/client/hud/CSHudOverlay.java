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
 * Кастомный HUD в стиле CS.
 *
 * В лобби (LOBBY) — стандартный ванильный HUD (хотбар, сердечки, голод).
 * В катке (BUY_TIME / FIGHTING / ROUND_END) — кастомный CS HUD.
 *
 * Хотбар НЕ отменяется — оставляем ванильный чтобы игрок видел свои предметы.
 * Кастомный HUD рисуется поверх.
 *
 * ВАЖНО: все элементы рисуются КАЖДЫЙ кадр без кэш-чеков.
 * Раньше были ранние return'ы по cache — это вызывало мерцание текста
 * (текст рисовался, потом не рисовался, потом снова появлялся).
 * Стоимость перерисовки мизерная: пара строк и rect'ов.
 */
@OnlyIn(Dist.CLIENT)
public class CSHudOverlay {

    @SubscribeEvent
    public void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        // В лобби — НЕ отменяем ванильные оверлеи
        if (ClientState.getPhase() == GamePhase.LOBBY) return;

        // В катке отменяем ТОЛЬКО health/armor/hunger, чтобы нарисовать свои.
        // Хотбар НЕ отменяем — игрок должен видеть выданное оружие.
        NamedGuiOverlay overlay = event.getOverlay();
        String id = overlay.id().toString();
        switch (id) {
            case "minecraft:player_health":
            case "minecraft:food_level":
            case "minecraft:air_level":
            case "minecraft:armor_level":
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
        // В лобби — не рисуем кастомный HUD
        if (ClientState.getPhase() == GamePhase.LOBBY) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.hideGui) return;

        GuiGraphics g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        // Всё рисуем каждый кадр — никаких кэшей
        drawHealthArmor(g, w, h, mc.player);
        drawMoney(g, w, h);
        drawAmmo(g, w, h, mc.player);
        drawPhaseTimer(g, w, h);
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

    private void drawHealthArmor(GuiGraphics g, int w, int h, Player p) {
        int barWidth = 160;
        int barHeight = 8;
        int x = 12;
        int y = h - 36;

        float maxHp = p.getMaxHealth();
        float healthRatio = maxHp > 0 ? Math.max(0, p.getHealth() / maxHp) : 0;
        CSRenderUtil.healthBar(g, x, y, barWidth, barHeight, healthRatio, CSRenderUtil.CS_RED);

        Font font = Minecraft.getInstance().font;
        g.drawString(font, String.valueOf((int) p.getHealth()), x + barWidth + 4, y, 0xFFFFFFFF);

        int y2 = y + barHeight + 3;
        int armor = p.getArmorValue();
        float armorRatio = armor > 0 ? Math.min(1f, armor / 20f) : 0;
        CSRenderUtil.healthBar(g, x, y2, barWidth, barHeight, armorRatio, CSRenderUtil.CS_BLUE);
        g.drawString(font, String.valueOf(armor), x + barWidth + 4, y2, 0xFFFFFFFF);
    }

    private void drawMoney(GuiGraphics g, int w, int h) {
        int money = ClientState.getMoney();
        Font font = Minecraft.getInstance().font;
        String text = "$" + money;
        int x = w - font.width(text) - 12;
        g.drawString(font, text, x, 10, CSRenderUtil.CS_GREEN);
    }

    private void drawAmmo(GuiGraphics g, int w, int h, Player p) {
        ItemStack held = p.getMainHandItem();
        int ammoIn = 0, ammoRes = 0;
        if (held.hasTag()) {
            var tag = held.getTag();
            if (tag != null) {
                ammoIn = tag.getInt("AmmoCount");
                ammoRes = tag.getInt("AmmoCountMax");
            }
        }
        Font font = Minecraft.getInstance().font;
        String ammo = ammoIn + " / " + ammoRes;
        int x = w - font.width(ammo) - 12;
        g.drawString(font, ammo, x, h - 18, 0xFFFFFFFF);
    }

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

        // Тёмный фон чтобы текст всегда читался (и не мерцал при перекрытии оверлеями)
        int bgX1 = cx - textW / 2 - 8;
        int bgX2 = cx + textW / 2 + 8;
        g.fill(bgX1, 4, bgX2, 22, 0xCC000000);
        g.fill(bgX1, 18, bgX2, 19, CSRenderUtil.CS_ORANGE);
        // Угловые акценты
        g.fill(bgX1, 4, bgX1 + 6, 5, CSRenderUtil.CS_ORANGE);
        g.fill(bgX1, 4, bgX1 + 1, 10, CSRenderUtil.CS_ORANGE);
        g.fill(bgX2 - 6, 4, bgX2, 5, CSRenderUtil.CS_ORANGE);
        g.fill(bgX2 - 1, 4, bgX2, 10, CSRenderUtil.CS_ORANGE);

        g.drawString(font, text, cx - textW / 2, 9, CSRenderUtil.CS_YELLOW);
    }
}
