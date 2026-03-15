package com.example.examplemod.inventory.core.network;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.inventory.core.network.packet.C2SCraftCreateIntentPacket;
import com.example.examplemod.inventory.core.network.packet.C2SInventoryActionPacket;
import com.example.examplemod.inventory.core.network.packet.C2SOpenExtendedInventoryPacket;
import com.example.examplemod.inventory.core.network.packet.S2CCraftCreateResultPacket;
import com.example.examplemod.inventory.core.network.packet.S2CFullInventorySyncPacket;
import com.example.examplemod.inventory.core.network.packet.S2CPartialInventorySyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.NotNull;

/**
 * Central network channel and packet registration for inventory sync/actions.
 */
public final class InventoryNetworkHandler {

    private static final String PROTOCOL_VERSION = "m8-ui-v2";

    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static boolean registered;

    private InventoryNetworkHandler() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        int id = 0;
        CHANNEL.messageBuilder(S2CFullInventorySyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CFullInventorySyncPacket::encode)
                .decoder(S2CFullInventorySyncPacket::decode)
                .consumerMainThread(S2CFullInventorySyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(S2CPartialInventorySyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CPartialInventorySyncPacket::encode)
                .decoder(S2CPartialInventorySyncPacket::decode)
                .consumerMainThread(S2CPartialInventorySyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(C2SInventoryActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SInventoryActionPacket::encode)
                .decoder(C2SInventoryActionPacket::decode)
                .consumerMainThread(C2SInventoryActionPacket::handle)
                .add();

        CHANNEL.messageBuilder(C2SOpenExtendedInventoryPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SOpenExtendedInventoryPacket::encode)
                .decoder(C2SOpenExtendedInventoryPacket::decode)
                .consumerMainThread(C2SOpenExtendedInventoryPacket::handle)
                .add();

        CHANNEL.messageBuilder(C2SCraftCreateIntentPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SCraftCreateIntentPacket::encode)
                .decoder(C2SCraftCreateIntentPacket::decode)
                .consumerMainThread(C2SCraftCreateIntentPacket::handle)
                .add();

        CHANNEL.messageBuilder(S2CCraftCreateResultPacket.class, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CCraftCreateResultPacket::encode)
                .decoder(S2CCraftCreateResultPacket::decode)
                .consumerMainThread(S2CCraftCreateResultPacket::handle)
                .add();

        registered = true;
    }

    public static void sendToPlayer(@NotNull ServerPlayer player, @NotNull Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(@NotNull Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
