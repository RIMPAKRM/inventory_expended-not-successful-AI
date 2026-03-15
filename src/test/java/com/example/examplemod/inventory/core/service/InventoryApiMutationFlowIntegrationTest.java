package com.example.examplemod.inventory.core.service;

import com.example.examplemod.inventory.api.InventoryMutationResult;
import com.example.examplemod.inventory.core.service.policy.PolicyDecision;
import com.example.examplemod.inventory.core.sync.InventorySyncOrchestrator;
import com.example.examplemod.inventory.core.sync.SyncTrigger;
import com.example.examplemod.inventory.core.transaction.TransactionResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InventoryApiMutationFlowIntegrationTest {

    @Test
    void externalApiCall_commitProducesPartialSyncDecision() {
        InventoryApiMutationFlow flow = new InventoryApiMutationFlow();
        InventorySyncOrchestrator orchestrator = new InventorySyncOrchestrator(10);
        UUID targetId = UUID.randomUUID();

        InventoryMutationResult result = flow.apply(
                targetId,
                PolicyDecision.allow(),
                true,
                true,
                TransactionResult.committed(List.of("slot_a", "slot_b")),
                10L,
                "layout-v2",
                orchestrator);

        assertEquals(InventoryMutationResult.Status.COMMITTED, result.getStatus());

        InventorySyncOrchestrator.SyncDecision syncDecision = orchestrator.drain(targetId);
        assertTrue(syncDecision.isPartial());
        assertEquals(List.of("slot_a", "slot_b"), syncDecision.getDirtySlotIds());
    }

    @Test
    void externalApiCall_staleRevisionProducesFullSyncDecision() {
        InventoryApiMutationFlow flow = new InventoryApiMutationFlow();
        InventorySyncOrchestrator orchestrator = new InventorySyncOrchestrator(10);
        UUID targetId = UUID.randomUUID();

        InventoryMutationResult result = flow.apply(
                targetId,
                PolicyDecision.allow(),
                false,
                true,
                TransactionResult.rejected("stale"),
                11L,
                "layout-v2",
                orchestrator);

        assertEquals(InventoryMutationResult.Status.CONFLICT, result.getStatus());

        InventorySyncOrchestrator.SyncDecision syncDecision = orchestrator.drain(targetId);
        assertTrue(syncDecision.isFull());
        assertEquals(SyncTrigger.REVISION_CONFLICT, syncDecision.getFullTrigger());
    }
}

