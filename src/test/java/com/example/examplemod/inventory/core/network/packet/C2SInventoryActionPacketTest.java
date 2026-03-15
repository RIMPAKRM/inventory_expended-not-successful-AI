package com.example.examplemod.inventory.core.network.packet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class C2SInventoryActionPacketTest {

    @Test
    void vanillaQuickMoveSlotPolicy_allowsOnlyHotbarOutsideCreative() {
        assertTrue(C2SInventoryActionPacket.isVanillaQuickMoveSlotAllowed(0, false));
        assertTrue(C2SInventoryActionPacket.isVanillaQuickMoveSlotAllowed(8, false));
        assertFalse(C2SInventoryActionPacket.isVanillaQuickMoveSlotAllowed(9, false));
        assertFalse(C2SInventoryActionPacket.isVanillaQuickMoveSlotAllowed(35, false));
    }

    @Test
    void vanillaQuickMoveSlotPolicy_allowsFullRangeInCreative() {
        assertTrue(C2SInventoryActionPacket.isVanillaQuickMoveSlotAllowed(0, true));
        assertTrue(C2SInventoryActionPacket.isVanillaQuickMoveSlotAllowed(9, true));
        assertTrue(C2SInventoryActionPacket.isVanillaQuickMoveSlotAllowed(35, true));
    }

    @Test
    void vanillaQuickMoveSlotPolicy_rejectsOutOfRange() {
        assertFalse(C2SInventoryActionPacket.isVanillaQuickMoveSlotAllowed(-1, false));
        assertFalse(C2SInventoryActionPacket.isVanillaQuickMoveSlotAllowed(36, false));
        assertFalse(C2SInventoryActionPacket.isVanillaQuickMoveSlotAllowed(100, true));
    }
}

