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

    // Item del bloque Rack Frio
    public static final DeferredItem<BlockItem> COLD_RACK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.COLD_RACK);

    // Item del bloque Terminal
    public static final DeferredItem<BlockItem> TERMINAL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.TERMINAL);

    // Item del bloque Triturador
    public static final DeferredItem<BlockItem> CRUSHER_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CRUSHER);

    // Item del bloque Perforadora
    public static final DeferredItem<BlockItem> DRILL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.DRILL);

    // Item del bloque Alarma
    public static final DeferredItem<BlockItem> ALARM_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ALARM);

    // Item del bloque Generador de Carbon
    public static final DeferredItem<BlockItem> COAL_GENERATOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.COAL_GENERATOR);

    // Item del bloque Centrífuga
    public static final DeferredItem<BlockItem> CENTRIFUGE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.CENTRIFUGE);

    // Item del bloque Lavadora
    public static final DeferredItem<BlockItem> WASHER_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.WASHER);

    // Item del bloque de Escoria
    public static final DeferredItem<BlockItem> SLAG_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SLAG_BLOCK);

    // Item del bloque Disipador
    public static final DeferredItem<BlockItem> HEATSINK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.HEATSINK);

    // Modulo de datos: aporta capacidad a los racks
    public static final DeferredItem<Item> DATA_MODULE = ITEMS.registerSimpleItem("data_module");
    public static final DeferredItem<Item> DATA_MODULE_ADVANCED = ITEMS.registerSimpleItem("data_module_advanced");
    public static final DeferredItem<Item> DATA_MODULE_MASTER = ITEMS.registerSimpleItem("data_module_master");

    // Subproductos y materiales
    public static final DeferredItem<Item> SLAG = ITEMS.registerSimpleItem("slag");
    public static final DeferredItem<Item> CRYSTAL_SHARD = ITEMS.registerSimpleItem("crystal_shard");
    public static final DeferredItem<Item> SLUDGE = ITEMS.registerSimpleItem("sludge");

    // Mineral A: Neoferrita
    public static final DeferredItem<Item> RAW_NEOFERRITE = ITEMS.register(
            "raw_neoferrite",
            () -> new Item(new Item.Properties().component(ModDataComponents.PURITY.get(), 1))
    );
    public static final DeferredItem<Item> NEOFERRITE_DUST = ITEMS.register(
            "neoferrite_dust",
            () -> new Item(new Item.Properties().component(ModDataComponents.PURITY.get(), 1))
    );
    public static final DeferredItem<Item> NEOFERRITE_INGOT = ITEMS.registerSimpleItem("neoferrite_ingot");

    // Mineral B: Cristalita
    public static final DeferredItem<Item> RAW_CRYSTALITE = ITEMS.register(
            "raw_crystalite",
            () -> new Item(new Item.Properties().component(ModDataComponents.PURITY.get(), 1))
    );
    public static final DeferredItem<Item> CRYSTALITE_DUST = ITEMS.register(
            "crystalite_dust",
            () -> new Item(new Item.Properties().component(ModDataComponents.PURITY.get(), 1))
    );

    // Cristal de datos
    public static final DeferredItem<Item> DATA_CRYSTAL = ITEMS.registerSimpleItem("data_crystal");

    // Escáner de vetas
    public static final DeferredItem<io.github.chema280101.logtech.item.ScannerItem> SCANNER =
            ITEMS.register("scanner", () -> new io.github.chema280101.logtech.item.ScannerItem(new Item.Properties().stacksTo(1)));

    // Chips de firmware
    public static final DeferredItem<io.github.chema280101.logtech.item.FirmwareItem> FIRMWARE_ECO =
            ITEMS.register("firmware_eco", () -> new io.github.chema280101.logtech.item.FirmwareItem(
                    io.github.chema280101.logtech.machine.firmware.FirmwareType.ECO,
                    new Item.Properties()
            ));
    public static final DeferredItem<io.github.chema280101.logtech.item.FirmwareItem> FIRMWARE_TURBO =
            ITEMS.register("firmware_turbo", () -> new io.github.chema280101.logtech.item.FirmwareItem(
                    io.github.chema280101.logtech.machine.firmware.FirmwareType.TURBO,
                    new Item.Properties()
            ));
    public static final DeferredItem<io.github.chema280101.logtech.item.FirmwareItem> FIRMWARE_SAFE =
            ITEMS.register("firmware_safe", () -> new io.github.chema280101.logtech.item.FirmwareItem(
                    io.github.chema280101.logtech.machine.firmware.FirmwareType.SAFE,
                    new Item.Properties()
            ));
    public static final DeferredItem<io.github.chema280101.logtech.item.FirmwareItem> FIRMWARE_SELECTIVE =
            ITEMS.register("firmware_selective", () -> new io.github.chema280101.logtech.item.FirmwareItem(
                    io.github.chema280101.logtech.machine.firmware.FirmwareType.SELECTIVE,
                    new Item.Properties()
            ));

    // Llave de configuración
    public static final DeferredItem<io.github.chema280101.logtech.item.ConfigKeyItem> CONFIG_KEY =
            ITEMS.register("config_key", () -> new io.github.chema280101.logtech.item.ConfigKeyItem(new Item.Properties()));
}
