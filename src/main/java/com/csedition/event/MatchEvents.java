package com.csedition.event;

import com.csedition.data.GamePhase;
import com.csedition.data.MapData;
import com.csedition.data.PlayerData;
import com.csedition.match.MatchManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

/**
 * Серверные события во время матча:
 * - Визуализация зоны покупки: золотые частицы в зоне, серый дым за пределами
 * - Kill feed: broadcast в чат "Killer ▸ Victim [weapon]"
 *
 * Оптимизация:
 * - Частицы тикают раз в 10 тиков (2 раза/сек) — плавно и недорого
 * - Kill feed шлётся per-player через прямое сообщение (не broadcast-пакет)
 */
public class MatchEvents {

    private static final int PARTICLE_INTERVAL = 10;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MatchManager mm = MatchManager.getInstance();
        if (mm.getPhase() != GamePhase.BUY_TIME) return;
        MapData map = mm.getCurrentMap();
        if (map == null) return;
        long tick = mm.getPhaseTicks();
        if (tick % PARTICLE_INTERVAL != 0) return;

        for (UUID uuid : mm.getPlayerDataMap().keySet()) {
            ServerPlayer p = MatchManager.getServerPlayerStatic(uuid);
            if (p == null) continue;
            PlayerData pd = mm.getOrCreate(p);
            spawnZoneParticles(p, map, pd);
        }
    }

    private void spawnZoneParticles(ServerPlayer player, MapData map, PlayerData pd) {
        boolean inZone = map.isInBuyZone(player.blockPosition(), pd.getTeam());
        ServerLevel level = player.serverLevel();
        double x = player.getX();
        double y = player.getY() + 0.2;
        double z = player.getZ();
        if (inZone) {
            for (int i = 0; i < 3; i++) {
                double dx = (level.random.nextDouble() - 0.5) * 1.2;
                double dz = (level.random.nextDouble() - 0.5) * 1.2;
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        x + dx, y + level.random.nextDouble() * 1.5, z + dz,
                        1, 0, 0, 0, 0);
            }
        } else {
            level.sendParticles(ParticleTypes.SMOKE,
                    x, y + 1.0, z,
                    1, 0, 0, 0, 0.02);
        }
    }

    public static void broadcastKill(ServerPlayer killer, ServerPlayer victim, ItemStack weapon) {
        String weaponName = weapon.isEmpty() ? "Unknown" : weapon.getHoverName().getString();
        Component msg = Component.literal(
                "\u00A7c" + killer.getName().getString()
                + " \u00A77\u25B8 \u00A7a" + victim.getName().getString()
                + " \u00A78[\u00A7f" + weaponName + "\u00A78]");
        MatchManager mm = MatchManager.getInstance();
        for (UUID uuid : mm.getPlayerDataMap().keySet()) {
            ServerPlayer p = MatchManager.getServerPlayerStatic(uuid);
            if (p != null) p.sendSystemMessage(msg);
        }
    }
}