package io.github.chema280101.logtech.registry;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.machine.recipe.CrushingRecipe;
import io.github.chema280101.logtech.machine.recipe.ProcessingRecipeSerializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, LogTech.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrushingRecipe>> CRUSHING =
            RECIPE_SERIALIZERS.register("crushing", () -> new ProcessingRecipeSerializer<>(CrushingRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<io.github.chema280101.logtech.machine.recipe.CentrifugingRecipe>> CENTRIFUGING =
            RECIPE_SERIALIZERS.register("centrifuging", () -> new ProcessingRecipeSerializer<>(io.github.chema280101.logtech.machine.recipe.CentrifugingRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<io.github.chema280101.logtech.machine.recipe.WashingRecipe>> WASHING =
            RECIPE_SERIALIZERS.register("washing", () -> new ProcessingRecipeSerializer<>(io.github.chema280101.logtech.machine.recipe.WashingRecipe::new));
}
