package com.example.examplemod.inventory.core.network.packet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class C2SCraftCreateIntentPacketTest {

    @Test
    void validRecipeId_acceptsKnownRecipe() {
        assertTrue(C2SCraftCreateIntentPacket.isValidRecipeId("test_leather_rig"));
    }

    @Test
    void validRecipeId_rejectsUnknownOrBlank() {
        assertFalse(C2SCraftCreateIntentPacket.isValidRecipeId(""));
        assertFalse(C2SCraftCreateIntentPacket.isValidRecipeId("unknown"));
    }
}

