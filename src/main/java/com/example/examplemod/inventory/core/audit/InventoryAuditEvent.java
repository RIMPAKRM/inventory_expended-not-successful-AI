package com.example.examplemod.inventory.core.audit;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Audit event for inventory API operations.
 */
public record InventoryAuditEvent(
        long epochMillis,
        @NotNull UUID actorId,
        @NotNull String actorName,
        @NotNull UUID targetId,
        @NotNull String targetName,
        @NotNull String modId,
        @NotNull String action,
        @NotNull String status,
        @NotNull String detail,
        long revision,
        @NotNull String layoutVersion
) {
}

