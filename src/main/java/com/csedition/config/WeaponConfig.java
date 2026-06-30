package com.csedition.config;

import com.csedition.CSEditionMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Конфиг оружия — читается из config/csedition-weapons.json.
 *
 * Если файл не существует — создаётся автоматически с дефолтным списком
 * (тот же что раньше был захардкожен в TaczHelper).
 *
 * Если файл существует — дефолты из него перекрывают встроенные.
 *
 * Формат файла:
 * {
 *   "weapons": [
 *     { "id": "tacz:glock_17", "displayName": "Glock 17", "magazineSize": 17, "fireMode": "SEMI", "price": 200, "category": "pistol" },
 *     { "id": "tacz:ak47",    "displayName": "AK-47",  "magazineSize": 30, "fireMode": "AUTO", "price": 2700, "category": "rifle",
 *       "attachments": { "AttachmentMUZZLE": "tacz:silencer" } }
 *   ]
 * }
 *
 * Пушка попадает в GunPriceTable по полям price/category,
 * в TaczHelper — по magazineSize/fireMode/attachments.
 *
 * Путь: config/csedition-weapons.json (общий для клиента и сервера).
 */
public final class WeaponConfig {
    private WeaponConfig() {}

    /** Магазин по умолчанию если пушка не указана в конфиге. */
    public static final int DEFAULT_MAGAZINE = 30;
    /** Fire mode по умолчанию. */
    public static final String DEFAULT_FIRE_MODE = "SEMI";

    // === Основные хранилища ===
    private static final Map<String, WeaponDef> WEAPONS = new LinkedHashMap<>();
    private static final Map<String, Integer> PRICES = new HashMap<>();
    private static final Map<String, String> CATEGORIES = new HashMap<>();

    // === Дефолтные списки (если конфиг не найден) ===
    private static final Map<String, int[]> DEFAULT_PISTOLS = new HashMap<>();
    private static final Map<String, int[]> DEFAULT_SMGS = new HashMap<>();
    private static final Map<String, int[]> DEFAULT_RIFLES = new HashMap<>();
    private static final Map<String, int[]> DEFAULT_SNIPERS = new HashMap<>();
    private static final Map<String, int[]> DEFAULT_SHOTGUNS = new HashMap<>();
    private static final Map<String, int[]> DEFAULT_HEAVY = new HashMap<>();

    static {
        // mag, price
        DEFAULT_PISTOLS.put("tacz:glock_17", new int[]{17, 200});
        DEFAULT_PISTOLS.put("mcs2:cs_usp", new int[]{12, 200});
        DEFAULT_PISTOLS.put("tacz:p320", new int[]{17, 300});
        DEFAULT_PISTOLS.put("tacz:deagle", new int[]{7, 700});
        DEFAULT_PISTOLS.put("tacz:cz75", new int[]{16, 500});
        DEFAULT_SMGS.put("tacz:hk_mp5a5", new int[]{30, 1500});
        DEFAULT_SMGS.put("tacz:ump45", new int[]{30, 1200});
        DEFAULT_SMGS.put("tacz:p90", new int[]{50, 2350});
        DEFAULT_SMGS.put("tacz:uzi", new int[]{32, 1200});
        DEFAULT_RIFLES.put("tacz:ak47", new int[]{30, 2700});
        DEFAULT_RIFLES.put("tacz:m4a1", new int[]{30, 3100});
        DEFAULT_RIFLES.put("tacz:fn_fal", new int[]{30, 3300});
        DEFAULT_RIFLES.put("tacz:hk416d", new int[]{30, 3300});
        DEFAULT_RIFLES.put("tacz:qbz_95", new int[]{30, 3000});
        DEFAULT_RIFLES.put("tacz:aug", new int[]{30, 3300});
        DEFAULT_RIFLES.put("tacz:scar_l", new int[]{30, 3200});
        DEFAULT_RIFLES.put("tacz:scar_h", new int[]{30, 3200});
        DEFAULT_SNIPERS.put("tacz:ai_awp", new int[]{5, 4750});
        DEFAULT_SNIPERS.put("mcs2:cs_awp", new int[]{5, 4750});
        DEFAULT_SNIPERS.put("tacz:m700", new int[]{5, 1700});
        DEFAULT_SHOTGUNS.put("tacz:m870", new int[]{5, 1050});
        DEFAULT_SHOTGUNS.put("tacz:spas_12", new int[]{6, 1800});
        DEFAULT_HEAVY.put("tacz:m249", new int[]{100, 5200});
        DEFAULT_HEAVY.put("tacz:fn_evolys", new int[]{75, 5500});
    }

    public static class WeaponDef {
        public String id;
        public String displayName;
        public int magazineSize;
        public String fireMode; // SEMI / AUTO / BURST
        public int price;
        public String category; // pistol / smg / rifle / sniper / shotgun / heavy / utility
        public Map<String, String> attachments;

