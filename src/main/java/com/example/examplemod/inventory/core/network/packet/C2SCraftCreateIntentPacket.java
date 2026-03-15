package com.example.examplemod.inventory.core.network.packet;

import com.example.examplemod.Config;
import com.example.examplemod.inventory.core.capability.IExtendedInventoryCapability;
import com.example.examplemod.inventory.core.craft.CraftRecipeId;
import com.example.examplemod.inventory.core.craft.CraftRecipeService;
import com.example.examplemod.inventory.core.layout.EquipmentLayoutService;
import com.example.examplemod.inventory.core.network.InventoryNetworkHandler;
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

import java.util.function.Supplier;

/**
 * Craft-panel create intent from client.
 */
public final class C2SCraftCreateIntentPacket {

    private final long actionId;
    private final String recipeId;
    private final long expectedRevision;
    private final String expectedLayoutVersion;

    public C2SCraftCreateIntentPacket(long actionId,
                                      @NotNull String recipeId,
                                      long expectedRevision,
                                      @NotNull String expectedLayoutVersion) {
        this.actionId = actionId;
        this.recipeId = recipeId;
        this.expectedRevision = expectedRevision;
        this.expectedLayoutVersion = expectedLayoutVersion;
    }

    public static C2SCraftCreateIntentPacket create(@NotNull CraftRecipeId recipeId,
                                                    long expectedRevision,
                                                    @NotNull String expectedLayoutVersion) {
        return new C2SCraftCreateIntentPacket(
                System.nanoTime(),
                recipeId.id(),
                expectedRevision,
                expectedLayoutVersion);
    }

    public static void encode(@NotNull C2SCraftCreateIntentPacket msg, @NotNull FriendlyByteBuf buf) {
        buf.writeLong(msg.actionId);
        buf.writeUtf(msg.recipeId, 64);
        buf.writeLong(msg.expectedRevision);
        buf.writeUtf(msg.expectedLayoutVersion, 256);
    }

    @NotNull
    public static C2SCraftCreateIntentPacket decode(@NotNull FriendlyByteBuf buf) {
        long actionId = buf.readLong();
        String recipeId = buf.readUtf(64);
        long expectedRevision = buf.readLong();
        String expectedLayoutVersion = buf.readUtf(256);
        return new C2SCraftCreateIntentPacket(actionId, recipeId, expectedRevision, expectedLayoutVersion);
    }

    public static void handle(@NotNull C2SCraftCreateIntentPacket msg,
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

    private static void handleServer(@NotNull C2SCraftCreateIntentPacket msg, @NotNull ServerPlayer player) {
        if (msg.actionId <= 0L || !isValidRecipeId(msg.recipeId)) {
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
            InventoryNetworkHandler.sendToPlayer(player, new S2CCraftCreateResultPacket(
                    false,
                    msg.recipeId,
                    "screen.inventory.extended_inventory.craft.revision_conflict"));
            return;
        }

        CraftRecipeId recipeId = CraftRecipeId.fromId(msg.recipeId);
        if (recipeId == null) {
            InventoryNetworkHandler.sendToPlayer(player, new S2CCraftCreateResultPacket(
                    false,
                    msg.recipeId,
                    "screen.inventory.extended_inventory.craft.unknown_recipe"));
            return;
        }

        CraftRecipeService.RecipeDefinition recipe = CraftRecipeService.definition(recipeId);
        if (recipe == null) {
            InventoryNetworkHandler.sendToPlayer(player, new S2CCraftCreateResultPacket(
                    false,
                    msg.recipeId,
                    "screen.inventory.extended_inventory.craft.unknown_recipe"));
            return;
        }

        TransactionResult result = InventoryTransactionService.getInstance().craftRecipe(
                cap,
                profile,
                recipe.ingredients(),
                recipe.createOutput());

        if (result.isSuccess()) {
            InventorySyncService.getInstance().queuePartialSync(player, result.getDirtySlotIds());
            InventoryNetworkHandler.sendToPlayer(player, new S2CCraftCreateResultPacket(
                    true,
                    msg.recipeId,
                    "screen.inventory.extended_inventory.craft.success"));
            return;
        }

        InventoryNetworkHandler.sendToPlayer(player, new S2CCraftCreateResultPacket(
                false,
                msg.recipeId,
                "screen.inventory.extended_inventory.craft.missing_or_no_space"));
    }

    static boolean isValidRecipeId(@NotNull String recipeId) {
        return !recipeId.isBlank() && recipeId.length() <= 64 && CraftRecipeService.isKnownRecipeId(recipeId);
    }
}
