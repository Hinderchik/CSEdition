package com.csedition.match;

import java.util.HashMap;
import java.util.Map;

/**
 * Таблица цен на оружие TaCZ.
 * Используется MatchManager при обработке покупки.
 * Цены приближены к CS:GO.
 */
public final class GunPriceTable {
    private static final Map<String, Integer> PRICES = new HashMap<>();

    static {
        // Пистолеты
        PRICES.put("tacz:glock_17", 200);
        PRICES.put("tacz:usp_45", 200);
        PRICES.put("tacz:p250", 300);
        PRICES.put("tacz:desert_eagle", 700);
        // SMG
        PRICES.put("tacz:mp5", 1500);
        PRICES.put("tacz:p90", 2350);
        PRICES.put("tacz:ump_45", 1200);
        // Винтовки
        PRICES.put("tacz:ak47", 2700);
        PRICES.put("tacz:m4a4", 3100);
        PRICES.put("tacz:awp", 4750);
        PRICES.put("tacz:aug", 3300);
        PRICES.put("tacz:sg556", 3000);
        // Тяжёлые
        PRICES.put("tacz:nova", 1050);
        PRICES.put("tacz:xm1014", 2000);
        PRICES.put("tacz:m249", 5200);
        // Снайперские
        PRICES.put("tacz:scout", 1700);
        // Броня
        PRICES.put("tacz:kevlar", 650);
        PRICES.put("tacz:helmet", 1000);
    }

    private GunPriceTable() {}

    public static int getPrice(String gunId) {
        return PRICES.getOrDefault(gunId, -1);
    }

    public static Map<String, Integer> getAll() { return PRICES; }
}
