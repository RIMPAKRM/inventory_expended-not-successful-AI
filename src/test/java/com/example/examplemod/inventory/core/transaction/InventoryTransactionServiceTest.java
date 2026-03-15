package com.example.examplemod.inventory.core.transaction;

import com.example.examplemod.inventory.core.capability.IExtendedInventoryCapability;
import com.example.examplemod.inventory.core.model.InventoryNbtCodec;
import com.example.examplemod.inventory.core.model.PlayerInventoryRecord;
import com.example.examplemod.inventory.core.slot.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link InventoryTransactionService}.
 *
 * <p>No Minecraft server bootstrap required — uses {@link StubCapability} and plain
 * {@link ItemStackHandler} only. All operations are pure in-memory logic.</p>
 */
@Disabled("Requires Minecraft bootstrap for ItemStack runtime; migrate to Forge GameTest")
class InventoryTransactionServiceTest {
    private static final net.minecraft.world.item.Item ITEM_A =
            new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties());
    private static final net.minecraft.world.item.Item ITEM_B =
            new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties());

    // ── Stub capability ───────────────────────────────────────────────────

    /**
     * Minimal in-memory capability stub for transaction tests.
     * Wraps {@link ItemStackHandler} without depending on Forge bootstrap.
     */
    static class StubCapability implements IExtendedInventoryCapability {
        private final ItemStackHandler handler;
        private long revision = 0L;
        private boolean dirty = false;
        private String layoutVersion = "";

        StubCapability(int slots) {
            this.handler = new ItemStackHandler(slots) {
                @Override
                protected void onContentsChanged(int slot) {
                    // Not used in tests — mutations come through setStackInSlot directly
                }
            };
        }

        @Override public ItemStackHandler getInventory() { return handler; }
        @Override public long getRevision() { return revision; }
        @Override public boolean isDirty() { return dirty; }
        @Override public void markDirty() { revision++; dirty = true; }
        @Override public void clearDirty() { dirty = false; }
        @Override public @NotNull String getLayoutVersion() { return layoutVersion; }
        @Override public void setLayoutVersion(@NotNull String lv) { this.layoutVersion = lv; }

        @Override
        public PlayerInventoryRecord toRecord() {
            return new PlayerInventoryRecord(
                    InventoryNbtCodec.CURRENT_SCHEMA_VERSION, revision, dirty,
                    handler.serializeNBT(), layoutVersion);
        }

        @Override
        public void loadFromRecord(@NotNull PlayerInventoryRecord record) {
            this.revision = record.revision();
            this.dirty = record.dirty();
            this.layoutVersion = record.layoutVersion();
            handler.deserializeNBT(record.inventoryTag());
        }

        @Override
        public void restoreFromSnapshot(@NotNull List<ItemStack> items, long snapRevision,
                                         boolean snapDirty, @NotNull String snapLayout) {
            int restoreCount = Math.min(handler.getSlots(), items.size());
            for (int i = 0; i < restoreCount; i++) {
                handler.setStackInSlot(i, items.get(i).copy());
            }
            for (int i = restoreCount; i < handler.getSlots(); i++) {
                handler.setStackInSlot(i, ItemStack.EMPTY);
            }
            this.revision = snapRevision;
            this.dirty = snapDirty;
            this.layoutVersion = snapLayout;
        }

        @Override public CompoundTag serializeCapabilityNbt() { return new CompoundTag(); }
        @Override public void deserializeCapabilityNbt(@NotNull CompoundTag nbt) {}
    }

    // ── Test helpers ──────────────────────────────────────────────────────

    private static PlayerLayoutProfile buildProfile(UUID id, int slots) {
        List<SlotDefinition> defs = new ArrayList<>();
        for (int i = 0; i < slots; i++) {
            defs.add(SlotDefinition
                    .builder("test:slot_" + i, SlotType.UTILITY, SlotSource.BASE)
                    .build());
        }
        SlotGroupDefinition group = SlotGroupDefinition
                .builder("test:base", "Base", SlotSource.BASE)
                .slots(defs).build();
        return new PlayerLayoutProfile(id, "token-v1", List.of(group));
    }

    private final InventoryTransactionService service = InventoryTransactionService.getInstance();
    private final UUID playerId = UUID.randomUUID();

    // ── moveItem tests ────────────────────────────────────────────────────

    @Test
    void moveItem_happyPath_commitsAndBumpsRevision() {
        StubCapability cap = new StubCapability(4);
        PlayerLayoutProfile profile = buildProfile(playerId, 4);

        // Put item in slot 0
        ItemStack sword = new ItemStack(ITEM_A, 1);
        cap.getInventory().setStackInSlot(0, sword.copy());

        long revBefore = cap.getRevision();
        TransactionResult result = service.moveItem(cap, profile, "test:slot_0", "test:slot_2");

        assertTrue(result.isSuccess(), "Expected commit but got: " + result.getDebugDetail());
        assertEquals(TransactionResult.Status.COMMITTED, result.getStatus());
        assertTrue(cap.getRevision() > revBefore, "Revision must increase after commit");
        assertTrue(cap.isDirty(), "Cap must be dirty after commit");

        // Verify item moved
        assertTrue(cap.getInventory().getStackInSlot(0).isEmpty(), "Source slot must be empty");
        assertEquals(ITEM_A, cap.getInventory().getStackInSlot(2).getItem());

        // Dirty slot ids reported
        assertTrue(result.getDirtySlotIds().contains("test:slot_0"));
        assertTrue(result.getDirtySlotIds().contains("test:slot_2"));
    }

    @Test
    void moveItem_sameSlot_isRejected() {
        StubCapability cap = new StubCapability(4);
        PlayerLayoutProfile profile = buildProfile(playerId, 4);
        cap.getInventory().setStackInSlot(0, new ItemStack(ITEM_B));

        TransactionResult result = service.moveItem(cap, profile, "test:slot_0", "test:slot_0");

        assertEquals(TransactionResult.Status.REJECTED, result.getStatus());
        assertFalse(result.getDirtySlotIds().contains("test:slot_0"));
    }

    @Test
    void moveItem_unknownSlot_isRejected() {
        StubCapability cap = new StubCapability(4);
        PlayerLayoutProfile profile = buildProfile(playerId, 4);
        cap.getInventory().setStackInSlot(0, new ItemStack(ITEM_B));

        TransactionResult result = service.moveItem(cap, profile, "test:slot_0", "test:nonexistent");

        assertEquals(TransactionResult.Status.REJECTED, result.getStatus());
    }

    @Test
    void moveItem_emptySourceSlot_isRejected() {
        StubCapability cap = new StubCapability(4);
        PlayerLayoutProfile profile = buildProfile(playerId, 4);
        // slot_0 is empty

        TransactionResult result = service.moveItem(cap, profile, "test:slot_0", "test:slot_1");

        assertEquals(TransactionResult.Status.REJECTED, result.getStatus());
    }

    @Test
    void moveItem_disabledDestination_isRolledBack() {
        StubCapability cap = new StubCapability(4);
        UUID pid = UUID.randomUUID();

        // Build profile with slot_1 disabled
        SlotDefinition s0 = SlotDefinition.builder("test:slot_0", SlotType.UTILITY, SlotSource.BASE).build();
        SlotDefinition s1 = SlotDefinition.builder("test:slot_1", SlotType.UTILITY, SlotSource.BASE)
                .disabled().build();
        SlotGroupDefinition g = SlotGroupDefinition.builder("test:g", "G", SlotSource.BASE)
                .slots(List.of(s0, s1)).build();
        PlayerLayoutProfile profile = new PlayerLayoutProfile(pid, "t", List.of(g));

        cap.getInventory().setStackInSlot(0, new ItemStack(ITEM_B));

        TransactionResult result = service.moveItem(cap, profile, "test:slot_0", "test:slot_1");

        assertEquals(TransactionResult.Status.ROLLED_BACK, result.getStatus());
        // Source item must still be in place (rollback)
        assertEquals(ITEM_B, cap.getInventory().getStackInSlot(0).getItem());
    }

    @Test
    void moveItem_acceptRuleRejects_isRolledBack() {
        StubCapability cap = new StubCapability(4);
        UUID pid = UUID.randomUUID();

        // Build profile with slot_1 that only accepts WOODEN_SWORD via predicate
        SlotAcceptRule swordsOnly = new SlotAcceptRule(
                stack -> stack.getItem() == ITEM_A,
                List.of());
        SlotDefinition s0 = SlotDefinition.builder("test:slot_0", SlotType.UTILITY, SlotSource.BASE).build();
        SlotDefinition s1 = SlotDefinition.builder("test:slot_1", SlotType.UTILITY, SlotSource.BASE)
                .acceptRule(swordsOnly).build();
        SlotGroupDefinition g = SlotGroupDefinition.builder("test:g", "G", SlotSource.BASE)
                .slots(List.of(s0, s1)).build();
        PlayerLayoutProfile profile = new PlayerLayoutProfile(pid, "t", List.of(g));

        // Put an apple in slot_0 (not a sword)
        cap.getInventory().setStackInSlot(0, new ItemStack(ITEM_B));

        TransactionResult result = service.moveItem(cap, profile, "test:slot_0", "test:slot_1");

        assertEquals(TransactionResult.Status.ROLLED_BACK, result.getStatus());
        // Rollback — apple stays in slot_0
        assertEquals(ITEM_B, cap.getInventory().getStackInSlot(0).getItem());
    }

    // ── Snapshot/rollback tests ───────────────────────────────────────────

    @Test
    void snapshot_capturesRevisionAndDirty() {
        StubCapability cap = new StubCapability(3);
        cap.getInventory().setStackInSlot(0, new ItemStack(ITEM_B, 5));
        cap.markDirty();

        InventorySnapshot snap = InventorySnapshot.take(cap);

        assertEquals(cap.getRevision(), snap.getRevision());
        assertTrue(snap.isDirty());
        assertEquals(5, snap.getItem(0).getCount());
    }

    @Test
    void snapshot_restore_undoesChanges() {
        StubCapability cap = new StubCapability(3);
        cap.getInventory().setStackInSlot(0, new ItemStack(ITEM_B, 5));
        long revBefore = cap.getRevision();
        boolean dirtyBefore = cap.isDirty();

        InventorySnapshot snap = InventorySnapshot.take(cap);

        // Mutate
        cap.getInventory().setStackInSlot(0, new ItemStack(ITEM_A, 3));
        cap.markDirty();

        // Restore
        snap.restore(cap);

        assertEquals(ITEM_B, cap.getInventory().getStackInSlot(0).getItem(),
                "Item must be restored after rollback");
        assertEquals(5, cap.getInventory().getStackInSlot(0).getCount());
        assertEquals(revBefore, cap.getRevision(), "Revision must be restored to pre-snapshot value");
        assertEquals(dirtyBefore, cap.isDirty(), "Dirty flag must be restored");
    }

    @Test
    void snapshot_restore_doesNotIncrementRevision() {
        StubCapability cap = new StubCapability(2);
        cap.getInventory().setStackInSlot(0, new ItemStack(ITEM_B, 10));
        long revAtSnapshot = cap.getRevision(); // 0

        InventorySnapshot snap = InventorySnapshot.take(cap);
        // Simulate failed transaction mutations
        cap.getInventory().setStackInSlot(0, ItemStack.EMPTY);
        cap.markDirty(); // revision = 1

        snap.restore(cap);

        assertEquals(revAtSnapshot, cap.getRevision(),
                "Rollback must restore revision to pre-transaction value");
    }

    // ── executeEquipmentRemoval tests ─────────────────────────────────────

    @Test
    void equipmentRemoval_emptyRemovedGroup_commitsImmediately() {
        // 4 base slots + 2 equipment slots
        StubCapability cap = new StubCapability(6);
        UUID pid = UUID.randomUUID();

        List<SlotDefinition> baseSlots = List.of(
                SlotDefinition.builder("test:base_0", SlotType.UTILITY, SlotSource.BASE).build(),
                SlotDefinition.builder("test:base_1", SlotType.UTILITY, SlotSource.BASE).build(),
                SlotDefinition.builder("test:base_2", SlotType.UTILITY, SlotSource.BASE).build(),
                SlotDefinition.builder("test:base_3", SlotType.UTILITY, SlotSource.BASE).build()
        );
        List<SlotDefinition> equipSlots = List.of(
                SlotDefinition.builder("test:equip_0", SlotType.MAG_POUCH, SlotSource.EQUIPMENT).build(),
                SlotDefinition.builder("test:equip_1", SlotType.MAG_POUCH, SlotSource.EQUIPMENT).build()
        );

        SlotGroupDefinition baseGroup = SlotGroupDefinition
                .builder("test:base", "Base", SlotSource.BASE).slots(baseSlots).build();
        SlotGroupDefinition equipGroup = SlotGroupDefinition
                .builder("test:equip", "Rig", SlotSource.EQUIPMENT).slots(equipSlots).build();

        PlayerLayoutProfile profile = new PlayerLayoutProfile(pid, "token", List.of(baseGroup, equipGroup));

        // Equipment slots are empty → removal is trivially safe
        TransactionResult result = service.executeEquipmentRemoval(
                cap, profile, equipGroup, List.of(baseGroup),
                OverflowPolicy.DENY_REMOVE_IF_OVERFLOW, null);

        assertTrue(result.isSuccess(), result.getDebugDetail());
        assertTrue(cap.isDirty());
    }

    @Test
    void equipmentRemoval_withItems_deniedWhenNoSpace() {
        // 2 base slots (both full) + 2 equipment slots (with items)
        StubCapability cap = new StubCapability(4);
        UUID pid = UUID.randomUUID();

        List<SlotDefinition> baseSlots = List.of(
                SlotDefinition.builder("test:base_0", SlotType.UTILITY, SlotSource.BASE).build(),
                SlotDefinition.builder("test:base_1", SlotType.UTILITY, SlotSource.BASE).build()
        );
        List<SlotDefinition> equipSlots = List.of(
                SlotDefinition.builder("test:equip_0", SlotType.MAG_POUCH, SlotSource.EQUIPMENT).build(),
                SlotDefinition.builder("test:equip_1", SlotType.MAG_POUCH, SlotSource.EQUIPMENT).build()
        );

        SlotGroupDefinition baseGroup = SlotGroupDefinition
                .builder("test:base", "Base", SlotSource.BASE).slots(baseSlots).build();
        SlotGroupDefinition equipGroup = SlotGroupDefinition
                .builder("test:equip", "Rig", SlotSource.EQUIPMENT).slots(equipSlots).build();

        PlayerLayoutProfile profile = new PlayerLayoutProfile(pid, "token", List.of(baseGroup, equipGroup));

        // Fill all 4 slots
        cap.getInventory().setStackInSlot(0, new ItemStack(ITEM_A, 64));   // base_0
        cap.getInventory().setStackInSlot(1, new ItemStack(ITEM_B, 64));   // base_1
        cap.getInventory().setStackInSlot(2, new ItemStack(ITEM_A, 64));   // equip_0
        cap.getInventory().setStackInSlot(3, new ItemStack(ITEM_B, 64));   // equip_1

        long revBefore = cap.getRevision();

        TransactionResult result = service.executeEquipmentRemoval(
                cap, profile, equipGroup, List.of(baseGroup),
                OverflowPolicy.DENY_REMOVE_IF_OVERFLOW, null);

        assertFalse(result.isSuccess(), "Must be denied when there is no space");
        assertEquals(TransactionResult.Status.ROLLED_BACK, result.getStatus());
        // Items must be unchanged (rollback)
        assertEquals(ITEM_A, cap.getInventory().getStackInSlot(2).getItem(),
                "Equipment slot items must be restored after rollback");
        assertEquals(revBefore, cap.getRevision(), "Revision must not change on rollback");
    }

    @Test
    void equipmentRemoval_withItems_committedWhenSpaceAvailable() {
        // 4 base slots (empty) + 2 equipment slots (with items) → relocation succeeds
        StubCapability cap = new StubCapability(6);
        UUID pid = UUID.randomUUID();

        List<SlotDefinition> baseSlots = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            baseSlots.add(SlotDefinition.builder("test:base_" + i, SlotType.UTILITY, SlotSource.BASE).build());
        }
        List<SlotDefinition> equipSlots = List.of(
                SlotDefinition.builder("test:equip_0", SlotType.UTILITY, SlotSource.EQUIPMENT).build(),
                SlotDefinition.builder("test:equip_1", SlotType.UTILITY, SlotSource.EQUIPMENT).build()
        );

        SlotGroupDefinition baseGroup = SlotGroupDefinition
                .builder("test:base", "Base", SlotSource.BASE).slots(baseSlots).build();
        SlotGroupDefinition equipGroup = SlotGroupDefinition
                .builder("test:equip", "Rig", SlotSource.EQUIPMENT).slots(equipSlots).build();

        PlayerLayoutProfile profile = new PlayerLayoutProfile(pid, "token", List.of(baseGroup, equipGroup));

        // Only equipment slots have items
        cap.getInventory().setStackInSlot(4, new ItemStack(ITEM_A, 10)); // equip_0
        cap.getInventory().setStackInSlot(5, new ItemStack(ITEM_B, 5)); // equip_1

        TransactionResult result = service.executeEquipmentRemoval(
                cap, profile, equipGroup, List.of(baseGroup),
                OverflowPolicy.DENY_REMOVE_IF_OVERFLOW, null);

        assertTrue(result.isSuccess(), result.getDebugDetail());
        // Equipment slots must be cleared
        assertTrue(cap.getInventory().getStackInSlot(4).isEmpty());
        assertTrue(cap.getInventory().getStackInSlot(5).isEmpty());
        // Items must have moved to base slots
        int totalItems = 0;
        for (int i = 0; i < 4; i++) {
            totalItems += cap.getInventory().getStackInSlot(i).getCount();
        }
        assertEquals(15, totalItems, "All items must be relocated to base slots");
    }

    // ── extractFromSlot tests ─────────────────────────────────────────────

    @Test
    void extractFromSlot_happyPath_clearsSlot() {
        StubCapability cap = new StubCapability(3);
        PlayerLayoutProfile profile = buildProfile(playerId, 3);
        cap.getInventory().setStackInSlot(1, new ItemStack(ITEM_B, 7));

        TransactionResult result = service.extractFromSlot(cap, profile, "test:slot_1");

        assertTrue(result.isSuccess(), result.getDebugDetail());
        assertTrue(cap.getInventory().getStackInSlot(1).isEmpty());
        assertTrue(result.getDirtySlotIds().contains("test:slot_1"));
    }

    @Test
    void extractFromSlot_alreadyEmpty_isRejected() {
        StubCapability cap = new StubCapability(3);
        PlayerLayoutProfile profile = buildProfile(playerId, 3);

        TransactionResult result = service.extractFromSlot(cap, profile, "test:slot_0");

        assertEquals(TransactionResult.Status.REJECTED, result.getStatus());
    }

    // ── TransactionResult factory tests ──────────────────────────────────

    @Test
    void transactionResult_committed_isSuccess() {
        TransactionResult r = TransactionResult.committed(List.of("slot:a", "slot:b"));
        assertTrue(r.isSuccess());
        assertEquals(2, r.getDirtySlotIds().size());
        assertNull(r.getPlayerMessage());
    }

    @Test
    void transactionResult_rolledBack_isNotSuccess() {
        TransactionResult r = TransactionResult.rolledBack("test reason");
        assertFalse(r.isSuccess());
        assertEquals(TransactionResult.Status.ROLLED_BACK, r.getStatus());
        assertTrue(r.getDirtySlotIds().isEmpty());
    }

    @Test
    void transactionResult_rejected_isNotSuccess() {
        TransactionResult r = TransactionResult.rejected("bad input");
        assertFalse(r.isSuccess());
        assertEquals(TransactionResult.Status.REJECTED, r.getStatus());
    }

    @Test
    void transactionResult_dirtySlotIds_isUnmodifiable() {
        TransactionResult r = TransactionResult.committed(new ArrayList<>(List.of("x")));
        assertThrows(UnsupportedOperationException.class,
                () -> mutateList(r.getDirtySlotIds()),
                "getDirtySlotIds must return an unmodifiable list");
    }

    private static void mutateList(List<String> values) {
        values.add("y");
    }
}
