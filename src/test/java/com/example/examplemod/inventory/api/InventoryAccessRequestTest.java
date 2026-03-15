package com.example.examplemod.inventory.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoryAccessRequestTest {

    @Test
    void readOnly_and_readWrite_setAccessMode() {
        InventoryAccessRequest ro = InventoryAccessRequest.readOnly("mymod", 1L, "layout");
        InventoryAccessRequest rw = InventoryAccessRequest.readWrite("mymod", 1L, "layout");

        assertEquals(AccessMode.READ_ONLY, ro.accessMode());
        assertEquals(AccessMode.READ_WRITE, rw.accessMode());
    }

    @Test
    void wildcardHelpers_workAsExpected() {
        InventoryAccessRequest request = InventoryAccessRequest.readOnly(
                "mymod",
                InventoryAccessRequest.ANY_REVISION,
                InventoryAccessRequest.ANY_LAYOUT);

        assertTrue(request.acceptsAnyRevision());
        assertTrue(request.acceptsAnyLayout());
    }

    @Test
    void blankModId_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new InventoryAccessRequest(" ", AccessMode.READ_ONLY, 0L, "layout"));
    }
}

