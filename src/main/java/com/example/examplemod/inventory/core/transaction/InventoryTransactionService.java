package com.example.examplemod.inventory.core.transaction;

import com.example.examplemod.Config;
import com.example.examplemod.inventory.core.capability.IExtendedInventoryCapability;
import com.example.examplemod.inventory.core.craft.CraftIngredientRequirement;
import com.example.examplemod.inventory.core.slot.PlayerLayoutProfile;
import com.example.examplemod.inventory.core.slot.SlotDefinition;
import com.example.examplemod.inventory.core.slot.SlotGroupDefinition;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Server-side transactional service for all inventory mutations.
 *
 * <h2>Transaction model</h2>
 * <pre>
 *   snapshot → dry-run → commit
 *                      ↘ rollback (on any failure)
 * </pre>
 *
 * <p>Every public mutating method follows this pattern:
 * <ol>
 *   <li>Take an {@link InventorySnapshot} of the current capability state.</li>
 *   <li>Perform a <em>dry-run</em> validation (no net-IO, pure logic).</li>
 *   <li>If validation passes, apply the real mutation and return
 *       {@link TransactionResult#committed(List)}.</li>
 *   <li>If validation fails, call {@link InventorySnapshot#restore(IExtendedInventoryCapability)}
 *       and return {@link TransactionResult#rolledBack(String)}.</li>
 * </ol>
 *
 * <p><strong>Thread safety:</strong> All methods MUST be called on the
 * <em>server logical thread</em>. No concurrent access protection is provided.</p>
 *
 * <h2>Layout awareness</h2>
 * <p>Operations that affect dynamic slots (equipment removal) accept a
 * {@link PlayerLayoutProfile} describing the <em>current</em> layout. The service
 * never reads equipment state directly; callers supply the resolved profile. This
 * keeps the service testable without a live server.</p>
 *
 * @see OverflowPolicy
 * @see TransactionResult
 * @see InventorySnapshot
 */
public final class InventoryTransactionService {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Singleton — one instance per server lifecycle. */
    private static final InventoryTransactionService INSTANCE = new InventoryTransactionService();

    private InventoryTransactionService() {}

    /** @return the singleton service instance */
    public static InventoryTransactionService getInstance() {
        return INSTANCE;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Moves a single item stack from one slot to another within the player's inventory.
     *
     * <p>Dry-run phase checks:
     * <ul>
     *   <li>Source slot is within range and contains a non-empty stack.</li>
     *   <li>Destination slot is within range and its {@link SlotDefinition#isEnabled()} is
     *       {@code true}.</li>
     *   <li>Destination's {@link com.example.examplemod.inventory.core.slot.SlotAcceptRule}
     *       accepts the stack.</li>
     *   <li>Destination has enough space (partial insert is not allowed for a move).</li>
     * </ul>
     *
     * @param cap       the player's capability (server-side only)
     * @param profile   the current resolved layout profile
     * @param fromSlotId  the source slot ID
     * @param toSlotId    the destination slot ID
     * @return transaction result; on failure the capability state is unchanged
     */
    @NotNull
    public TransactionResult moveItem(@NotNull IExtendedInventoryCapability cap,
                                       @NotNull PlayerLayoutProfile profile,
                                       @NotNull String fromSlotId,
                                       @NotNull String toSlotId) {
        // ── Validate inputs ──────────────────────────────────────────────
        if (fromSlotId.equals(toSlotId)) {
            return TransactionResult.rejected("move: source == destination slotId=" + fromSlotId);
        }

        int fromIdx = profile.getSlotIndex(fromSlotId);
        int toIdx   = profile.getSlotIndex(toSlotId);

        if (fromIdx < 0) {
            return TransactionResult.rejected("move: unknown fromSlotId=" + fromSlotId);
        }
        if (toIdx < 0) {
            return TransactionResult.rejected("move: unknown toSlotId=" + toSlotId);
        }

        SlotDefinition toSlot = profile.findSlot(toSlotId);
        if (toSlot == null) {
            return TransactionResult.rejected("move: toSlot definition missing for id=" + toSlotId);
        }
        if (!toSlot.isEnabled()) {
            return TransactionResult.rolledBack(
                    "move: destination slot disabled id=" + toSlotId,
                    Component.translatable("examplemod.inventory.error.slot_disabled"));
        }

        // ── Snapshot ─────────────────────────────────────────────────────
        InventorySnapshot snapshot = InventorySnapshot.take(cap);

        try {
            ItemStack moving = cap.getInventory().getStackInSlot(fromIdx).copy();
            if (moving.isEmpty()) {
                return TransactionResult.rejected("move: source slot is empty id=" + fromSlotId);
            }

            // ── Dry-run: check accept rule ────────────────────────────────
            SlotValidationResult validation = validateSlotPlacement(toSlot, moving, cap, toIdx);
            if (!validation.isAllowed()) {
                return TransactionResult.rolledBack(
                        "move: slot rule rejected: " + validation.getRejectionReason(),
                        Component.translatable("examplemod.inventory.error.slot_rejected"));
            }
            if (!validation.getRemainder().isEmpty()) {
                return TransactionResult.rolledBack(
                        "move: partial insert not allowed for move op: remainder=" + validation.getRemainder().getCount(),
                        Component.translatable("examplemod.inventory.error.no_space"));
            }

            // ── Commit ────────────────────────────────────────────────────
            cap.getInventory().setStackInSlot(fromIdx, ItemStack.EMPTY);
            cap.getInventory().setStackInSlot(toIdx, moving);
            cap.markDirty();

            debugLog("moveItem committed: {} → {} player revision={}",
                    fromSlotId, toSlotId, cap.getRevision());

            return TransactionResult.committed(List.of(fromSlotId, toSlotId));

        } catch (Exception e) {
            LOGGER.error("[M3 transaction] moveItem unexpected error, rolling back", e);
            snapshot.restore(cap);
            return TransactionResult.rolledBack("moveItem internal error: " + e.getMessage());
        }
    }

    /**
     * Handles the server-side logic when a player removes an equipment item that
     * contributed dynamic slots to the layout.
     *
     * <p>The method resolves overflow items (items in the slots provided by the
     * removed group) and applies the configured {@link OverflowPolicy}.</p>
     *
     * <h2>Dry-run phase</h2>
     * <p>Before any real mutation, the service simulates relocation of all items
     * currently in the group's slots into the remaining available layout. Only if
     * all items can be placed (or the policy allows partial success) is the
     * transaction committed.</p>
     *
     * @param cap             the player's capability
     * @param profile         the layout profile <em>before</em> the equipment item is removed
     * @param removedGroup    the slot group that will disappear after the removal
     * @param remainingGroups layout groups that will remain after removal (used for relocation)
     * @param policy          overflow handling policy
     * @param player          the server player (used for DROP policy; may be null for tests)
     * @return transaction result
     */
    @NotNull
    public TransactionResult executeEquipmentRemoval(@NotNull IExtendedInventoryCapability cap,
                                                       @NotNull PlayerLayoutProfile profile,
                                                       @NotNull SlotGroupDefinition removedGroup,
                                                       @NotNull List<SlotGroupDefinition> remainingGroups,
                                                       @NotNull OverflowPolicy policy,
                                                       @Nullable ServerPlayer player) {
        // ── Snapshot ─────────────────────────────────────────────────────
        InventorySnapshot snapshot = InventorySnapshot.take(cap);

        try {
            // Collect items currently occupying the removed group's slots
            List<ItemInSlot> overflowItems = collectGroupItems(cap, profile, removedGroup);

            if (overflowItems.isEmpty()) {
                // Nothing to relocate — safe to remove
                clearGroupSlots(cap, profile, removedGroup);
                cap.markDirty();
                debugLog("equipmentRemoval: group {} empty, committed", removedGroup.getGroupId());
                return TransactionResult.committed(removedGroupSlotIds(removedGroup));
            }

            // ── Dry-run: simulate relocation ──────────────────────────────
            RelocateResult relocate = dryRunRelocate(overflowItems, remainingGroups, cap, profile);

            if (!relocate.allPlaced()) {
                return handleUnplacedItems(cap, snapshot, profile, removedGroup, relocate,
                        policy, player);
            }

            // ── Commit: clear removed slots, apply relocated items ────────
            clearGroupSlots(cap, profile, removedGroup);
            applyRelocation(relocate, cap, profile);
            cap.markDirty();

            List<String> dirty = buildDirtyList(removedGroup, relocate);
            debugLog("equipmentRemoval committed: group={} relocated={}",
                    removedGroup.getGroupId(), relocate.placements().size());

            return TransactionResult.committed(dirty);

        } catch (Exception e) {
            LOGGER.error("[M3 transaction] executeEquipmentRemoval unexpected error, rolling back", e);
            snapshot.restore(cap);
            return TransactionResult.rolledBack("equipmentRemoval internal error: " + e.getMessage());
        }
    }

    /**
     * Inserts an item stack into the first compatible available slot in the given profile.
     *
     * <p>Iterates all enabled, non-full slots in layout order and inserts as much as
     * possible (up to the full stack). Returns the remainder if the stack only partially fits.</p>
     *
     * @param cap     the capability to insert into
     * @param profile the current layout profile
     * @param stack   the stack to insert (not modified; a copy is used internally)
     * @return result with {@link TransactionResult#getDirtySlotIds()} listing touched slots;
     *         if the full stack was inserted the result is committed even if remainder is empty
     */
    @NotNull
    public TransactionResult insertItem(@NotNull IExtendedInventoryCapability cap,
                                         @NotNull PlayerLayoutProfile profile,
                                         @NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            return TransactionResult.rejected("insertItem: empty stack");
        }

        InventorySnapshot snapshot = InventorySnapshot.take(cap);

        try {
            ItemStack remaining = stack.copy();
            List<String> dirtySlots = new ArrayList<>();

            for (SlotDefinition slot : profile.getAllSlots()) {
                if (!slot.isEnabled()) continue;
                int idx = profile.getSlotIndex(slot.getSlotId());
                if (idx < 0) continue;

                SlotValidationResult valid = validateSlotPlacement(slot, remaining, cap, idx);
                if (!valid.isAllowed()) continue;

                int before = remaining.getCount();
                ItemStack inserted = cap.getInventory().insertItem(idx, remaining.copy(), false);
                if (inserted.getCount() < before) {
                    dirtySlots.add(slot.getSlotId());
                }
                remaining = inserted;
                if (remaining.isEmpty()) break;
            }

            if (!remaining.isEmpty() && remaining.getCount() == stack.getCount()) {
                // Nothing was inserted at all — rollback and reject
                snapshot.restore(cap);
                return TransactionResult.rolledBack(
                        "insertItem: no compatible slot found for " + stack.getItem(),
                        Component.translatable("examplemod.inventory.error.no_compatible_slot"));
            }

            cap.markDirty();
            debugLog("insertItem committed: item={} dirtySlots={}", stack.getItem(), dirtySlots.size());
            return TransactionResult.committed(dirtySlots);

        } catch (Exception e) {
            LOGGER.error("[M3 transaction] insertItem unexpected error, rolling back", e);
            snapshot.restore(cap);
            return TransactionResult.rolledBack("insertItem internal error: " + e.getMessage());
        }
    }

    /**
     * Extracts (removes) all contents from a specific slot.
     *
     * @param cap     the capability
     * @param profile the current layout profile
     * @param slotId  the slot to clear
     * @return the extracted stack wrapped in the result; dirty list contains the slot id
     */
    @NotNull
    public TransactionResult extractFromSlot(@NotNull IExtendedInventoryCapability cap,
                                              @NotNull PlayerLayoutProfile profile,
                                              @NotNull String slotId) {
        int idx = profile.getSlotIndex(slotId);
        if (idx < 0) {
            return TransactionResult.rejected("extractFromSlot: unknown slotId=" + slotId);
        }

        SlotDefinition slotDef = profile.findSlot(slotId);
        if (slotDef != null && !slotDef.isEnabled()) {
            return TransactionResult.rolledBack(
                    "extractFromSlot: slot disabled id=" + slotId,
                    Component.translatable("examplemod.inventory.error.slot_disabled"));
        }

        InventorySnapshot snapshot = InventorySnapshot.take(cap);
        try {
            ItemStack current = cap.getInventory().getStackInSlot(idx);
            if (current.isEmpty()) {
                return TransactionResult.rejected("extractFromSlot: slot already empty id=" + slotId);
            }

            cap.getInventory().setStackInSlot(idx, ItemStack.EMPTY);
            cap.markDirty();
            debugLog("extractFromSlot committed: slotId={}", slotId);
            return TransactionResult.committed(List.of(slotId));
        } catch (Exception e) {
            LOGGER.error("[M3 transaction] extractFromSlot unexpected error, rolling back", e);
            snapshot.restore(cap);
            return TransactionResult.rolledBack("extractFromSlot internal error: " + e.getMessage());
        }
    }

    /**
     * Swaps the contents of two slots.
     *
     * <p>Dry-run phase checks:
     * <ul>
     *   <li>Both slots are within range and enabled.</li>
     *   <li>Neither slot is empty, or both slots are empty (allowing empty ↔ empty swap).</li>
     *   <li>Items are stackable in their target slots (or the slots are empty).</li>
     * </ul>
     *
     * @param cap       the player's capability (server-side only)
     * @param profile   the current resolved layout profile
     * @param firstSlotId  the first slot ID
     * @param secondSlotId    the second slot ID
     * @return transaction result; on failure the capability state is unchanged
     */
    @NotNull
    public TransactionResult swapItems(@NotNull IExtendedInventoryCapability cap,
                                       @NotNull PlayerLayoutProfile profile,
                                       @NotNull String firstSlotId,
                                       @NotNull String secondSlotId) {
        if (firstSlotId.equals(secondSlotId)) {
            return TransactionResult.rejected("swap: same slotId=" + firstSlotId);
        }

        int firstIdx = profile.getSlotIndex(firstSlotId);
        int secondIdx = profile.getSlotIndex(secondSlotId);
        if (firstIdx < 0 || secondIdx < 0) {
            return TransactionResult.rejected("swap: unknown slot id");
        }

        SlotDefinition firstSlot = profile.findSlot(firstSlotId);
        SlotDefinition secondSlot = profile.findSlot(secondSlotId);
        if (firstSlot == null || secondSlot == null) {
            return TransactionResult.rejected("swap: missing slot definition");
        }
        if (!firstSlot.isEnabled() || !secondSlot.isEnabled()) {
            return TransactionResult.rolledBack("swap: disabled slot involved");
        }

        InventorySnapshot snapshot = InventorySnapshot.take(cap);
        try {
            ItemStack firstStack = cap.getInventory().getStackInSlot(firstIdx).copy();
            ItemStack secondStack = cap.getInventory().getStackInSlot(secondIdx).copy();
            if (firstStack.isEmpty() && secondStack.isEmpty()) {
                return TransactionResult.rejected("swap: both slots empty");
            }
            if (!isWholeStackReplaceable(firstSlot, secondStack, cap, firstIdx)) {
                return TransactionResult.rolledBack("swap: second stack cannot go into first slot");
            }
            if (!isWholeStackReplaceable(secondSlot, firstStack, cap, secondIdx)) {
                return TransactionResult.rolledBack("swap: first stack cannot go into second slot");
            }

            cap.getInventory().setStackInSlot(firstIdx, secondStack);
            cap.getInventory().setStackInSlot(secondIdx, firstStack);
            cap.markDirty();
            return TransactionResult.committed(List.of(firstSlotId, secondSlotId));
        } catch (Exception e) {
            LOGGER.error("[M7 transaction] swapItems unexpected error, rolling back", e);
            snapshot.restore(cap);
            return TransactionResult.rolledBack("swapItems internal error: " + e.getMessage());
        }
    }

    /**
     * Splits a stack in half (or nearly half) and moves the split-off part to a compatible slot.
     *
     * <p>Dry-run phase checks:
     * <ul>
     *   <li>Source slot is within range and contains a stack with at least 2 items.</li>
     *   <li>Destination slot is within range and its {@link SlotDefinition#isEnabled()} is
     *       {@code true}.</li>
     *   <li>Destination's {@link com.example.examplemod.inventory.core.slot.SlotAcceptRule}
     *       accepts the stack.</li>
     *   <li>Destination has enough space for the split-off stack.</li>
     * </ul>
     *
     * @param cap       the player's capability (server-side only)
     * @param profile   the current resolved layout profile
     * @param fromSlotId  the source slot ID
     * @return transaction result; on failure the capability state is unchanged
     */
    @NotNull
    public TransactionResult splitStack(@NotNull IExtendedInventoryCapability cap,
                                        @NotNull PlayerLayoutProfile profile,
                                        @NotNull String fromSlotId) {
        int fromIdx = profile.getSlotIndex(fromSlotId);
        if (fromIdx < 0) {
            return TransactionResult.rejected("splitStack: unknown slotId=" + fromSlotId);
        }

        InventorySnapshot snapshot = InventorySnapshot.take(cap);
        try {
            ItemStack source = cap.getInventory().getStackInSlot(fromIdx).copy();
            if (source.isEmpty()) {
                return TransactionResult.rejected("splitStack: source empty id=" + fromSlotId);
            }
            if (source.getCount() < 2) {
                return TransactionResult.rejected("splitStack: stack too small id=" + fromSlotId);
            }

            int movingCount = source.getCount() / 2;
            ItemStack moving = source.copy();
            moving.setCount(movingCount);
            TransferTarget target = findFirstCompatibleTarget(cap, profile, fromSlotId, moving);
            if (target == null) {
                return TransactionResult.rolledBack("splitStack: no compatible target for id=" + fromSlotId);
            }

            ItemStack remainingSource = source.copy();
            remainingSource.shrink(movingCount);
            cap.getInventory().setStackInSlot(fromIdx, remainingSource);
            ItemStack remainder = cap.getInventory().insertItem(target.slotIndex(), moving.copy(), false);
            if (!remainder.isEmpty()) {
                throw new IllegalStateException("splitStack commit left unexpected remainder=" + remainder.getCount());
            }
            cap.markDirty();
            return TransactionResult.committed(List.of(fromSlotId, target.slotId()));
        } catch (Exception e) {
            LOGGER.error("[M7 transaction] splitStack unexpected error, rolling back", e);
            snapshot.restore(cap);
            return TransactionResult.rolledBack("splitStack internal error: " + e.getMessage());
        }
    }

    /**
     * Quickly moves an item stack from one slot to another, bypassing normal insertion logic.
     *
     * <p>Dry-run phase checks:
     * <ul>
     *   <li>Source slot is within range and contains a non-empty stack.</li>
     *   <li>Destination slot is within range and its {@link SlotDefinition#isEnabled()} is
     *       {@code true}.</li>
     *   <li>Destination's {@link com.example.examplemod.inventory.core.slot.SlotAcceptRule}
     *       accepts the stack.</li>
     *   <li>Destination has enough space (partial insert is not allowed for quick move).</li>
     * </ul>
     *
     * @param cap       the player's capability (server-side only)
     * @param profile   the current resolved layout profile
     * @param fromSlotId  the source slot ID
     * @return transaction result; on failure the capability state is unchanged
     */
    @NotNull
    public TransactionResult quickMoveItem(@NotNull IExtendedInventoryCapability cap,
                                           @NotNull PlayerLayoutProfile profile,
                                           @NotNull String fromSlotId) {
        int fromIdx = profile.getSlotIndex(fromSlotId);
        if (fromIdx < 0) {
            return TransactionResult.rejected("quickMoveItem: unknown slotId=" + fromSlotId);
        }

        InventorySnapshot snapshot = InventorySnapshot.take(cap);
        try {
            ItemStack source = cap.getInventory().getStackInSlot(fromIdx).copy();
            if (source.isEmpty()) {
                return TransactionResult.rejected("quickMoveItem: source empty id=" + fromSlotId);
            }

            TransferTarget target = findFirstCompatibleTarget(cap, profile, fromSlotId, source);
            if (target == null) {
                return TransactionResult.rolledBack("quickMoveItem: no compatible target for id=" + fromSlotId);
            }

            cap.getInventory().setStackInSlot(fromIdx, ItemStack.EMPTY);
            ItemStack remainder = cap.getInventory().insertItem(target.slotIndex(), source.copy(), false);
            if (!remainder.isEmpty()) {
                throw new IllegalStateException("quickMoveItem commit left unexpected remainder=" + remainder.getCount());
            }
            cap.markDirty();
            return TransactionResult.committed(List.of(fromSlotId, target.slotId()));
        } catch (Exception e) {
            LOGGER.error("[M7 transaction] quickMoveItem unexpected error, rolling back", e);
            snapshot.restore(cap);
            return TransactionResult.rolledBack("quickMoveItem internal error: " + e.getMessage());
        }
    }

    /**
     * Quickly moves an item stack from one slot in the vanilla player inventory to an extended slot.
     *
     * <p>Dry-run phase checks:
     * <ul>
     *   <li>Source slot is within range and contains a non-empty stack.</li>
     *   <li>Destination slot is within range and its {@link SlotDefinition#isEnabled()} is
     *       {@code true}.</li>
     *   <li>Destination's {@link com.example.examplemod.inventory.core.slot.SlotAcceptRule}
     *       accepts the stack.</li>
     *   <li>Destination has enough space (partial insert is not allowed for quick move).</li>
     * </ul>
     *
     * @param player          the player (for accessing the vanilla inventory)
     * @param cap             the player's capability (server-side only)
     * @param profile         the current resolved layout profile
     * @param playerInventorySlot  the source slot index in the vanilla player inventory
     * @return transaction result; on failure the capability state is unchanged
     */
    @NotNull
    public TransactionResult quickMoveFromVanillaSlot(@NotNull ServerPlayer player,
                                                      @NotNull IExtendedInventoryCapability cap,
                                                      @NotNull PlayerLayoutProfile profile,
                                                      int playerInventorySlot) {
        if (playerInventorySlot < 0 || playerInventorySlot >= player.getInventory().items.size()) {
            return TransactionResult.rejected("quickMoveFromVanillaSlot: invalid player slot=" + playerInventorySlot);
        }

        InventorySnapshot snapshot = InventorySnapshot.take(cap);
        List<ItemStack> playerItemsSnapshot = new ArrayList<>(player.getInventory().items.size());
        for (ItemStack stack : player.getInventory().items) {
            playerItemsSnapshot.add(stack.copy());
        }

        try {
            ItemStack source = player.getInventory().items.get(playerInventorySlot).copy();
            if (source.isEmpty()) {
                return TransactionResult.rejected("quickMoveFromVanillaSlot: source empty slot=" + playerInventorySlot);
            }

            ItemStack remaining = source.copy();
            List<String> dirtySlots = new ArrayList<>();
            for (SlotDefinition slot : profile.getAllSlots()) {
                if (!slot.isEnabled()) {
                    continue;
                }
                int idx = profile.getSlotIndex(slot.getSlotId());
                if (idx < 0) {
                    continue;
                }
                SlotValidationResult valid = validateSlotPlacement(slot, remaining, cap, idx);
                if (!valid.isAllowed()) {
                    continue;
                }

                int before = remaining.getCount();
                remaining = cap.getInventory().insertItem(idx, remaining.copy(), false);
                if (remaining.getCount() < before) {
                    dirtySlots.add(slot.getSlotId());
                }
                if (remaining.isEmpty()) {
                    break;
                }
            }

            if (remaining.getCount() == source.getCount()) {
                restorePlayerItems(player, playerItemsSnapshot);
                snapshot.restore(cap);
                return TransactionResult.rolledBack("quickMoveFromVanillaSlot: no compatible extended slot");
            }

            player.getInventory().items.set(playerInventorySlot, remaining);
            cap.markDirty();
            return TransactionResult.committed(dirtySlots);
        } catch (Exception e) {
            LOGGER.error("[M7 transaction] quickMoveFromVanillaSlot unexpected error, rolling back", e);
            restorePlayerItems(player, playerItemsSnapshot);
            snapshot.restore(cap);
            return TransactionResult.rolledBack("quickMoveFromVanillaSlot internal error: " + e.getMessage());
        }
    }

    /**
     * Craft transaction for the M8 panel.
     */
    @NotNull
    public TransactionResult craftRecipe(@NotNull IExtendedInventoryCapability cap,
                                         @NotNull PlayerLayoutProfile profile,
                                         @NotNull List<CraftIngredientRequirement> ingredients,
                                         @NotNull ItemStack output) {
        if (ingredients.isEmpty() || output.isEmpty()) {
            return TransactionResult.rejected("craftRecipe: invalid recipe payload");
        }

        Map<Item, Integer> required = new LinkedHashMap<>();
        for (CraftIngredientRequirement ingredient : ingredients) {
            required.merge(ingredient.item(), ingredient.count(), Integer::sum);
        }

        InventorySnapshot snapshot = InventorySnapshot.take(cap);
        try {
            Map<String, Integer> consumePlan = new LinkedHashMap<>();
            Map<Item, Integer> remaining = new LinkedHashMap<>(required);

            for (SlotDefinition slot : profile.getAllSlots()) {
                int idx = profile.getSlotIndex(slot.getSlotId());
                if (idx < 0) {
                    continue;
                }
                ItemStack stack = cap.getInventory().getStackInSlot(idx);
                if (stack.isEmpty()) {
                    continue;
                }

                Integer needed = remaining.get(stack.getItem());
                if (needed == null || needed <= 0) {
                    continue;
                }

                int consume = Math.min(needed, stack.getCount());
                if (consume > 0) {
                    consumePlan.put(slot.getSlotId(), consume);
                    remaining.put(stack.getItem(), needed - consume);
                }
            }

            for (int missing : remaining.values()) {
                if (missing > 0) {
                    return TransactionResult.rolledBack("craftRecipe: missing ingredients");
                }
            }

            ItemStack outputRemainderDryRun = output.copy();
            for (SlotDefinition slot : profile.getAllSlots()) {
                if (!slot.isEnabled()) {
                    continue;
                }
                int idx = profile.getSlotIndex(slot.getSlotId());
                if (idx < 0) {
                    continue;
                }
                SlotValidationResult valid = validateSlotPlacement(slot, outputRemainderDryRun, cap, idx);
                if (!valid.isAllowed()) {
                    continue;
                }
                outputRemainderDryRun = valid.getRemainder();
                if (outputRemainderDryRun.isEmpty()) {
                    break;
                }
            }

            if (!outputRemainderDryRun.isEmpty()) {
                return TransactionResult.rolledBack("craftRecipe: no output space");
            }

            LinkedHashSet<String> dirty = new LinkedHashSet<>();
            for (Map.Entry<String, Integer> entry : consumePlan.entrySet()) {
                String slotId = entry.getKey();
                int consumeCount = entry.getValue();
                int idx = profile.getSlotIndex(slotId);
                if (idx < 0) {
                    throw new IllegalStateException("craftRecipe: planned slot disappeared id=" + slotId);
                }

                ItemStack current = cap.getInventory().getStackInSlot(idx).copy();
                if (current.isEmpty() || current.getCount() < consumeCount) {
                    throw new IllegalStateException("craftRecipe: inconsistent consume state for id=" + slotId);
                }

                current.shrink(consumeCount);
                cap.getInventory().setStackInSlot(idx, current);
                dirty.add(slotId);
            }

            ItemStack outputRemainder = output.copy();
            for (SlotDefinition slot : profile.getAllSlots()) {
                if (!slot.isEnabled()) {
                    continue;
                }
                int idx = profile.getSlotIndex(slot.getSlotId());
                if (idx < 0) {
                    continue;
                }
                SlotValidationResult valid = validateSlotPlacement(slot, outputRemainder, cap, idx);
                if (!valid.isAllowed()) {
                    continue;
                }

                int before = outputRemainder.getCount();
                outputRemainder = cap.getInventory().insertItem(idx, outputRemainder.copy(), false);
                if (outputRemainder.getCount() < before) {
                    dirty.add(slot.getSlotId());
                }
                if (outputRemainder.isEmpty()) {
                    break;
                }
            }

            if (!outputRemainder.isEmpty()) {
                throw new IllegalStateException("craftRecipe commit left unexpected remainder=" + outputRemainder.getCount());
            }

            cap.markDirty();
            return TransactionResult.committed(new ArrayList<>(dirty));
        } catch (Exception e) {
            LOGGER.error("[M8 transaction] craftRecipe unexpected error, rolling back", e);
            snapshot.restore(cap);
            return TransactionResult.rolledBack("craftRecipe internal error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Validation helpers (package-private for tests)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Validates whether a given {@link ItemStack} may be placed into a slot.
     *
     * <p>Checks in order:
     * <ol>
     *   <li>Slot {@link SlotDefinition#isEnabled()} — disabled slots reject all stacks.</li>
     *   <li>Slot {@link com.example.examplemod.inventory.core.slot.SlotAcceptRule} predicate.</li>
     *   <li>Forge {@link net.minecraftforge.items.ItemStackHandler#isItemValid} contract.</li>
     * </ol>
     *
     * @param slotDef the slot definition
     * @param stack   the stack to validate
     * @param cap     the handler (for Forge's isItemValid)
     * @param slotIdx the physical slot index in the handler
     * @return validation result
     */
    @NotNull
    SlotValidationResult validateSlotPlacement(@NotNull SlotDefinition slotDef,
                                                @NotNull ItemStack stack,
                                                @NotNull IExtendedInventoryCapability cap,
                                                int slotIdx) {
        if (!slotDef.isEnabled()) {
            return SlotValidationResult.rejected("slot disabled: " + slotDef.getSlotId());
        }
        if (!slotDef.getAcceptRule().test(stack)) {
            return SlotValidationResult.rejected("accept rule rejected: " + slotDef.getSlotId());
        }
        if (!cap.getInventory().isItemValid(slotIdx, stack)) {
            return SlotValidationResult.rejected("isItemValid=false: " + slotDef.getSlotId());
        }

        // Simulate insertion to compute remainder (uses Forge's internal capacity logic)
        ItemStack remainder = cap.getInventory().insertItem(slotIdx, stack.copy(), /* simulate */ true);
        if (remainder.getCount() == stack.getCount()) {
            // Nothing fits
            return SlotValidationResult.rejected("no capacity: " + slotDef.getSlotId());
        }
        return SlotValidationResult.allowed(remainder);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Private helpers
    // ═══════════════════════════════════════════════════════════════════════

    /** Collects all non-empty items from a slot group (preserving slot order). */
    private List<ItemInSlot> collectGroupItems(@NotNull IExtendedInventoryCapability cap,
                                                @NotNull PlayerLayoutProfile profile,
                                                @NotNull SlotGroupDefinition group) {
        List<ItemInSlot> result = new ArrayList<>();
        for (SlotDefinition slotDef : group.getSlots()) {
            int idx = profile.getSlotIndex(slotDef.getSlotId());
            if (idx < 0) continue;
            ItemStack stack = cap.getInventory().getStackInSlot(idx);
            if (!stack.isEmpty()) {
                result.add(new ItemInSlot(slotDef.getSlotId(), idx, stack.copy()));
            }
        }
        return result;
    }

    /** Clears all slots of a group in the capability handler. */
    private void clearGroupSlots(@NotNull IExtendedInventoryCapability cap,
                                  @NotNull PlayerLayoutProfile profile,
                                  @NotNull SlotGroupDefinition group) {
        for (SlotDefinition slotDef : group.getSlots()) {
            int idx = profile.getSlotIndex(slotDef.getSlotId());
            if (idx >= 0) {
                cap.getInventory().setStackInSlot(idx, ItemStack.EMPTY);
            }
        }
    }

    /**
     * Simulates relocating each overflow item into the remaining groups.
     * Does NOT mutate the capability.
     */
    private RelocateResult dryRunRelocate(@NotNull List<ItemInSlot> overflowItems,
                                           @NotNull List<SlotGroupDefinition> remainingGroups,
                                           @NotNull IExtendedInventoryCapability cap,
                                           @NotNull PlayerLayoutProfile profile) {
        // Build a working copy of remaining slot occupancy (slotId → simulated stack)
        java.util.Map<String, ItemStack> simSlots = new java.util.LinkedHashMap<>();
        for (SlotGroupDefinition group : remainingGroups) {
            for (SlotDefinition slot : group.getSlots()) {
                int simIdx = profile.getSlotIndex(slot.getSlotId());
                if (simIdx >= 0) {
                    simSlots.put(slot.getSlotId(), cap.getInventory().getStackInSlot(simIdx).copy());
                }
            }
        }

        List<Placement> placements = new ArrayList<>();
        List<ItemInSlot> unplaced = new ArrayList<>();

        for (ItemInSlot overflow : overflowItems) {
            ItemStack toPlace = overflow.stack().copy();
            boolean placed = false;

            for (SlotGroupDefinition group : remainingGroups) {
                for (SlotDefinition slot : group.getSlots()) {
                    if (!slot.isEnabled()) continue;
                    ItemStack current = simSlots.getOrDefault(slot.getSlotId(), ItemStack.EMPTY);
                    if (!current.isEmpty() && current.getCount() >= current.getMaxStackSize()) continue;

                    if (!slot.getAcceptRule().test(toPlace)) continue;

                    // Simulate insert into this simulated slot
                    if (current.isEmpty()) {
                        simSlots.put(slot.getSlotId(), toPlace.copy());
                        placements.add(new Placement(slot.getSlotId(), toPlace.copy()));
                        toPlace = ItemStack.EMPTY;
                        placed = true;
                        break;
                    } else if (ItemStack.isSameItemSameTags(current, toPlace)) {
                        int space = current.getMaxStackSize() - current.getCount();
                        int toMove = Math.min(space, toPlace.getCount());
                        current.grow(toMove);
                        toPlace.shrink(toMove);
                        simSlots.put(slot.getSlotId(), current);
                        placements.add(new Placement(slot.getSlotId(), current.copy()));
                        if (toPlace.isEmpty()) {
                            placed = true;
                            break;
                        }
                    }
                }
                if (placed || toPlace.isEmpty()) break;
            }

            if (!toPlace.isEmpty()) {
                unplaced.add(new ItemInSlot(overflow.slotId(), overflow.slotIdx(), toPlace));
            }
        }

        return new RelocateResult(placements, unplaced);
    }

    /** Applies the relocation placements to the real capability handler. */
    private void applyRelocation(@NotNull RelocateResult relocate,
                                  @NotNull IExtendedInventoryCapability cap,
                                  @NotNull PlayerLayoutProfile profile) {
        for (Placement p : relocate.placements()) {
            int pIdx = profile.getSlotIndex(p.slotId());
            if (pIdx >= 0) {
                cap.getInventory().setStackInSlot(pIdx, p.stack().copy());
            }
        }
    }

    /**
     * Handles the case where some items could not be placed — applies the overflow policy.
     */
    private TransactionResult handleUnplacedItems(@NotNull IExtendedInventoryCapability cap,
                                                    @NotNull InventorySnapshot snapshot,
                                                    @NotNull PlayerLayoutProfile profile,
                                                    @NotNull SlotGroupDefinition removedGroup,
                                                    @NotNull RelocateResult relocate,
                                                    @NotNull OverflowPolicy policy,
                                                    @Nullable ServerPlayer player) {
        switch (policy) {
            case DENY_REMOVE_IF_OVERFLOW -> {
                snapshot.restore(cap);
                return TransactionResult.rolledBack(
                        "equipmentRemoval: overflow items cannot be relocated, policy=DENY",
                        Component.translatable("examplemod.inventory.error.overflow_deny"));
            }

            case MOVE_TO_AVAILABLE_SLOTS_FIRST -> {
                // Partial: apply what we can, deny if there is still remainder
                snapshot.restore(cap);
                return TransactionResult.rolledBack(
                        "equipmentRemoval: partial relocation failed, policy=MOVE_FIRST. unplaced="
                                + relocate.unplaced().size(),
                        Component.translatable("examplemod.inventory.error.overflow_deny"));
            }

            case ALLOW_OVERFLOW_READONLY -> {
                // Commit whatever fits; leave overflow slots as read-only placeholders
                // (M4+ will add UI for this; for now, clear removed slots and place whatever fits)
                clearGroupSlots(cap, profile, removedGroup);
                applyRelocation(relocate, cap, profile);
                // Overflow items that didn't fit are lost from the removed group — intentional
                // for this policy (admin/operator emergency use only).
                LOGGER.warn("[M3 transaction] ALLOW_OVERFLOW_READONLY: {} items could not be placed, "
                        + "they are lost from the removed group={}", relocate.unplaced().size(),
                        removedGroup.getGroupId());
                cap.markDirty();
                return TransactionResult.committed(buildDirtyList(removedGroup, relocate));
            }

            case DROP_TO_WORLD_LAST_RESORT -> {
                clearGroupSlots(cap, profile, removedGroup);
                applyRelocation(relocate, cap, profile);
                if (player != null) {
                    for (ItemInSlot unplaced : relocate.unplaced()) {
                        player.drop(unplaced.stack().copy(), false);
                    }
                    LOGGER.warn("[M3 transaction] DROP_TO_WORLD: dropped {} items for player={}",
                            relocate.unplaced().size(), player.getGameProfile().getName());
                } else {
                    LOGGER.error("[M3 transaction] DROP_TO_WORLD: player is null, {} items lost!",
                            relocate.unplaced().size());
                }
                cap.markDirty();
                return TransactionResult.committed(buildDirtyList(removedGroup, relocate));
            }

            default -> {
                snapshot.restore(cap);
                return TransactionResult.rolledBack("unknown overflow policy: " + policy);
            }
        }
    }

    private List<String> removedGroupSlotIds(@NotNull SlotGroupDefinition group) {
        List<String> ids = new ArrayList<>();
        for (SlotDefinition s : group.getSlots()) {
            ids.add(s.getSlotId());
        }
        return ids;
    }

    private List<String> buildDirtyList(@NotNull SlotGroupDefinition removedGroup,
                                         @NotNull RelocateResult relocate) {
        List<String> dirty = new ArrayList<>(removedGroupSlotIds(removedGroup));
        for (Placement p : relocate.placements()) {
            if (!dirty.contains(p.slotId())) dirty.add(p.slotId());
        }
        return dirty;
    }

    private void debugLog(String msg, Object... args) {
        if (Config.debugLifecycleLogs) {
            LOGGER.info("[M3 transaction] " + msg, args);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Inner record types (package-private for tests)
    // ═══════════════════════════════════════════════════════════════════════

    /** Holds an item found in a slot during collection. */
    record ItemInSlot(String slotId, int slotIdx, ItemStack stack) {}

    /** Single placement decision produced by dry-run relocation. */
    record Placement(String slotId, ItemStack stack) {}

    /** Result of the dry-run relocation simulation. */
    record RelocateResult(List<Placement> placements, List<ItemInSlot> unplaced) {
        boolean allPlaced() { return unplaced.isEmpty(); }
    }

    @Nullable
    private TransferTarget findFirstCompatibleTarget(@NotNull IExtendedInventoryCapability cap,
                                                     @NotNull PlayerLayoutProfile profile,
                                                     @NotNull String sourceSlotId,
                                                     @NotNull ItemStack moving) {
        for (SlotDefinition slot : profile.getAllSlots()) {
            if (!slot.isEnabled() || slot.getSlotId().equals(sourceSlotId)) {
                continue;
            }
            int idx = profile.getSlotIndex(slot.getSlotId());
            if (idx < 0) {
                continue;
            }
            SlotValidationResult validation = validateSlotPlacement(slot, moving, cap, idx);
            if (!validation.isAllowed()) {
                continue;
            }
            ItemStack simulatedRemainder = cap.getInventory().insertItem(idx, moving.copy(), true);
            if (simulatedRemainder.isEmpty()) {
                return new TransferTarget(slot.getSlotId(), idx);
            }
        }
        return null;
    }

    private boolean isWholeStackReplaceable(@NotNull SlotDefinition targetSlot,
                                            @NotNull ItemStack moving,
                                            @NotNull IExtendedInventoryCapability cap,
                                            int targetSlotIdx) {
        if (moving.isEmpty()) {
            return true;
        }
        if (!targetSlot.getAcceptRule().test(moving) || !cap.getInventory().isItemValid(targetSlotIdx, moving)) {
            return false;
        }
        int slotLimit = Math.min(cap.getInventory().getSlotLimit(targetSlotIdx), moving.getMaxStackSize());
        return moving.getCount() <= slotLimit;
    }

    private static void restorePlayerItems(@NotNull ServerPlayer player,
                                           @NotNull List<ItemStack> snapshot) {
        int limit = Math.min(player.getInventory().items.size(), snapshot.size());
        for (int i = 0; i < limit; i++) {
            player.getInventory().items.set(i, snapshot.get(i).copy());
        }
    }

    private static @NotNull List<ItemStack> copyPlayerItems(@NotNull ServerPlayer player) {
        List<ItemStack> snapshot = new ArrayList<>(player.getInventory().items.size());
        for (ItemStack stack : player.getInventory().items) {
            snapshot.add(stack.copy());
        }
        return snapshot;
    }

    private static @NotNull ItemStack insertIntoVanillaRange(@NotNull List<ItemStack> vanillaSlots,
                                                              @NotNull ItemStack stack,
                                                              int startInclusive,
                                                              int endExclusive) {
        ItemStack remaining = stack.copy();

        for (int i = startInclusive; i < endExclusive && !remaining.isEmpty(); i++) {
            ItemStack existing = vanillaSlots.get(i);
            if (!canStackTogether(existing, remaining)) {
                continue;
            }
            int max = Math.min(existing.getMaxStackSize(), remaining.getMaxStackSize());
            int free = max - existing.getCount();
            if (free <= 0) {
                continue;
            }
            int moved = Math.min(free, remaining.getCount());
            existing.grow(moved);
            remaining.shrink(moved);
        }

        for (int i = startInclusive; i < endExclusive && !remaining.isEmpty(); i++) {
            ItemStack existing = vanillaSlots.get(i);
            if (!existing.isEmpty()) {
                continue;
            }
            int moved = Math.min(remaining.getMaxStackSize(), remaining.getCount());
            ItemStack placed = remaining.copy();
            placed.setCount(moved);
            vanillaSlots.set(i, placed);
            remaining.shrink(moved);
        }

        return remaining;
    }

    private static boolean canStackTogether(@NotNull ItemStack first, @NotNull ItemStack second) {
        return !first.isEmpty() && ItemStack.isSameItemSameTags(first, second);
    }

    /**
     * Moves an item stack from a vanilla player inventory slot to an extended mod slot.
     *
     * <p>Dry-run phase checks:
     * <ul>
     *   <li>Source slot is within range and contains a non-empty stack.</li>
     *   <li>Destination slot is within range and its {@link SlotDefinition#isEnabled()} is
     *       {@code true}.</li>
     *   <li>Destination's {@link com.example.examplemod.inventory.core.slot.SlotAcceptRule}
     *       accepts the stack.</li>
     *   <li>Destination has enough space (partial insert is not allowed for a move).</li>
     * </ul>
     *
     * @param player          the player (for accessing the vanilla inventory)
     * @param cap             the player's capability (server-side only)
     * @param profile         the current resolved layout profile
     * @param playerInventorySlot  the source slot index in the vanilla player inventory
     * @param targetSlotId    the destination slot ID in the mod inventory
     * @return transaction result; on failure the capability state is unchanged
     */
    @NotNull
    public TransactionResult moveVanillaToExtendedSlot(@NotNull ServerPlayer player,
                                                       @NotNull IExtendedInventoryCapability cap,
                                                       @NotNull PlayerLayoutProfile profile,
                                                       int playerInventorySlot,
                                                       @NotNull String targetSlotId) {
        if (playerInventorySlot < 0 || playerInventorySlot >= player.getInventory().items.size()) {
            return TransactionResult.rejected("moveVanillaToExtendedSlot: invalid player slot=" + playerInventorySlot);
        }

        int targetIdx = profile.getSlotIndex(targetSlotId);
        if (targetIdx < 0) {
            return TransactionResult.rejected("moveVanillaToExtendedSlot: unknown targetSlotId=" + targetSlotId);
        }

        SlotDefinition targetSlot = profile.findSlot(targetSlotId);
        if (targetSlot == null || !targetSlot.isEnabled()) {
            return TransactionResult.rolledBack("moveVanillaToExtendedSlot: disabled or missing target slot");
        }

        InventorySnapshot snapshot = InventorySnapshot.take(cap);
        List<ItemStack> playerItemsSnapshot = copyPlayerItems(player);

        try {
            ItemStack source = player.getInventory().items.get(playerInventorySlot).copy();
            if (source.isEmpty()) {
                return TransactionResult.rejected("moveVanillaToExtendedSlot: source empty slot=" + playerInventorySlot);
            }

            SlotValidationResult validation = validateSlotPlacement(targetSlot, source, cap, targetIdx);
            if (!validation.isAllowed() || !validation.getRemainder().isEmpty()) {
                restorePlayerItems(player, playerItemsSnapshot);
                snapshot.restore(cap);
                return TransactionResult.rolledBack("moveVanillaToExtendedSlot: target rejected item or has no space");
            }

            player.getInventory().items.set(playerInventorySlot, ItemStack.EMPTY);
            ItemStack remainder = cap.getInventory().insertItem(targetIdx, source.copy(), false);
            if (!remainder.isEmpty()) {
                throw new IllegalStateException("moveVanillaToExtendedSlot: unexpected remainder=" + remainder.getCount());
            }

            cap.markDirty();
            return TransactionResult.committed(List.of(targetSlotId));
        } catch (Exception e) {
            LOGGER.error("[M8 transaction] moveVanillaToExtendedSlot unexpected error, rolling back", e);
            restorePlayerItems(player, playerItemsSnapshot);
            snapshot.restore(cap);
            return TransactionResult.rolledBack("moveVanillaToExtendedSlot internal error: " + e.getMessage());
        }
    }

    /**
     * Moves an item stack from an extended mod slot to a vanilla player inventory slot.
     *
     * <p>Dry-run phase checks:
     * <ul>
     *   <li>Source slot is within range and contains a non-empty stack.</li>
     *   <li>Destination slot is within range and its {@link SlotDefinition#isEnabled()} is
     *       {@code true}.</li>
     *   <li>Destination's {@link com.example.examplemod.inventory.core.slot.SlotAcceptRule}
     *       accepts the stack.</li>
     *   <li>Destination has enough space (partial insert is not allowed for a move).</li>
     * </ul>
     *
     * @param player          the player (for accessing the vanilla inventory)
     * @param cap             the player's capability (server-side only)
     * @param profile         the current resolved layout profile
     * @param sourceSlotId    the source slot ID in the mod inventory
     * @param creativeMode    if true, allows moving to the full vanilla inventory (creative mode)
     * @return transaction result; on failure the capability state is unchanged
     */
    @NotNull
    public TransactionResult moveExtendedToVanilla(@NotNull ServerPlayer player,
                                                   @NotNull IExtendedInventoryCapability cap,
                                                   @NotNull PlayerLayoutProfile profile,
                                                   @NotNull String sourceSlotId,
                                                   boolean creativeMode) {
        int sourceIdx = profile.getSlotIndex(sourceSlotId);
        if (sourceIdx < 0) {
            return TransactionResult.rejected("moveExtendedToVanilla: unknown sourceSlotId=" + sourceSlotId);
        }

        SlotDefinition sourceDef = profile.findSlot(sourceSlotId);
        if (sourceDef != null && !sourceDef.isEnabled()) {
            return TransactionResult.rolledBack("moveExtendedToVanilla: source slot disabled");
        }

        int allowedEndExclusive = creativeMode ? 36 : 9;
        InventorySnapshot snapshot = InventorySnapshot.take(cap);
        List<ItemStack> playerItemsSnapshot = copyPlayerItems(player);

        try {
            ItemStack source = cap.getInventory().getStackInSlot(sourceIdx).copy();
            if (source.isEmpty()) {
                return TransactionResult.rejected("moveExtendedToVanilla: source slot empty id=" + sourceSlotId);
            }

            ItemStack remainder = insertIntoVanillaRange(player.getInventory().items, source.copy(), 0, allowedEndExclusive);
            if (!remainder.isEmpty()) {
                restorePlayerItems(player, playerItemsSnapshot);
                snapshot.restore(cap);
                return TransactionResult.rolledBack("moveExtendedToVanilla: no vanilla space in allowed range");
            }

            cap.getInventory().setStackInSlot(sourceIdx, ItemStack.EMPTY);
            cap.markDirty();
            return TransactionResult.committed(List.of(sourceSlotId));
        } catch (Exception e) {
            LOGGER.error("[M8 transaction] moveExtendedToVanilla unexpected error, rolling back", e);
            restorePlayerItems(player, playerItemsSnapshot);
            snapshot.restore(cap);
            return TransactionResult.rolledBack("moveExtendedToVanilla internal error: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Inner class: TransferTarget
    // ═══════════════════════════════════════════════════════════════════════

    private record TransferTarget(String slotId, int slotIndex) {}
}
