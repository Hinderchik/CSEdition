package com.csedition.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для замены фона экрана загрузки мира/сервера (LevelLoadingScreen).
 *
 * Это НЕ Forge LoadingOverlay (ранний загрузочный экран) — это игровой экран,
 * который показывается при входе в мир или подключении к серверу.
 * Трогать раннюю загрузку запрещено, а этот экран — обычный игровой Screen.
 *
 * Использует текстуру assets/csedition/textures/gui/loading_background.png
 * (положите свой start.jpg/png туда).
 */
@Mixin(LevelLoadingScreen.class)
public class MixinLevelLoadingScreen {

    private static final ResourceLocation CUSTOM_BG =
            new ResourceLocation("csedition", "textures/gui/loading_background.png");

    /**
     * Полностью заменяет стандартный фон на нашу текстуру.
     * Вызывается в начале render() — оригинальный метод отменяется.
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void csEdition$drawCustomBackground(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, CUSTOM_BG);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(0.0, height, 0.0).uv(0.0F, 1.0F).endVertex();
        buffer.vertex(width, height, 0.0).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(width, 0.0, 0.0).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(0.0, 0.0, 0.0).uv(0.0F, 0.0F).endVertex();
        BufferUploader.drawWithShader(buffer.end());

        // Отменяем оригинальный метод — наш фон уже нарисован
        ci.cancel();
    }
}
