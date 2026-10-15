package io.github.chema280101.logtech.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompactionLogicTest {

    @Test
    @DisplayName("Criterio: 5 000 lingotes de hierro y regla «mayor que 1 000 → compactar» convierte el excedente")
    void testCompactionIronIngotCriterion() {
        long current = 5000L;
        long threshold = 1000L;
        long excess = current - threshold; // 4000
        int ratio = 9;
        long maxConversions = 1000L;

        CompactionLogic.ConversionPlan plan = CompactionLogic.planCompaction(excess, ratio, maxConversions);

        // 4000 / 9 = 444 conversiones completas
        assertEquals(444L, plan.conversions());
        assertEquals(3996L, plan.baseItemsToExtract());
        assertEquals(444L, plan.compactItemsToProduce());

        long remaining = current - plan.baseItemsToExtract();
        assertEquals(1004L, remaining, "Quedan 1004 lingotes (1000 umbral + 4 que no llegan a formar otro bloque)");
    }

    @Test
    @DisplayName("Criterio: 5 005 lingotes con umbral 1 000 deja exactamente 1 000 lingotes")
    void testCompactionExactMultipleLeavesExactThreshold() {
        long current = 5005L;
        long threshold = 1000L;
        long excess = current - threshold; // 4005
        int ratio = 9;
        long maxConversions = 1000L;

        CompactionLogic.ConversionPlan plan = CompactionLogic.planCompaction(excess, ratio, maxConversions);

        // 4005 / 9 = 445 conversiones exactas
        assertEquals(445L, plan.conversions());
        assertEquals(4005L, plan.baseItemsToExtract());
        assertEquals(445L, plan.compactItemsToProduce());

        long remaining = current - plan.baseItemsToExtract();
        assertEquals(1000L, remaining, "Con múltiplo exacto de 9 en el excedente, quedan exactamente 1 000 lingotes");
    }

    @Test
    @DisplayName("Límite de conversiones por ciclo respeta maxConversions")
    void testCompactionRespectsMaxConversionsPerCycle() {
        long excess = 4000L;
        int ratio = 9;
        long maxConversions = 64L; // Valor por defecto en configuración

        CompactionLogic.ConversionPlan plan = CompactionLogic.planCompaction(excess, ratio, maxConversions);

        assertEquals(64L, plan.conversions());
        assertEquals(576L, plan.baseItemsToExtract()); // 64 * 9
        assertEquals(64L, plan.compactItemsToProduce());
    }

    @Test
    @DisplayName("Ratio 2x2 (ratio = 4) convierte correctamente")
    void testCompactionRatio4() {
        long excess = 83L;
        int ratio = 4;
        long maxConversions = 100L;

        CompactionLogic.ConversionPlan plan = CompactionLogic.planCompaction(excess, ratio, maxConversions);

        assertEquals(20L, plan.conversions());
        assertEquals(80L, plan.baseItemsToExtract());
        assertEquals(20L, plan.compactItemsToProduce());
    }

    @Test
    @DisplayName("Sin excedente o excedente inferior al ratio no realiza conversiones")
    void testNoCompactionWhenBelowOrInsufficientExcess() {
        // Excedente 0 o negativo
        assertEquals(CompactionLogic.ConversionPlan.ZERO, CompactionLogic.planCompaction(0L, 9, 64L));
        assertEquals(CompactionLogic.ConversionPlan.ZERO, CompactionLogic.planCompaction(-10L, 9, 64L));

        // Excedente menor que el ratio (8 < 9)
        assertEquals(CompactionLogic.ConversionPlan.ZERO, CompactionLogic.planCompaction(8L, 9, 64L));

        // maxConversions = 0
        assertEquals(CompactionLogic.ConversionPlan.ZERO, CompactionLogic.planCompaction(100L, 9, 0L));
    }
}
