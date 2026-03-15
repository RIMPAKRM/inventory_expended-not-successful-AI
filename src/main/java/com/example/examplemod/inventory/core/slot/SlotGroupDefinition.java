package com.example.examplemod.inventory.core.slot;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable description of a group of related inventory slots.
 *
 * <p>A {@code SlotGroupDefinition} is a logical container that bundles
 * {@link SlotDefinition} instances together for display and handling purposes.
 * Examples: "Chest Rig Pouches", "Jacket Pockets", "Backpack Main Compartment".</p>
 *
 * <p>Groups drive how the GUI organises slot panels. They carry their own label
 * (for GUI headers), anchor, and priority, which are independent from per-slot values
 * and can serve as defaults for any slot that does not specify its own anchor.</p>
 *
 * <p>All slots within a group must have unique {@code slotId} values.
 * Groups are identified by a stable {@link #groupId} string that persists across sessions.</p>
 */
public final class SlotGroupDefinition {

    /**
     * Stable unique identifier for this group.
     * Format: {@code "<source_namespace>:<meaningful_name>"}, e.g.
     * {@code "examplemod:chest_rig_mags"}.
     */
    private final String groupId;

    /**
     * Localisation key (or direct display label) shown as a group header in the GUI.
     */
    private final String displayLabel;

    /** Source of the slots in this group. All slots in a group share the same source. */
    private final SlotSource source;

    /** Ordered, immutable list of slot definitions that belong to this group. */
    private final List<SlotDefinition> slots;

    /**
     * GUI anchor for this group as a whole.
     * The layout engine uses this to decide which panel region to render the group in.
     */
    private final String uiAnchor;

    /**
     * Layout priority of this group. Groups with higher priority are laid out first
     * in a given panel region, pushing lower-priority groups further down or to a
     * secondary panel.
     */
    private final int uiPriority;

    /** Whether this group is currently visible in the GUI. */
    private final boolean visible;

    private SlotGroupDefinition(Builder builder) {
        this.groupId = Objects.requireNonNull(builder.groupId, "groupId");
        this.displayLabel = Objects.requireNonNull(builder.displayLabel, "displayLabel");
        this.source = Objects.requireNonNull(builder.source, "source");
        this.slots = Collections.unmodifiableList(List.copyOf(builder.slots));
        this.uiAnchor = builder.uiAnchor;
        this.uiPriority = builder.uiPriority;
        this.visible = builder.visible;
    }

    // ── Accessors ──────────────────────────────────────────────────────────

    /** @return stable unique group identifier */
    @NotNull
    public String getGroupId() { return groupId; }

    /** @return display label / localisation key for the GUI header */
    @NotNull
    public String getDisplayLabel() { return displayLabel; }

    /** @return common source for all slots in this group */
    @NotNull
    public SlotSource getSource() { return source; }

    /** @return ordered, immutable list of slot definitions */
    @NotNull
    public List<SlotDefinition> getSlots() { return slots; }

    /** @return number of slot definitions in this group */
    public int getSlotCount() { return slots.size(); }

    /** @return GUI anchor hint for this group */
    @NotNull
    public String getUiAnchor() { return uiAnchor; }

    /** @return layout priority; higher values are placed first */
    public int getUiPriority() { return uiPriority; }

    /** @return {@code true} if the group should be rendered in the GUI */
    public boolean isVisible() { return visible; }

    // ── equals / hashCode / toString ──────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SlotGroupDefinition)) return false;
        SlotGroupDefinition that = (SlotGroupDefinition) o;
        return Objects.equals(groupId, that.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId);
    }

    @Override
    public String toString() {
        return "SlotGroupDefinition{id='" + groupId + "', slots=" + slots.size()
                + ", source=" + source + '}';
    }

    // ── Builder ───────────────────────────────────────────────────────────

    /**
     * Creates a new builder for a {@code SlotGroupDefinition}.
     *
     * @param groupId      stable unique group identifier
     * @param displayLabel GUI header label or localisation key
     * @param source       origin of all slots in this group
     * @return a new {@link Builder}
     */
    public static Builder builder(@NotNull String groupId,
                                  @NotNull String displayLabel,
                                  @NotNull SlotSource source) {
        return new Builder(groupId, displayLabel, source);
    }

    /** Fluent builder for {@link SlotGroupDefinition}. */
    public static final class Builder {
        private final String groupId;
        private final String displayLabel;
        private final SlotSource source;
        private List<SlotDefinition> slots = List.of();
        private String uiAnchor = "default";
        private int uiPriority = 0;
        private boolean visible = true;

        private Builder(String groupId, String displayLabel, SlotSource source) {
            this.groupId = groupId;
            this.displayLabel = displayLabel;
            this.source = source;
        }

        /**
         * Sets the ordered list of slots for this group.
         * The list is copied defensively on {@link #build()}.
         */
        public Builder slots(@NotNull List<SlotDefinition> slotList) {
            this.slots = slotList;
            return this;
        }

        /** Sets the GUI anchor region for this group. */
        public Builder uiAnchor(@NotNull String anchor) {
            this.uiAnchor = anchor;
            return this;
        }

        /** Sets the layout priority for this group. */
        public Builder uiPriority(int priority) {
            this.uiPriority = priority;
            return this;
        }

        /** Hides the entire group from the GUI. */
        public Builder hidden() {
            this.visible = false;
            return this;
        }

        /** Builds and returns an immutable {@link SlotGroupDefinition}. */
        public SlotGroupDefinition build() {
            return new SlotGroupDefinition(this);
        }
    }
}

