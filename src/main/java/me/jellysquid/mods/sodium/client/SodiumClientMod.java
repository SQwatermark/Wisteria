package me.jellysquid.mods.sodium.client;

import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Mod("sodium")
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = "sodium")
public class SodiumClientMod {
    private static SodiumGameOptions CONFIG;
    public static Logger LOGGER;

    private static String MOD_VERSION;

    @SubscribeEvent
    public static void onInitializeClient(FMLClientSetupEvent event) {
//        ModContainer mod = FabricLoader.getInstance()
//                .getModContainer("sodium")
//                .orElseThrow(NullPointerException::new);

//        MOD_VERSION = mod.getMetadata()
//                .getVersion()
//                .getFriendlyString();

        MOD_VERSION = "0.0.1";

        LOGGER = LoggerFactory.getLogger("Sodium");
        CONFIG = loadConfig();
    }

    public static SodiumGameOptions options() {
        if (CONFIG == null) {
            throw new IllegalStateException("Config not yet available");
        }

        return CONFIG;
    }

    public static Logger logger() {
        if (LOGGER == null) {
            throw new IllegalStateException("Logger not yet available");
        }

        return LOGGER;
    }

    private static SodiumGameOptions loadConfig() {
        try {
            return SodiumGameOptions.load();
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration file", e);
            LOGGER.error("Using default configuration file in read-only mode");

            var config = new SodiumGameOptions();
            config.setReadOnly();

            return config;
        }
    }

    public static void restoreDefaultOptions() {
        CONFIG = SodiumGameOptions.defaults();

        try {
            CONFIG.writeChanges();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write config file", e);
        }
    }

    public static String getVersion() {
        if (MOD_VERSION == null) {
            throw new NullPointerException("Mod version hasn't been populated yet");
        }

        return MOD_VERSION;
    }

    public static boolean isDirectMemoryAccessEnabled() {
        return options().advanced.allowDirectMemoryAccess;
    }
}
