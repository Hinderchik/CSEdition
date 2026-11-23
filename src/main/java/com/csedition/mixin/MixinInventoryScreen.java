package com.csedition.mixin;

import com.csedition.client.render.CSRenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Стилизует InventoryScreen под шутер.
 * НЕ отменяем renderBg — рисуем заголовок поверх через render @TAIL.
 */
@Mixin(value = InventoryScreen.class, remap = false)
public class MixinInventoryScreen {

    @Inject(method = "render", at = @At("TAIL"))
    private void cs$overlay(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        InventoryScreen self = (InventoryScreen)(Object)this;
        var font = net.minecraft.client.Minecraft.getInstance().font;
        String title = "INVENTORY";
        int titleW = font.width(title) + 40;
        CSRenderUtil.csPanel(g, self.width / 2 - titleW / 2, 8, titleW, 24, null, font);
        CSRenderUtil.cornerAccents(g, self.width / 2 - titleW / 2, 8, titleW, 24, 6, CSRenderUtil.CS_ORANGE);
        g.drawCenteredString(font, title, self.width / 2, 16, CSRenderUtil.CS_ORANGE);
    }
}
