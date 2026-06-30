package com.csedition.match;

import com.csedition.CSEditionMod;
import com.csedition.config.CSConfig;
import com.csedition.config.MapConfig;
import com.csedition.config.ModeConfig;
import com.csedition.data.GameMode;
import com.csedition.data.GamePhase;
import com.csedition.data.MapData;
import com.csedition.data.PlayerData;
import com.csedition.data.Team;
import com.csedition.network.CSPackets;
import com.csedition.network.PacketMapList;
import com.csedition.network.PacketMoneyUpdate;
import com.csedition.network.PacketPhaseUpdate;
import com.csedition.network.PacketRoundEnd;
import com.csedition.event.MatchEvents;
import com.csedition.tacz.TaczHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

/**
 * Р В РІР‚СљР В Р’В»Р В Р’В°Р В Р вЂ Р В Р вЂ¦Р РЋРІР‚в„–Р В РІвЂћвЂ“ Р РЋР С“Р В Р’ВµР РЋР вЂљР В Р вЂ Р В Р’ВµР РЋР вЂљР В Р вЂ¦Р РЋРІР‚в„–Р В РІвЂћвЂ“ Р В РЎВР В Р’ВµР В Р вЂ¦Р В Р’ВµР В РўвЂР В Р’В¶Р В Р’ВµР РЋР вЂљ Р В РЎВР В Р’В°Р РЋРІР‚С™Р РЋРІР‚РЋР В Р’В°.
 * Singleton Р Р†Р вЂљРІР‚Сњ Р В РЎвЂўР В РўвЂР В РЎвЂР В Р вЂ¦ Р В РЎВР В Р’В°Р РЋРІР‚С™Р РЋРІР‚РЋ Р В Р вЂ¦Р В Р’В° Р РЋР С“Р В Р’ВµР РЋР вЂљР В Р вЂ Р В Р’ВµР РЋР вЂљ.
 *
 * Р В Р Р‹Р В РЎвЂўР РЋР С“Р РЋРІР‚С™Р В РЎвЂўР РЋР РЏР В Р вЂ¦Р В РЎвЂР В Р’Вµ:
 *   - phase: Р РЋРІР‚С™Р В Р’ВµР В РЎвЂќР РЋРЎвЂњР РЋРІР‚В°Р В Р’В°Р РЋР РЏ Р РЋРІР‚С›Р В Р’В°Р В Р’В·Р В Р’В° (LOBBY / BUY_TIME / FIGHTING / ROUND_END)
 *   - currentMapId: id Р В Р вЂ Р РЋРІР‚в„–Р В Р’В±Р РЋР вЂљР В Р’В°Р В Р вЂ¦Р В Р вЂ¦Р В РЎвЂўР В РІвЂћвЂ“ Р В РЎвЂќР В Р’В°Р РЋР вЂљР РЋРІР‚С™Р РЋРІР‚в„–
 *   - currentModeId: id Р В Р вЂ Р РЋРІР‚в„–Р В Р’В±Р РЋР вЂљР В Р’В°Р В Р вЂ¦Р В Р вЂ¦Р В РЎвЂўР В РЎвЂ“Р В РЎвЂў Р РЋР вЂљР В Р’ВµР В Р’В¶Р В РЎвЂР В РЎВР В Р’В°
 *   - phaseTicks: Р В РЎвЂўР РЋР С“Р РЋРІР‚С™Р В Р’В°Р В Р вЂ Р РЋРІвЂљВ¬Р В Р’ВµР В Р’ВµР РЋР С“Р РЋР РЏ Р В Р вЂ Р РЋР вЂљР В Р’ВµР В РЎВР РЋР РЏ Р РЋРІР‚С›Р В Р’В°Р В Р’В·Р РЋРІР‚в„– Р В Р вЂ  Р РЋРІР‚С™Р В РЎвЂР В РЎвЂќР В Р’В°Р РЋРІР‚В¦
 *   - playerDataMap: Р В РўвЂР В Р’В°Р В Р вЂ¦Р В Р вЂ¦Р РЋРІР‚в„–Р В Р’Вµ Р В РЎвЂР В РЎвЂ“Р РЋР вЂљР В РЎвЂўР В РЎвЂќР В РЎвЂўР В Р вЂ  (UUID -> PlayerData)
 *   - roundNumber: Р В Р вЂ¦Р В РЎвЂўР В РЎВР В Р’ВµР РЋР вЂљ Р РЋРІР‚С™Р В Р’ВµР В РЎвЂќР РЋРЎвЂњР РЋРІР‚В°Р В Р’ВµР В РЎвЂ“Р В РЎвЂў Р РЋР вЂљР В Р’В°Р РЋРЎвЂњР В Р вЂ¦Р В РўвЂР В Р’В°
 *   - matchOver: true Р В Р’ВµР РЋР С“Р В Р’В»Р В РЎвЂ Р В РЎВР В Р’В°Р РЋРІР‚С™Р РЋРІР‚РЋ Р В РЎвЂўР В РЎвЂќР В РЎвЂўР В Р вЂ¦Р РЋРІР‚РЋР В Р’ВµР В Р вЂ¦ (Р В РЎвЂќР РЋРІР‚С™Р В РЎвЂў-Р РЋРІР‚С™Р В РЎвЂў Р В Р вЂ¦Р В Р’В°Р В Р’В±Р РЋР вЂљР В Р’В°Р В Р’В» killsToWin)
 *
 * Р В Р Р‹Р В РЎвЂР В Р вЂ¦Р РЋРІР‚В¦Р РЋР вЂљР В РЎвЂўР В Р вЂ¦Р В РЎвЂР В Р’В·Р В Р’В°Р РЋРІР‚В Р В РЎвЂР РЋР РЏ Р РЋР С“ Р В РЎвЂќР В Р’В»Р В РЎвЂР В Р’ВµР В Р вЂ¦Р РЋРІР‚С™Р В РЎвЂўР В РЎВ:
 *   - Р В РЎСџР РЋР вЂљР В РЎвЂ Р РЋР С“Р В РЎВР В Р’ВµР В Р вЂ¦Р В Р’Вµ Р РЋРІР‚С›Р В Р’В°Р В Р’В·Р РЋРІР‚в„– Р РЋР вЂљР В Р’В°Р РЋР С“Р РЋР С“Р РЋРІР‚в„–Р В Р’В»Р В Р’В°Р В Р’ВµР РЋРІР‚С™ PacketPhaseUpdate Р В Р вЂ Р РЋР С“Р В Р’ВµР В РЎВ Р В РЎвЂР В РЎвЂ“Р РЋР вЂљР В РЎвЂўР В РЎвЂќР В Р’В°Р В РЎВ.
 *   - Р В РЎСџР РЋР вЂљР В РЎвЂ Р В РЎвЂР В Р’В·Р В РЎВР В Р’ВµР В Р вЂ¦Р В Р’ВµР В Р вЂ¦Р В РЎвЂР В РЎвЂ Р В РўвЂР В Р’ВµР В Р вЂ¦Р В Р’ВµР В РЎвЂ“/Р РЋРЎвЂњР В Р’В±Р В РЎвЂР В РІвЂћвЂ“Р РЋР С“Р РЋРІР‚С™Р В Р вЂ  Р В РЎвЂўР РЋРІР‚С™Р В РЎвЂ”Р РЋР вЂљР В Р’В°Р В Р вЂ Р В Р’В»Р РЋР РЏР В Р’ВµР РЋРІР‚С™ PacketMoneyUpdate Р В РЎвЂќР В РЎвЂўР В Р вЂ¦Р В РЎвЂќР РЋР вЂљР В Р’ВµР РЋРІР‚С™Р В Р вЂ¦Р В РЎвЂўР В РЎВР РЋРЎвЂњ Р В РЎвЂР В РЎвЂ“Р РЋР вЂљР В РЎвЂўР В РЎвЂќР РЋРЎвЂњ.
 *   - Р В РЎСџР РЋР вЂљР В РЎвЂ Р В РЎвЂўР В РЎвЂќР В РЎвЂўР В Р вЂ¦Р РЋРІР‚РЋР В Р’В°Р В Р вЂ¦Р В РЎвЂР В РЎвЂ Р РЋР вЂљР В Р’В°Р РЋРЎвЂњР В Р вЂ¦Р В РўвЂР В Р’В° Р В РЎвЂўР РЋРІР‚С™Р В РЎвЂ”Р РЋР вЂљР В Р’В°Р В Р вЂ Р В Р’В»Р РЋР РЏР В Р’ВµР РЋРІР‚С™ PacketRoundEnd (Р В РЎвЂ”Р В РЎвЂўР В Р’В±Р В Р’ВµР В РўвЂР В РЎвЂР РЋРІР‚С™Р В Р’ВµР В Р’В»Р РЋР Р‰ + Р В РЎвЂ”Р РЋР вЂљР В РЎвЂР РЋРІР‚РЋР В РЎвЂР В Р вЂ¦Р В Р’В°).
 *   - Р В РЎСџР РЋР вЂљР В РЎвЂ Р В Р вЂ Р РЋРІР‚В¦Р В РЎвЂўР В РўвЂР В Р’Вµ Р В РЎвЂР В РЎвЂ“Р РЋР вЂљР В РЎвЂўР В РЎвЂќР В Р’В° Р В РЎвЂўР РЋРІР‚С™Р В РЎвЂ”Р РЋР вЂљР В Р’В°Р В Р вЂ Р В Р’В»Р РЋР РЏР В Р’ВµР РЋРІР‚С™ PacketMapList Р РЋР С“Р В РЎвЂў Р РЋР С“Р В РЎвЂ”Р В РЎвЂР РЋР С“Р В РЎвЂќР В РЎвЂўР В РЎВ Р В РЎвЂќР В Р’В°Р РЋР вЂљР РЋРІР‚С™ Р В РЎвЂ PacketSyncModes.
 */
