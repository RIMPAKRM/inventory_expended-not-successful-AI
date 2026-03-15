package com.example.examplemod.inventory.api;

import com.example.examplemod.inventory.core.slot.SlotSource;
import com.example.examplemod.inventory.core.slot.SlotType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable read model for external API consumers.
 */
public record InventorySlotView(
        @NotNull String slotId,
        @NotNull SlotType slotType,
        @NotNull SlotSource source,
        boolean enabled,
        @NotNull ItemStack stack
) {
    public InventorySlotView {
        stack = stack.copy();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }
}

