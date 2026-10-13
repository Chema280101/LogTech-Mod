package io.github.chema280101.logtech.machine.recipe;

public final class ProcessingLogic {

    private ProcessingLogic() {
    }

    /**
     * Calcula la pureza resultante según el modo de pureza y la pureza de entrada.
     * La pureza de entrada se normaliza entre 1 y 5 (1 por defecto).
     *
     * @param inputPurity Pureza de la materia prima de entrada.
     * @param mode        Modo de pureza (PRESERVE o INCREASE).
     * @return Pureza calculada en el rango [1, 5].
     */
    public static int calculateOutputPurity(int inputPurity, PurityMode mode) {
        int clampedInput = clampPurity(inputPurity);
        if (mode == null) {
            return clampedInput;
        }
        return switch (mode) {
            case PRESERVE -> clampedInput;
            case INCREASE -> Math.min(5, clampedInput + 1);
        };
    }

    /**
     * Calcula la probabilidad efectiva para una salida considerando su escalado y la pureza de entrada.
     * En modo INVERSE_PURITY, a mayor pureza la probabilidad de subproducto disminuye linealmente:
     * - Pureza 1: 100% de baseChance
     * - Pureza 2: 80% de baseChance
     * - Pureza 3: 60% de baseChance
     * - Pureza 4: 40% de baseChance
     * - Pureza 5: 20% de baseChance
     *
     * @param baseChance Probabilidad base definida en la receta [0.0, 1.0].
     * @param scaling    Tipo de escalado (NONE o INVERSE_PURITY).
     * @param purity     Pureza del ítem de entrada.
     * @return Probabilidad efectiva en [0.0, 1.0].
     */
    public static float calculateEffectiveChance(float baseChance, OutputScaling scaling, int purity) {
        if (baseChance <= 0.0f) {
            return 0.0f;
        }
        if (baseChance >= 1.0f && scaling == OutputScaling.NONE) {
            return 1.0f;
        }
        int clampedPurity = clampPurity(purity);
        if (scaling == OutputScaling.INVERSE_PURITY) {
            float factor = (6 - clampedPurity) / 5.0f;
            return Math.max(0.0f, Math.min(1.0f, baseChance * factor));
        }
        return Math.max(0.0f, Math.min(1.0f, baseChance));
    }

    /**
     * Determina si una salida probabilística se produce dado un número aleatorio uniforme en [0.0, 1.0).
     */
    public static boolean shouldProduce(float effectiveChance, double randomRoll) {
        if (effectiveChance >= 1.0f) {
            return true;
        }
        if (effectiveChance <= 0.0f) {
            return false;
        }
        return randomRoll < effectiveChance;
    }

    /**
     * Clampea la pureza al rango válido [1, 5]. Si el valor es menor o igual a 0, asume 1 por defecto.
     */
    public static int clampPurity(int purity) {
        if (purity <= 0) {
            return 1;
        }
        return Math.min(5, purity);
    }
}
