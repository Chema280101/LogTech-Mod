package io.github.chema280101.logtech.machine;

import net.minecraft.network.chat.Component;
import java.util.Locale;

public enum MachineStatus {
    WORKING,
    NO_ENERGY,
    NO_INPUT,
    OUTPUT_FULL,
    NO_RESOURCE,
    NO_FLUID,
    IDLE,
    SHED;

    public String getStatusKey() {
        return "gui.logtech.machine_status." + this.name().toLowerCase(Locale.ROOT);
    }

    public String getCauseKey() {
        return "gui.logtech.machine_status.cause." + this.name().toLowerCase(Locale.ROOT);
    }

    public Component getStatusComponent() {
        return Component.translatable(getStatusKey());
    }

    public Component getCauseComponent() {
        return Component.translatable(getCauseKey());
    }

    public Component getDisplayName() {
        return getStatusComponent();
    }

    public boolean isActive() {
        return this == WORKING;
    }
}
