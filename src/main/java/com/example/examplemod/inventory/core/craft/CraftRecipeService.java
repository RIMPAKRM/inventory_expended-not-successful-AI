package com.example.examplemod.inventory.core.craft;

import com.example.examplemod.inventory.core.capability.IExtendedInventoryCapability;
import com.example.examplemod.inventory.core.slot.PlayerLayoutProfile;
import com.example.examplemod.inventory.core.slot.SlotDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal M8 server-authoritative craft recipe registry.
 */
public final class CraftRecipeService {

    private static final Map<CraftRecipeId, RawRecipeDefinition> RECIPES = Map.of(
            CraftRecipeId.TEST_LEATHER_RIG,
            new RawRecipeDefinition(
                    List.of(
                            new RawIngredient("minecraft:leather", 4),
                            new RawIngredient("minecraft:string", 2)
                    ),
                    "inventory:test_leather_rig",
                    1
            )
    );

    private CraftRecipeService() {
    }

    public static boolean isKnownRecipeId(@NotNull String recipeId) {
        return CraftRecipeId.fromId(recipeId) != null;
    }

    @Nullable
    public static RecipeDefinition definition(@NotNull CraftRecipeId recipeId) {
        RawRecipeDefinition raw = RECIPES.get(recipeId);
        if (raw == null) {
            return null;
        }

        List<CraftIngredientRequirement> resolvedIngredients = new ArrayList<>();
        for (RawIngredient ingredient : raw.ingredients()) {
            Item item = resolveItem(ingredient.itemId());
            if (item == null) {
                return null;
            }
            resolvedIngredients.add(new CraftIngredientRequirement(item, ingredient.count()));
        }

        Item outputItem = resolveItem(raw.outputItemId());
        if (outputItem == null) {
            return null;
        }

        return new RecipeDefinition(resolvedIngredients, new ItemStack(outputItem, raw.outputCount()));
    }

    public static boolean canCraftFromClientSlots(@NotNull CraftRecipeId recipeId,
                                                  @NotNull Map<String, ItemStack> slots) {
        RecipeDefinition recipe = definition(recipeId);
        if (recipe == null) {
            return false;
        }

        Map<Object, Integer> counts = new LinkedHashMap<>();
        for (ItemStack stack : slots.values()) {
            if (stack.isEmpty()) {
                continue;
            }
            counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }

        for (CraftIngredientRequirement ingredient : recipe.ingredients()) {
            int available = counts.getOrDefault(ingredient.item(), 0);
            if (available < ingredient.count()) {
                return false;
            }
        }
        return true;
    }

    public static boolean canCraft(@NotNull IExtendedInventoryCapability cap,
                                   @NotNull PlayerLayoutProfile profile,
                                   @NotNull CraftRecipeId recipeId) {
        RecipeDefinition recipe = definition(recipeId);
        if (recipe == null) {
            return false;
        }

        Map<Object, Integer> counts = new LinkedHashMap<>();
        for (SlotDefinition slot : profile.getAllSlots()) {
            int idx = profile.getSlotIndex(slot.getSlotId());
            if (idx < 0) {
                continue;
            }
            ItemStack stack = cap.getInventory().getStackInSlot(idx);
            if (stack.isEmpty()) {
                continue;
            }
            counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }

        for (CraftIngredientRequirement ingredient : recipe.ingredients()) {
            int available = counts.getOrDefault(ingredient.item(), 0);
            if (available < ingredient.count()) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static Item resolveItem(@NotNull String itemId) {
        ResourceLocation key = ResourceLocation.tryParse(itemId);
        if (key == null) {
            return null;
        }
        Item item = ForgeRegistries.ITEMS.getValue(key);
        return item;
    }

    private record RawIngredient(@NotNull String itemId, int count) {
    }

    private record RawRecipeDefinition(@NotNull List<RawIngredient> ingredients,
                                       @NotNull String outputItemId,
                                       int outputCount) {
    }

    public record RecipeDefinition(@NotNull List<CraftIngredientRequirement> ingredients,
                                   @NotNull ItemStack output) {
        public RecipeDefinition {
            ingredients = List.copyOf(new ArrayList<>(ingredients));
            output = output.copy();
        }

        @NotNull
        public ItemStack createOutput() {
            return output.copy();
        }
    }
}
