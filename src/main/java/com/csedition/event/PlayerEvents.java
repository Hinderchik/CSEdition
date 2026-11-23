package com.csedition.event;

import com.csedition.data.GamePhase;
import com.csedition.match.MatchManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Серверные события игроков.
 */
public class PlayerEvents {

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            MatchManager.getInstance().onPlayerJoin(sp);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            MatchManager.getInstance().onPlayerLeave(sp);
        }
    }

    /**
     * В лобби отключаем урон только для ServerPlayer (не для клиента).
     */
    @SubscribeEvent
    public void onLivingHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (MatchManager.getInstance().getPhase() == GamePhase.LOBBY) {
            if (event.getEntity() instanceof ServerPlayer) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * Обработка убийств — вызывает onPlayerKill в MatchManager.
     * Там обрабатываются смерти при любой фазе, но награды/конец раунда — только в FIGHTING.
     */
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        ServerPlayer killer = null;
        if (event.getSource().getEntity() instanceof ServerPlayer k) {
            killer = k;
        }
        MatchManager.getInstance().onPlayerKill(victim, killer);
    }
}
