package com.example.examplemod.inventory.core.sync;

import com.example.examplemod.ExampleMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Wires lifecycle and server tick to the M4 sync service.
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InventorySyncEvents {

    private InventorySyncEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        InventorySyncService.getInstance().flushAll(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        String menuClassName = event.getContainer().getClass().getName();
        // Avoid syncing for unrelated vanilla/foreign menus.
        if (!menuClassName.startsWith("com.example.examplemod")) {
            return;
        }

        InventorySyncService.getInstance().requestFullSync(player, SyncTrigger.OPEN_MENU);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        InventorySyncService.getInstance().clearPending(player.getUUID());
    }
}
