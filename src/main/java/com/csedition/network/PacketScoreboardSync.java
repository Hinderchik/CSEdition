package com.csedition.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * S2C: сервер синхронизирует полное табло (scoreboard) с клиентом.
 */
public class PacketScoreboardSync {

    public static class Entry {
        public UUID uuid;
        public String name;
        public String team;
        public int money;
        public boolean alive;
        public int kills;
        public int deaths;

        public Entry(UUID uuid, String name, String team, int money, boolean alive, int kills, int deaths) {
            this.uuid = uuid;
            this.name = name;
            this.team = team;
            this.money = money;
            this.alive = alive;
            this.kills = kills;
            this.deaths = deaths;
        }

        public static void encode(Entry e, FriendlyByteBuf buf) {
            buf.writeLong(e.uuid.getMostSignificantBits());
            buf.writeLong(e.uuid.getLeastSignificantBits());
            buf.writeUtf(e.name != null ? e.name : "", 64);
            buf.writeUtf(e.team != null ? e.team : "", 4);
            buf.writeInt(e.money);
            buf.writeBoolean(e.alive);
            buf.writeInt(e.kills);
            buf.writeInt(e.deaths);
        }

        public static Entry decode(FriendlyByteBuf buf) {
            long msb = buf.readLong();
            long lsb = buf.readLong();
            UUID uuid = new UUID(msb, lsb);
            return new Entry(
                    uuid,
                    buf.readUtf(64),
                    buf.readUtf(4),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readInt()
            );
        }
    }

    public String mapId;
    public String modeId;
    public int phaseTicks;
    public String phaseStr;
    public int roundNumber;
    public int tScore;
    public int ctScore;
    public Entry[] entries;

    public PacketScoreboardSync(String mapId, String modeId, int phaseTicks,
                                 String phaseStr, int roundNumber,
                                 int tScore, int ctScore, Entry[] entries) {
        this.mapId = mapId;
        this.modeId = modeId;
        this.phaseTicks = phaseTicks;
        this.phaseStr = phaseStr;
        this.roundNumber = roundNumber;
        this.tScore = tScore;
        this.ctScore = ctScore;
        this.entries = entries;
    }

    public static void encode(PacketScoreboardSync msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.mapId != null ? msg.mapId : "", 64);
        buf.writeUtf(msg.modeId != null ? msg.modeId : "", 64);
        buf.writeInt(msg.phaseTicks);
        buf.writeUtf(msg.phaseStr != null ? msg.phaseStr : "", 32);
        buf.writeInt(msg.roundNumber);
        buf.writeInt(msg.tScore);
        buf.writeInt(msg.ctScore);
        buf.writeInt(msg.entries.length);
        for (Entry e : msg.entries) {
            Entry.encode(e, buf);
        }
    }

    public static PacketScoreboardSync decode(FriendlyByteBuf buf) {
        String mapId = buf.readUtf(64);
        String modeId = buf.readUtf(64);
        int phaseTicks = buf.readInt();
        String phaseStr = buf.readUtf(32);
        int roundNumber = buf.readInt();
        int tScore = buf.readInt();
        int ctScore = buf.readInt();
        int n = buf.readInt();
        Entry[] entries = new Entry[n];
        for (int i = 0; i < n; i++) {
            entries[i] = Entry.decode(buf);
        }
        return new PacketScoreboardSync(mapId, modeId, phaseTicks, phaseStr, roundNumber, tScore, ctScore, entries);
    }

    public static void handle(PacketScoreboardSync msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.csedition.client.ClientScoreboardCache.apply(msg);
        });
        ctx.get().setPacketHandled(true);
    }
}