package com.csedition.network;

import com.csedition.data.GamePhase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: сервер уведомляет клиентов о смене фазы матча.
 * Также передаёт оставшееся время до конца фазы (в тиках).
 */
public class PacketPhaseUpdate {
    private final GamePhase phase;
    private final int remainingTicks;
    private final String currentMapId;

    public PacketPhaseUpdate(GamePhase phase, int remainingTicks, String currentMapId) {
        this.phase = phase;
        this.remainingTicks = remainingTicks;
        this.currentMapId = currentMapId == null ? "" : currentMapId;
    }

    public static void encode(PacketPhaseUpdate msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.phase.ordinal());
        buf.writeInt(msg.remainingTicks);
        buf.writeUtf(msg.currentMapId);
    }

    public static PacketPhaseUpdate decode(FriendlyByteBuf buf) {
        GamePhase[] phases = GamePhase.values();
        GamePhase phase = phases[buf.readInt()];
        return new PacketPhaseUpdate(phase, buf.readInt(), buf.readUtf());
    }

    public static void handle(PacketPhaseUpdate msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.csedition.client.ClientState.updatePhase(msg.phase, msg.remainingTicks, msg.currentMapId);
        });
        ctx.get().setPacketHandled(true);
    }
}
