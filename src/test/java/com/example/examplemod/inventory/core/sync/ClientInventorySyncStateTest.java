package com.example.examplemod.inventory.core.sync;

import com.example.examplemod.inventory.core.network.client.ClientInventorySyncState;
import com.example.examplemod.inventory.core.network.packet.S2CFullInventorySyncPacket;
import com.example.examplemod.inventory.core.network.packet.S2CPartialInventorySyncPacket;
import com.example.examplemod.inventory.core.model.InventoryNbtCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientInventorySyncStateTest {

    @AfterEach
    void cleanup() {
        ClientInventorySyncState.resetForTests();
    }

    @Test
    @Disabled("Requires Minecraft bootstrap for ItemStack/packet runtime; migrate to Forge GameTest")
    void applyFull_setsSchemaRevisionLayoutAndSlots() {
        ItemStackHandler handler = new ItemStackHandler(2);
        CompoundTag inventoryTag = handler.serializeNBT();

        S2CFullInventorySyncPacket full = new S2CFullInventorySyncPacket(
                InventoryNbtCodec.CURRENT_SCHEMA_VERSION,
                10L,
                "layout-v1",
                inventoryTag,
                List.of("slot_a", "slot_b"),
                "LOGIN");

        ClientInventorySyncState.applyFull(full);

        assertEquals(InventoryNbtCodec.CURRENT_SCHEMA_VERSION, ClientInventorySyncState.schemaVersion());
        assertEquals(10L, ClientInventorySyncState.revision());
        assertEquals("layout-v1", ClientInventorySyncState.layoutVersion());
        assertEquals(List.of("slot_a", "slot_b"), ClientInventorySyncState.slotOrder());
        assertEquals(2, ClientInventorySyncState.slots().size());
    }

    @Test
    @Disabled("Requires Minecraft bootstrap for ItemStack/packet runtime; migrate to Forge GameTest")
    void applyPartial_updatesOnMatchingLayoutAndNewerRevision() {
        S2CFullInventorySyncPacket full = new S2CFullInventorySyncPacket(
                InventoryNbtCodec.CURRENT_SCHEMA_VERSION,
                3L,
                "layout-v1",
                new ItemStackHandler(1).serializeNBT(),
                List.of("slot_a"),
                "LOGIN");
        ClientInventorySyncState.applyFull(full);

        Map<String, ItemStack> updates = new LinkedHashMap<>();
        updates.put("slot_a", ItemStack.EMPTY);

        S2CPartialInventorySyncPacket partial = new S2CPartialInventorySyncPacket(
                InventoryNbtCodec.CURRENT_SCHEMA_VERSION,
                4L,
                "layout-v1",
                updates);

        ClientInventorySyncState.applyPartial(partial);

        assertEquals(4L, ClientInventorySyncState.revision());
        assertTrue(ClientInventorySyncState.slots().containsKey("slot_a"));
    }

    @Test
    @Disabled("Requires Minecraft bootstrap for ItemStack/packet runtime; migrate to Forge GameTest")
    void applyPartial_ignoresStaleOrLayoutMismatchDelta() {
        S2CFullInventorySyncPacket full = new S2CFullInventorySyncPacket(
                InventoryNbtCodec.CURRENT_SCHEMA_VERSION,
                8L,
                "layout-v1",
                new ItemStackHandler(1).serializeNBT(),
                List.of("slot_a"),
                "LOGIN");
        ClientInventorySyncState.applyFull(full);

        Map<String, ItemStack> updates = new LinkedHashMap<>();
        updates.put("slot_a", ItemStack.EMPTY);

        S2CPartialInventorySyncPacket stale = new S2CPartialInventorySyncPacket(
                InventoryNbtCodec.CURRENT_SCHEMA_VERSION,
                7L,
                "layout-v1",
                updates);
        ClientInventorySyncState.applyPartial(stale);
        assertEquals(8L, ClientInventorySyncState.revision());

        S2CPartialInventorySyncPacket mismatch = new S2CPartialInventorySyncPacket(
                InventoryNbtCodec.CURRENT_SCHEMA_VERSION,
                9L,
                "layout-v2",
                updates);
        ClientInventorySyncState.applyPartial(mismatch);
        assertEquals(8L, ClientInventorySyncState.revision());
    }
}
