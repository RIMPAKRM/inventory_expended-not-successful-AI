package com.example.examplemod.inventory.core.transaction;

/**
 * Defines the server-side policy applied when an equipment-change operation would leave
 * items without a valid destination slot (overflow condition).
 *
 * <p>Evaluated inside {@link InventoryTransactionService} during the <em>dry-run</em> phase.
 * The selected policy determines whether the transaction is committed or rejected.</p>
 */
public enum OverflowPolicy {

    /**
     * Reject the entire operation if any item in the removed slots cannot be
     * relocated to a free compatible slot in the remaining layout.
     * This is the <strong>safest default</strong>.
     */
    DENY_REMOVE_IF_OVERFLOW,

    /**
     * Try to relocate overflow items to the first available compatible slot in the
     * remaining layout. If any item still cannot fit, the operation is rejected.
     */
    MOVE_TO_AVAILABLE_SLOTS_FIRST,

    /**
     * Allow the operation even when overflow items remain.
     * Overflow items are moved to read-only overflow tracking. Intended for
     * emergency migrations.
     */
    ALLOW_OVERFLOW_READONLY,

    /**
     * Try relocation first; if items still cannot fit, drop them to the world.
     * <strong>Use with extreme caution</strong> — may cause item loss.
     */
    DROP_TO_WORLD_LAST_RESORT
}

