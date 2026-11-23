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

import java.util.function.Supplier;

/**
 * Миксин для замены фона экрана загрузки мира/сервера (LevelLoadingScreen).
 * ResourceLocation ленивый — никакой инициализации на стадии sided_setup.
 */
@Mixin(value = LevelLoadingScreen.class, remap = false)
public class MixinLevelLoadingScreen {

    // Ленивый ResourceLocation — создаётся только при первом обращении
    private static final java.util.function.Supplier<ResourceLocation> CUSTOM_BG =
            () -> new ResourceLocation("csedition", "textures/gui/loading_background.png");

    /**
     * Полностью заменяет стандартный фон на нашу текстуру.
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void csEdition$drawCustomBackground(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, CUSTOM_BG.get());

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(0.0, height, 0.0).uv(0.0F, 1.0F).endVertex();
        buffer.vertex(width, height, 0.0).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(width, 0.0, 0.0).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(0.0, 0.0, 0.0).uv(0.0F, 0.0F).endVertex();
        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        ci.cancel();
    }
}