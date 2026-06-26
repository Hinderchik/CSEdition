package com.csedition.mixin;

import com.csedition.client.render.CSRenderUtil;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Стилизует TitleScreen под тёмную тему CS.
 * remap = false — в Forge 1.20.1 с official mappings имена уже deobfuscated.
 */
@Mixin(value = TitleScreen.class, remap = false)
public class MixinTitleScreen {

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void cs$restyle(CallbackInfo ci) {
        TitleScreen self = (TitleScreen)(Object)this;
        self.clearWidgets();
        int cx = self.width / 2;
        int y = self.height / 2;
        self.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                net.minecraft.network.chat.Component.literal("Singleplayer"),
                b -> self.minecraft.setScreen(new net.minecraft.client.gui.screens.worldselection.SelectWorldScreen(self)))
                .bounds(cx - 120, y - 40, 240, 28).build());
        self.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                net.minecraft.network.chat.Component.literal("Options"),
                b -> self.minecraft.setScreen(new net.minecraft.client.gui.screens.OptionsScreen(self, self.minecraft.options)))
                .bounds(cx - 120, y - 8, 240, 28).build());
        self.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                net.minecraft.network.chat.Component.literal("Quit Game"),
                b -> self.minecraft.stop())
                .bounds(cx - 120, y + 24, 240, 28).build());
    }

    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void cs$render(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        TitleScreen self = (TitleScreen)(Object)this;
        g.fill(0, 0, self.width, self.height, 0xEE050505);
        CSRenderUtil.scanlines(g, 0, 0, self.width, self.height, 3);
        String title = "CS EDITION";
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int titleW = font.width(title) + 40;
        int titleX = self.width / 2 - titleW / 2;
        int titleY = 40;
        CSRenderUtil.csPanel(g, titleX, titleY, titleW, 50, null, font);
        CSRenderUtil.cornerAccents(g, titleX, titleY, titleW, 50, 10, CSRenderUtil.CS_ORANGE);
        CSRenderUtil.glow(g, self.width / 2, titleY + 25, 120, CSRenderUtil.withAlpha(CSRenderUtil.CS_ORANGE, 60));
        g.drawCenteredString(font, title, self.width / 2, titleY + 20, CSRenderUtil.CS_ORANGE);
    }
}
