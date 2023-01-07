package me.jellysquid.mods.sodium.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.Platform;
import org.lwjgl.system.jemalloc.JEmalloc;

import java.util.Objects;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = SodiumClientMod.MOD_ID)
public class SodiumPreLaunch {
    private static final Logger LOGGER = LogManager.getLogger(SodiumClientMod.MOD_NAME);

    @SubscribeEvent
    public static void onPreLaunch(FMLCommonSetupEvent event) {
        // see https://forge.gemwire.uk/wiki/Sides/1.18#Writing_One-Sided_Mods
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        checkJemalloc();
    }

    private static void checkJemalloc() {
        // LWJGL 3.2.3 ships Jemalloc 5.2.0 which seems to be broken on Windows and suffers from critical memory leak problems
        // Using the system allocator prevents memory leaks and other problems
        // See changelog here: https://github.com/jemalloc/jemalloc/releases/tag/5.2.1
        if (Platform.get() == Platform.WINDOWS && isVersionWithinRange(JEmalloc.JEMALLOC_VERSION, "5.0.0", "5.2.0")) {
            LOGGER.info("Trying to switch memory allocators to work around memory leaks present with Jemalloc 5.0.0 through 5.2.0 on Windows");

            if (!Objects.equals(Configuration.MEMORY_ALLOCATOR.get(), "system")) {
                Configuration.MEMORY_ALLOCATOR.set("system");
            }
        }
    }

    private static boolean isVersionWithinRange(String curStr, String minStr, String maxStr) {
        return true;
//        SemanticVersion cur, min, max;
//
//        try {
//            cur = SemanticVersion.parse(curStr);
//            min = SemanticVersion.parse(minStr);
//            max = SemanticVersion.parse(maxStr);
//        } catch (VersionParsingException e) {
//            LOGGER.warn("Unable to parse version string", e);
//            return false;
//        }
//
//        return cur.compareTo(min) >= 0 && cur.compareTo(max) <= 0;
    }
}
