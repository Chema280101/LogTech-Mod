package io.github.chema280101.logtech.machine;

import io.github.chema280101.logtech.network.EnergyPriority;

/**
 * Interfaz para menús de máquinas con botón de configuración de prioridad de energía.
 */
public interface PriorityConfigurableMenu {
    EnergyPriority getEnergyPriority();
    void setEnergyPriority(EnergyPriority priority);
}
