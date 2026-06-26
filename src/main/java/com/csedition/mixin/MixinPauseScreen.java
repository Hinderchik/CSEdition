package com.csedition.mixin;

import com.csedition.client.render.CSRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Стилизует PauseScreen под тёмную тему CS.
 * remap = false — в Forge 1.20.1 с official mappings имена уже deobfuscated.
 * @Shadow открывает доступ к protected членам Screen.
 */
@Mixin(value = PauseScreen.class, remap = false)
public abstract class MixinPauseScreen extends Screen {

    @Shadow protected Minecraft minecraft;

    protected MixinPauseScreen(Component title) {
        super(title);
    }

    @Shadow public abstract void clearWidgets();

    @Shadow protected abstract <T extends GuiEventListener & net.minecraft.client.gui.components.Renderable & NarratableEntry> T addRenderableWidget(T widget);

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void cs$restyle(CallbackInfo ci) {
        PauseScreen self = (PauseScreen)(Object)this;
        self.clearWidgets();
        int cx = self.width / 2;
        int y = self.height / 2;
        self.addRenderableWidget(Button.builder(
                Component.literal("Back to Game"),
                b -> self.minecraft.setScreen(null))
                .bounds(cx - 120, y - 40, 240, 28).build());
        self.addRenderableWidget(Button.builder(
                Component.literal("Options"),
                b -> self.minecraft.setScreen(new OptionsScreen(self, self.minecraft.options)))
                .bounds(cx - 120, y - 8, 240, 28).build());
        self.addRenderableWidget(Button.builder(
                Component.literal("Disconnect"),
                b -> self.minecraft.level.disconnect())
                .bounds(cx - 120, y + 24, 240, 28).build());
    }

    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void cs$render(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        PauseScreen self = (PauseScreen)(Object)this;
        g.fill(0, 0, self.width, self.height, 0xEE050505);
        CSRenderUtil.scanlines(g, 0, 0, self.width, self.height, 3);
        String title = "PAUSED";
        var font = Minecraft.getInstance().font;
        int titleW = font.width(title) + 40;
        int titleX = self.width / 2 - titleW / 2;
        int titleY = 40;
        CSRenderUtil.csPanel(g, titleX, titleY, titleW, 40, null, font);
        CSRenderUtil.cornerAccents(g, titleX, titleY, titleW, 40, 8, CSRenderUtil.CS_ORANGE);
        g.drawCenteredString(font, title, self.width / 2, titleY + 16, CSRenderUtil.CS_ORANGE);
    }
}
