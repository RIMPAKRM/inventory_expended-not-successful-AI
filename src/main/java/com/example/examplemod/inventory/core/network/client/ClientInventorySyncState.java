package com.example.examplemod.inventory.core.network.client;

import com.example.examplemod.inventory.core.network.packet.S2CFullInventorySyncPacket;
import com.example.examplemod.inventory.core.network.packet.S2CPartialInventorySyncPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight client mirror used by GUI code.
 *
 * <p>This state is updated only by S2C sync packets. Clients never mutate it directly.</p>
 */
public final class ClientInventorySyncState {

    private static int schemaVersion;
    private static long revision;
    private static String layoutVersion = "";
    private static List<String> slotOrder = List.of();
    private static Map<String, ItemStack> slots = Map.of();

    private ClientInventorySyncState() {
    }

    public static synchronized void applyFull(S2CFullInventorySyncPacket packet) {
        schemaVersion = packet.schemaVersion();
        revision = packet.revision();
        layoutVersion = packet.layoutVersion();
        slotOrder = List.copyOf(packet.orderedSlotIds());

        ItemStackHandler handler = new ItemStackHandler(slotOrder.size());
        handler.deserializeNBT(packet.inventoryTag());

        Map<String, ItemStack> next = new LinkedHashMap<>();
        for (int i = 0; i < slotOrder.size(); i++) {
            next.put(slotOrder.get(i), handler.getStackInSlot(i).copy());
        }
        slots = next;
    }

    public static synchronized void applyPartial(S2CPartialInventorySyncPacket packet) {
        // Ignore stale or incompatible deltas; server will eventually push a full sync.
        if (!layoutVersion.equals(packet.layoutVersion()) || packet.revision() < revision) {
            return;
        }

        Map<String, ItemStack> next = new LinkedHashMap<>(slots);
        for (Map.Entry<String, ItemStack> entry : packet.slotUpdates().entrySet()) {
            next.put(entry.getKey(), entry.getValue().copy());
        }

        slots = next;
        revision = packet.revision();
        schemaVersion = packet.schemaVersion();
    }

    public record Snapshot(int schemaVersion,
                           long revision,
                           @NotNull String layoutVersion,
                           @NotNull List<String> slotOrder,
                           @NotNull Map<String, ItemStack> slots) {
        public boolean hasUsableSnapshot() {
            return !layoutVersion.isBlank() && !slotOrder.isEmpty();
        }
    }

    public static synchronized Snapshot snapshot() {
        return new Snapshot(
                schemaVersion,
                revision,
                layoutVersion,
                new ArrayList<>(slotOrder),
                new LinkedHashMap<>(slots));
    }

    // Test/GUI read helpers
    public static synchronized int schemaVersion() {
        return schemaVersion;
    }

    public static synchronized long revision() {
        return revision;
    }

    public static synchronized String layoutVersion() {
        return layoutVersion;
    }

    public static synchronized List<String> slotOrder() {
        return new ArrayList<>(slotOrder);
    }

    public static synchronized Map<String, ItemStack> slots() {
        return new LinkedHashMap<>(slots);
    }

    public static synchronized boolean hasUsableSnapshot() {
        return !layoutVersion.isBlank() && !slotOrder.isEmpty();
    }

    public static synchronized void resetForTests() {
        schemaVersion = 0;
        revision = 0L;
        layoutVersion = "";
        slotOrder = List.of();
        slots = Map.of();
    }
}
