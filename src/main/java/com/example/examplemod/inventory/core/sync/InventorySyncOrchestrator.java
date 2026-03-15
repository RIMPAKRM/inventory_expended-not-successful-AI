package com.example.examplemod.inventory.core.sync;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects sync requests per player and drains them once per tick.
 *
 * <p>This is the anti-flood core for M4: many local mutations in one tick become
 * a single packet decision (full or partial).</p>
 */
public final class InventorySyncOrchestrator {

    private final int partialToFullThreshold;
    private final Map<UUID, PendingSync> pendingByPlayer = new ConcurrentHashMap<>();

    public InventorySyncOrchestrator(int partialToFullThreshold) {
        this.partialToFullThreshold = Math.max(1, partialToFullThreshold);
    }

    public void requestFull(@NotNull UUID playerId, @NotNull SyncTrigger trigger) {
        pendingByPlayer.compute(playerId, (id, old) -> {
            PendingSync next = old == null ? new PendingSync() : old;
            next.fullRequested = true;
            next.fullTrigger = trigger;
            next.dirtySlotIds.clear();
            return next;
        });
    }

    public void queuePartial(@NotNull UUID playerId, @NotNull List<String> dirtySlotIds) {
        if (dirtySlotIds.isEmpty()) {
            return;
        }

        pendingByPlayer.compute(playerId, (id, old) -> {
            PendingSync next = old == null ? new PendingSync() : old;
            if (next.fullRequested) {
                return next;
            }

            for (String slotId : dirtySlotIds) {
                if (slotId == null || slotId.isBlank()) {
                    continue;
                }
                next.dirtySlotIds.add(slotId);
            }

            if (next.dirtySlotIds.size() > partialToFullThreshold) {
                next.fullRequested = true;
                next.fullTrigger = SyncTrigger.TOO_MANY_DIRTY_SLOTS;
                next.dirtySlotIds.clear();
            }
            return next;
        });
    }

    @NotNull
    public SyncDecision drain(@NotNull UUID playerId) {
        PendingSync pending = pendingByPlayer.remove(playerId);
        if (pending == null) {
            return SyncDecision.none();
        }

        if (pending.fullRequested) {
            return SyncDecision.full(pending.fullTrigger == null
                    ? SyncTrigger.TOO_MANY_DIRTY_SLOTS
                    : pending.fullTrigger);
        }

        if (pending.dirtySlotIds.isEmpty()) {
            return SyncDecision.none();
        }

        return SyncDecision.partial(new ArrayList<>(pending.dirtySlotIds));
    }

    public void clear(@NotNull UUID playerId) {
        pendingByPlayer.remove(playerId);
    }

    private static final class PendingSync {
        boolean fullRequested;
        SyncTrigger fullTrigger;
        final LinkedHashSet<String> dirtySlotIds = new LinkedHashSet<>();
    }

    public static final class SyncDecision {
        public enum Type {
            NONE,
            FULL,
            PARTIAL
        }

        private static final SyncDecision NONE = new SyncDecision(Type.NONE, null, List.of());

        private final Type type;
        private final SyncTrigger fullTrigger;
        private final List<String> dirtySlotIds;

        private SyncDecision(Type type, SyncTrigger fullTrigger, List<String> dirtySlotIds) {
            this.type = type;
            this.fullTrigger = fullTrigger;
            this.dirtySlotIds = List.copyOf(dirtySlotIds);
        }

        public static SyncDecision none() {
            return NONE;
        }

        public static SyncDecision full(@NotNull SyncTrigger trigger) {
            return new SyncDecision(Type.FULL, trigger, List.of());
        }

        public static SyncDecision partial(@NotNull List<String> dirtySlotIds) {
            return new SyncDecision(Type.PARTIAL, null, dirtySlotIds);
        }

        @NotNull
        public Type getType() {
            return type;
        }

        public boolean isFull() {
            return type == Type.FULL;
        }

        public boolean isPartial() {
            return type == Type.PARTIAL;
        }

        @NotNull
        public SyncTrigger getFullTrigger() {
            if (fullTrigger == null) {
                throw new IllegalStateException("No full trigger for decision type " + type);
            }
            return fullTrigger;
        }

        @NotNull
        public List<String> getDirtySlotIds() {
            return dirtySlotIds;
        }
    }
}

