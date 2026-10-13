package io.github.chema280101.logtech.machine.recipe;

import io.github.chema280101.logtech.registry.ModDataComponents;
import io.github.chema280101.logtech.registry.ModRecipeSerializers;
import io.github.chema280101.logtech.registry.ModRecipeTypes;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public class WashingRecipe extends ProcessingRecipe {

    public WashingRecipe(
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
    public boolean matches(SingleRecipeInput input, Level level) {
        if (input.isEmpty()) {
            return false;
        }
        ItemStack itemStack = input.item();
        if (itemStack.has(ModDataComponents.PURITY.get()) && ModDataComponents.getPurity(itemStack) >= 5) {
            return false;
        }
        return super.matches(input, level);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.WASHING.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.WASHING.get();
    }
}
