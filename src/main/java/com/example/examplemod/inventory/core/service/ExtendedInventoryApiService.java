package com.example.examplemod.inventory.core.service;

import com.example.examplemod.Config;
import com.example.examplemod.inventory.api.IExtendedInventoryApi;
import com.example.examplemod.inventory.api.InventoryAccessRequest;
import com.example.examplemod.inventory.api.InventoryMutationResult;
import com.example.examplemod.inventory.api.InventorySlotView;
import com.example.examplemod.inventory.core.audit.InventoryAuditEvent;
import com.example.examplemod.inventory.core.audit.InventoryAuditService;
import com.example.examplemod.inventory.core.capability.IExtendedInventoryCapability;
import com.example.examplemod.inventory.core.layout.EquipmentLayoutService;
import com.example.examplemod.inventory.core.network.guard.InventoryActionFloodGuard;
import com.example.examplemod.inventory.core.service.policy.InventoryAccessPolicyService;
import com.example.examplemod.inventory.core.service.policy.InventoryActionKind;
import com.example.examplemod.inventory.core.service.policy.PolicyDecision;
import com.example.examplemod.inventory.core.slot.PlayerLayoutProfile;
import com.example.examplemod.inventory.core.slot.SlotDefinition;
import com.example.examplemod.inventory.core.storage.PlayerInventoryPersistenceService;
import com.example.examplemod.inventory.core.sync.InventorySyncService;
import com.example.examplemod.inventory.core.sync.SyncTrigger;
import com.example.examplemod.inventory.core.transaction.InventoryTransactionService;
import com.example.examplemod.inventory.core.transaction.TransactionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * M6 public API implementation for server-side integrations.
 */
public final class ExtendedInventoryApiService implements IExtendedInventoryApi {

    private static final ExtendedInventoryApiService INSTANCE = new ExtendedInventoryApiService();

    private final InventoryTransactionService transactionService;
    private final InventoryAccessPolicyService policyService;
    private final InventoryAuditService auditService;

    private ExtendedInventoryApiService() {
        this(InventoryTransactionService.getInstance(),
                InventoryAccessPolicyService.getInstance(),
                InventoryAuditService.getInstance());
    }

    ExtendedInventoryApiService(@NotNull InventoryTransactionService transactionService,
                                @NotNull InventoryAccessPolicyService policyService,
                                @NotNull InventoryAuditService auditService) {
        this.transactionService = transactionService;
        this.policyService = policyService;
        this.auditService = auditService;
    }

    @NotNull
    public static IExtendedInventoryApi getInstance() {
        return INSTANCE;
    }

    @Override
    @NotNull
    public List<InventorySlotView> snapshot(@NotNull ServerPlayer actor,
                                            @NotNull ServerPlayer target,
                                            @NotNull InventoryAccessRequest request) {
        IExtendedInventoryCapability cap = PlayerInventoryPersistenceService.requireCapability(target);
        PlayerLayoutProfile profile = EquipmentLayoutService.getInstance().getOrBuild(target);

        PolicyDecision decision = policyService.evaluate(actor, target, request, InventoryActionKind.SNAPSHOT);
        if (!decision.allowed()) {
            return List.of();
        }

        if (!matchesExpectedState(request, cap.getRevision(), profile.getLayoutToken())) {
            InventorySyncService.getInstance().requestFullSync(target, SyncTrigger.REVISION_CONFLICT);
            return List.of();
        }

        List<InventorySlotView> result = new ArrayList<>(profile.getTotalSlotCount());
        for (SlotDefinition slot : profile.getAllSlots()) {
            int slotIndex = profile.getSlotIndex(slot.getSlotId());
            ItemStack stack = slotIndex < 0 ? ItemStack.EMPTY : cap.getInventory().getStackInSlot(slotIndex).copy();
            result.add(new InventorySlotView(
                    slot.getSlotId(),
                    slot.getSlotType(),
                    slot.getSource(),
                    slot.isEnabled(),
                    stack));
        }
        return result;
    }

    @Override
    @NotNull
    public Optional<InventorySlotView> findFirst(@NotNull ServerPlayer actor,
                                                  @NotNull ServerPlayer target,
                                                  @NotNull InventoryAccessRequest request,
                                                  @NotNull Predicate<InventorySlotView> filter) {
        return snapshot(actor, target, request).stream().filter(filter).findFirst();
    }

    @Override
    @NotNull
    public InventoryMutationResult move(@NotNull ServerPlayer actor,
                                        @NotNull ServerPlayer target,
                                        @NotNull InventoryAccessRequest request,
                                        @NotNull String fromSlotId,
                                        @NotNull String toSlotId) {
        return executeMutation(actor, target, request, InventoryActionKind.MOVE,
                (cap, profile) -> transactionService.moveItem(cap, profile, fromSlotId, toSlotId));
    }

