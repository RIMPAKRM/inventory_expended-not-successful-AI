package com.example.examplemod.inventory.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Public server-side API for external mods.
 * Implementations must remain server authoritative and transactional.
 */
public interface IExtendedInventoryApi {

    @NotNull
    List<InventorySlotView> snapshot(@NotNull ServerPlayer actor,
                                     @NotNull ServerPlayer target,
                                     @NotNull InventoryAccessRequest request);

    @NotNull
    Optional<InventorySlotView> findFirst(@NotNull ServerPlayer actor,
                                          @NotNull ServerPlayer target,
                                          @NotNull InventoryAccessRequest request,
                                          @NotNull Predicate<InventorySlotView> filter);

    @NotNull
    InventoryMutationResult move(@NotNull ServerPlayer actor,
                                 @NotNull ServerPlayer target,
                                 @NotNull InventoryAccessRequest request,
                                 @NotNull String fromSlotId,
                                 @NotNull String toSlotId);

    @NotNull
    InventoryMutationResult extract(@NotNull ServerPlayer actor,
                                    @NotNull ServerPlayer target,
                                    @NotNull InventoryAccessRequest request,
                                    @NotNull String slotId);

    @NotNull
    InventoryMutationResult insert(@NotNull ServerPlayer actor,
                                   @NotNull ServerPlayer target,
                                   @NotNull InventoryAccessRequest request,
                                   @NotNull ItemStack stack);
}
