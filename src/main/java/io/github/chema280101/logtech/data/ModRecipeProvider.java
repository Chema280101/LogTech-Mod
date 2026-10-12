package io.github.chema280101.logtech.data;

import io.github.chema280101.logtech.registry.ModItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.crafting.Ingredient;

import io.github.chema280101.logtech.LogTech;
import net.minecraft.resources.ResourceLocation;

import io.github.chema280101.logtech.registry.ModBlocks;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    private static ResourceLocation modLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(LogTech.MOD_ID, path);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        // Horno y Alto Horno: neoferrite_dust -> neoferrite_ingot
        SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(ModItems.NEOFERRITE_DUST.get()),
                RecipeCategory.MISC,
                ModItems.NEOFERRITE_INGOT.get(),
                0.7f,
                200
        ).unlockedBy("has_neoferrite_dust", has(ModItems.NEOFERRITE_DUST.get()))
        .save(recipeOutput, modLoc("neoferrite_ingot_from_smelting_neoferrite_dust"));

        SimpleCookingRecipeBuilder.blasting(
                Ingredient.of(ModItems.NEOFERRITE_DUST.get()),
                RecipeCategory.MISC,
                ModItems.NEOFERRITE_INGOT.get(),
                0.7f,
                100
        ).unlockedBy("has_neoferrite_dust", has(ModItems.NEOFERRITE_DUST.get()))
        .save(recipeOutput, modLoc("neoferrite_ingot_from_blasting_neoferrite_dust"));

        // Horno y Alto Horno: crystalite_dust -> data_crystal
        SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(ModItems.CRYSTALITE_DUST.get()),
                RecipeCategory.MISC,
                ModItems.DATA_CRYSTAL.get(),
                0.7f,
                200
        ).unlockedBy("has_crystalite_dust", has(ModItems.CRYSTALITE_DUST.get()))
        .save(recipeOutput, modLoc("data_crystal_from_smelting_crystalite_dust"));

        SimpleCookingRecipeBuilder.blasting(
                Ingredient.of(ModItems.CRYSTALITE_DUST.get()),
                RecipeCategory.MISC,
                ModItems.DATA_CRYSTAL.get(),
                0.7f,
                100
        ).unlockedBy("has_crystalite_dust", has(ModItems.CRYSTALITE_DUST.get()))
        .save(recipeOutput, modLoc("data_crystal_from_blasting_crystalite_dust"));

        // Crafteos en mesa de trabajo

        // 1. Backbone (x16) - Cable/Bus de red
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.BACKBONE.get(), 16)
                .pattern("NRN")
                .define('N', ModItems.NEOFERRITE_INGOT.get())
                .define('R', Items.REDSTONE)
                .unlockedBy("has_neoferrite_ingot", has(ModItems.NEOFERRITE_INGOT.get()))
                .save(recipeOutput, modLoc("backbone"));

        // 2. Rack (x1) - Estructura de bahías de módulos
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RACK.get(), 1)
                .pattern("INI")
                .pattern("IRI")
                .pattern("INI")
                .define('I', Items.IRON_INGOT)
                .define('N', ModItems.NEOFERRITE_INGOT.get())
                .define('R', Items.REDSTONE)
                .unlockedBy("has_neoferrite_ingot", has(ModItems.NEOFERRITE_INGOT.get()))
                .save(recipeOutput, modLoc("rack"));

        // 3. Data Module (x1) - Módulo de datos
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DATA_MODULE.get(), 1)
                .pattern("NRN")
                .pattern("RCR")
                .pattern("NIN")
                .define('N', ModItems.NEOFERRITE_INGOT.get())
                .define('R', Items.REDSTONE)
                .define('C', ModItems.DATA_CRYSTAL.get())
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_data_crystal", has(ModItems.DATA_CRYSTAL.get()))
                .save(recipeOutput, modLoc("data_module"));

        // 4. Core (x1) - Núcleo maestro
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CORE.get(), 1)
                .pattern("NCN")
                .pattern("CIC")
                .pattern("NRN")
                .define('N', ModItems.NEOFERRITE_INGOT.get())
                .define('C', ModItems.DATA_CRYSTAL.get())
                .define('I', Items.IRON_BLOCK)
                .define('R', Items.REDSTONE_BLOCK)
                .unlockedBy("has_data_crystal", has(ModItems.DATA_CRYSTAL.get()))
                .save(recipeOutput, modLoc("core"));

        // 5. Terminal (x1) - Terminal interactiva
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.TERMINAL.get(), 1)
                .pattern("IGI")
                .pattern("NCN")
                .pattern("NRN")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .define('N', ModItems.NEOFERRITE_INGOT.get())
                .define('C', ModItems.DATA_CRYSTAL.get())
                .define('R', Items.REDSTONE)
                .unlockedBy("has_data_crystal", has(ModItems.DATA_CRYSTAL.get()))
                .save(recipeOutput, modLoc("terminal"));

        // 6. Crusher (x1) - Triturador (solo materiales vanilla)
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CRUSHER.get(), 1)
                .pattern("IRI")
                .pattern("IFI")
                .pattern("CCC")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('F', Items.FURNACE)
                .define('C', Items.COBBLESTONE)
                .unlockedBy("has_furnace", has(Items.FURNACE))
                .save(recipeOutput, modLoc("crusher"));

        // 7. Scanner (x1) - Escáner (solo materiales vanilla)
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SCANNER.get(), 1)
                .pattern("IRI")
                .pattern("ICI")
                .pattern(" I ")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('C', Items.COMPASS)
                .unlockedBy("has_compass", has(Items.COMPASS))
                .save(recipeOutput, modLoc("scanner"));

        // 8. Drill (x1) - Perforadora (solo materiales vanilla)
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.DRILL.get(), 1)
                .pattern("IPI")
                .pattern("IRI")
                .pattern("ICI")
                .define('I', Items.IRON_INGOT)
                .define('P', Items.IRON_PICKAXE)
                .define('R', Items.REDSTONE)
                .define('C', Items.COBBLESTONE)
                .unlockedBy("has_iron_pickaxe", has(Items.IRON_PICKAXE))
                .save(recipeOutput, modLoc("drill"));
    }
}
