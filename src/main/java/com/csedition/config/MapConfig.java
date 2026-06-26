package com.csedition.config;

import com.csedition.CSEditionMod;
import com.csedition.data.MapData;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Загружает maps.json из config/csedition/maps.json.
 * Файл создаётся автоматически с дефолтным содержимым, если отсутствует.
 *
 * Устойчив к ошибкам: если одна карта в JSON битая, остальные всё равно загрузятся.
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
    private static final Map<String, MapData> MAPS = new HashMap<>();
    private static BlockPos lobbySpawn = new BlockPos(0, 100, 0);

    public static void load() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("csedition");
        Path file = configDir.resolve("maps.json");

        try {
            if (!Files.exists(configDir)) Files.createDirectories(configDir);
            if (!Files.exists(file)) {
                writeDefault(file);
            }

            // Читаем файл целиком в строку, чтобы безопасно парсить по частям
            String content = Files.readString(file);
            JsonObject root;
            try {
                root = new Gson().fromJson(content, JsonObject.class);
            } catch (JsonSyntaxException e) {
                CSEditionMod.LOGGER.error("[CS-Edition] maps.json is completely broken: {}", e.getMessage());
                CSEditionMod.LOGGER.error("[CS-Edition] Fix the file at: {}", file);
                return; // Не крашим мод, просто нет карт
            }
            if (root == null) return;

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
                        MAPS.put(id, new MapData(id, name, tSpawns, ctSpawns,
                                tMin, tMax, ctMin, ctMax, lobbySpawn));
                        CSEditionMod.LOGGER.info("[CS-Edition] Loaded map: {} ({})", id, name);
                    } catch (Exception e) {
                        CSEditionMod.LOGGER.error("[CS-Edition] Skipping broken map entry #{}: {}", i, e.getMessage());
                    }
                }
            }
            CSEditionMod.LOGGER.info("[CS-Edition] Loaded {} maps from maps.json", MAPS.size());
        } catch (IOException e) {
            CSEditionMod.LOGGER.error("[CS-Edition] Failed to read maps.json", e);
        }
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

    public static Map<String, MapData> getMaps() { return MAPS; }
    public static MapData getMap(String id) { return MAPS.get(id); }
    public static BlockPos getLobbySpawn() { return lobbySpawn; }
}
