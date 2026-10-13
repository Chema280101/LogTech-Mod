package io.github.chema280101.logtech.machine.recipe;

import io.github.chema280101.logtech.registry.ModDataComponents;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public abstract class ProcessingRecipe implements Recipe<SingleRecipeInput> {

    protected final Ingredient ingredient;
    protected final List<ProcessingOutput> outputs;
    protected final int energy;
    protected final int ticks;
    protected final PurityMode purityMode;
    protected final int fluidAmount;

    public ProcessingRecipe(
            Ingredient ingredient,
            List<ProcessingOutput> outputs,
            int energy,
            int ticks,
            PurityMode purityMode,
            int fluidAmount
    ) {
        this.ingredient = ingredient;
        this.outputs = List.copyOf(outputs);
        this.energy = energy;
        this.ticks = ticks;
        this.purityMode = purityMode != null ? purityMode : PurityMode.PRESERVE;
        this.fluidAmount = Math.max(0, fluidAmount);
    }

    public Ingredient ingredient() {
        return this.ingredient;
    }

    public List<ProcessingOutput> outputs() {
        return this.outputs;
    }

    public int energy() {
        return this.energy;
    }

    public int ticks() {
        return this.ticks;
    }

    public PurityMode purityMode() {
        return this.purityMode;
    }

    public int fluidAmount() {
        return this.fluidAmount;
    }

    public int getEnergyPerTick() {
        return this.ticks > 0 ? (this.energy / this.ticks) : this.energy;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        if (input.isEmpty()) {
            return false;
        }
        return this.ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        if (this.outputs.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = this.outputs.get(0).createStack();
        ItemStack inputStack = input.item();
        if (!inputStack.isEmpty() && inputStack.has(ModDataComponents.PURITY.get())) {
            int inputPurity = ModDataComponents.getPurity(inputStack);
            int outputPurity = ProcessingLogic.calculateOutputPurity(inputPurity, this.purityMode);
            ModDataComponents.setPurity(result, outputPurity);
        }
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.outputs.isEmpty() ? ItemStack.EMPTY : this.outputs.get(0).createStack();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(this.ingredient);
        return list;
    }

    @Override
    public abstract RecipeSerializer<?> getSerializer();

    @Override
    public abstract RecipeType<?> getType();
}
