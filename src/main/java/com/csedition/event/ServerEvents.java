package com.csedition.event;

import com.csedition.command.CSCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Регистрирует команду /cs при старте сервера.
 * Работает и на выделенном сервере, и в integrated server (LAN/одиночка).
 */
public class ServerEvents {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CSCommand.register(event.getDispatcher());
    }
}