        public WeaponDef() {}

        public WeaponDef(String id, String displayName, int magazineSize,
                         String fireMode, int price, String category) {
            this.id = id;
            this.displayName = displayName;
            this.magazineSize = magazineSize;
            this.fireMode = fireMode;
            this.price = price;
            this.category = category;
            this.attachments = new HashMap<>();
        }
    }

    /**
     * Загружает оружие из config/csedition-weapons.json.
     * Если файла нет — создаёт с дефолтами.
     * Можно вызывать повторно для reload.
     */
    public static void load() {
        WEAPONS.clear();
        PRICES.clear();
        CATEGORIES.clear();

        Path path = getConfigPath();
        if (!Files.exists(path)) {
            CSEditionMod.LOGGER.info("[CS-Edition] weapons config not found, creating defaults at {}", path);
            writeDefaults(path);
        }

        // Всегда сначала грузим дефолты (на случай если в конфиге только часть пушек)
        loadDefaults();

        // Потом читаем из файла и перекрываем
        try (Reader r = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("weapons");
            if (arr != null) {
                for (JsonElement el : arr) {
                    try {
                        JsonObject obj = el.getAsJsonObject();
                        String id = obj.get("id").getAsString();
                        WeaponDef def = parseWeapon(obj);
                        if (def != null) {
                            WEAPONS.put(id, def);
                            PRICES.put(id, def.price);
                            CATEGORIES.put(id, def.category != null ? def.category : "other");
                        }
                    } catch (Exception e) {
                        CSEditionMod.LOGGER.warn("[CS-Edition] Bad weapon entry: {}", e.getMessage());
                    }
                }
            }
            CSEditionMod.LOGGER.info("[CS-Edition] Loaded {} weapons from {}", WEAPONS.size(), path);
        } catch (Exception e) {
            CSEditionMod.LOGGER.error("[CS-Edition] Failed to load weapons config: {}", e.getMessage());
        }
    }

    private static WeaponDef parseWeapon(JsonObject obj) {
        WeaponDef def = new WeaponDef();
        def.id = obj.get("id").getAsString();
        def.displayName = obj.has("displayName") ? obj.get("displayName").getAsString() : def.id;
        def.magazineSize = obj.has("magazineSize") ? obj.get("magazineSize").getAsInt() : DEFAULT_MAGAZINE;
        def.fireMode = obj.has("fireMode") ? obj.get("fireMode").getAsString() : DEFAULT_FIRE_MODE;
        def.price = obj.has("price") ? obj.get("price").getAsInt() : 0;
        def.category = obj.has("category") ? obj.get("category").getAsString() : "other";
        def.attachments = new HashMap<>();
        if (obj.has("attachments")) {
            JsonObject att = obj.getAsJsonObject("attachments");
            for (Map.Entry<String, JsonElement> e : att.entrySet()) {
                def.attachments.put(e.getKey(), e.getValue().getAsString());
            }
        }
        return def;
    }

    /**
     * Грузит встроенные дефолты. Если в файле есть такая же пушка — будет перекрыта.
     */
    private static void loadDefaults() {
        addDefault(DEFAULT_PISTOLS, "pistol", "SEMI");
        addDefault(DEFAULT_SMGS, "smg", "AUTO");
        addDefault(DEFAULT_RIFLES, "rifle", "AUTO");
        addDefault(DEFAULT_SNIPERS, "sniper", "SEMI");
        addDefault(DEFAULT_SHOTGUNS, "shotgun", "SEMI");
        addDefault(DEFAULT_HEAVY, "heavy", "AUTO");
        // Утилити (броня)
        WEAPONS.put("tacz:kevlar", new WeaponDef("tacz:kevlar", "БРОНЯ", 0, "NONE", 1000, "utility"));
        PRICES.put("tacz:kevlar", 1000);
        CATEGORIES.put("tacz:kevlar", "utility");
        WEAPONS.put("tacz:helmet", new WeaponDef("tacz:helmet", "ШЛЕМ", 0, "NONE", 650, "utility"));
        PRICES.put("tacz:helmet", 650);
        CATEGORIES.put("tacz:helmet", "utility");
    }

    private static void addDefault(Map<String, int[]> src, String category, String mode) {
        for (Map.Entry<String, int[]> e : src.entrySet()) {
            String id = e.getKey();
            int mag = e.getValue()[0];
            int price = e.getValue()[1];
            WEAPONS.put(id, new WeaponDef(id, id, mag, mode, price, category));
            PRICES.put(id, price);
            CATEGORIES.put(id, category);
        }
    }

