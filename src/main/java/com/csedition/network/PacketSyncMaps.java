package com.csedition.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Сервер -> Клиент: полная синхронизация maps.json.
 * Хост (dedicated или LAN) отправляет свой конфиг карт каждому подключающемуся клиенту.
 * Клиенту не нужен свой maps.json — всё приходит по сети.
 *
 * Формат: JSON-строка, идентичная содержимому maps.json на хосте.
 */
public class PacketSyncMaps {
    private final String mapsJson;

    public PacketSyncMaps(String mapsJson) {
        this.mapsJson = mapsJson;
    }

    public String getMapsJson() { return mapsJson; }

    public static void encode(PacketSyncMaps msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.mapsJson);
    }

    public static PacketSyncMaps decode(FriendlyByteBuf buf) {
        return new PacketSyncMaps(buf.readUtf());
    }

    public static void handle(PacketSyncMaps msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Сохраняем на клиенте
            com.csedition.client.ClientState.setMapsJson(msg.mapsJson);
        });
        ctx.get().setPacketHandled(true);
    }
}
