package com.csedition.client.hud;

import com.csedition.client.ClientState;
import com.csedition.client.ClientScoreboardCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Табло CS2-стиля по TAB.
 * Отменяет ванильный player list и рисует своё.
 * Плавная анимация появления/исчезновения через альфа-множитель.
 * Регистрация ленивая через getInstance() в clientSetup через enqueueWork.
 */
@OnlyIn(Dist.CLIENT)
public class CsScoreboardOverlay {

    // Ленивый синглтон
    private static CsScoreboardOverlay instance = null;

    // Анимация
    private float alpha = 0f;
    private boolean isVisible = false;
    private long lastToggleTime = 0;

    // Константы стиля
    private static final int PAD = 6;
    private static final int ROW_H = 13;
    private static final int HEADER_H = 16;
    private static final int PANEL_MIN_W = 380;
    private static final int PANEL_MAX_W = 560;

    // Цвета CS2
    private static final int COLOR_T   = 0xFFD9A33A;
    private static final int COLOR_CT  = 0xFF5B8FD6;
    private static final int COLOR_GREY = 0xFF6B7280;
    private static final int COLOR_MONEY = 0xFF7ED957;
    private static final int COLOR_SELF_BG = 0x33E8B84B;
    private static final int COLOR_BORDER = 0xFF2A2F36;
    private static final int COLOR_TEXT_WHITE = 0xFFFFFFFF;
    private static final int COLOR_PANEL_BG = 0x99101418;

    private CsScoreboardOverlay() {
        // Конструктор НЕ регистрируется в шине
    }

    public static CsScoreboardOverlay getInstance() {
        if (instance == null) {
            instance = new CsScoreboardOverlay();
        }
        return instance;
    }

    // ====================== Отмена ванильного player list ======================

    @SubscribeEvent
    public void onVanillaPlayerListPre(RenderGuiOverlayEvent.Pre event) {
        if (!"minecraft:player_list".equals(event.getOverlay().id().toString())) return;
        if (isVisible && alpha > 0.01f) {
            event.setCanceled(true);
        }
    }

    // ====================== Рендер своего таблица ======================

    @SubscribeEvent
    public void onRenderPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        boolean tabPressed = mc.options.keyPlayerList.isDown();
        if (tabPressed != isVisible) {
            isVisible = tabPressed;
            lastToggleTime = System.currentTimeMillis();
        }

        // Плавная анимация альфы (200 мс)
        long elapsed = System.currentTimeMillis() - lastToggleTime;
        float targetAlpha = isVisible ? 1f : 0f;
        float speed = 5f; // 200ms = 1/0.2s
        alpha += (targetAlpha - alpha) * Math.min(1f, elapsed * speed / 1000f);
        if (Math.abs(alpha - targetAlpha) < 0.01f) alpha = targetAlpha;
        if (alpha < 0.005f) alpha = 0f;

        if (alpha <= 0f) return;

