package com.example.examplemod.inventory.core.transaction;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The result of a server-side slot placement validation check.
 *
 * <p>Produced by transaction-service validation logic and used inside the
 * dry-run phase before any actual mutation is committed.</p>
 */
public final class SlotValidationResult {

    /** Whether the placement is allowed. */
    private final boolean allowed;
    /** The remainder stack after insertion (may be empty if fully inserted). */
    @NotNull
    private final ItemStack remainder;
    /** Rejection reason for logging / player feedback (null if allowed). */
    @Nullable
    private final String rejectionReason;

    private SlotValidationResult(boolean allowed, @NotNull ItemStack remainder,
                                  @Nullable String rejectionReason) {
        this.allowed = allowed;
        this.remainder = remainder;
        this.rejectionReason = rejectionReason;
    }

    /** Creates an "allowed" result with the given remainder stack. */
    public static SlotValidationResult allowed(@NotNull ItemStack remainder) {
        return new SlotValidationResult(true, remainder, null);
    }

    /** Creates a "fully allowed" result (the stack fits entirely). */
    public static SlotValidationResult fullyAllowed() {
        return new SlotValidationResult(true, ItemStack.EMPTY, null);
    }

    /** Creates a "rejected" result with a server-side reason string. */
    public static SlotValidationResult rejected(@NotNull String reason) {
        return new SlotValidationResult(false, ItemStack.EMPTY, reason);
    }

    /** @return {@code true} if the placement passes validation */
    public boolean isAllowed() { return allowed; }

    /**
     * @return the leftover stack after simulated insertion; empty if the stack
     *         fits completely or if the validation was rejected
     */
    @NotNull
    public ItemStack getRemainder() { return remainder; }

    /** @return rejection reason string, or {@code null} if allowed */
    @Nullable
    public String getRejectionReason() { return rejectionReason; }
}
