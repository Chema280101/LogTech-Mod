package io.github.chema280101.logtech.data;

import io.github.chema280101.logtech.machine.recipe.CrushingRecipe;
import io.github.chema280101.logtech.machine.recipe.OutputScaling;
import io.github.chema280101.logtech.machine.recipe.ProcessingOutput;
import io.github.chema280101.logtech.machine.recipe.ProcessingRecipe;
import io.github.chema280101.logtech.machine.recipe.ProcessingRecipeSerializer;
import io.github.chema280101.logtech.machine.recipe.PurityMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class ProcessingRecipeBuilder implements RecipeBuilder {

    private final ProcessingRecipeSerializer.Factory<? extends ProcessingRecipe> factory;
    private final Ingredient ingredient;
    private final List<ProcessingOutput> outputs = new ArrayList<>();
    private int energy = 4000;
    private int ticks = 100;
    private PurityMode purityMode = PurityMode.PRESERVE;
    private int fluidAmount = 0;
    @Nullable
    private String group;
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    private ProcessingRecipeBuilder(ProcessingRecipeSerializer.Factory<? extends ProcessingRecipe> factory, Ingredient ingredient) {
        this.factory = factory;
        this.ingredient = ingredient;
    }

    public static ProcessingRecipeBuilder crushing(Ingredient input) {
        return new ProcessingRecipeBuilder(CrushingRecipe::new, input);
    }

    public static ProcessingRecipeBuilder crushing(ItemLike input) {
        return crushing(Ingredient.of(input));
    }

    public static ProcessingRecipeBuilder centrifuging(Ingredient input) {
        return new ProcessingRecipeBuilder(io.github.chema280101.logtech.machine.recipe.CentrifugingRecipe::new, input);
    }

    public static ProcessingRecipeBuilder centrifuging(ItemLike input) {
        return centrifuging(Ingredient.of(input));
    }

    public static ProcessingRecipeBuilder washing(Ingredient input) {
        return new ProcessingRecipeBuilder(io.github.chema280101.logtech.machine.recipe.WashingRecipe::new, input);
    }

    public static ProcessingRecipeBuilder washing(ItemLike input) {
        return washing(Ingredient.of(input));
    }

    public ProcessingRecipeBuilder addOutput(ItemLike item, int count, float chance, OutputScaling scaling) {
        this.outputs.add(new ProcessingOutput(item.asItem(), count, chance, scaling));
        return this;
    }

    public ProcessingRecipeBuilder addOutput(ItemLike item, int count) {
        return addOutput(item, count, 1.0f, OutputScaling.NONE);
    }

    public ProcessingRecipeBuilder energy(int energy) {
        this.energy = energy;
        return this;
    }

    public ProcessingRecipeBuilder ticks(int ticks) {
        this.ticks = ticks;
        return this;
    }

    public ProcessingRecipeBuilder purityMode(PurityMode purityMode) {
        this.purityMode = purityMode;
        return this;
    }

    public ProcessingRecipeBuilder fluidAmount(int fluidAmount) {
        this.fluidAmount = fluidAmount;
        return this;
    }

    @Override
    public ProcessingRecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        this.criteria.put(name, criterion);
        return this;
    }

    @Override
    public ProcessingRecipeBuilder group(@Nullable String group) {
        this.group = group;
        return this;
    }

    @Override
    public Item getResult() {
        return this.outputs.isEmpty() ? Items.AIR : this.outputs.get(0).item();
    }

    @Override
    public void save(RecipeOutput output, ResourceLocation id) {
        if (this.outputs.isEmpty()) {
            throw new IllegalStateException("Processing recipe " + id + " has no outputs defined!");
        }

        Advancement.Builder advancement = null;
        if (!this.criteria.isEmpty()) {
            advancement = output.advancement()
                    .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
                    .rewards(AdvancementRewards.Builder.recipe(id))
                    .requirements(AdvancementRequirements.Strategy.OR);
            this.criteria.forEach(advancement::addCriterion);
        }

        ProcessingRecipe recipe = this.factory.create(
                this.ingredient,
                this.outputs,
                this.energy,
                this.ticks,
                this.purityMode,
                this.fluidAmount
        );

        output.accept(
                id,
                recipe,
                advancement != null ? advancement.build(id.withPrefix("recipes/")) : null
        );
    }
}
