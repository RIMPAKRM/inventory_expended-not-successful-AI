package com.example.examplemod.inventory.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoryScreenRouterTest {

    @Test
    void shouldSendOpenRequest_enforcesCooldown() {
        assertTrue(InventoryScreenRouter.shouldSendOpenRequest(1_000L, 0L));
        assertFalse(InventoryScreenRouter.shouldSendOpenRequest(1_100L, 1_000L));
        assertTrue(InventoryScreenRouter.shouldSendOpenRequest(1_260L, 1_000L));
    }

    @Test
    void shouldReplaceVanillaInventory_obeysCreativeRule() {
        assertTrue(InventoryScreenRouter.shouldReplaceVanillaInventory(true, true, false));
        assertFalse(InventoryScreenRouter.shouldReplaceVanillaInventory(true, true, true));
        assertFalse(InventoryScreenRouter.shouldReplaceVanillaInventory(false, true, false));
        assertFalse(InventoryScreenRouter.shouldReplaceVanillaInventory(true, false, false));
    }
}
