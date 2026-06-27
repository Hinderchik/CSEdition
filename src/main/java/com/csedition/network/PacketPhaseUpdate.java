package com.csedition.network;

import com.csedition.data.GamePhase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: сервер уведомляет клиентов о смене фазы матча.
 * Также передаёт оставшееся время до конца фазы (в тиках),
 * id текущей карты и id текущего режима.
 */
public class PacketPhaseUpdate {
    private final GamePhase phase;
    private final int remainingTicks;
    private final String currentMapId;
    private final String currentModeId;

    public PacketPhaseUpdate(GamePhase phase, int remainingTicks, String currentMapId, String currentModeId) {
        this.phase = phase;
        this.remainingTicks = remainingTicks;
        this.currentMapId = currentMapId == null ? "" : currentMapId;
        this.currentModeId = currentModeId == null ? "" : currentModeId;
    }

    public static void encode(PacketPhaseUpdate msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.phase.ordinal());
        buf.writeInt(msg.remainingTicks);
        buf.writeUtf(msg.currentMapId);
        buf.writeUtf(msg.currentModeId);
    }

    public static PacketPhaseUpdate decode(FriendlyByteBuf buf) {
        GamePhase[] phases = GamePhase.values();
        GamePhase phase = phases[buf.readInt()];
        return new PacketPhaseUpdate(phase, buf.readInt(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(PacketPhaseUpdate msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.csedition.client.ClientState.updatePhase(msg.phase, msg.remainingTicks, msg.currentMapId, msg.currentModeId);
        });
        ctx.get().setPacketHandled(true);
    }
}
