package com.csedition.client.keybind;

import com.csedition.CSEditionMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Регистрация клавиш мода.
 * B — открыть меню закупа.
 */
public class KeyBindings {
    public static final String CATEGORY = "key.categories." + CSEditionMod.MODID;

    public static final KeyMapping OPEN_BUY_MENU = new KeyMapping(
            "key." + CSEditionMod.MODID + ".buy_menu",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY
    );

    public static void register() {
        // Регистрация через событие RegisterKeyMappingsEvent
    }

    public static void onRegister(RegisterKeyMappingsEvent event) {
        event.register(OPEN_BUY_MENU);
    }
}
