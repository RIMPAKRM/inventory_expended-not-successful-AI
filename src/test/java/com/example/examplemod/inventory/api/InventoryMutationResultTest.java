package com.example.examplemod.inventory.api;

import com.example.examplemod.inventory.core.transaction.TransactionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InventoryMutationResultTest {

    @Test
    void fromTransaction_mapsCommittedAndDirtySlots() {
        TransactionResult tx = TransactionResult.committed(List.of("slot_a", "slot_b"));

        InventoryMutationResult result = InventoryMutationResult.fromTransaction(tx, 7L, "layout-v3");

        assertEquals(InventoryMutationResult.Status.COMMITTED, result.getStatus());
        assertTrue(result.isCommitted());
        assertEquals(7L, result.getRevision());
        assertEquals("layout-v3", result.getLayoutVersion());
        assertEquals(List.of("slot_a", "slot_b"), result.getDirtySlotIds());
    }

    @Test
    void conflict_forbidden_rateLimited_haveExpectedStatuses() {
        assertEquals(InventoryMutationResult.Status.CONFLICT,
                InventoryMutationResult.conflict("x", 1L, "l").getStatus());
        assertEquals(InventoryMutationResult.Status.FORBIDDEN,
                InventoryMutationResult.forbidden("x", 1L, "l").getStatus());
        assertEquals(InventoryMutationResult.Status.RATE_LIMITED,
                InventoryMutationResult.rateLimited("x", 1L, "l").getStatus());
    }
}

