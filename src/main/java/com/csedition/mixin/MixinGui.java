package com.csedition.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Отключает стандартный рендер HUD (сердечки, голод, воздух, броня).
 * Свой HUD рисуется в CSHudOverlay.
 */
@Mixin(Gui.class)
public class MixinGui {

    @Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
    private void cs$cancelHearts(GuiGraphics g, Player p, int x, int y, int z, boolean blink, float partial, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void cs$cancelFood(GuiGraphics g, Player p, int x, int y, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "renderAir", at = @At("HEAD"), cancellable = true)
    private void cs$cancelAir(GuiGraphics g, Player p, int x, int y, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private void cs$cancelArmor(GuiGraphics g, Player p, int x, int y, int z, CallbackInfo ci) {
        ci.cancel();
    }
}
