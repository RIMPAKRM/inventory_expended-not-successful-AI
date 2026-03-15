package com.example.examplemod.inventory.core.network.packet;

import com.example.examplemod.Config;
import com.example.examplemod.inventory.core.layout.EquipmentLayoutService;
import com.example.examplemod.inventory.core.slot.PlayerLayoutProfile;
import com.example.examplemod.inventory.core.storage.PlayerInventoryPersistenceService;
import com.example.examplemod.inventory.menu.ExtendedInventoryMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Client request to open the extended inventory UI.
 */
public final class C2SOpenExtendedInventoryPacket {

    public static void encode(@NotNull C2SOpenExtendedInventoryPacket msg, @NotNull FriendlyByteBuf buf) {
    }

    @NotNull
    public static C2SOpenExtendedInventoryPacket decode(@NotNull FriendlyByteBuf buf) {
        return new C2SOpenExtendedInventoryPacket();
    }

    public static void handle(@NotNull C2SOpenExtendedInventoryPacket msg,
                              @NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();

        context.enqueueWork(() -> {
            if (player == null) {
                return;
            }
            if (!Config.isReplaceVanillaInventoryUiEnabled() || Config.isInventoryUiKillSwitchEnabled()) {
                return;
            }
            if (player.isCreative()) {
                return;
            }
            if (player.containerMenu instanceof ExtendedInventoryMenu) {
                return;
            }

            PlayerLayoutProfile profile = EquipmentLayoutService.getInstance().getOrBuild(player);
            var capability = PlayerInventoryPersistenceService.requireCapability(player);

            NetworkHooks.openScreen(player,
                    new net.minecraft.world.SimpleMenuProvider(
                            (containerId, inventory, ignored) ->
                                    new ExtendedInventoryMenu(containerId, inventory, player, capability, profile),
                            Component.translatable("screen.inventory.extended_inventory")),
                    buf -> ExtendedInventoryMenu.writeModSlotIds(buf, profile));
        });
        context.setPacketHandled(true);
    }
}
