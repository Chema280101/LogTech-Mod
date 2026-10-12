package io.github.chema280101.logtech;

import io.github.chema280101.logtech.registry.ModBlocks;
import io.github.chema280101.logtech.registry.ModCreativeTabs;
import io.github.chema280101.logtech.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(LogTech.MOD_ID)
public class LogTech {
    public static final String MOD_ID = "logtech";

    public LogTech(IEventBus modEventBus, ModContainer modContainer) {
        // Primero bloques, luego items (los items dependen de los bloques)
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
    }
}
