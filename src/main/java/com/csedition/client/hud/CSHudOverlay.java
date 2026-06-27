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
 * Оптимизирован для максимального FPS:
 *   - Кэширование значений (не пересчитываем каждый кадр)
 *   - Пропуск отрисовки при отсутствии изменений
 *   - Минимум аллокаций в горячем пути
 *   - Прямые вызовы fill() вместо сложных утилит
 *
 * В лобби (LOBBY) показывается стандартный ванильный HUD (сердечки, голод, хотбар).
 * В катке (BUY_TIME / FIGHTING / ROUND_END) — кастомный CS HUD.
 */
@OnlyIn(Dist.CLIENT)
public class CSHudOverlay {

    // Кэш для пропуска отрисовки при отсутствии изменений
    private int cachedMoney = Integer.MIN_VALUE;
    private int cachedPhase = Integer.MIN_VALUE;
    private int cachedPhaseTicks = Integer.MIN_VALUE;
    private float cachedHealth = -1f;
    private int cachedArmor = -1;
    private int cachedAmmoIn = -1;
    private int cachedAmmoRes = -1;
    private int cachedW = -1, cachedH = -1;

    // Номер последнего кадра, в котором мы рисовали HUD
    private long lastRenderFrame = -1;

    @SubscribeEvent
    public void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        // В лобби — НЕ отключаем ванильные оверлеи (показываем стандартный HUD)
        if (ClientState.getPhase() == GamePhase.LOBBY) return;

        NamedGuiOverlay overlay = event.getOverlay();
        String id = overlay.id().toString();
        // В катке — отключаем стандартные оверлеи, рисуем свои
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
        // В лобби — не рисуем кастомный HUD (используется ванильный)
        if (ClientState.getPhase() == GamePhase.LOBBY) return;

        // Рисуем HUD только один раз за кадр.
        Minecraft mc = Minecraft.getInstance();
        long frameId = System.nanoTime() / 16_666_666L;
        if (frameId == lastRenderFrame) return;
        lastRenderFrame = frameId;

        if (mc.player == null || mc.level == null) return;
        if (mc.options.hideGui) return;

        GuiGraphics g = event.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        // Если размер окна изменился — сбрасываем кэш
        if (w != cachedW || h != cachedH) {
            cachedW = w; cachedH = h;
            cachedMoney = Integer.MIN_VALUE;
            cachedPhase = Integer.MIN_VALUE;
            cachedPhaseTicks = Integer.MIN_VALUE;
            cachedHealth = -1f;
            cachedArmor = -1;
            cachedAmmoIn = -1;
            cachedAmmoRes = -1;
        }

        drawHealthArmor(g, w, h, mc.player);
        drawMoney(g, w, h);
        drawAmmo(g, w, h, mc.player);
        drawPhaseTimer(g, w, h);
        drawRoundEndScreen(g, w, h);
    }

    private void drawRoundEndScreen(GuiGraphics g, int w, int h) {
        if (!ClientState.shouldShowRoundEnd()) return;
        // Затемнение фона
        g.fill(0, 0, w, h, 0x80000000);
        // Заголовок
        Font font = Minecraft.getInstance().font;
        String winner = ClientState.getRoundEndWinner();
        String reason = ClientState.getRoundEndReason();
        int round = ClientState.getRoundEndRound();
        int topKills = ClientState.getRoundEndTopKills();

        String title;
        int titleColor;
        if ("TARGET_KILLS".equals(reason)) {
            title = "MATCH OVER";
            titleColor = 0xFFFFAA00;
        } else {
            title = "ROUND " + round + " OVER";
            titleColor = 0xFFFFFFFF;
        }
        int titleX = w / 2 - font.width(title) / 2;
        g.drawString(font, title, titleX, h / 2 - 40, titleColor);

        String winnerText = "Winner: " + winner;
        int winnerX = w / 2 - font.width(winnerText) / 2;
        g.drawString(font, winnerText, winnerX, h / 2 - 20, 0xFF55FF55);

        String reasonText = switch (reason) {
            case "ELIMINATION" -> "All enemies eliminated";
            case "TIME_OUT" -> "Time ran out";
            case "TARGET_KILLS" -> "Target kills reached (" + topKills + ")";
            default -> reason;
        };
        int reasonX = w / 2 - font.width(reasonText) / 2;
        g.drawString(font, reasonText, reasonX, h / 2, 0xFFCCCCCC);
    }

    private void drawHealthArmor(GuiGraphics g, int w, int h, Player p) {
        float health = p.getHealth();
        int armor = p.getArmorValue();

        // Пропуск если ничего не изменилось
        if (health == cachedHealth && armor == cachedArmor) return;
        cachedHealth = health;
        cachedArmor = armor;

        int barWidth = 160;
        int barHeight = 8;
        int x = 12;
        int y = h - 36;

        float maxHp = p.getMaxHealth();
        float healthRatio = maxHp > 0 ? Math.max(0, health / maxHp) : 0;
        CSRenderUtil.healthBar(g, x, y, barWidth, barHeight, healthRatio, CSRenderUtil.CS_RED);

        Font font = Minecraft.getInstance().font;
        g.drawString(font, String.valueOf((int) health), x + barWidth + 4, y, 0xFFFFFFFF);

        int y2 = y + barHeight + 3;
        float armorRatio = armor > 0 ? Math.min(1f, armor / 20f) : 0;
        CSRenderUtil.healthBar(g, x, y2, barWidth, barHeight, armorRatio, CSRenderUtil.CS_BLUE);
        g.drawString(font, String.valueOf(armor), x + barWidth + 4, y2, 0xFFFFFFFF);
    }

    private void drawMoney(GuiGraphics g, int w, int h) {
        int money = ClientState.getMoney();
        if (money == cachedMoney) return;
        cachedMoney = money;

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
        if (ammoIn == cachedAmmoIn && ammoRes == cachedAmmoRes) return;
        cachedAmmoIn = ammoIn;
        cachedAmmoRes = ammoRes;

        Font font = Minecraft.getInstance().font;
        String ammo = ammoIn + " / " + ammoRes;
        int x = w - font.width(ammo) - 12;
        g.drawString(font, ammo, x, h - 18, 0xFFFFFFFF);
    }

    private void drawPhaseTimer(GuiGraphics g, int w, int h) {
        int phase = ClientState.getPhase().ordinal();
        int ticks = ClientState.getPhaseTicks();
        if (phase == cachedPhase && ticks == cachedPhaseTicks) return;
        cachedPhase = phase;
        cachedPhaseTicks = ticks;

        Font font = Minecraft.getInstance().font;
        String text = ClientState.getPhase().name() + "  " + (ticks / 20) + "s";
        int x = w / 2 - font.width(text) / 2;
        g.drawString(font, text, x, 8, CSRenderUtil.CS_YELLOW);
    }
}
