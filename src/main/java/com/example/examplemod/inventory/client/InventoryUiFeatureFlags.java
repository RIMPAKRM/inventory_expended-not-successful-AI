package com.example.examplemod.inventory.client;

import com.example.examplemod.Config;

/**
 * Client-side M7 rollout flags.
 */
public final class InventoryUiFeatureFlags {

    private InventoryUiFeatureFlags() {
    }

    public static boolean isUiReplacementEnabled() {
        return Config.isReplaceVanillaInventoryUiEnabled() && !Config.isInventoryUiKillSwitchEnabled();
    }

    static boolean isUiReplacementEnabled(boolean replaceEnabled, boolean killSwitchEnabled) {
        return replaceEnabled && !killSwitchEnabled;
    }
}

