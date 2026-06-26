package com.csedition.event;

import com.csedition.CSEditionMod;
import com.csedition.config.MapConfig;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Серверные события:
 * - Регистрация команд /cs
 * - Загрузка maps.json при старте сервера (когда мир уже доступен)
 */
public class ServerEvents {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // CSCommand сам подписан на это событие через @SubscribeEvent
        // Ничего не делаем здесь
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // Мир загружен — можно читать maps.json из папки мира
        CSEditionMod.LOGGER.info("[CS-Edition] Server started, loading maps.json...");
        MapConfig.load();
    }
}
