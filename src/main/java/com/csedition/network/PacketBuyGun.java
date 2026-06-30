package com.csedition.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: клиент отправляет запрос на покупку пушки.
 * Сервер проверяет деньги, фазу, зону закупа и выдаёт предмет.
 */
public class PacketBuyGun {
    private final String gunId;

    public PacketBuyGun(String gunId) {
        this.gunId = gunId;
    }

    public static void encode(PacketBuyGun msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.gunId);
    }

    public static PacketBuyGun decode(FriendlyByteBuf buf) {
        return new PacketBuyGun(buf.readUtf());
    }

    public static void handle(PacketBuyGun msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Обработка на сервере — MatchManager обработает через PlayerEvents
            com.csedition.match.MatchManager.getInstance()
                    .handleBuyRequest(ctx.get().getSender(), msg.gunId);
        });
        ctx.get().setPacketHandled(true);
    }
}