    @Override
    @NotNull
    public InventoryMutationResult extract(@NotNull ServerPlayer actor,
                                           @NotNull ServerPlayer target,
                                           @NotNull InventoryAccessRequest request,
                                           @NotNull String slotId) {
        return executeMutation(actor, target, request, InventoryActionKind.EXTRACT,
                (cap, profile) -> transactionService.extractFromSlot(cap, profile, slotId));
    }

    @Override
    @NotNull
    public InventoryMutationResult insert(@NotNull ServerPlayer actor,
                                          @NotNull ServerPlayer target,
                                          @NotNull InventoryAccessRequest request,
                                          @NotNull ItemStack stack) {
        return executeMutation(actor, target, request, InventoryActionKind.INSERT,
                (cap, profile) -> transactionService.insertItem(cap, profile, stack));
    }

    @NotNull
    private InventoryMutationResult executeMutation(@NotNull ServerPlayer actor,
                                                    @NotNull ServerPlayer target,
                                                    @NotNull InventoryAccessRequest request,
                                                    @NotNull InventoryActionKind actionKind,
                                                    @NotNull MutationOperation operation) {
        IExtendedInventoryCapability cap = PlayerInventoryPersistenceService.requireCapability(target);
        PlayerLayoutProfile profile = EquipmentLayoutService.getInstance().getOrBuild(target);

        PolicyDecision policy = policyService.evaluate(actor, target, request, actionKind);
        if (!policy.allowed()) {
            InventoryMutationResult forbidden = InventoryMutationResult.forbidden(
                    policy.reason(),
                    cap.getRevision(),
                    profile.getLayoutToken());
            audit(actor, target, request, actionKind, forbidden);
            return forbidden;
        }

        if (!matchesExpectedState(request, cap.getRevision(), profile.getLayoutToken())) {
            InventorySyncService.getInstance().requestFullSync(target, SyncTrigger.REVISION_CONFLICT);
            InventoryMutationResult conflict = InventoryMutationResult.conflict(
                    "revision/layout mismatch",
                    cap.getRevision(),
                    profile.getLayoutToken());
            audit(actor, target, request, actionKind, conflict);
            return conflict;
        }

        boolean accepted = InventoryActionFloodGuard.getInstance().tryAcquire(
                target.getUUID(),
                "api",
                target.serverLevel().getGameTime(),
                Config.getApiMutationsPerTickLimit());
        if (!accepted) {
            InventoryMutationResult limited = InventoryMutationResult.rateLimited(
                    "api mutation rate exceeded for current tick",
                    cap.getRevision(),
                    profile.getLayoutToken());
            audit(actor, target, request, actionKind, limited);
            return limited;
        }

        TransactionResult txResult = operation.execute(cap, profile);
        if (txResult.isSuccess()) {
            InventorySyncService.getInstance().queuePartialSync(target, txResult.getDirtySlotIds());
        }

        InventoryMutationResult finalResult = InventoryMutationResult.fromTransaction(
                txResult,
                cap.getRevision(),
                profile.getLayoutToken());
        audit(actor, target, request, actionKind, finalResult);
        return finalResult;
    }

    private void audit(@NotNull ServerPlayer actor,
                       @NotNull ServerPlayer target,
                       @NotNull InventoryAccessRequest request,
                       @NotNull InventoryActionKind actionKind,
                       @NotNull InventoryMutationResult result) {
        auditService.record(new InventoryAuditEvent(
                System.currentTimeMillis(),
                actor.getUUID(),
                actor.getGameProfile().getName(),
                target.getUUID(),
                target.getGameProfile().getName(),
                request.modId(),
                actionKind.name(),
                result.getStatus().name(),
                result.getDetail(),
                result.getRevision(),
                result.getLayoutVersion()));
    }

    static boolean matchesExpectedState(@NotNull InventoryAccessRequest request,
                                        long currentRevision,
                                        @NotNull String currentLayoutVersion) {
        boolean revisionOk = request.acceptsAnyRevision() || request.expectedRevision() == currentRevision;
        boolean layoutOk = request.acceptsAnyLayout() || request.expectedLayoutVersion().equals(currentLayoutVersion);
        return revisionOk && layoutOk;
    }

    @FunctionalInterface
    private interface MutationOperation {
        @NotNull
        TransactionResult execute(@NotNull IExtendedInventoryCapability cap,
                                  @NotNull PlayerLayoutProfile profile);
    }
}
