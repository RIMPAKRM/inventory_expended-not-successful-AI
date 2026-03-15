package com.example.examplemod.inventory.core.craft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CraftRecipeServiceTest {

    @Test
    void knownRecipeId_isRecognized() {
        assertTrue(CraftRecipeService.isKnownRecipeId("test_leather_rig"));
        assertFalse(CraftRecipeService.isKnownRecipeId("unknown_recipe"));
    }
}
