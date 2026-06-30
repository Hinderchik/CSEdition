package com.csedition.client;

import com.csedition.data.GamePhase;
import com.csedition.network.PacketMapList;

import java.util.ArrayList;
import java.util.List;

/**
 * Клиентское состояние, синхронизированное с сервера.
 * Сюда пишут пакеты, отсюда читает HUD и GUI.
 *
 * Карты (mapsJson) приходят с сервера через PacketSyncMaps.
 * Режимы (modesJson) приходят с сервера через PacketSyncModes.
 * Клиенту не нужны свои файлы — всё приходит по сети.
 */
public class ClientState {
    private static int money = 800;
    private static int kills = 0;
    private static int deaths = 0;
    private static GamePhase phase = GamePhase.LOBBY;
    private static int phaseTicks = 0;
    private static String currentMapId = "";
    private static String currentModeId = "classic";
    private static List<PacketMapList.MapEntry> mapList = new ArrayList<>();
    private static String mapsJson = "";
    private static String modesJson = "";
    private static String selectedModeId = "classic";

    // Данные для экрана окончания раунда
    private static boolean showRoundEndScreen = false;
    private static String roundEndWinner = "";
    private static String roundEndReason = "";
    private static int roundEndRound = 0;
    private static int roundEndTopKills = -1;
    private static long roundEndStartTime = 0;
    private static final long ROUND_END_DISPLAY_MS = 5000; // 5 секунд

    public static void update(int m, int k, int d) {
        money = m; kills = k; deaths = d;
    }

    public static void updatePhase(GamePhase p, int ticks, String mapId, String modeId) {
        phase = p; phaseTicks = ticks;
        currentMapId = mapId == null ? "" : mapId;
        currentModeId = modeId == null || modeId.isEmpty() ? "classic" : modeId;
    }

    public static void updateMapList(List<PacketMapList.MapEntry> list) {
        mapList = list;
    }

    public static void setMapsJson(String json) {
        mapsJson = json == null ? "" : json;
    }

    public static void setModesJson(String json) {
        modesJson = json == null ? "" : json;
    }

    public static void setSelectedModeId(String modeId) {
        selectedModeId = modeId == null || modeId.isEmpty() ? "classic" : modeId;
    }

    /**
     * Показывает экран окончания раунда/матча.
     */
    public static void showRoundEnd(String winner, String reason, int round, int topKills) {
        showRoundEndScreen = true;
        roundEndWinner = winner;
        roundEndReason = reason;
        roundEndRound = round;
        roundEndTopKills = topKills;
        roundEndStartTime = System.currentTimeMillis();
    }

    /**
     * Проверяет, нужно ли ещё показывать экран окончания раунда.
     */
    public static boolean shouldShowRoundEnd() {
        if (!showRoundEndScreen) return false;
        if (System.currentTimeMillis() - roundEndStartTime > ROUND_END_DISPLAY_MS) {
            showRoundEndScreen = false;
            return false;
        }
        return true;
    }

    public static String getRoundEndWinner() { return roundEndWinner; }
    public static String getRoundEndReason() { return roundEndReason; }
    public static int getRoundEndRound() { return roundEndRound; }
    public static int getRoundEndTopKills() { return roundEndTopKills; }

    public static int getMoney() { return money; }
    public static int getKills() { return kills; }
    public static int getDeaths() { return deaths; }
    public static GamePhase getPhase() { return phase; }
    public static int getPhaseTicks() { return phaseTicks; }
    public static String getCurrentMapId() { return currentMapId; }
    public static String getCurrentModeId() { return currentModeId; }
    public static List<PacketMapList.MapEntry> getMapList() { return mapList; }
    public static String getMapsJson() { return mapsJson; }
    public static String getModesJson() { return modesJson; }
    public static String getSelectedModeId() { return selectedModeId; }
}
