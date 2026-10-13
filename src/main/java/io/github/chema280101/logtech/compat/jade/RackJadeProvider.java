package io.github.chema280101.logtech.compat.jade;

import io.github.chema280101.logtech.block.RackBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

public enum RackJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("logtech", "rack");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof RackBlockEntity rack) {
            tag.putString("tier", rack.getTier().name().toLowerCase(Locale.ROOT));
            tag.putInt("modules", rack.getModuleCount());
            tag.putLong("capacity", rack.getCapacity());
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag tag = accessor.getServerData();
        if (tag == null || !tag.contains("tier")) {
            return;
        }

        // 1. Nivel: Caliente o Frío
        String tier = tag.getString("tier");
        ChatFormatting tierColor = "cold".equals(tier) ? ChatFormatting.AQUA : ChatFormatting.GOLD;
        Component tierComp = Component.translatable("gui.logtech.jade.rack.tier_" + tier).withStyle(tierColor);
        tooltip.add(Component.translatable("gui.logtech.jade.rack.tier", tierComp).withStyle(ChatFormatting.GRAY));

        // 2. Módulos
        int modules = tag.getInt("modules");
        tooltip.add(Component.translatable("gui.logtech.jade.rack.modules", modules, 4).withStyle(ChatFormatting.GRAY));

        // 3. Capacidad
        long capacity = tag.getLong("capacity");
        tooltip.add(Component.translatable("gui.logtech.jade.rack.capacity",
                String.format("%,d", capacity)).withStyle(ChatFormatting.GRAY));
    }
}
