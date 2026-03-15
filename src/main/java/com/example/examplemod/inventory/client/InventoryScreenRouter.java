package com.example.examplemod.inventory.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.inventory.core.network.InventoryNetworkHandler;
import com.example.examplemod.inventory.core.network.packet.C2SOpenExtendedInventoryPacket;
import com.example.examplemod.inventory.menu.ExtendedInventoryMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Replaces vanilla inventory screen opening with the M7 extended inventory menu.
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InventoryScreenRouter {

    private static long lastOpenRequestMillis;

    private InventoryScreenRouter() {
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof InventoryScreen)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !shouldReplaceVanillaInventory(InventoryUiFeatureFlags.isUiReplacementEnabled(),
                true,
                minecraft.player.isCreative())) {
            return;
        }

        event.setCanceled(true);
        if (!shouldSendOpenRequest(System.currentTimeMillis(), lastOpenRequestMillis)
                || minecraft.player.containerMenu instanceof ExtendedInventoryMenu) {
            return;
        }

        lastOpenRequestMillis = System.currentTimeMillis();
        InventoryNetworkHandler.sendToServer(new C2SOpenExtendedInventoryPacket());
    }

    public static void onReplacementOpened() {
        lastOpenRequestMillis = 0L;
    }

    static boolean shouldSendOpenRequest(long nowMillis, long previousRequestMillis) {
        return previousRequestMillis <= 0L || nowMillis - previousRequestMillis >= 250L;
    }

    static boolean shouldReplaceVanillaInventory(boolean uiReplacementEnabled,
                                                 boolean hasPlayer,
                                                 boolean isCreativeMode) {
        return uiReplacementEnabled && hasPlayer && !isCreativeMode;
    }
}
