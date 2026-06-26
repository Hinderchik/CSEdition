package com.csedition.client;

import com.csedition.data.GamePhase;
import com.csedition.network.PacketMapList;

import java.util.ArrayList;
import java.util.List;

/**
 * Клиентское состояние, синхронизированное с сервера.
 * Сюда пишут пакеты, отсюда читает HUD и GUI.
 */
public class ClientState {
    private static int money = 800;
    private static int kills = 0;
    private static int deaths = 0;
    private static GamePhase phase = GamePhase.LOBBY;
    private static int phaseTicks = 0;
    private static String currentMapId = "";
    private static List<PacketMapList.MapEntry> mapList = new ArrayList<>();

    public static void update(int m, int k, int d) {
        money = m; kills = k; deaths = d;
    }

    public static void updatePhase(GamePhase p, int ticks, String mapId) {
        phase = p; phaseTicks = ticks; currentMapId = mapId;
    }

    public static void updateMapList(List<PacketMapList.MapEntry> list) {
        mapList = list;
    }

    public static int getMoney() { return money; }
    public static int getKills() { return kills; }
    public static int getDeaths() { return deaths; }
    public static GamePhase getPhase() { return phase; }
    public static int getPhaseTicks() { return phaseTicks; }
    public static String getCurrentMapId() { return currentMapId; }
    public static List<PacketMapList.MapEntry> getMapList() { return mapList; }
}
