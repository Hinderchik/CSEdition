package com.csedition.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: клиент выбрал карту в лобби.
 * Сервер устанавливает её как текущую и при готовности запускает матч.
 */
public class PacketSelectMap {
    private final String mapId;

    public PacketSelectMap(String mapId) {
        this.mapId = mapId;
    }

    public static void encode(PacketSelectMap msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.mapId);
    }

    public static PacketSelectMap decode(FriendlyByteBuf buf) {
        return new PacketSelectMap(buf.readUtf());
    }

    public static void handle(PacketSelectMap msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.csedition.match.MatchManager.getInstance()
                    .handleMapSelect(ctx.get().getSender(), msg.mapId);
        });
        ctx.get().setPacketHandled(true);
    }
}
