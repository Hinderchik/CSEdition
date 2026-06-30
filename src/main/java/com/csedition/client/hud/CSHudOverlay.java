package com.csedition.client.hud;

import com.csedition.client.ClientState;
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

/**
 * CS-style HUD rendered via RenderGuiOverlayEvent.Pre on minecraft:chat
 * (works with OptiFine, Embeddium, Sodium, vanilla).
 *
 * Layout (Counter-Strike style):
 *   - HP bar:  bottom-left, horizontal red bar + number
 *   - AP bar:  below HP, horizontal white bar + number
 *   - Money:   top-right with $ prefix
 *   - Timer:   top-center, large yellow text
 *   - Score:   below timer: "T X : Y CT / rounds"
 *   - Hotbar:  bottom-right, vertical (3 slots) - keep existing
 *
 * No ammo counter (по запросу).
 */
@OnlyIn(Dist.CLIENT)
public class CSHudOverlay {

    private static final CSHudOverlay INSTANCE = new CSHudOverlay();
    private static final int HOTBAR_SLOTS = 3;
    private static final int SLOT_GAP = 3;

    // CS colors
    private static final int CS_HP_COLOR = 0xFFFF2222;       // bright red
    private static final int CS_HP_BG = 0xFF1A0606;           // dark red bg
    private static final int CS_AP_COLOR = 0xFFCCCCDD;        // light blue-white
    private static final int CS_AP_BG = 0xFF06061A;           // dark blue bg
    private static final int CS_MONEY_COLOR = 0xFF55FF55;     // green
    private static final int CS_MONEY_BG = 0xEE000000;
    private static final int CS_TIMER_COLOR = 0xFFFFFF55;     // yellow
    private static final int CS_T_COLOR = 0xFFFF8844;          // orange (T)
    private static final int CS_CT_COLOR = 0xFF4488FF;        // blue (CT)
    private static final int CS_PANEL_BG = 0xEE000000;
    private static final int CS_BORDER = 0xFF1A1A1A;

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

