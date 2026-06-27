package com.csedition.event;

import com.csedition.CSEditionMod;
import com.csedition.config.CSConfig;
import com.csedition.config.MapConfig;
import com.csedition.config.ModeConfig;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Серверные события:
 * - Регистрация команд /cs
 * - Загрузка maps.json, modes.json, csconfig.json при старте сервера и при загрузке мира
 */
public class ServerEvents {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // CSCommand сам подписан на это событие через @SubscribeEvent
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        CSEditionMod.LOGGER.info("[CS-Edition] Server started, loading configs...");
        MapConfig.load();
        ModeConfig.load();
        CSConfig.load();
    }

    /**
     * Перезагружаем конфиги при загрузке мира (на случай если ServerStartedEvent
     * сработал до полной инициализации мира).
     */
    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) return;
        CSEditionMod.LOGGER.info("[CS-Edition] Level loaded, reloading configs...");
        MapConfig.load();
        ModeConfig.load();
        CSConfig.load();
    }
}
