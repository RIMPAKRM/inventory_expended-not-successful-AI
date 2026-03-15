package com.example.examplemod.inventory.core;

import com.example.examplemod.inventory.core.model.InventoryNbtCodec;
import com.example.examplemod.inventory.core.model.PlayerInventoryRecord;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class InventoryNbtCodecTest {

    @Test
    void roundTripPreservesSchemaRevisionAndDirty() {
        CompoundTag inventory = new CompoundTag();
        inventory.putInt("size", 12);

        PlayerInventoryRecord input = new PlayerInventoryRecord(
                InventoryNbtCodec.CURRENT_SCHEMA_VERSION, 5L, true, inventory);
        CompoundTag serialized = InventoryNbtCodec.write(input);
        PlayerInventoryRecord decoded = InventoryNbtCodec.read(serialized);

        Assertions.assertEquals(InventoryNbtCodec.CURRENT_SCHEMA_VERSION, decoded.schemaVersion());
        Assertions.assertEquals(5L, decoded.revision());
        Assertions.assertTrue(decoded.dirty());
        Assertions.assertEquals(12, decoded.inventoryTag().getInt("size"));
    }

    @Test
    void roundTripPreservesLayoutVersion() {
        CompoundTag inventory = new CompoundTag();
        String token = "base:12|CHEST:myweaponmod:tactical_vest";

        PlayerInventoryRecord input = new PlayerInventoryRecord(
                InventoryNbtCodec.CURRENT_SCHEMA_VERSION, 3L, false, inventory, token);
        CompoundTag serialized = InventoryNbtCodec.write(input);
        PlayerInventoryRecord decoded = InventoryNbtCodec.read(serialized);

        Assertions.assertEquals(token, decoded.layoutVersion());
    }

    @Test
    void readV1RecordWithoutLayoutVersionDefaultsToEmpty() {
        // Simulate a V1 (M1) record that has no layoutVersion field
        CompoundTag tag = new CompoundTag();
        tag.putInt("schemaVersion", 1);
        tag.putLong("revision", 2L);
        tag.putBoolean("dirty", false);
        tag.put("inventory", new CompoundTag());
        // No "layoutVersion" key present

        PlayerInventoryRecord record = InventoryNbtCodec.read(tag);

        Assertions.assertEquals("", record.layoutVersion(),
                "Pre-M2 records should default to empty layoutVersion");
    }

    @Test
    void emptyRecordHasCurrentSchemaVersion() {
        PlayerInventoryRecord empty = InventoryNbtCodec.emptyRecord();
        Assertions.assertEquals(InventoryNbtCodec.CURRENT_SCHEMA_VERSION, empty.schemaVersion());
        Assertions.assertEquals("", empty.layoutVersion());
        Assertions.assertFalse(empty.dirty());
        Assertions.assertEquals(0L, empty.revision());
    }
}