public class MatchManager {
    private static final MatchManager INSTANCE = new MatchManager();

    public static final int MIN_PLAYERS = 2;

    /**
     * Индекс слота инвентаря (0-based) который защищён от очистки и покупок.
     * В этом слоте лежит нож из мода (MCS2/Knifepack) — мод его не трогает.
     * Слот 4 в UI = индекс 3.
     */
    public static final int KNIFE_SLOT = 3;

    private GamePhase phase = GamePhase.LOBBY;
    private String currentMapId = null;
    private String currentModeId = "classic";
    private int phaseTicks = 0;
    private int roundNumber = 0;
    private boolean matchOver = false;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private final Map<Team, Integer> roundsWon = new java.util.EnumMap<>(Team.class);
    {
        roundsWon.put(Team.T, 0);
        roundsWon.put(Team.CT, 0);
    }
    private final Random random = new Random();
    private PacketPhaseUpdate cachedPhasePacket = null;
    private GamePhase lastBroadcastPhase = null;
    private int lastBroadcastTicks = -1;
    private String lastBroadcastMap = null;
    private String lastBroadcastMode = null;

    private MatchManager() {}

    public static MatchManager getInstance() { return INSTANCE; }

    // ====================== Р В Р’ВР В РЎвЂ“Р РЋР вЂљР В РЎвЂўР В РЎвЂќР В РЎвЂ ======================

    public PlayerData getOrCreate(ServerPlayer player) {
        return playerDataMap.computeIfAbsent(player.getUUID(), PlayerData::new);
    }

    public PlayerData get(UUID uuid) { return playerDataMap.get(uuid); }
    public Map<UUID, PlayerData> getPlayerDataMap() { return playerDataMap; }

