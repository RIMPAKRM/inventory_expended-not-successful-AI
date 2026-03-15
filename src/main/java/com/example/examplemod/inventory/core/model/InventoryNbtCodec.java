package com.example.examplemod.inventory.core.model;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * NBT serialisation codec for {@link PlayerInventoryRecord}.
 *
 * <h2>Schema version history</h2>
 * <ul>
 *   <li>Version 1 (M1): schemaVersion, revision, dirty, inventory.</li>
 *   <li>Version 2 (M2): adds {@code layoutVersion} string field.</li>
 * </ul>
 *
 * <p>When reading an older record, missing fields are filled in with safe defaults so
 * existing saves are never corrupted on upgrade.</p>
 */
public final class InventoryNbtCodec {
    /** Current NBT schema version written by this codec. Bump when format changes. */
    public static final int CURRENT_SCHEMA_VERSION = 2;

    private static final String KEY_SCHEMA_VERSION = "schemaVersion";
    private static final String KEY_REVISION = "revision";
    private static final String KEY_DIRTY = "dirty";
    private static final String KEY_INVENTORY = "inventory";
    /** Added in schema version 2 (M2). */
    private static final String KEY_LAYOUT_VERSION = "layoutVersion";

    private InventoryNbtCodec() {
    }

    /**
     * Serialises a {@link PlayerInventoryRecord} into a {@link CompoundTag}.
     *
     * @param record the record to serialise; must not be {@code null}
     * @return a new {@link CompoundTag} containing all record fields
     */
    public static CompoundTag write(PlayerInventoryRecord record) {
        CompoundTag out = new CompoundTag();
        out.putInt(KEY_SCHEMA_VERSION, CURRENT_SCHEMA_VERSION);
        out.putLong(KEY_REVISION, record.revision());
        out.putBoolean(KEY_DIRTY, record.dirty());
        out.put(KEY_INVENTORY, record.inventoryTag());
        out.putString(KEY_LAYOUT_VERSION, record.layoutVersion());
        return out;
    }

    /**
     * Deserialises a {@link PlayerInventoryRecord} from a {@link CompoundTag}.
     *
     * <p>Missing fields are filled with safe defaults to support migration from
     * older schema versions.</p>
     *
     * @param in the source tag; must not be {@code null}
     * @return the deserialised record
     */
    public static PlayerInventoryRecord read(CompoundTag in) {
        int schemaVersion = in.contains(KEY_SCHEMA_VERSION, Tag.TAG_INT)
                ? in.getInt(KEY_SCHEMA_VERSION)
                : 1; // treat missing as schema v1 (pre-M2)
        long revision = in.contains(KEY_REVISION, Tag.TAG_LONG) ? in.getLong(KEY_REVISION) : 0L;
        boolean dirty = in.contains(KEY_DIRTY, Tag.TAG_BYTE) && in.getBoolean(KEY_DIRTY);

        CompoundTag inventoryTag = in.contains(KEY_INVENTORY, Tag.TAG_COMPOUND)
                ? in.getCompound(KEY_INVENTORY)
                : new CompoundTag();

        // layoutVersion was introduced in schema v2; default empty string for older records
        String layoutVersion = in.contains(KEY_LAYOUT_VERSION, Tag.TAG_STRING)
                ? in.getString(KEY_LAYOUT_VERSION)
                : "";

        return new PlayerInventoryRecord(schemaVersion, revision, dirty, inventoryTag, layoutVersion);
    }

    /**
     * Returns an empty record with current schema version and empty layout version.
     *
     * @return a fresh, empty {@link PlayerInventoryRecord}
     */
    public static PlayerInventoryRecord emptyRecord() {
        return new PlayerInventoryRecord(CURRENT_SCHEMA_VERSION, 0L, false, new CompoundTag(), "");
    }
}

