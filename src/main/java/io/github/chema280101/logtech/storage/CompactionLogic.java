package io.github.chema280101.logtech.storage;

/**
 * Lógica matemática de conversión de compactación en Java puro desacoplada de Minecraft.
 */
public final class CompactionLogic {

    private CompactionLogic() {}

    public record ConversionPlan(long conversions, long baseItemsToExtract, long compactItemsToProduce) {
        public static final ConversionPlan ZERO = new ConversionPlan(0L, 0L, 0L);
    }

    /**
     * Calcula el plan de conversión para un excedente por encima del umbral.
     *
     * @param excess cantidad que supera el umbral (debe ser > 0)
     * @param ratio ratio de la receta (4 o 9)
     * @param maxConversions límite de conversiones por ciclo
     * @return plan con conversiones a realizar y cantidades
     */
    public static ConversionPlan planCompaction(long excess, int ratio, long maxConversions) {
        if (excess <= 0 || ratio <= 0 || maxConversions <= 0) {
            return ConversionPlan.ZERO;
        }
        long possibleConversions = excess / ratio;
        long conversionsToExecute = Math.min(possibleConversions, maxConversions);
        if (conversionsToExecute <= 0) {
            return ConversionPlan.ZERO;
        }
        long toExtract = conversionsToExecute * ratio;
        long toProduce = conversionsToExecute;
        return new ConversionPlan(conversionsToExecute, toExtract, toProduce);
    }
}
