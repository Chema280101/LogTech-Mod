package io.github.chema280101.logtech.machine;

/**
 * Lógica pura de cálculo de disipación y enfriamiento térmico de máquinas.
 * Desacoplada de dependencias de Minecraft para permitir pruebas unitarias aisladas.
 */
public class DissipationLogic {

    /**
     * Calcula la tasa total de enfriamiento / disipación térmica por segundo.
     *
     * @param baseCooling   tasa de disipación base de la máquina (por defecto 2.0)
     * @param heatsinkCount cantidad de bloques disipadores (heatsinks) en contacto directo (0 a 6)
     * @param waterCount    cantidad de caras en contacto con agua (0 a 6)
     * @param heatsinkRate  enfriamiento aportado por cada disipador (por defecto 1.0)
     * @param waterRate     enfriamiento aportado por cada cara con agua (por defecto 0.5)
     * @return tasa total de enfriamiento por segundo
     */
    public static double calculateCooling(
            double baseCooling,
            int heatsinkCount,
            int waterCount,
            double heatsinkRate,
            double waterRate
    ) {
        int clampedHeatsinks = Math.clamp(heatsinkCount, 0, 6);
        int clampedWater = Math.clamp(waterCount, 0, 6);
        double total = baseCooling + (clampedHeatsinks * heatsinkRate) + (clampedWater * waterRate);
        return Math.max(0.0, total);
    }

    /**
     * Calcula la temperatura de equilibrio asintótico (°C) bajo funcionamiento continuo.
     * En equilibrio térmico: Generación = Disipación(H_eq)
     * donde Disipación(H) = coolingRate * (H / 50.0).
     *
     * @param generationRate       tasa base de calor generado por segundo
     * @param generationMultiplier multiplicador de generación (ej. 1.0 normal, 2.1 turbo)
     * @param coolingRate          tasa total de enfriamiento por segundo
     * @return temperatura de equilibrio en °C
     */
    public static double calculateEquilibriumHeat(double generationRate, double generationMultiplier, double coolingRate) {
        if (coolingRate <= 0.0) {
            return HeatModel.DEFAULT_OVERHEAT_THRESHOLD;
        }
        double genTotal = generationRate * generationMultiplier;
        double continuousEq = (genTotal * HeatModel.DEFAULT_NORMAL_EQUILIBRIUM_HEAT) / coolingRate;
        return Math.max(0.0, continuousEq - genTotal);
    }

    /**
     * Simula el tiempo en segundos (ticks de 1 segundo) necesario para alcanzar
     * el sobrecalentamiento desde una temperatura inicial.
     *
     * @return segundos transcurridos hasta sobrecalentarse, o -1 si no se sobrecalienta (estable)
     */
    public static int simulateTimeToOverheat(
            double initialHeat,
            double generationRate,
            double generationMultiplier,
            double coolingRate,
            int maxSeconds
    ) {
        HeatModel model = new HeatModel(initialHeat, false);
        for (int s = 1; s <= maxSeconds; s++) {
            model.tick(true, generationRate, generationMultiplier, coolingRate);
            if (model.isOverheated()) {
                return s;
            }
        }
        return -1;
    }
}
