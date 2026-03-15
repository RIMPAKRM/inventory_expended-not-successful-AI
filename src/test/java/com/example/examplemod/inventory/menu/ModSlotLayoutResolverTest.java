package com.example.examplemod.inventory.menu;

import com.example.examplemod.inventory.client.screen.InventoryLayoutConstants;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModSlotLayoutResolverTest {

    @Test
    void anchorsAreAssignedToExpectedSlots() {
        ModSlotLayoutResolver.Layout layout = ModSlotLayoutResolver.resolve(List.of(
                "examplemod:helmet_slot",
                "examplemod:gas_mask_slot",
                "examplemod:jacket_slot",
                "examplemod:vest_slot",
                "examplemod:backpack_slot",
                "examplemod:gloves_slot",
                "examplemod:pants_slot",
                "examplemod:boots_slot"));

        assertEquals("examplemod:helmet_slot", layout.equipmentSlotId(ModSlotLayoutResolver.EquipmentAnchor.HEAD));
        assertEquals("examplemod:gas_mask_slot", layout.equipmentSlotId(ModSlotLayoutResolver.EquipmentAnchor.FACE));
        assertEquals("examplemod:jacket_slot", layout.equipmentSlotId(ModSlotLayoutResolver.EquipmentAnchor.UPPER));
        assertEquals("examplemod:vest_slot", layout.equipmentSlotId(ModSlotLayoutResolver.EquipmentAnchor.VEST));
        assertEquals("examplemod:backpack_slot", layout.equipmentSlotId(ModSlotLayoutResolver.EquipmentAnchor.BACKPACK));
        assertEquals("examplemod:gloves_slot", layout.equipmentSlotId(ModSlotLayoutResolver.EquipmentAnchor.GLOVES));
        assertEquals("examplemod:pants_slot", layout.equipmentSlotId(ModSlotLayoutResolver.EquipmentAnchor.PANTS));
        assertEquals("examplemod:boots_slot", layout.equipmentSlotId(ModSlotLayoutResolver.EquipmentAnchor.BOOTS));
    }

    @Test
    void dynamicSlotsUseGridFromConstants() {
        ModSlotLayoutResolver.Layout layout = ModSlotLayoutResolver.resolve(List.of(
                "examplemod:base_general_0",
                "examplemod:base_general_1",
                "examplemod:base_general_2",
                "examplemod:base_general_3",
                "examplemod:base_general_4",
                "examplemod:base_general_5",
                "examplemod:base_general_6",
                "examplemod:base_general_7",
                "examplemod:base_general_8",
                "examplemod:base_general_9"));

        ModSlotLayoutResolver.SlotPosition first = layout.positions().get("examplemod:base_general_0");
        ModSlotLayoutResolver.SlotPosition tenth = layout.positions().get("examplemod:base_general_9");

        assertNotNull(first);
        assertNotNull(tenth);
        assertEquals(InventoryLayoutConstants.DYNAMIC_X, first.x());
        assertEquals(InventoryLayoutConstants.DYNAMIC_Y, first.y());
        assertEquals(InventoryLayoutConstants.DYNAMIC_X, tenth.x());
        assertEquals(InventoryLayoutConstants.DYNAMIC_Y + InventoryLayoutConstants.SLOT_STEP, tenth.y());
    }
}

