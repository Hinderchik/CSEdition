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
 * Р“Р»Р°РІРЅС‹Р№ СЃРµСЂРІРµСЂРЅС‹Р№ РјРµРЅРµРґР¶РµСЂ РјР°С‚С‡Р°.
 * Singleton вЂ” РѕРґРёРЅ РјР°С‚С‡ РЅР° СЃРµСЂРІРµСЂ.
 *
 * РЎРѕСЃС‚РѕСЏРЅРёРµ:
 *   - phase: С‚РµРєСѓС‰Р°СЏ С„Р°Р·Р° (LOBBY / BUY_TIME / FIGHTING / ROUND_END)
 *   - currentMapId: id РІС‹Р±СЂР°РЅРЅРѕР№ РєР°СЂС‚С‹
 *   - currentModeId: id РІС‹Р±СЂР°РЅРЅРѕРіРѕ СЂРµР¶РёРјР°
 *   - phaseTicks: РѕСЃС‚Р°РІС€РµРµСЃСЏ РІСЂРµРјСЏ С„Р°Р·С‹ РІ С‚РёРєР°С…
 *   - playerDataMap: РґР°РЅРЅС‹Рµ РёРіСЂРѕРєРѕРІ (UUID -> PlayerData)
 *   - roundNumber: РЅРѕРјРµСЂ С‚РµРєСѓС‰РµРіРѕ СЂР°СѓРЅРґР°
 *   - matchOver: true РµСЃР»Рё РјР°С‚С‡ РѕРєРѕРЅС‡РµРЅ (РєС‚Рѕ-С‚Рѕ РЅР°Р±СЂР°Р» killsToWin)
 *
 * РЎРёРЅС…СЂРѕРЅРёР·Р°С†РёСЏ СЃ РєР»РёРµРЅС‚РѕРј:
 *   - РџСЂРё СЃРјРµРЅРµ С„Р°Р·С‹ СЂР°СЃСЃС‹Р»Р°РµС‚ PacketPhaseUpdate РІСЃРµРј РёРіСЂРѕРєР°Рј.
 *   - РџСЂРё РёР·РјРµРЅРµРЅРёРё РґРµРЅРµРі/СѓР±РёР№СЃС‚РІ РѕС‚РїСЂР°РІР»СЏРµС‚ PacketMoneyUpdate РєРѕРЅРєСЂРµС‚РЅРѕРјСѓ РёРіСЂРѕРєСѓ.
 *   - РџСЂРё РѕРєРѕРЅС‡Р°РЅРёРё СЂР°СѓРЅРґР° РѕС‚РїСЂР°РІР»СЏРµС‚ PacketRoundEnd (РїРѕР±РµРґРёС‚РµР»СЊ + РїСЂРёС‡РёРЅР°).
 *   - РџСЂРё РІС…РѕРґРµ РёРіСЂРѕРєР° РѕС‚РїСЂР°РІР»СЏРµС‚ PacketMapList СЃРѕ СЃРїРёСЃРєРѕРј РєР°СЂС‚ Рё PacketSyncModes.
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

    // ====================== РРіСЂРѕРєРё ======================

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
        // Р”Р°РЅРЅС‹Рµ СЃРѕС…СЂР°РЅСЏРµРј
    }

    // ====================== Р¤Р°Р·С‹ ======================

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

    // ====================== РўРёРє ======================

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // РћР±СЂР°Р±РѕС‚РєР° РѕС‚Р»РѕР¶РµРЅРЅРѕР№ РѕС‡РёСЃС‚РєРё РїРѕСЃР»Рµ РѕРєРѕРЅС‡Р°РЅРёСЏ РјР°С‚С‡Р°
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

    // ====================== Р Р°СѓРЅРґС‹ ======================

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

        // РЎР±СЂРѕСЃ РґРµРЅРµРі Рё СЃРѕСЃС‚РѕСЏРЅРёСЏ (РЅРѕ РќР• РёРЅРІРµРЅС‚Р°СЂСЏ вЂ” РѕРЅ РѕС‡РёС‰Р°РµС‚СЃСЏ С‚РѕР»СЊРєРѕ РїРѕСЃР»Рµ РјР°С‚С‡Р°)
        for (PlayerData pd : playerDataMap.values()) {
            pd.resetForRound(mode.getStartMoney());
        }

        // РўРµР»РµРїРѕСЂС‚ Рё РІС‹РґР°С‡Р° РѕСЂСѓР¶РёСЏ
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
     * РћРєРѕРЅС‡Р°РЅРёРµ СЂР°СѓРЅРґР°.
     * @param winner РїРѕР±РµРґРёРІС€Р°СЏ РєРѕРјР°РЅРґР° (РёР»Рё null РїСЂРё РЅРёС‡СЊРµР№)
     * @param reason РїСЂРёС‡РёРЅР°: ELIMINATION, TIME_OUT, TARGET_KILLS
     */
    public void endRound(Team winner, String reason) {
        GameMode mode = getCurrentMode();
        for (PlayerData pd : playerDataMap.values()) {
            if (winner != null && pd.getTeam() == winner) {
                pd.onRoundWin(mode.getRoundWinReward());
            }
        }

        // РџСЂРѕРІРµСЂРєР° killsToWin вЂ” РµСЃР»Рё РєС‚Рѕ-С‚Рѕ РЅР°Р±СЂР°Р» РЅСѓР¶РЅРѕРµ С‡РёСЃР»Рѕ РєРёР»Р»РѕРІ, РјР°С‚С‡ РѕРєРѕРЅС‡РµРЅ
        int killsToWin = CSConfig.getKillsToWin();
        PlayerData topKiller = null;
        for (PlayerData pd : playerDataMap.values()) {
            if (pd.getKills() >= killsToWin && (topKiller == null || pd.getKills() > topKiller.getKills())) {
                topKiller = pd;
            }
        }
        if (topKiller != null) {
            // РњР°С‚С‡ РѕРєРѕРЅС‡РµРЅ!
            matchOver = true;
            Team matchWinner = topKiller.getTeam();
            String matchReason = "TARGET_KILLS";
            broadcastRoundEnd(matchWinner, matchReason, roundNumber, topKiller.getKills());
            // РЎРѕРѕР±С‰РµРЅРёРµ
            for (UUID uuid : playerDataMap.keySet()) {
                ServerPlayer sp = getServerPlayer(uuid);
                if (sp != null) {
                    sp.sendSystemMessage(Component.literal("=== MATCH OVER === " + matchWinner.name() + " wins! (" + matchReason + ")"));
                    sendMoneyUpdate(sp, playerDataMap.get(uuid));
                }
            }
            setPhase(GamePhase.ROUND_END);
            // Р§РµСЂРµР· 5 СЃРµРє вЂ” С‚РµР»РµРїРѕСЂС‚ РІ Р»РѕР±Р±Рё Рё РѕС‡РёСЃС‚РєР° РёРЅРІРµРЅС‚Р°СЂСЏ
            scheduleMatchEndCleanup();
            return;
        }

        // РћР±С‹С‡РЅРѕРµ РѕРєРѕРЅС‡Р°РЅРёРµ СЂР°СѓРЅРґР°
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
     * РџРµСЂРµРіСЂСѓР·РєР° РґР»СЏ РѕР±СЂР°С‚РЅРѕР№ СЃРѕРІРјРµСЃС‚РёРјРѕСЃС‚Рё.
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
        cleanupTicks = 5 * 20; // 5 СЃРµРєСѓРЅРґ
    }

    private void performMatchEndCleanup() {
        // РћС‡РёСЃС‚РєР° РёРЅРІРµРЅС‚Р°СЂСЏ (СЃ СѓС‡С‘С‚РѕРј keptItems)
        for (UUID uuid : playerDataMap.keySet()) {
            ServerPlayer sp = getServerPlayer(uuid);
            if (sp == null) continue;
            if (CSConfig.isClearInventoryOnMatchEnd()) {
                clearInventoryKeeping(sp);
            }
            teleportToLobby(sp);
        }
        // РЎР±СЂРѕСЃ СЃС‚Р°С‚РёСЃС‚РёРєРё
        for (PlayerData pd : playerDataMap.values()) {
            pd.resetStats();
        }
        matchOver = false;
        roundNumber = 0;
        setPhase(GamePhase.LOBBY);
    }

    /**
     * РћС‡РёС‰Р°РµС‚ РёРЅРІРµРЅС‚Р°СЂСЊ, СЃРѕС…СЂР°РЅСЏСЏ РїСЂРµРґРјРµС‚С‹ РёР· CSConfig.keptItems.
     */
    private void clearInventoryKeeping(ServerPlayer player) {
        var inv = player.getInventory();
        // РЎРѕР±РёСЂР°РµРј С‚Рѕ, С‡С‚Рѕ РЅСѓР¶РЅРѕ СЃРѕС…СЂР°РЅРёС‚СЊ
        List<ItemStack> keep = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && CSConfig.shouldKeepItem(stack.getItem())) {
                keep.add(stack.copy());
            }
        }
        inv.clearContent();
        // Р’РѕР·РІСЂР°С‰Р°РµРј СЃРѕС…СЂР°РЅС‘РЅРЅС‹Рµ
        for (ItemStack s : keep) {
            inv.add(s);
        }
    }

    // ====================== РџРѕРєСѓРїРєР° ======================

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

        // === РЎРїРµС†СЃР»СѓС‡Р°Р№: Р±СЂРѕРЅСЏ (kevlar/helmet) ===
        // Р­С‚Рѕ РќР• TaCZ-РїСѓС€РєРё вЂ” РґРѕР±Р°РІР»СЏРµРј armor attribute РЅР°РїСЂСЏРјСѓСЋ, Р±РµР· РІС‹РґР°С‡Рё РїСЂРµРґРјРµС‚Р°.
        if (isArmorId(gunId)) {
            applyArmor(player, gunId);
            sendMoneyUpdate(player, pd);
            player.sendSystemMessage(Component.literal("В§a+" + armorPointsFor(gunId) + " armor"));
            return;
        }

        ItemStack gun = TaczHelper.createGun(gunId);
        if (gun.isEmpty()) {
            pd.addMoney(price);
            player.sendSystemMessage(Component.literal("Weapon not available."));
            return;
        }
        // РџСЂРѕРІРµСЂРєР° Р»РёРјРёС‚Р° СЃР»РѕС‚РѕРІ
        if (!hasInventorySpace(player)) {
            pd.addMoney(price);
            player.sendSystemMessage(Component.literal("Inventory full! Max " + CSConfig.getMaxInventorySlots() + " slots."));
            return;
        }
        player.getInventory().add(gun);
        sendMoneyUpdate(player, pd);
    }

    /**
     * Р­С‚Рѕ Р±СЂРѕРЅСЏ (kevlar/helmet), Р° РЅРµ TaCZ-РїСѓС€РєР°.
     */
    private static boolean isArmorId(String gunId) {
        return "tacz:kevlar".equals(gunId) || "tacz:helmet".equals(gunId);
    }

    /**
     * РЎРєРѕР»СЊРєРѕ armor-РѕС‡РєРѕРІ РґР°С‘С‚ РїСЂРµРґРјРµС‚.
     */
    private static int armorPointsFor(String gunId) {
        return "tacz:helmet".equals(gunId) ? 100 : 50; // С€Р»РµРј РґР°С‘С‚ Р±РѕР»СЊС€Рµ (helmet + kevlar)
    }

    /**
     * РџСЂРёРјРµРЅСЏРµС‚ Р±СЂРѕРЅСЋ: СЃС‚Р°РІРёС‚ leather_chestplate/leather_helmet РІ armor-СЃР»РѕС‚,
     * Р»РёР±Рѕ РґРѕР±Р°РІР»СЏРµС‚ armor attribute РµСЃР»Рё СЃР»РѕС‚ Р·Р°РЅСЏС‚.
     *
     * РСЃРїРѕР»СЊР·СѓРµС‚ СѓРЅРёРєР°Р»СЊРЅС‹Р№ UUID РґР»СЏ РјРѕРґРёС„РёРєР°С‚РѕСЂР° С‡С‚РѕР±С‹ РЅРµ РґСѓР±Р»РёСЂРѕРІР°С‚СЊ.
     */
    private static final java.util.UUID ARMOR_MODIFIER_ID =
            java.util.UUID.fromString("9c5b6f1e-3a2d-4e8b-9f1c-7a8b9c0d1e2f");

    private void applyArmor(ServerPlayer player, String gunId) {
        var armorAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        if (armorAttr == null) return;
        // РЈР±РёСЂР°РµРј СЃС‚Р°СЂС‹Р№ РјРѕРґРёС„РёРєР°С‚РѕСЂ РµСЃР»Рё Р±С‹Р»
        armorAttr.removeModifier(ARMOR_MODIFIER_ID);
        // Р”РѕР±Р°РІР»СЏРµРј РЅРѕРІС‹Р№
        int points = armorPointsFor(gunId);
        var modifier = new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                ARMOR_MODIFIER_ID, "cs-edition armor", points,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION);
        armorAttr.addPermanentModifier(modifier);
        // РўР°РєР¶Рµ СЃС‚Р°РІРёРј РІРёР·СѓР°Р»СЊРЅС‹Р№ armor- РїСЂРµРґРјРµС‚ РІ СЃР»РѕС‚
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

    /**
     * РџСЂРѕРІРµСЂСЏРµС‚, РµСЃС‚СЊ Р»Рё РјРµСЃС‚Рѕ РІ РёРЅРІРµРЅС‚Р°СЂРµ СЃ СѓС‡С‘С‚РѕРј Р»РёРјРёС‚Р° СЃР»РѕС‚РѕРІ.
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

    // ====================== РљР°СЂС‚С‹ ======================

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

    // ====================== РЈС‚РёР»РёС‚С‹ ======================

    public void teleportToLobby(ServerPlayer player) {
        BlockPos lobby = MapConfig.getLobbySpawn();
        player.teleportTo(lobby.getX() + 0.5, lobby.getY(), lobby.getZ() + 0.5);
    }

    private void giveBaseLoadout(ServerPlayer player, PlayerData pd, GameMode mode) {
        // РћС‡РёС‰Р°РµРј РёРЅРІРµРЅС‚Р°СЂСЊ С‚РѕР»СЊРєРѕ РІ РїРµСЂРІРѕРј СЂР°СѓРЅРґРµ.
        // РњРµР¶РґСѓ СЂР°СѓРЅРґР°РјРё РёРЅРІРµРЅС‚Р°СЂСЊ СЃРѕС…СЂР°РЅСЏРµС‚СЃСЏ (РїРѕРєСѓРїРєРё РѕСЃС‚Р°СЋС‚СЃСЏ).
        if (roundNumber == 1) {
            clearInventoryKeeping(player);
        }
        List<String> weapons = mode.getStartWeapons(pd.getTeam());
        if (weapons == null || weapons.isEmpty()) {
            // Р”РµС„РѕР»С‚: РїРёСЃС‚РѕР»РµС‚ + РЅРѕР¶ РІ С…РѕС‚Р±Р°СЂ
            TaczHelper.giveGunToSlot(player, pd.getTeam() == Team.T ? "tacz:glock_17" : "tacz:usp_45", 0);
            TaczHelper.giveGunToSlot(player, "tacz:combat_knife", 1);
            return;
        }
        // РљР»Р°РґС‘Рј СЃС‚Р°СЂС‚РѕРІРѕРµ РѕСЂСѓР¶РёРµ РІ СЃР»РѕС‚С‹ С…РѕС‚Р±Р°СЂР° РїРѕ РїРѕСЂСЏРґРєСѓ: 0, 1, 2...
        // Р•СЃР»Рё РѕСЂСѓР¶РёР№ Р±РѕР»СЊС€Рµ С‡РµРј 3 вЂ” Р»РёС€РЅРµРµ СѓС…РѕРґРёС‚ РІ РѕСЃРЅРѕРІРЅРѕР№ РёРЅРІРµРЅС‚Р°СЂСЊ.
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
}
