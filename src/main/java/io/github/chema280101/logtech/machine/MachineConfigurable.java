package io.github.chema280101.logtech.machine;

import io.github.chema280101.logtech.block.ConfigTypes;
import io.github.chema280101.logtech.block.Configurable;
import io.github.chema280101.logtech.network.EnergyPriority;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Interfaz que especializa {@link Configurable} para máquinas de LogTech.
 * En las máquinas, la llave de configuración copia y pega la prioridad de energía.
 */
public interface MachineConfigurable extends Configurable {

    @Override
    default String getConfigType() {
        return ConfigTypes.MACHINE;
    }

    EnergyPriority getEnergyPriority();

    void setEnergyPriority(EnergyPriority priority);

    @Override
    default CompoundTag exportConfig(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        EnergyPriority priority = getEnergyPriority();
        tag.putString("energy_priority", priority != null ? priority.name() : EnergyPriority.NORMAL.name());
        return tag;
    }

    @Override
    default boolean importConfig(CompoundTag tag, HolderLookup.Provider registries, @Nullable Player player) {
        if (tag == null || !tag.contains("energy_priority")) {
            return false;
        }
        String name = tag.getString("energy_priority");
        try {
            EnergyPriority priority = EnergyPriority.valueOf(name);
            setEnergyPriority(priority);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
