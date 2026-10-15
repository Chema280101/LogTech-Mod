package io.github.chema280101.logtech.machine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DissipationLogicTest {

    @Test
    @DisplayName("Cálculo de enfriamiento base sin disipadores ni agua")
    void testCalculateCoolingBaseOnly() {
        double cooling = DissipationLogic.calculateCooling(2.0, 0, 0, 1.0, 0.5);
        assertEquals(2.0, cooling, 0.001);
    }

    @Test
    @DisplayName("Cálculo de enfriamiento con disipadores (heatsinks)")
    void testCalculateCoolingWithHeatsinks() {
        // 1 disipador (+1.0)
        double c1 = DissipationLogic.calculateCooling(2.0, 1, 0, 1.0, 0.5);
        assertEquals(3.0, c1, 0.001);

        // 2 disipadores (+2.0)
        double c2 = DissipationLogic.calculateCooling(2.0, 2, 0, 1.0, 0.5);
        assertEquals(4.0, c2, 0.001);

        // 6 disipadores (+6.0)
        double c6 = DissipationLogic.calculateCooling(2.0, 6, 0, 1.0, 0.5);
        assertEquals(8.0, c6, 0.001);

        // Acotamiento al límite físico de 6 caras del cubo
        double cClamped = DissipationLogic.calculateCooling(2.0, 10, 0, 1.0, 0.5);
        assertEquals(8.0, cClamped, 0.001);
    }

    @Test
    @DisplayName("Cálculo de enfriamiento con caras en contacto con agua")
    void testCalculateCoolingWithWater() {
        // 1 cara con agua (+0.5)
        double w1 = DissipationLogic.calculateCooling(2.0, 0, 1, 1.0, 0.5);
        assertEquals(2.5, w1, 0.001);

        // 4 caras con agua (+2.0) -> equivalente a 2 disipadores
        double w4 = DissipationLogic.calculateCooling(2.0, 0, 4, 1.0, 0.5);
        assertEquals(4.0, w4, 0.001);
    }

    @Test
    @DisplayName("Cálculo de enfriamiento combinado (disipadores + agua)")
    void testCalculateCoolingCombined() {
        // 1 disipador (+1.0) y 2 caras con agua (+1.0) -> +2.0 extra
        double combined = DissipationLogic.calculateCooling(2.0, 1, 2, 1.0, 0.5);
        assertEquals(4.0, combined, 0.001);
    }

    @Test
    @DisplayName("Balance: máquina con Turbo sin refrigeración se sobrecalienta en ~1 minuto")
    void testTurboWithoutRefrigerationOverheatsInOneMinute() {
        double baseGen = 2.0;
        double turboMultiplier = 2.2; // Generación turbo 4.4 /s
        double baseCooling = 2.0;

        // Desde equilibrio de trabajo normal (50 °C)
        int secondsFrom50 = DissipationLogic.simulateTimeToOverheat(50.0, baseGen, turboMultiplier, baseCooling, 120);
        assertTrue(secondsFrom50 >= 55 && secondsFrom50 <= 65,
                "Desde equilibrio (50 °C) sin disipadores debe sobrecalentarse en aproximadamente un minuto (55-65 s), fue: " + secondsFrom50);
        assertEquals(57, secondsFrom50, "En paso discreto se sobrecalienta en 57 segundos");

        // Desde máquina fría apagada (0 °C)
        int secondsFrom0 = DissipationLogic.simulateTimeToOverheat(0.0, baseGen, turboMultiplier, baseCooling, 120);
        assertTrue(secondsFrom0 >= 70 && secondsFrom0 <= 75,
                "Desde frío (0 °C) sin disipadores debe tardar ~72 segundos en sobrecalentarse, fue: " + secondsFrom0);
    }

    @Test
    @DisplayName("Balance: máquina con Turbo y 2 disipadores aguanta indefinidamente")
    void testTurboWithTwoHeatsinksHoldsIndefinitely() {
        double baseGen = 2.0;
        double turboMultiplier = 2.2;
        // Base 2.0 + 2 disipadores (2 * 1.0) = 4.0
        double coolingWith2Heatsinks = DissipationLogic.calculateCooling(2.0, 2, 0, 1.0, 0.5);
        assertEquals(4.0, coolingWith2Heatsinks, 0.001);

        HeatModel model = new HeatModel(50.0, false);

        // Simular 10 000 segundos de trabajo ininterrumpido con Turbo
        for (int s = 0; s < 10_000; s++) {
            model.tick(true, baseGen, turboMultiplier, coolingWith2Heatsinks);
            assertFalse(model.isOverheated(), "No debe sobrecalentarse en ningún momento con 2 disipadores");
            assertTrue(model.getHeat() < 70.0,
                    "La temperatura no debe superar el umbral de ralentización (70 °C), fue: " + model.getHeat());
        }

        // La temperatura final debe converger exactamente al equilibrio calculado
        double expectedEquilibrium = DissipationLogic.calculateEquilibriumHeat(baseGen, turboMultiplier, coolingWith2Heatsinks);
        assertEquals(50.6, expectedEquilibrium, 0.001);
        assertEquals(expectedEquilibrium, model.getHeat(), 0.001,
                "La temperatura debe estabilizarse en ~50.6 °C indefinidamente");
        assertEquals(1.0, model.getSpeedMultiplier(), 0.001, "Debe trabajar a velocidad máxima (1.0x)");
    }

    @Test
    @DisplayName("Balance: 4 caras con agua también permiten aguantar indefinidamente con Turbo")
    void testTurboWithFourWaterSidesHoldsIndefinitely() {
        double baseGen = 2.0;
        double turboMultiplier = 2.2;
        // Base 2.0 + 4 caras de agua (4 * 0.5) = 4.0
        double coolingWithWater = DissipationLogic.calculateCooling(2.0, 0, 4, 1.0, 0.5);
        assertEquals(4.0, coolingWithWater, 0.001);

        HeatModel model = new HeatModel(50.0, false);
        for (int s = 0; s < 1_000; s++) {
            model.tick(true, baseGen, turboMultiplier, coolingWithWater);
            assertFalse(model.isOverheated());
            assertTrue(model.getHeat() < 70.0);
        }
        double expectedEquilibrium = DissipationLogic.calculateEquilibriumHeat(baseGen, turboMultiplier, coolingWithWater);
        assertEquals(expectedEquilibrium, model.getHeat(), 0.001);
    }
}
