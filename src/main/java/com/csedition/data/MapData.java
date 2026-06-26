package com.csedition.data;

import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Данные одной карты. Загружаются из maps.json.
 * Содержит спавны для T и CT, а также зону закупа (прямоугольник).
 */
public class MapData {
    private final String id;
    private final String displayName;
    private final List<BlockPos> tSpawns;
    private final List<BlockPos> ctSpawns;
    private final BlockPos buyZoneMin;
    private final BlockPos buyZoneMax;
    private final BlockPos lobbySpawn;

    public MapData(String id, String displayName,
                   List<BlockPos> tSpawns, List<BlockPos> ctSpawns,
                   BlockPos buyZoneMin, BlockPos buyZoneMax,
                   BlockPos lobbySpawn) {
        this.id = id;
        this.displayName = displayName;
        this.tSpawns = tSpawns;
        this.ctSpawns = ctSpawns;
        this.buyZoneMin = buyZoneMin;
        this.buyZoneMax = buyZoneMax;
        this.lobbySpawn = lobbySpawn;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public List<BlockPos> getTSpawns() { return tSpawns; }
    public List<BlockPos> getCtSpawns() { return ctSpawns; }
    public BlockPos getLobbySpawn() { return lobbySpawn; }

    /**
     * Проверяет, находится ли позиция внутри зоны закупа (AABB).
     */
    public boolean isInBuyZone(BlockPos pos) {
        return pos.getX() >= Math.min(buyZoneMin.getX(), buyZoneMax.getX())
            && pos.getX() <= Math.max(buyZoneMin.getX(), buyZoneMax.getX())
            && pos.getY() >= Math.min(buyZoneMin.getY(), buyZoneMax.getY())
            && pos.getY() <= Math.max(buyZoneMin.getY(), buyZoneMax.getY())
            && pos.getZ() >= Math.min(buyZoneMin.getZ(), buyZoneMax.getZ())
            && pos.getZ() <= Math.max(buyZoneMin.getZ(), buyZoneMax.getZ());
    }

    public BlockPos getRandomSpawn(Team team, java.util.Random random) {
        List<BlockPos> list = (team == Team.T) ? tSpawns : ctSpawns;
        if (list.isEmpty()) return lobbySpawn;
        return list.get(random.nextInt(list.size()));
    }
}
