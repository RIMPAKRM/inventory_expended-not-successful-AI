package com.example.examplemod.inventory.core.capability;

import com.example.examplemod.inventory.core.model.PlayerInventoryRecord;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Runtime capability attached to every {@link net.minecraft.world.entity.player.Player} entity.
 *
 * <p>This is the primary server-side access point for reading and writing the player's
 * extended inventory. All mutations must happen on the <strong>server thread</strong>.</p>
 *
 * <h2>Key contracts</h2>
 * <ul>
 *   <li>The server is the single source of truth — clients never write directly.</li>
 *   <li>{@link #markDirty()} bumps the revision counter atomically.</li>
 *   <li>{@link #toRecord()} / {@link #loadFromRecord(PlayerInventoryRecord)} are the only
 *       serialisation entry points used by persistence and sync code.</li>
 *   <li>{@link #getLayoutVersion()} stores the layout token that was active when the
 *       inventory was last persisted; used at load time to detect stale layouts (M2+).</li>
 * </ul>
 *
 * <p>External mods should depend only on this interface, never on the implementation class.</p>
 */
@AutoRegisterCapability
public interface IExtendedInventoryCapability {

    /**
     * @return the underlying item storage handler; prefer higher-level methods where possible
     */
    ItemStackHandler getInventory();

    /**
     * @return monotonically increasing revision counter; bumped on every content change
     */
    long getRevision();

    /**
     * @return {@code true} if the inventory has changes that have not yet been persisted
     */
    boolean isDirty();

    /** Marks the inventory dirty and increments the revision counter. */
    void markDirty();

    /** Clears the dirty flag without changing the revision counter. */
    void clearDirty();

    /**
     * Snapshots the current capability state into an immutable data record.
     *
     * @return a new {@link PlayerInventoryRecord} capturing current state
     */
    PlayerInventoryRecord toRecord();

    /**
     * Restores capability state from a previously persisted record.
     *
     * @param record the record to restore from; must not be {@code null}
     */
    void loadFromRecord(@NotNull PlayerInventoryRecord record);

    /**
     * Serialises the full capability state to NBT for Forge capability save/load.
     *
     * @return a new {@link CompoundTag} representing the full capability state
     */
    CompoundTag serializeCapabilityNbt();

    /**
     * Deserialises the full capability state from NBT.
     *
     * @param nbt the tag previously produced by {@link #serializeCapabilityNbt()}
     */
    void deserializeCapabilityNbt(@NotNull CompoundTag nbt);

    /**
     * Returns the layout version token that was active when this capability was last
     * loaded from persistence.
     *
     * <p>An empty string indicates a pre-M2 record or a capability that has not yet
     * been persisted in an M2+ world.</p>
     *
     * @return non-null layout version token
     */
    @NotNull
    default String getLayoutVersion() { return ""; }

    /**
     * Updates the stored layout version token.
     *
     * <p>Called by the persistence service after a successful load to record which
     * layout configuration was active at load time. This allows the layout service
     * to detect a mismatch on the next login and trigger a resize/migration.</p>
     *
     * @param layoutVersion the current layout token; must not be {@code null}
     */
    default void setLayoutVersion(@NotNull String layoutVersion) { /* no-op for legacy impls */ }

    /**
     * Atomically restores the capability to a prior state captured by
     * {@link com.example.examplemod.inventory.core.transaction.InventorySnapshot}.
     *
     * <p>This method MUST suppress the dirty/revision tracking during slot restoration
     * so that the rollback does not increment the revision counter. After restoring
     * slot contents the method must set {@code revision} and {@code dirty} to the
     * values supplied.</p>
     *
     * <p>Called exclusively by {@link com.example.examplemod.inventory.core.transaction.InventorySnapshot#restore}
     * on the <strong>server thread</strong>.</p>
     *
     * @param items          slot contents to restore (index-aligned with the handler)
     * @param snapshotRevision revision value at snapshot time
     * @param snapshotDirty  dirty flag at snapshot time
     * @param snapshotLayoutVersion layout version at snapshot time
     */
    default void restoreFromSnapshot(@NotNull java.util.List<net.minecraft.world.item.ItemStack> items,
                                      long snapshotRevision,
                                      boolean snapshotDirty,
                                      @NotNull String snapshotLayoutVersion) {
        // Default: use loadFromRecord as a fallback (NBT round-trip, less efficient)
        net.minecraft.nbt.CompoundTag tag = getInventory().serializeNBT();
        // restore items into the tag by re-serialising a fresh handler snapshot
        net.minecraftforge.items.ItemStackHandler tmp =
                new net.minecraftforge.items.ItemStackHandler(items.size());
        for (int i = 0; i < items.size(); i++) {
            tmp.setStackInSlot(i, items.get(i).copy());
        }
        tag = tmp.serializeNBT();
        loadFromRecord(new com.example.examplemod.inventory.core.model.PlayerInventoryRecord(
                com.example.examplemod.inventory.core.model.InventoryNbtCodec.CURRENT_SCHEMA_VERSION,
                snapshotRevision, snapshotDirty, tag, snapshotLayoutVersion));
    }
}
