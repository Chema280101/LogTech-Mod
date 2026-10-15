package io.github.chema280101.logtech.compat.jade;

import io.github.chema280101.logtech.LogTechConfig;
import io.github.chema280101.logtech.machine.CentrifugeBlockEntity;
import io.github.chema280101.logtech.machine.CrusherBlockEntity;
import io.github.chema280101.logtech.machine.DrillBlockEntity;
import io.github.chema280101.logtech.machine.MachineStatus;
import io.github.chema280101.logtech.machine.WasherBlockEntity;
import io.github.chema280101.logtech.network.EnergyPriority;
import io.github.chema280101.logtech.ore.DepositSavedData;
import io.github.chema280101.logtech.ore.DepositVein;
import io.github.chema280101.logtech.ore.VeinGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.List;

public enum MachineJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("logtech", "machine");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();
        if (be instanceof CrusherBlockEntity crusher) {
            tag.putString("machine_type", "crusher");
            appendBaseData(tag, crusher.getStatus(), crusher.getProgress(), crusher.getMaxProgress(),
                    crusher.getEnergyStorage().getEnergyStored(), crusher.getEnergyStorage().getMaxEnergyStored(),
                    crusher.getEnergyPriority(), crusher.getHeat(), crusher.getFirmwareType());
            tag.putDouble("cooling", crusher.getCachedCooling());
        } else if (be instanceof WasherBlockEntity washer) {
            tag.putString("machine_type", "washer");
            appendBaseData(tag, washer.getStatus(), washer.getProgress(), washer.getMaxProgress(),
                    washer.getEnergyStorage().getEnergyStored(), washer.getEnergyStorage().getMaxEnergyStored(),
                    washer.getEnergyPriority(), washer.getHeat(), washer.getFirmwareType());
            tag.putDouble("cooling", washer.getCachedCooling());
            tag.putInt("water", washer.getFluidTank().getFluidAmount());
            tag.putInt("max_water", washer.getFluidTank().getCapacity());
        } else if (be instanceof CentrifugeBlockEntity centrifuge) {
            tag.putString("machine_type", "centrifuge");
            appendBaseData(tag, centrifuge.getStatus(), centrifuge.getProgress(), centrifuge.getMaxProgress(),
                    centrifuge.getEnergyStorage().getEnergyStored(), centrifuge.getEnergyStorage().getMaxEnergyStored(),
                    centrifuge.getEnergyPriority(), centrifuge.getHeat(), centrifuge.getFirmwareType());
            tag.putDouble("cooling", centrifuge.getCachedCooling());
        } else if (be instanceof DrillBlockEntity drill) {
            tag.putString("machine_type", "drill");
            int maxProgress = LogTechConfig.DRILL_PROCESS_TIME.get();
            appendBaseData(tag, drill.getStatus(), drill.getProgress(), maxProgress,
                    drill.getEnergyStorage().getEnergyStored(), drill.getEnergyStorage().getMaxEnergyStored(),
                    drill.getEnergyPriority(), drill.getHeat(), drill.getFirmwareType());
            tag.putDouble("cooling", drill.getCachedCooling());

            if (accessor.getLevel() instanceof ServerLevel serverLevel) {
                DepositSavedData savedData = DepositSavedData.get(serverLevel);
                List<DepositVein> veins = VeinGenerator.getVeinsAt(serverLevel.getSeed(), accessor.getPosition().getX(), accessor.getPosition().getZ());
                DepositVein targetVein = null;
                for (DepositVein vein : veins) {
                    if (savedData.isDiscovered(vein)) {
                        targetVein = vein;
                        break;
                    }
                }

                if (targetVein != null) {
                    tag.putInt("vein_purity", targetVein.getPurity());
                    boolean infinite = LogTechConfig.INFINITE_RESERVES.get();
                    tag.putBoolean("infinite", infinite);
                    tag.putInt("vein_reserve", savedData.getRemainingReserve(targetVein));
                } else {
                    tag.putInt("vein_purity", 0);
                    tag.putInt("vein_reserve", 0);
                }
            }
        }
    }

    private void appendBaseData(CompoundTag tag, MachineStatus status, int progress, int maxProgress,
                                int energy, int maxEnergy, EnergyPriority priority, double heat,
                                io.github.chema280101.logtech.machine.firmware.FirmwareType firmware) {
        tag.putString("status_cause", status.getCauseKey());
        tag.putInt("progress", progress);
        tag.putInt("max_progress", maxProgress);
        tag.putInt("energy", energy);
        tag.putInt("max_energy", maxEnergy);
        tag.putString("priority_key", priority.getTranslationKey());
        tag.putInt("heat", (int) Math.round(heat));
        if (firmware != null && firmware != io.github.chema280101.logtech.machine.firmware.FirmwareType.NONE) {
            tag.putString("firmware", firmware.getId());
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag tag = accessor.getServerData();
        if (tag == null || !tag.contains("machine_type")) {
            return;
        }

        // 1. Estado con su causa
        if (tag.contains("status_cause")) {
            String causeKey = tag.getString("status_cause");
            tooltip.add(Component.translatable("gui.logtech.jade.status",
                    Component.translatable(causeKey).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
        }

        // 2. Progreso
        int maxProgress = tag.getInt("max_progress");
        int progress = tag.getInt("progress");
        if (maxProgress > 0) {
            int pct = Math.clamp((int) ((long) progress * 100 / maxProgress), 0, 100);
            tooltip.add(Component.translatable("gui.logtech.jade.progress", pct).withStyle(ChatFormatting.GRAY));
        }

        // 3. Energía
        int energy = tag.getInt("energy");
        int maxEnergy = tag.getInt("max_energy");
        tooltip.add(Component.translatable("gui.logtech.jade.energy",
                String.format("%,d", energy),
                String.format("%,d", maxEnergy)).withStyle(ChatFormatting.AQUA));

        // 4. Calor
        if (tag.contains("heat")) {
            int heat = tag.getInt("heat");
            ChatFormatting heatColor = (heat >= 100) ? ChatFormatting.RED : (heat > 70 ? ChatFormatting.GOLD : ChatFormatting.GREEN);
            tooltip.add(Component.translatable("gui.logtech.jade.heat", heat).withStyle(heatColor));
        }

        // 4b. Enfriamiento / Disipación
        if (tag.contains("cooling")) {
            double cooling = tag.getDouble("cooling");
            tooltip.add(Component.translatable("gui.logtech.jade.cooling", String.format(java.util.Locale.ROOT, "%.1f", cooling)).withStyle(ChatFormatting.AQUA));
        }

        // 5. Prioridad
        if (tag.contains("priority_key")) {
            String priorityKey = tag.getString("priority_key");
            tooltip.add(Component.translatable("gui.logtech.jade.priority",
                    Component.translatable(priorityKey).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY));
        }

        // 5b. Firmware
        if (tag.contains("firmware")) {
            String fwId = tag.getString("firmware");
            tooltip.add(Component.translatable("gui.logtech.jade.firmware",
                    Component.translatable("item.logtech.firmware_" + fwId)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        // 6. Lavadora: Agua
        if (tag.contains("water")) {
            int water = tag.getInt("water");
            int maxWater = tag.getInt("max_water");
            tooltip.add(Component.translatable("gui.logtech.jade.water",
                    String.format("%,d", water),
                    String.format("%,d", maxWater)).withStyle(ChatFormatting.BLUE));
        }

        // 6. Perforadora: Pureza y reserva de la veta
        if ("drill".equals(tag.getString("machine_type"))) {
            int purity = tag.getInt("vein_purity");
            if (purity > 0) {
                tooltip.add(Component.translatable("gui.logtech.jade.vein_purity", purity).withStyle(ChatFormatting.GREEN));
                if (tag.getBoolean("infinite")) {
                    tooltip.add(Component.translatable("gui.logtech.jade.vein_reserve",
                            Component.translatable("gui.logtech.jade.vein_reserve_infinite")).withStyle(ChatFormatting.GOLD));
                } else {
                    int reserve = tag.getInt("vein_reserve");
                    tooltip.add(Component.translatable("gui.logtech.jade.vein_reserve",
                            String.format("%,d", reserve)).withStyle(ChatFormatting.GOLD));
                }
            } else {
                tooltip.add(Component.translatable("gui.logtech.jade.no_vein").withStyle(ChatFormatting.RED));
            }
        }
    }
}
