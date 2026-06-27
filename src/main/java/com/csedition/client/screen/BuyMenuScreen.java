package com.csedition.client.screen;

import com.csedition.client.ClientState;
import com.csedition.client.render.CSRenderUtil;
import com.csedition.match.GunPriceTable;
import com.csedition.network.CSPackets;
import com.csedition.network.PacketBuyGun;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GUI меню закупа. Открывается по B в фазе BUY_TIME.
 * Стиль CS: тёмные панели, оранжевые акценты, угловые элементы.
 * Оружие разделено по категориям (Pistols, SMGs, Rifles, Snipers, Heavy, Utility).
 * Всё процедурно — никаких PNG.
 */
public class BuyMenuScreen extends Screen {
    private static final int SLOT = 40;
    private static final int PADDING = 8;
    private static final int CATS_PER_ROW = 6;

    // Категории для отображения
    private static final String[] CATEGORIES = {"pistol", "smg", "rifle", "sniper", "heavy", "utility"};
    private static final String[] CATEGORY_NAMES = {"ПИСТОЛЕТЫ", "ПП", "АВТОМАТЫ", "СНАЙПЕРКИ", "ТЯЖЁЛОЕ", "СНАРЯЖЕНИЕ"};

    public BuyMenuScreen() {
        super(Component.literal("Меню закупа"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Тёмный фон со сканлайнами
        g.fill(0, 0, this.width, this.height, 0xDD050505);
        CSRenderUtil.scanlines(g, 0, 0, this.width, this.height, 3);

        // Заголовок в панели
        String title = "МЕНЮ ЗАКУПА  $" + ClientState.getMoney();
        int titleW = this.font.width(title) + 24;
        CSRenderUtil.csPanel(g, this.width / 2 - titleW / 2, 8, titleW, 26, null, this.font);
        CSRenderUtil.cornerAccents(g, this.width / 2 - titleW / 2, 8, titleW, 26, 6, CSRenderUtil.CS_ORANGE);
        g.drawCenteredString(this.font, title, this.width / 2, 16, CSRenderUtil.CS_ORANGE);

        // Рисуем категории
        int catY = 50;
        for (int catIdx = 0; catIdx < CATEGORIES.length; catIdx++) {
            String cat = CATEGORIES[catIdx];
            String catName = CATEGORY_NAMES[catIdx];

            // Заголовок категории
            int nameW = this.font.width(catName) + 12;
            CSRenderUtil.csPanel(g, 12, catY, nameW, 16, null, this.font);
            g.drawString(this.font, catName, 18, catY + 4, CSRenderUtil.CS_YELLOW);

            // Слоты оружия в этой категории
            List<Map.Entry<String, Integer>> items = getItemsInCategory(cat);
            int slotX = 12;
            int slotY = catY + 22;

            for (int i = 0; i < items.size(); i++) {
                Map.Entry<String, Integer> entry = items.get(i);
                String gunId = entry.getKey();
                int price = entry.getValue();
                boolean canAfford = ClientState.getMoney() >= price;
                boolean hovered = mouseX >= slotX && mouseX <= slotX + SLOT
                        && mouseY >= slotY && mouseY <= slotY + SLOT;

                // Кнопка-слот
                CSRenderUtil.csButton(g, slotX, slotY, SLOT, SLOT, "", hovered, this.font);

                // Иконка предмета (центрирована)
                ItemStack stack = getItemStack(gunId);
                if (!stack.isEmpty()) {
                    int iconX = slotX + (SLOT - 16) / 2;
                    int iconY = slotY + 4;
                    g.renderItem(stack, iconX, iconY);
                }

                // Цена внизу
                String priceStr = "$" + price;
                int priceColor = canAfford ? CSRenderUtil.CS_GREEN : CSRenderUtil.CS_RED;
                int priceW = this.font.width(priceStr);
                g.drawString(this.font, priceStr, slotX + (SLOT - priceW) / 2, slotY + SLOT - 10, priceColor);

                slotX += SLOT + PADDING;
            }

            catY += SLOT + 30;
        }

        // Подсказка
        g.drawCenteredString(this.font, "ESC — закрыть  |  Z=последнее  X=автомат  C=пистолет  4=снаряжение",
                this.width / 2, this.height - 16, 0xFF888888);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int catY = 50;
        for (int catIdx = 0; catIdx < CATEGORIES.length; catIdx++) {
            String cat = CATEGORIES[catIdx];
            List<Map.Entry<String, Integer>> items = getItemsInCategory(cat);
            int slotX = 12;
            int slotY = catY + 22;

            for (int i = 0; i < items.size(); i++) {
                Map.Entry<String, Integer> entry = items.get(i);
                if (mouseX >= slotX && mouseX <= slotX + SLOT
                        && mouseY >= slotY && mouseY <= slotY + SLOT) {
                    String gunId = entry.getKey();
                    int price = entry.getValue();
                    if (ClientState.getMoney() >= price) {
                        CSPackets.CHANNEL.sendToServer(new PacketBuyGun(gunId));
                        this.onClose();
                    }
                    return true;
                }
                slotX += SLOT + PADDING;
            }
            catY += SLOT + 30;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private List<Map.Entry<String, Integer>> getItemsInCategory(String category) {
        List<Map.Entry<String, Integer>> result = new ArrayList<>();
        for (Map.Entry<String, Integer> e : GunPriceTable.getAll().entrySet()) {
            if (category.equals(GunPriceTable.getCategory(e.getKey()))) {
                result.add(e);
            }
        }
        return result;
    }

    private ItemStack getItemStack(String gunId) {
        try {
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(gunId));
            if (item != null) return new ItemStack(item);
        } catch (Exception ignored) {}
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
