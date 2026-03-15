package com.example.examplemod.inventory.core.sync;

import com.example.examplemod.Config;
import com.example.examplemod.inventory.core.capability.IExtendedInventoryCapability;
import com.example.examplemod.inventory.core.layout.EquipmentLayoutService;
import com.example.examplemod.inventory.core.model.InventoryNbtCodec;
import com.example.examplemod.inventory.core.network.InventoryNetworkHandler;
import com.example.examplemod.inventory.core.network.packet.S2CFullInventorySyncPacket;
import com.example.examplemod.inventory.core.network.packet.S2CPartialInventorySyncPacket;
import com.example.examplemod.inventory.core.slot.PlayerLayoutProfile;
import com.example.examplemod.inventory.core.slot.SlotDefinition;
import com.example.examplemod.inventory.core.storage.PlayerInventoryPersistenceService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Event-driven sync coordinator for M4.
 */
public final class InventorySyncService {

    private static final InventorySyncService INSTANCE =
            new InventorySyncService(new InventorySyncOrchestrator(Config.getPartialSyncSlotLimit()));

    private final InventorySyncOrchestrator orchestrator;

    InventorySyncService(@NotNull InventorySyncOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public static InventorySyncService getInstance() {
        return INSTANCE;
    }

    public void requestFullSync(@NotNull ServerPlayer player, @NotNull SyncTrigger trigger) {
        orchestrator.requestFull(player.getUUID(), trigger);
    }

    public void queuePartialSync(@NotNull ServerPlayer player, @NotNull List<String> dirtySlotIds) {
        orchestrator.queuePartial(player.getUUID(), dirtySlotIds);
    }

    public void clearPending(@NotNull UUID playerId) {
        orchestrator.clear(playerId);
    }

    public void flushAll(@NotNull MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            flushPlayer(player);
        }
    }

    public void flushPlayer(@NotNull ServerPlayer player) {
        InventorySyncOrchestrator.SyncDecision decision = orchestrator.drain(player.getUUID());
        if (decision.getType() == InventorySyncOrchestrator.SyncDecision.Type.NONE) {
            return;
        }

        IExtendedInventoryCapability cap = PlayerInventoryPersistenceService.requireCapability(player);
        PlayerLayoutProfile profile = EquipmentLayoutService.getInstance().getOrBuild(player);

        if (decision.isFull()) {
            sendFull(player, cap, profile, decision.getFullTrigger());
            return;
        }

        Map<String, ItemStack> updates = new LinkedHashMap<>();
        for (String slotId : decision.getDirtySlotIds()) {
            int slotIndex = profile.getSlotIndex(slotId);
            if (slotIndex < 0) {
                sendFull(player, cap, profile, SyncTrigger.LAYOUT_CHANGED);
                return;
            }
            updates.put(slotId, cap.getInventory().getStackInSlot(slotIndex).copy());
        }

        if (updates.isEmpty()) {
            return;
        }

        S2CPartialInventorySyncPacket packet = new S2CPartialInventorySyncPacket(
                InventoryNbtCodec.CURRENT_SCHEMA_VERSION,
                cap.getRevision(),
                profile.getLayoutToken(),
                updates);

        InventoryNetworkHandler.sendToPlayer(player, packet);
    }

    private void sendFull(@NotNull ServerPlayer player,
                          @NotNull IExtendedInventoryCapability cap,
                          @NotNull PlayerLayoutProfile profile,
                          @NotNull SyncTrigger trigger) {
        List<String> orderedSlotIds = profile.getAllSlots().stream()
                .map(SlotDefinition::getSlotId)
                .toList();

        S2CFullInventorySyncPacket packet = new S2CFullInventorySyncPacket(
                InventoryNbtCodec.CURRENT_SCHEMA_VERSION,
                cap.getRevision(),
                profile.getLayoutToken(),
                cap.getInventory().serializeNBT(),
                orderedSlotIds,
                trigger.name());

        InventoryNetworkHandler.sendToPlayer(player, packet);
    }
}

