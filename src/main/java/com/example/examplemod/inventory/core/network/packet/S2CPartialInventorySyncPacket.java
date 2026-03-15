package com.example.examplemod.inventory.core.network.packet;

import com.example.examplemod.inventory.core.network.client.ClientInventorySyncState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Partial inventory delta push from server to client.
 */
public final class S2CPartialInventorySyncPacket {

    private final int schemaVersion;
    private final long revision;
    private final String layoutVersion;
    private final Map<String, ItemStack> slotUpdates;

    public S2CPartialInventorySyncPacket(int schemaVersion,
                                         long revision,
                                         @NotNull String layoutVersion,
                                         @NotNull Map<String, ItemStack> slotUpdates) {
        this.schemaVersion = schemaVersion;
        this.revision = revision;
        this.layoutVersion = layoutVersion;
        this.slotUpdates = Map.copyOf(slotUpdates);
    }

    public static void encode(@NotNull S2CPartialInventorySyncPacket msg, @NotNull FriendlyByteBuf buf) {
        buf.writeVarInt(msg.schemaVersion);
        buf.writeLong(msg.revision);
        buf.writeUtf(msg.layoutVersion, 256);
        buf.writeVarInt(msg.slotUpdates.size());
        for (Map.Entry<String, ItemStack> entry : msg.slotUpdates.entrySet()) {
            buf.writeUtf(entry.getKey(), 128);
            buf.writeItem(entry.getValue());
        }
    }

    @NotNull
    public static S2CPartialInventorySyncPacket decode(@NotNull FriendlyByteBuf buf) {
        int schemaVersion = buf.readVarInt();
        long revision = buf.readLong();
        String layoutVersion = buf.readUtf(256);
        int size = Math.max(0, buf.readVarInt());

        Map<String, ItemStack> updates = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            String slotId = buf.readUtf(128);
            ItemStack stack = buf.readItem();
            updates.put(slotId, stack);
        }

        return new S2CPartialInventorySyncPacket(schemaVersion, revision, layoutVersion, updates);
    }

    public static void handle(@NotNull S2CPartialInventorySyncPacket msg,
                              @NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientInventorySyncState.applyPartial(msg));
        context.setPacketHandled(true);
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public long revision() {
        return revision;
    }

    @NotNull
    public String layoutVersion() {
        return layoutVersion;
    }

    @NotNull
    public Map<String, ItemStack> slotUpdates() {
        return slotUpdates;
    }
}

