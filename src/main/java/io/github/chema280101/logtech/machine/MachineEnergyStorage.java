package io.github.chema280101.logtech.machine;

import net.neoforged.neoforge.energy.EnergyStorage;

public class MachineEnergyStorage extends EnergyStorage {
    private final Runnable onChanged;

    public MachineEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChanged) {
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

    /**
     * Recibe energía proveniente de la red de LogTech directamente al búfer interno,
     * aceptando hasta la capacidad máxima disponible (sin limitarse por maxReceive de cables externos).
     */
    public int receiveNetworkEnergy(int toReceive) {
        if (toReceive <= 0) {
            return 0;
        }
        int space = this.capacity - this.energy;
        int accepted = Math.min(toReceive, space);
        if (accepted > 0) {
            this.energy += accepted;
            if (onChanged != null) {
                onChanged.run();
            }
        }
        return accepted;
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
