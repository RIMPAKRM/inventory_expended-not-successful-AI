package com.example.examplemod.inventory.api;

import com.example.examplemod.inventory.core.transaction.TransactionResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Public API mutation result used by external mods.
 */
public final class InventoryMutationResult {

    public enum Status {
        COMMITTED,
        ROLLED_BACK,
        REJECTED,
        CONFLICT,
        FORBIDDEN,
        RATE_LIMITED
    }

    private final Status status;
    private final String detail;
    private final long revision;
    private final String layoutVersion;
    private final List<String> dirtySlotIds;

    private InventoryMutationResult(@NotNull Status status,
                                    @NotNull String detail,
                                    long revision,
                                    @NotNull String layoutVersion,
                                    @NotNull List<String> dirtySlotIds) {
        this.status = Objects.requireNonNull(status, "status");
        this.detail = Objects.requireNonNull(detail, "detail");
        this.revision = revision;
        this.layoutVersion = Objects.requireNonNull(layoutVersion, "layoutVersion");
        this.dirtySlotIds = List.copyOf(dirtySlotIds);
    }

    @NotNull
    public static InventoryMutationResult fromTransaction(@NotNull TransactionResult result,
                                                          long revision,
                                                          @NotNull String layoutVersion) {
        Status mapped = switch (result.getStatus()) {
            case COMMITTED -> Status.COMMITTED;
            case ROLLED_BACK -> Status.ROLLED_BACK;
            case REJECTED -> Status.REJECTED;
        };
        return new InventoryMutationResult(mapped, result.getDebugDetail(), revision, layoutVersion,
                result.getDirtySlotIds());
    }

    @NotNull
    public static InventoryMutationResult conflict(@NotNull String detail,
                                                   long revision,
                                                   @NotNull String layoutVersion) {
        return new InventoryMutationResult(Status.CONFLICT, detail, revision, layoutVersion, List.of());
    }

    @NotNull
    public static InventoryMutationResult forbidden(@NotNull String detail,
                                                    long revision,
                                                    @NotNull String layoutVersion) {
        return new InventoryMutationResult(Status.FORBIDDEN, detail, revision, layoutVersion, List.of());
    }

    @NotNull
    public static InventoryMutationResult rateLimited(@NotNull String detail,
                                                      long revision,
                                                      @NotNull String layoutVersion) {
        return new InventoryMutationResult(Status.RATE_LIMITED, detail, revision, layoutVersion, List.of());
    }

    @NotNull
    public Status getStatus() {
        return status;
    }

    public boolean isCommitted() {
        return status == Status.COMMITTED;
    }

    @NotNull
    public String getDetail() {
        return detail;
    }

    public long getRevision() {
        return revision;
    }

    @NotNull
    public String getLayoutVersion() {
        return layoutVersion;
    }

    @NotNull
    public List<String> getDirtySlotIds() {
        return dirtySlotIds;
    }
}

