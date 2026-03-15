package com.example.examplemod.inventory.api;

import com.example.examplemod.inventory.core.service.ExtendedInventoryApiService;
import org.jetbrains.annotations.NotNull;

/**
 * Stable entrypoint for external mods.
 */
public final class ExtendedInventoryApi {

    private ExtendedInventoryApi() {
    }

    @NotNull
    public static IExtendedInventoryApi get() {
        return ExtendedInventoryApiService.getInstance();
    }
}

