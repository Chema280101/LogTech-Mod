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
}
