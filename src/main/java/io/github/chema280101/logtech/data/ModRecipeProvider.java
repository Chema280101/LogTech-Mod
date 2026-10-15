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
import io.github.chema280101.logtech.machine.recipe.OutputScaling;
import io.github.chema280101.logtech.machine.recipe.PurityMode;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
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

        // 2b. Cold Rack (x1) - Rack frío de alta densidad
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.COLD_RACK.get(), 1)
                .pattern("ICI")
                .pattern("CRC")
                .pattern("ICI")
                .define('I', Items.IRON_INGOT)
                .define('C', ModItems.DATA_CRYSTAL.get())
                .define('R', ModBlocks.RACK.get())
                .unlockedBy("has_rack", has(ModBlocks.RACK.get()))
                .save(recipeOutput, modLoc("cold_rack"));

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

        // 3b. Data Module Advanced (x1) - Módulo de datos avanzado
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DATA_MODULE_ADVANCED.get(), 1)
                .pattern("SCS")
                .pattern("SMS")
                .pattern("SNS")
                .define('S', ModItems.CRYSTAL_SHARD.get())
                .define('C', ModItems.DATA_CRYSTAL.get())
                .define('M', ModItems.DATA_MODULE.get())
                .define('N', ModItems.NEOFERRITE_INGOT.get())
                .unlockedBy("has_crystal_shard", has(ModItems.CRYSTAL_SHARD.get()))
                .save(recipeOutput, modLoc("data_module_advanced"));

        // 3c. Data Module Master (x1) - Módulo de datos maestro
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DATA_MODULE_MASTER.get(), 1)
                .pattern("SCS")
                .pattern("CMC")
                .pattern("SCS")
                .define('S', ModItems.CRYSTAL_SHARD.get())
                .define('C', ModItems.DATA_CRYSTAL.get())
                .define('M', ModItems.DATA_MODULE_ADVANCED.get())
                .unlockedBy("has_crystal_shard", has(ModItems.CRYSTAL_SHARD.get()))
                .save(recipeOutput, modLoc("data_module_master"));

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

        // 8b. Alarm (x1) - Alarma de red
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.ALARM.get(), 1)
                .pattern(" T ")
                .pattern("INI")
                .pattern("IRI")
                .define('T', Items.REDSTONE_TORCH)
                .define('N', ModItems.NEOFERRITE_INGOT.get())
                .define('R', Items.REDSTONE)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_neoferrite_ingot", has(ModItems.NEOFERRITE_INGOT.get()))
                .save(recipeOutput, modLoc("alarm"));

        // 8c. Coal Generator (x1) - Generador temporal de carbón
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.COAL_GENERATOR.get(), 1)
                .pattern("III")
                .pattern("IFI")
                .pattern("CRC")
                .define('I', Items.IRON_INGOT)
                .define('F', Items.FURNACE)
                .define('R', Items.REDSTONE)
                .define('C', Items.COBBLESTONE)
                .unlockedBy("has_furnace", has(Items.FURNACE))
                .save(recipeOutput, modLoc("coal_generator"));

        // 9. Recetas del Triturador (logtech:crushing)
        ProcessingRecipeBuilder.crushing(ModItems.RAW_NEOFERRITE.get())
                .addOutput(ModItems.NEOFERRITE_DUST.get(), 2)
                .energy(4000)
                .ticks(100)
                .purityMode(io.github.chema280101.logtech.machine.recipe.PurityMode.PRESERVE)
                .unlockedBy("has_raw_neoferrite", has(ModItems.RAW_NEOFERRITE.get()))
                .save(recipeOutput, modLoc("crushing/raw_neoferrite"));

        ProcessingRecipeBuilder.crushing(ModItems.RAW_CRYSTALITE.get())
                .addOutput(ModItems.CRYSTALITE_DUST.get(), 2)
                .energy(4000)
                .ticks(100)
                .purityMode(PurityMode.PRESERVE)
                .unlockedBy("has_raw_crystalite", has(ModItems.RAW_CRYSTALITE.get()))
                .save(recipeOutput, modLoc("crushing/raw_crystalite"));

        // 10. Centrífuga (x1) - Separador industrial de subproductos
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CENTRIFUGE.get(), 1)
                .pattern("INI")
                .pattern("NRN")
                .pattern("ICI")
                .define('I', Items.IRON_INGOT)
                .define('N', ModItems.NEOFERRITE_INGOT.get())
                .define('R', Items.REDSTONE)
                .define('C', ModItems.DATA_CRYSTAL.get())
                .unlockedBy("has_neoferrite_ingot", has(ModItems.NEOFERRITE_INGOT.get()))
                .save(recipeOutput, modLoc("centrifuge"));

        // 11. Bloque de escoria industrial (x1) a partir de 4 escorias
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.SLAG_BLOCK.get(), 1)
                .pattern("SS")
                .pattern("SS")
                .define('S', ModItems.SLAG.get())
                .unlockedBy("has_slag", has(ModItems.SLAG.get()))
                .save(recipeOutput, modLoc("slag_block"));

        // 11b. Desarmar bloque de escoria en 4 escorias
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.SLAG.get(), 4)
                .requires(ModBlocks.SLAG_BLOCK.get())
                .unlockedBy("has_slag_block", has(ModBlocks.SLAG_BLOCK.get()))
                .save(recipeOutput, modLoc("slag_from_slag_block"));

        // 12. Recetas de la Centrífuga (logtech:centrifuging)
        // Polvo de neoferrita: 1 polvo (120 ticks, 80 FE/t = 9600 FE) -> 1 polvo (100%) + escoria (80% escalada con inverse_purity)
        ProcessingRecipeBuilder.centrifuging(ModItems.NEOFERRITE_DUST.get())
                .addOutput(ModItems.NEOFERRITE_DUST.get(), 1, 1.0f, OutputScaling.NONE)
                .addOutput(ModItems.SLAG.get(), 1, 0.8f, OutputScaling.INVERSE_PURITY)
                .energy(9600)
                .ticks(120)
                .purityMode(PurityMode.PRESERVE)
                .unlockedBy("has_neoferrite_dust", has(ModItems.NEOFERRITE_DUST.get()))
                .save(recipeOutput, modLoc("centrifuging/neoferrite_dust"));

        // Polvo de cristalita: 1 polvo (120 ticks, 80 FE/t = 9600 FE) -> 1 polvo (100%) + escoria (80% escalada con inverse_purity)
        ProcessingRecipeBuilder.centrifuging(ModItems.CRYSTALITE_DUST.get())
                .addOutput(ModItems.CRYSTALITE_DUST.get(), 1, 1.0f, OutputScaling.NONE)
                .addOutput(ModItems.SLAG.get(), 1, 0.8f, OutputScaling.INVERSE_PURITY)
                .energy(9600)
                .ticks(120)
                .purityMode(PurityMode.PRESERVE)
                .unlockedBy("has_crystalite_dust", has(ModItems.CRYSTALITE_DUST.get()))
                .save(recipeOutput, modLoc("centrifuging/crystalite_dust"));

        // Lodo: 1 lodo (120 ticks, 80 FE/t = 9600 FE) -> 1 fragmento de cristal (25% de probabilidad)
        ProcessingRecipeBuilder.centrifuging(ModItems.SLUDGE.get())
                .addOutput(ModItems.CRYSTAL_SHARD.get(), 1, 0.25f, OutputScaling.NONE)
                .energy(9600)
                .ticks(120)
                .purityMode(PurityMode.PRESERVE)
                .unlockedBy("has_sludge", has(ModItems.SLUDGE.get()))
                .save(recipeOutput, modLoc("centrifuging/sludge"));

        // 13. Lavadora (x1) - Purificador de minerales por flotación y agua
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.WASHER.get(), 1)
                .pattern("INI")
                .pattern("NBN")
                .pattern("ICI")
                .define('I', Items.IRON_INGOT)
                .define('N', ModItems.NEOFERRITE_INGOT.get())
                .define('B', Items.WATER_BUCKET)
                .define('C', ModItems.DATA_CRYSTAL.get())
                .unlockedBy("has_neoferrite_ingot", has(ModItems.NEOFERRITE_INGOT.get()))
                .save(recipeOutput, modLoc("washer"));

        // 14. Recetas de la Lavadora (logtech:washing)
        // 1 polvo de pureza p (<5) + 250 mB agua en 80 ticks @ 60 FE/t (4800 FE) -> 1 polvo pureza p+1 + 1 lodo
        ProcessingRecipeBuilder.washing(ModItems.NEOFERRITE_DUST.get())
                .addOutput(ModItems.NEOFERRITE_DUST.get(), 1)
                .addOutput(ModItems.SLUDGE.get(), 1)
                .energy(4800)
                .ticks(80)
                .fluidAmount(250)
                .purityMode(PurityMode.INCREASE)
                .unlockedBy("has_neoferrite_dust", has(ModItems.NEOFERRITE_DUST.get()))
                .save(recipeOutput, modLoc("washing/neoferrite_dust"));

        ProcessingRecipeBuilder.washing(ModItems.CRYSTALITE_DUST.get())
                .addOutput(ModItems.CRYSTALITE_DUST.get(), 1)
                .addOutput(ModItems.SLUDGE.get(), 1)
                .energy(4800)
                .ticks(80)
                .fluidAmount(250)
                .purityMode(PurityMode.INCREASE)
                .unlockedBy("has_crystalite_dust", has(ModItems.CRYSTALITE_DUST.get()))
                .save(recipeOutput, modLoc("washing/crystalite_dust"));

        // 15. Disipador térmico (x1) - Refrigeración pasiva para máquinas
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.HEATSINK.get(), 1)
                .pattern("BBB")
                .pattern("NNN")
                .pattern("III")
                .define('B', Items.IRON_BARS)
                .define('N', ModItems.NEOFERRITE_INGOT.get())
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_neoferrite_ingot", has(ModItems.NEOFERRITE_INGOT.get()))
                .save(recipeOutput, modLoc("heatsink"));

        // 16. Firmware Chips (x1) - Chips de optimización y comportamiento
        // 16a. Firmware Eco
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FIRMWARE_ECO.get(), 1)
                .pattern(" S ")
                .pattern("SCS")
                .pattern(" S ")
                .define('S', ModItems.CRYSTAL_SHARD.get())
                .define('C', ModItems.DATA_CRYSTAL.get())
                .unlockedBy("has_data_crystal", has(ModItems.DATA_CRYSTAL.get()))
                .save(recipeOutput, modLoc("firmware_eco"));

        // 16b. Firmware Turbo
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FIRMWARE_TURBO.get(), 1)
                .pattern(" R ")
                .pattern("SCS")
                .pattern(" R ")
                .define('R', Items.REDSTONE)
                .define('S', ModItems.CRYSTAL_SHARD.get())
                .define('C', ModItems.DATA_CRYSTAL.get())
                .unlockedBy("has_data_crystal", has(ModItems.DATA_CRYSTAL.get()))
                .save(recipeOutput, modLoc("firmware_turbo"));

        // 16c. Firmware Seguro
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FIRMWARE_SAFE.get(), 1)
                .pattern(" I ")
                .pattern("SCS")
                .pattern(" I ")
                .define('I', Items.IRON_INGOT)
                .define('S', ModItems.CRYSTAL_SHARD.get())
                .define('C', ModItems.DATA_CRYSTAL.get())
                .unlockedBy("has_data_crystal", has(ModItems.DATA_CRYSTAL.get()))
                .save(recipeOutput, modLoc("firmware_safe"));

        // 16d. Firmware Selectivo
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FIRMWARE_SELECTIVE.get(), 1)
                .pattern(" H ")
                .pattern("SCS")
                .pattern(" H ")
                .define('H', Items.HOPPER)
                .define('S', ModItems.CRYSTAL_SHARD.get())
                .define('C', ModItems.DATA_CRYSTAL.get())
                .unlockedBy("has_data_crystal", has(ModItems.DATA_CRYSTAL.get()))
                .save(recipeOutput, modLoc("firmware_selective"));

        // 17. Llave de configuración
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.CONFIG_KEY.get(), 1)
                .pattern("  I")
                .pattern(" R ")
                .pattern("N  ")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('N', ModItems.NEOFERRITE_INGOT.get())
                .unlockedBy("has_neoferrite_ingot", has(ModItems.NEOFERRITE_INGOT.get()))
                .save(recipeOutput, modLoc("config_key"));
    }
}
