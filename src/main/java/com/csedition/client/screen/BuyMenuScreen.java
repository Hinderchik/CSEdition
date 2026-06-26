package com.csedition.client.screen;

import com.csedition.client.ClientState;
import com.csedition.client.render.CSRenderUtil;
import com.csedition.match.GunPriceTable;
import com.csedition.network.CSPackets;
import com.csedition.network.PacketBuyGun;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * GUI меню закупа. Открывается по B в фазе BUY_TIME.
 * Стиль CS: тёмные панели, оранжевые акценты, угловые элементы.
 * Всё процедурно — никаких PNG.
 */
public class BuyMenuScreen extends Screen {
    private static final int COLS = 4;
    private static final int SLOT = 36;
    private static final int PADDING = 6;

    public BuyMenuScreen() {
        super(Component.literal("Buy Menu"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Тёмный фон со сканлайнами
        g.fill(0, 0, this.width, this.height, 0xDD050505);
        CSRenderUtil.scanlines(g, 0, 0, this.width, this.height, 3);

        // Заголовок в панели
        String title = "BUY MENU  $" + ClientState.getMoney();
        int titleW = this.font.width(title) + 20;
        CSRenderUtil.csPanel(g, this.width / 2 - titleW / 2, 8, titleW, 24, null, this.font);
        CSRenderUtil.cornerAccents(g, this.width / 2 - titleW / 2, 8, titleW, 24, 6, CSRenderUtil.CS_ORANGE);
        g.drawCenteredString(this.font, title, this.width / 2, 16, CSRenderUtil.CS_ORANGE);

        Map<String, Integer> prices = GunPriceTable.getAll();
        var entries = prices.entrySet().stream().toList();

        int gridW = COLS * (SLOT + PADDING);
        int startX = (this.width - gridW) / 2;
        int startY = 50;

        for (int i = 0; i < entries.size(); i++) {
            int row = i / COLS;
            int col = i % COLS;
            int x = startX + col * (SLOT + PADDING);
            int y = startY + row * (SLOT + PADDING);

            var entry = entries.get(i);
            String gunId = entry.getKey();
            int price = entry.getValue();
            boolean canAfford = ClientState.getMoney() >= price;
            boolean hovered = mouseX >= x && mouseX <= x + SLOT && mouseY >= y && mouseY <= y + SLOT;

            // Кнопка-слот
            CSRenderUtil.csButton(g, x, y, SLOT, SLOT, "", hovered, this.font);

            // Иконка предмета
            var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                    new net.minecraft.resources.ResourceLocation(gunId));
            if (item != null) {
                g.renderItem(new net.minecraft.world.item.ItemStack(item), x + 10, y + 6);
            }

            // Цена внизу
            String priceStr = "$" + price;
            int priceColor = canAfford ? CSRenderUtil.CS_GREEN : CSRenderUtil.CS_RED;
            g.drawString(this.font, priceStr, x + 4, y + SLOT - 10, priceColor);
        }

        // Подсказка
        g.drawCenteredString(this.font, "Press ESC to close",
                this.width / 2, this.height - 20, 0xFF888888);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Map<String, Integer> prices = GunPriceTable.getAll();
        var entries = prices.entrySet().stream().toList();
        int gridW = COLS * (SLOT + PADDING);
        int startX = (this.width - gridW) / 2;
        int startY = 50;

        for (int i = 0; i < entries.size(); i++) {
            int row = i / COLS;
            int col = i % COLS;
            int x = startX + col * (SLOT + PADDING);
            int y = startY + row * (SLOT + PADDING);
            if (mouseX >= x && mouseX <= x + SLOT && mouseY >= y && mouseY <= y + SLOT) {
                String gunId = entries.get(i).getKey();
                int price = entries.get(i).getValue();
                if (ClientState.getMoney() >= price) {
                    CSPackets.CHANNEL.sendToServer(new PacketBuyGun(gunId));
                    this.onClose();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
