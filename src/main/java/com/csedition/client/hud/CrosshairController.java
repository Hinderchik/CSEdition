package com.csedition.client.hud;

import com.csedition.config.CrosshairConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Контроллер динамического прицела.
 * Работает в ClientTickEvent.Phase.END, обновляет currentSpread.
 * Для статического режима — ранний return, никаких вычислений по движению.
 */
public class CrosshairController {

    private float currentSpread = 0f;
    private float shotImpulse = 0f;
    private boolean lastSwing = false;

    public CrosshairController() {}

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null) return;

        CrosshairConfig cfg = CrosshairOverlay.getConfig();
        if (cfg == null) return;

        // Статический режим — ранний return, всегда baseSpread
        if (!cfg.dynamic) {
            currentSpread = cfg.baseSpread;
            shotImpulse = 0f;
            return;
        }

        // Динамический режим
        float target = cfg.baseSpread;

        // Движение
        if (p.getDeltaMovement().horizontalDistanceSqr() > 0.0015) {
            target += cfg.moveSpread;
        }
        if (p.isSprinting()) {
            target += cfg.sprintSpread;
        }
        if (!p.onGround()) {
            target += cfg.airSpread;
        }

        // Импульс выстрела затухает
        shotImpulse *= (1f - cfg.shotRecovery);
        if (shotImpulse < 0.01f) shotImpulse = 0f;
        target += shotImpulse;

        // Lerp к цели
        currentSpread += (target - currentSpread) * cfg.lerpSpeed;
        if (currentSpread < 0f) currentSpread = 0f;
    }

    /** Вызывается из CrosshairOverlay при обнаружении выстрела. */
    public void onShotFired() {
        CrosshairConfig cfg = CrosshairOverlay.getConfig();
        if (cfg == null || !cfg.dynamic) return;
        shotImpulse += cfg.shotSpread;
    }

    /** Получить текущий spread (для превью). */
    public float getCurrentSpread() {
        return currentSpread;
    }
}
