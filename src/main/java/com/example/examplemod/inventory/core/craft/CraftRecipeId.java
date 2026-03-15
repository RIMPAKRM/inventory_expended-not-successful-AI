package com.example.examplemod.inventory.core.craft;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum CraftRecipeId {
    TEST_LEATHER_RIG("test_leather_rig");

    private final String id;

    CraftRecipeId(@NotNull String id) {
        this.id = id;
    }

    @NotNull
    public String id() {
        return id;
    }

    @Nullable
    public static CraftRecipeId fromId(@NotNull String id) {
        for (CraftRecipeId value : values()) {
            if (value.id.equals(id)) {
                return value;
            }
        }
        return null;
    }
}

