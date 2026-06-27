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
 * Главный серверный менеджер матча.
 * Singleton — один матч на сервер.
 *
 * Состояние:
 *   - phase: текущая фаза (LOBBY / BUY_TIME / FIGHTING / ROUND_END)
 *   - currentMapId: id выбранной карты
 *   - currentModeId: id выбранного режима
 *   - phaseTicks: оставшееся время фазы в тиках
 *   - playerDataMap: данные игроков (UUID -> PlayerData)
 *   - roundNumber: номер текущего раунда
 *   - matchOver: true если матч окончен (кто-то набрал killsToWin)
 *
 * Синхронизация с клиентом:
 *   - При смене фазы рассылает PacketPhaseUpdate всем игрокам.
 *   - При изменении денег/убийств отправляет PacketMoneyUpdate конкретному игроку.
 *   - При окончании раунда отправляет PacketRoundEnd (победитель + причина).
 *   - При входе игрока отправляет PacketMapList со списком карт и PacketSyncModes.
 */
public class MatchManager {
    private static final MatchManager INSTANCE = new MatchManager();

    public static final int MIN_PLAYERS = 2;

    private GamePhase phase = GamePhase.LOBBY;
    private String currentMapId = null;
    private String currentModeId = "classic";
    private int phaseTicks = 0;
    private int roundNumber = 0;
    private boolean matchOver = false;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private final Random random = new Random();
    private PacketPhaseUpdate cachedPhasePacket = null;
    private GamePhase lastBroadcastPhase = null;
    private int lastBroadcastTicks = -1;
    private String lastBroadcastMap = null;
    private String lastBroadcastMode = null;

    private MatchManager() {}

    public static MatchManager getInstance() { return INSTANCE; }

    // ====================== Игроки ======================

    public PlayerData getOrCreate(ServerPlayer player) {
        return playerDataMap.computeIfAbsent(player.getUUID(), PlayerData::new);
    }

