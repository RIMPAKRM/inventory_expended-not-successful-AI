package com.example.examplemod.inventory.core.network.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientCraftPanelStateTest {

    @BeforeEach
    void reset() {
        ClientCraftPanelState.resetForTests();
    }

    @Test
    void applyCreateResult_updatesState() {
        ClientCraftPanelState.applyCreateResult(
                true,
                "test_leather_rig",
                "screen.inventory.extended_inventory.craft.success");

        assertTrue(ClientCraftPanelState.lastSuccess());
        assertEquals("test_leather_rig", ClientCraftPanelState.lastRecipeId());
        assertEquals("screen.inventory.extended_inventory.craft.success", ClientCraftPanelState.lastMessageKey());
    }
}
