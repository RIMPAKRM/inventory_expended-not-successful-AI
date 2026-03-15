package com.example.examplemod.inventory.core.transaction;

import com.example.examplemod.inventory.core.capability.IExtendedInventoryCapability;
import com.example.examplemod.inventory.core.model.InventoryNbtCodec;
import com.example.examplemod.inventory.core.model.PlayerInventoryRecord;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Focused tests for {@link InventorySnapshot} restore semantics.
 */
@Disabled("Requires Minecraft bootstrap for ItemStack runtime; migrate to Forge GameTest")
class InventorySnapshotTest {

    private static final Item ITEM_A = new Item(new Item.Properties());
    private static final Item ITEM_B = new Item(new Item.Properties());

    static final class SnapshotCap implements IExtendedInventoryCapability {
        private final ItemStackHandler handler;
        private long revision;
        private boolean dirty;
        private String layoutVersion = "";

        SnapshotCap(int slots) {
            this.handler = new ItemStackHandler(slots);
        }

        @Override public ItemStackHandler getInventory() { return handler; }
        @Override public long getRevision() { return revision; }
        @Override public boolean isDirty() { return dirty; }
        @Override public void markDirty() { revision++; dirty = true; }
        @Override public void clearDirty() { dirty = false; }
        @Override public @NotNull String getLayoutVersion() { return layoutVersion; }
        @Override public void setLayoutVersion(@NotNull String layoutVersion) { this.layoutVersion = layoutVersion; }

        @Override
        public PlayerInventoryRecord toRecord() {
            return new PlayerInventoryRecord(
                    InventoryNbtCodec.CURRENT_SCHEMA_VERSION,
                    revision,
                    dirty,
                    handler.serializeNBT(),
                    layoutVersion);
        }

        @Override
        public void loadFromRecord(@NotNull PlayerInventoryRecord record) {
            this.revision = record.revision();
            this.dirty = record.dirty();
            this.layoutVersion = record.layoutVersion();
            this.handler.deserializeNBT(record.inventoryTag());
        }

        @Override
        public void restoreFromSnapshot(@NotNull List<ItemStack> items,
                                        long snapshotRevision,
                                        boolean snapshotDirty,
                                        @NotNull String snapshotLayoutVersion) {
            int count = Math.min(handler.getSlots(), items.size());
            for (int i = 0; i < count; i++) {
                handler.setStackInSlot(i, items.get(i).copy());
            }
            for (int i = count; i < handler.getSlots(); i++) {
                handler.setStackInSlot(i, ItemStack.EMPTY);
            }
            this.revision = snapshotRevision;
            this.dirty = snapshotDirty;
            this.layoutVersion = snapshotLayoutVersion;
        }

        @Override public CompoundTag serializeCapabilityNbt() { return new CompoundTag(); }
        @Override public void deserializeCapabilityNbt(@NotNull CompoundTag nbt) {}
    }

    @Test
    void restoreRestoresItemsAndMeta() {
        SnapshotCap cap = new SnapshotCap(3);
        cap.getInventory().setStackInSlot(0, new ItemStack(ITEM_A, 8));
        cap.setLayoutVersion("layout-v1");
        cap.markDirty();

        InventorySnapshot snapshot = InventorySnapshot.take(cap);

        cap.getInventory().setStackInSlot(0, new ItemStack(ITEM_B, 1));
        cap.getInventory().setStackInSlot(1, new ItemStack(ITEM_B, 64));
        cap.setLayoutVersion("layout-v2");
        cap.markDirty();

        snapshot.restore(cap);

        Assertions.assertEquals(ITEM_A, cap.getInventory().getStackInSlot(0).getItem());
        Assertions.assertTrue(cap.getInventory().getStackInSlot(1).isEmpty());
        Assertions.assertEquals("layout-v1", cap.getLayoutVersion());
        Assertions.assertEquals(snapshot.getRevision(), cap.getRevision());
        Assertions.assertEquals(snapshot.isDirty(), cap.isDirty());
    }
}
