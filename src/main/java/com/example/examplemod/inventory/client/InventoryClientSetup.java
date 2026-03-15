package com.example.examplemod.inventory.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.inventory.client.screen.ExtendedInventoryScreen;
import com.example.examplemod.inventory.menu.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class InventoryClientSetup {

    private InventoryClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenus.EXTENDED_INVENTORY.get(), ExtendedInventoryScreen::new));
    }
}

