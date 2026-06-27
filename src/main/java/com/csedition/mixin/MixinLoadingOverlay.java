package com.csedition.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.client.gui.screen.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для замены стандартного фона Forge LoadingOverlay на кастомную фотку.
 *
 * Использует текстуру assets/csedition/textures/gui/loading_background.png
 * (положите свой start.jpg/png туда).
 *
 * Рисует полноэкранный фон через Tesselator, затем отменяет оригинальный drawBackground().
 */
@Mixin(LoadingOverlay.class)
public class MixinLoadingOverlay {

    private static final ResourceLocation CUSTOM_BG =
            new ResourceLocation("csedition", "textures/gui/loading_background.png");

    @Shadow
    private Minecraft minecraft;

    /**
     * Полностью заменяет стандартный фон на нашу текстуру.
     * Вызывается в начале drawBackground() — оригинальный метод отменяется.
     */
    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true)
    private void csEdition$drawCustomBackground(float partialTicks, CallbackInfo ci) {
        if (this.minecraft == null) return;

        int width = this.minecraft.getWindow().getGuiScaledWidth();
        int height = this.minecraft.getWindow().getGuiScaledHeight();

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
