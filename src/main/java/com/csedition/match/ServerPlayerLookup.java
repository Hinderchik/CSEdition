package com.csedition.match;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;

/**
 * Утилита для получения ServerPlayer по UUID.
 * Используется MatchManager для рассылки пакетов.
 */
public final class ServerPlayerLookup {
    private ServerPlayerLookup() {}

    public static ServerPlayer get(UUID uuid) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        return server.getPlayerList().getPlayer(uuid);
    }
}
