package io.github.chema280101.logtech.client;

import io.github.chema280101.logtech.LogTech;
import io.github.chema280101.logtech.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Agrega el tooltip informativo de pureza en el cliente para todos los ítems que poseen
 * el data component logtech:purity.
 */
@EventBusSubscriber(modid = LogTech.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientTooltipHandler {
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.has(ModDataComponents.PURITY.get())) {
            int purity = ModDataComponents.getPurity(stack);
            event.getToolTip().add(Component.translatable("tooltip.logtech.purity", purity).withStyle(ChatFormatting.GRAY));
        }

        if (io.github.chema280101.logtech.block.RackBlockEntity.isDataModule(stack)) {
            int multiplier = io.github.chema280101.logtech.block.RackBlockEntity.getModuleMultiplier(stack);
            int baseHot = 4096;
            int baseCold = 16384;
            try {
                if (io.github.chema280101.logtech.LogTechConfig.DATA_MODULE_CAPACITY != null) {
                    baseHot = io.github.chema280101.logtech.LogTechConfig.DATA_MODULE_CAPACITY.get();
                }
                if (io.github.chema280101.logtech.LogTechConfig.COLD_DATA_MODULE_CAPACITY != null) {
                    baseCold = io.github.chema280101.logtech.LogTechConfig.COLD_DATA_MODULE_CAPACITY.get();
                }
            } catch (Exception ignored) {
            }
            long hotCap = (long) baseHot * multiplier;
            long coldCap = (long) baseCold * multiplier;
            java.text.NumberFormat nf = java.text.NumberFormat.getIntegerInstance();
            event.getToolTip().add(Component.translatable("tooltip.logtech.data_module.capacity", nf.format(hotCap)).withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.translatable("tooltip.logtech.data_module.capacity_cold", nf.format(coldCap)).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
