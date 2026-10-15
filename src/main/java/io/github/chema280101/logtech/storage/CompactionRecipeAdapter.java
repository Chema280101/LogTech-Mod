package io.github.chema280101.logtech.storage;

import io.github.chema280101.logtech.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;

public final class CompactionRecipeAdapter {

    private CompactionRecipeAdapter() {}

    public static CompactionCandidate<Item> toCandidate(
            RecipeHolder<CraftingRecipe> holder,
            HolderLookup.Provider registries
    ) {
        if (holder == null || registries == null) {
            return null;
        }
        CraftingRecipe recipe = holder.value();
        ItemStack resultStack = recipe.getResultItem(registries);
        if (resultStack.isEmpty() || resultStack.getCount() <= 0) {
            return null;
        }
        // Solo ítems sin componentes de datos especiales y sin pureza
        if (!resultStack.getComponentsPatch().isEmpty() || resultStack.has(ModDataComponents.PURITY.get())) {
            return null;
        }
        Item resultItem = resultStack.getItem();
        int resultCount = resultStack.getCount();

        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients == null || ingredients.isEmpty()) {
            return null;
        }

        boolean isGrid = false;
        int gridWidth = ingredients.size();
        int gridHeight = 1;
        if (recipe instanceof ShapedRecipe shaped) {
            isGrid = true;
            gridWidth = shaped.getWidth();
            gridHeight = shaped.getHeight();
        }

        List<Item> inputItems = new ArrayList<>();
        boolean hasRemainder = false;

        for (Ingredient ing : ingredients) {
            if (ing.isEmpty()) {
                // Hueco vacío en la cuadrícula de crafteo (ej. 8 ítems en 3x3)
                return null;
            }
            ItemStack[] items = ing.getItems();
            if (items.length == 0) {
                return null;
            }
            // Exigir que todas las opciones del ingrediente correspondan al mismo ítem sin ambigüedad
            Item firstItem = items[0].getItem();
            for (ItemStack stack : items) {
                if (stack.getItem() != firstItem) {
                    return null; // Tag o ingrediente ambiguo con múltiples tipos de ítems
                }
                if (!stack.getComponentsPatch().isEmpty() || stack.has(ModDataComponents.PURITY.get())) {
                    return null; // Componentes especiales en ingrediente
                }
                if (stack.hasCraftingRemainingItem()) {
                    hasRemainder = true;
                }
            }
            if (firstItem.hasCraftingRemainingItem()) {
                hasRemainder = true;
            }
            inputItems.add(firstItem);
        }

        return new CompactionCandidate<>(
                holder.id().toString(),
                inputItems,
                isGrid,
                gridWidth,
                gridHeight,
                hasRemainder,
                resultItem,
                resultCount
        );
    }
}
