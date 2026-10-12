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
                        output.accept(ModItems.DATA_MODULE.get());
                    })
                    .build());
}
