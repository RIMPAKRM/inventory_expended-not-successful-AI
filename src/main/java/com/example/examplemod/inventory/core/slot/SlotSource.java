package com.example.examplemod.inventory.core.slot;

/**
 * Describes the origin of an inventory slot — who or what created it.
 *
 * <p>This is used by the layout resolver to understand whether a slot comes from the
 * base configuration, an equipped item, or an attached container (e.g. a backpack
 * inside a chest rig). It also drives decisions about how to handle overflow when a
 * slot source is removed.</p>
 */
public enum SlotSource {

    /**
     * The slot is part of the player's base inventory layout defined by the mod config.
     * Base slots are always present unless explicitly disabled in configuration.
     */
    BASE,

    /**
     * The slot was contributed by an equipped item (jacket, vest, chest rig, etc.)
     * via {@link IEquipmentSlotProvider}. These slots exist only while the contributing
     * item is equipped and will be removed (with overflow handling) when unequipped.
     */
    EQUIPMENT,

    /**
     * The slot is provided by an attached container item (e.g. a backpack, pouch, or
     * medical kit placed inside an equipment slot). Removing the container item will
     * trigger overflow handling for the slots it provided.
     */
    CONTAINER,

    /**
     * A temporary slot, e.g. used during drag-and-drop operations or crafting previews.
     * Temporary slots are not persisted and are cleared at the end of the interaction.
     */
    TEMPORARY;
}

