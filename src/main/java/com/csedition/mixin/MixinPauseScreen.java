package com.csedition.mixin;

import com.csedition.client.render.CSRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Стилизует PauseScreen под тёмную тему CS.
 * Рендер фона — в renderBackground через событие, не отменяем стандартный рендер кнопок.
 */
@Mixin(value = PauseScreen.class, remap = false)
public abstract class MixinPauseScreen extends net.minecraft.client.gui.screens.Screen {

    protected MixinPauseScreen(Component title) {
        super(title);
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void cs$renderBackground(GuiGraphics g, CallbackInfo ci) {
        // Отменяем только фоновую текстуру/градиент, сохраняя кнопки
        g.fill(0, 0, this.width, this.height, 0xEE050505);
        CSRenderUtil.scanlines(g, 0, 0, this.width, this.height, 3);
        ci.cancel();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void cs$overlay(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        String title = "PAUSED";
        var font = Minecraft.getInstance().font;
        int titleW = font.width(title) + 40;
        int titleX = this.width / 2 - titleW / 2;
        int titleY = 40;
        CSRenderUtil.csPanel(g, titleX, titleY, titleW, 40, null, font);
        CSRenderUtil.cornerAccents(g, titleX, titleY, titleW, 40, 8, CSRenderUtil.CS_ORANGE);
        g.drawCenteredString(font, title, this.width / 2, titleY + 16, CSRenderUtil.CS_ORANGE);
    }
}
