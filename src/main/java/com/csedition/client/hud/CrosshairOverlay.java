package com.csedition.client.hud;

import com.csedition.config.CrosshairConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Оверлей прицела CS2-стиля.
 * Регистрация ленивая через getInstance() в clientSetup через enqueueWork.
 * Отменяет ванильный крестик (RenderGuiOverlayEvent.Pre на minecraft:crosshair)
 * и рисует свой в RenderGuiEvent.Post.
 */
@OnlyIn(Dist.CLIENT)
public class CrosshairOverlay {

    // Ленивый синглтон — не создаётся до первого вызова getInstance()
    private static CrosshairOverlay instance = null;

    private static CrosshairConfig config = null;
    private final CrosshairController controller = new CrosshairController();

    private boolean lastSwing = false;

    private CrosshairOverlay() {
        // Конструктор НЕ регистрируется в шине — это делается в clientSetup
    }

    public static CrosshairOverlay getInstance() {
        if (instance == null) {
            instance = new CrosshairOverlay();
        }
        return instance;
    }

    public static CrosshairConfig getConfig() { return config; }
    public static void setConfig(CrosshairConfig cfg) { config = cfg; }

    // ====================== Отмена ванильного крестика ======================

    @SubscribeEvent
    public void onVanillaCrosshairPre(RenderGuiOverlayEvent.Pre event) {
        if (!"minecraft:crosshair".equals(event.getOverlay().id().toString())) return;
        event.setCanceled(true);
    }

    // ====================== Рендер своего прицела ======================

    @SubscribeEvent
    public void onRenderPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        CrosshairConfig cfg = config;
        if (cfg == null) return;

        // Проверки условий скрытия
        if (mc.options.hideGui && cfg.hideWithHud) return;
        if (p == null || mc.level == null) return;
        if (p.isSpectator() && cfg.hideInSpectator) return;
        if (cfg.hideOnSniperAds && isSniperAds(mc, p)) return;

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int cx = w / 2;
        int cy = h / 2;

        float spread;
        if (cfg.dynamic) {
            spread = controller.getCurrentSpread();
        } else {
            spread = cfg.baseSpread;
        }

        GuiGraphics g = event.getGuiGraphics();
        g.pose().pushPose();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        CrosshairRenderer.draw(g, cx, cy, cfg, spread);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        g.pose().popPose();

        // Детект выстрела
        detectShot(p);
    }

    /** Быстрое определение зума снайперки через TACЗ. */
    private static boolean isSniperAds(Minecraft mc, Player p) {
        if (!p.isUsingItem()) return false;
        // TACZ uses specific gun items that have remaining use time when ADS
        try {
            return p.getUseItemRemainingTicks() > 10;
        } catch (Exception ignored) {}
        return false;
    }

    private void detectShot(Player p) {
        boolean swinging = p.swinging;
        if (swinging && !lastSwing && p.swingTime == 1) {
            controller.onShotFired();
        }
    }
}