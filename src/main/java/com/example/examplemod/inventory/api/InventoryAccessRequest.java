package com.example.examplemod.inventory.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Request metadata for external API calls.
 * Callers should pass their expected revision/layout to detect stale state.
 */
public record InventoryAccessRequest(
        @NotNull String modId,
        @NotNull AccessMode accessMode,
        long expectedRevision,
        @NotNull String expectedLayoutVersion
) {
    public static final long ANY_REVISION = -1L;
    public static final String ANY_LAYOUT = "*";

    public InventoryAccessRequest {
        Objects.requireNonNull(modId, "modId");
        Objects.requireNonNull(accessMode, "accessMode");
        Objects.requireNonNull(expectedLayoutVersion, "expectedLayoutVersion");
        if (modId.isBlank()) {
            throw new IllegalArgumentException("modId must not be blank");
        }
    }

    @NotNull
    public static InventoryAccessRequest readOnly(@NotNull String modId,
                                                  long expectedRevision,
                                                  @NotNull String expectedLayoutVersion) {
        return new InventoryAccessRequest(modId, AccessMode.READ_ONLY, expectedRevision, expectedLayoutVersion);
    }

    @NotNull
    public static InventoryAccessRequest readWrite(@NotNull String modId,
                                                   long expectedRevision,
                                                   @NotNull String expectedLayoutVersion) {
        return new InventoryAccessRequest(modId, AccessMode.READ_WRITE, expectedRevision, expectedLayoutVersion);
    }

    public boolean acceptsAnyRevision() {
        return expectedRevision == ANY_REVISION;
    }

    public boolean acceptsAnyLayout() {
        return ANY_LAYOUT.equals(expectedLayoutVersion);
    }
}

