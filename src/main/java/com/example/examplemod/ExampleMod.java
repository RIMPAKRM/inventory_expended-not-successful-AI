package com.example.examplemod;

import com.example.examplemod.inventory.content.ModItems;
import com.example.examplemod.inventory.core.network.InventoryNetworkHandler;
import com.example.examplemod.inventory.menu.ModMenus;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ExampleMod.MODID)
public class ExampleMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "inventory";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public ExampleMod(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener(this::commonSetup);
        ModItems.ITEMS.register(context.getModEventBus());
        ModMenus.MENUS.register(context.getModEventBus());

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(InventoryNetworkHandler::register);
        LOGGER.info("Extended Inventory M7 UI replacement foundation initialized");
    }
}
