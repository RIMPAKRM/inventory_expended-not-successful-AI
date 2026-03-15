package com.example.examplemod.inventory.core.capability;

import com.example.examplemod.inventory.core.model.InventoryNbtCodec;
import com.example.examplemod.inventory.core.model.PlayerInventoryRecord;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Default implementation of {@link IExtendedInventoryCapability}.
 *
 * <p>Holds a runtime {@link ItemStackHandler} whose size is set at construction.
 * The production code passes {@code Config.getBaseRuntimeSlots()} from
 * {@link ExtendedInventoryProvider}; tests pass an explicit count to avoid
 * triggering the Minecraft/Forge bootstrap via {@code Config}'s static initialiser.</p>
 *
 * <p>All mutations auto-increment the {@link #revision} counter and set the
 * {@link #dirty} flag via the overridden {@code onContentsChanged()} hook, unless
 * {@link #suppressTracking} is active during bulk load operations.</p>
 */
public class ExtendedInventoryCapability implements IExtendedInventoryCapability {
    private final ItemStackHandler inventory;
    private long revision;
    private boolean dirty;
    private boolean suppressTracking;
    /** Stores the layout token active at last load/persist. Empty = pre-M2. */
    private String layoutVersion = "";

    /**
     * Creates a capability with the given initial slot count.
     *
     * <p>In production, call this from {@link ExtendedInventoryProvider} passing
     * {@code Config.getBaseRuntimeSlots()}. In unit tests, pass any positive integer.</p>
     *
     * @param slotCount number of slots to allocate (must be &gt;= 1)
     */
    public ExtendedInventoryCapability(int slotCount) {
        this.inventory = new ItemStackHandler(Math.max(1, slotCount)) {
            @Override
            protected void onContentsChanged(int slot) {
                if (suppressTracking) {
                    return;
                }
                revision++;
                dirty = true;
            }
        };
        this.revision = 0L;
        this.dirty = false;
    }

    @Override
    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Override
    public long getRevision() {
        return revision;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void markDirty() {
        revision++;
        dirty = true;
    }

    @Override
    public void clearDirty() {
        dirty = false;
    }

    @Override
    @NotNull
    public String getLayoutVersion() {
        return layoutVersion;
    }

    @Override
    public void setLayoutVersion(@NotNull String layoutVersion) {
        this.layoutVersion = layoutVersion == null ? "" : layoutVersion;
    }

    @Override
    public PlayerInventoryRecord toRecord() {
        CompoundTag inventoryTag = inventory.serializeNBT();
        return new PlayerInventoryRecord(
                InventoryNbtCodec.CURRENT_SCHEMA_VERSION,
                revision,
                dirty,
                inventoryTag,
                layoutVersion);
    }

    @Override
    public void loadFromRecord(PlayerInventoryRecord record) {
        revision = record.revision();
        dirty = record.dirty();
        layoutVersion = record.layoutVersion();

        suppressTracking = true;
        inventory.deserializeNBT(record.inventoryTag());
        suppressTracking = false;
    }

    @Override
    public void restoreFromSnapshot(@NotNull java.util.List<net.minecraft.world.item.ItemStack> items,
                                     long snapshotRevision, boolean snapshotDirty,
                                     @NotNull String snapshotLayoutVersion) {
        suppressTracking = true;
        try {
            int restoreCount = Math.min(inventory.getSlots(), items.size());
            for (int i = 0; i < restoreCount; i++) {
                inventory.setStackInSlot(i, items.get(i).copy());
            }
            // Clear any slots beyond the snapshot size (e.g. after a layout resize)
            for (int i = restoreCount; i < inventory.getSlots(); i++) {
                inventory.setStackInSlot(i, net.minecraft.world.item.ItemStack.EMPTY);
            }
        } finally {
            suppressTracking = false;
        }
        this.revision = snapshotRevision;
        this.dirty = snapshotDirty;
        this.layoutVersion = snapshotLayoutVersion;
    }

    @Override
    public CompoundTag serializeCapabilityNbt() {
        return InventoryNbtCodec.write(toRecord());
    }

    @Override
    public void deserializeCapabilityNbt(CompoundTag nbt) {
        PlayerInventoryRecord record = InventoryNbtCodec.read(nbt);
        loadFromRecord(record);
    }
}

