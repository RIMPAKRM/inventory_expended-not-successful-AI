package com.example.examplemod.inventory.core.service.policy;

/**
 * Logical action types used by access policy and audit.
 */
public enum InventoryActionKind {
    SNAPSHOT,
    FIND,
    MOVE,
    EXTRACT,
    INSERT;

    public boolean isMutation() {
        return this == MOVE || this == EXTRACT || this == INSERT;
    }
}

