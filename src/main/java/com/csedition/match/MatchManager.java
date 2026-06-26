package com.csedition.match;

import com.csedition.CSEditionMod;
import com.csedition.config.MapConfig;
import com.csedition.data.GamePhase;
import com.csedition.data.MapData;
import com.csedition.data.PlayerData;
import com.csedition.data.Team;
import com.csedition.network.CSPackets;
import com.csedition.network.PacketMapList;
import com.csedition.network.PacketMoneyUpdate;
import com.csedition.network.PacketPhaseUpdate;
import com.csedition.tacz.TaczHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
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
 *   - phaseTicks: оставшееся время фазы в тиках
 *   - playerDataMap: данные игроков (UUID -> PlayerData)
 *   - roundNumber: номер текущего раунда
 *
 * Синхронизация с клиентом:
 *   - При смене фазы рассылает PacketPhaseUpdate всем игрокам.
 *   - При изменении денег/убийств отправляет PacketMoneyUpdate конкретному игроку.
 *   - При входе игрока отправляет PacketMapList со списком карт.
 */
public class MatchManager {
    private static final MatchManager INSTANCE = new MatchManager();

    public static final int BUY_TIME_TICKS = 15 * 20;   // 15 секунд
    public static final int FIGHTING_TICKS = 120 * 20;  // 2 минуты
    public static final int ROUND_END_TICKS = 5 * 20;   // 5 секунд

    private GamePhase phase = GamePhase.LOBBY;
    private String currentMapId = null;
    private int phaseTicks = 0;
    private int roundNumber = 0;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private final Random random = new Random();

    private MatchManager() {}

    public static MatchManager getInstance() { return INSTANCE; }

    // ====================== Игроки ======================

    public PlayerData getOrCreate(ServerPlayer player) {
        return playerDataMap.computeIfAbsent(player.getUUID(), PlayerData::new);
    }

    public PlayerData get(UUID uuid) { return playerDataMap.get(uuid); }

