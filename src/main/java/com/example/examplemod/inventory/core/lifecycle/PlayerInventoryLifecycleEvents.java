package com.example.examplemod.inventory.core.lifecycle;

import com.example.examplemod.Config;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.inventory.core.capability.ExtendedInventoryProvider;
import com.example.examplemod.inventory.core.capability.ModCapabilities;
import com.example.examplemod.inventory.core.storage.PlayerInventoryPersistenceService;
import com.example.examplemod.inventory.core.sync.InventorySyncService;
import com.example.examplemod.inventory.core.sync.SyncTrigger;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerInventoryLifecycleEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation CAPABILITY_KEY =
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "extended_inventory");

    private PlayerInventoryLifecycleEvents() {
    }

    @SubscribeEvent
    public static void onAttachPlayerCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player)) {
            return;
        }

        ExtendedInventoryProvider provider = new ExtendedInventoryProvider();
        event.addCapability(CAPABILITY_KEY, provider);
        event.addListener(provider::invalidate);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerInventoryPersistenceService.loadIntoPlayer(serverPlayer);
        InventorySyncService.getInstance().requestFullSync(serverPlayer, SyncTrigger.LOGIN);
        debug("login", serverPlayer);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerInventoryPersistenceService.saveFromPlayer(serverPlayer);
        InventorySyncService.getInstance().clearPending(serverPlayer.getUUID());
        debug("logout", serverPlayer);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer) || !(event.getOriginal() instanceof ServerPlayer oldPlayer)) {
            return;
        }

        oldPlayer.reviveCaps();
        oldPlayer.getCapability(ModCapabilities.EXTENDED_INVENTORY).ifPresent(oldCap ->
                newPlayer.getCapability(ModCapabilities.EXTENDED_INVENTORY).ifPresent(newCap -> {
                    newCap.deserializeCapabilityNbt(oldCap.serializeCapabilityNbt());
                    newCap.clearDirty();
                }));
        oldPlayer.invalidateCaps();

        PlayerInventoryPersistenceService.saveFromPlayer(newPlayer);
        InventorySyncService.getInstance().requestFullSync(newPlayer, SyncTrigger.CLONE);
        debug(event.isWasDeath() ? "clone_death" : "clone_non_death", newPlayer);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerInventoryPersistenceService.loadIntoPlayer(serverPlayer);
        InventorySyncService.getInstance().requestFullSync(serverPlayer, SyncTrigger.RESPAWN);
        debug("respawn", serverPlayer);
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerInventoryPersistenceService.saveFromPlayer(serverPlayer);
        PlayerInventoryPersistenceService.loadIntoPlayer(serverPlayer);
        InventorySyncService.getInstance().requestFullSync(serverPlayer, SyncTrigger.DIMENSION_CHANGE);
        debug("dimension_change", serverPlayer);
    }

    private static void debug(String phase, ServerPlayer player) {
        if (!Config.debugLifecycleLogs) {
            return;
        }

        player.getCapability(ModCapabilities.EXTENDED_INVENTORY).ifPresent(cap ->
                LOGGER.info("[M1 lifecycle] {} player={} uuid={} revision={} dirty={}",
                        phase,
                        player.getGameProfile().getName(),
                        player.getUUID(),
                        cap.getRevision(),
                        cap.isDirty()));
    }
}
