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
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    private final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

    public CSEditionMod() {
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(MatchManager.getInstance());
        MinecraftForge.EVENT_BUS.register(new com.csedition.event.PlayerEvents());
        MinecraftForge.EVENT_BUS.register(new com.csedition.event.ServerEvents());

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,
                net.minecraftforge.common.ForgeConfigSpec.Builder.builder().build(),
                "csedition-common.toml");

        MapConfig.load();
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
            MinecraftForge.EVENT_BUS.register(new com.csedition.client.hud.CSHudOverlay());
            MinecraftForge.EVENT_BUS.register(new com.csedition.client.input.InputHandler());
            modBus.addListener(com.csedition.client.keybind.KeyBindings::onRegister);
            LOGGER.info("[CS-Edition] Client setup complete.");
        }
    }
}
