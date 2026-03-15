package com.example.examplemod.inventory.core.layout;

import com.example.examplemod.Config;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.inventory.core.sync.InventorySyncService;
import com.example.examplemod.inventory.core.sync.SyncTrigger;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Listens for equipment-change and dimension-change events to invalidate
 * stale {@link EquipmentLayoutService} cache entries.
 *
 * <p>The rule is simple:
 * <ul>
 *   <li>When a {@link ServerPlayer} equips or unequips an item, invalidate the
 *       cached layout so the next capability access triggers a rebuild.</li>
 *   <li>When a player changes dimension or respawns, invalidate + save, because the
 *       runtime slot count may change (different equipment reachable after crossing dimensions).</li>
 *   <li>When a player logs out, evict the entry to free memory.</li>
 * </ul>
 *
 * <p>These listeners are registered on the {@link Mod.EventBusSubscriber.Bus#FORGE} bus.</p>
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LayoutInvalidationEvents {

    private static final Logger LOGGER = LogUtils.getLogger();

    private LayoutInvalidationEvents() {}

    /**
     * Fires whenever a living entity's equipment changes.
     * We only care about server-side player equipment changes.
     */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Only invalidate when the old or new stack could carry a slot provider cap.
        // A quick check: if both stacks are empty, skip (shouldn't happen, but be safe).
        if (event.getFrom().isEmpty() && event.getTo().isEmpty()) {
            return;
        }

        EquipmentLayoutService.getInstance().invalidate(player.getUUID());
        InventorySyncService.getInstance().requestFullSync(player, SyncTrigger.LAYOUT_CHANGED);

        if (Config.debugLifecycleLogs) {
            LOGGER.info("[M2 layout] Invalidated layout for player={} slot={}",
                    player.getGameProfile().getName(),
                    event.getSlot().getName());
        }
    }

    /**
     * On player logout, evict the cache entry to free memory.
     * Persistence is handled by {@link com.example.examplemod.inventory.core.lifecycle.PlayerInventoryLifecycleEvents}.
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        EquipmentLayoutService.getInstance().evict(player.getUUID());
    }

    /**
     * On respawn, invalidate layout so it is rebuilt with the player's new equipment state.
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        EquipmentLayoutService.getInstance().invalidate(player.getUUID());
    }

    /**
     * On dimension change, invalidate layout (equipment may have been altered by the transition).
     */
    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        EquipmentLayoutService.getInstance().invalidate(player.getUUID());
    }
}
