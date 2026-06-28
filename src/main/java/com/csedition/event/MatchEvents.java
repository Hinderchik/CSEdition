package com.csedition.event;

import com.csedition.data.GamePhase;
import com.csedition.data.MapData;
import com.csedition.data.PlayerData;
import net.minecraftforge.event.TickEvent;
import net.minecraft.world.phys.Vec3;
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
 * РЎРµСЂРІРµСЂРЅС‹Рµ СЃРѕР±С‹С‚РёСЏ РІРѕ РІСЂРµРјСЏ РјР°С‚С‡Р°:
 * - Р’РёР·СѓР°Р»РёР·Р°С†РёСЏ Р·РѕРЅС‹ РїРѕРєСѓРїРєРё: Р·РѕР»РѕС‚С‹Рµ С‡Р°СЃС‚РёС†С‹ РІ Р·РѕРЅРµ, СЃРµСЂС‹Р№ РґС‹Рј Р·Р° РїСЂРµРґРµР»Р°РјРё
 * - Kill feed: broadcast РІ С‡Р°С‚ "Killer в–ё Victim [weapon]"
 *
 * РћРїС‚РёРјРёР·Р°С†РёСЏ:
 * - Р§Р°СЃС‚РёС†С‹ С‚РёРєР°СЋС‚ СЂР°Р· РІ 10 С‚РёРєРѕРІ (2 СЂР°Р·Р°/СЃРµРє) вЂ” РїР»Р°РІРЅРѕ Рё РЅРµРґРѕСЂРѕРіРѕ
 * - Kill feed С€Р»С‘С‚СЃСЏ per-player С‡РµСЂРµР· РїСЂСЏРјРѕРµ СЃРѕРѕР±С‰РµРЅРёРµ (РЅРµ broadcast-РїР°РєРµС‚)
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

    /**
     * Во время BUY_TIME игроки не могут двигаться.
     * Обнуляем X/Z компоненты движения каждый тик (сервер-авторитативно).
     * Клиент тоже получит это через server-authoritative движение.
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof net.minecraft.server.level.ServerPlayer player)) return;
        MatchManager mm = MatchManager.getInstance();
        if (mm.getPhase() != GamePhase.BUY_TIME) return;

        Vec3 motion = player.getDeltaMovement();
        if (motion.x != 0 || motion.z != 0) {
            // Оставляем Y (gravity/falling), обнуляем горизонталь
            player.setDeltaMovement(0.0, motion.y, 0.0);
        }
    }
}