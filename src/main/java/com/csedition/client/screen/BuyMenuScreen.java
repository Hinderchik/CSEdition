package com.csedition.client.screen;

import com.csedition.client.ClientState;
import com.csedition.client.render.CSRenderUtil;
import com.csedition.match.GunPriceTable;
import com.csedition.network.CSPackets;
import com.csedition.network.PacketBuyGun;
import com.csedition.tacz.TaczHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI меню закупа. Открывается по B в фазе BUY_TIME.
 * Оружие рендерится из кэша — ItemStack создаётся один раз при инициализации.
 */
public class BuyMenuScreen extends Screen {
    private static final int SLOT = 40;
    private static final int PADDING = 8;
    private static final int CATS_PER_ROW = 6;

    // Категории для отображения
    private static final String[] CATEGORIES = {"pistol", "smg", "rifle", "sniper", "heavy", "utility"};
    private static final String[] CATEGORY_NAMES = {"ПИСТОЛЕТЫ", "ПП", "АВТОМАТЫ", "СНАЙПЕРКИ", "ТЯЖЁЛОЕ", "СНАРЯЖЕНИЕ"};

    // Скролл
    private int scrollOffset = 0;
    private int maxScroll = 0;

    // Кэш предметных стеков: gunId -> ItemStack
    private final Map<String, ItemStack> itemStackCache = new LinkedHashMap<>();

    public BuyMenuScreen() {
        super(Component.literal("Меню закупа"));
    }

    @Override
    public void init() {
        super.init();
        buildItemCache();
        computeMaxScroll();
    }

    private void buildItemCache() {
        itemStackCache.clear();
        for (Map.Entry<String, Integer> e : GunPriceTable.getAll().entrySet()) {
            String gunId = e.getKey();
            if (!itemStackCache.containsKey(gunId)) {
                ItemStack stack = TaczHelper.createGun(gunId);
                itemStackCache.put(gunId, stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
        }
    }

    private void computeMaxScroll() {
        int totalH = CATEGORIES.length * (SLOT + 30);
        int availableH = this.height - 80;
        maxScroll = Math.max(0, totalH - availableH);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Тёмный фон со сканлайнами
        g.fill(0, 0, this.width, this.height, 0xDD050505);
        CSRenderUtil.scanlines(g, 0, 0, this.width, this.height, 3);

        // Заголовок в панели
        String title = "МЕНЮ ЗАКУПА  $" + ClientState.getMoney();
        Font font = this.font;
        int titleW = font.width(title) + 24;
        CSRenderUtil.csPanel(g, this.width / 2 - titleW / 2, 8, titleW, 26, null, font);
        CSRenderUtil.cornerAccents(g, this.width / 2 - titleW / 2, 8, titleW, 26, 6, CSRenderUtil.CS_ORANGE);
        g.drawCenteredString(font, title, this.width / 2, 16, CSRenderUtil.CS_ORANGE);

        // Рисуем категории
        int catY = 50 - scrollOffset;
        for (int catIdx = 0; catIdx < CATEGORIES.length; catIdx++) {
            String cat = CATEGORIES[catIdx];
            String catName = CATEGORY_NAMES[catIdx];

            // Заголовок категории
            int nameW = font.width(catName) + 12;
            CSRenderUtil.csPanel(g, 12, catY, nameW, 16, null, font);
            g.drawString(font, catName, 18, catY + 4, CSRenderUtil.CS_YELLOW);

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
                CSRenderUtil.csButton(g, slotX, slotY, SLOT, SLOT, "", hovered, font);

                // Иконка предмета из кэша
                ItemStack stack = itemStackCache.getOrDefault(gunId, ItemStack.EMPTY);
                if (!stack.isEmpty()) {
                    int iconX = slotX + (SLOT - 16) / 2;
                    int iconY = slotY + 4;
                    g.renderItem(stack, iconX, iconY);
                }

                // Цена внизу
                String priceStr = "$" + price;
                int priceColor = canAfford ? CSRenderUtil.CS_GREEN : CSRenderUtil.CS_RED;
                int priceW = font.width(priceStr);
                g.drawString(font, priceStr, slotX + (SLOT - priceW) / 2, slotY + SLOT - 10, priceColor);

                slotX += SLOT + PADDING;
            }

            catY += SLOT + 30;
        }

        // Подсказка
        g.drawCenteredString(font, "ESC — закрыть  |  Z=последнее  X=автомат  C=пистолет  4=снаряжение",
                this.width / 2, this.height - 16, 0xFF888888);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int catY = 50 - scrollOffset;
        for (String cat : CATEGORIES) {
            List<Map.Entry<String, Integer>> items = getItemsInCategory(cat);
            int slotX = 12;
            int slotY = catY + 22;

            for (Map.Entry<String, Integer> entry : items) {
                String gunId = entry.getKey();
                int price = entry.getValue();
                if (mouseX >= slotX && mouseX <= slotX + SLOT
                        && mouseY >= slotY && mouseY <= slotY + SLOT) {
                    if (ClientState.getMoney() >= price) {
                        CSPackets.CHANNEL.sendToServer(new PacketBuyGun(gunId));
                        this.onClose();
                    } else {
                        this.minecraft.player.displayClientMessage(
                                Component.literal("\u00A7c\u00A7lНедостаточно денег: $\u00A7r" + price), true);
                    }
                    return true;
                }
                slotX += SLOT + PADDING;
            }
            catY += SLOT + 30;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int step = (SLOT + 30) / 2;
        int old = scrollOffset;
        if (delta > 0) scrollOffset = Math.max(0, scrollOffset - step);
        else if (delta < 0) scrollOffset = Math.min(maxScroll, scrollOffset + step);
        return old != scrollOffset;
    }

    @Override
    public void resize(net.minecraft.client.Minecraft mc, int width, int height) {
        computeMaxScroll();
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

    @Override
    public boolean isPauseScreen() { return false; }
}
