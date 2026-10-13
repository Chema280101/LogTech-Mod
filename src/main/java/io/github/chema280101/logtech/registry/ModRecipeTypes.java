package io.github.chema280101.logtech.registry;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.machine.recipe.CrushingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, LogTech.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CrushingRecipe>> CRUSHING =
            RECIPE_TYPES.register("crushing", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "crushing")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<io.github.chema280101.logtech.machine.recipe.CentrifugingRecipe>> CENTRIFUGING =
            RECIPE_TYPES.register("centrifuging", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "centrifuging")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<io.github.chema280101.logtech.machine.recipe.WashingRecipe>> WASHING =
            RECIPE_TYPES.register("washing", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "washing")));
}
