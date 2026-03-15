package com.example.examplemod.inventory.core.sync;

/**
 * Reasons that can trigger a full inventory synchronisation.
 */
public enum SyncTrigger {
    LOGIN,
    OPEN_MENU,
    CLONE,
    RESPAWN,
    DIMENSION_CHANGE,
    LAYOUT_CHANGED,
    REVISION_CONFLICT,
    TOO_MANY_DIRTY_SLOTS
}

