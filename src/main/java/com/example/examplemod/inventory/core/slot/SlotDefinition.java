package com.example.examplemod.inventory.core.slot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Immutable description of a single inventory slot.
 *
 * <p>A {@code SlotDefinition} describes everything about a slot <em>except</em> its content.
 * It is used by the layout resolver to build a {@link PlayerLayoutProfile} and by the GUI
 * to position and render each slot correctly.</p>
 *
 * <p>Slots are identified by a unique {@link #slotId} string within a player's resolved
 * layout. The id must be stable across sessions so that items can be matched back to their
 * slot after a save/load cycle.</p>
 *
 * <h2>UI positioning</h2>
 * <p>The {@link #uiAnchor} and {@link #uiPriority} fields are hints for the client-side
 * layout engine introduced in Etap 3 GUI. For now they carry the string value and an
 * ordering integer so the renderer can place slots without hardcoded coordinates.</p>
 */
public final class SlotDefinition {

    /** The slot type / category (body, storage, etc.). */
    private final SlotType slotType;

    /**
     * Unique identifier for this slot within the player's layout.
     * Format: {@code "<source_namespace>:<meaningful_name>"}, e.g.
     * {@code "examplemod:chest_rig_mag_0"} or {@code "examplemod:base_general_0"}.
     */
    private final String slotId;

    /**
     * Human-readable display group used by the GUI to group related slots
     * under a common heading (e.g. "Chest Rig", "Jacket Pockets", "Backpack").
     */
    private final String displayGroup;

    /** The source that contributed this slot (base config, equipment item, container, etc.). */
    private final SlotSource source;

    /** Controls which items may be placed in this slot. */
    private final SlotAcceptRule acceptRule;

    /**
     * Ordering hint within the display group. Lower values appear first.
     * Ties are broken by {@link #slotId} lexicographic order.
     */
    private final int order;

    /**
     * Whether the slot is currently visible in the GUI.
     * Hidden slots still exist and hold items; they are just not rendered.
     */
    private final boolean visible;

    /**
     * Whether items can be placed into or removed from this slot.
     * A disabled (read-only) slot still shows its contents but rejects all mutations
     * except server-side overflow operations.
     */
    private final boolean enabled;

    /**
     * Optional GUI anchor hint for the client layout engine.
     * Examples: {@code "left_panel"}, {@code "right_column"}, {@code "top_row"}.
     * {@code null} means the layout engine may place the slot anywhere within its group.
     */
    @Nullable
    private final String uiAnchor;

    /**
     * Priority used by the client layout engine when multiple groups compete
     * for the same anchor region. Higher priority groups/slots are placed first.
     */
    private final int uiPriority;

    private SlotDefinition(Builder builder) {
        this.slotType = Objects.requireNonNull(builder.slotType, "slotType");
        this.slotId = Objects.requireNonNull(builder.slotId, "slotId");
        this.displayGroup = Objects.requireNonNull(builder.displayGroup, "displayGroup");
        this.source = Objects.requireNonNull(builder.source, "source");
        this.acceptRule = Objects.requireNonNull(builder.acceptRule, "acceptRule");
        this.order = builder.order;
        this.visible = builder.visible;
        this.enabled = builder.enabled;
        this.uiAnchor = builder.uiAnchor;
        this.uiPriority = builder.uiPriority;
    }

    // ── Accessors ──────────────────────────────────────────────────────────

    /** @return the semantic type of this slot */
    @NotNull
    public SlotType getSlotType() { return slotType; }

    /** @return unique stable identifier within the player layout */
    @NotNull
    public String getSlotId() { return slotId; }

    /** @return display group name used for GUI grouping */
    @NotNull
    public String getDisplayGroup() { return displayGroup; }

    /** @return origin of this slot */
    @NotNull
    public SlotSource getSource() { return source; }

    /** @return acceptance rule for items placed in this slot */
    @NotNull
    public SlotAcceptRule getAcceptRule() { return acceptRule; }

    /** @return ordering index within the display group */
    public int getOrder() { return order; }

    /** @return {@code true} if the slot should be rendered in the GUI */
    public boolean isVisible() { return visible; }

    /**
     * @return {@code true} if mutations are allowed;
     *         {@code false} means the slot is effectively read-only
     */
    public boolean isEnabled() { return enabled; }

    /** @return optional GUI anchor hint, or {@code null} */
    @Nullable
    public String getUiAnchor() { return uiAnchor; }

    /** @return layout engine priority for this slot */
    public int getUiPriority() { return uiPriority; }

    // ── equals / hashCode / toString ──────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SlotDefinition)) return false;
        SlotDefinition that = (SlotDefinition) o;
        return Objects.equals(slotId, that.slotId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotId);
    }

    @Override
    public String toString() {
        return "SlotDefinition{id='" + slotId + "', type=" + slotType
                + ", source=" + source + ", enabled=" + enabled + '}';
    }

    // ── Builder ───────────────────────────────────────────────────────────

    /**
     * Creates a new builder for a {@code SlotDefinition}.
     *
     * @param slotId  unique slot identifier
     * @param type    the semantic type of this slot
     * @param source  who contributed this slot
     * @return a new {@link Builder}
     */
    public static Builder builder(@NotNull String slotId, @NotNull SlotType type, @NotNull SlotSource source) {
        return new Builder(slotId, type, source);
    }

    /** Fluent builder for {@link SlotDefinition}. */
    public static final class Builder {
        private final SlotType slotType;
        private final String slotId;
        private final SlotSource source;
        private String displayGroup = "default";
        private SlotAcceptRule acceptRule = SlotAcceptRule.ACCEPT_ALL;
        private int order = 0;
        private boolean visible = true;
        private boolean enabled = true;
        @Nullable
        private String uiAnchor = null;
        private int uiPriority = 0;

        private Builder(String slotId, SlotType type, SlotSource source) {
            this.slotId = slotId;
            this.slotType = type;
            this.source = source;
        }

        /** Sets the GUI display group label. */
        public Builder displayGroup(@NotNull String group) {
            this.displayGroup = group;
            return this;
        }

        /** Sets the acceptance rule. Defaults to {@link SlotAcceptRule#ACCEPT_ALL}. */
        public Builder acceptRule(@NotNull SlotAcceptRule rule) {
            this.acceptRule = rule;
            return this;
        }

        /** Sets the ordering index within the display group. */
        public Builder order(int order) {
            this.order = order;
            return this;
        }

        /** Hides the slot from the GUI (items still stored, not rendered). */
        public Builder hidden() {
            this.visible = false;
            return this;
        }

        /** Makes the slot read-only — items visible but cannot be moved by the player. */
        public Builder disabled() {
            this.enabled = false;
            return this;
        }

        /** Sets the GUI anchor hint. */
        public Builder uiAnchor(@Nullable String anchor) {
            this.uiAnchor = anchor;
            return this;
        }

        /** Sets the layout engine priority. */
        public Builder uiPriority(int priority) {
            this.uiPriority = priority;
            return this;
        }

        /** Builds and returns an immutable {@link SlotDefinition}. */
        public SlotDefinition build() {
            return new SlotDefinition(this);
        }
    }
}

