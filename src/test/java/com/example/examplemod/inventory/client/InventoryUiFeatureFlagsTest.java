package com.example.examplemod.inventory.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoryUiFeatureFlagsTest {

    @Test
    void uiReplacementEnabled_requiresFeatureOnAndKillSwitchOff() {
        assertTrue(InventoryUiFeatureFlags.isUiReplacementEnabled(true, false));
        assertFalse(InventoryUiFeatureFlags.isUiReplacementEnabled(false, false));
        assertFalse(InventoryUiFeatureFlags.isUiReplacementEnabled(true, true));
        assertFalse(InventoryUiFeatureFlags.isUiReplacementEnabled(false, true));
    }
}

