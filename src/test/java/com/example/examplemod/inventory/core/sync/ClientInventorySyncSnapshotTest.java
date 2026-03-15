package com.example.examplemod.inventory.core.sync;

import com.example.examplemod.inventory.core.network.client.ClientInventorySyncState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientInventorySyncSnapshotTest {

    @AfterEach
    void cleanup() {
        ClientInventorySyncState.resetForTests();
    }

    @Test
    void snapshot_emptyState_isNotUsable() {
        ClientInventorySyncState.Snapshot snapshot = ClientInventorySyncState.snapshot();
        assertFalse(snapshot.hasUsableSnapshot());
        assertEquals(0L, snapshot.revision());
        assertTrue(snapshot.layoutVersion().isBlank());
        assertTrue(snapshot.slotOrder().isEmpty());
        assertTrue(snapshot.slots().isEmpty());
    }
}

