package com.example.examplemod.inventory.core.layout;

import com.example.examplemod.Config;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.inventory.core.slot.IEquipmentSlotProvider;
import com.example.examplemod.inventory.core.slot.PlayerLayoutProfile;
import com.example.examplemod.inventory.core.slot.SlotDefinition;
import com.example.examplemod.inventory.core.slot.SlotGroupDefinition;
import com.example.examplemod.inventory.core.slot.SlotSource;
import com.example.examplemod.inventory.core.slot.SlotType;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side service responsible for building and caching {@link PlayerLayoutProfile}
 * instances for online players.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>On player login / respawn, call {@link #invalidate(UUID)} then
 *       {@link #getOrBuild(ServerPlayer)} to get a fresh profile.</li>
 *   <li>On equip/unequip events, call {@link #invalidate(UUID)} and let the
 *       next capability access rebuild the profile lazily.</li>
 *   <li>On player logout, call {@link #evict(UUID)} to release the cached entry.</li>
 * </ol>
 *
 * <h2>Token-based staleness check</h2>
 * <p>Each cached entry stores the token that was used to build it. Before returning
 * a cached profile, the service recomputes the current token cheaply and compares.
 * If they differ the profile is rebuilt automatically.</p>
 *
 * <p>This class is <strong>thread-safe</strong> at the cache level via
 * {@link ConcurrentHashMap}, but the actual rebuild step must only run on the
 * <strong>server thread</strong>. Call sites must ensure this.</p>
 */
public final class EquipmentLayoutService {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Capability used to query {@link IEquipmentSlotProvider} from item stacks.
     * Registered in mod setup when a weapons/equipment mod attaches it.
     */
    public static final Capability<IEquipmentSlotProvider> SLOT_PROVIDER_CAP =
            CapabilityManager.get(new CapabilityToken<>() {});

    /** Singleton instance — one service per server lifecycle. */
    private static final EquipmentLayoutService INSTANCE = new EquipmentLayoutService();

    /** Cache: playerUUID → cached layout entry. */
    private final Map<UUID, CachedEntry> cache = new ConcurrentHashMap<>();

    private EquipmentLayoutService() {}

    /** @return the singleton service instance */
    public static EquipmentLayoutService getInstance() {
        return INSTANCE;
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Returns the resolved {@link PlayerLayoutProfile} for the given player,
     * rebuilding it if the cached version is absent or stale.
     *
     * <p>Must be called on the <strong>server thread</strong>.</p>
     *
     * @param player the online server player
     * @return a non-null, up-to-date layout profile
     */
    @NotNull
    public PlayerLayoutProfile getOrBuild(@NotNull ServerPlayer player) {
        UUID playerId = player.getUUID();
        String currentToken = computeToken(player);

        CachedEntry entry = cache.get(playerId);
        if (entry != null && entry.token.equals(currentToken)) {
            return entry.profile;
        }

        PlayerLayoutProfile profile = build(player, currentToken);
        cache.put(playerId, new CachedEntry(currentToken, profile));

        if (Config.debugLifecycleLogs) {
            LOGGER.info("[M2 layout] Built profile player={} slots={} token={}",
                    player.getGameProfile().getName(),
                    profile.getTotalSlotCount(),
                    currentToken);
        }
        return profile;
    }

    /**
     * Invalidates the cached layout for the given player UUID.
     * The next call to {@link #getOrBuild(ServerPlayer)} will trigger a rebuild.
     *
     * @param playerId the UUID to invalidate
     */
    public void invalidate(@NotNull UUID playerId) {
        cache.remove(playerId);
    }

    /**
     * Removes the cache entry entirely for the given player UUID.
     * Called on player logout to prevent stale entries for offline players.
     *
     * @param playerId the UUID to evict
     */
    public void evict(@NotNull UUID playerId) {
        cache.remove(playerId);
    }

    /** Clears all cached layout profiles. Useful for server stop / world unload. */
    public void clearAll() {
        cache.clear();
    }

    // ── Token computation ──────────────────────────────────────────────────

    /**
     * Computes a cheap, stable token string representing the current layout inputs
     * (equipped items that implement {@link IEquipmentSlotProvider}).
     *
     * <p>The token is built by concatenating version tokens from all equipped items
     * in a fixed order. Changes in equipped items, their NBT, or their slot counts
     * will produce a different token and trigger a rebuild.</p>
     *
     * @param player the server player
     * @return a non-empty token string
     */
    @NotNull
    String computeToken(@NotNull ServerPlayer player) {
        StringBuilder sb = new StringBuilder();
        sb.append("base:").append(Config.getBaseRuntimeSlots());

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            stack.getCapability(SLOT_PROVIDER_CAP).ifPresent(provider -> {
                sb.append('|').append(slot.getName())
                  .append(':').append(provider.getLayoutVersionToken(stack));
            });
        }
        return sb.toString();
    }

    // ── Profile building ───────────────────────────────────────────────────

    /**
     * Builds a fresh {@link PlayerLayoutProfile} for the given player.
     *
     * <p>The build order is:
     * <ol>
     *   <li>Base slots from config.</li>
     *   <li>Equipment-contributed slots, in {@link EquipmentSlot} enum order.</li>
     * </ol>
     *
     * @param player       the server player
     * @param layoutToken  pre-computed token string for this build
     * @return a complete, immutable layout profile
     */
    @NotNull
    private PlayerLayoutProfile build(@NotNull ServerPlayer player, @NotNull String layoutToken) {
        List<SlotGroupDefinition> groups = new ArrayList<>();

        // 1. Base slots
        groups.add(buildBaseGroup());

        // 2. Equipment slots
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;

            stack.getCapability(SLOT_PROVIDER_CAP).ifPresent(provider -> {
                List<SlotGroupDefinition> contributed = provider.getSlotGroups(stack);
                if (contributed != null) {
                    groups.addAll(contributed);
                }
            });
        }

        return new PlayerLayoutProfile(player.getUUID(), layoutToken, groups);
    }

    /**
     * Builds the base slot group from the current config value.
     * The base group always exists and uses {@link SlotSource#BASE}.
     *
     * @return a {@link SlotGroupDefinition} representing the base slots
     */
    @NotNull
    private SlotGroupDefinition buildBaseGroup() {
        int count = Config.getBaseRuntimeSlots();
        List<SlotDefinition> slots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            slots.add(SlotDefinition
                    .builder(ExampleMod.MODID + ":base_general_" + i,
                             SlotType.UTILITY,
                             SlotSource.BASE)
                    .displayGroup("inv.group.base")
                    .order(i)
                    .uiAnchor("left_panel")
                    .uiPriority(100)
                    .build());
        }
        return SlotGroupDefinition
                .builder(ExampleMod.MODID + ":base",
                         "inv.group.base",
                         SlotSource.BASE)
                .slots(slots)
                .uiAnchor("left_panel")
                .uiPriority(100)
                .build();
    }

    // ── Inner types ────────────────────────────────────────────────────────

    private static final class CachedEntry {
        final String token;
        final PlayerLayoutProfile profile;

        CachedEntry(String token, PlayerLayoutProfile profile) {
            this.token = token;
            this.profile = profile;
        }
    }
}

