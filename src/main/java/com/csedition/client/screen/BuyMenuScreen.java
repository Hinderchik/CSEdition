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
import java.util.List;
import java.util.Map;

/**
 * GUI меню закупа. Открывается по B в фазе BUY_TIME.
 * Стиль CS: тёмные панели, оранжевые акценты, угловые элементы.
 * Оружие разделено по категориям (Pistols, SMGs, Rifles, Snipers, Heavy, Utility).
 * Всё процедурно — никаких PNG.
 *
 * Иконки оружия берутся через TaczHelper.createGun() — это гарантирует что
 * предмет рендерится с правильной текстурой (TaCZ определяет оружие по NBT GunId).
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

    public BuyMenuScreen() {
        super(Component.literal("Меню закупа"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Для отрисовки тултипов в конце
        String hoveredTooltip = null;
        int hoveredTx = 0, hoveredTy = 0;
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

        // Сколько предметов в каждой категории — для расчёта ширины строки
        int maxRowWidth = 0;
        for (String cat : CATEGORIES) {
            int n = getItemsInCategory(cat).size();
            int w = n * (SLOT + PADDING);
            if (w > maxRowWidth) maxRowWidth = w;
        }

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

                // Иконка предмета (центрирована) — через TaczHelper, чтобы был правильный рендер TaCZ
                ItemStack stack = getItemStack(gunId);
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

                // Тултип для брони
                if (hovered) {
                    String tip = getTooltipForItem(gunId);
                    if (tip != null) {
                        hoveredTooltip = tip;
                        hoveredTx = slotX;
                        hoveredTy = slotY + SLOT + 2;
                    }
                }

                slotX += SLOT + PADDING;
            }

            catY += SLOT + 30;
        }

        // Подсказка
        g.drawCenteredString(font, "ESC — закрыть  |  Z=последнее  X=автомат  C=пистолет  4=снаряжение",
                this.width / 2, this.height - 16, 0xFF888888);

        // Тултип на наведённом слоте (рисуется последним поверх всего)
        if (hoveredTooltip != null) {
            int tw = font.width(hoveredTooltip) + 8;
            int th = 14;
            int tx = hoveredTx;
            int ty = hoveredTy;
            g.fill(tx - 2, ty - 2, tx + tw, ty + th, 0xEE000000);
            g.fill(tx - 2, ty - 2, tx + tw - 2, ty - 1, CSRenderUtil.CS_ORANGE);
            g.drawString(font, hoveredTooltip, tx + 2, ty + 2, 0xFFFFFFFF);
        }
    }

    /**
     * Тултип для конкретного предмета. Возвращает null если тултип не нужен.
     */
    private String getTooltipForItem(String gunId) {
        if ("tacz:kevlar".equals(gunId)) {
            return "Netherite Chestplate + Prot IV";
        }
        if ("tacz:helmet".equals(gunId)) {
            return "Netherite Helmet + Prot IV";
        }
        return null;
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
                        // Маленький визуальный отклик — даже без покупки
                        this.minecraft.player.displayClientMessage(
                                Component.literal("§cНедостаточно денег: $" + price), true);
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
        int totalH = CATEGORIES.length * (SLOT + 30);
        int availableH = this.height - 80;
        int maxScroll = Math.max(0, totalH - availableH);
        int step = (SLOT + 30) / 2;
        int old = scrollOffset;
        if (delta > 0) scrollOffset = Math.max(0, scrollOffset - step);
        else if (delta < 0) scrollOffset = Math.min(maxScroll, scrollOffset + step);
        return old != scrollOffset;
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

    /**
     * Создаёт ItemStack для отображения иконки в меню закупа.
     * - TaCZ оружие: TaczHelper.createGun (правильный base item + NBT GunId)
     * - Броня (tacz:kevlar / tacz:helmet): настоящая Netherite броня с Protection IV
     */
    private ItemStack getItemStack(String gunId) {
        if ("tacz:kevlar".equals(gunId)) {
            // Kevlar = Netherite Chestplate с Protection IV ($1000)
            return createArmorIcon("minecraft:netherite_chestplate");
        }
        if ("tacz:helmet".equals(gunId)) {
            // Helmet = Netherite Helmet с Protection IV ($650)
            return createArmorIcon("minecraft:netherite_helmet");
        }
        ItemStack stack = TaczHelper.createGun(gunId);
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    /**
     * Создаёт ItemStack с Netherite бронёй и Protection IV для отображения иконки.
     */
    private ItemStack createArmorIcon(String itemId) {
        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS
                .getValue(new net.minecraft.resources.ResourceLocation(itemId));
        if (item == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        var ench = net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS
                .getValue(new net.minecraft.resources.ResourceLocation("minecraft:protection"));
        if (ench != null) stack.enchant(ench, 4);
        return stack;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
