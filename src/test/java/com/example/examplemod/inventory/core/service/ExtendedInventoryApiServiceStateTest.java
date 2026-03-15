package com.example.examplemod.inventory.core.service;

import com.example.examplemod.inventory.api.InventoryAccessRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExtendedInventoryApiServiceStateTest {

    @Test
    void matchesExpectedState_acceptsExactRevisionAndLayout() {
        InventoryAccessRequest request = InventoryAccessRequest.readOnly("testmod", 12L, "layout-v2");

        assertTrue(ExtendedInventoryApiService.matchesExpectedState(request, 12L, "layout-v2"));
        assertFalse(ExtendedInventoryApiService.matchesExpectedState(request, 11L, "layout-v2"));
        assertFalse(ExtendedInventoryApiService.matchesExpectedState(request, 12L, "layout-v1"));
    }

    @Test
    void matchesExpectedState_supportsWildcardRevisionAndLayout() {
        InventoryAccessRequest any = InventoryAccessRequest.readWrite(
                "testmod",
                InventoryAccessRequest.ANY_REVISION,
                InventoryAccessRequest.ANY_LAYOUT);

        assertTrue(ExtendedInventoryApiService.matchesExpectedState(any, 1L, "x"));
        assertTrue(ExtendedInventoryApiService.matchesExpectedState(any, 999L, "layout-any"));
    }
}