    public void onPlayerJoin(ServerPlayer player) {
        getOrCreate(player);
        List<PacketMapList.MapEntry> entries = new ArrayList<>();
        for (MapData m : MapConfig.getMaps().values()) {
            entries.add(new PacketMapList.MapEntry(m.getId(), m.getDisplayName(), m.getModeId()));
        }
        CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PacketMapList(entries));
        CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new com.csedition.network.PacketSyncMaps(MapConfig.toJson()));
        CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new com.csedition.network.PacketSyncModes(ModeConfig.toJson()));
        broadcastPhase();
        teleportToLobby(player);
    }

    public void onPlayerLeave(ServerPlayer player) {
        // Р В РІР‚СњР В Р’В°Р В Р вЂ¦Р В Р вЂ¦Р РЋРІР‚в„–Р В Р’Вµ Р РЋР С“Р В РЎвЂўР РЋРІР‚В¦Р РЋР вЂљР В Р’В°Р В Р вЂ¦Р РЋР РЏР В Р’ВµР В РЎВ
    }

    // ====================== Р В Р’В¤Р В Р’В°Р В Р’В·Р РЋРІР‚в„– ======================

    public GamePhase getPhase() { return phase; }
    public String getCurrentMapId() { return currentMapId; }
    public String getCurrentModeId() { return currentModeId; }
    public MapData getCurrentMap() {
        return currentMapId == null ? null : MapConfig.getMap(currentMapId);
    }
    public GameMode getCurrentMode() {
        return ModeConfig.getOrDefault(currentModeId);
    }
    public int getPhaseTicks() { return phaseTicks; }
    public boolean isMatchOver() { return matchOver; }
    public int getRoundsWon(com.csedition.data.Team team) {
        return roundsWon.getOrDefault(team, 0);
    }

    public void setCurrentMap(String mapId) {
        if (MapConfig.getMap(mapId) != null) {
            this.currentMapId = mapId;
            broadcastPhase();
        }
    }

    public void setCurrentMode(String modeId) {
        if (ModeConfig.getMode(modeId) != null) {
            this.currentModeId = modeId;
            broadcastPhase();
        }
    }

    public void setPhase(GamePhase newPhase) {
        this.phase = newPhase;
        GameMode mode = getCurrentMode();
        switch (newPhase) {
            case BUY_TIME -> this.phaseTicks = mode.getBuyTimeSeconds() * 20;
            case FIGHTING -> this.phaseTicks = mode.getRoundTimeSeconds() * 20;
            case ROUND_END -> this.phaseTicks = 5 * 20;
            default -> this.phaseTicks = 0;
        }
        broadcastPhase();
        CSEditionMod.LOGGER.info("[CS-Edition] Phase -> {} ({} ticks, mode={})", newPhase, phaseTicks, currentModeId);
    }

    public void broadcastPhase() {
        if (cachedPhasePacket == null
                || phase != lastBroadcastPhase
                || phaseTicks != lastBroadcastTicks
                || currentMapId != lastBroadcastMap
                || !Objects.equals(currentModeId, lastBroadcastMode)) {
            cachedPhasePacket = new PacketPhaseUpdate(phase, phaseTicks, currentMapId, currentModeId);
            lastBroadcastPhase = phase;
            lastBroadcastTicks = phaseTicks;
            lastBroadcastMap = currentMapId;
            lastBroadcastMode = currentModeId;
        }
        // Send to ALL connected players, not just those in playerDataMap.
        // If a player hasn't been registered yet (e.g. just joined),
        // they still need the current phase so ClientState updates and HUD shows.
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), cachedPhasePacket);
            }
        } else {
            // Fallback: only registered players
            for (UUID uuid : playerDataMap.keySet()) {
                ServerPlayer sp = getServerPlayer(uuid);
                if (sp != null) {
                    CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), cachedPhasePacket);
                }
            }
        }
    }

    // ====================== Р В РЎС›Р В РЎвЂР В РЎвЂќ ======================

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Р В РЎвЂєР В Р’В±Р РЋР вЂљР В Р’В°Р В Р’В±Р В РЎвЂўР РЋРІР‚С™Р В РЎвЂќР В Р’В° Р В РЎвЂўР РЋРІР‚С™Р В Р’В»Р В РЎвЂўР В Р’В¶Р В Р’ВµР В Р вЂ¦Р В Р вЂ¦Р В РЎвЂўР В РІвЂћвЂ“ Р В РЎвЂўР РЋРІР‚РЋР В РЎвЂР РЋР С“Р РЋРІР‚С™Р В РЎвЂќР В РЎвЂ Р В РЎвЂ”Р В РЎвЂўР РЋР С“Р В Р’В»Р В Р’Вµ Р В РЎвЂўР В РЎвЂќР В РЎвЂўР В Р вЂ¦Р РЋРІР‚РЋР В Р’В°Р В Р вЂ¦Р В РЎвЂР РЋР РЏ Р В РЎВР В Р’В°Р РЋРІР‚С™Р РЋРІР‚РЋР В Р’В°
        if (cleanupTicks > 0) {
            cleanupTicks--;
            if (cleanupTicks <= 0) {
                performMatchEndCleanup();
            }
            return;
        }

        if (phase == GamePhase.LOBBY) return;

        if (phaseTicks > 0) {
            phaseTicks--;
            if (phaseTicks % 20 == 0) broadcastPhase();
        }

        if (phaseTicks <= 0) {
            advancePhase();
        }
    }

    private void advancePhase() {
        switch (phase) {
            case BUY_TIME -> setPhase(GamePhase.FIGHTING);
            case FIGHTING -> endRound(Team.CT, "TIME_OUT");
            case ROUND_END -> startNewRound();
            default -> {}
        }
    }

    // ====================== Р В Р’В Р В Р’В°Р РЋРЎвЂњР В Р вЂ¦Р В РўвЂР РЋРІР‚в„– ======================

    public void startNewRound() {
        int onlineCount = 0;
        for (UUID uuid : playerDataMap.keySet()) {
            if (getServerPlayer(uuid) != null) onlineCount++;
        }
        if (onlineCount < MIN_PLAYERS) {
            for (UUID uuid : playerDataMap.keySet()) {
                ServerPlayer sp = getServerPlayer(uuid);
                if (sp != null) {
                    sp.sendSystemMessage(Component.literal("Not enough players (need " + MIN_PLAYERS + "). Returning to lobby."));
                    teleportToLobby(sp);
                }
            }
            setPhase(GamePhase.LOBBY);
            return;
        }

        roundNumber++;
        MapData map = getCurrentMap();
        if (map == null) {
            setPhase(GamePhase.LOBBY);
            return;
        }

        GameMode mode = getCurrentMode();

        // Сброс денег на старте каждого раунда (нужен только новый инвентарь без отображения нового слота, как слот 1 для отображения нового)
        for (PlayerData pd : playerDataMap.values()) {
            pd.resetForRound(mode.getStartMoney());
        }

        // Очищаем инвентарь при старте НОВОГО матча (round 1) — кроме слота с ножом
        if (roundNumber == 1) {
            for (UUID uuid : playerDataMap.keySet()) {
                ServerPlayer sp = getServerPlayer(uuid);
                if (sp != null) clearInventoryExceptKnifeSlot(sp);
            }
        }

        // Р В РЎС›Р В Р’ВµР В Р’В»Р В Р’ВµР В РЎвЂ”Р В РЎвЂўР РЋР вЂљР РЋРІР‚С™ Р В РЎвЂ Р В Р вЂ Р РЋРІР‚в„–Р В РўвЂР В Р’В°Р РЋРІР‚РЋР В Р’В° Р В РЎвЂўР РЋР вЂљР РЋРЎвЂњР В Р’В¶Р В РЎвЂР РЋР РЏ
        for (UUID uuid : playerDataMap.keySet()) {
            ServerPlayer sp = getServerPlayer(uuid);
            if (sp == null) continue;
            PlayerData pd = playerDataMap.get(uuid);
            BlockPos spawn = map.getRandomSpawn(pd.getTeam(), random);
            if (spawn == null) {
                // Нет ни спавнов ни лобби — не телепортируем, оставляем где есть
                sp.sendSystemMessage(Component.literal("§cNo spawns configured for " + pd.getTeam()
                        + " and no lobby set! Use /cs setlobby and /cs setspawn " + currentMapId + " " + pd.getTeam()));
                giveBaseLoadout(sp, pd, mode);
                sendMoneyUpdate(sp, pd);
                continue;
            }
            sp.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
            giveBaseLoadout(sp, pd, mode);
            sendMoneyUpdate(sp, pd);
        }

        setPhase(GamePhase.BUY_TIME);
    }

    /**
     * Р В РЎвЂєР В РЎвЂќР В РЎвЂўР В Р вЂ¦Р РЋРІР‚РЋР В Р’В°Р В Р вЂ¦Р В РЎвЂР В Р’Вµ Р РЋР вЂљР В Р’В°Р РЋРЎвЂњР В Р вЂ¦Р В РўвЂР В Р’В°.
     * @param winner Р В РЎвЂ”Р В РЎвЂўР В Р’В±Р В Р’ВµР В РўвЂР В РЎвЂР В Р вЂ Р РЋРІвЂљВ¬Р В Р’В°Р РЋР РЏ Р В РЎвЂќР В РЎвЂўР В РЎВР В Р’В°Р В Р вЂ¦Р В РўвЂР В Р’В° (Р В РЎвЂР В Р’В»Р В РЎвЂ null Р В РЎвЂ”Р РЋР вЂљР В РЎвЂ Р В Р вЂ¦Р В РЎвЂР РЋРІР‚РЋР РЋР Р‰Р В Р’ВµР В РІвЂћвЂ“)
     * @param reason Р В РЎвЂ”Р РЋР вЂљР В РЎвЂР РЋРІР‚РЋР В РЎвЂР В Р вЂ¦Р В Р’В°: ELIMINATION, TIME_OUT, TARGET_KILLS
     */
    public void endRound(Team winner, String reason) {
        GameMode mode = getCurrentMode();
        for (PlayerData pd : playerDataMap.values()) {
            if (winner != null && pd.getTeam() == winner) {
                pd.onRoundWin(mode.getRoundWinReward());
            }
        }
        // Track rounds won per team (for MR / CS-style match)
        if (winner != null && roundsWon.containsKey(winner)) {
            roundsWon.put(winner, roundsWon.get(winner) + 1);
        }

        // Р В РЎСџР РЋР вЂљР В РЎвЂўР В Р вЂ Р В Р’ВµР РЋР вЂљР В РЎвЂќР В Р’В° killsToWin Р Р†Р вЂљРІР‚Сњ Р В Р’ВµР РЋР С“Р В Р’В»Р В РЎвЂ Р В РЎвЂќР РЋРІР‚С™Р В РЎвЂў-Р РЋРІР‚С™Р В РЎвЂў Р В Р вЂ¦Р В Р’В°Р В Р’В±Р РЋР вЂљР В Р’В°Р В Р’В» Р В Р вЂ¦Р РЋРЎвЂњР В Р’В¶Р В Р вЂ¦Р В РЎвЂўР В Р’Вµ Р РЋРІР‚РЋР В РЎвЂР РЋР С“Р В Р’В»Р В РЎвЂў Р В РЎвЂќР В РЎвЂР В Р’В»Р В Р’В»Р В РЎвЂўР В Р вЂ , Р В РЎВР В Р’В°Р РЋРІР‚С™Р РЋРІР‚РЋ Р В РЎвЂўР В РЎвЂќР В РЎвЂўР В Р вЂ¦Р РЋРІР‚РЋР В Р’ВµР В Р вЂ¦
        int killsToWin = CSConfig.getKillsToWin();
        PlayerData topKiller = null;
        for (PlayerData pd : playerDataMap.values()) {
            if (pd.getKills() >= killsToWin && (topKiller == null || pd.getKills() > topKiller.getKills())) {
                topKiller = pd;
            }
        }
        if (topKiller != null) {
            // Р В РЎС™Р В Р’В°Р РЋРІР‚С™Р РЋРІР‚РЋ Р В РЎвЂўР В РЎвЂќР В РЎвЂўР В Р вЂ¦Р РЋРІР‚РЋР В Р’ВµР В Р вЂ¦!
            matchOver = true;
            Team matchWinner = topKiller.getTeam();
            String matchReason = "TARGET_KILLS";
            broadcastRoundEnd(matchWinner, matchReason, roundNumber, topKiller.getKills());
            // Р В Р Р‹Р В РЎвЂўР В РЎвЂўР В Р’В±Р РЋРІР‚В°Р В Р’ВµР В Р вЂ¦Р В РЎвЂР В Р’Вµ
            for (UUID uuid : playerDataMap.keySet()) {
                ServerPlayer sp = getServerPlayer(uuid);
                if (sp != null) {
                    sp.sendSystemMessage(Component.literal("=== MATCH OVER === " + matchWinner.name() + " wins! (" + matchReason + ")"));
                    sendMoneyUpdate(sp, playerDataMap.get(uuid));
                }
            }
            setPhase(GamePhase.ROUND_END);
            // Р В Р’В§Р В Р’ВµР РЋР вЂљР В Р’ВµР В Р’В· 5 Р РЋР С“Р В Р’ВµР В РЎвЂќ Р Р†Р вЂљРІР‚Сњ Р РЋРІР‚С™Р В Р’ВµР В Р’В»Р В Р’ВµР В РЎвЂ”Р В РЎвЂўР РЋР вЂљР РЋРІР‚С™ Р В Р вЂ  Р В Р’В»Р В РЎвЂўР В Р’В±Р В Р’В±Р В РЎвЂ Р В РЎвЂ Р В РЎвЂўР РЋРІР‚РЋР В РЎвЂР РЋР С“Р РЋРІР‚С™Р В РЎвЂќР В Р’В° Р В РЎвЂР В Р вЂ¦Р В Р вЂ Р В Р’ВµР В Р вЂ¦Р РЋРІР‚С™Р В Р’В°Р РЋР вЂљР РЋР РЏ
            scheduleMatchEndCleanup();
            return;
        }

        // Check CS-style MR: did the winning team reach roundsToWin?
        int roundsToWin = CSConfig.getEffectiveRoundsToWin(mode.getRoundsToWin());
        Team matchWinner = null;
        if (winner == Team.T && roundsWon.get(Team.T) >= roundsToWin) matchWinner = Team.T;
        else if (winner == Team.CT && roundsWon.get(Team.CT) >= roundsToWin) matchWinner = Team.CT;
        if (matchWinner != null) {
            matchOver = true;
            String matchReason = "ROUNDS_WON";
            broadcastRoundEnd(matchWinner, matchReason, roundNumber, -1);
            for (UUID uuid : playerDataMap.keySet()) {
                ServerPlayer sp = getServerPlayer(uuid);
                if (sp != null) {
                    sp.sendSystemMessage(Component.literal("=== MATCH OVER === " + matchWinner.name() + " wins! ("
                            + roundsWon.get(Team.T) + "-" + roundsWon.get(Team.CT) + ", need " + roundsToWin + ")"));
                    sendMoneyUpdate(sp, playerDataMap.get(uuid));
                }
            }
            setPhase(GamePhase.ROUND_END);
            scheduleMatchEndCleanup();
            return;
        }

        // Continue to next round (announce with current score)
        String reasonText = switch (reason) {
            case "ELIMINATION" -> "All enemies eliminated";
            case "TIME_OUT" -> "Time ran out";
            default -> reason;
        };
        broadcastRoundEnd(winner, reason, roundNumber, -1);
        Component msg = Component.literal("Round " + roundNumber + " won by " + (winner != null ? winner.name() : "DRAW")
                + " (" + reasonText + ") [" + roundsWon.get(Team.T) + "-" + roundsWon.get(Team.CT) + "/" + roundsToWin + "]");
        for (UUID uuid : playerDataMap.keySet()) {
            ServerPlayer sp = getServerPlayer(uuid);
            if (sp != null) {
                sp.sendSystemMessage(msg);
                sendMoneyUpdate(sp, playerDataMap.get(uuid));
            }
        }
        setPhase(GamePhase.ROUND_END);
    }

    /**
     * Р В РЎСџР В Р’ВµР РЋР вЂљР В Р’ВµР В РЎвЂ“Р РЋР вЂљР РЋРЎвЂњР В Р’В·Р В РЎвЂќР В Р’В° Р В РўвЂР В Р’В»Р РЋР РЏ Р В РЎвЂўР В Р’В±Р РЋР вЂљР В Р’В°Р РЋРІР‚С™Р В Р вЂ¦Р В РЎвЂўР В РІвЂћвЂ“ Р РЋР С“Р В РЎвЂўР В Р вЂ Р В РЎВР В Р’ВµР РЋР С“Р РЋРІР‚С™Р В РЎвЂР В РЎВР В РЎвЂўР РЋР С“Р РЋРІР‚С™Р В РЎвЂ.
     */
    public void endRound(Team winner) {
        endRound(winner, "ELIMINATION");
    }

    private void broadcastRoundEnd(Team winner, String reason, int round, int topKills) {
        PacketRoundEnd pkt = new PacketRoundEnd(winner != null ? winner.name() : "DRAW", reason, round, topKills);
        for (UUID uuid : playerDataMap.keySet()) {
            ServerPlayer sp = getServerPlayer(uuid);
            if (sp != null) {
                CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), pkt);
            }
        }
    }

    private int cleanupTicks = -1;

    private void scheduleMatchEndCleanup() {
        cleanupTicks = 5 * 20; // 5 Р РЋР С“Р В Р’ВµР В РЎвЂќР РЋРЎвЂњР В Р вЂ¦Р В РўвЂ
    }

    private void performMatchEndCleanup() {
        // Р В РЎвЂєР РЋРІР‚РЋР В РЎвЂР РЋР С“Р РЋРІР‚С™Р В РЎвЂќР В Р’В° Р В РЎвЂР В Р вЂ¦Р В Р вЂ Р В Р’ВµР В Р вЂ¦Р РЋРІР‚С™Р В Р’В°Р РЋР вЂљР РЋР РЏ (Р РЋР С“ Р РЋРЎвЂњР РЋРІР‚РЋР РЋРІР‚ВР РЋРІР‚С™Р В РЎвЂўР В РЎВ keptItems)
        for (UUID uuid : playerDataMap.keySet()) {
            ServerPlayer sp = getServerPlayer(uuid);
            if (sp == null) continue;
            if (CSConfig.isClearInventoryOnMatchEnd()) {
                clearInventoryKeeping(sp);
            }
            teleportToLobby(sp);
        }
        // Р В Р Р‹Р В Р’В±Р РЋР вЂљР В РЎвЂўР РЋР С“ Р РЋР С“Р РЋРІР‚С™Р В Р’В°Р РЋРІР‚С™Р В РЎвЂР РЋР С“Р РЋРІР‚С™Р В РЎвЂР В РЎвЂќР В РЎвЂ
        for (PlayerData pd : playerDataMap.values()) {
            pd.resetStats();
        }
        matchOver = false;
        roundNumber = 0;
        roundsWon.put(Team.T, 0);
        roundsWon.put(Team.CT, 0);
        setPhase(GamePhase.LOBBY);
    }

    /**
     * Р В РЎвЂєР РЋРІР‚РЋР В РЎвЂР РЋРІР‚В°Р В Р’В°Р В Р’ВµР РЋРІР‚С™ Р В РЎвЂР В Р вЂ¦Р В Р вЂ Р В Р’ВµР В Р вЂ¦Р РЋРІР‚С™Р В Р’В°Р РЋР вЂљР РЋР Р‰, Р РЋР С“Р В РЎвЂўР РЋРІР‚В¦Р РЋР вЂљР В Р’В°Р В Р вЂ¦Р РЋР РЏР РЋР РЏ Р В РЎвЂ”Р РЋР вЂљР В Р’ВµР В РўвЂР В РЎВР В Р’ВµР РЋРІР‚С™Р РЋРІР‚в„– Р В РЎвЂР В Р’В· CSConfig.keptItems.
     */
    private void clearInventoryKeeping(ServerPlayer player) {
        var inv = player.getInventory();
        // Р В Р Р‹Р В РЎвЂўР В Р’В±Р В РЎвЂР РЋР вЂљР В Р’В°Р В Р’ВµР В РЎВ Р РЋРІР‚С™Р В РЎвЂў, Р РЋРІР‚РЋР РЋРІР‚С™Р В РЎвЂў Р В Р вЂ¦Р РЋРЎвЂњР В Р’В¶Р В Р вЂ¦Р В РЎвЂў Р РЋР С“Р В РЎвЂўР РЋРІР‚В¦Р РЋР вЂљР В Р’В°Р В Р вЂ¦Р В РЎвЂР РЋРІР‚С™Р РЋР Р‰
        List<ItemStack> keep = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && CSConfig.shouldKeepItem(stack.getItem())) {
                keep.add(stack.copy());
            }
        }
        inv.clearContent();
        // Р В РІР‚в„ўР В РЎвЂўР В Р’В·Р В Р вЂ Р РЋР вЂљР В Р’В°Р РЋРІР‚В°Р В Р’В°Р В Р’ВµР В РЎВ Р РЋР С“Р В РЎвЂўР РЋРІР‚В¦Р РЋР вЂљР В Р’В°Р В Р вЂ¦Р РЋРІР‚ВР В Р вЂ¦Р В Р вЂ¦Р РЋРІР‚в„–Р В Р’Вµ
        for (ItemStack s : keep) {
            inv.add(s);
        }
    }

    // ====================== Р В РЎСџР В РЎвЂўР В РЎвЂќР РЋРЎвЂњР В РЎвЂ”Р В РЎвЂќР В Р’В° ======================

    public void handleBuyRequest(ServerPlayer player, String gunId) {
        if (player == null) return;
        GameMode mode = getCurrentMode();
        if (!mode.isAllowBuy()) {
            player.sendSystemMessage(Component.literal("Buying is disabled in this mode!"));
            return;
        }
        if (phase != GamePhase.BUY_TIME) {
            player.sendSystemMessage(Component.literal("Buy time is over!"));
            return;
        }
        MapData map = getCurrentMap();
        PlayerData pd = getOrCreate(player);
        if (map == null || !map.isInBuyZone(player.blockPosition(), pd.getTeam())) {
            player.sendSystemMessage(Component.literal("You must be in the buy zone!"));
            return;
        }
        int price = GunPriceTable.getPrice(gunId);
        if (price < 0) {
            player.sendSystemMessage(Component.literal("Unknown weapon."));
            return;
        }
        if (!pd.trySpend(price)) {
            player.sendSystemMessage(Component.literal("Not enough money!"));
            return;
        }

        // === Р В Р Р‹Р В РЎвЂ”Р В Р’ВµР РЋРІР‚В Р РЋР С“Р В Р’В»Р РЋРЎвЂњР РЋРІР‚РЋР В Р’В°Р В РІвЂћвЂ“: Р В Р’В±Р РЋР вЂљР В РЎвЂўР В Р вЂ¦Р РЋР РЏ (kevlar/helmet) ===
        // Р В Р’В­Р РЋРІР‚С™Р В РЎвЂў Р В РЎСљР В РІР‚Сћ TaCZ-Р В РЎвЂ”Р РЋРЎвЂњР РЋРІвЂљВ¬Р В РЎвЂќР В РЎвЂ Р Р†Р вЂљРІР‚Сњ Р В РўвЂР В РЎвЂўР В Р’В±Р В Р’В°Р В Р вЂ Р В Р’В»Р РЋР РЏР В Р’ВµР В РЎВ armor attribute Р В Р вЂ¦Р В Р’В°Р В РЎвЂ”Р РЋР вЂљР РЋР РЏР В РЎВР РЋРЎвЂњР РЋР вЂ№, Р В Р’В±Р В Р’ВµР В Р’В· Р В Р вЂ Р РЋРІР‚в„–Р В РўвЂР В Р’В°Р РЋРІР‚РЋР В РЎвЂ Р В РЎвЂ”Р РЋР вЂљР В Р’ВµР В РўвЂР В РЎВР В Р’ВµР РЋРІР‚С™Р В Р’В°.
        if (isArmorId(gunId)) {
            applyArmor(player, gunId);
            sendMoneyUpdate(player, pd);
            player.sendSystemMessage(Component.literal("§a+" + armorPointsFor(gunId) + " armor"));
            return;
        }

        ItemStack gun = TaczHelper.createGun(gunId);
        if (gun.isEmpty()) {
            pd.addMoney(price);
            player.sendSystemMessage(Component.literal("Weapon not available."));
            return;
        }
        // Р В РЎСџР РЋР вЂљР В РЎвЂўР В Р вЂ Р В Р’ВµР РЋР вЂљР В РЎвЂќР В Р’В° Р В Р’В»Р В РЎвЂР В РЎВР В РЎвЂР РЋРІР‚С™Р В Р’В° Р РЋР С“Р В Р’В»Р В РЎвЂўР РЋРІР‚С™Р В РЎвЂўР В Р вЂ 
        if (!hasInventorySpace(player)) {
            pd.addMoney(price);
            player.sendSystemMessage(Component.literal("Inventory full! Max " + CSConfig.getMaxInventorySlots() + " slots."));
            return;
        }
        player.getInventory().add(gun);
        sendMoneyUpdate(player, pd);
    }

    /**
     * Р В Р’В­Р РЋРІР‚С™Р В РЎвЂў Р В Р’В±Р РЋР вЂљР В РЎвЂўР В Р вЂ¦Р РЋР РЏ (kevlar/helmet), Р В Р’В° Р В Р вЂ¦Р В Р’Вµ TaCZ-Р В РЎвЂ”Р РЋРЎвЂњР РЋРІвЂљВ¬Р В РЎвЂќР В Р’В°.
     */
    private static boolean isArmorId(String gunId) {
        return "tacz:kevlar".equals(gunId) || "tacz:helmet".equals(gunId);
    }

    /**
     * Р В Р Р‹Р В РЎвЂќР В РЎвЂўР В Р’В»Р РЋР Р‰Р В РЎвЂќР В РЎвЂў armor-Р В РЎвЂўР РЋРІР‚РЋР В РЎвЂќР В РЎвЂўР В Р вЂ  Р В РўвЂР В Р’В°Р РЋРІР‚ВР РЋРІР‚С™ Р В РЎвЂ”Р РЋР вЂљР В Р’ВµР В РўвЂР В РЎВР В Р’ВµР РЋРІР‚С™.
     */
    private static int armorPointsFor(String gunId) {
        return "tacz:helmet".equals(gunId) ? 100 : 50; // Р РЋРІвЂљВ¬Р В Р’В»Р В Р’ВµР В РЎВ Р В РўвЂР В Р’В°Р РЋРІР‚ВР РЋРІР‚С™ Р В Р’В±Р В РЎвЂўР В Р’В»Р РЋР Р‰Р РЋРІвЂљВ¬Р В Р’Вµ (helmet + kevlar)
    }

    /**
     * Р В РЎСџР РЋР вЂљР В РЎвЂР В РЎВР В Р’ВµР В Р вЂ¦Р РЋР РЏР В Р’ВµР РЋРІР‚С™ Р В Р’В±Р РЋР вЂљР В РЎвЂўР В Р вЂ¦Р РЋР вЂ№: Р РЋР С“Р РЋРІР‚С™Р В Р’В°Р В Р вЂ Р В РЎвЂР РЋРІР‚С™ leather_chestplate/leather_helmet Р В Р вЂ  armor-Р РЋР С“Р В Р’В»Р В РЎвЂўР РЋРІР‚С™,
     * Р В Р’В»Р В РЎвЂР В Р’В±Р В РЎвЂў Р В РўвЂР В РЎвЂўР В Р’В±Р В Р’В°Р В Р вЂ Р В Р’В»Р РЋР РЏР В Р’ВµР РЋРІР‚С™ armor attribute Р В Р’ВµР РЋР С“Р В Р’В»Р В РЎвЂ Р РЋР С“Р В Р’В»Р В РЎвЂўР РЋРІР‚С™ Р В Р’В·Р В Р’В°Р В Р вЂ¦Р РЋР РЏР РЋРІР‚С™.
     *
     * Р В Р’ВР РЋР С“Р В РЎвЂ”Р В РЎвЂўР В Р’В»Р РЋР Р‰Р В Р’В·Р РЋРЎвЂњР В Р’ВµР РЋРІР‚С™ Р РЋРЎвЂњР В Р вЂ¦Р В РЎвЂР В РЎвЂќР В Р’В°Р В Р’В»Р РЋР Р‰Р В Р вЂ¦Р РЋРІР‚в„–Р В РІвЂћвЂ“ UUID Р В РўвЂР В Р’В»Р РЋР РЏ Р В РЎВР В РЎвЂўР В РўвЂР В РЎвЂР РЋРІР‚С›Р В РЎвЂР В РЎвЂќР В Р’В°Р РЋРІР‚С™Р В РЎвЂўР РЋР вЂљР В Р’В° Р РЋРІР‚РЋР РЋРІР‚С™Р В РЎвЂўР В Р’В±Р РЋРІР‚в„– Р В Р вЂ¦Р В Р’Вµ Р В РўвЂР РЋРЎвЂњР В Р’В±Р В Р’В»Р В РЎвЂР РЋР вЂљР В РЎвЂўР В Р вЂ Р В Р’В°Р РЋРІР‚С™Р РЋР Р‰.
     */
    private static final java.util.UUID ARMOR_MODIFIER_ID =
            java.util.UUID.fromString("9c5b6f1e-3a2d-4e8b-9f1c-7a8b9c0d1e2f");

    private void applyArmor(ServerPlayer player, String gunId) {
        var armorAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.removeModifier(ARMOR_MODIFIER_ID);
            int points = armorPointsFor(gunId);
            var modifier = new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    ARMOR_MODIFIER_ID, "cs-edition armor", points,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION);
            armorAttr.addPermanentModifier(modifier);
        }
        // Give actual Netherite armor (chestplate/helmet) with Protection IV.
        // Replaces existing armor in slot (old armor is dropped at player's feet).
        // Note: armor is placed FIRST, then enchanted — so if enchanting fails
        // for any reason, the Netherite item is still equipped.
        String itemId = "tacz:helmet".equals(gunId)
                ? "minecraft:netherite_helmet"
                : "minecraft:netherite_chestplate";
        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                new net.minecraft.resources.ResourceLocation(itemId));
        if (item != null) {
            int slot = "tacz:helmet".equals(gunId) ? 3 : 2;
            var inv = player.getInventory();
            ItemStack armor = new ItemStack(item);
            // Place armor FIRST (before enchanting) so it's always equipped
            ItemStack existing = inv.armor.get(slot);
            if (!existing.isEmpty()) {
                player.drop(existing.copy(), false);
            }
            inv.armor.set(slot, armor);
            // Then try to add Protection IV via registry lookup (safer than
            // relying on Enchantments.ALL_DAMAGE_PROTECTION which may not
            // be available in all Forge versions)
            var ench = net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS
                    .getValue(new net.minecraft.resources.ResourceLocation("minecraft:protection"));
            if (ench != null) {
                armor.enchant(ench, 4);
            }
        }
    }

    /**
     * Р РЎСџР вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р’В°Р Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р’ВµР Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р’В°Р Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р’В°Р Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р’В»Р Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р’ВµР Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р’В»Р Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р’В¶Р Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р’ВµР Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р’В»Р Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р’В» Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р’ВµР Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р вЂ™Р’В°.
     */
    /**
     * Очищает инвентарь игрока, оставляя предмет в KNIFE_SLOT нетронутым.
     * Используется при старте нового матча.
     */
    public static void clearInventoryExceptKnifeSlot(ServerPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (i == KNIFE_SLOT) continue; // не трогаем нож
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                player.drop(stack.copy(), false);
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    private boolean hasInventorySpace(ServerPlayer player) {
        int max = CSConfig.getMaxInventorySlots();
        var inv = player.getInventory();
        int used = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (i == KNIFE_SLOT) continue; // нож не считаем
            if (!inv.getItem(i).isEmpty()) used++;
        }
        return used < max;
    }

    public static void handleQuickBuy(ServerPlayer player, com.csedition.network.PacketQuickBuy.Type type) {
        if (player == null) return;
        MatchManager mm = getInstance();
        GameMode mode = mm.getCurrentMode();
        if (!mode.isAllowBuy()) {
            player.sendSystemMessage(Component.literal("Buying is disabled in this mode!"));
            return;
        }
        if (mm.phase != GamePhase.BUY_TIME) {
            player.sendSystemMessage(Component.literal("Buy time is over!"));
            return;
        }
        MapData map = mm.getCurrentMap();
        PlayerData pd = mm.getOrCreate(player);
        if (map == null || !map.isInBuyZone(player.blockPosition(), pd.getTeam())) {
            player.sendSystemMessage(Component.literal("You must be in the buy zone!"));
            return;
        }

        String gunId = null;
        switch (type) {
            case LAST -> {
                gunId = pd.getLastBought();
                if (gunId == null) {
                    player.sendSystemMessage(Component.literal("No previous purchase."));
                    return;
                }
            }
            case PRIMARY -> gunId = GunPriceTable.getCheapestOfCategory("rifle");
            case SECONDARY -> gunId = GunPriceTable.getCheapestOfCategory("pistol");
            case UTILITY -> gunId = GunPriceTable.getCheapestOfCategory("utility");
        }

        if (gunId == null) {
            player.sendSystemMessage(Component.literal("No weapon available."));
            return;
        }

        mm.handleBuyRequest(player, gunId);
        pd.setLastBought(gunId);
    }

    // ====================== Р В РЎв„ўР В Р’В°Р РЋР вЂљР РЋРІР‚С™Р РЋРІР‚в„– ======================

    public void handleMapSelect(ServerPlayer player, String mapId) {
        if (phase != GamePhase.LOBBY) return;
        MapData map = MapConfig.getMap(mapId);
        if (map == null) return;
        if (!map.isForMode(currentModeId)) {
            player.sendSystemMessage(Component.literal("This map is not for the current mode!"));
            return;
        }
        this.currentMapId = mapId;
        List<UUID> ids = new ArrayList<>(playerDataMap.keySet());
        Collections.sort(ids);
        for (int i = 0; i < ids.size(); i++) {
            PlayerData pd = playerDataMap.get(ids.get(i));
            pd.setTeam(i < ids.size() / 2 ? Team.T : Team.CT);
        }
        broadcastPhase();
        startNewRound();
    }

    // ====================== Р В Р в‚¬Р РЋРІР‚С™Р В РЎвЂР В Р’В»Р В РЎвЂР РЋРІР‚С™Р РЋРІР‚в„– ======================

    public void teleportToLobby(ServerPlayer player) {
        BlockPos lobby = MapConfig.getLobbySpawn();
        player.teleportTo(lobby.getX() + 0.5, lobby.getY(), lobby.getZ() + 0.5);
    }

    private void giveBaseLoadout(ServerPlayer player, PlayerData pd, GameMode mode) {
        // Р В РЎвЂєР РЋРІР‚РЋР В РЎвЂР РЋРІР‚В°Р В Р’В°Р В Р’ВµР В РЎВ Р В РЎвЂР В Р вЂ¦Р В Р вЂ Р В Р’ВµР В Р вЂ¦Р РЋРІР‚С™Р В Р’В°Р РЋР вЂљР РЋР Р‰ Р РЋРІР‚С™Р В РЎвЂўР В Р’В»Р РЋР Р‰Р В РЎвЂќР В РЎвЂў Р В Р вЂ  Р В РЎвЂ”Р В Р’ВµР РЋР вЂљР В Р вЂ Р В РЎвЂўР В РЎВ Р РЋР вЂљР В Р’В°Р РЋРЎвЂњР В Р вЂ¦Р В РўвЂР В Р’Вµ.
        // Р В РЎС™Р В Р’ВµР В Р’В¶Р В РўвЂР РЋРЎвЂњ Р РЋР вЂљР В Р’В°Р РЋРЎвЂњР В Р вЂ¦Р В РўвЂР В Р’В°Р В РЎВР В РЎвЂ Р В РЎвЂР В Р вЂ¦Р В Р вЂ Р В Р’ВµР В Р вЂ¦Р РЋРІР‚С™Р В Р’В°Р РЋР вЂљР РЋР Р‰ Р РЋР С“Р В РЎвЂўР РЋРІР‚В¦Р РЋР вЂљР В Р’В°Р В Р вЂ¦Р РЋР РЏР В Р’ВµР РЋРІР‚С™Р РЋР С“Р РЋР РЏ (Р В РЎвЂ”Р В РЎвЂўР В РЎвЂќР РЋРЎвЂњР В РЎвЂ”Р В РЎвЂќР В РЎвЂ Р В РЎвЂўР РЋР С“Р РЋРІР‚С™Р В Р’В°Р РЋР вЂ№Р РЋРІР‚С™Р РЋР С“Р РЋР РЏ).
        if (roundNumber == 1) {
            clearInventoryKeeping(player);
        }
        List<String> weapons = mode.getStartWeapons(pd.getTeam());
        if (weapons == null || weapons.isEmpty()) {
            // Р В РІР‚СњР В Р’ВµР РЋРІР‚С›Р В РЎвЂўР В Р’В»Р РЋРІР‚С™: Р В РЎвЂ”Р В РЎвЂР РЋР С“Р РЋРІР‚С™Р В РЎвЂўР В Р’В»Р В Р’ВµР РЋРІР‚С™ + Р В Р вЂ¦Р В РЎвЂўР В Р’В¶ Р В Р вЂ  Р РЋРІР‚В¦Р В РЎвЂўР РЋРІР‚С™Р В Р’В±Р В Р’В°Р РЋР вЂљ
            TaczHelper.giveGunToSlot(player, pd.getTeam() == Team.T ? "tacz:glock_17" : "tacz:usp_45", 0);
            TaczHelper.giveGunToSlot(player, "tacz:combat_knife", 1);
            return;
        }
        // Р В РЎв„ўР В Р’В»Р В Р’В°Р В РўвЂР РЋРІР‚ВР В РЎВ Р РЋР С“Р РЋРІР‚С™Р В Р’В°Р РЋР вЂљР РЋРІР‚С™Р В РЎвЂўР В Р вЂ Р В РЎвЂўР В Р’Вµ Р В РЎвЂўР РЋР вЂљР РЋРЎвЂњР В Р’В¶Р В РЎвЂР В Р’Вµ Р В Р вЂ  Р РЋР С“Р В Р’В»Р В РЎвЂўР РЋРІР‚С™Р РЋРІР‚в„– Р РЋРІР‚В¦Р В РЎвЂўР РЋРІР‚С™Р В Р’В±Р В Р’В°Р РЋР вЂљР В Р’В° Р В РЎвЂ”Р В РЎвЂў Р В РЎвЂ”Р В РЎвЂўР РЋР вЂљР РЋР РЏР В РўвЂР В РЎвЂќР РЋРЎвЂњ: 0, 1, 2...
        // Р В РІР‚СћР РЋР С“Р В Р’В»Р В РЎвЂ Р В РЎвЂўР РЋР вЂљР РЋРЎвЂњР В Р’В¶Р В РЎвЂР В РІвЂћвЂ“ Р В Р’В±Р В РЎвЂўР В Р’В»Р РЋР Р‰Р РЋРІвЂљВ¬Р В Р’Вµ Р РЋРІР‚РЋР В Р’ВµР В РЎВ 3 Р Р†Р вЂљРІР‚Сњ Р В Р’В»Р В РЎвЂР РЋРІвЂљВ¬Р В Р вЂ¦Р В Р’ВµР В Р’Вµ Р РЋРЎвЂњР РЋРІР‚В¦Р В РЎвЂўР В РўвЂР В РЎвЂР РЋРІР‚С™ Р В Р вЂ  Р В РЎвЂўР РЋР С“Р В Р вЂ¦Р В РЎвЂўР В Р вЂ Р В Р вЂ¦Р В РЎвЂўР В РІвЂћвЂ“ Р В РЎвЂР В Р вЂ¦Р В Р вЂ Р В Р’ВµР В Р вЂ¦Р РЋРІР‚С™Р В Р’В°Р РЋР вЂљР РЋР Р‰.
        int hotbarSlot = 0;
        for (String gunId : weapons) {
            if (hotbarSlot <= 8) {
                if (TaczHelper.giveGunToSlot(player, gunId, hotbarSlot)) {
                    hotbarSlot++;
                } else {
                    TaczHelper.giveGun(player, gunId);
                }
            } else {
                TaczHelper.giveGun(player, gunId);
            }
        }
    }

    public void sendMoneyUpdate(ServerPlayer player, PlayerData pd) {
        CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new PacketMoneyUpdate(pd.getMoney(), pd.getKills(), pd.getDeaths()));
    }

    public void onPlayerKill(ServerPlayer victim, ServerPlayer killer) {
        if (phase != GamePhase.FIGHTING) return;
        GameMode mode = getCurrentMode();
        PlayerData vd = get(victim.getUUID());
        PlayerData kd = get(killer.getUUID());
        if (vd != null) vd.onDeath();
        if (kd != null) kd.onKill(mode.getKillReward());
        if (killer != null && kd != null) sendMoneyUpdate(killer, kd);
        checkRoundEnd();
    }

    private void checkRoundEnd() {
        int tAlive = 0, ctAlive = 0;
        for (PlayerData pd : playerDataMap.values()) {
            if (!pd.isAlive()) continue;
            if (pd.getTeam() == Team.T) tAlive++;
            else if (pd.getTeam() == Team.CT) ctAlive++;
        }
        if (tAlive == 0 && ctAlive > 0) endRound(Team.CT, "ELIMINATION");
        else if (ctAlive == 0 && tAlive > 0) endRound(Team.T, "ELIMINATION");
    }

    private ServerPlayer getServerPlayer(UUID uuid) {
        return ServerPlayerLookup.get(uuid);
    }

    /**
     * Публичный static lookup игрока по UUID.
     * Используется в обработчиках событий (MatchEvents) где нет инстанса MatchManager.
     */
    public static ServerPlayer getServerPlayerStatic(UUID uuid) {
        return ServerPlayerLookup.get(uuid);
    }
}