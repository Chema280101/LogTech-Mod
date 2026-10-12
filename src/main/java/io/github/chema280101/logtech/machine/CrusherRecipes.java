package io.github.chema280101.logtech.machine;

import io.github.chema280101.logtech.registry.ModItems;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class CrusherRecipes {
    public record Recipe(Item input, ItemStack output, int processingTime, int energyPerTick) {}

    private static final Map<Item, Recipe> RECIPES = new HashMap<>();

    public static Optional<Recipe> getRecipe(ItemStack input) {
        if (input.isEmpty()) {
            return Optional.empty();
        }
        ensureInitialized();
        return Optional.ofNullable(RECIPES.get(input.getItem()));
    }

    public static boolean hasRecipe(ItemStack input) {
        return getRecipe(input).isPresent();
    }

    private static synchronized void ensureInitialized() {
        if (RECIPES.isEmpty()) {
            RECIPES.put(
                    ModItems.RAW_NEOFERRITE.get(),
                    new Recipe(ModItems.RAW_NEOFERRITE.get(), new ItemStack(ModItems.NEOFERRITE_DUST.get(), 2), 100, 40)
            );
            RECIPES.put(
                    ModItems.RAW_CRYSTALITE.get(),
                    new Recipe(ModItems.RAW_CRYSTALITE.get(), new ItemStack(ModItems.CRYSTALITE_DUST.get(), 2), 100, 40)
            );
        }
    }
}
