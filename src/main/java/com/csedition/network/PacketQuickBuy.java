package com.csedition.network;

import com.csedition.CSEditionMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Пакет быстрой закупки (Z/X/C/4).
 * Клиент отправляет тип закупки, сервер выбирает оружие и выдаёт.
 */
public class PacketQuickBuy {

    public enum Type {
        LAST,       // Z — последнее купленное
        PRIMARY,    // X — основное оружие
        SECONDARY,  // C — пистолет
        UTILITY     // 4 — снаряжение
    }

    private final Type type;

    public PacketQuickBuy(Type type) {
        this.type = type;
    }

    public static void encode(PacketQuickBuy msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.type);
    }

    public static PacketQuickBuy decode(FriendlyByteBuf buf) {
        return new PacketQuickBuy(buf.readEnum(Type.class));
    }

    public static void handle(PacketQuickBuy msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // Делегируем обработку в MatchManager
            com.csedition.match.MatchManager.handleQuickBuy(player, msg.type);
        });
        ctx.get().setPacketHandled(true);
    }
}
