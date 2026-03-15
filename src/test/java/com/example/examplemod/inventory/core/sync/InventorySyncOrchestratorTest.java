package com.example.examplemod.inventory.core.sync;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InventorySyncOrchestratorTest {

    @Test
    void queuePartial_mergesAndDeduplicatesSlotIds() {
        InventorySyncOrchestrator orchestrator = new InventorySyncOrchestrator(10);
        UUID player = UUID.randomUUID();

        orchestrator.queuePartial(player, List.of("a", "b"));
        orchestrator.queuePartial(player, List.of("b", "c", " ", ""));

        InventorySyncOrchestrator.SyncDecision decision = orchestrator.drain(player);

        assertEquals(InventorySyncOrchestrator.SyncDecision.Type.PARTIAL, decision.getType());
        assertEquals(List.of("a", "b", "c"), decision.getDirtySlotIds());
    }

    @Test
    void requestFull_overridesAnyPendingPartial() {
        InventorySyncOrchestrator orchestrator = new InventorySyncOrchestrator(10);
        UUID player = UUID.randomUUID();

        orchestrator.queuePartial(player, List.of("slot_1", "slot_2"));
        orchestrator.requestFull(player, SyncTrigger.LOGIN);

        InventorySyncOrchestrator.SyncDecision decision = orchestrator.drain(player);

        assertTrue(decision.isFull());
        assertEquals(SyncTrigger.LOGIN, decision.getFullTrigger());
        assertTrue(decision.getDirtySlotIds().isEmpty());
    }

    @Test
    void thresholdOverflow_fallsBackToFull() {
        InventorySyncOrchestrator orchestrator = new InventorySyncOrchestrator(2);
        UUID player = UUID.randomUUID();

        orchestrator.queuePartial(player, List.of("slot_1", "slot_2", "slot_3"));

        InventorySyncOrchestrator.SyncDecision decision = orchestrator.drain(player);

        assertTrue(decision.isFull());
        assertEquals(SyncTrigger.TOO_MANY_DIRTY_SLOTS, decision.getFullTrigger());
    }

    @Test
    void drainWithoutPending_returnsNone() {
        InventorySyncOrchestrator orchestrator = new InventorySyncOrchestrator(2);
        InventorySyncOrchestrator.SyncDecision decision = orchestrator.drain(UUID.randomUUID());

        assertEquals(InventorySyncOrchestrator.SyncDecision.Type.NONE, decision.getType());
        assertFalse(decision.isFull());
        assertFalse(decision.isPartial());
    }
}

