package io.github.chema280101.logtech.compat.jei;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.client.CentrifugeScreen;
import io.github.chema280101.logtech.client.CrusherScreen;
import io.github.chema280101.logtech.client.WasherScreen;
import io.github.chema280101.logtech.machine.recipe.CentrifugingRecipe;
import io.github.chema280101.logtech.machine.recipe.CrushingRecipe;
import io.github.chema280101.logtech.machine.recipe.WashingRecipe;
import io.github.chema280101.logtech.registry.ModBlocks;
import io.github.chema280101.logtech.registry.ModRecipeTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;

@JeiPlugin
public class LogTechJeiPlugin implements IModPlugin {

    public static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, "jei_plugin");

    public static final RecipeType<CrushingRecipe> CRUSHING_TYPE =
            RecipeType.create(LogTech.MOD_ID, "crushing", CrushingRecipe.class);

    public static final RecipeType<WashingRecipe> WASHING_TYPE =
            RecipeType.create(LogTech.MOD_ID, "washing", WashingRecipe.class);

    public static final RecipeType<CentrifugingRecipe> CENTRIFUGING_TYPE =
            RecipeType.create(LogTech.MOD_ID, "centrifuging", CentrifugingRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new CrushingCategory(registration.getJeiHelpers().getGuiHelper()),
                new WashingCategory(registration.getJeiHelpers().getGuiHelper()),
                new CentrifugingCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager recipeManager = getRecipeManager();
        if (recipeManager == null) {
            return;
        }

        List<CrushingRecipe> crushingRecipes = recipeManager.getAllRecipesFor(ModRecipeTypes.CRUSHING.get())
                .stream().map(RecipeHolder::value).toList();
        registration.addRecipes(CRUSHING_TYPE, crushingRecipes);

        List<WashingRecipe> washingRecipes = recipeManager.getAllRecipesFor(ModRecipeTypes.WASHING.get())
                .stream().map(RecipeHolder::value).toList();
        registration.addRecipes(WASHING_TYPE, washingRecipes);

        List<CentrifugingRecipe> centrifugingRecipes = recipeManager.getAllRecipesFor(ModRecipeTypes.CENTRIFUGING.get())
                .stream().map(RecipeHolder::value).toList();
        registration.addRecipes(CENTRIFUGING_TYPE, centrifugingRecipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.CRUSHER.get(), CRUSHING_TYPE);
        registration.addRecipeCatalyst(ModBlocks.WASHER.get(), WASHING_TYPE);
        registration.addRecipeCatalyst(ModBlocks.CENTRIFUGE.get(), CENTRIFUGING_TYPE);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(CrusherScreen.class, 79, 34, 24, 17, CRUSHING_TYPE);
        registration.addRecipeClickArea(WasherScreen.class, 82, 34, 24, 17, WASHING_TYPE);
        registration.addRecipeClickArea(CentrifugeScreen.class, 72, 34, 24, 17, CENTRIFUGING_TYPE);
    }

    private static RecipeManager getRecipeManager() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection != null) {
            return connection.getRecipeManager();
        }
        if (minecraft.level != null) {
            return minecraft.level.getRecipeManager();
        }
        return null;
    }
}
