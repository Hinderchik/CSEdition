package com.csedition.client.input;

import com.csedition.client.ClientState;
import com.csedition.client.keybind.KeyBindings;
import com.csedition.client.screen.BuyMenuScreen;
import com.csedition.client.screen.MapSelectScreen;
import com.csedition.data.GamePhase;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Обработка нажатий клавиш на клиенте.
 * B — открыть меню закупа (только в BUY_TIME).
 */
@OnlyIn(Dist.CLIENT)
public class InputHandler {

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (KeyBindings.OPEN_BUY_MENU.consumeClick()) {
            GamePhase phase = ClientState.getPhase();
            if (phase == GamePhase.LOBBY) {
                // В лобби открываем выбор карты
                mc.setScreen(new MapSelectScreen());
            } else if (phase == GamePhase.BUY_TIME) {
                mc.setScreen(new BuyMenuScreen());
            }
        }
    }
}
