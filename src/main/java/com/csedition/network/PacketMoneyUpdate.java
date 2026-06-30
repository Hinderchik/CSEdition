package com.csedition.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: сервер отправляет клиенту обновление денег и статистики игрока.
 * Клиент сохраняет в ClientState и HUD рисует по этим данным.
 */
public class PacketMoneyUpdate {
    private final int money;
    private final int kills;
    private final int deaths;

    public PacketMoneyUpdate(int money, int kills, int deaths) {
        this.money = money;
        this.kills = kills;
        this.deaths = deaths;
    }

    public static void encode(PacketMoneyUpdate msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.money);
        buf.writeInt(msg.kills);
        buf.writeInt(msg.deaths);
    }

    public static PacketMoneyUpdate decode(FriendlyByteBuf buf) {
        return new PacketMoneyUpdate(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(PacketMoneyUpdate msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Клиентская сторона
            com.csedition.client.ClientState.update(msg.money, msg.kills, msg.deaths);
        });
        ctx.get().setPacketHandled(true);
    }
}
