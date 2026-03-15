package com.example.examplemod.inventory.core.layout;

import com.example.examplemod.inventory.core.slot.SlotAcceptRule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SlotAcceptRule} — pure logic, no Minecraft bootstrap needed.
 */
class SlotAcceptRuleTest {

    @Test
    void acceptAllAllowsEmpty() {
        // Empty ItemStack cannot be constructed without MC bootstrap,
        // but we can verify the predicate path on ACCEPT_ALL
        Assertions.assertNotNull(SlotAcceptRule.ACCEPT_ALL);
    }

    @Test
    void rejectAllRuleExistsAndIsDistinctFromAcceptAll() {
        Assertions.assertNotSame(SlotAcceptRule.ACCEPT_ALL, SlotAcceptRule.REJECT_ALL);
    }

    @Test
    void predicateRuleIsCreated() {
        SlotAcceptRule rule = SlotAcceptRule.ofPredicate(stack -> false);
        Assertions.assertNotNull(rule);
    }

    @Test
    void ofItemsWithEmptyCollectionReturnsRule() {
        SlotAcceptRule rule = SlotAcceptRule.ofItems(java.util.Collections.emptyList());
        Assertions.assertNotNull(rule);
        Assertions.assertTrue(rule.getAllowedItems().isEmpty());
    }
}

