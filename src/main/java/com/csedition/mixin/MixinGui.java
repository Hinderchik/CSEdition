package com.csedition.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Отключает стандартный рендер сердечек, голода, воздуха и брони.
 */
@Mixin(value = Gui.class, remap = false)
public class MixinGui {

    @Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
    private void cs$cancelHearts(GuiGraphics g, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void cs$cancelFood(GuiGraphics g, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "renderAir", at = @At("HEAD"), cancellable = true)
    private void cs$cancelAir(GuiGraphics g, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private void cs$cancelArmor(GuiGraphics g, CallbackInfo ci) {
        ci.cancel();
    }
}
