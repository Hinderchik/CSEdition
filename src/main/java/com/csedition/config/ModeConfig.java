package com.csedition.config;

import com.csedition.CSEditionMod;
import com.csedition.data.GameMode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Конфиг режимов игры. Хранится в <world>/data/csedition/modes.json.
 *
 * Содержит встроенные режимы (classic, deathmatch, gungame, pistol_only)
 * и пользовательские режимы, созданные через /cs mode.
 *
 * Клиент получает JSON целиком через PacketSyncModes и парсит локально.
 */
public final class ModeConfig {
    private static final Map<String, GameMode> MODES = new LinkedHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static String currentFile = "<not loaded>";
    private static String cachedJson = null;

    static {
        // Встроенные режимы — всегда доступны
        registerBuiltIn(GameMode.classic());
        registerBuiltIn(GameMode.deathmatch());
        registerBuiltIn(GameMode.gungame());
        registerBuiltIn(GameMode.pistolOnly());
    }

    private ModeConfig() {}

    public static void registerBuiltIn(GameMode mode) {
        MODES.put(mode.getId(), mode);
    }

    public static Map<String, GameMode> getModes() {
        return MODES;
    }

    public static GameMode getMode(String id) {
        return MODES.get(id);
    }

    public static GameMode getOrDefault(String id) {
        GameMode m = MODES.get(id);
        return m != null ? m : MODES.get("classic");
    }

    public static String getCurrentFile() {
        return currentFile;
    }

    /**
     * Загружает режимы с диска (только сервер).
     * Вызывается из ServerEvents.onServerStarted.
     */
    public static void load() {
        if (FMLEnvironment.dist == Dist.CLIENT) return;
        try {
            Path path = getConfigPath();
            currentFile = path.toString();
            if (!Files.exists(path)) {
                CSEditionMod.LOGGER.info("[CS-Edition] modes.json not found, using built-in modes only");
                save();
                return;
            }
            try (Reader r = Files.newBufferedReader(path)) {
                JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
                JsonArray arr = root.getAsJsonArray("modes");
                if (arr != null) {
                    for (JsonElement el : arr) {
                        try {
                            JsonObject obj = el.getAsJsonObject();
                            String id = obj.get("id").getAsString();
                            // Не перезаписываем встроенные
                            GameMode existing = MODES.get(id);
                            if (existing != null && existing.isBuiltIn()) continue;
                            GameMode mode = parseMode(obj);
                            if (mode != null) MODES.put(id, mode);
                        } catch (Exception e) {
                            CSEditionMod.LOGGER.warn("[CS-Edition] Skipped bad mode entry: {}", e.getMessage());
                        }
                    }
                }
            }
            CSEditionMod.LOGGER.info("[CS-Edition] Loaded {} modes from {}", MODES.size(), path);
        } catch (Exception e) {
            CSEditionMod.LOGGER.error("[CS-Edition] Failed to load modes.json: {}", e.getMessage());
        }
    }

