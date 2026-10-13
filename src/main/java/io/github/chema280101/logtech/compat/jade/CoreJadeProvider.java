package io.github.chema280101.logtech.compat.jade;

import io.github.chema280101.logtech.block.CoreBlockEntity;
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

public enum CoreJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("logtech", "core");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof CoreBlockEntity core) {
            tag.putLong("used_cap", core.getTotalStored());
            tag.putLong("total_cap", core.getCapacity());
            tag.putInt("energy", core.getEnergyStored());
            tag.putInt("max_energy", core.getMaxEnergyStored());

            String netStatus;
            if (!core.isPowered()) {
                netStatus = "offline";
            } else if (core.isOverloaded()) {
                netStatus = "overloaded";
            } else if (core.getOrRecalculateNetwork().isValid()) {
                netStatus = "valid";
            } else {
                netStatus = "offline";
            }
            tag.putString("net_status", netStatus);
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag tag = accessor.getServerData();
        if (tag == null || !tag.contains("net_status")) {
            return;
        }

        // 1. Capacidad usada y total
        long used = tag.getLong("used_cap");
        long total = tag.getLong("total_cap");
        tooltip.add(Component.translatable("gui.logtech.jade.core.storage",
                String.format("%,d", used),
                String.format("%,d", total)).withStyle(ChatFormatting.GRAY));

        // 2. Estado de la red (válida, sobrecargada o apagada)
        String netStatus = tag.getString("net_status");
        ChatFormatting statusColor = switch (netStatus) {
            case "valid" -> ChatFormatting.GREEN;
            case "overloaded" -> ChatFormatting.RED;
            default -> ChatFormatting.DARK_RED;
        };
        Component statusComp = Component.translatable("gui.logtech.jade.core.network_" + netStatus).withStyle(statusColor);
        tooltip.add(Component.translatable("gui.logtech.jade.core.network", statusComp).withStyle(ChatFormatting.GRAY));

        // 3. Energía
        int energy = tag.getInt("energy");
        int maxEnergy = tag.getInt("max_energy");
        tooltip.add(Component.translatable("gui.logtech.jade.energy",
                String.format("%,d", energy),
                String.format("%,d", maxEnergy)).withStyle(ChatFormatting.AQUA));
    }
}
