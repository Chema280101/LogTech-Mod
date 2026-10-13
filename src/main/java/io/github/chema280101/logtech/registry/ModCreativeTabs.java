package io.github.chema280101.logtech.registry;

import io.github.chema280101.logtech.LogTech;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LogTech.MOD_ID);

    public static final Supplier<CreativeModeTab> LOGTECH_TAB = TABS.register("logtech", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + LogTech.MOD_ID))
                    .icon(() -> new ItemStack(ModItems.CORE_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.CORE_ITEM.get());
                        output.accept(ModItems.BACKBONE_ITEM.get());
                        output.accept(ModItems.RACK_ITEM.get());
                        output.accept(ModItems.COLD_RACK_ITEM.get());
                        output.accept(ModItems.TERMINAL_ITEM.get());
                        output.accept(ModItems.CRUSHER_ITEM.get());
                        output.accept(ModItems.DRILL_ITEM.get());
                        output.accept(ModItems.ALARM_ITEM.get());
                        output.accept(ModItems.COAL_GENERATOR_ITEM.get());
                        output.accept(ModItems.CENTRIFUGE_ITEM.get());
                        output.accept(ModItems.WASHER_ITEM.get());
                        output.accept(ModItems.SLAG_BLOCK_ITEM.get());
                        output.accept(ModItems.DATA_MODULE.get());
                        output.accept(ModItems.DATA_MODULE_ADVANCED.get());
                        output.accept(ModItems.DATA_MODULE_MASTER.get());
                        output.accept(ModItems.RAW_NEOFERRITE.get());
                        output.accept(ModItems.NEOFERRITE_DUST.get());
                        output.accept(ModItems.NEOFERRITE_INGOT.get());
                        output.accept(ModItems.RAW_CRYSTALITE.get());
                        output.accept(ModItems.CRYSTALITE_DUST.get());
                        output.accept(ModItems.DATA_CRYSTAL.get());
                        output.accept(ModItems.SLAG.get());
                        output.accept(ModItems.CRYSTAL_SHARD.get());
                        output.accept(ModItems.SLUDGE.get());
                        output.accept(ModItems.SCANNER.get());
                    })
                    .build());
}
