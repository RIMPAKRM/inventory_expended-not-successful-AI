package com.example.examplemod.inventory.core.model;

import net.minecraft.nbt.CompoundTag;

/**
 * Immutable data transfer record representing the full persisted state of a
 * player's extended inventory at a specific point in time.
 *
 * <p>{@code layoutVersion} is a compact string token that captures which equipment was
 * worn when this snapshot was taken. It is used at load time to detect whether the
 * current layout still matches the snapshot, so the runtime slot handler can be resized
 * if needed (e.g. after an item upgrade changed the number of contributed slots).</p>
 *
 * <p>All fields are immutable. {@link #inventoryTag()} returns a defensive copy to
 * prevent mutation of the stored tag from outside.</p>
 */
public final class PlayerInventoryRecord {
    private final int schemaVersion;
    private final long revision;
    private final boolean dirty;
    private final CompoundTag inventoryTag;
    /**
     * Layout token at the time this snapshot was created.
     * An empty string means "unknown / pre-M2 record".
     */
    private final String layoutVersion;

    /**
     * Full constructor including layout version (M2+).
     *
     * @param schemaVersion  NBT schema version (see {@link InventoryNbtCodec#CURRENT_SCHEMA_VERSION})
     * @param revision       monotonically increasing change counter
     * @param dirty          whether this record has unsaved changes
     * @param inventoryTag   serialised {@link net.minecraftforge.items.ItemStackHandler} NBT
     * @param layoutVersion  layout token string at snapshot time; empty = pre-M2 record
     */
    public PlayerInventoryRecord(int schemaVersion, long revision, boolean dirty,
                                 CompoundTag inventoryTag, String layoutVersion) {
        this.schemaVersion = schemaVersion;
        this.revision = revision;
        this.dirty = dirty;
        this.inventoryTag = inventoryTag.copy();
        this.layoutVersion = layoutVersion == null ? "" : layoutVersion;
    }

    /**
     * Legacy constructor without {@code layoutVersion} — treated as pre-M2 record.
     * Kept for backwards compatibility with existing tests and callers.
     */
    public PlayerInventoryRecord(int schemaVersion, long revision, boolean dirty,
                                 CompoundTag inventoryTag) {
        this(schemaVersion, revision, dirty, inventoryTag, "");
    }

    /** @return NBT schema version */
    public int schemaVersion() { return schemaVersion; }

    /** @return monotonically increasing change counter */
    public long revision() { return revision; }

    /** @return {@code true} if the record has unsaved changes */
    public boolean dirty() { return dirty; }

    /** @return a defensive copy of the serialised inventory NBT tag */
    public CompoundTag inventoryTag() { return inventoryTag.copy(); }

    /**
     * @return the layout token string at snapshot time;
     *         empty string for records created before M2
     */
    public String layoutVersion() { return layoutVersion; }
}

