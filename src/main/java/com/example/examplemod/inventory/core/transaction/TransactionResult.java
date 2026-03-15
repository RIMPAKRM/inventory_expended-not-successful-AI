package com.example.examplemod.inventory.core.transaction;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Immutable result returned by every inventory transaction operation.
 *
 * <p>A result is either a <em>commit</em> (success) or a <em>rollback</em> (failure).
 * In case of failure the service guarantees the inventory state is identical to the
 * snapshot taken at the start of the transaction.</p>
 */
public final class TransactionResult {

    /** Type of transaction outcome. */
    public enum Status {
        /** All changes committed successfully. */
        COMMITTED,
        /** Dry-run validation failed; inventory unchanged (rollback). */
        ROLLED_BACK,
        /** Operation was rejected before dry-run (invalid input, permission denied, etc.). */
        REJECTED
    }

    private final Status status;
    @Nullable
    private final Component playerMessage;
    /** Additional diagnostic detail — server-log only, not sent to client. */
    @NotNull
    private final String debugDetail;
    /**
     * Slot IDs that were modified (non-empty only on {@link Status#COMMITTED}).
     * Used by M4 to determine which dirty slots to include in a partial sync.
     */
    @NotNull
    private final List<String> dirtySlotIds;

    private TransactionResult(Status status,
                               @Nullable Component playerMessage,
                               @NotNull String debugDetail,
                               @NotNull List<String> dirtySlotIds) {
        this.status = status;
        this.playerMessage = playerMessage;
        this.debugDetail = debugDetail;
        this.dirtySlotIds = List.copyOf(dirtySlotIds);
    }

    // ── Factory methods ─────────────────────────────────────────────────────

    /** Creates a successful committed result with the given dirty slot ids. */
    public static TransactionResult committed(@NotNull List<String> dirtySlotIds) {
        return new TransactionResult(Status.COMMITTED, null, "ok", dirtySlotIds);
    }

    /** Creates a successful committed result (no dirty-slot tracking needed). */
    public static TransactionResult committed() {
        return committed(List.of());
    }

    /** Creates a rolled-back (validation failed) result. */
    public static TransactionResult rolledBack(@NotNull String debugDetail,
                                               @Nullable Component playerMessage) {
        return new TransactionResult(Status.ROLLED_BACK, playerMessage, debugDetail, List.of());
    }

    /** Creates a rolled-back result without a player-facing message. */
    public static TransactionResult rolledBack(@NotNull String debugDetail) {
        return rolledBack(debugDetail, null);
    }

    /** Creates a rejected result (bad input / denied before any snapshot). */
    public static TransactionResult rejected(@NotNull String debugDetail,
                                             @Nullable Component playerMessage) {
        return new TransactionResult(Status.REJECTED, playerMessage, debugDetail, List.of());
    }

    /** Creates a rejected result without a player-facing message. */
    public static TransactionResult rejected(@NotNull String debugDetail) {
        return rejected(debugDetail, null);
    }

    // ── Accessors ───────────────────────────────────────────────────────────

    /** @return the outcome status */
    @NotNull
    public Status getStatus() { return status; }

    /** @return {@code true} if the transaction was committed successfully */
    public boolean isSuccess() { return status == Status.COMMITTED; }

    /**
     * @return an optional player-facing error message to display in chat;
     *         {@code null} if there is nothing meaningful to show
     */
    @Nullable
    public Component getPlayerMessage() { return playerMessage; }

    /** @return server-side debug detail string (never shown to clients) */
    @NotNull
    public String getDebugDetail() { return debugDetail; }

    /**
     * @return unmodifiable list of slotIds that were modified;
     *         non-empty only on {@link Status#COMMITTED}; used by M4 partial sync
     */
    @NotNull
    public List<String> getDirtySlotIds() { return dirtySlotIds; }

    @Override
    public String toString() {
        return "TransactionResult{status=" + status
                + ", dirty=" + dirtySlotIds.size()
                + ", detail='" + debugDetail + "'}";
    }
}
