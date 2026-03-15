package com.example.examplemod.inventory.core.storage;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.inventory.core.capability.IExtendedInventoryCapability;
import com.example.examplemod.inventory.core.capability.ModCapabilities;
import com.example.examplemod.inventory.core.model.InventoryNbtCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

public final class PlayerInventoryPersistenceService {
    private PlayerInventoryPersistenceService() {
    }

    public static ExtendedInventorySavedData getData(ServerLevel serverLevel) {
        ServerLevel overworld = serverLevel.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is unavailable, cannot access " + ExampleMod.MODID + " saved data");
        }

        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(ExtendedInventorySavedData::load, ExtendedInventorySavedData::new, ExtendedInventorySavedData.DATA_NAME);
    }

    public static void loadIntoPlayer(@NotNull ServerPlayer player) {
        player.getCapability(ModCapabilities.EXTENDED_INVENTORY).ifPresent(capability -> {
            ExtendedInventorySavedData data = getData(player.serverLevel());
            capability.loadFromRecord(data.getOrCreate(player.getUUID()));
            capability.clearDirty();
        });
    }

    public static void saveFromPlayer(@NotNull ServerPlayer player) {
        player.getCapability(ModCapabilities.EXTENDED_INVENTORY).ifPresent(capability -> {
            ExtendedInventorySavedData data = getData(player.serverLevel());
            data.put(player.getUUID(), capability.toRecord());
            capability.clearDirty();
        });
    }

    public static IExtendedInventoryCapability requireCapability(ServerPlayer player) {
        return player.getCapability(ModCapabilities.EXTENDED_INVENTORY)
                .orElseThrow(() -> new IllegalStateException("Missing extended inventory capability for player " + player.getGameProfile().getName()));
    }

    public static void migrateFromLegacyPlaceholder(ServerPlayer player) {
        // TODO(M1-legacy-import): migrate data from legacy player NBT if a pre-M1 prototype exists.
        player.getCapability(ModCapabilities.EXTENDED_INVENTORY).ifPresent(capability -> {
            if (capability.getRevision() < 0) {
                capability.loadFromRecord(InventoryNbtCodec.emptyRecord());
            }
        });
    }
}
