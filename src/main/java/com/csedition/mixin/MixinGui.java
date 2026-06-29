package com.csedition.mixin;

import com.csedition.client.hud.CSHudOverlay;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels vanilla HUD elements (hearts, food, air, armor) and injects
 * our custom HUD render at the END of Gui.render — guaranteeing it
 * fires every frame regardless of overlay system behavior.
 *
 * remap = false — Forge 1.20.1 with official mappings has deobfuscated names.
 */
@Mixin(value = Gui.class, remap = false)
public class MixinGui {

    @Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true, remap = false)
    private void cs$cancelHearts(GuiGraphics g, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true, remap = false)
    private void cs$cancelFood(GuiGraphics g, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "renderAir", at = @At("HEAD"), cancellable = true, remap = false)
    private void cs$cancelAir(GuiGraphics g, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true, remap = false)
    private void cs$cancelArmor(GuiGraphics g, CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * Inject at TAIL of Gui.render — this is called every frame
     * during the GUI render phase. Our CSHudOverlay.render() draws
     * HP/AP bars, money, phase timer, and the custom hotbar.
     *
     * The frame counter inside render() prevents double-drawing
     * if the custom overlay also fires.
     */
    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void cs$drawHud(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        CSHudOverlay.drawHud(guiGraphics);
    }
}
