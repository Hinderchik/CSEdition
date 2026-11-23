package com.csedition.event;

import com.csedition.CSEditionMod;
import com.csedition.config.CSConfig;
import com.csedition.config.MapConfig;
import com.csedition.config.ModeConfig;
import com.csedition.config.WeaponConfig;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Серверные события:
 * - регистрация команд /cs
 * - загрузка maps.json, modes.json, csconfig.json, weapons.json
 */
public class ServerEvents {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // CSCommand сам регистрируется через @SubscribeEvent
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        CSEditionMod.LOGGER.info("[CS-Edition] Server started, loading configs...");
        WeaponConfig.load();
        MapConfig.load();
        ModeConfig.load();
        CSConfig.load();
    }
}
