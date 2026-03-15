package com.example.examplemod.inventory.core.craft;

import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

public record CraftIngredientRequirement(@NotNull Item item, int count) {

    public CraftIngredientRequirement {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be > 0");
        }
    }
}

