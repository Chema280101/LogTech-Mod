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

    // Modulo de datos: mas adelante aportara capacidad a los racks
    public static final DeferredItem<Item> DATA_MODULE = ITEMS.registerSimpleItem("data_module");
}
