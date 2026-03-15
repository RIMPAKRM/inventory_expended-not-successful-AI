package com.example.examplemod.inventory.core.service.policy;

import com.example.examplemod.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InventoryAccessPolicyServiceTest {

    private final InventoryAccessPolicyService service = InventoryAccessPolicyService.getInstance();

    @AfterEach
    void resetConfigState() {
        Config.apiAllowedModIds = List.of();
    }

    @Test
    void crossPlayer_requiresAdminPermission() {
        PolicyDecision denied = service.evaluateInputs("testmod", true, false, false, false);
        PolicyDecision allowed = service.evaluateInputs("testmod", true, false, true, false);

        assertFalse(denied.allowed());
        assertTrue(allowed.allowed());
    }

    @Test
    void mutation_requiresReadWriteAccessMode() {
        PolicyDecision denied = service.evaluateInputs("testmod", false, true, true, true);
        PolicyDecision allowed = service.evaluateInputs("testmod", true, true, true, true);

        assertFalse(denied.allowed());
        assertTrue(allowed.allowed());
    }

    @Test
    void modId_allowListIsApplied() {
        Config.apiAllowedModIds = List.of("allowed_mod");

        assertTrue(service.evaluateInputs("allowed_mod", true, false, false, true).allowed());
        assertFalse(service.evaluateInputs("blocked_mod", true, false, true, true).allowed());
    }
}

