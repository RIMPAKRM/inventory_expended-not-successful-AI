package com.example.examplemod.inventory.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for revision + dirty-flag state tracking logic.
 *
 * <p>These tests operate only on plain state holders — no Minecraft/Forge bootstrap required.</p>
 */
class RevisionDirtyStateTest {

    /**
     * Minimal in-process state holder that mirrors the revision/dirty contract of
     * {@link com.example.examplemod.inventory.core.capability.ExtendedInventoryCapability}
     * without depending on {@link net.minecraftforge.items.ItemStackHandler}.
     */
    static class RevisionState {
        private long revision = 0L;
        private boolean dirty = false;

        void markDirty()   { revision++; dirty = true; }
        void clearDirty()  { dirty = false; }
        long getRevision() { return revision; }
        boolean isDirty()  { return dirty; }
    }

    @Test
    void initialStateIsZeroRevisionNotDirty() {
        RevisionState state = new RevisionState();
        Assertions.assertEquals(0L, state.getRevision());
        Assertions.assertFalse(state.isDirty());
    }

    @Test
    void markDirtyIncrementsRevisionAndSetsDirtyFlag() {
        RevisionState state = new RevisionState();

        state.markDirty();

        Assertions.assertEquals(1L, state.getRevision());
        Assertions.assertTrue(state.isDirty());
    }

    @Test
    void clearDirtyDoesNotChangeRevision() {
        RevisionState state = new RevisionState();
        state.markDirty();
        long revBefore = state.getRevision();

        state.clearDirty();

        Assertions.assertFalse(state.isDirty());
        Assertions.assertEquals(revBefore, state.getRevision(),
                "clearDirty must not change revision");
    }

    @Test
    void multipleMarkDirtyCallsIncrementRevisionEachTime() {
        RevisionState state = new RevisionState();
        state.markDirty();
        state.markDirty();
        state.markDirty();

        Assertions.assertEquals(3L, state.getRevision());
        Assertions.assertTrue(state.isDirty());
    }
}



