package com.csedition.event;

import com.csedition.CSEditionMod;
import com.csedition.config.CSConfig;
import com.csedition.config.MapConfig;
import com.csedition.config.ModeConfig;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Серверные события:
 * - Регистрация команд /cs
 * - Загрузка maps.json, modes.json, csconfig.json при старте сервера
 */
public class ServerEvents {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // CSCommand сам подписан на это событие через @SubscribeEvent
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Мир загружен — можно читать конфиги из папки мира
        CSEditionMod.LOGGER.info("[CS-Edition] Server started, loading configs...");
        MapConfig.load();
        ModeConfig.load();
        CSConfig.load();
    }
}
