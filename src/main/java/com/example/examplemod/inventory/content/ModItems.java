package com.example.examplemod.inventory.content;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.inventory.content.item.TestLeatherRigItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ExampleMod.MODID);

    public static final RegistryObject<Item> TEST_LEATHER_RIG = ITEMS.register(
            TestLeatherRigLayoutTemplate.ITEM_NAME,
            () -> new TestLeatherRigItem(ArmorMaterials.LEATHER, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}

