package me.jellysquid.mods.sodium.client;

import me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SodiumOptionsScreenEvent {

    @SubscribeEvent
    public static void loadScreen(ScreenOpenEvent event) {
        if (event.getScreen() instanceof VideoSettingsScreen) {
            event.setScreen(new SodiumOptionsGUI(Minecraft.getInstance().screen));
        }
    }

}
