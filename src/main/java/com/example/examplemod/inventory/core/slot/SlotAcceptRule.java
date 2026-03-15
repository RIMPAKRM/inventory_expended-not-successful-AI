package com.example.examplemod.inventory.core.slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
/**
 * Defines the acceptance rules for a single inventory slot.
 *
 * <p>When an item is being placed into a slot, the slot's SlotAcceptRule is
 * consulted to determine whether the placement is allowed.</p>
 *
 * <p>Rules are evaluated on the server only.</p>
 */
public final class SlotAcceptRule {
    /** A rule that accepts every ItemStack, including empty ones. */
    public static final SlotAcceptRule ACCEPT_ALL = new SlotAcceptRule(null, Collections.emptySet(), false);
    /** A rule that rejects every ItemStack (e.g. for disabled/overflow-readonly slots). */
    public static final SlotAcceptRule REJECT_ALL = new SlotAcceptRule(null, Collections.emptySet(), true);
    /**
     * Functional interface for an arbitrary predicate on an ItemStack.
     */
    @FunctionalInterface
    public interface StackPredicate {
        boolean test(ItemStack stack);
    }
    private final StackPredicate extraPredicate;
    private final Set<Item> allowedItems;
    private final boolean rejectAll;
    private SlotAcceptRule(StackPredicate extraPredicate, Collection<Item> allowedItems, boolean rejectAll) {
        this.extraPredicate = extraPredicate;
        this.allowedItems = allowedItems.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(allowedItems));
        this.rejectAll = rejectAll;
    }
    /**
     * Creates an accept rule with an optional predicate and item whitelist.
     *
     * @param extraPredicate optional predicate; null means no extra restriction
     * @param allowedItems   item whitelist; empty means all items allowed
     */
    public SlotAcceptRule(StackPredicate extraPredicate, Collection<Item> allowedItems) {
        this(extraPredicate, allowedItems, false);
    }
    /**
     * Evaluates whether the given ItemStack may be placed into the slot.
     *
     * @param stack the stack to test
     * @return true if the stack is acceptable
     */
    public boolean test(ItemStack stack) {
        if (rejectAll) {
            return false;
        }
        if (stack.isEmpty()) {
            return true;
        }
        if (!allowedItems.isEmpty() && !allowedItems.contains(stack.getItem())) {
            return false;
        }
        return extraPredicate == null || extraPredicate.test(stack);
    }
    /**
     * Returns the set of items explicitly allowed by this rule.
     * An empty set means no item-type restriction.
     */
    public Set<Item> getAllowedItems() {
        return allowedItems;
    }
    /**
     * Creates a new rule that allows only items in the given collection.
     */
    public static SlotAcceptRule ofItems(Collection<Item> items) {
        return new SlotAcceptRule(null, items, false);
    }
    /**
     * Creates a new rule that applies only the given predicate.
     */
    public static SlotAcceptRule ofPredicate(StackPredicate predicate) {
        return new SlotAcceptRule(predicate, Collections.emptySet(), false);
    }
}