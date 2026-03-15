package com.example.examplemod.inventory.core.transaction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

/**
 * Basic contract tests for {@link OverflowPolicy}.
 */
class OverflowPolicyTest {

    @Test
    void containsAllExpectedPolicies() {
        EnumSet<OverflowPolicy> values = EnumSet.allOf(OverflowPolicy.class);
        Assertions.assertEquals(4, values.size());
        Assertions.assertTrue(values.contains(OverflowPolicy.DENY_REMOVE_IF_OVERFLOW));
        Assertions.assertTrue(values.contains(OverflowPolicy.MOVE_TO_AVAILABLE_SLOTS_FIRST));
        Assertions.assertTrue(values.contains(OverflowPolicy.ALLOW_OVERFLOW_READONLY));
        Assertions.assertTrue(values.contains(OverflowPolicy.DROP_TO_WORLD_LAST_RESORT));
    }

    @Test
    void enumValueOfRoundTrip() {
        for (OverflowPolicy policy : OverflowPolicy.values()) {
            Assertions.assertEquals(policy, OverflowPolicy.valueOf(policy.name()));
        }
    }
}
