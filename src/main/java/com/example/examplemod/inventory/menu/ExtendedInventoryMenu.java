package com.example.examplemod.inventory.menu;

import com.example.examplemod.inventory.client.screen.InventoryLayoutConstants;
import com.example.examplemod.inventory.core.capability.IExtendedInventoryCapability;
import com.example.examplemod.inventory.core.slot.PlayerLayoutProfile;
import com.example.examplemod.inventory.core.slot.SlotDefinition;
import com.example.examplemod.inventory.core.sync.InventorySyncService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * M8 menu with only Forge slots.
 *
 * <p>Vanilla hotbar is always present, vanilla main (3x9) is present only in creative,
 * and all extended equipment/dynamic slots are registered as Forge {@link SlotItemHandler} slots.</p>
 */
public final class ExtendedInventoryMenu extends AbstractContainerMenu {

    /** Индекс первого hotbar-слота в этом меню (для quickMoveStack). */
    public static final int HOTBAR_SLOT_START = 0;
    /** Индекс последнего hotbar-слота (включительно). */
    public static final int HOTBAR_SLOT_END   = 8;
    /** Индекс первого main-слота (только Creative). */
    public static final int MAIN_SLOT_START   = 9;
    /** Индекс последнего main-слота (включительно). */
    public static final int MAIN_SLOT_END     = 35;

    private final boolean isCreative;
    private final int vanillaSlotEndExclusive;
    private final int modSlotStart;
    private final int modSlotEndInclusive;

    private final List<String> orderedModSlotIds;
    private final Map<String, Integer> menuIndexByModSlotId;

    @Nullable
    private final ServerPlayer serverPlayer;
    @Nullable
    private final IExtendedInventoryCapability serverCapability;
    private long lastObservedRevision;
    private final Map<String, ItemStack> lastObservedModStacks;

    public ExtendedInventoryMenu(int containerId,
                                 @NotNull Inventory playerInventory,
                                 @Nullable FriendlyByteBuf data) {
        this(containerId,
                playerInventory,
                null,
                null,
                readModSlotIds(data),
                null,
                null);
    }

    public ExtendedInventoryMenu(int containerId,
                                 @NotNull Inventory playerInventory,
                                 @NotNull ServerPlayer player,
                                 @NotNull IExtendedInventoryCapability capability,
                                 @NotNull PlayerLayoutProfile profile) {
        this(containerId,
                playerInventory,
                profile,
                capability,
                orderedSlotIds(profile),
                player,
                capability.getInventory());
    }

    private ExtendedInventoryMenu(int containerId,
                                  @NotNull Inventory playerInventory,
                                  @Nullable PlayerLayoutProfile profile,
                                  @Nullable IExtendedInventoryCapability capability,
                                  @NotNull List<String> orderedSlotIds,
                                  @Nullable ServerPlayer player,
                                  @Nullable IItemHandler serverHandler) {
        super(ModMenus.EXTENDED_INVENTORY.get(), containerId);

        this.isCreative = playerInventory.player.isCreative();
        this.vanillaSlotEndExclusive = this.isCreative ? 36 : 9;
        this.serverPlayer = player;
        this.serverCapability = capability;
        this.orderedModSlotIds = List.copyOf(orderedSlotIds);
        this.menuIndexByModSlotId = new LinkedHashMap<>();
        this.lastObservedModStacks = new LinkedHashMap<>();

        addVanillaSlots(playerInventory);

        IItemHandler modHandler = serverHandler != null
                ? serverHandler
                : new ItemStackHandler(this.orderedModSlotIds.size());
        ModSlotLayoutResolver.Layout layout = ModSlotLayoutResolver.resolve(this.orderedModSlotIds);

        int addedModSlots = 0;
        for (int i = 0; i < this.orderedModSlotIds.size(); i++) {
            String slotId = this.orderedModSlotIds.get(i);
            ModSlotLayoutResolver.SlotPosition pos = layout.positions().get(slotId);
            if (pos == null) {
                continue;
            }

            int handlerIndex = profile != null ? profile.getSlotIndex(slotId) : i;
            if (handlerIndex < 0 || handlerIndex >= modHandler.getSlots()) {
                continue;
            }

            SlotDefinition definition = profile != null ? profile.findSlot(slotId) : null;
            this.addSlot(new ExtendedModSlot(modHandler, handlerIndex, pos.x(), pos.y(), definition));
            this.menuIndexByModSlotId.put(slotId, this.slots.size() - 1);
            this.lastObservedModStacks.put(slotId, this.slots.get(this.slots.size() - 1).getItem().copy());
            addedModSlots++;
        }

        this.modSlotStart = this.vanillaSlotEndExclusive;
        this.modSlotEndInclusive = this.modSlotStart + addedModSlots - 1;
        this.lastObservedRevision = this.serverCapability != null ? this.serverCapability.getRevision() : 0L;
    }

