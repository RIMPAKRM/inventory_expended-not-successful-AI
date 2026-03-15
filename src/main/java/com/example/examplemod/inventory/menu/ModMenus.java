package com.example.examplemod.inventory.menu;

import com.example.examplemod.ExampleMod;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ExampleMod.MODID);

    public static final RegistryObject<MenuType<ExtendedInventoryMenu>> EXTENDED_INVENTORY = MENUS.register(
            "extended_inventory",
            () -> IForgeMenuType.create((windowId, inventory, data) -> new ExtendedInventoryMenu(windowId, inventory, data)));

    private ModMenus() {
    }
}

