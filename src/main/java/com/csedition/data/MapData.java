package com.csedition.data;

import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Данные одной карты. Загружаются из maps.json.
 * Содержит спавны для T и CT, раздельные зоны закупа для каждой команды,
 * и привязку к режиму (modeId). Если modeId пустой — карта доступна во всех режимах.
 *
 * Зона закупа: по Y всегда от -60 до 222 (полная высота мира),
 * по XZ выбирается игроком при отметке (две точки).
 */
public class MapData {
    /** Минимальная Y-координата зоны закупа (всегда). */
    public static final int BUY_ZONE_Y_MIN = -60;
    /** Максимальная Y-координата зоны закупа (всегда). */
    public static final int BUY_ZONE_Y_MAX = 222;

    private final String id;
    private final String displayName;
    private final String modeId; // привязка к режиму (пустая строка = все режимы)
    private final List<BlockPos> tSpawns;
    private final List<BlockPos> ctSpawns;
    private final BlockPos tBuyZoneMin;
    private final BlockPos tBuyZoneMax;
    private final BlockPos ctBuyZoneMin;
    private final BlockPos ctBuyZoneMax;
    private final BlockPos lobbySpawn;

    public MapData(String id, String displayName, String modeId,
                   List<BlockPos> tSpawns, List<BlockPos> ctSpawns,
                   BlockPos tBuyZoneMin, BlockPos tBuyZoneMax,
                   BlockPos ctBuyZoneMin, BlockPos ctBuyZoneMax,
                   BlockPos lobbySpawn) {
        this.id = id;
        this.displayName = displayName;
        this.modeId = modeId == null ? "" : modeId;
        this.tSpawns = tSpawns;
        this.ctSpawns = ctSpawns;
        this.tBuyZoneMin = tBuyZoneMin;
        this.tBuyZoneMax = tBuyZoneMax;
        this.ctBuyZoneMin = ctBuyZoneMin;
        this.ctBuyZoneMax = ctBuyZoneMax;
        this.lobbySpawn = lobbySpawn;
    }

    /**
     * Конструктор без modeId (для обратной совместимости).
     */
    public MapData(String id, String displayName,
                   List<BlockPos> tSpawns, List<BlockPos> ctSpawns,
                   BlockPos tBuyZoneMin, BlockPos tBuyZoneMax,
                   BlockPos ctBuyZoneMin, BlockPos ctBuyZoneMax,
                   BlockPos lobbySpawn) {
        this(id, displayName, "", tSpawns, ctSpawns,
             tBuyZoneMin, tBuyZoneMax, ctBuyZoneMin, ctBuyZoneMax, lobbySpawn);
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getModeId() { return modeId; }
    public List<BlockPos> getTSpawns() { return tSpawns; }
    public List<BlockPos> getCtSpawns() { return ctSpawns; }
    public BlockPos getLobbySpawn() { return lobbySpawn; }
    public BlockPos getTBuyZoneMin() { return tBuyZoneMin; }
    public BlockPos getTBuyZoneMax() { return tBuyZoneMax; }
    public BlockPos getCtBuyZoneMin() { return ctBuyZoneMin; }
    public BlockPos getCtBuyZoneMax() { return ctBuyZoneMax; }

    /**
     * Специальное значение modeId — карта доступна во всех режимах.
     */
    public static final String MODE_ANY = "any";

    /**
     * Проверяет, подходит ли карта для указанного режима.
     * Пустой modeId или "any" = карта доступна во всех режимах.
     */
    public boolean isForMode(String modeId) {
        if (this.modeId == null || this.modeId.isEmpty()) return true;
        if (MODE_ANY.equalsIgnoreCase(this.modeId)) return true;
        return this.modeId.equals(modeId);
    }

    /**
     * Проверяет, находится ли позиция внутри зоны закупа для указанной команды.
     * Y всегда проверяется от -60 до 222, XZ — по выбранным точкам.
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
        if (min == null || max == null) return false;
        // Y всегда от -60 до 222
        if (pos.getY() < BUY_ZONE_Y_MIN || pos.getY() > BUY_ZONE_Y_MAX) return false;
        // XZ — по выбранным точкам
        return pos.getX() >= Math.min(min.getX(), max.getX())
            && pos.getX() <= Math.max(min.getX(), max.getX())
            && pos.getZ() >= Math.min(min.getZ(), max.getZ())
            && pos.getZ() <= Math.max(min.getZ(), max.getZ());
    }

    public BlockPos getRandomSpawn(Team team, java.util.Random random) {
        List<BlockPos> list = (team == Team.T) ? tSpawns : ctSpawns;
        if (list.isEmpty()) {
            // Нет спавнов для команды — фоллбек на лобби.
            // Если лобби тоже не задано, возвращаем null чтобы caller мог обработать.
            return lobbySpawn;
        }
        return list.get(random.nextInt(list.size()));
    }
}
