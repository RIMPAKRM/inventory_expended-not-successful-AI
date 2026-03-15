package com.example.examplemod.inventory.core.audit;

import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * In-memory audit sink with structured logs for M6 admin operations.
 */
public final class InventoryAuditService {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_EVENTS = 1000;
    private static final InventoryAuditService INSTANCE = new InventoryAuditService();

    private final Deque<InventoryAuditEvent> recentEvents = new ArrayDeque<>();

    private InventoryAuditService() {
    }

    @NotNull
    public static InventoryAuditService getInstance() {
        return INSTANCE;
    }

    public synchronized void record(@NotNull InventoryAuditEvent event) {
        while (recentEvents.size() >= MAX_EVENTS) {
            recentEvents.removeFirst();
        }
        recentEvents.addLast(event);

        LOGGER.info("[inventory-audit] actor={} target={} mod={} action={} status={} detail={}",
                event.actorName(), event.targetName(), event.modId(), event.action(), event.status(), event.detail());
    }

    @NotNull
    public synchronized List<InventoryAuditEvent> recent() {
        return new ArrayList<>(recentEvents);
    }

    public synchronized void clear() {
        recentEvents.clear();
    }
}