    public PlayerData get(UUID uuid) { return playerDataMap.get(uuid); }

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
        // Данные сохраняем
    }

    // ====================== Фазы ======================

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
        for (UUID uuid : playerDataMap.keySet()) {
            ServerPlayer sp = getServerPlayer(uuid);
            if (sp != null) {
                CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), cachedPhasePacket);
            }
        }
    }

    // ====================== Тик ======================

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Обработка отложенной очистки после окончания матча
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

    // ====================== Раунды ======================

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

        // Сброс денег и состояния (но НЕ инвентаря — он очищается только после матча)
        for (PlayerData pd : playerDataMap.values()) {
            pd.resetForRound(mode.getStartMoney());
        }

        // Телепорт и выдача оружия
        for (UUID uuid : playerDataMap.keySet()) {
            ServerPlayer sp = getServerPlayer(uuid);
            if (sp == null) continue;
            PlayerData pd = playerDataMap.get(uuid);
            BlockPos spawn = map.getRandomSpawn(pd.getTeam(), random);
            sp.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
            giveBaseLoadout(sp, pd, mode);
            sendMoneyUpdate(sp, pd);
        }

        setPhase(GamePhase.BUY_TIME);
    }

    /**
     * Окончание раунда.
     * @param winner победившая команда (или null при ничьей)
     * @param reason причина: ELIMINATION, TIME_OUT, TARGET_KILLS
     */
    public void endRound(Team winner, String reason) {
        GameMode mode = getCurrentMode();
        for (PlayerData pd : playerDataMap.values()) {
            if (winner != null && pd.getTeam() == winner) {
                pd.onRoundWin(mode.getRoundWinReward());
            }
        }

        // Проверка killsToWin — если кто-то набрал нужное число киллов, матч окончен
        int killsToWin = CSConfig.getKillsToWin();
        PlayerData topKiller = null;
        for (PlayerData pd : playerDataMap.values()) {
            if (pd.getKills() >= killsToWin && (topKiller == null || pd.getKills() > topKiller.getKills())) {
                topKiller = pd;
            }
        }
        if (topKiller != null) {
            // Матч окончен!
            matchOver = true;
            Team matchWinner = topKiller.getTeam();
            String matchReason = "TARGET_KILLS";
            broadcastRoundEnd(matchWinner, matchReason, roundNumber, topKiller.getKills());
            // Сообщение
            for (UUID uuid : playerDataMap.keySet()) {
                ServerPlayer sp = getServerPlayer(uuid);
                if (sp != null) {
                    sp.sendSystemMessage(Component.literal("=== MATCH OVER === " + matchWinner.name() + " wins! (" + matchReason + ")"));
                    sendMoneyUpdate(sp, playerDataMap.get(uuid));
                }
            }
            setPhase(GamePhase.ROUND_END);
            // Через 5 сек — телепорт в лобби и очистка инвентаря
            scheduleMatchEndCleanup();
            return;
        }

        // Обычное окончание раунда
        String reasonText = switch (reason) {
            case "ELIMINATION" -> "All enemies eliminated";
            case "TIME_OUT" -> "Time ran out";
            default -> reason;
        };
        broadcastRoundEnd(winner, reason, roundNumber, -1);
        Component msg = Component.literal("Round " + roundNumber + " won by " + (winner != null ? winner.name() : "DRAW") + " (" + reasonText + ")");
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
     * Перегрузка для обратной совместимости.
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
        cleanupTicks = 5 * 20; // 5 секунд
    }

    private void performMatchEndCleanup() {
        // Очистка инвентаря (с учётом keptItems)
        for (UUID uuid : playerDataMap.keySet()) {
            ServerPlayer sp = getServerPlayer(uuid);
            if (sp == null) continue;
            if (CSConfig.isClearInventoryOnMatchEnd()) {
                clearInventoryKeeping(sp);
            }
            teleportToLobby(sp);
        }
        // Сброс статистики
        for (PlayerData pd : playerDataMap.values()) {
            pd.resetStats();
        }
        matchOver = false;
        roundNumber = 0;
        setPhase(GamePhase.LOBBY);
    }

    /**
     * Очищает инвентарь, сохраняя предметы из CSConfig.keptItems.
     */
    private void clearInventoryKeeping(ServerPlayer player) {
        var inv = player.getInventory();
        // Собираем то, что нужно сохранить
        List<ItemStack> keep = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && CSConfig.shouldKeepItem(stack.getItem())) {
                keep.add(stack.copy());
            }
        }
        inv.clearContent();
        // Возвращаем сохранённые
        for (ItemStack s : keep) {
            inv.add(s);
        }
    }

    // ====================== Покупка ======================

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
        ItemStack gun = TaczHelper.createGun(gunId);
        if (gun.isEmpty()) {
            pd.addMoney(price);
            player.sendSystemMessage(Component.literal("Weapon not available."));
            return;
        }
        // Проверка лимита слотов
        if (!hasInventorySpace(player)) {
            pd.addMoney(price);
            player.sendSystemMessage(Component.literal("Inventory full! Max " + CSConfig.getMaxInventorySlots() + " slots."));
            return;
        }
        player.getInventory().add(gun);
        sendMoneyUpdate(player, pd);
    }

    /**
     * Проверяет, есть ли место в инвентаре с учётом лимита слотов.
     */
    private boolean hasInventorySpace(ServerPlayer player) {
        int max = CSConfig.getMaxInventorySlots();
        var inv = player.getInventory();
        int used = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
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

    // ====================== Карты ======================

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

    // ====================== Утилиты ======================

    public void teleportToLobby(ServerPlayer player) {
        BlockPos lobby = MapConfig.getLobbySpawn();
        player.teleportTo(lobby.getX() + 0.5, lobby.getY(), lobby.getZ() + 0.5);
    }

    private void giveBaseLoadout(ServerPlayer player, PlayerData pd, GameMode mode) {
        // Очищаем инвентарь только если это первый раунд или если включена очистка
        // По новой логике — инвентарь НЕ очищается между раундами, только после матча
        // Но при старте нового матча — очищаем
        if (roundNumber == 1) {
            clearInventoryKeeping(player);
        }
        List<String> weapons = mode.getStartWeapons(pd.getTeam());
        if (weapons == null || weapons.isEmpty()) {
            player.getInventory().add(TaczHelper.createPistol(pd.getTeam() == Team.T));
            player.getInventory().add(TaczHelper.createKnife());
            return;
        }
        for (String gunId : weapons) {
            ItemStack gun = TaczHelper.createGun(gunId);
            if (!gun.isEmpty()) {
                player.getInventory().add(gun);
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
}
