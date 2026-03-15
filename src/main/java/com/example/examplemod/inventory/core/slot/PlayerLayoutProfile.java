package com.example.examplemod.inventory.core.slot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Resolved, immutable snapshot of all inventory slots available to a specific player
 * at a given point in time.
 *
 * <p>A {@code PlayerLayoutProfile} is built by
 * {@link com.example.examplemod.inventory.core.layout.EquipmentLayoutService} by merging:
 * <ol>
 *   <li>Base slots from the mod configuration ({@link SlotSource#BASE}).</li>
 *   <li>Slots contributed by currently-equipped items ({@link SlotSource#EQUIPMENT}).</li>
 *   <li>Slots contributed by container items placed in equipment slots ({@link SlotSource#CONTAINER}).</li>
 * </ol>
 *
 * <p>The profile provides:
 * <ul>
 *   <li>An ordered flat list of all {@link SlotDefinition} instances.</li>
 *   <li>Fast lookup of a slot by its stable {@code slotId}.</li>
 *   <li>Grouped view via {@link SlotGroupDefinition} for GUI rendering.</li>
 *   <li>A compact {@link #layoutToken} that captures the current state, used to detect
 *       stale cached profiles without a full rebuild.</li>
 * </ul>
 *
 * <p>Profiles are per-player and are discarded and rebuilt whenever the player equips or
 * unequips an item that implements {@link IEquipmentSlotProvider}.</p>
 *
 * <p>Instances of this class are <strong>server-side only</strong> during runtime. A
 * lightweight summary is sent to the client for GUI rendering purposes via sync packets.</p>
 */
public final class PlayerLayoutProfile {

    /** UUID of the player this profile belongs to. */
    private final UUID playerId;

    /**
     * Compact hash / token representing the current layout composition.
     * Changes whenever the set of groups or their configurations changes.
     */
    private final String layoutToken;

    /** All groups in display order. */
    private final List<SlotGroupDefinition> groups;

    /** Flat ordered list of all slot definitions across all groups. */
    private final List<SlotDefinition> allSlots;

    /**
     * Map from {@code slotId} → {@link SlotDefinition} for O(1) lookups.
     * Populated at construction time.
     */
    private final Map<String, SlotDefinition> slotById;

    /**
     * Map from {@code slotId} → runtime slot index within {@code allSlots}.
     * The index corresponds directly to the position in the underlying
     * {@link net.minecraftforge.items.ItemStackHandler}.
     */
    private final Map<String, Integer> indexById;

    /**
     * Constructs a resolved layout profile.
     *
     * @param playerId    UUID of the owning player
     * @param layoutToken compact token representing this layout version
     * @param groups      ordered list of slot groups
     */
    public PlayerLayoutProfile(@NotNull UUID playerId,
                               @NotNull String layoutToken,
                               @NotNull List<SlotGroupDefinition> groups) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.layoutToken = Objects.requireNonNull(layoutToken, "layoutToken");
        this.groups = Collections.unmodifiableList(List.copyOf(
                Objects.requireNonNull(groups, "groups")));

        // Build flat list and lookup maps
        List<SlotDefinition> flat = new ArrayList<>();
        Map<String, SlotDefinition> byId = new LinkedHashMap<>();
        Map<String, Integer> indexMap = new LinkedHashMap<>();

        for (SlotGroupDefinition group : this.groups) {
            for (SlotDefinition slot : group.getSlots()) {
                int idx = flat.size();
                flat.add(slot);
                byId.put(slot.getSlotId(), slot);
                indexMap.put(slot.getSlotId(), idx);
            }
        }

        this.allSlots = Collections.unmodifiableList(flat);
        this.slotById = Collections.unmodifiableMap(byId);
        this.indexById = Collections.unmodifiableMap(indexMap);
    }

    // ── Accessors ──────────────────────────────────────────────────────────

    /** @return UUID of the player this profile belongs to */
    @NotNull
    public UUID getPlayerId() { return playerId; }

    /**
     * @return a compact token representing the current layout composition;
     *         can be compared cheaply to detect layout changes
     */
    @NotNull
    public String getLayoutToken() { return layoutToken; }

    /** @return ordered list of all slot groups */
    @NotNull
    public List<SlotGroupDefinition> getGroups() { return groups; }

    /** @return flat ordered list of all slot definitions across all groups */
    @NotNull
    public List<SlotDefinition> getAllSlots() { return allSlots; }

    /** @return total number of slots in this layout */
    public int getTotalSlotCount() { return allSlots.size(); }

    /**
     * Looks up a slot by its stable id.
     *
     * @param slotId the slot identifier
     * @return the {@link SlotDefinition}, or {@code null} if not found
     */
    @Nullable
    public SlotDefinition findSlot(@NotNull String slotId) {
        return slotById.get(slotId);
    }

    /**
     * Returns the runtime index of the slot with the given id in the underlying
     * {@link net.minecraftforge.items.ItemStackHandler}.
     *
     * @param slotId the slot identifier
     * @return the zero-based index, or {@code -1} if the slot is not in this layout
     */
    public int getSlotIndex(@NotNull String slotId) {
        return indexById.getOrDefault(slotId, -1);
    }

    /**
     * Returns {@code true} if the slot with the given id exists in this layout.
     *
     * @param slotId the slot identifier
     */
    public boolean containsSlot(@NotNull String slotId) {
        return slotById.containsKey(slotId);
    }

    /**
     * Returns all slots of the given {@link SlotType} as a new list.
     *
     * @param type the desired slot type
     * @return matching slot definitions; may be empty
     */
    @NotNull
    public List<SlotDefinition> getSlotsByType(@NotNull SlotType type) {
        List<SlotDefinition> result = new ArrayList<>();
        for (SlotDefinition slot : allSlots) {
            if (slot.getSlotType() == type) {
                result.add(slot);
            }
        }
        return result;
    }

    /**
     * Returns all slots contributed by the given {@link SlotSource} as a new list.
     *
     * @param source the desired source
     * @return matching slot definitions; may be empty
     */
    @NotNull
    public List<SlotDefinition> getSlotsBySource(@NotNull SlotSource source) {
        List<SlotDefinition> result = new ArrayList<>();
        for (SlotDefinition slot : allSlots) {
            if (slot.getSource() == source) {
                result.add(slot);
            }
        }
        return result;
    }

    // ── equals / hashCode / toString ──────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerLayoutProfile)) return false;
        PlayerLayoutProfile that = (PlayerLayoutProfile) o;
        return Objects.equals(playerId, that.playerId)
                && Objects.equals(layoutToken, that.layoutToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, layoutToken);
    }

    @Override
    public String toString() {
        return "PlayerLayoutProfile{player=" + playerId
                + ", slots=" + allSlots.size()
                + ", token='" + layoutToken + "'}";
    }
}

