package com.example.examplemod.inventory.core.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class ModCapabilities {
    public static final Capability<IExtendedInventoryCapability> EXTENDED_INVENTORY = CapabilityManager.get(new CapabilityToken<>() {
    });

    private ModCapabilities() {
    }
}
