package com.csedition.config;

import com.csedition.CSEditionMod;
import com.csedition.data.MapData;
import com.csedition.data.Team;
import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Загружает maps.json из ПАПКИ МИРА: &lt;world&gt;/data/csedition/maps.json
 *
 * Это значит:
 *   - Каждый мир имеет свой набор карт
 *   - Файл перемещается вместе с миром
 *   - На dedicated сервере: &lt;server_dir&gt;/worlds/&lt;world&gt;/data/csedition/maps.json
 *   - В одиночке: &lt;saves&gt;/&lt;world&gt;/data/csedition/maps.json
 *
 * Устойчив к ошибкам: если одна карта битая, остальные загружаются.
 * Загружается ТОЛЬКО на сервере. Клиенты получают карты по сети через PacketSyncMaps.
 *
 * Формат maps.json:
 * {
 *   "lobbySpawn": [x, y, z],
 *   "maps": [
 *     {
 *       "id": "de_dust2",
 *       "displayName": "Dust 2",
 *       "tSpawns": [[x,y,z], ...],
 *       "ctSpawns": [[x,y,z], ...],
 *       "tBuyZoneMin": [x, y, z],
 *       "tBuyZoneMax": [x, y, z],
 *       "ctBuyZoneMin": [x, y, z],
 *       "ctBuyZoneMax": [x, y, z]
 *     }
 *   ]
 * }
 */
public class MapConfig {
    private static final Map<String, MapData> MAPS = new LinkedHashMap<>();
    private static BlockPos lobbySpawn = new BlockPos(0, 100, 0);
    private static String cachedJson = null;
    private static Path currentFile = null;