        drawScoreboard(event.getGuiGraphics(), mc);
    }

    private void drawScoreboard(GuiGraphics g, Minecraft mc) {
        Font font = mc.font;
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        ClientScoreboardCache cache = ClientScoreboardCache.getInstance();
        if (cache.tPlayers.isEmpty() && cache.ctPlayers.isEmpty()) return;

        int panelW = Math.min(PANEL_MAX_W, Math.max(PANEL_MIN_W, w - 40));
        int panelX = (w - panelW) / 2;
        int panelTop = 40;
        int panelBottom = h - 20;

        // Вычисляем необходимую высоту
        int maxPlayers = Math.max(cache.tPlayers.size(), cache.ctPlayers.size());
        int contentH = maxPlayers * ROW_H + HEADER_H * 2 + PAD * 4;
        int panelH = Math.min(panelBottom - panelTop, Math.max(contentH, 140));

        // Фон панели с текущей альфой
        int bgCol = alphaToColor(COLOR_PANEL_BG);
        int borderCol = alphaToColor(COLOR_BORDER);
        g.fill(panelX, panelTop, panelX + panelW, panelTop + panelH, bgCol);
        g.renderOutline(panelX, panelTop, panelW, panelH, borderCol);

        int cx = panelX + panelW / 2;

        // Заголовок
        drawHeader(g, font, cx, panelTop, cache);

        // Две колонки
        int colY = panelTop + HEADER_H + PAD;
        int colW = (panelW - PAD * 3) / 2;
        int leftX = panelX + PAD;

        drawTeamColumn(g, font, cache.tPlayers, "T", COLOR_T, leftX, colY, colW, panelBottom, cache);
        drawTeamColumn(g, font, cache.ctPlayers, "CT", COLOR_CT, leftX + colW + PAD, colY, colW, panelBottom, cache);

        // Подвал
        int footerY = panelTop + panelH - PAD - 12;
        int specCount = cache.specPlayers.size();
        int totalOnline = cache.tPlayers.size() + cache.ctPlayers.size() + specCount;
        String footerText = "Spec: " + specCount + "  Online: " + totalOnline;
        int fW = font.width(footerText);
        g.drawString(font, footerText, cx - fW / 2, footerY, mulAlpha(0xFFAAAAAA));
    }

    private void drawHeader(GuiGraphics g, Font font, int cx, int topY, ClientScoreboardCache cache) {
        String mapInfo = cache.mapId.isEmpty() ? "Unnamed Map" : cache.mapId.toUpperCase();
        String modeInfo = cache.modeId.toUpperCase();
        String scoreText = cache.tScore + " : " + cache.ctScore;
        String timerText = formatTimer(cache.phaseTicks);

        // Строка карты
        String line1 = mapInfo + "  [" + modeInfo + "]";
        int l1W = font.width(line1);
        g.drawString(font, line1, cx - l1W / 2, topY + 2, mulAlpha(0xFFAAAAAA));

        // Строка счёта и таймера
        String line2 = cache.tScore + "  [  " + scoreText + "  ]  " + timerText;
        int l2W = font.width(line2);
        g.drawString(font, line2, cx - l2W / 2, topY + HEADER_H, mulAlpha(COLOR_TEXT_WHITE));
    }

    private void drawTeamColumn(GuiGraphics g, Font font, java.util.List<ClientScoreboardCache.Entry> players,
                                 String teamName, int teamColor, int x, int y, int w, int bottom,
                                 ClientScoreboardCache cache) {
        // Заголовок
        String header = teamName + "  (" + players.size() + ")";
        int hw = font.width(header);
        g.drawString(font, header, x + (w - hw) / 2, y, mulAlpha(teamColor));
        y += HEADER_H + 2;

        // Разделитель
        g.fill(x, y, x + w, y + 1, mulAlpha(0x442A2F36));
        y += PAD;

        for (ClientScoreboardCache.Entry e : players) {
            if (y + ROW_H > bottom) break;

            if (e.isSelf) {
                g.fill(x, y, x + w, y + ROW_H, mulAlpha(COLOR_SELF_BG));
            }

            // Имя
            String nameTxt = e.isSelf ? "\u00A7e\u00A7l" + e.name : e.name;
            int nameColor = mulAlpha(e.alive ? COLOR_TEXT_WHITE : COLOR_GREY);
            int nameW = font.width(nameTxt);
            g.drawString(font, nameTxt, x + 4, y + 1, nameColor);

            // Деньги справа
            String moneyTxt = "$" + e.money;
            int mW = font.width(moneyTxt);
            g.drawString(font, moneyTxt, x + w - mW - 4, y + 1, mulAlpha(COLOR_MONEY));

            // K/D или DEAD
            String kdTxt = e.alive ? e.kills + "/" + e.deaths : "DEAD";
            int kdW = font.width(kdTxt);
            int kdColor = mulAlpha(e.alive ? 0xFFAAAAAA : COLOR_GREY);
            g.drawString(font, kdTxt, x + w - mW - kdW - 8, y + 1, kdColor);

            y += ROW_H;
        }
    }

    private static int mulAlpha(int color) {
        int a = (color >>> 24) & 0xFF;
        a = (int) Math.min(255, a * getInstance().alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static int alphaToColor(int color) {
        int a = (color >>> 24) & 0xFF;
        a = (int) (a * getInstance().alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static String formatTimer(int ticks) {
        int seconds = ticks / 20;
        if (seconds <= 0) return "---";
        return seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }
}