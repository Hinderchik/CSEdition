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
    private static String mapsJson = "";  // Полный maps.json с сервера
    private static String modesJson = ""; // Полный modes.json с сервера
    private static String selectedModeId = "classic"; // Выбранный режим на клиенте (для GUI)

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
