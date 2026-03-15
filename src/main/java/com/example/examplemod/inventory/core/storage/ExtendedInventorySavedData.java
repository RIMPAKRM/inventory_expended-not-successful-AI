package com.example.examplemod.inventory.core.storage;

import com.example.examplemod.inventory.core.model.InventoryNbtCodec;
import com.example.examplemod.inventory.core.model.PlayerInventoryRecord;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExtendedInventorySavedData extends SavedData {
    public static final String DATA_NAME = "extended_inventory_data";

    private static final String KEY_PLAYERS = "players";
    private static final String KEY_UUID = "uuid";
    private static final String KEY_DATA = "data";

    private final Map<UUID, PlayerInventoryRecord> records = new HashMap<>();

    public static ExtendedInventorySavedData load(CompoundTag root) {
        ExtendedInventorySavedData data = new ExtendedInventorySavedData();

        ListTag players = root.getList(KEY_PLAYERS, Tag.TAG_COMPOUND);
        for (Tag playerTag : players) {
            CompoundTag entry = (CompoundTag) playerTag;
            if (!entry.hasUUID(KEY_UUID) || !entry.contains(KEY_DATA, Tag.TAG_COMPOUND)) {
                continue;
            }

            UUID playerId = entry.getUUID(KEY_UUID);
            PlayerInventoryRecord record = InventoryNbtCodec.read(entry.getCompound(KEY_DATA));
            data.records.put(playerId, record);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag players = new ListTag();
        for (Map.Entry<UUID, PlayerInventoryRecord> entry : records.entrySet()) {
            CompoundTag out = new CompoundTag();
            out.putUUID(KEY_UUID, entry.getKey());
            out.put(KEY_DATA, InventoryNbtCodec.write(entry.getValue()));
            players.add(out);
        }

        root.put(KEY_PLAYERS, players);
        return root;
    }

    public PlayerInventoryRecord getOrCreate(UUID playerId) {
        return records.getOrDefault(playerId, InventoryNbtCodec.emptyRecord());
    }

    public void put(UUID playerId, PlayerInventoryRecord record) {
        records.put(playerId, record);
        setDirty();
    }

    public boolean contains(UUID playerId) {
        return records.containsKey(playerId);
    }
}

