package com.csedition.match;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Таблица цен на оружие.
 * Используется MatchManager при обработке покупки.
 * Цены приближены к CS:GO.
 *
 * Категории:
 *   - pistol: пистолеты
 *   - smg: пистолеты-пулемёты
 *   - rifle: винтовки
 *   - sniper: снайперские
 *   - heavy: тяжёлые (LMG)
 *   - shotgun: дробовики
 *   - utility: снаряжение (броня, шлем)
 *
 * Список пушек должен совпадать с TaczHelper.MAGAZINE_SIZES.
 */
public final class GunPriceTable {
    private static final Map<String, Integer> PRICES = new LinkedHashMap<>();
    private static final Map<String, String> CATEGORIES = new HashMap<>();

    static {
        // Пистолеты
        PRICES.put("tacz:glock_17", 200);    CATEGORIES.put("tacz:glock_17", "pistol");
        PRICES.put("mcs2:cs_usp", 200);      CATEGORIES.put("mcs2:cs_usp", "pistol");
        PRICES.put("tacz:p320", 300);        CATEGORIES.put("tacz:p320", "pistol");
        PRICES.put("tacz:deagle", 700);      CATEGORIES.put("tacz:deagle", "pistol");
        PRICES.put("tacz:cz75", 500);        CATEGORIES.put("tacz:cz75", "pistol");
        // SMG
        PRICES.put("tacz:hk_mp5a5", 1500);   CATEGORIES.put("tacz:hk_mp5a5", "smg");
        PRICES.put("tacz:ump45", 1200);      CATEGORIES.put("tacz:ump45", "smg");
        PRICES.put("tacz:p90", 2350);        CATEGORIES.put("tacz:p90", "smg");
        PRICES.put("tacz:uzi", 1200);        CATEGORIES.put("tacz:uzi", "smg");
        // Винтовки
        PRICES.put("tacz:ak47", 2700);       CATEGORIES.put("tacz:ak47", "rifle");
        PRICES.put("tacz:m4a1", 3100);       CATEGORIES.put("tacz:m4a1", "rifle");
        PRICES.put("tacz:fn_fal", 3300);     CATEGORIES.put("tacz:fn_fal", "rifle");
        PRICES.put("tacz:hk416d", 3300);     CATEGORIES.put("tacz:hk416d", "rifle");
        PRICES.put("tacz:qbz_95", 3000);     CATEGORIES.put("tacz:qbz_95", "rifle");
        PRICES.put("tacz:aug", 3300);        CATEGORIES.put("tacz:aug", "rifle");
        PRICES.put("tacz:scar_l", 3200);     CATEGORIES.put("tacz:scar_l", "rifle");
        PRICES.put("tacz:scar_h", 3200);     CATEGORIES.put("tacz:scar_h", "rifle");
        // Снайперские
        PRICES.put("tacz:ai_awp", 4750);     CATEGORIES.put("tacz:ai_awp", "sniper");
        PRICES.put("mcs2:cs_awp", 4750);     CATEGORIES.put("mcs2:cs_awp", "sniper");
        PRICES.put("tacz:m700", 1700);       CATEGORIES.put("tacz:m700", "sniper");
        // Дробовики
        PRICES.put("tacz:m870", 1050);       CATEGORIES.put("tacz:m870", "shotgun");
        PRICES.put("tacz:spas_12", 1800);    CATEGORIES.put("tacz:spas_12", "shotgun");
        // LMG
        PRICES.put("tacz:m249", 5200);       CATEGORIES.put("tacz:m249", "heavy");
        PRICES.put("tacz:fn_evolys", 5500);  CATEGORIES.put("tacz:fn_evolys", "heavy");
        // Снаряжение
        PRICES.put("tacz:kevlar", 650);      CATEGORIES.put("tacz:kevlar", "utility");
        PRICES.put("tacz:helmet", 1000);     CATEGORIES.put("tacz:helmet", "utility");
    }

    private GunPriceTable() {}

    public static int getPrice(String gunId) {
        return PRICES.getOrDefault(gunId, -1);
    }

    public static String getCategory(String gunId) {
        return CATEGORIES.getOrDefault(gunId, "other");
    }

    public static Map<String, Integer> getAll() { return PRICES; }

    /**
     * Возвращает самое дешёвое оружие в категории (для быстрой закупки).
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
