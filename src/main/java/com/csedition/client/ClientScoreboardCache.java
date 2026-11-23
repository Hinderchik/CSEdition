package com.csedition.client;

import com.csedition.data.GamePhase;
import com.csedition.network.PacketScoreboardSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Клиентский кэш таблица — обновляется только по пакету, не в рендер-тике.
 * Хранит отсортированные списки T и CT с готовыми ширинами строк.
 */
public class ClientScoreboardCache {

    // Данные из пакета
    public String mapId = "";
    public String modeId = "classic";
    public int phaseTicks = 0;
    public String phaseStr = "FIGHTING";
    public int roundNumber = 0;
    public int tScore = 0;
    public int ctScore = 0;

    // Отсортированные списки
    public List<Entry> tPlayers = new ArrayList<>();
    public List<Entry> ctPlayers = new ArrayList<>();
    public List<Entry> specPlayers = new ArrayList<>();

    // Кэш ширин строк (ключ = имя игрока)
    private final java.util.Map<String, Integer> widthCache = new java.util.HashMap<>();

    private static final ClientScoreboardCache INSTANCE = new ClientScoreboardCache();

    private ClientScoreboardCache() {}

    public static ClientScoreboardCache getInstance() { return INSTANCE; }

    public static void apply(PacketScoreboardSync pkt) {
        INSTANCE.mapId = pkt.mapId != null ? pkt.mapId : "";
        INSTANCE.modeId = pkt.modeId != null ? pkt.modeId : "classic";
        INSTANCE.phaseTicks = pkt.phaseTicks;
        INSTANCE.phaseStr = pkt.phaseStr != null ? pkt.phaseStr : "FIGHTING";
        INSTANCE.roundNumber = pkt.roundNumber;
        INSTANCE.tScore = pkt.tScore;
        INSTANCE.ctScore = pkt.ctScore;

        INSTANCE.tPlayers.clear();
        INSTANCE.ctPlayers.clear();
        INSTANCE.specPlayers.clear();
        INSTANCE.widthCache.clear();

        UUID selfUuid = Minecraft.getInstance().player != null
                ? Minecraft.getInstance().player.getUUID() : null;

        for (PacketScoreboardSync.Entry e : pkt.entries) {
            Entry entry = new Entry(e.uuid, e.name, e.team, e.money, e.alive, e.kills, e.deaths,
                    selfUuid != null && selfUuid.equals(e.uuid));
            switch (e.team) {
                case "T" -> INSTANCE.tPlayers.add(entry);
                case "CT" -> INSTANCE.ctPlayers.add(entry);
                default -> INSTANCE.specPlayers.add(entry);
            }
        }

        // Сортировка: живые первые, затем по убыванию очков (kills)
        INSTANCE.tPlayers.sort(Comparator.comparingInt((Entry en) -> en.isSelf ? 0 : 1)
                .thenComparingInt(en -> -en.kills));
        INSTANCE.ctPlayers.sort(Comparator.comparingInt((Entry en) -> en.isSelf ? 0 : 1)
                .thenComparingInt(en -> -en.kills));
    }

    /** Получить ширину строки с кэшированием. */
    public int getWidth(Font font, String text) {
        Integer cached = widthCache.get(text);
        if (cached != null) return cached;
        int w = font.width(text);
        widthCache.put(text, w);
        return w;
    }

    /** Очистить кэш ширин при ресайзе или полном обновлении. */
    public void invalidateWidths() {
        widthCache.clear();
    }

    // ====================== DTO для кэша ======================

    public static class Entry {
        public final UUID uuid;
        public final String name;
        public final String team;
        public final int money;
        public final boolean alive;
        public final int kills;
        public final int deaths;
        public final boolean isSelf;

        public Entry(UUID uuid, String name, String team, int money,
                     boolean alive, int kills, int deaths, boolean isSelf) {
            this.uuid = uuid;
            this.name = name;
            this.team = team;
            this.money = money;
            this.alive = alive;
            this.kills = kills;
            this.deaths = deaths;
            this.isSelf = isSelf;
        }
    }
}