        drawHealth(g, p, h);
        drawArmor(g, p, h);
        drawMoney(g, w);
        drawTimer(g, w);
        drawScoreboard(g, w);
        drawHotbar(g, p, w, h);
    }

    // ====================== HP ======================

    private void drawHealth(GuiGraphics g, Player p, int screenH) {
        Font font = Minecraft.getInstance().font;
        int barW = Math.max(100, mcScale(80));
        int barH = Math.max(3, mcScale(2));
        int x = mcScale(8);
        int y = screenH - mcScale(28);

        float maxHp = Math.max(1f, p.getMaxHealth());
        float hpRatio = Math.min(1f, p.getHealth() / maxHp);

        // bg
        g.fill(x - 1, y - 1, x + barW + 1, y + barH + 1, CS_BORDER);
        g.fill(x, y, x + barW, y + barH, CS_HP_BG);
        // fill
        int fill = (int) (barW * hpRatio);
        if (fill > 0) g.fill(x, y, x + fill, y + barH, CS_HP_COLOR);
        // number
        int hpInt = (int) p.getHealth();
        if (hpInt != lastHpInt) {
            String s = String.valueOf(hpInt);
            g.drawString(font, s, x + barW + 3, y - 1, CS_HP_COLOR);
            lastHpInt = hpInt;
        }
    }

    // ====================== Armor ======================

    private void drawArmor(GuiGraphics g, Player p, int screenH) {
        Font font = Minecraft.getInstance().font;
        int barW = Math.max(100, mcScale(80));
        int barH = Math.max(3, mcScale(2));
        int x = mcScale(8);
        int y = screenH - mcScale(22);

        float apRatio = Math.min(1f, p.getArmorValue() / 20f);

        g.fill(x - 1, y - 1, x + barW + 1, y + barH + 1, CS_BORDER);
        g.fill(x, y, x + barW, y + barH, CS_AP_BG);
        int fill = (int) (barW * apRatio);
        if (fill > 0) g.fill(x, y, x + fill, y + barH, CS_AP_COLOR);
        int armor = p.getArmorValue();
        if (armor != lastArmor) {
            String s = String.valueOf(armor);
            g.drawString(font, s, x + barW + 3, y - 1, CS_AP_COLOR);
            lastArmor = armor;
        }
    }

    // ====================== Money (top-right) ======================

    private void drawMoney(GuiGraphics g, int screenW) {
        int money = ClientState.getMoney();
        Font font = Minecraft.getInstance().font;
        String text = "$" + money;
        int padX = mcScale(6);
        int boxH = mcScale(16);
        int textW = font.width(text);
        int boxX = screenW - textW - padX * 2 - mcScale(8);
        int boxY = mcScale(8);

        g.fill(boxX, boxY, boxX + textW + padX * 2, boxY + boxH, CS_MONEY_BG);
        g.fill(boxX, boxY, boxX + 2, boxY + boxH, 0xFF55FF55);
        if (money != lastMoney) {
            g.drawString(font, text, boxX + padX, boxY + mcScale(4), CS_MONEY_COLOR);
            lastMoney = money;
        }
    }

    // ====================== Round timer (top-center, large) ======================

    private void drawTimer(GuiGraphics g, int screenW) {
        GamePhase phase = ClientState.getPhase();
        int ticks = ClientState.getPhaseTicks();
        Font font = Minecraft.getInstance().font;
        int seconds = ticks / 20;
        int mm = seconds / 60;
        int ss = seconds % 60;
        String time = String.format("%d:%02d", mm, ss);
        int cx = screenW / 2;
        int y = mcScale(8);

        // CS-style: big yellow time + small phase label above
        String label = switch (phase) {
            case BUY_TIME -> "BUY TIME";
            case FIGHTING -> "";
            case ROUND_END -> "ROUND END";
            default -> "";
        };

        if (!label.isEmpty()) {
            int labelW = font.width(label);
            g.drawString(font, label, cx - labelW / 2, y, 0xFFCCCCCC);
        }
        int timeW = font.width(time);
        g.drawString(font, time, cx - timeW / 2, y + mcScale(10), CS_TIMER_COLOR);
    }

    // ====================== Scoreboard (below timer) ======================

    private void drawScoreboard(GuiGraphics g, int screenW) {
        MatchManager mm = MatchManager.getInstance();
        if (mm == null) return;
        int tScore = mm.getRoundsWon(Team.T);
        int ctScore = mm.getRoundsWon(Team.CT);
        int target = mm.getCurrentMode().getRoundsToWin();
        Font font = Minecraft.getInstance().font;
        int cx = screenW / 2;
        int y = mcScale(36);

        String left = "T " + tScore;
        String right = ctScore + " CT";
        String slash = " : ";
        int leftW = font.width(left);
        int slashW = font.width(slash);
        int rightW = font.width(right);
        int totalW = leftW + slashW + rightW;
        int bgX = cx - totalW / 2 - mcScale(4);
        int bgX2 = cx + totalW / 2 + mcScale(4);

        g.fill(bgX, y - 1, bgX2, y + mcScale(10), CS_PANEL_BG);
        g.drawString(font, left, bgX + mcScale(2), y, CS_T_COLOR);
        g.drawString(font, slash, bgX + mcScale(2) + leftW, y, 0xFFFFFFFF);
        g.drawString(font, right, bgX + mcScale(2) + leftW + slashW, y, CS_CT_COLOR);
    }

    // ====================== Hotbar (vertical, right side) ======================

    private void drawHotbar(GuiGraphics g, Player p, int screenW, int screenH) {
        Font font = Minecraft.getInstance().font;
        int slotSize = Math.max(24, mcScale(12));
        int totalH = HOTBAR_SLOTS * slotSize + (HOTBAR_SLOTS - 1) * SLOT_GAP;
        int hotbarX = screenW - slotSize - mcScale(8);
        int hotbarY = screenH - totalH - mcScale(8);

        int selected = p.getInventory().selected;

        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            int slotY = hotbarY + i * (slotSize + SLOT_GAP);
            ItemStack stack = p.getInventory().getItem(i);
            boolean isSelected = (i == selected);

            int bgColor = isSelected ? 0xEE2A2A3A : 0xEE0E0E14;
            g.fill(hotbarX, slotY, hotbarX + slotSize, slotY + slotSize, bgColor);

            // border
            int border = isSelected ? 0xFFFF8844 : 0xFF3A3A3A;
            g.fill(hotbarX, slotY, hotbarX + slotSize, slotY + 1, border);
            g.fill(hotbarX, slotY + slotSize - 1, hotbarX + slotSize, slotY + slotSize, border);
            g.fill(hotbarX, slotY, hotbarX + 1, slotY + slotSize, border);
            g.fill(hotbarX + slotSize - 1, slotY, hotbarX + slotSize, slotY + slotSize, border);

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

            // slot number (small, gray)
            String num = String.valueOf(i + 1);
            g.drawString(font, num,
                    hotbarX - font.width(num) - 3,
                    slotY + 2,
                    0xFF888888);
        }
    }

    private static int mcScale(int base) {
        Minecraft mc = Minecraft.getInstance();
        int guiScale = (int) mc.getWindow().getGuiScale();
        return Math.max(1, base * guiScale / 3);
    }
}
