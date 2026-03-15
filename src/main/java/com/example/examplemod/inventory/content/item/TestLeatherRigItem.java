package com.example.examplemod.inventory.content.item;

import com.example.examplemod.inventory.content.TestLeatherRigLayoutTemplate;
import com.example.examplemod.inventory.core.slot.IEquipmentSlotProvider;
import com.example.examplemod.inventory.core.slot.SlotGroupDefinition;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.DyeableArmorItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Test M7 equipment item based on leather chest armor that contributes extra inventory slots.
 */
public class TestLeatherRigItem extends DyeableArmorItem implements IEquipmentSlotProvider {

    public TestLeatherRigItem(@NotNull ArmorMaterial material,
                              @NotNull ArmorItem.Type type,
                              @NotNull Properties properties) {
        super(material, type, properties);
    }

    @Override
    @NotNull
    public List<SlotGroupDefinition> getSlotGroups(@NotNull ItemStack stack) {
        return TestLeatherRigLayoutTemplate.createGroups();
    }

    @Override
    @NotNull
    public String getLayoutVersionToken(@NotNull ItemStack stack) {
        return TestLeatherRigLayoutTemplate.layoutToken();
    }
}

