package com.example.examplemod.inventory.core.service.policy;

import com.example.examplemod.Config;
import com.example.examplemod.inventory.api.InventoryAccessRequest;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * M6 policy layer above AccessMode: modId allow-list + permission checks.
 */
public final class InventoryAccessPolicyService {

    private static final InventoryAccessPolicyService INSTANCE = new InventoryAccessPolicyService();

    private InventoryAccessPolicyService() {
    }

    @NotNull
    public static InventoryAccessPolicyService getInstance() {
        return INSTANCE;
    }

    @NotNull
    public PolicyDecision evaluate(@NotNull ServerPlayer actor,
                                   @NotNull ServerPlayer target,
                                   @NotNull InventoryAccessRequest request,
                                   @NotNull InventoryActionKind actionKind) {
        return evaluateInputs(
                request.modId(),
                request.accessMode().canMutate(),
                actionKind.isMutation(),
                actor.hasPermissions(Config.getApiAdminPermissionLevel()),
                actor.getUUID().equals(target.getUUID()));
    }

    @NotNull
    PolicyDecision evaluateInputs(@NotNull String modId,
                                  boolean accessCanMutate,
                                  boolean mutationAction,
                                  boolean hasAdminPermission,
                                  boolean samePlayer) {
        if (!isModIdAllowed(modId)) {
            return PolicyDecision.deny("modId is not allowed: " + modId);
        }

        if (mutationAction && !accessCanMutate) {
            return PolicyDecision.deny("access mode is read-only for mutation action");
        }

        if (samePlayer) {
            return PolicyDecision.allow();
        }

        if (hasAdminPermission) {
            return PolicyDecision.allow();
        }

        return PolicyDecision.deny("admin permission required for cross-player inventory access");
    }

    boolean isModIdAllowed(@NotNull String modId) {
        List<String> allowed = Config.getApiAllowedModIds();
        if (allowed.isEmpty()) {
            return true;
        }

        String normalized = modId.trim().toLowerCase(Locale.ROOT);
        for (String candidate : allowed) {
            if (normalized.equals(candidate.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
