package io.github.chema280101.logtech.machine.recipe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessingLogicTest {

    @Test
    @DisplayName("Cálculo de pureza con modo PRESERVE mantiene la pureza original")
    void testPurityPreserve() {
        for (int p = 1; p <= 5; p++) {
            assertEquals(p, ProcessingLogic.calculateOutputPurity(p, PurityMode.PRESERVE),
                    "La pureza preservada debe ser idéntica a la de entrada: " + p);
        }
    }

    @Test
    @DisplayName("Cálculo de pureza con modo INCREASE incrementa en 1 con tope en 5")
    void testPurityIncrease() {
        assertEquals(2, ProcessingLogic.calculateOutputPurity(1, PurityMode.INCREASE));
        assertEquals(3, ProcessingLogic.calculateOutputPurity(2, PurityMode.INCREASE));
        assertEquals(4, ProcessingLogic.calculateOutputPurity(3, PurityMode.INCREASE));
        assertEquals(5, ProcessingLogic.calculateOutputPurity(4, PurityMode.INCREASE));
        assertEquals(5, ProcessingLogic.calculateOutputPurity(5, PurityMode.INCREASE), "El tope máximo debe ser 5");
    }

    @Test
    @DisplayName("Cálculo de pureza clampea valores fuera de rango")
    void testPurityClamping() {
        assertEquals(1, ProcessingLogic.calculateOutputPurity(0, PurityMode.PRESERVE), "Valores <= 0 deben normalizarse a 1");
        assertEquals(1, ProcessingLogic.calculateOutputPurity(-5, PurityMode.PRESERVE), "Valores negativos deben normalizarse a 1");
        assertEquals(5, ProcessingLogic.calculateOutputPurity(10, PurityMode.PRESERVE), "Valores > 5 deben clampearse a 5");
        assertEquals(5, ProcessingLogic.calculateOutputPurity(6, PurityMode.INCREASE), "Valores > 5 con INCREASE deben topar en 5");
    }

    @Test
    @DisplayName("Escalado NONE mantiene la probabilidad base independientemente de la pureza")
    void testScalingNone() {
        float baseChance = 0.75f;
        for (int p = 1; p <= 5; p++) {
            assertEquals(baseChance, ProcessingLogic.calculateEffectiveChance(baseChance, OutputScaling.NONE, p), 0.0001f,
                    "Con escalado NONE la probabilidad no debe variar con la pureza");
        }
    }

    @Test
    @DisplayName("Escalado INVERSE_PURITY decrece monótonamente conforme sube la pureza")
    void testScalingInversePurity() {
        float baseChance = 1.0f;
        float c1 = ProcessingLogic.calculateEffectiveChance(baseChance, OutputScaling.INVERSE_PURITY, 1);
        float c2 = ProcessingLogic.calculateEffectiveChance(baseChance, OutputScaling.INVERSE_PURITY, 2);
        float c3 = ProcessingLogic.calculateEffectiveChance(baseChance, OutputScaling.INVERSE_PURITY, 3);
        float c4 = ProcessingLogic.calculateEffectiveChance(baseChance, OutputScaling.INVERSE_PURITY, 4);
        float c5 = ProcessingLogic.calculateEffectiveChance(baseChance, OutputScaling.INVERSE_PURITY, 5);

        assertEquals(1.0f, c1, 0.0001f, "Pureza 1 debe dar 100% de la probabilidad base");
        assertEquals(0.8f, c2, 0.0001f, "Pureza 2 debe dar 80% de la probabilidad base");
        assertEquals(0.6f, c3, 0.0001f, "Pureza 3 debe dar 60% de la probabilidad base");
        assertEquals(0.4f, c4, 0.0001f, "Pureza 4 debe dar 40% de la probabilidad base");
        assertEquals(0.2f, c5, 0.0001f, "Pureza 5 debe dar 20% de la probabilidad base");

        assertTrue(c1 > c2 && c2 > c3 && c3 > c4 && c4 > c5, "Debe ser estrictamente decreciente");
    }

    @Test
    @DisplayName("Determinación de tirada aleatoria (shouldProduce)")
    void testShouldProduce() {
        assertTrue(ProcessingLogic.shouldProduce(1.0f, 0.5));
        assertTrue(ProcessingLogic.shouldProduce(1.0f, 0.999));
        assertFalse(ProcessingLogic.shouldProduce(0.0f, 0.1));

        assertTrue(ProcessingLogic.shouldProduce(0.5f, 0.3));
        assertFalse(ProcessingLogic.shouldProduce(0.5f, 0.7));
    }
}
