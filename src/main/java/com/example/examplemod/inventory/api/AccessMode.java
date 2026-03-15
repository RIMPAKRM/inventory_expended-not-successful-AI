package com.example.examplemod.inventory.api;

/**
 * Access mode for external inventory API callers.
 */
public enum AccessMode {
    READ_ONLY,
    READ_WRITE;

    public boolean canMutate() {
        return this == READ_WRITE;
    }
}