    private static void writeDefaults(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            // Утилити
            JsonObject kevlar = new JsonObject();
            kevlar.addProperty("id", "tacz:kevlar");
            kevlar.addProperty("displayName", "Kevlar");
            kevlar.addProperty("price", 650);
            kevlar.addProperty("category", "utility");
            arr.add(kevlar);
            JsonObject helmet = new JsonObject();
            helmet.addProperty("id", "tacz:helmet");
            helmet.addProperty("displayName", "Helmet");
            helmet.addProperty("price", 1000);
            helmet.addProperty("category", "utility");
            arr.add(helmet);
            // Пушки
            for (var entry : DEFAULT_PISTOLS.entrySet()) {
                arr.add(makeWeaponJson(entry.getKey(), entry.getValue()[0], "SEMI", entry.getValue()[1], "pistol"));
            }
            for (var entry : DEFAULT_SMGS.entrySet()) {
                arr.add(makeWeaponJson(entry.getKey(), entry.getValue()[0], "AUTO", entry.getValue()[1], "smg"));
            }
            for (var entry : DEFAULT_RIFLES.entrySet()) {
                arr.add(makeWeaponJson(entry.getKey(), entry.getValue()[0], "AUTO", entry.getValue()[1], "rifle"));
            }
            for (var entry : DEFAULT_SNIPERS.entrySet()) {
                arr.add(makeWeaponJson(entry.getKey(), entry.getValue()[0], "SEMI", entry.getValue()[1], "sniper"));
            }
            for (var entry : DEFAULT_SHOTGUNS.entrySet()) {
                arr.add(makeWeaponJson(entry.getKey(), entry.getValue()[0], "SEMI", entry.getValue()[1], "shotgun"));
            }
            for (var entry : DEFAULT_HEAVY.entrySet()) {
                arr.add(makeWeaponJson(entry.getKey(), entry.getValue()[0], "AUTO", entry.getValue()[1], "heavy"));
            }
            // Пример с аттачментом для mcs2:cs_usp
            JsonObject usp = new JsonObject();
            usp.addProperty("id", "mcs2:cs_usp");
            usp.addProperty("displayName", "USP-S");
            usp.addProperty("magazineSize", 12);
            usp.addProperty("fireMode", "SEMI");
            usp.addProperty("price", 200);
            usp.addProperty("category", "pistol");
            JsonObject att = new JsonObject();
            att.addProperty("AttachmentMUZZLE", "mcs2:usp_silencer");
            usp.add("attachments", att);
            arr.add(usp);
            root.add("weapons", arr);
            try (Writer w = Files.newBufferedWriter(path)) {
                gson.toJson(root, w);
            }
            CSEditionMod.LOGGER.info("[CS-Edition] Wrote default weapons config to {}", path);
        } catch (IOException e) {
            CSEditionMod.LOGGER.error("[CS-Edition] Failed to write default weapons config: {}", e.getMessage());
        }
    }

    private static JsonObject makeWeaponJson(String id, int mag, String mode, int price, String cat) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("displayName", id);
        obj.addProperty("magazineSize", mag);
        obj.addProperty("fireMode", mode);
        obj.addProperty("price", price);
        obj.addProperty("category", cat);
        return obj;
    }

    private static Path getConfigPath() {
        return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("csedition-weapons.json");
    }

    // === Геттеры для других классов ===

    public static WeaponDef getWeapon(String id) {
        return WEAPONS.get(id);
    }

    public static int getMagazineSize(String id) {
        WeaponDef def = WEAPONS.get(id);
        return def != null ? def.magazineSize : DEFAULT_MAGAZINE;
    }

    public static String getFireMode(String id) {
        WeaponDef def = WEAPONS.get(id);
        return def != null ? def.fireMode : DEFAULT_FIRE_MODE;
    }

    public static int getPrice(String id) {
        return PRICES.getOrDefault(id, -1);
    }

    public static String getCategory(String id) {
        return CATEGORIES.getOrDefault(id, "other");
    }

    public static Map<String, Integer> getAllPrices() {
        return new HashMap<>(PRICES);
    }

    public static Map<String, WeaponDef> getAllWeapons() {
        return new HashMap<>(WEAPONS);
    }

    public static Map<String, String> getAttachments(String id) {
        WeaponDef def = WEAPONS.get(id);
        if (def == null || def.attachments == null) return new HashMap<>();
        return new HashMap<>(def.attachments);
    }

    /**
     * Самая дешёвая пушка в категории.
     */
    public static String getCheapestOfCategory(String category) {
        String cheapest = null;
        int minPrice = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> e : PRICES.entrySet()) {
            if (category.equals(CATEGORIES.get(e.getKey())) && e.getValue() < minPrice) {
                minPrice = e.getValue();
                cheapest = e.getKey();
            }
        }
        return cheapest;
    }
}
