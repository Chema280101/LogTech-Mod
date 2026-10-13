package io.github.chema280101.logtech.storage;

import io.github.chema280101.logtech.LogTechConfig;
import net.neoforged.neoforge.energy.EnergyStorage;

import java.util.function.Consumer;

public class CoreEnergyStorage extends EnergyStorage {
    private final Runnable onChanged;
    private final Consumer<Integer> onReceived;

    public CoreEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChanged) {
        this(capacity, maxReceive, maxExtract, onChanged, null);
    }

    public CoreEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChanged, Consumer<Integer> onReceived) {
        super(capacity, maxReceive, maxExtract);
        this.onChanged = onChanged;
        this.onReceived = onReceived;
    }

    @Override
    public int getMaxEnergyStored() {
        try {
            if (LogTechConfig.CORE_ENERGY_CAPACITY != null) {
                return LogTechConfig.CORE_ENERGY_CAPACITY.get();
            }
        } catch (Exception ignored) {
        }
        return this.capacity;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        if (!canReceive() || toReceive <= 0) {
            return 0;
        }
        int maxCap = getMaxEnergyStored();
        int space = Math.max(0, maxCap - this.energy);
        int energyReceived = Math.min(space, Math.min(this.maxReceive, toReceive));
        if (!simulate && energyReceived > 0) {
            this.energy += energyReceived;
            if (onReceived != null) {
                onReceived.accept(energyReceived);
            }
            if (onChanged != null) {
                onChanged.run();
            }
        }
        return energyReceived;
    }

    public boolean consume(int toConsume) {
        if (toConsume <= 0) {
            return true;
        }
        if (this.energy >= toConsume) {
            this.energy -= toConsume;
            if (onChanged != null) {
                onChanged.run();
            }
            return true;
        }
        return false;
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(getMaxEnergyStored(), energy));
        if (onChanged != null) {
            onChanged.run();
        }
    }
}
