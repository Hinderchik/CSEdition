package com.csedition.event;

import com.csedition.data.GamePhase;
import com.csedition.match.MatchManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Серверные события игроков.
 * Подписан на шину Forge (зарегистрирован в CSEditionMod).
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
     * В лобби отключаем любой урон.
     */
    @SubscribeEvent
    public void onLivingHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (MatchManager.getInstance().getPhase() == GamePhase.LOBBY) {
            if (event.getEntity() instanceof Player) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * Обработка убийств — начисление денег, проверка конца раунда.
     */
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            MatchManager.getInstance().onPlayerKill(victim, killer);
        } else {
            // Умер не от игрока — просто отметим как мёртвого
            MatchManager.getInstance().onPlayerKill(victim, null);
        }
    }
}
