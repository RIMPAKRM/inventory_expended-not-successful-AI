package com.example.examplemod.inventory.core.transaction;

import com.example.examplemod.inventory.core.capability.IExtendedInventoryCapability;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An immutable point-in-time snapshot of a player's extended inventory contents.
 *
 * <p>Used as the rollback target inside {@link InventoryTransactionService}.
 * The snapshot captures item stacks by deep-copying them, ensuring that subsequent
 * mutations to the live {@link net.minecraftforge.items.ItemStackHandler} do not
 * affect the stored copies.</p>
 *
 * <h2>Transaction lifecycle</h2>
 * <ol>
 *   <li>Call {@link #take(IExtendedInventoryCapability)} before any mutation.</li>
 *   <li>Perform the dry-run or real mutations.</li>
 *   <li>On failure call {@link #restore(IExtendedInventoryCapability)} to roll back.</li>
 * </ol>
 *
 * <p>The snapshot also captures the {@code revision} and {@code dirty} flag so that
 * a successful rollback returns the capability to its exact pre-transaction state,
 * including the revision counter.</p>
 */
public final class InventorySnapshot {

    /** Deep copies of each slot, indexed by slot index. */
    private final List<ItemStack> items;
    /** Revision counter at snapshot time. */
    private final long revision;
    /** Dirty flag at snapshot time. */
    private final boolean dirty;
    /** Layout version at snapshot time. */
    private final String layoutVersion;

    private InventorySnapshot(@NotNull List<ItemStack> items, long revision,
                               boolean dirty, @NotNull String layoutVersion) {
        this.items = Collections.unmodifiableList(items);
        this.revision = revision;
        this.dirty = dirty;
        this.layoutVersion = layoutVersion;
    }

    // ── Factory ─────────────────────────────────────────────────────────────

    /**
     * Takes a snapshot of the current capability state.
     *
     * <p>Must be called on the <strong>server thread</strong>.</p>
     *
     * @param cap the capability to snapshot
     * @return a new, immutable snapshot
     */
    @NotNull
    public static InventorySnapshot take(@NotNull IExtendedInventoryCapability cap) {
        int size = cap.getInventory().getSlots();
        List<ItemStack> copies = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            copies.add(cap.getInventory().getStackInSlot(i).copy());
        }
        return new InventorySnapshot(copies, cap.getRevision(), cap.isDirty(), cap.getLayoutVersion());
    }

    // ── Restore ─────────────────────────────────────────────────────────────

    /**
     * Restores the capability to the state captured in this snapshot.
     *
     * <p>Uses {@link IExtendedInventoryCapability#restoreFromSnapshot} which suppresses
     * dirty tracking, so the rollback does not increment the revision counter.</p>
     *
     * @param cap the capability to restore
     */
    public void restore(@NotNull IExtendedInventoryCapability cap) {
        cap.restoreFromSnapshot(items, revision, dirty, layoutVersion);
    }

    // ── Accessors ───────────────────────────────────────────────────────────

    /** @return slot count in this snapshot */
    public int size() { return items.size(); }

    /** @return deep-copied item stack at the given index */
    @NotNull
    public ItemStack getItem(int index) { return items.get(index).copy(); }

    /** @return revision counter at snapshot time */
    public long getRevision() { return revision; }

    /** @return dirty flag at snapshot time */
    public boolean isDirty() { return dirty; }

    /** @return layout version at snapshot time */
    @NotNull
    public String getLayoutVersion() { return layoutVersion; }
}
