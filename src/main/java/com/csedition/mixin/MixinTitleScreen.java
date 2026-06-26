package com.csedition.mixin;

import com.csedition.client.render.CSRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Стилизует TitleScreen под тёмную тему CS.
 * remap = false — в Forge 1.20.1 с official mappings имена уже deobfuscated.
 * @Shadow открывает доступ к protected членам Screen.
 */
@Mixin(value = TitleScreen.class, remap = false)
public abstract class MixinTitleScreen extends Screen {

    @Shadow protected Minecraft minecraft;

    protected MixinTitleScreen(Component title) {
        super(title);
    }

    @Shadow public abstract void clearWidgets();

    @Shadow protected abstract <T extends GuiEventListener & net.minecraft.client.gui.components.Renderable & NarratableEntry> T addRenderableWidget(T widget);

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void cs$restyle(CallbackInfo ci) {
        TitleScreen self = (TitleScreen)(Object)this;
        self.clearWidgets();
        int cx = self.width / 2;
        int y = self.height / 2;
        self.addRenderableWidget(Button.builder(
                Component.literal("Singleplayer"),
                b -> self.minecraft.setScreen(new SelectWorldScreen(self)))
                .bounds(cx - 120, y - 40, 240, 28).build());
        self.addRenderableWidget(Button.builder(
                Component.literal("Options"),
                b -> self.minecraft.setScreen(new OptionsScreen(self, self.minecraft.options)))
                .bounds(cx - 120, y - 8, 240, 28).build());
        self.addRenderableWidget(Button.builder(
                Component.literal("Quit Game"),
                b -> self.minecraft.stop())
                .bounds(cx - 120, y + 24, 240, 28).build());
    }

    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void cs$render(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        TitleScreen self = (TitleScreen)(Object)this;
        g.fill(0, 0, self.width, self.height, 0xEE050505);
        CSRenderUtil.scanlines(g, 0, 0, self.width, self.height, 3);
        String title = "CS EDITION";
        var font = Minecraft.getInstance().font;
        int titleW = font.width(title) + 40;
        int titleX = self.width / 2 - titleW / 2;
        int titleY = 40;
        CSRenderUtil.csPanel(g, titleX, titleY, titleW, 50, null, font);
        CSRenderUtil.cornerAccents(g, titleX, titleY, titleW, 50, 10, CSRenderUtil.CS_ORANGE);
        CSRenderUtil.glow(g, self.width / 2, titleY + 25, 120, CSRenderUtil.withAlpha(CSRenderUtil.CS_ORANGE, 60));
        g.drawCenteredString(font, title, self.width / 2, titleY + 20, CSRenderUtil.CS_ORANGE);
    }
}
