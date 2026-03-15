package com.example.examplemod.inventory.core.network.packet;

import com.example.examplemod.inventory.core.network.client.ClientCraftPanelState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Result of a craft create intent processed by the server.
 */
public final class S2CCraftCreateResultPacket {

    private final boolean success;
    private final String recipeId;
    private final String messageKey;

    public S2CCraftCreateResultPacket(boolean success,
                                      @NotNull String recipeId,
                                      @NotNull String messageKey) {
        this.success = success;
        this.recipeId = recipeId;
        this.messageKey = messageKey;
    }

    public static void encode(@NotNull S2CCraftCreateResultPacket msg, @NotNull FriendlyByteBuf buf) {
        buf.writeBoolean(msg.success);
        buf.writeUtf(msg.recipeId, 64);
        buf.writeUtf(msg.messageKey, 128);
    }

    @NotNull
    public static S2CCraftCreateResultPacket decode(@NotNull FriendlyByteBuf buf) {
        boolean success = buf.readBoolean();
        String recipeId = buf.readUtf(64);
        String messageKey = buf.readUtf(128);
        return new S2CCraftCreateResultPacket(success, recipeId, messageKey);
    }

    public static void handle(@NotNull S2CCraftCreateResultPacket msg,
                              @NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientCraftPanelState.applyCreateResult(msg.success(), msg.recipeId(), msg.messageKey()));
        context.setPacketHandled(true);
    }

    public boolean success() {
        return success;
    }

    @NotNull
    public String recipeId() {
        return recipeId;
    }

    @NotNull
    public String messageKey() {
        return messageKey;
    }
}
