package com.example.examplemod.inventory.core.network.guard;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InventoryActionFloodGuardTest {

    @Test
    void tryAcquire_limitsPerTickPerChannel() {
        InventoryActionFloodGuard guard = InventoryActionFloodGuard.getInstance();
        UUID playerId = UUID.randomUUID();
        guard.clearPlayer(playerId);

        assertTrue(guard.tryAcquire(playerId, "c2s", 100L, 2));
        assertTrue(guard.tryAcquire(playerId, "c2s", 100L, 2));
        assertFalse(guard.tryAcquire(playerId, "c2s", 100L, 2));
    }

    @Test
    void tryAcquire_resetsWindowOnNextTick() {
        InventoryActionFloodGuard guard = InventoryActionFloodGuard.getInstance();
        UUID playerId = UUID.randomUUID();
        guard.clearPlayer(playerId);

        assertTrue(guard.tryAcquire(playerId, "api", 200L, 1));
        assertFalse(guard.tryAcquire(playerId, "api", 200L, 1));

        assertTrue(guard.tryAcquire(playerId, "api", 201L, 1));
    }

    @Test
    void channels_areIndependent() {
        InventoryActionFloodGuard guard = InventoryActionFloodGuard.getInstance();
        UUID playerId = UUID.randomUUID();
        guard.clearPlayer(playerId);

        assertTrue(guard.tryAcquire(playerId, "c2s", 300L, 1));
        assertTrue(guard.tryAcquire(playerId, "api", 300L, 1));
        assertFalse(guard.tryAcquire(playerId, "c2s", 300L, 1));
        assertFalse(guard.tryAcquire(playerId, "api", 300L, 1));
    }
}

