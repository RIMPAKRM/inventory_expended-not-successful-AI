package com.example.examplemod.inventory.core.capability;

import com.example.examplemod.Config;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExtendedInventoryProvider implements ICapabilitySerializable<CompoundTag> {
    private final ExtendedInventoryCapability backend = new ExtendedInventoryCapability(Config.getBaseRuntimeSlots());
    private final LazyOptional<IExtendedInventoryCapability> optional = LazyOptional.of(() -> backend);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.EXTENDED_INVENTORY) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return backend.serializeCapabilityNbt();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        backend.deserializeCapabilityNbt(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }
}

