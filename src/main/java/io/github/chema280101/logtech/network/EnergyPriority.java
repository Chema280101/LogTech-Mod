package io.github.chema280101.logtech.network;

import java.util.Locale;

/**
 * Prioridad de red para el reparto de energía.
 * ALTA (HIGH), NORMAL (NORMAL), BAJA (LOW).
 */
public enum EnergyPriority {
    HIGH(0, "gui.logtech.energy_priority.high"),
    NORMAL(1, "gui.logtech.energy_priority.normal"),
    LOW(2, "gui.logtech.energy_priority.low");

    private final int level;
    private final String translationKey;

    EnergyPriority(int level, String translationKey) {
        this.level = level;
        this.translationKey = translationKey;
    }

    public int getLevel() {
        return this.level;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    /**
     * Cicla la prioridad en orden: NORMAL -> HIGH -> LOW -> NORMAL.
     */
    public EnergyPriority next() {
        return switch (this) {
            case NORMAL -> HIGH;
            case HIGH -> LOW;
            case LOW -> NORMAL;
        };
    }

    public static EnergyPriority fromOrdinal(int ordinal) {
        EnergyPriority[] values = values();
        if (ordinal >= 0 && ordinal < values.length) {
            return values[ordinal];
        }
        return NORMAL;
    }

    public static EnergyPriority fromName(String name) {
        if (name == null || name.isEmpty()) {
            return NORMAL;
        }
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}