    /**
     * Сохраняет режимы на диск (только сервер).
     */
    public static void save() {
        if (FMLEnvironment.dist == Dist.CLIENT) return;
        try {
            Path path = getConfigPath();
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) {
                JsonObject root = new JsonObject();
                JsonArray arr = new JsonArray();
                for (GameMode m : MODES.values()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("id", m.getId());
                    obj.addProperty("displayName", m.getDisplayName());
                    obj.addProperty("description", m.getDescription());
                    obj.addProperty("startMoney", m.getStartMoney());
                    obj.addProperty("killReward", m.getKillReward());
                    obj.addProperty("roundWinReward", m.getRoundWinReward());
                    obj.addProperty("buyTimeSeconds", m.getBuyTimeSeconds());
                    obj.addProperty("roundTimeSeconds", m.getRoundTimeSeconds());
                    obj.addProperty("respawn", m.isRespawn());
                    obj.addProperty("allowBuy", m.isAllowBuy());
                    obj.addProperty("builtIn", m.isBuiltIn());
                    JsonArray tArr = new JsonArray();
                    for (String s : m.getStartWeaponsT()) tArr.add(s);
                    obj.add("startWeaponsT", tArr);
                    JsonArray ctArr = new JsonArray();
                    for (String s : m.getStartWeaponsCT()) ctArr.add(s);
                    obj.add("startWeaponsCT", ctArr);
                    arr.add(obj);
                }
                root.add("modes", arr);
                GSON.toJson(root, w);
            }
            CSEditionMod.LOGGER.info("[CS-Edition] Saved {} modes to {}", MODES.size(), path);
        } catch (IOException e) {
            CSEditionMod.LOGGER.error("[CS-Edition] Failed to save modes.json: {}", e.getMessage());
        }
    }

    private static GameMode parseMode(JsonObject obj) {
        String id = obj.get("id").getAsString();
        String name = obj.has("displayName") ? obj.get("displayName").getAsString() : id;
        String desc = obj.has("description") ? obj.get("description").getAsString() : "";
        int startMoney = obj.has("startMoney") ? obj.get("startMoney").getAsInt() : 800;
        int killReward = obj.has("killReward") ? obj.get("killReward").getAsInt() : 300;
        int roundWin = obj.has("roundWinReward") ? obj.get("roundWinReward").getAsInt() : 3000;
        int buyTime = obj.has("buyTimeSeconds") ? obj.get("buyTimeSeconds").getAsInt() : 15;
        int roundTime = obj.has("roundTimeSeconds") ? obj.get("roundTimeSeconds").getAsInt() : 120;
        int roundsToWin = obj.has("roundsToWin") ? obj.get("roundsToWin").getAsInt() : 8;
        boolean respawn = obj.has("respawn") && obj.get("respawn").getAsBoolean();
        boolean allowBuy = !obj.has("allowBuy") || obj.get("allowBuy").getAsBoolean();
        List<String> tW = new ArrayList<>();
        if (obj.has("startWeaponsT")) {
            for (JsonElement e : obj.getAsJsonArray("startWeaponsT")) tW.add(e.getAsString());
        }
        List<String> ctW = new ArrayList<>();
        if (obj.has("startWeaponsCT")) {
            for (JsonElement e : obj.getAsJsonArray("startWeaponsCT")) ctW.add(e.getAsString());
        }
        return new GameMode(id, name, desc, startMoney, killReward, roundWin,
                buyTime, roundTime, roundsToWin, respawn, allowBuy, tW, ctW, false);
    }

    /**
     * Возвращает JSON всех режимов для синхронизации с клиентом.
     */
    public static String toJson() {
        if (cachedJson != null) return cachedJson;
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        for (GameMode m : MODES.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", m.getId());
            obj.addProperty("displayName", m.getDisplayName());
            obj.addProperty("description", m.getDescription());
            obj.addProperty("startMoney", m.getStartMoney());
            obj.addProperty("killReward", m.getKillReward());
            obj.addProperty("roundWinReward", m.getRoundWinReward());
            obj.addProperty("buyTimeSeconds", m.getBuyTimeSeconds());
            obj.addProperty("roundTimeSeconds", m.getRoundTimeSeconds());
            obj.addProperty("respawn", m.isRespawn());
            obj.addProperty("allowBuy", m.isAllowBuy());
            JsonArray tArr = new JsonArray();
            for (String s : m.getStartWeaponsT()) tArr.add(s);
            obj.add("startWeaponsT", tArr);
            JsonArray ctArr = new JsonArray();
            for (String s : m.getStartWeaponsCT()) ctArr.add(s);
            obj.add("startWeaponsCT", ctArr);
            arr.add(obj);
        }
        root.add("modes", arr);
        cachedJson = GSON.toJson(root);
        return cachedJson;
    }

    /**
     * Парсит JSON на клиенте и обновляет локальный кэш.
     */
    @OnlyIn(Dist.CLIENT)
    public static void fromJson(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("modes");
            if (arr != null) {
                for (JsonElement el : arr) {
                    try {
                        JsonObject obj = el.getAsJsonObject();
                        String id = obj.get("id").getAsString();
                        GameMode existing = MODES.get(id);
                        if (existing != null && existing.isBuiltIn()) continue;
                        GameMode mode = parseMode(obj);
                        if (mode != null) MODES.put(id, mode);
                    } catch (Exception e) {
                        // skip bad entry
                    }
                }
            }
        } catch (Exception e) {
            CSEditionMod.LOGGER.error("[CS-Edition] Failed to parse modes JSON: {}", e.getMessage());
        }
    }

    public static void invalidateCache() {
        cachedJson = null;
    }

    /**
     * Добавляет или обновляет пользовательский режим.
     */
    public static void addOrUpdateMode(GameMode mode) {
        MODES.put(mode.getId(), mode);
        invalidateCache();
        save();
    }

    public static void deleteMode(String id) {
        GameMode m = MODES.get(id);
        if (m != null && !m.isBuiltIn()) {
            MODES.remove(id);
            invalidateCache();
            save();
        }
    }

    private static Path getConfigPath() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return Path.of("config", "csedition-modes.json");
        }
        return server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve("data").resolve("csedition").resolve("modes.json");
    }
}
