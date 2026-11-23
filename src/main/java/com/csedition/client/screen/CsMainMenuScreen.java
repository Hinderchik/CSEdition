package com.csedition.client.screen;

import com.csedition.CSEditionMod;
import com.csedition.client.ClientState;
import com.csedition.client.render.CSRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Главное меню в стиле CS2: фулл-скрин арт start.jpg, кнопки справа снизу, без логотипа.
 * Заменяет ванильный TitleScreen через ScreenEvent.Opening.
 * Все статические поля ленивы — никакой инициализации Minecraft на стадии sided_setup.
 */
@OnlyIn(Dist.CLIENT)
public class CsMainMenuScreen extends Screen {

    // Ленивый ResourceLocation — создаётся только при первом обращении
    private static final Supplier<ResourceLocation> BG = () -> new ResourceLocation("csedition", "textures/gui/start.jpg");
    // Реальные размеры start.jpg (захардкожено, чтобы не грузить NativeImage)
    private static final int TEX_W = 1920;
    private static final int TEX_H = 1080;

    // Кнопки
    private static final int BTN_W = 180;
    private static final int BTN_H = 20;
    private static final int GAP = 4;
    private static final int RIGHT = 24;
    private static final int BOTTOM = 32;

    // Cover-параметры
    private int drawW, drawH, drawX, drawY;
    private float scale;

    // Кэш строк
    private String modeMapText = "";
    private String statusText = "";

    public CsMainMenuScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        computeCover();
        buildButtons();
        updateCachedStrings();
    }

    @Override
    public void resize(Minecraft mc, int width, int height) {
        String currentModeMap = modeMapText;
        String currentStatus = statusText;
        super.resize(mc, width, height);
        computeCover();
        // Пересоздаем кнопки на новых позициях
        clearWidgets();
        buildButtons();
        // Восстанавливаем кэш строк (они не зависят от размера)
        modeMapText = currentModeMap;
        statusText = currentStatus;
    }

    private void computeCover() {
        scale = Math.max((float) width / TEX_W, (float) height / TEX_H);
        drawW = (int) (TEX_W * scale);
        drawH = (int) (TEX_H * scale);
        drawX = (width - drawW) / 2;
        drawY = (height - drawH) / 2;
    }

    private void buildButtons() {
        int x = width - RIGHT - BTN_W;
        int y = height - BOTTOM - BTN_H;

        // Порядок снизу вверх: Выход, Настройки, Настройки прицела, Выбор карты, Выбор режима, Играть
        // Создаём в обратном порядке, чтобы стек рос вверх

        // 6. Выход (снизу)
        addRenderableWidget(new CsButton(x, y, BTN_W, BTN_H, Component.literal("ВЫХОД"),
                b -> minecraft.stop(), false));
        y -= BTN_H + GAP;

        // 5. Настройки
        addRenderableWidget(new CsButton(x, y, BTN_W, BTN_H, Component.literal("НАСТРОЙКИ"),
                b -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)), false));
        y -= BTN_H + GAP;

        // 4. Настройки прицела
        addRenderableWidget(new CsButton(x, y, BTN_W, BTN_H, Component.literal("НАСТРОЙКИ ПРИЦЕЛА"),
                b -> minecraft.setScreen(new com.csedition.client.screen.CrosshairConfigScreen(true)), false));
        y -= BTN_H + GAP;

        // 3. Выбор карты
        addRenderableWidget(new CsButton(x, y, BTN_W, BTN_H, Component.literal("ВЫБОР КАРТЫ"),
                b -> minecraft.setScreen(new com.csedition.client.screen.MapSelectScreen(
                        ClientState.getSelectedModeId(), this)), false));
        y -= BTN_H + GAP;

        // 2. Выбор режима
        addRenderableWidget(new CsButton(x, y, BTN_W, BTN_H, Component.literal("ВЫБОР РЕЖИМА"),
                b -> minecraft.setScreen(new com.csedition.client.screen.ModeSelectScreen(this)), false));
        y -= BTN_H + GAP;

        // 1. Играть (акцентная, сверху)
        addRenderableWidget(new CsButton(x, y, BTN_W, BTN_H, Component.literal("ИГРАТЬ"),
                b -> minecraft.setScreen(new SelectWorldScreen(this)), true));
        // y -= BTN_H + GAP; // больше кнопок нет

        // Заголовок блока над стеком (мелкий текст)
        // Рисуется в renderBackground, здесь только позиция
    }

    private void updateCachedStrings() {
        String modeId = ClientState.getCurrentModeId();
        String mapId = ClientState.getCurrentMapId();
        String modeDisplay = modeId != null && !modeId.isEmpty() ? modeId.toUpperCase() : "CLASSIC";
        String mapDisplay = mapId != null && !mapId.isEmpty() ? mapId.toUpperCase() : "DE_DUST2";
        modeMapText = modeDisplay + " \u00B7 " + mapDisplay;
        statusText = minecraft.getUser().getName() + "  \u00A77v" + CSEditionMod.MODID + "-" + CSEditionMod.VERSION;
    }

    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 1. Заливка краев (если арт не покрывает весь экран)
        g.fill(0, 0, width, height, 0xFF0B0E12);

        // 2. Арт start.jpg (cover)
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        g.blit(BG.get(), drawX, drawY, 0, 0, drawW, drawH, TEX_W, TEX_H);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();

        // 3. Глобальное затемнение
        g.fill(0, 0, width, height, 0x66000000);

        // 4. Дополнительное затемнение правого нижнего угла (зона кнопок)
        int panelX = width - RIGHT - BTN_W - 16;
        int panelY = height - BOTTOM - BTN_H * 6 - GAP * 5 - 24;
        int panelW = BTN_W + 16;
        int panelH = BTN_H * 6 + GAP * 5 + 24 + 16 + 12; // кнопки + заголовок + статус + отступы
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0x66000000);

        // 5. Подложка под стек кнопок (панель)
        int pad = 8;
        int bgX = panelX - pad;
        int bgY = panelY - pad;
        int bgW = panelW + pad * 2;
        int bgH = panelH + pad * 2;
        g.fill(bgX, bgY, bgX + bgW, bgY + bgH, CSRenderUtil.COLOR_PANEL_BG);
        g.renderOutline(bgX, bgY, bgW, bgH, CSRenderUtil.COLOR_BORDER);

        // 6. Заголовок блока над стеком
        int titleY = bgY + 6;
        g.drawString(font, modeMapText, bgX + (bgW - font.width(modeMapText)) / 2, titleY, CSRenderUtil.COLOR_TEXT_MUTED, true);

        // 7. Статус под кнопками
        int statusY = bgY + bgH - 12;
        g.drawString(font, statusText, bgX + bgW - font.width(statusText) - 8, statusY, CSRenderUtil.COLOR_TEXT_MUTED);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ====================== Событие подмены TitleScreen ======================

    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.TitleScreen) {
            event.setNewScreen(new CsMainMenuScreen());
        }
    }

    public static void registerClientSetup(FMLClientSetupEvent event) {
        // Регистрация слушателя событий экранов через enqueueWork — безопасно для sided_setup
        event.enqueueWork(() -> {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(CsMainMenuScreen::onScreenOpening);
        });
    }
}