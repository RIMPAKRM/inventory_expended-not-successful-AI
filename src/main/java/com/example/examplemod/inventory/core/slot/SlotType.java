package com.example.examplemod.inventory.core.slot;

/**
 * Defines the semantic type (category) of an inventory slot.
 *
 * <p>Each SlotType represents a specific equipment or storage zone on the player.
 * Items may only be placed into compatible slots based on accept rules derived
 * from the slot's type. The GUI groups slots by their type for display ordering.</p>
 *
 * <p>Note: the vanilla hotbar and inventory are not managed by this type;
 * they remain separate and are referenced through vanilla mechanisms.</p>
 *
 * <p>New types can be added without changing existing saves because slot
 * definitions reference their type by enum constant name, and unknown names
 * will be ignored (overflow/read-only fallback in a future migration).</p>
 */
public enum SlotType {

    // ── Body / Worn Equipment ─────────────────────────────────────────────

    /** Outer jacket / coat. Typically allows 1–2 small pocket sub-slots. */
    BODY_OUTERWEAR,

    /** Soft/hard body armour vest. Provides protection; may or may not add slots. */
    BODY_ARMOR,

    /** Chest rig / plate carrier / chest harness. Usually adds magazine and utility pouches. */
    CHEST_RIG,

    /** Full face mask, gas mask, respirator, ballistic visor. */
    FACE_MASK,

    /** Helmet or headgear. */
    HEAD,

    /** Gloves / hand protection. */
    HANDS,

    /** Footwear / boots. */
    FEET,

    /** Pants / lower-body clothing. May allow a couple of pocket sub-slots. */
    LEGS,

    // ── Carry / Load-bearing ──────────────────────────────────────────────

    /** Backpack or rucksack. Opens a large secondary storage block. */
    BACK,

    /** Belt / holster rig. Usually holds sidearms and small items. */
    BELT,

    // ── Storage Pouches & Containers ─────────────────────────────────────

    /**
     * General-purpose utility pouch or container attached to an equipment item.
     * Used when the exact pouch type is not more specific.
     */
    UTILITY,

    /** Magazine pouch — designed for firearm magazines. */
    MAG_POUCH,

    /** Medical/first-aid pouch. */
    MEDICAL,

    /** Radio carrier or communication device slot. */
    RADIO,

    /** Patch panel / morale patch slot (cosmetic or minor stat). */
    PATCH,

    // ── Special / Dynamic ────────────────────────────────────────────────

    /**
     * A slot whose category is determined at runtime by a mod or item.
     * Resolved dynamically via {@link IEquipmentSlotProvider}.
     */
    DYNAMIC,

    /**
     * A slot that is no longer defined in the current configuration or layout.
     * Items stored here are accessible in read-only overflow mode to prevent loss.
     */
    OVERFLOW_READONLY;

    /**
     * Returns {@code true} if this type normally represents a worn piece of equipment
     * (body, head, hands, feet, face) rather than a storage container.
     */
    public boolean isEquipmentWorn() {
        return switch (this) {
            case BODY_OUTERWEAR, BODY_ARMOR, CHEST_RIG, FACE_MASK, HEAD, HANDS, FEET, LEGS -> true;
            default -> false;
        };
    }

    /**
     * Returns {@code true} if this type is a storage/carry container
     * (back, belt, pouches, utility, medical, etc.).
     */
    public boolean isStorage() {
        return switch (this) {
            case BACK, BELT, UTILITY, MAG_POUCH, MEDICAL, RADIO, PATCH -> true;
            default -> false;
        };
    }
}

