package io.github.chema280101.logtech.storage;

import net.neoforged.neoforge.energy.EnergyStorage;

public class CoreEnergyStorage extends EnergyStorage {
    private final Runnable onChanged;

    public CoreEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChanged) {
        super(capacity, maxReceive, maxExtract);
        this.onChanged = onChanged;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int received = super.receiveEnergy(toReceive, simulate);
        if (received > 0 && !simulate && onChanged != null) {
            onChanged.run();
        }
        return received;
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
        this.energy = Math.max(0, Math.min(this.capacity, energy));
        if (onChanged != null) {
            onChanged.run();
        }
    }
}
