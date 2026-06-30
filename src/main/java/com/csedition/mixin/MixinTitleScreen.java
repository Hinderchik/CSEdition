package com.csedition.mixin;

import com.csedition.client.render.CSRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Стилизует TitleScreen под тёмную тему CS.
 * remap = false — в Forge 1.20.1 с official mappings имена уже deobfuscated.
 * Миксин extends Screen → protected методы доступны через this.
 */
@Mixin(value = TitleScreen.class, remap = false)
public abstract class MixinTitleScreen extends Screen {

    protected MixinTitleScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void cs$restyle(CallbackInfo ci) {
        this.clearWidgets();
        Minecraft mc = Minecraft.getInstance();
        int cx = this.width / 2;
        int y = this.height / 2;
        this.addRenderableWidget(Button.builder(
                Component.literal("Одиночная игра"),
                b -> mc.setScreen(new SelectWorldScreen((TitleScreen)(Object)this)))
                .bounds(cx - 120, y - 40, 240, 28).build());
        this.addRenderableWidget(Button.builder(
                Component.literal("Настройки"),
                b -> mc.setScreen(new OptionsScreen((TitleScreen)(Object)this, mc.options)))
                .bounds(cx - 120, y - 8, 240, 28).build());
        this.addRenderableWidget(Button.builder(
                Component.literal("Выйти из игры"),
                b -> mc.stop())
                .bounds(cx - 120, y + 24, 240, 28).build());
    }

    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void cs$render(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        g.fill(0, 0, this.width, this.height, 0xEE050505);
        CSRenderUtil.scanlines(g, 0, 0, this.width, this.height, 3);
        String title = "CS EDITION";
        var font = Minecraft.getInstance().font;
        int titleW = font.width(title) + 40;
        int titleX = this.width / 2 - titleW / 2;
        int titleY = 40;
        CSRenderUtil.csPanel(g, titleX, titleY, titleW, 50, null, font);
        CSRenderUtil.cornerAccents(g, titleX, titleY, titleW, 50, 10, CSRenderUtil.CS_ORANGE);
        CSRenderUtil.glow(g, this.width / 2, titleY + 25, 120, CSRenderUtil.withAlpha(CSRenderUtil.CS_ORANGE, 60));
        g.drawCenteredString(font, title, this.width / 2, titleY + 20, CSRenderUtil.CS_ORANGE);
    }
}
