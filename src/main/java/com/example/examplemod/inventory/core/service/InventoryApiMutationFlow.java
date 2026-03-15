package com.example.examplemod.inventory.core.service;

import com.example.examplemod.inventory.api.InventoryMutationResult;
import com.example.examplemod.inventory.core.service.policy.PolicyDecision;
import com.example.examplemod.inventory.core.sync.InventorySyncOrchestrator;
import com.example.examplemod.inventory.core.sync.SyncTrigger;
import com.example.examplemod.inventory.core.transaction.TransactionResult;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Pure mutation flow used to verify API->transaction->sync orchestration.
 */
public final class InventoryApiMutationFlow {

    @NotNull
    public InventoryMutationResult apply(@NotNull UUID targetPlayerId,
                                         @NotNull PolicyDecision policyDecision,
                                         boolean stateMatches,
                                         boolean rateAccepted,
                                         @NotNull TransactionResult txResult,
                                         long currentRevision,
                                         @NotNull String currentLayoutVersion,
                                         @NotNull InventorySyncOrchestrator orchestrator) {
        if (!policyDecision.allowed()) {
            return InventoryMutationResult.forbidden(policyDecision.reason(), currentRevision, currentLayoutVersion);
        }

        if (!stateMatches) {
            orchestrator.requestFull(targetPlayerId, SyncTrigger.REVISION_CONFLICT);
            return InventoryMutationResult.conflict("revision/layout mismatch", currentRevision, currentLayoutVersion);
        }

        if (!rateAccepted) {
            return InventoryMutationResult.rateLimited("api mutation rate exceeded for current tick",
                    currentRevision, currentLayoutVersion);
        }

        if (txResult.isSuccess()) {
            orchestrator.queuePartial(targetPlayerId, txResult.getDirtySlotIds());
        }

        return InventoryMutationResult.fromTransaction(txResult, currentRevision, currentLayoutVersion);
    }
}

