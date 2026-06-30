package com.csedition.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S2C: сервер отправляет клиенту список доступных карт (id + displayName + modeId).
 * Используется при входе в лобби для отрисовки GUI выбора карты.
 */
public class PacketMapList {
    private final List<MapEntry> maps;

    public PacketMapList(List<MapEntry> maps) {
        this.maps = maps;
    }

    public static void encode(PacketMapList msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.maps.size());
        for (MapEntry e : msg.maps) {
            buf.writeUtf(e.id);
            buf.writeUtf(e.displayName);
            buf.writeUtf(e.modeId == null ? "" : e.modeId);
        }
    }

    public static PacketMapList decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<MapEntry> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(new MapEntry(buf.readUtf(), buf.readUtf(), buf.readUtf()));
        }
        return new PacketMapList(list);
    }

    public static void handle(PacketMapList msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.csedition.client.ClientState.updateMapList(msg.maps);
        });
        ctx.get().setPacketHandled(true);
    }

    public static class MapEntry {
        public final String id;
        public final String displayName;
        public final String modeId;
        public MapEntry(String id, String displayName, String modeId) {
            this.id = id;
            this.displayName = displayName;
            this.modeId = modeId == null ? "" : modeId;
        }
    }
}
