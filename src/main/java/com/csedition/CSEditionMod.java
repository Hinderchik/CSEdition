package com.csedition;

import com.csedition.config.MapConfig;
import com.csedition.match.MatchManager;
import com.csedition.network.CSPackets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CSEditionMod.MODID)
public class CSEditionMod {
    public static final String MODID = "csedition";
    public static final String VERSION = "1.2.57";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    private final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

    public CSEditionMod() {
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(MatchManager.getInstance());
        MinecraftForge.EVENT_BUS.register(new com.csedition.event.PlayerEvents());
        MinecraftForge.EVENT_BUS.register(new com.csedition.event.ServerEvents());
        MinecraftForge.EVENT_BUS.register(new com.csedition.event.MatchEvents());
        MinecraftForge.EVENT_BUS.register(new com.csedition.command.CSCommand());

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,
                new net.minecraftforge.common.ForgeConfigSpec.Builder().build(),
                "csedition-common.toml");

        LOGGER.info("[CS-Edition] Mod initialized.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CSPackets.register();
            LOGGER.info("[CS-Edition] Packets registered.");
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.register(com.csedition.client.hud.CSHudOverlay.getInstance());
            MinecraftForge.EVENT_BUS.register(new com.csedition.client.input.InputHandler());
            com.csedition.client.hud.CrosshairOverlay.getInstance();
            com.csedition.client.hud.CsScoreboardOverlay.getInstance();
            com.csedition.client.screen.CsMainMenuScreen.registerClientSetup(event);
            modBus.addListener(com.csedition.client.keybind.KeyBindings::onRegister);
            com.csedition.config.CrosshairConfig cfg = com.csedition.config.CrosshairConfig.load();
            com.csedition.client.hud.CrosshairOverlay.setConfig(cfg);
            LOGGER.info("[CS-Edition] Client setup complete.");
        }
    }
}