    public void onPlayerJoin(ServerPlayer player) {
        getOrCreate(player);
        // Отправим список карт
        List<PacketMapList.MapEntry> entries = new ArrayList<>();
        for (MapData m : MapConfig.getMaps().values()) {
            entries.add(new PacketMapList.MapEntry(m.getId(), m.getDisplayName()));
        }
        CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PacketMapList(entries));
        // Отправим текущую фазу
        broadcastPhase();
        // Телепортируем в лобби
        teleportToLobby(player);
    }

    public void onPlayerLeave(ServerPlayer player) {
        // Данные сохраняем, чтобы при возврате не терять статистику
        // playerDataMap.remove(player.getUUID()); // раскомментировать если нужно
    }

    // ====================== Фазы ======================

    public GamePhase getPhase() { return phase; }
    public String getCurrentMapId() { return currentMapId; }
    public MapData getCurrentMap() {
        return currentMapId == null ? null : MapConfig.getMap(currentMapId);
    }
    public int getPhaseTicks() { return phaseTicks; }

    public void setCurrentMap(String mapId) {
        if (MapConfig.getMap(mapId) != null) {
            this.currentMapId = mapId;
            broadcastPhase();
        }
    }

    public void setPhase(GamePhase newPhase) {
        this.phase = newPhase;
        switch (newPhase) {
            case BUY_TIME -> this.phaseTicks = BUY_TIME_TICKS;
            case FIGHTING -> this.phaseTicks = FIGHTING_TICKS;
            case ROUND_END -> this.phaseTicks = ROUND_END_TICKS;
            default -> this.phaseTicks = 0;
        }
        broadcastPhase();
        CSEditionMod.LOGGER.info("[CS-Edition] Phase -> {} ({} ticks)", newPhase, phaseTicks);
    }

    public void broadcastPhase() {
        PacketPhaseUpdate pkt = new PacketPhaseUpdate(phase, phaseTicks, currentMapId);
        for (UUID uuid : playerDataMap.keySet()) {
            ServerPlayer sp = getServerPlayer(uuid);
            if (sp != null) {
                CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), pkt);
            }
        }
    }

    // ====================== Тик ======================

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (phase == GamePhase.LOBBY) return;

        if (phaseTicks > 0) {
            phaseTicks--;
            // Каждые 20 тиков (1 сек) обновляем HUD с таймером
            if (phaseTicks % 20 == 0) broadcastPhase();
        }

        if (phaseTicks <= 0) {
            advancePhase();
        }
    }

    private void advancePhase() {
        switch (phase) {
            case BUY_TIME -> {
                setPhase(GamePhase.FIGHTING);
            }
            case FIGHTING -> {
                // Таймаут — победа CT по умолчанию
                endRound(Team.CT);
            }
            case ROUND_END -> {
                startNewRound();
            }
            default -> {}
        }
    }

    // ====================== Раунды ======================

    public void startNewRound() {
        roundNumber++;
        MapData map = getCurrentMap();
        if (map == null) {
            setPhase(GamePhase.LOBBY);
            return;
        }

        // Сброс денег и состояния
        for (PlayerData pd : playerDataMap.values()) {
            pd.resetForRound();
        }

        // Телепорт и выдача оружия
        for (UUID uuid : playerDataMap.keySet()) {
            ServerPlayer sp = getServerPlayer(uuid);
            if (sp == null) continue;
            PlayerData pd = playerDataMap.get(uuid);
            BlockPos spawn = map.getRandomSpawn(pd.getTeam(), random);
            sp.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
            giveBaseLoadout(sp, pd);
            sendMoneyUpdate(sp, pd);
        }

        setPhase(GamePhase.BUY_TIME);
    }

    public void endRound(Team winner) {
        for (PlayerData pd : playerDataMap.values()) {
            if (pd.getTeam() == winner) {
                pd.onRoundWin();
            }
        }
        // Сообщение
        Component msg = Component.literal("Round " + roundNumber + " won by " + winner.name());
        for (UUID uuid : playerDataMap.keySet()) {
            ServerPlayer sp = getServerPlayer(uuid);
            if (sp != null) {
                sp.sendSystemMessage(msg);
                sendMoneyUpdate(sp, playerDataMap.get(uuid));
            }
        }
        setPhase(GamePhase.ROUND_END);
    }

    private void startNewRoundAfterEnd() {
        // После ROUND_END телепортируем всех в лобби, потом новый раунд
        for (UUID uuid : playerDataMap.keySet()) {
            ServerPlayer sp = getServerPlayer(uuid);
            if (sp != null) teleportToLobby(sp);
        }
        startNewRound();
    }

    // ====================== Покупка ======================

    public void handleBuyRequest(ServerPlayer player, String gunId) {
        if (player == null) return;
        if (phase != GamePhase.BUY_TIME) {
            player.sendSystemMessage(Component.literal("Buy time is over!"));
            return;
        }
        MapData map = getCurrentMap();
        if (map == null || !map.isInBuyZone(player.blockPosition())) {
            player.sendSystemMessage(Component.literal("You must be in the buy zone!"));
            return;
        }
        PlayerData pd = getOrCreate(player);
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
            // Возвращаем деньги, если предмет не найден
            pd.addMoney(price);
            player.sendSystemMessage(Component.literal("Weapon not available."));
            return;
        }
        // Заменяем текущий основной слот (или даём в инвентарь)
        player.getInventory().add(gun);
        sendMoneyUpdate(player, pd);
    }

    // ====================== Карты ======================

    public void handleMapSelect(ServerPlayer player, String mapId) {
        if (phase != GamePhase.LOBBY) return;
        if (MapConfig.getMap(mapId) == null) return;
        this.currentMapId = mapId;
        // Распределяем команды: первая половина — T, вторая — CT
        List<UUID> ids = new ArrayList<>(playerDataMap.keySet());
        Collections.sort(ids);
        for (int i = 0; i < ids.size(); i++) {
            PlayerData pd = playerDataMap.get(ids.get(i));
            pd.setTeam(i < ids.size() / 2 ? Team.T : Team.CT);
        }
        broadcastPhase();
        // Сразу запускаем матч
        startNewRound();
    }

    // ====================== Утилиты ======================

    public void teleportToLobby(ServerPlayer player) {
        BlockPos lobby = MapConfig.getLobbySpawn();
        player.teleportTo(lobby.getX() + 0.5, lobby.getY(), lobby.getZ() + 0.5);
    }

    private void giveBaseLoadout(ServerPlayer player, PlayerData pd) {
        player.getInventory().clearContent();
        player.getInventory().add(TaczHelper.createPistol(pd.getTeam() == Team.T));
        player.getInventory().add(TaczHelper.createKnife());
    }

    public void sendMoneyUpdate(ServerPlayer player, PlayerData pd) {
        CSPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new PacketMoneyUpdate(pd.getMoney(), pd.getKills(), pd.getDeaths()));
    }

    public void onPlayerKill(ServerPlayer victim, ServerPlayer killer) {
        if (phase != GamePhase.FIGHTING) return;
        PlayerData vd = get(victim.getUUID());
        PlayerData kd = get(killer.getUUID());
        if (vd != null) vd.onDeath();
        if (kd != null) kd.onKill();
        if (killer != null && kd != null) sendMoneyUpdate(killer, kd);
        // Проверка окончания раунда
        checkRoundEnd();
    }

    private void checkRoundEnd() {
        int tAlive = 0, ctAlive = 0;
        for (PlayerData pd : playerDataMap.values()) {
            if (!pd.isAlive()) continue;
            if (pd.getTeam() == Team.T) tAlive++;
            else if (pd.getTeam() == Team.CT) ctAlive++;
        }
        if (tAlive == 0 && ctAlive > 0) endRound(Team.CT);
        else if (ctAlive == 0 && tAlive > 0) endRound(Team.T);
    }

    private ServerPlayer getServerPlayer(UUID uuid) {
        // Получаем ServerPlayer по UUID через общий список игроков сервера
        // (вызывающий код должен передать сервер; здесь используем утилиту)
        return ServerPlayerLookup.get(uuid);
    }
}
