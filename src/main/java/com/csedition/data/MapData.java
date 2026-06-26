package com.csedition.data;

import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Данные одной карты. Загружаются из maps.json.
 * Содержит спавны для T и CT, а также раздельные зоны закупа для каждой команды.
 */
public class MapData {
    private final String id;
    private final String displayName;
    private final List<BlockPos> tSpawns;
    private final List<BlockPos> ctSpawns;
    // Раздельные зоны закупа для каждой команды
    private final BlockPos tBuyZoneMin;
    private final BlockPos tBuyZoneMax;
    private final BlockPos ctBuyZoneMin;
    private final BlockPos ctBuyZoneMax;
    private final BlockPos lobbySpawn;

    public MapData(String id, String displayName,
                   List<BlockPos> tSpawns, List<BlockPos> ctSpawns,
                   BlockPos tBuyZoneMin, BlockPos tBuyZoneMax,
                   BlockPos ctBuyZoneMin, BlockPos ctBuyZoneMax,
                   BlockPos lobbySpawn) {
        this.id = id;
        this.displayName = displayName;
        this.tSpawns = tSpawns;
        this.ctSpawns = ctSpawns;
        this.tBuyZoneMin = tBuyZoneMin;
        this.tBuyZoneMax = tBuyZoneMax;
        this.ctBuyZoneMin = ctBuyZoneMin;
        this.ctBuyZoneMax = ctBuyZoneMax;
        this.lobbySpawn = lobbySpawn;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public List<BlockPos> getTSpawns() { return tSpawns; }
    public List<BlockPos> getCtSpawns() { return ctSpawns; }
    public BlockPos getLobbySpawn() { return lobbySpawn; }
    public BlockPos getTBuyZoneMin() { return tBuyZoneMin; }
    public BlockPos getTBuyZoneMax() { return tBuyZoneMax; }
    public BlockPos getCtBuyZoneMin() { return ctBuyZoneMin; }
    public BlockPos getCtBuyZoneMax() { return ctBuyZoneMax; }

    /**
     * Проверяет, находится ли позиция внутри зоны закупа для указанной команды.
     */
    public boolean isInBuyZone(BlockPos pos, Team team) {
        BlockPos min, max;
        if (team == Team.T) {
            min = tBuyZoneMin;
            max = tBuyZoneMax;
        } else {
            min = ctBuyZoneMin;
            max = ctBuyZoneMax;
        }
        return pos.getX() >= Math.min(min.getX(), max.getX())
            && pos.getX() <= Math.max(min.getX(), max.getX())
            && pos.getY() >= Math.min(min.getY(), max.getY())
            && pos.getY() <= Math.max(min.getY(), max.getY())
            && pos.getZ() >= Math.min(min.getZ(), max.getZ())
            && pos.getZ() <= Math.max(min.getZ(), max.getZ());
    }

    public BlockPos getRandomSpawn(Team team, java.util.Random random) {
        List<BlockPos> list = (team == Team.T) ? tSpawns : ctSpawns;
        if (list.isEmpty()) return lobbySpawn;
        return list.get(random.nextInt(list.size()));
    }
}
