package com.example.examplemod.inventory.core.slot;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Capability interface implemented by items that contribute additional inventory slots
 * to the player when equipped.
 *
 * <p>Any item (jacket, vest, chest rig, backpack, etc.) that wants to add slots to the
 * player's extended inventory should implement this interface via a Forge capability
 * attached to the {@link net.minecraft.world.item.ItemStack}.</p>
 *
 * <p><strong>Server-side only.</strong> The server collects groups from all equipped
 * items through {@link com.example.examplemod.inventory.core.layout.EquipmentLayoutService}
 * and caches the resolved {@link PlayerLayoutProfile} in the player's capability. The
 * client receives only the resolved layout data (slot count, slot ids, display info) via
 * a sync packet — it never calls this interface directly.</p>
 *
 * <p>External mods (e.g. a weapons mod) implement this interface and attach it to their
 * item stacks to integrate with the extended inventory system without depending on
 * any internal implementation class.</p>
 *
 * <h2>Usage example</h2>
 * <pre>{@code
 * // Inside the item's capability attachment event:
 * stack.getCapability(ExtendedInventorySlotProviderCap.CAP, null)
 *      .ifPresent(provider -> layout = provider.getSlotGroups(stack));
 * }</pre>
 *
 * @see SlotGroupDefinition
 * @see PlayerLayoutProfile
 */
public interface IEquipmentSlotProvider {

    /**
     * Returns the list of {@link SlotGroupDefinition slot groups} that this item
     * contributes to the player's inventory when equipped.
     *
     * <p>This method is called on the <strong>server thread</strong> every time the
     * layout needs to be rebuilt (e.g. on equip/unequip or respawn). Implementations
     * should be deterministic and cheap — avoid heavy computation or I/O.</p>
     *
     * <p>All {@link SlotDefinition} instances returned must have globally unique
     * {@code slotId} values. The recommended id format is:
     * {@code "<mod_id>:<item_registry_name>:<slot_purpose>_<index>"},
     * for example: {@code "myweaponmod:tactical_vest:mag_pouch_0"}.</p>
     *
     * <p>Groups whose slots carry {@link SlotSource#EQUIPMENT} are expected here.
     * Groups with {@link SlotSource#CONTAINER} may be returned if the item also
     * directly provides a container (e.g. a backpack with built-in compartments).</p>
     *
     * @param stack the equipped {@link ItemStack}; never empty when this method is called
     * @return an ordered, non-null list of slot group definitions;
     *         may be empty if the item currently provides no extra slots
     */
    @NotNull
    List<SlotGroupDefinition> getSlotGroups(@NotNull ItemStack stack);

    /**
     * Returns a stable version token for the layout contributed by this item.
     *
     * <p>The token is a compact string that changes whenever the set or configuration
     * of contributed slots changes (e.g. when the item is upgraded or its NBT changes
     * the slot count). The layout service uses this token to decide whether the cached
     * layout is still valid without rebuilding the full group list.</p>
     *
     * <p>A simple implementation may return the item's registry name combined with
     * a relevant NBT tag value. Return an empty string if versioning is not needed.</p>
     *
     * @param stack the equipped item stack
     * @return a stable, non-null version token string; must not be {@code null}
     */
    @NotNull
    default String getLayoutVersionToken(@NotNull ItemStack stack) {
        return stack.getItem().getDescriptionId();
    }
}

