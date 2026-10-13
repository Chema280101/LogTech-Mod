package io.github.chema280101.logtech.network;

import io.github.chema280101.logtech.machine.ActivityCounter;
import io.github.chema280101.logtech.machine.MachineStatus;
import net.minecraft.network.chat.Component;

/**
 * Interfaz que representa un miembro (máquina) conectado a la red de LogTech.
 * Proporciona el nombre de la máquina, su estado operativo y su contador de actividad/telemetría.
 */
public interface NetworkMember {
    Component getName();

    MachineStatus getStatus();

    ActivityCounter getActivityCounter();

    default Component getMachineName() {
        return getName();
    }

    default net.minecraft.core.BlockPos getBlockPos() {
        return net.minecraft.core.BlockPos.ZERO;
    }

    /**
     * Devuelve la cantidad de energía en FE que la máquina solicita a la red para su búfer interno.
     * Por defecto 0 (máquinas sin consumo o productoras).
     */
    default int requestedEnergy() {
        return 0;
    }

    /**
     * Recibe energía empujada por la red hacia el búfer interno de la máquina.
     *
     * @param amount cantidad de FE ofrecida por la red
     * @return cantidad de FE efectivamente aceptada
     */
    default int receiveNetworkEnergy(int amount) {
        return 0;
    }

    /**
     * Prioridad de energía de la máquina en la red (ALTA, NORMAL, BAJA).
     */
    default EnergyPriority getEnergyPriority() {
        return EnergyPriority.NORMAL;
    }

    default void setEnergyPriority(EnergyPriority priority) {
    }

    /**
     * Indica si la máquina fue apagada/despriorizada en el último ciclo por falta de energía.
     */
    default boolean isEnergyShed() {
        return false;
    }

    default void setEnergyShed(boolean shed) {
    }
}
