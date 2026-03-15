package com.example.examplemod.inventory.core;

import com.example.examplemod.inventory.core.model.InventoryNbtCodec;
import com.example.examplemod.inventory.core.model.PlayerInventoryRecord;
import com.example.examplemod.inventory.core.storage.ExtendedInventorySavedData;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class SavedDataRoundTripTest {
    @Test
    void savedDataPersistsAndLoadsByUuid() {
        ExtendedInventorySavedData source = new ExtendedInventorySavedData();
        UUID playerId = UUID.randomUUID();

        CompoundTag inventoryTag = new CompoundTag();
        inventoryTag.putString("marker", "m1");
        PlayerInventoryRecord record = new PlayerInventoryRecord(InventoryNbtCodec.CURRENT_SCHEMA_VERSION, 7L, false, inventoryTag);
        source.put(playerId, record);

        CompoundTag persisted = source.save(new CompoundTag());
        ExtendedInventorySavedData restored = ExtendedInventorySavedData.load(persisted);
        PlayerInventoryRecord loaded = restored.getOrCreate(playerId);

        Assertions.assertEquals(7L, loaded.revision());
        Assertions.assertFalse(loaded.dirty());
        Assertions.assertEquals("m1", loaded.inventoryTag().getString("marker"));
    }
}

