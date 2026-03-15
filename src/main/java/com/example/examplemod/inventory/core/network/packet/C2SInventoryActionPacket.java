package com.example.examplemod.inventory.core.network.packet;

import com.example.examplemod.Config;
import com.example.examplemod.inventory.core.capability.IExtendedInventoryCapability;
import com.example.examplemod.inventory.core.layout.EquipmentLayoutService;
import com.example.examplemod.inventory.core.network.guard.InventoryActionFloodGuard;
import com.example.examplemod.inventory.core.slot.PlayerLayoutProfile;
import com.example.examplemod.inventory.core.storage.PlayerInventoryPersistenceService;
import com.example.examplemod.inventory.core.sync.InventorySyncService;
import com.example.examplemod.inventory.core.sync.SyncTrigger;
import com.example.examplemod.inventory.core.transaction.InventoryTransactionService;
import com.example.examplemod.inventory.core.transaction.TransactionResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Client intent packet. The server validates all fields and executes via transaction service.
 */
public final class C2SInventoryActionPacket {

    public enum ActionType {
        MOVE,
        SWAP,
        SPLIT,
        QUICK_MOVE,
        QUICK_MOVE_VANILLA,
        MOVE_VANILLA_TO_MOD,
        MOVE_MOD_TO_VANILLA,
        EXTRACT
    }

    private final long actionId;
    private final ActionType actionType;
    private final String primarySlotId;
    @Nullable
    private final String secondarySlotId;
    private final long expectedRevision;
    private final String expectedLayoutVersion;

    public C2SInventoryActionPacket(long actionId,
                                    @NotNull ActionType actionType,
                                    @NotNull String primarySlotId,
                                    @Nullable String secondarySlotId,
                                    long expectedRevision,
                                    @NotNull String expectedLayoutVersion) {
        this.actionId = actionId;
        this.actionType = actionType;
        this.primarySlotId = primarySlotId;
        this.secondarySlotId = secondarySlotId;
        this.expectedRevision = expectedRevision;
        this.expectedLayoutVersion = expectedLayoutVersion;
    }

    public static C2SInventoryActionPacket move(@NotNull String fromSlotId,
                                                @NotNull String toSlotId,
                                                long expectedRevision,
                                                @NotNull String expectedLayoutVersion) {
        return new C2SInventoryActionPacket(System.nanoTime(), ActionType.MOVE, fromSlotId, toSlotId,
                expectedRevision, expectedLayoutVersion);
    }

    public static C2SInventoryActionPacket swap(@NotNull String fromSlotId,
                                                @NotNull String toSlotId,
                                                long expectedRevision,
                                                @NotNull String expectedLayoutVersion) {
        return new C2SInventoryActionPacket(System.nanoTime(), ActionType.SWAP, fromSlotId, toSlotId,
                expectedRevision, expectedLayoutVersion);
    }

    public static C2SInventoryActionPacket extract(@NotNull String slotId,
                                                   long expectedRevision,
                                                   @NotNull String expectedLayoutVersion) {
        return new C2SInventoryActionPacket(System.nanoTime(), ActionType.EXTRACT, slotId, null,
                expectedRevision, expectedLayoutVersion);
    }

    public static C2SInventoryActionPacket split(@NotNull String slotId,
                                                 long expectedRevision,
                                                 @NotNull String expectedLayoutVersion) {
        return new C2SInventoryActionPacket(System.nanoTime(), ActionType.SPLIT, slotId, null,
                expectedRevision, expectedLayoutVersion);
    }

    public static C2SInventoryActionPacket quickMove(@NotNull String slotId,
                                                     long expectedRevision,
                                                     @NotNull String expectedLayoutVersion) {
        return new C2SInventoryActionPacket(System.nanoTime(), ActionType.QUICK_MOVE, slotId, null,
                expectedRevision, expectedLayoutVersion);
    }

    public static C2SInventoryActionPacket quickMoveVanilla(int playerInventorySlot,
                                                            long expectedRevision,
                                                            @NotNull String expectedLayoutVersion) {
        return new C2SInventoryActionPacket(System.nanoTime(), ActionType.QUICK_MOVE_VANILLA,
                "vanilla:" + playerInventorySlot, null, expectedRevision, expectedLayoutVersion);
    }

    public static C2SInventoryActionPacket moveVanillaToMod(int playerInventorySlot,
                                                            @NotNull String targetSlotId,
                                                            long expectedRevision,
                                                            @NotNull String expectedLayoutVersion) {
        return new C2SInventoryActionPacket(System.nanoTime(), ActionType.MOVE_VANILLA_TO_MOD,
                "vanilla:" + playerInventorySlot, targetSlotId, expectedRevision, expectedLayoutVersion);
    }

    public static C2SInventoryActionPacket moveToVanilla(@NotNull String slotId,
                                                          long expectedRevision,
                                                          @NotNull String expectedLayoutVersion) {
        return new C2SInventoryActionPacket(System.nanoTime(), ActionType.MOVE_MOD_TO_VANILLA,
                slotId, null, expectedRevision, expectedLayoutVersion);
    }