    private void addVanillaSlots(@NotNull Inventory playerInventory) {
        // ── Hotbar (слоты 0..8 ванильного инвентаря) — всегда ────────────
        // Slot(container, vanillaIndex, guiX, guiY)
        // guiX/guiY — координаты относительно leftPos/topPos экрана
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory,
                    col,                                                // vanilla slot index 0-8 = hotbar
                    InventoryLayoutConstants.HOTBAR_X + col * InventoryLayoutConstants.SLOT_STEP,
                    InventoryLayoutConstants.HOTBAR_Y));
        }

        // ── Main-инвентарь 3×9 (слоты 9..35) — только Creative ───────────
        if (isCreative) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    this.addSlot(new Slot(playerInventory,
                            9 + row * 9 + col,                          // vanilla slot index 9-35
                            InventoryLayoutConstants.VANILLA_MAIN_X + col * InventoryLayoutConstants.SLOT_STEP,
                            InventoryLayoutConstants.VANILLA_MAIN_Y + row * InventoryLayoutConstants.SLOT_STEP));
                }
            }
        }
    }

    /** @return {@code true} если меню открыто в Creative-режиме. */
    public boolean isCreative() {
        return isCreative;
    }

    /**
     * Shift-клик между ванильной частью меню и расширенными слотами.
     * В survival vanilla-часть ограничена hotbar (0..8), в creative доступен 0..35.
     */
    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot clickedSlot = this.slots.get(index);
        if (!clickedSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = clickedSlot.getItem();
        ItemStack stackCopy = sourceStack.copy();
        boolean moved;

        if (isModMenuSlot(index)) {
            moved = this.moveItemStackTo(sourceStack, HOTBAR_SLOT_START, this.vanillaSlotEndExclusive, false);
        } else if (index < this.vanillaSlotEndExclusive && hasModSlots()) {
            moved = this.moveItemStackTo(sourceStack, this.modSlotStart, this.modSlotEndInclusive + 1, false);
        } else if (isCreative && index >= MAIN_SLOT_START && index <= MAIN_SLOT_END) {
            moved = this.moveItemStackTo(sourceStack, HOTBAR_SLOT_START, HOTBAR_SLOT_END + 1, false);
        } else {
            moved = false;
        }

        if (!moved) {
            return ItemStack.EMPTY;
        }

        if (sourceStack.isEmpty()) {
            clickedSlot.set(ItemStack.EMPTY);
        } else {
            clickedSlot.setChanged();
        }
        clickedSlot.onTake(player, sourceStack);
        return stackCopy;
    }

    private boolean hasModSlots() {
        return this.modSlotEndInclusive >= this.modSlotStart;
    }

    private boolean isModMenuSlot(int menuIndex) {
        return hasModSlots() && menuIndex >= this.modSlotStart && menuIndex <= this.modSlotEndInclusive;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (this.serverCapability == null || this.serverPlayer == null) {
            return;
        }

        long currentRevision = this.serverCapability.getRevision();
        if (currentRevision == this.lastObservedRevision) {
            return;
        }

        List<String> dirtySlotIds = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : this.menuIndexByModSlotId.entrySet()) {
            String slotId = entry.getKey();
            int menuIndex = entry.getValue();
            if (menuIndex < 0 || menuIndex >= this.slots.size()) {
                continue;
            }

            ItemStack current = this.slots.get(menuIndex).getItem().copy();
            ItemStack previous = this.lastObservedModStacks.getOrDefault(slotId, ItemStack.EMPTY);
            if (!ItemStack.matches(previous, current)) {
                dirtySlotIds.add(slotId);
                this.lastObservedModStacks.put(slotId, current);
            }
        }

        if (!dirtySlotIds.isEmpty()) {
            InventorySyncService.getInstance().queuePartialSync(this.serverPlayer, dirtySlotIds);
        }
        this.lastObservedRevision = currentRevision;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    public @NotNull List<String> orderedModSlotIds() {
        return this.orderedModSlotIds;
    }

    public @NotNull Map<String, ItemStack> modSlotSnapshot() {
        Map<String, ItemStack> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : this.menuIndexByModSlotId.entrySet()) {
            int menuIndex = entry.getValue();
            if (menuIndex < 0 || menuIndex >= this.slots.size()) {
                continue;
            }
            snapshot.put(entry.getKey(), this.slots.get(menuIndex).getItem().copy());
        }
        return snapshot;
    }

    public static void writeModSlotIds(@NotNull FriendlyByteBuf buf, @NotNull PlayerLayoutProfile profile) {
        List<String> ordered = orderedSlotIds(profile);
        buf.writeVarInt(ordered.size());
        for (String slotId : ordered) {
            buf.writeUtf(slotId, 128);
        }
    }

    static @NotNull List<String> readModSlotIds(@Nullable FriendlyByteBuf buf) {
        if (buf == null) {
            return List.of();
        }
        int count = Math.max(0, buf.readVarInt());
        List<String> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(buf.readUtf(128));
        }
        return ids;
    }

    private static @NotNull List<String> orderedSlotIds(@NotNull PlayerLayoutProfile profile) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (SlotDefinition slot : profile.getAllSlots()) {
            ids.add(slot.getSlotId());
        }
        return List.copyOf(ids);
    }

    private static final class ExtendedModSlot extends SlotItemHandler {
        @Nullable
        private final SlotDefinition definition;

        private ExtendedModSlot(@NotNull IItemHandler itemHandler,
                                int index,
                                int xPosition,
                                int yPosition,
                                @Nullable SlotDefinition definition) {
            super(itemHandler, index, xPosition, yPosition);
            this.definition = definition;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            if (this.definition != null) {
                if (!this.definition.isEnabled()) {
                    return false;
                }
                if (!this.definition.getAcceptRule().test(stack)) {
                    return false;
                }
            }
            return super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(@NotNull Player playerIn) {
            return this.definition == null || this.definition.isEnabled();
        }
    }
}
