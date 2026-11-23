package com.csedition.client.hud;

import com.csedition.config.CrosshairConfig;

/**
 * Утилиты для анимированного превью динамического прицела.
 */
public final class CrosshairPreviewUtil {
    private CrosshairPreviewUtil() {}

    /**
     * Циклическая демонстрация прицела: покой → движение → выстрел → затухание.
     * Без аллокаций, только примитивы.
     */
    public static float getDemoSpread(CrosshairConfig cfg) {
        long ms = System.currentTimeMillis() % 6000L;
        float demo;
        if (ms < 2000L) {
            // покой
            demo = cfg.baseSpread;
        } else if (ms < 4000L) {
            // движение
            demo = cfg.baseSpread + cfg.moveSpread;
        } else {
            // выстрел + затухание
            float decay = 1.0f - (float)(ms - 4000L) / 2000.0f;
            demo = cfg.baseSpread + cfg.moveSpread + cfg.shotSpread * Math.max(0.0f, decay);
        }
        return demo;
    }
}
