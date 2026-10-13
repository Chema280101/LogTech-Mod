package io.github.chema280101.logtech.machine.recipe;

import io.github.chema280101.logtech.registry.ModRecipeSerializers;
import io.github.chema280101.logtech.registry.ModRecipeTypes;
import java.util.List;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public class CrushingRecipe extends ProcessingRecipe {

    public CrushingRecipe(
            Ingredient ingredient,
            List<ProcessingOutput> outputs,
            int energy,
            int ticks,
            PurityMode purityMode,
            int fluidAmount
    ) {
        super(ingredient, outputs, energy, ticks, purityMode, fluidAmount);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.CRUSHING.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.CRUSHING.get();
    }
}
