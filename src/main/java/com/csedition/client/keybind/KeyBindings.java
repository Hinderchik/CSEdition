package com.csedition.client.keybind;

import com.csedition.CSEditionMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Регистрация клавиш мода в стиле Counter-Strike.
 *
 * B  — открыть меню закупа (BUY_TIME) / выбор карты (LOBBY)
 * M  — открыть выбор карты (только в LOBBY)
 * G  — выбросить текущее оружие
 * Z  — быстрая закупка (последнее купленное)
 * X  — быстрая закупка (основное оружие)
 * C  — быстрая закупка (пистолет)
 * 4  — быстрая закупка (снаряжение)
 * F  — осмотреть оружие
 * N  — переключить ночное зрение
 */
public class KeyBindings {
    public static final String CATEGORY = "key.categories." + CSEditionMod.MODID;

    // === Меню ===
    public static final KeyMapping OPEN_BUY_MENU = new KeyMapping(
            "key." + CSEditionMod.MODID + ".buy_menu",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY
    );

    public static final KeyMapping OPEN_MAP_SELECT = new KeyMapping(
            "key." + CSEditionMod.MODID + ".map_select",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            CATEGORY
    );

    // === Действия с оружием ===
    public static final KeyMapping DROP_WEAPON = new KeyMapping(
            "key." + CSEditionMod.MODID + ".drop_weapon",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    );

    public static final KeyMapping INSPECT_WEAPON = new KeyMapping(
            "key." + CSEditionMod.MODID + ".inspect_weapon",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            CATEGORY
    );

    // === Быстрая закупка ===
    public static final KeyMapping QUICK_BUY_LAST = new KeyMapping(
            "key." + CSEditionMod.MODID + ".quick_buy_last",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            CATEGORY
    );

    public static final KeyMapping QUICK_BUY_PRIMARY = new KeyMapping(
            "key." + CSEditionMod.MODID + ".quick_buy_primary",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            CATEGORY
    );

    public static final KeyMapping QUICK_BUY_SECONDARY = new KeyMapping(
            "key." + CSEditionMod.MODID + ".quick_buy_secondary",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY
    );

    public static final KeyMapping QUICK_BUY_UTILITY = new KeyMapping(
            "key." + CSEditionMod.MODID + ".quick_buy_utility",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_4,
            CATEGORY
    );

    // === Прочее ===
    public static final KeyMapping TOGGLE_NIGHT_VISION = new KeyMapping(
            "key." + CSEditionMod.MODID + ".night_vision",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            CATEGORY
    );

    public static void onRegister(RegisterKeyMappingsEvent event) {
        event.register(OPEN_BUY_MENU);
        event.register(OPEN_MAP_SELECT);
        event.register(DROP_WEAPON);
        event.register(INSPECT_WEAPON);
        event.register(QUICK_BUY_LAST);
        event.register(QUICK_BUY_PRIMARY);
        event.register(QUICK_BUY_SECONDARY);
        event.register(QUICK_BUY_UTILITY);
        event.register(TOGGLE_NIGHT_VISION);
    }
}
