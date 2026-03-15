package com.example.examplemod.inventory.core.network.packet;

import com.example.examplemod.inventory.core.network.client.ClientInventorySyncState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Full inventory state push from server to client.
 */
public final class S2CFullInventorySyncPacket {

    private final int schemaVersion;
    private final long revision;
    private final String layoutVersion;
    private final CompoundTag inventoryTag;
    private final List<String> orderedSlotIds;
    private final String reason;

    public S2CFullInventorySyncPacket(int schemaVersion,
                                      long revision,
                                      @NotNull String layoutVersion,
                                      @NotNull CompoundTag inventoryTag,
                                      @NotNull List<String> orderedSlotIds,
                                      @NotNull String reason) {
        this.schemaVersion = schemaVersion;
        this.revision = revision;
        this.layoutVersion = layoutVersion;
        this.inventoryTag = inventoryTag;
        this.orderedSlotIds = List.copyOf(orderedSlotIds);
        this.reason = reason;
    }

    public static void encode(@NotNull S2CFullInventorySyncPacket msg, @NotNull FriendlyByteBuf buf) {
        buf.writeVarInt(msg.schemaVersion);
        buf.writeLong(msg.revision);
        buf.writeUtf(msg.layoutVersion, 256);
        buf.writeNbt(msg.inventoryTag);
        buf.writeVarInt(msg.orderedSlotIds.size());
        for (String slotId : msg.orderedSlotIds) {
            buf.writeUtf(slotId, 128);
        }
        buf.writeUtf(msg.reason, 64);
    }

    @NotNull
    public static S2CFullInventorySyncPacket decode(@NotNull FriendlyByteBuf buf) {
        int schemaVersion = buf.readVarInt();
        long revision = buf.readLong();
        String layoutVersion = buf.readUtf(256);
        CompoundTag inventoryTag = buf.readNbt();
        if (inventoryTag == null) {
            inventoryTag = new CompoundTag();
        }

        int size = Math.max(0, buf.readVarInt());
        List<String> slotIds = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            slotIds.add(buf.readUtf(128));
        }
        String reason = buf.readUtf(64);

        return new S2CFullInventorySyncPacket(schemaVersion, revision, layoutVersion, inventoryTag, slotIds, reason);
    }

    public static void handle(@NotNull S2CFullInventorySyncPacket msg,
                              @NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientInventorySyncState.applyFull(msg));
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
    public CompoundTag inventoryTag() {
        return inventoryTag;
    }

    @NotNull
    public List<String> orderedSlotIds() {
        return orderedSlotIds;
    }

    @NotNull
    public String reason() {
        return reason;
    }
}

