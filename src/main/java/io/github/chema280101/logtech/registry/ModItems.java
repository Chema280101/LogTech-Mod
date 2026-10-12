package io.github.chema280101.logtech.registry;

import io.github.chema280101.logtech.LogTech;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LogTech.MOD_ID);

    // Item del bloque Nucleo (usa el nombre del bloque: "core")
    public static final DeferredItem<BlockItem> CORE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CORE);

    // Item del bloque Backbone
    public static final DeferredItem<BlockItem> BACKBONE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BACKBONE);

    // Item del bloque Rack
    public static final DeferredItem<BlockItem> RACK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.RACK);

    // Item del bloque Terminal
    public static final DeferredItem<BlockItem> TERMINAL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.TERMINAL);

    // Item del bloque Triturador
    public static final DeferredItem<BlockItem> CRUSHER_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CRUSHER);

    // Item del bloque Perforadora
    public static final DeferredItem<BlockItem> DRILL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.DRILL);

    // Modulo de datos: aporta capacidad a los racks
    public static final DeferredItem<Item> DATA_MODULE = ITEMS.registerSimpleItem("data_module");

    // Mineral A: Neoferrita
    public static final DeferredItem<Item> RAW_NEOFERRITE = ITEMS.registerSimpleItem("raw_neoferrite");
    public static final DeferredItem<Item> NEOFERRITE_DUST = ITEMS.registerSimpleItem("neoferrite_dust");
    public static final DeferredItem<Item> NEOFERRITE_INGOT = ITEMS.registerSimpleItem("neoferrite_ingot");

    // Mineral B: Cristalita
    public static final DeferredItem<Item> RAW_CRYSTALITE = ITEMS.registerSimpleItem("raw_crystalite");
    public static final DeferredItem<Item> CRYSTALITE_DUST = ITEMS.registerSimpleItem("crystalite_dust");

    // Cristal de datos
    public static final DeferredItem<Item> DATA_CRYSTAL = ITEMS.registerSimpleItem("data_crystal");

    // Escáner de vetas
    public static final DeferredItem<io.github.chema280101.logtech.item.ScannerItem> SCANNER =
            ITEMS.register("scanner", () -> new io.github.chema280101.logtech.item.ScannerItem(new Item.Properties().stacksTo(1)));
}
