package com.csedition.client.keybind;

import com.csedition.CSEditionMod;
import com.csedition.client.screen.CrosshairConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

/**
 * Регистрация клавиш мода в стиле Counter-Strike.
 * KeyMapping создаются лениво в RegisterKeyMappingsEvent,
 * чтобы избежать ExceptionInInitializerError на стадии sided_setup.
 */
@OnlyIn(Dist.CLIENT)
public class KeyBindings {
    public static final String CATEGORY = "key.categories." + CSEditionMod.MODID;

    // Ленивые поставщики KeyMapping — создаются только в RegisterKeyMappingsEvent
    private static final Supplier<KeyMapping> OPEN_BUY_MENU = () -> new KeyMapping(
            "key." + CSEditionMod.MODID + ".buy_menu",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY
    );

    private static final Supplier<KeyMapping> OPEN_MAP_SELECT = () -> new KeyMapping(
            "key." + CSEditionMod.MODID + ".map_select",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            CATEGORY
    );

    private static final Supplier<KeyMapping> DROP_WEAPON = () -> new KeyMapping(
            "key." + CSEditionMod.MODID + ".drop_weapon",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    );

    private static final Supplier<KeyMapping> INSPECT_WEAPON = () -> new KeyMapping(
            "key." + CSEditionMod.MODID + ".inspect_weapon",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            CATEGORY
    );

    private static final Supplier<KeyMapping> QUICK_BUY_LAST = () -> new KeyMapping(
            "key." + CSEditionMod.MODID + ".quick_buy_last",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            CATEGORY
    );

    private static final Supplier<KeyMapping> QUICK_BUY_PRIMARY = () -> new KeyMapping(
            "key." + CSEditionMod.MODID + ".quick_buy_primary",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            CATEGORY
    );

    private static final Supplier<KeyMapping> QUICK_BUY_SECONDARY = () -> new KeyMapping(
            "key." + CSEditionMod.MODID + ".quick_buy_secondary",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY
    );

    private static final Supplier<KeyMapping> QUICK_BUY_UTILITY = () -> new KeyMapping(
            "key." + CSEditionMod.MODID + ".quick_buy_utility",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_4,
            CATEGORY
    );

    private static final Supplier<KeyMapping> TOGGLE_NIGHT_VISION = () -> new KeyMapping(
            "key." + CSEditionMod.MODID + ".night_vision",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            CATEGORY
    );

    private static final Supplier<KeyMapping> OPEN_CROSSHAIR_CONFIG = () -> new KeyMapping(
            "key." + CSEditionMod.MODID + ".crosshair_config",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F8,
            CATEGORY
    );

    // Публичные геттеры для доступа к KeyMapping
    public static KeyMapping OPEN_BUY_MENU() { return OPEN_BUY_MENU.get(); }
    public static KeyMapping OPEN_MAP_SELECT() { return OPEN_MAP_SELECT.get(); }
    public static KeyMapping DROP_WEAPON() { return DROP_WEAPON.get(); }
    public static KeyMapping INSPECT_WEAPON() { return INSPECT_WEAPON.get(); }
    public static KeyMapping QUICK_BUY_LAST() { return QUICK_BUY_LAST.get(); }
    public static KeyMapping QUICK_BUY_PRIMARY() { return QUICK_BUY_PRIMARY.get(); }
    public static KeyMapping QUICK_BUY_SECONDARY() { return QUICK_BUY_SECONDARY.get(); }
    public static KeyMapping QUICK_BUY_UTILITY() { return QUICK_BUY_UTILITY.get(); }
    public static KeyMapping TOGGLE_NIGHT_VISION() { return TOGGLE_NIGHT_VISION.get(); }
    public static KeyMapping OPEN_CROSSHAIR_CONFIG() { return OPEN_CROSSHAIR_CONFIG.get(); }

    @SubscribeEvent
    public static void onRegister(RegisterKeyMappingsEvent event) {
        event.register(OPEN_BUY_MENU.get());
        event.register(OPEN_MAP_SELECT.get());
        event.register(DROP_WEAPON.get());
        event.register(INSPECT_WEAPON.get());
        event.register(QUICK_BUY_LAST.get());
        event.register(QUICK_BUY_PRIMARY.get());
        event.register(QUICK_BUY_SECONDARY.get());
        event.register(QUICK_BUY_UTILITY.get());
        event.register(TOGGLE_NIGHT_VISION.get());
        event.register(OPEN_CROSSHAIR_CONFIG.get());
    }
}