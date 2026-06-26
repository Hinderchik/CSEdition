package com.csedition.match;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Таблица цен на оружие TaCZ.
 * Используется MatchManager при обработке покупки.
 * Цены приближены к CS:GO.
 *
 * Категории:
 *   - pistol: пистолеты
 *   - smg: пистолеты-пулемёты
 *   - rifle: винтовки
 *   - sniper: снайперские
 *   - heavy: тяжёлые
 *   - utility: снаряжение (броня, шлем)
 */
public final class GunPriceTable {
    private static final Map<String, Integer> PRICES = new LinkedHashMap<>();
    private static final Map<String, String> CATEGORIES = new HashMap<>();

    static {
        // Пистолеты
        PRICES.put("tacz:glock_17", 200);
        CATEGORIES.put("tacz:glock_17", "pistol");
        PRICES.put("tacz:usp_45", 200);
        CATEGORIES.put("tacz:usp_45", "pistol");
        PRICES.put("tacz:p250", 300);
        CATEGORIES.put("tacz:p250", "pistol");
        PRICES.put("tacz:desert_eagle", 700);
        CATEGORIES.put("tacz:desert_eagle", "pistol");
        // SMG
        PRICES.put("tacz:mp5", 1500);
        CATEGORIES.put("tacz:mp5", "smg");
        PRICES.put("tacz:p90", 2350);
        CATEGORIES.put("tacz:p90", "smg");
        PRICES.put("tacz:ump_45", 1200);
        CATEGORIES.put("tacz:ump_45", "smg");
        // Винтовки
        PRICES.put("tacz:ak47", 2700);
        CATEGORIES.put("tacz:ak47", "rifle");
        PRICES.put("tacz:m4a4", 3100);
        CATEGORIES.put("tacz:m4a4", "rifle");
        PRICES.put("tacz:awp", 4750);
        CATEGORIES.put("tacz:awp", "sniper");
        PRICES.put("tacz:aug", 3300);
        CATEGORIES.put("tacz:aug", "rifle");
        PRICES.put("tacz:sg556", 3000);
        CATEGORIES.put("tacz:sg556", "rifle");
        // Тяжёлые
        PRICES.put("tacz:nova", 1050);
        CATEGORIES.put("tacz:nova", "heavy");
        PRICES.put("tacz:xm1014", 2000);
        CATEGORIES.put("tacz:xm1014", "heavy");
        PRICES.put("tacz:m249", 5200);
        CATEGORIES.put("tacz:m249", "heavy");
        // Снайперские
        PRICES.put("tacz:scout", 1700);
        CATEGORIES.put("tacz:scout", "sniper");
        // Снаряжение
        PRICES.put("tacz:kevlar", 650);
        CATEGORIES.put("tacz:kevlar", "utility");
        PRICES.put("tacz:helmet", 1000);
        CATEGORIES.put("tacz:helmet", "utility");
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