    /**
     * Возвращает путь к файлу maps.json в папке текущего мира.
     * Если мир не загружен — возвращает null.
     */
    public static Path getWorldConfigPath() {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return null;
            Path worldDataDir = server.getWorldPath(LevelResource.ROOT);
            Path modDir = worldDataDir.resolve("csedition");
            if (!Files.exists(modDir)) Files.createDirectories(modDir);
            return modDir.resolve("maps.json");
        } catch (Exception e) {
            CSEditionMod.LOGGER.error("[CS-Edition] Failed to get world config path", e);
            return null;
        }
    }

    public static void load() {
        // На клиенте карты приходят по сети
        if (!isServerSide()) {
            CSEditionMod.LOGGER.info("[CS-Edition] Client side — maps will be synced from server");
            return;
        }

        Path file = getWorldConfigPath();
        if (file == null) {
            CSEditionMod.LOGGER.warn("[CS-Edition] No world loaded, skipping maps.json load");
            return;
        }
        currentFile = file;

        try {
            if (!Files.exists(file)) {
                writeDefault(file);
            }

            String content = Files.readString(file);
            JsonObject root;
            try {
                root = new Gson().fromJson(content, JsonObject.class);
            } catch (JsonSyntaxException e) {
                CSEditionMod.LOGGER.error("[CS-Edition] maps.json is broken: {}", e.getMessage());
                CSEditionMod.LOGGER.error("[CS-Edition] Fix the file at: {}", file);
                return;
            }
            if (root == null) return;

            MAPS.clear();
            cachedJson = null;

            if (root.has("lobbySpawn")) {
                try {
                    lobbySpawn = parsePos(root.getAsJsonArray("lobbySpawn"));
                } catch (Exception e) {
                    CSEditionMod.LOGGER.warn("[CS-Edition] Bad lobbySpawn, using default");
                }
            }

            JsonArray arr = root.getAsJsonArray("maps");
            if (arr != null) {
                for (int i = 0; i < arr.size(); i++) {
                    JsonElement el = arr.get(i);
                    try {
                        JsonObject obj = el.getAsJsonObject();
                        String id = obj.get("id").getAsString();
                        String name = obj.has("displayName") ? obj.get("displayName").getAsString() : id;
                        List<BlockPos> tSpawns = parsePosList(obj.getAsJsonArray("tSpawns"));
                        List<BlockPos> ctSpawns = parsePosList(obj.getAsJsonArray("ctSpawns"));
                        BlockPos tMin = parsePos(obj.getAsJsonArray("tBuyZoneMin"));
                        BlockPos tMax = parsePos(obj.getAsJsonArray("tBuyZoneMax"));
                        BlockPos ctMin = parsePos(obj.getAsJsonArray("ctBuyZoneMin"));
                        BlockPos ctMax = parsePos(obj.getAsJsonArray("ctBuyZoneMax"));
                        String modeId = obj.has("modeId") ? obj.get("modeId").getAsString() : "";
                        MAPS.put(id, new MapData(id, name, modeId, tSpawns, ctSpawns,
                                tMin, tMax, ctMin, ctMax, lobbySpawn));
                        CSEditionMod.LOGGER.info("[CS-Edition] Loaded map: {} ({})", id, name);
                    } catch (Exception e) {
                        CSEditionMod.LOGGER.error("[CS-Edition] Skipping broken map entry #{}: {}", i, e.getMessage());
                    }
                }
            }
            CSEditionMod.LOGGER.info("[CS-Edition] Loaded {} maps from {}", MAPS.size(), file);
        } catch (IOException e) {
            CSEditionMod.LOGGER.error("[CS-Edition] Failed to read maps.json", e);
        }
    }

    /**
     * Сохраняет текущее состояние в файл maps.json в папке мира.
     * Вызывается после каждого изменения через команды.
     */
    public static void save() {
        Path file = currentFile != null ? currentFile : getWorldConfigPath();
        if (file == null) {
            CSEditionMod.LOGGER.warn("[CS-Edition] Cannot save: no world loaded");
            return;
        }
        try {
            if (!Files.exists(file.getParent())) Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                w.write(toJson());
            }
            cachedJson = null; // Сброс кэша
            CSEditionMod.LOGGER.info("[CS-Edition] Saved maps.json to {}", file);
        } catch (IOException e) {
            CSEditionMod.LOGGER.error("[CS-Edition] Failed to save maps.json", e);
        }
    }

    /**
     * Возвращает текущее состояние карт в виде JSON-строки для отправки клиентам.
     */
    public static String toJson() {
        if (cachedJson != null) return cachedJson;
        JsonObject root = new JsonObject();
        JsonArray lobbyArr = new JsonArray();
        lobbyArr.add(lobbySpawn.getX());
        lobbyArr.add(lobbySpawn.getY());
        lobbyArr.add(lobbySpawn.getZ());
        root.add("lobbySpawn", lobbyArr);

        JsonArray mapsArr = new JsonArray();
        for (MapData m : MAPS.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", m.getId());
            obj.addProperty("displayName", m.getDisplayName());
            obj.addProperty("modeId", m.getModeId());
            obj.add("tSpawns", posListToJson(m.getTSpawns()));
            obj.add("ctSpawns", posListToJson(m.getCtSpawns()));
            obj.add("tBuyZoneMin", posToJson(m.getTBuyZoneMin()));
            obj.add("tBuyZoneMax", posToJson(m.getTBuyZoneMax()));
            obj.add("ctBuyZoneMin", posToJson(m.getCtBuyZoneMin()));
            obj.add("ctBuyZoneMax", posToJson(m.getCtBuyZoneMax()));
            mapsArr.add(obj);
        }
        root.add("maps", mapsArr);

        cachedJson = new GsonBuilder().setPrettyPrinting().create().toJson(root);
        return cachedJson;
    }

    private static JsonArray posToJson(BlockPos p) {
        JsonArray a = new JsonArray();
        a.add(p.getX()); a.add(p.getY()); a.add(p.getZ());
        return a;
    }

    private static JsonArray posListToJson(List<BlockPos> list) {
        JsonArray arr = new JsonArray();
        for (BlockPos p : list) arr.add(posToJson(p));
        return arr;
    }

    private static void writeDefault(Path file) throws IOException {
        try (Writer w = Files.newBufferedWriter(file)) {
            w.write("{\n" +
                    "  \"lobbySpawn\": [0, 100, 0],\n" +
                    "  \"maps\": [\n" +
                    "    {\n" +
                    "      \"id\": \"de_dust2\",\n" +
                    "      \"displayName\": \"Dust 2\",\n" +
                    "      \"tSpawns\": [[10, 100, 10]],\n" +
                    "      \"ctSpawns\": [[-10, 100, -10]],\n" +
                    "      \"tBuyZoneMin\": [5, 95, 5],\n" +
                    "      \"tBuyZoneMax\": [15, 110, 15],\n" +
                    "      \"ctBuyZoneMin\": [-15, 95, -15],\n" +
                    "      \"ctBuyZoneMax\": [-5, 110, -5]\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n");
        }
    }

    private static BlockPos parsePos(JsonArray arr) {
        return new BlockPos(arr.get(0).getAsInt(), arr.get(1).getAsInt(), arr.get(2).getAsInt());
    }

    private static List<BlockPos> parsePosList(JsonArray arr) {
        List<BlockPos> list = new ArrayList<>();
        if (arr == null) return list;
        for (JsonElement el : arr) list.add(parsePos(el.getAsJsonArray()));
        return list;
    }

    private static boolean isServerSide() {
        try {
            net.minecraftforge.api.distmarker.Dist dist =
                net.minecraftforge.fml.loading.FMLEnvironment.dist;
            return dist.isDedicatedServer() || isLanHost();
        } catch (Exception e) {
            return true;
        }
    }

    private static boolean isLanHost() {
        try {
            return ServerLifecycleHooks.getCurrentServer() != null;
        } catch (Exception e) {
            return false;
        }
    }

    // ====================== Мутаторы (для команд) ======================

    public static void setLobbySpawn(BlockPos pos) {
        lobbySpawn = pos;
        cachedJson = null;
        save();
    }

    public static void addOrUpdateMap(String id, String displayName) {
        addOrUpdateMap(id, displayName, "");
    }

    public static void addOrUpdateMap(String id, String displayName, String modeId) {
        MapData existing = MAPS.get(id);
        if (existing != null) {
            // Обновляем имя и modeId, спавны и зоны оставляем
            MapData updated = new MapData(id, displayName, modeId,
                    existing.getTSpawns(), existing.getCtSpawns(),
                    existing.getTBuyZoneMin(), existing.getTBuyZoneMax(),
                    existing.getCtBuyZoneMin(), existing.getCtBuyZoneMax(),
                    lobbySpawn);
            MAPS.put(id, updated);
        } else {
            // Новая карта с дефолтными значениями вокруг лобби
            BlockPos base = lobbySpawn;
            MapData newMap = new MapData(id, displayName, modeId,
                    new ArrayList<>(List.of(base.offset(10, 0, 10))),
                    new ArrayList<>(List.of(base.offset(-10, 0, -10))),
                    base.offset(5, -5, 5), base.offset(15, 15, 15),
                    base.offset(-15, -5, -15), base.offset(-5, 15, -5),
                    lobbySpawn);
            MAPS.put(id, newMap);
        }
        cachedJson = null;
        save();
    }

    public static void setMapMode(String mapId, String modeId) {
        MapData m = MAPS.get(mapId);
        if (m == null) return;
        MapData updated = new MapData(m.getId(), m.getDisplayName(), modeId,
                m.getTSpawns(), m.getCtSpawns(),
                m.getTBuyZoneMin(), m.getTBuyZoneMax(),
                m.getCtBuyZoneMin(), m.getCtBuyZoneMax(),
                m.getLobbySpawn());
        MAPS.put(mapId, updated);
        cachedJson = null;
        save();
    }

    public static void deleteMap(String id) {
        MAPS.remove(id);
        cachedJson = null;
        save();
    }

    public static void addSpawn(String mapId, Team team, BlockPos pos) {
        MapData m = MAPS.get(mapId);
        if (m == null) return;
        List<BlockPos> list = (team == Team.T) ? new ArrayList<>(m.getTSpawns()) : new ArrayList<>(m.getCtSpawns());
        list.add(pos);
        MapData updated = new MapData(m.getId(), m.getDisplayName(),
                team == Team.T ? list : m.getTSpawns(),
                team == Team.CT ? list : m.getCtSpawns(),
                m.getTBuyZoneMin(), m.getTBuyZoneMax(),
                m.getCtBuyZoneMin(), m.getCtBuyZoneMax(),
                m.getLobbySpawn());
        MAPS.put(mapId, updated);
        cachedJson = null;
        save();
    }

    public static void clearSpawns(String mapId, Team team) {
        MapData m = MAPS.get(mapId);
        if (m == null) return;
        MapData updated = new MapData(m.getId(), m.getDisplayName(),
                team == Team.T ? new ArrayList<>() : m.getTSpawns(),
                team == Team.CT ? new ArrayList<>() : m.getCtSpawns(),
                m.getTBuyZoneMin(), m.getTBuyZoneMax(),
                m.getCtBuyZoneMin(), m.getCtBuyZoneMax(),
                m.getLobbySpawn());
        MAPS.put(mapId, updated);
        cachedJson = null;
        save();
    }

    /**
     * Устанавливает зону закупа для команды.
     * Y автоматически ставится от -60 до 222 (полная высота мира),
     * XZ берётся из переданных точек.
     */
    public static void setBuyZone(String mapId, Team team, BlockPos min, BlockPos max) {
        MapData m = MAPS.get(mapId);
        if (m == null) return;
        // Y всегда от -60 до 222, XZ — из выбранных точек
        BlockPos realMin = new BlockPos(min.getX(), MapData.BUY_ZONE_Y_MIN, min.getZ());
        BlockPos realMax = new BlockPos(max.getX(), MapData.BUY_ZONE_Y_MAX, max.getZ());
        MapData updated = new MapData(m.getId(), m.getDisplayName(),
                m.getTSpawns(), m.getCtSpawns(),
                team == Team.T ? realMin : m.getTBuyZoneMin(),
                team == Team.T ? realMax : m.getTBuyZoneMax(),
                team == Team.CT ? realMin : m.getCtBuyZoneMin(),
                team == Team.CT ? realMax : m.getCtBuyZoneMax(),
                m.getLobbySpawn());
        MAPS.put(mapId, updated);
        cachedJson = null;
        save();
    }

    public static Map<String, MapData> getMaps() { return MAPS; }
    public static MapData getMap(String id) { return MAPS.get(id); }
    public static BlockPos getLobbySpawn() { return lobbySpawn; }
    public static Path getCurrentFile() { return currentFile; }
}
