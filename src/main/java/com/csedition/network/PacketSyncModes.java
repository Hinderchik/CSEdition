package com.csedition.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: сервер отправляет клиенту JSON со всеми режимами.
 * Клиент парсит и обновляет локальный ModeConfig.
 */
public class PacketSyncModes {
    private final String modesJson;

    public PacketSyncModes(String modesJson) {
        this.modesJson = modesJson == null ? "{}" : modesJson;
    }

    public static void encode(PacketSyncModes msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.modesJson);
    }

    public static PacketSyncModes decode(FriendlyByteBuf buf) {
        return new PacketSyncModes(buf.readUtf());
    }

    public static void handle(PacketSyncModes msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.csedition.config.ModeConfig.fromJson(msg.modesJson);
        });
        ctx.get().setPacketHandled(true);
    }
}
