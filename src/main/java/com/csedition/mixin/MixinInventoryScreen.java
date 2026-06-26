package com.csedition.mixin;

import com.csedition.client.render.CSRenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Стилизует InventoryScreen под шутер.
 * Заменяет стандартный фон на тёмный, рисует слоты в стиле CS.
 * Иконки предметов берутся из реестра (включая TaCZ).
 * Всё процедурно через CSRenderUtil — никаких PNG.
 */
@Mixin(InventoryScreen.class)
public class MixinInventoryScreen {

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void cs$renderBg(GuiGraphics g, float partial, int mouseX, int mouseY, CallbackInfo ci) {
        ci.cancel();
        InventoryScreen self = (InventoryScreen)(Object)this;
        // Тёмный фон со сканлайнами
        g.fill(0, 0, self.width, self.height, 0xEE0A0A0A);
        CSRenderUtil.scanlines(g, 0, 0, self.width, self.height, 3);

        // Заголовок
        var font = net.minecraft.client.Minecraft.getInstance().font;
        String title = "INVENTORY";
        int titleW = font.width(title) + 40;
        CSRenderUtil.csPanel(g, self.width / 2 - titleW / 2, 8, titleW, 24, null, font);
        CSRenderUtil.cornerAccents(g, self.width / 2 - titleW / 2, 8, titleW, 24, 6, CSRenderUtil.CS_ORANGE);
        g.drawCenteredString(font, title, self.width / 2, 16, CSRenderUtil.CS_ORANGE);

        // Слоты инвентаря игрока (3 ряда по 9)
        int startX = self.width / 2 - 176 / 2;
        int startY = self.height / 2 - 50;
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null) {
            // Панель вокруг инвентаря
            CSRenderUtil.csPanel(g, startX - 6, startY - 6, 9 * 18 + 12, 3 * 18 + 12, null, font);
            CSRenderUtil.cornerAccents(g, startX - 6, startY - 6, 9 * 18 + 12, 3 * 18 + 12, 6, CSRenderUtil.CS_ORANGE);

            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    int x = startX + col * 18;
                    int y = startY + row * 18;
                    g.fill(x, y, x + 16, y + 16, 0xFF1A1A1A);
                    g.renderOutline(x, y, 16, 16, CSRenderUtil.CS_BORDER);
                    ItemStack stack = player.getInventory().getItem(9 + row * 9 + col);
                    if (!stack.isEmpty()) {
                        g.renderItem(stack, x, y);
                    }
                }
            }
            // Хотбар
            int hotbarY = startY + 3 * 18 + 8;
            CSRenderUtil.csPanel(g, startX - 6, hotbarY - 6, 9 * 18 + 12, 18 + 12, null, font);
            CSRenderUtil.cornerAccents(g, startX - 6, hotbarY - 6, 9 * 18 + 12, 18 + 12, 6, CSRenderUtil.CS_ORANGE);
            for (int col = 0; col < 9; col++) {
                int x = startX + col * 18;
                g.fill(x, hotbarY, x + 16, hotbarY + 16, 0xFF1A1A1A);
                g.renderOutline(x, hotbarY, 16, 16, CSRenderUtil.CS_BORDER);
                ItemStack stack = player.getInventory().getItem(col);
                if (!stack.isEmpty()) {
                    g.renderItem(stack, x, hotbarY);
                }
            }
        }
    }
}
