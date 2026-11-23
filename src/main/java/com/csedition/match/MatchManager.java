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
 * Серверная логика матча: фаза, таймер, распределение по командам.
 * Singleton, делегирует пакеты клиентам через сеть.
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
    private long lastScoreboardSend = 0L;

    private MatchManager() {}

    public static MatchManager getInstance() { return INSTANCE; }

    // ====================== Управление игроками ======================

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
        sendScoreboardSync(); // синк таблица при входе
        teleportToLobby(player);
    }

    public void onPlayerLeave(ServerPlayer player) {
        playerDataMap.remove(player.getUUID());
        sendScoreboardSync(); // синк таблица при выходе
    }

    // ====================== Геттеры фазы ======================

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
    public int getRoundsWon(Team team) {
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

    /** Слать пакет только при реальной смене состояния, не каждый тик. */
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

    /**
     * Отправляет полное табло всем игрокам.
     * Вызывается по изменению состава / смерти / покупки и ресинком раз в 2с.
     */
    public void sendScoreboardSync() {
        lastScoreboardSend = System.currentTimeMillis();
        com.csedition.network.PacketScoreboardSync.Entry[] entries =
                new com.csedition.network.PacketScoreboardSync.Entry[playerDataMap.size()];
        int idx = 0;
        UUID selfUuid = null;
        try {
            selfUuid = net.minecraft.server.players.PlayerList.class
                    .getMethod("getPlayer", java.util.UUID.class)
                    .invoke(null, null) instanceof ServerPlayer sp ? sp.getUUID() : null;
        } catch (Exception ignored) {}
        // Проще: берём первого доступного
        for (java.util.Map.Entry<UUID, PlayerData> e : playerDataMap.entrySet()) {
            ServerPlayer sp = getServerPlayer(e.getKey());
            String name = sp != null ? sp.getName().getString() : "Unknown";
            PlayerData pd = e.getValue();
            entries[idx++] = new com.csedition.network.PacketScoreboardSync.Entry(
                    e.getKey(), name,
                    pd.getTeam() == Team.T ? "T" : (pd.getTeam() == Team.CT ? "CT" : "SPEC"),
                    pd.getMoney(), pd.isAlive(), pd.getKills(), pd.getDeaths()
            );
        }
        String phaseStr = phase.name();
        com.csedition.network.PacketScoreboardSync pkt =
                new com.csedition.network.PacketScoreboardSync(
                        currentMapId, currentModeId, phaseTicks, phaseStr,
                        roundNumber, roundsWon.getOrDefault(Team.T, 0),
                        roundsWon.getOrDefault(Team.CT, 0), entries);
        for (UUID uuid : playerDataMap.keySet()) {
            ServerPlayer sp = getServerPlayer(uuid);
            if (sp != null) {
                CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), pkt);
            }
        }
    }

    // ====================== Тик сервера ======================

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (cleanupTicks > 0) {
            cleanupTicks--;
            if (cleanupTicks <= 0) {
                performMatchEndCleanup();
            }
            return;
        }

        if (phase == GamePhase.LOBBY) {
            // В лобби тоже синкаем табло (команды назначены)
            if (System.currentTimeMillis() - lastScoreboardSend > 2000L) {
                sendScoreboardSync();
            }
            return;
        }

        if (phaseTicks > 0) {
            phaseTicks--;
            if (phaseTicks % 100 == 0) broadcastPhase();
        }

        if (phaseTicks <= 0) {
            advancePhase();
        }

        // Ресинк таблица раз в 2 секунды
        if (System.currentTimeMillis() - lastScoreboardSend > 2000L) {
            sendScoreboardSync();
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
        // Guard against double-call from endRound
        if (phase == GamePhase.ROUND_END) return;

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

        // Сброс денег для всех игроков (старт раунда)
        for (PlayerData pd : playerDataMap.values()) {
            pd.resetForRound(mode.getStartMoney());
        }

        // Разброс по спавнам и выдача базового лута
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
     * @param winner победившая команда (null = ничья)
     * @param reason ELIMINATION, TIME_OUT, TARGET_KILLS
     */
    public void endRound(Team winner, String reason) {
        // Guard: не вызывать дважды за один раунд
        if (phase == GamePhase.ROUND_END) return;

        GameMode mode = getCurrentMode();
        for (PlayerData pd : playerDataMap.values()) {
            if (winner != null && pd.getTeam() == winner) {
                pd.onRoundWin(mode.getRoundWinReward());
            }
        }

        // Проверяем killsToWin — если кто-то набрал, матч оконччен
        int killsToWin = CSConfig.getKillsToWin();
        PlayerData topKiller = null;
        for (PlayerData pd : playerDataMap.values()) {
            if (pd.getKills() >= killsToWin && (topKiller == null || pd.getKills() > topKiller.getKills())) {
                topKiller = pd;
            }
        }
        if (topKiller != null) {
            matchOver = true;
            Team matchWinner = topKiller.getTeam();
            String matchReason = "TARGET_KILLS";
            broadcastRoundEnd(matchWinner, matchReason, roundNumber, topKiller.getKills());
            for (UUID uuid : playerDataMap.keySet()) {
                ServerPlayer sp = getServerPlayer(uuid);
                if (sp != null) {
                    sp.sendSystemMessage(Component.literal("=== MATCH OVER === " + matchWinner.name() + " wins! (" + matchReason + ")"));
                    sendMoneyUpdate(sp, playerDataMap.get(uuid));
                }
            }
            setPhase(GamePhase.ROUND_END);
            scheduleMatchEndCleanup();
            return;
        }

        // Обычное окончание раунда по eliminations или времени
        // Проверяем roundsToWin — если кто-то набрал, матч оконччен
        int roundsToWin = getCurrentMode().getRoundsToWin();
        if (winner != null && roundsWon.getOrDefault(winner, 0) >= roundsToWin) {
            matchOver = true;
            String matchReason = "ROUNDS_TO_WIN";
            broadcastRoundEnd(winner, matchReason, roundNumber, -1);
            for (UUID uuid : playerDataMap.keySet()) {
                ServerPlayer sp = getServerPlayer(uuid);
                if (sp != null) {
                    sp.sendSystemMessage(Component.literal("=== MATCH OVER === " + winner.name() + " wins! (" + matchReason + ")"));
                    sendMoneyUpdate(sp, playerDataMap.get(uuid));
                }
            }
            setPhase(GamePhase.ROUND_END);
            scheduleMatchEndCleanup();
            return;
        }

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
        for (UUID uuid : playerDataMap.keySet()) {
            ServerPlayer sp = getServerPlayer(uuid);
            if (sp == null) continue;
            if (CSConfig.isClearInventoryOnMatchEnd()) {
                clearInventoryKeeping(sp);
            }
            teleportToLobby(sp);
        }
        // Полный сброс статистики после матча
        for (PlayerData pd : playerDataMap.values()) {
            pd.resetStats();
        }
        matchOver = false;
        roundNumber = 0;
        roundsWon.put(Team.T, 0);
        roundsWon.put(Team.CT, 0);
        setPhase(GamePhase.LOBBY);
    }

    /** Очищает инвентарь, сохраняя только specified items из CSConfig.keptItems. */
    private void clearInventoryKeeping(ServerPlayer player) {
        var inv = player.getInventory();
        List<ItemStack> keep = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && CSConfig.shouldKeepItem(stack.getItem())) {
                keep.add(stack.copy());
            }
        }
        inv.clearContent();
        for (ItemStack s : keep) {
            inv.add(s);
        }
    }

    // ====================== Закуп оружия ======================

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
        // Проверяем место И цену ДО списания денег
        if (!hasInventorySpace(player)) {
            player.sendSystemMessage(Component.literal("Inventory full! Max " + CSConfig.getMaxInventorySlots() + " slots."));
            return;
        }
        if (!pd.trySpend(price)) {
            player.sendSystemMessage(Component.literal("Not enough money!"));
            return;
        }

        // === Броня: tacz:kevlar / tacz:helmet ===
        // Применяется к armor attribute, не требует слота в инвентаре как предмета
        if (isArmorId(gunId)) {
            // Цена уже проверена выше (trySpend), применяем броню
            applyArmor(player, gunId);
            sendMoneyUpdate(player, pd);
            player.sendSystemMessage(Component.literal("\u00A7a+" + armorPointsFor(gunId) + " armor"));
            return;
        }

        // Проверка: оружие должно существовать
        ItemStack gun = TaczHelper.createGun(gunId);
        if (gun.isEmpty()) {
            pd.addMoney(price); // возвращаем деньги
            player.sendSystemMessage(Component.literal("Weapon not available."));
            return;
        }
        player.getInventory().add(gun);
        sendMoneyUpdate(player, pd);
    }

    private static boolean isArmorId(String gunId) {
        return "tacz:kevlar".equals(gunId) || "tacz:helmet".equals(gunId);
    }

    private static int armorPointsFor(String gunId) {
        return "tacz:helmet".equals(gunId) ? 100 : 50;
    }

    private static final java.util.UUID ARMOR_MODIFIER_ID =
            java.util.UUID.fromString("9c5b6f1e-3a2d-4e8b-9f1c-7a8b9c0d1e2f");

    private void applyArmor(ServerPlayer player, String gunId) {
        var armorAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        if (armorAttr == null) return;
        armorAttr.removeModifier(ARMOR_MODIFIER_ID);
        int points = armorPointsFor(gunId);
        var modifier = new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                ARMOR_MODIFIER_ID, "cs-edition armor", points,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION);
        armorAttr.addPermanentModifier(modifier);
        try {
            String itemId = "tacz:helmet".equals(gunId)
                    ? "minecraft:leather_helmet"
                    : "minecraft:leather_chestplate";
            var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                    new net.minecraft.resources.ResourceLocation(itemId));
            if (item != null) {
                int slot = "tacz:helmet".equals(gunId) ? 3 : 2; // head=3, chest=2
                var inv = player.getInventory();
                if (inv.armor.get(slot).isEmpty()) {
                    inv.armor.set(slot, new net.minecraft.world.item.ItemStack(item));
                }
            }
        } catch (Exception ignored) {}
    }

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

    // ====================== Выбор карты ======================

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
        // На первом раунде очищаем инвентарь (кроме keptItems)
        if (roundNumber == 0) {
            clearInventoryKeeping(player);
        }
        List<String> weapons = mode.getStartWeapons(pd.getTeam());
        if (weapons == null || weapons.isEmpty()) {
            // Стандартная стартовая экипировка: пистолет + нож
            TaczHelper.giveGunToSlot(player, pd.getTeam() == Team.T ? "tacz:glock_17" : "tacz:usp_45", 0);
            TaczHelper.giveGunToSlot(player, "tacz:combat_knife", 2);
            return;
        }
        // Распределяем по слотам через SlotLayout
        boolean[] occupied = new boolean[4];
        for (String gunId : weapons) {
            String category = com.csedition.config.WeaponConfig.getCategory(gunId);
            int slot = com.csedition.client.SlotLayout.slotFor(gunId, category, occupied);
            if (slot >= 0 && slot < 4) {
                if (!TaczHelper.giveGunToSlot(player, gunId, slot)) {
                    TaczHelper.giveGun(player, gunId);
                } else {
                    occupied[slot] = true;
                }
            } else {
                TaczHelper.giveGun(player, gunId);
            }
        }
    }

    public void sendMoneyUpdate(ServerPlayer player, PlayerData pd) {
        CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new PacketMoneyUpdate(pd.getMoney(), pd.getKills(), pd.getDeaths()));
        // Триггерим синк таблица — деньги изменились
        sendScoreboardSync();
    }

    public void onPlayerKill(ServerPlayer victim, ServerPlayer killer) {
        // Учитываем смерть при ЛЮБОЙ фазе (LOBBY, BUY_TIME, FIGHTING),
        // но начисляем награды и проверяем конец раунда только в FIGHTING
        PlayerData vd = get(victim.getUUID());
        if (vd != null) vd.onDeath();

        if (phase != GamePhase.FIGHTING) {
            sendScoreboardSync();
            return;
        }
        if (killer == null) return;
        GameMode mode = getCurrentMode();
        PlayerData kd = get(killer.getUUID());
        if (kd != null) kd.onKill(mode.getKillReward());
        if (kd != null) sendMoneyUpdate(killer, kd);
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

    public static ServerPlayer getServerPlayerStatic(UUID uuid) {
        return ServerPlayerLookup.get(uuid);
    }
}
