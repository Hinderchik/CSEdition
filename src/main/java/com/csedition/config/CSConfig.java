package com.csedition.config;

import com.csedition.CSEditionMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Конфиг мода: хранится в &lt;world&gt;/data/csedition/csconfig.json
 *
 * Параметры:
 *   - maxInventorySlots: максимум слотов инвентаря во время матча (по умолчанию 3)
 *   - killsToWin: количество киллов для победы в матче (по умолчанию 10)
 *   - keptItems: список предметов, которые НЕ пропадают из инвентаря при очистке
 *   - clearInventoryOnMatchEnd: очищать ли инвентарь после окончания матча (по умолчанию true)
 */
public class CSConfig {
    private static final String FILE_NAME = "csconfig.json";

    private static int maxInventorySlots = 3;
    private static int killsToWin = 10;
    private static boolean clearInventoryOnMatchEnd = true;
    private static final Set<String> keptItems = new HashSet<>();
    private static Path currentFile = null;

    // Дефолтные предметы, которые не пропадают (компас, часы и т.п.)
    static {
        keptItems.add("minecraft:compass");
        keptItems.add("minecraft:clock");
    }

    public static int getMaxInventorySlots() { return maxInventorySlots; }
    public static int getKillsToWin() { return killsToWin; }
    public static boolean isClearInventoryOnMatchEnd() { return clearInventoryOnMatchEnd; }
    public static Set<String> getKeptItems() { return Collections.unmodifiableSet(keptItems); }

    public static void setMaxInventorySlots(int v) {
        maxInventorySlots = Math.max(1, Math.min(36, v));
        save();
    }

    public static void setKillsToWin(int v) {
        killsToWin = Math.max(1, v);
        save();
    }

    public static void setClearInventoryOnMatchEnd(boolean v) {
        clearInventoryOnMatchEnd = v;
        save();
    }

    public static void addKeptItem(String itemId) {
        keptItems.add(itemId);
        save();
    }

    public static void removeKeptItem(String itemId) {
        keptItems.remove(itemId);
        save();
    }

    /**
     * Проверяет, нужно ли сохранять предмет при очистке инвентаря.
     */
    public static boolean shouldKeepItem(Item item) {
        if (item == null) return false;
        var key = BuiltInRegistries.ITEM.getKey(item);
        return keptItems.contains(key.toString());
    }

    /**
     * Возвращает путь к файлу конфига в папке мира.
     */
    private static Path getConfigPath() {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return null;
            Path worldDataDir = server.getWorldPath(LevelResource.ROOT);
            Path modDir = worldDataDir.resolve("csedition");
            if (!Files.exists(modDir)) Files.createDirectories(modDir);
            return modDir.resolve(FILE_NAME);
        } catch (Exception e) {
            CSEditionMod.LOGGER.error("[CS-Edition] Failed to get csconfig path", e);
            return null;
        }
    }

    public static void load() {
        try {
            net.minecraftforge.api.distmarker.Dist dist =
                net.minecraftforge.fml.loading.FMLEnvironment.dist;
            if (dist.isClient() && ServerLifecycleHooks.getCurrentServer() == null) {
                return; // На чистом клиенте не загружаем
            }
        } catch (Exception ignored) {}

        Path file = getConfigPath();
        if (file == null) return;
        currentFile = file;

        try {
            if (!Files.exists(file)) {
                writeDefault(file);
                return;
            }
            String content = Files.readString(file);
            JsonObject root = new Gson().fromJson(content, JsonObject.class);
            if (root == null) return;

            if (root.has("maxInventorySlots")) {
                maxInventorySlots = Math.max(1, Math.min(36, root.get("maxInventorySlots").getAsInt()));
            }
            if (root.has("killsToWin")) {
                killsToWin = Math.max(1, root.get("killsToWin").getAsInt());
            }
            if (root.has("clearInventoryOnMatchEnd")) {
                clearInventoryOnMatchEnd = root.get("clearInventoryOnMatchEnd").getAsBoolean();
            }
            if (root.has("keptItems")) {
                keptItems.clear();
                JsonArray arr = root.getAsJsonArray("keptItems");
                if (arr != null) {
                    for (int i = 0; i < arr.size(); i++) {
                        keptItems.add(arr.get(i).getAsString());
                    }
                }
            }
            CSEditionMod.LOGGER.info("[CS-Edition] Loaded csconfig.json: slots={}, killsToWin={}, kept={}",
                    maxInventorySlots, killsToWin, keptItems.size());
        } catch (Exception e) {
            CSEditionMod.LOGGER.error("[CS-Edition] Failed to load csconfig.json", e);
        }
    }

    public static void save() {
        Path file = currentFile != null ? currentFile : getConfigPath();
        if (file == null) return;
        try {
            if (!Files.exists(file.getParent())) Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                w.write(toJson());
            }
        } catch (IOException e) {
            CSEditionMod.LOGGER.error("[CS-Edition] Failed to save csconfig.json", e);
        }
    }

    public static String toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("maxInventorySlots", maxInventorySlots);
        root.addProperty("killsToWin", killsToWin);
        root.addProperty("clearInventoryOnMatchEnd", clearInventoryOnMatchEnd);
        JsonArray arr = new JsonArray();
        for (String s : keptItems) arr.add(s);
        root.add("keptItems", arr);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }

    private static void writeDefault(Path file) throws IOException {
        try (Writer w = Files.newBufferedWriter(file)) {
            w.write(toJson());
        }
        CSEditionMod.LOGGER.info("[CS-Edition] Created default csconfig.json at {}", file);
    }
}
