package io.github.chema280101.logtech.machine.firmware;

import java.util.Objects;

/**
 * Clase Java pura desacoplada de Minecraft para calcular los efectos de los chips de firmware
 * en el rendimiento, consumo energético, comportamiento térmico y automatización de las máquinas.
 */
public final class FirmwareEffects {
    public static final double DEFAULT_ECO_SPEED = 0.7;
    public static final double DEFAULT_ECO_ENERGY = 0.5;
    public static final double DEFAULT_ECO_HEAT = 0.5;

    public static final double DEFAULT_TURBO_SPEED = 1.5;
    public static final double DEFAULT_TURBO_ENERGY = 2.0;
    public static final double DEFAULT_TURBO_HEAT = 3.0;

    public static final double DEFAULT_SAFE_OVERHEAT_THRESHOLD = 85.0;
    public static final double DEFAULT_SAFE_RESUME_THRESHOLD = 30.0;

    private FirmwareEffects() {
    }

    /**
     * Calcula el multiplicador de velocidad según el firmware instalado.
     */
    public static double getSpeedMultiplier(FirmwareType type, double ecoSpeed, double turboSpeed) {
        if (type == null) {
            return 1.0;
        }
        return switch (type) {
            case ECO -> ecoSpeed;
            case TURBO -> turboSpeed;
            case SAFE, SELECTIVE, NONE -> 1.0;
        };
    }

    public static double getSpeedMultiplier(FirmwareType type) {
        return getSpeedMultiplier(type, DEFAULT_ECO_SPEED, DEFAULT_TURBO_SPEED);
    }

    /**
     * Calcula el multiplicador de energía por tick según el firmware instalado.
     */
    public static double getEnergyMultiplier(FirmwareType type, double ecoEnergy, double turboEnergy) {
        if (type == null) {
            return 1.0;
        }
        return switch (type) {
            case ECO -> ecoEnergy;
            case TURBO -> turboEnergy;
            case SAFE, SELECTIVE, NONE -> 1.0;
        };
    }

    public static double getEnergyMultiplier(FirmwareType type) {
        return getEnergyMultiplier(type, DEFAULT_ECO_ENERGY, DEFAULT_TURBO_ENERGY);
    }

    /**
     * Calcula el multiplicador de generación de calor por segundo según el firmware instalado.
     */
    public static double getHeatMultiplier(FirmwareType type, double ecoHeat, double turboHeat) {
        if (type == null) {
            return 1.0;
        }
        return switch (type) {
            case ECO -> ecoHeat;
            case TURBO -> turboHeat;
            case SAFE, SELECTIVE, NONE -> 1.0;
        };
    }

    public static double getHeatMultiplier(FirmwareType type) {
        return getHeatMultiplier(type, DEFAULT_ECO_HEAT, DEFAULT_TURBO_HEAT);
    }

    /**
     * Obtiene el umbral de sobrecalentamiento térmico (apagado) para el modelo de calor.
     */
    public static double getOverheatThreshold(FirmwareType type, double normalThreshold, double safeThreshold) {
        return type == FirmwareType.SAFE ? safeThreshold : normalThreshold;
    }

    public static double getOverheatThreshold(FirmwareType type, double normalThreshold) {
        return getOverheatThreshold(type, normalThreshold, DEFAULT_SAFE_OVERHEAT_THRESHOLD);
    }

    /**
     * Obtiene el umbral de reanudación térmica tras sobrecalentamiento.
     */
    public static double getResumeThreshold(FirmwareType type, double normalThreshold, double safeThreshold) {
        return type == FirmwareType.SAFE ? safeThreshold : normalThreshold;
    }

    public static double getResumeThreshold(FirmwareType type, double normalThreshold) {
        return getResumeThreshold(type, normalThreshold, DEFAULT_SAFE_RESUME_THRESHOLD);
    }

    /**
     * Obtiene la velocidad de ralentización térmica. Para Seguro, nunca se ralentiza (1.0x).
     */
    public static double getSlowdownSpeed(FirmwareType type, double normalSlowdownSpeed) {
        return type == FirmwareType.SAFE ? 1.0 : normalSlowdownSpeed;
    }

    /**
     * Obtiene el umbral de ralentización térmica. Para Seguro, iguala al umbral de apagado para nunca operar ralentizado.
     */
    public static double getSlowdownThreshold(FirmwareType type, double normalThreshold, double safeThreshold) {
        return type == FirmwareType.SAFE ? safeThreshold : normalThreshold;
    }

    /**
     * Calcula la velocidad efectiva combinando el multiplicador térmico y el del firmware.
     */
    public static double calculateEffectiveSpeed(FirmwareType type, double heatSpeedMultiplier, double ecoSpeed, double turboSpeed) {
        if (heatSpeedMultiplier <= 0.0) {
            return 0.0;
        }
        return heatSpeedMultiplier * getSpeedMultiplier(type, ecoSpeed, turboSpeed);
    }

    /**
     * Calcula el coste energético ajustado por el firmware.
     */
    public static int calculateEnergyCost(int baseCost, FirmwareType type, double ecoEnergy, double turboEnergy) {
        if (baseCost <= 0) {
            return 0;
        }
        double mult = getEnergyMultiplier(type, ecoEnergy, turboEnergy);
        return (int) Math.max(1, Math.round(baseCost * mult));
    }

    /**
     * Calcula la energía demandada por la máquina en la red (requestedEnergy T33), reflejando
     * el multiplicador de energía y el estado térmico.
     */
    public static int calculateRequestedEnergy(int deficit, double heatSpeedMultiplier, FirmwareType type, double ecoEnergy, double turboEnergy) {
        if (deficit <= 0 || heatSpeedMultiplier <= 0.0) {
            return 0;
        }
        double energyMult = getEnergyMultiplier(type, ecoEnergy, turboEnergy);
        return (int) Math.round(deficit * heatSpeedMultiplier * energyMult);
    }

    /**
     * Valida si un ítem puede ser extraído mediante automatización externa (tolvas, tuberías).
     * Con firmware selectivo, solo permite extraer el ítem configurado en el chip.
     * En caso de firmware sin filtro configurado o cualquier otro tipo, permite la extracción normal.
     */
    public static boolean canExtractAutomated(FirmwareType type, Object filterTarget, Object itemToExtract) {
        if (type != FirmwareType.SELECTIVE || filterTarget == null) {
            return true;
        }
        if (itemToExtract == null) {
            return false;
        }
        return Objects.equals(filterTarget, itemToExtract);
    }
}
