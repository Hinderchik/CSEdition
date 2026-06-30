package com.csedition.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C: сервер сообщает клиенту об окончании раунда/матча.
 * Поля:
 *   - winner: имя команды-победителя (T, CT, DRAW)
 *   - reason: причина (ELIMINATION, TIME_OUT, TARGET_KILLS)
 *   - round: номер раунда
 *   - topKills: киллы лучшего игрока (-1 если не TARGET_KILLS)
 */
public class PacketRoundEnd {
    private final String winner;
    private final String reason;
    private final int round;
    private final int topKills;

    public PacketRoundEnd(String winner, String reason, int round, int topKills) {
        this.winner = winner;
        this.reason = reason;
        this.round = round;
        this.topKills = topKills;
    }

    public static void encode(PacketRoundEnd msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.winner);
        buf.writeUtf(msg.reason);
        buf.writeInt(msg.round);
        buf.writeInt(msg.topKills);
    }

    public static PacketRoundEnd decode(FriendlyByteBuf buf) {
        return new PacketRoundEnd(
                buf.readUtf(),
                buf.readUtf(),
                buf.readInt(),
                buf.readInt()
        );
    }

    public static void handle(PacketRoundEnd msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.csedition.client.ClientState.showRoundEnd(msg.winner, msg.reason, msg.round, msg.topKills);
        });
        ctx.get().setPacketHandled(true);
    }

    public String getWinner() { return winner; }
    public String getReason() { return reason; }
    public int getRound() { return round; }
    public int getTopKills() { return topKills; }
}
