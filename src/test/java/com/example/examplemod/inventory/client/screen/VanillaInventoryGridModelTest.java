package com.example.examplemod.inventory.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VanillaInventoryGridModelTest {

    @Test
    void findsMainInventorySlotIndex() {
        Integer slot = VanillaInventoryGridModel.findPlayerInventorySlot(
                40, 120,
                20, 110,
                20, 168);
        assertEquals(10, slot); // row 0, col 1 => 9 + 1
    }

    @Test
    void findsHotbarSlotIndex() {
        Integer slot = VanillaInventoryGridModel.findPlayerInventorySlot(
                74, 170,
                20, 110,
                20, 168);
        assertEquals(3, slot);
    }

    @Test
    void ignoresMainInventoryWhenHotbarOnlyMode() {
        Integer slot = VanillaInventoryGridModel.findPlayerInventorySlot(
                40, 120,
                20, 110,
                20, 168,
                false);
        assertNull(slot);

        Integer hotbar = VanillaInventoryGridModel.findPlayerInventorySlot(
                74, 170,
                20, 110,
                20, 168,
                false);
        assertEquals(3, hotbar);
    }

    @Test
    void returnsNullOutsideSlots() {
        Integer slot = VanillaInventoryGridModel.findPlayerInventorySlot(
                5, 5,
                20, 110,
                20, 168);
        assertNull(slot);
    }
}