    public static void encode(@NotNull C2SInventoryActionPacket msg, @NotNull FriendlyByteBuf buf) {
        buf.writeLong(msg.actionId);
        buf.writeEnum(msg.actionType);
        buf.writeUtf(msg.primarySlotId, 128);
        buf.writeBoolean(msg.secondarySlotId != null);
        if (msg.secondarySlotId != null) {
            buf.writeUtf(msg.secondarySlotId, 128);
        }
        buf.writeLong(msg.expectedRevision);
        buf.writeUtf(msg.expectedLayoutVersion, 256);
    }

    @NotNull
    public static C2SInventoryActionPacket decode(@NotNull FriendlyByteBuf buf) {
        long actionId = buf.readLong();
        ActionType type = buf.readEnum(ActionType.class);
        String primary = buf.readUtf(128);
        String secondary = buf.readBoolean() ? buf.readUtf(128) : null;
        long expectedRevision = buf.readLong();
        String expectedLayoutVersion = buf.readUtf(256);
        return new C2SInventoryActionPacket(actionId, type, primary, secondary, expectedRevision, expectedLayoutVersion);
    }

    public static void handle(@NotNull C2SInventoryActionPacket msg,
                              @NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();

        context.enqueueWork(() -> {
            if (player == null) {
                return;
            }
            handleServer(msg, player);
        });
        context.setPacketHandled(true);
    }

    private static void handleServer(@NotNull C2SInventoryActionPacket msg, @NotNull ServerPlayer player) {
        if (msg.actionId <= 0L || !isValidSlotId(msg.primarySlotId)) {
            return;
        }
        if ((msg.actionType == ActionType.MOVE
                || msg.actionType == ActionType.SWAP
                || msg.actionType == ActionType.MOVE_VANILLA_TO_MOD)
                && !isValidSlotId(msg.secondarySlotId)) {
            return;
        }

        long tick = player.serverLevel().getGameTime();
        boolean accepted = InventoryActionFloodGuard.getInstance().tryAcquire(
                player.getUUID(),
                "c2s",
                tick,
                Config.getC2sActionsPerTickLimit());
        if (!accepted) {
            return;
        }

        IExtendedInventoryCapability cap = PlayerInventoryPersistenceService.requireCapability(player);
        PlayerLayoutProfile profile = EquipmentLayoutService.getInstance().getOrBuild(player);

        if (msg.expectedRevision != cap.getRevision()
                || !msg.expectedLayoutVersion.equals(profile.getLayoutToken())) {
            InventorySyncService.getInstance().requestFullSync(player, SyncTrigger.REVISION_CONFLICT);
            return;
        }

        TransactionResult result;
        InventoryTransactionService tx = InventoryTransactionService.getInstance();

        if (msg.actionType == ActionType.MOVE) {
            result = tx.moveItem(cap, profile, msg.primarySlotId, msg.secondarySlotId);
        } else if (msg.actionType == ActionType.SWAP) {
            result = tx.swapItems(cap, profile, msg.primarySlotId, msg.secondarySlotId);
        } else if (msg.actionType == ActionType.SPLIT) {
            result = tx.splitStack(cap, profile, msg.primarySlotId);
        } else if (msg.actionType == ActionType.QUICK_MOVE) {
            result = tx.quickMoveItem(cap, profile, msg.primarySlotId);
        } else if (msg.actionType == ActionType.QUICK_MOVE_VANILLA) {
            int slotIndex = parseVanillaSlotIndex(msg.primarySlotId);
            if (!isVanillaQuickMoveSlotAllowed(slotIndex, player.isCreative())) {
                return;
            }
            result = tx.quickMoveFromVanillaSlot(player, cap, profile, slotIndex);
        } else if (msg.actionType == ActionType.MOVE_VANILLA_TO_MOD) {
            int slotIndex = parseVanillaSlotIndex(msg.primarySlotId);
            if (!isVanillaQuickMoveSlotAllowed(slotIndex, player.isCreative()) || msg.secondarySlotId == null) {
                return;
            }
            result = tx.moveVanillaToExtendedSlot(player, cap, profile, slotIndex, msg.secondarySlotId);
        } else if (msg.actionType == ActionType.MOVE_MOD_TO_VANILLA) {
            result = tx.moveExtendedToVanilla(player, cap, profile, msg.primarySlotId, player.isCreative());
        } else {
            result = tx.extractFromSlot(cap, profile, msg.primarySlotId);
        }

        if (result.isSuccess()) {
            InventorySyncService.getInstance().queuePartialSync(player, result.getDirtySlotIds());
        }
    }

    static boolean isVanillaQuickMoveSlotAllowed(int slotIndex, boolean creativeMode) {
        if (slotIndex < 0 || slotIndex >= 36) {
            return false;
        }
        if (creativeMode) {
            return true;
        }
        return slotIndex >= 0 && slotIndex <= 8;
    }

    private static int parseVanillaSlotIndex(@NotNull String encoded) {
        if (!encoded.startsWith("vanilla:")) {
            return -1;
        }
        try {
            return Integer.parseInt(encoded.substring("vanilla:".length()));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static boolean isValidSlotId(@Nullable String slotId) {
        return slotId != null && !slotId.isBlank() && slotId.length() <= 128;
    }
}
