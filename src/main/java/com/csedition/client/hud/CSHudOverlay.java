package com.csedition.client.hud;

import com.csedition.client.ClientState;
import com.csedition.client.render.CSRenderUtil;
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
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class CSHudOverlay {

    private static final CSHudOverlay INSTANCE = new CSHudOverlay();

    private static final int HOTBAR_SLOTS = 3;
    private static final int SLOT_GAP = 4;

    private int lastHpInt = -1;
    private int lastArmor = -1;
    private int lastMoney = Integer.MIN_VALUE;
    private int lastPhaseTicks = Integer.MIN_VALUE;
    private GamePhase lastPhase = null;

    public CSHudOverlay() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    public static CSHudOverlay getInstance() {
        return INSTANCE;
    }

    @SubscribeEvent
    public void onChatOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (!"minecraft:chat".equals(event.getOverlay().id().toString())) return;
        if (!shouldRender()) return;
        doRender(event.getGuiGraphics());
    }

    private boolean shouldRender() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        if (mc.options.hideGui) return false;
        if (mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)) {
            return false;
        }
        return ClientState.getPhase() != GamePhase.LOBBY;
    }

    private void doRender(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        drawHealthArmor(g, p, h);
        drawMoney(g, w);
        drawPhaseTimer(g, w);
        drawScoreboard(g, w);
        drawHotbar(g, p, w, h);
    }

    private void drawHealthArmor(GuiGraphics g, Player p, int screenH) {
        Font font = Minecraft.getInstance().font;
        int barW = Math.max(120, mcScale(90));
        int barH = Math.max(4, mcScale(3));
        int x = mcScale(8);
        int hpY = screenH - mcScale(48);

        float maxHp = Math.max(1f, p.getMaxHealth());
        float health = Math.max(0f, p.getHealth());
        float hpRatio = Math.min(1f, health / maxHp);
        int armor = p.getArmorValue();
        float apRatio = Math.min(1f, armor / 20f);

        g.fill(x - 1, hpY - 1, x + barW + 1, hpY + barH + 1, 0xFF000000);
        g.fill(x, hpY, x + barW, hpY + barH, 0xFF1A0A0A);
        int hpFill = (int) (barW * hpRatio);
        if (hpFill > 0) g.fill(x, hpY, x + hpFill, hpY + barH, CSRenderUtil.CS_RED);
        for (int i = 1; i < 4; i++) {
            int sx = x + (barW * i / 4);
            g.fill(sx, hpY, sx + 1, hpY + barH, 0x66000000);
        }
        g.drawString(font, "HP", x, hpY - 9, CSRenderUtil.CS_RED);
        int hpInt = (int) health;
        if (hpInt != lastHpInt) {
            g.drawString(font, String.valueOf(hpInt), x + barW + 4, hpY, 0xFFFFFFFF);
            lastHpInt = hpInt;
        }

        int apY = hpY + barH + mcScale(6);
        g.fill(x - 1, apY - 1, x + barW + 1, apY + barH + 1, 0xFF000000);
        g.fill(x, apY, x + barW, apY + barH, 0xFF1A0A0A);
        int apFill = (int) (barW * apRatio);
        if (apFill > 0) g.fill(x, apY, x + apFill, apY + barH, CSRenderUtil.CS_BLUE);
        for (int i = 1; i < 4; i++) {
            int sx = x + (barW * i / 4);
            g.fill(sx, apY, sx + 1, apY + barH, 0x66000000);
        }
        g.drawString(font, "AP", x, apY - 9, CSRenderUtil.CS_BLUE);
        if (armor != lastArmor) {
            g.drawString(font, String.valueOf(armor), x + barW + 4, apY, 0xFFFFFFFF);
            lastArmor = armor;
        }
    }

    private void drawMoney(GuiGraphics g, int screenW) {
        int money = ClientState.getMoney();
        Font font = Minecraft.getInstance().font;
        String text = "$" + money;
        int padX = mcScale(8);
        int boxH = mcScale(18);
        int textW = font.width(text);
        int boxX = screenW - textW - padX * 2 - mcScale(8);
        int boxY = mcScale(8);

        g.fill(boxX, boxY, boxX + textW + padX * 2, boxY + boxH, 0xCC000000);
        g.fill(boxX, boxY, boxX + 2, boxY + boxH, CSRenderUtil.CS_ORANGE);
        if (money != lastMoney) {
            g.drawString(font, text, boxX + padX, boxY + mcScale(5), CSRenderUtil.CS_GREEN);
            lastMoney = money;
        }
    }

    private void drawPhaseTimer(GuiGraphics g, int screenW) {
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
        int cx = screenW / 2;
        int bgY2 = mcScale(22);
        int bgY1 = mcScale(4);
        int padX = mcScale(10);

        int bgX1 = cx - textW / 2 - padX;
        int bgX2 = cx + textW / 2 + padX;

        g.fill(bgX1, bgY1, bgX2, bgY2, 0xCC000000);
        g.fill(bgX1, bgY2 - 4, bgX2, bgY2 - 3, CSRenderUtil.CS_ORANGE);

        if (phase != lastPhase || ticks != lastPhaseTicks) {
            g.fill(bgX1 + 1, bgY1 + 1, bgX2 - 1, bgY2 - 5, 0xCC000000);
            g.drawString(font, text, cx - textW / 2, bgY1 + mcScale(5), CSRenderUtil.CS_YELLOW);
            lastPhase = phase;
            lastPhaseTicks = ticks;
        }
    }

    private void drawScoreboard(GuiGraphics g, int screenW) {
        MatchManager mm = MatchManager.getInstance();
        if (mm == null) return;
        int tScore = mm.getRoundsWon(Team.T);
        int ctScore = mm.getRoundsWon(Team.CT);
        int target = mm.getCurrentMode().getRoundsToWin();
        Font font = Minecraft.getInstance().font;
        int cx = screenW / 2;
        int y = mcScale(28);

        String tText = "T " + tScore;
        String ctText = "CT";
        int tW = font.width(tText);
        int colonW = font.width(" : ");
        int ctW = font.width(ctText);
        int slashW = font.width(" / " + target);
        int totalW = tW + colonW + ctW + slashW;
        int bgX1 = cx - totalW / 2 - 6;
        int bgX2 = cx + totalW / 2 + 6;

        g.fill(bgX1, y, bgX2, y + mcScale(14), 0xCC000000);
        g.drawString(font, tText, bgX1 + 4, y + 3, CSRenderUtil.CS_ORANGE);
        g.drawString(font, " : ", bgX1 + 4 + tW, y + 3, 0xFFFFFFFF);
        g.drawString(font, ctText, bgX1 + 4 + tW + colonW, y + 3, CSRenderUtil.CS_BLUE);
        g.drawString(font, " / " + target, bgX1 + 4 + tW + colonW + ctW, y + 3, CSRenderUtil.CS_YELLOW);
    }

    private void drawHotbar(GuiGraphics g, Player p, int screenW, int screenH) {
        Font font = Minecraft.getInstance().font;
        int slotSize = Math.max(28, mcScale(14));
        int totalH = HOTBAR_SLOTS * slotSize + (HOTBAR_SLOTS - 1) * SLOT_GAP;
        int hotbarX = screenW - slotSize - mcScale(8);
        int hotbarY = screenH - totalH - mcScale(8);

        int selected = p.getInventory().selected;

        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            int slotY = hotbarY + i * (slotSize + SLOT_GAP);
            ItemStack stack = p.getInventory().getItem(i);
            boolean isSelected = (i == selected);

            int bgColor = isSelected ? 0xEE3A2A1A : 0xEE0E0E0E;
            g.fill(hotbarX, slotY, hotbarX + slotSize, slotY + slotSize, bgColor);

            if (isSelected) {
                g.renderOutline(hotbarX, slotY, slotSize, slotSize, CSRenderUtil.CS_ORANGE);
            } else {
                int border = 0xFF3A3A3A;
                g.fill(hotbarX, slotY, hotbarX + slotSize, slotY + 1, border);
                g.fill(hotbarX, slotY + slotSize - 1, hotbarX + slotSize, slotY + slotSize, border);
                g.fill(hotbarX, slotY, hotbarX + 1, slotY + slotSize, border);
                g.fill(hotbarX + slotSize - 1, slotY, hotbarX + slotSize, slotY + slotSize, border);
            }

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

            String num = String.valueOf(i + 1);
            int numColor = isSelected ? CSRenderUtil.CS_ORANGE : 0xFF888888;
            g.drawString(font, num,
                    hotbarX - font.width(num) - 4,
                    slotY + slotSize / 2 - 4,
                    numColor);
        }
    }

    private static int mcScale(int base) {
        Minecraft mc = Minecraft.getInstance();
        int guiScale = (int) mc.getWindow().getGuiScale();
        return Math.max(1, base * guiScale / 3);
    }
}
