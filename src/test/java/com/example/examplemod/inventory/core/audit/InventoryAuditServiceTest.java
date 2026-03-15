package com.example.examplemod.inventory.core.audit;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InventoryAuditServiceTest {

    @Test
    void record_storesEventForAdminAudit() {
        InventoryAuditService service = InventoryAuditService.getInstance();
        service.clear();

        InventoryAuditEvent event = new InventoryAuditEvent(
                System.currentTimeMillis(),
                UUID.randomUUID(),
                "admin",
                UUID.randomUUID(),
                "target",
                "external_mod",
                "MOVE",
                "COMMITTED",
                "ok",
                42L,
                "layout-v1");

        service.record(event);

        List<InventoryAuditEvent> recent = service.recent();
        assertEquals(1, recent.size());
        assertEquals("admin", recent.get(0).actorName());
        assertEquals("MOVE", recent.get(0).action());
    }
}

