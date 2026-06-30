package com.csedition.match;

import com.csedition.config.WeaponConfig;

import java.util.Map;

/**
 * Таблица цен на оружие.
 * Делегирует все запросы в WeaponConfig — там же хранятся magazine/fireMode/attachments.
 *
 * Категории:
 *   - pistol: пистолеты
 *   - smg: пистолеты-пулемёты
 *   - rifle: винтовки
 *   - sniper: снайперские
 *   - shotgun: дробовики
 *   - heavy: тяжёлые (LMG)
 *   - utility: снаряжение (броня, шлем)
 */
public final class GunPriceTable {
    private GunPriceTable() {}

    public static int getPrice(String gunId) {
        return WeaponConfig.getPrice(gunId);
    }

    public static String getCategory(String gunId) {
        return WeaponConfig.getCategory(gunId);
    }

    public static Map<String, Integer> getAll() {
        return WeaponConfig.getAllPrices();
    }

    /**
     * Самая дешёвая пушка в категории (для быстрой закупки).
     */
    public static String getCheapestOfCategory(String category) {
        return WeaponConfig.getCheapestOfCategory(category);
    }
}