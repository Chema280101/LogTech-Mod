package io.github.chema280101.logtech.machine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeatModelTest {

    @Test
    @DisplayName("Estado inicial: calor en 0, no sobrecalentada y velocidad al 100%")
    void testInitialState() {
        HeatModel model = new HeatModel();
        assertEquals(0.0, model.getHeat(), 0.001);
        assertFalse(model.isOverheated());
        assertEquals(1.0, model.getSpeedMultiplier(), 0.001);
    }

    @Test
    @DisplayName("Operación normal con generación base: calor se estabiliza por debajo de 70 y nunca se sobrecalienta")
    void testNormalOperationEquilibrium() {
        HeatModel model = new HeatModel();
        double baseGen = 2.0;
        double multiplier = 1.0;
        double cooling = 2.0;

        // Simular 100 ciclos de trabajo (100 segundos de operación continua)
        for (int i = 0; i < 100; i++) {
            model.tick(true, baseGen, multiplier, cooling);
            assertFalse(model.isOverheated(), "Con generación base nunca debe sobrecalentarse");
            assertTrue(model.getHeat() <= 70.0, "La temperatura debe estabilizarse por debajo de 70");
            assertEquals(1.0, model.getSpeedMultiplier(), 0.001, "La máquina debe mantenerse al 100% de velocidad");
        }

        // El calor debe haber subido y estar en equilibrio alrededor de 50 °C ("el calor existe")
        assertTrue(model.getHeat() >= 45.0 && model.getHeat() <= 55.0,
                "El calor en equilibrio debe rondar los 50 °C, actual: " + model.getHeat());
    }

    @Test
    @DisplayName("Sobrecalentamiento con generación elevada: subida, ralentización a 70 y apagado a 100")
    void testOverheatingFlow() {
        HeatModel model = new HeatModel();
        double highGen = 8.0; // Generación elevada (subida en config o Turbo)
        double multiplier = 1.0;
        double cooling = 2.0;

        // Calentar hasta pasar de 70
        while (model.getHeat() <= 70.0) {
            model.tick(true, highGen, multiplier, cooling);
        }

        // Al superar 70, la velocidad debe reducirse a la mitad (0.5x)
        assertTrue(model.getHeat() > 70.0);
        assertFalse(model.isOverheated());
        assertEquals(0.5, model.getSpeedMultiplier(), 0.001, "Por encima de 70 debe trabajar a media velocidad");

        // Continuar calentando hasta 100
        while (!model.isOverheated()) {
            model.tick(true, highGen, multiplier, cooling);
        }

        // Al llegar a 100 se apaga por completo
        assertEquals(100.0, model.getHeat(), 0.001);
        assertTrue(model.isOverheated(), "Al llegar a 100 debe marcarse como sobrecalentada");
        assertEquals(0.0, model.getSpeedMultiplier(), 0.001, "Una máquina sobrecalentada tiene velocidad 0");
    }

    @Test
    @DisplayName("Histéresis: una vez sobrecalentada, permanece apagada hasta descender a <= 40 °C")
    void testHysteresisAndResume() {
        HeatModel model = new HeatModel();
        model.setHeat(100.0);
        model.setOverheated(true);

        double cooling = 4.0;

        // Se apaga y comienza a enfriarse (working = false)
        while (model.getHeat() > 40.0) {
            model.tick(false, 0.0, 1.0, cooling);
            if (model.getHeat() > 40.0) {
                assertTrue(model.isOverheated(),
                        "A temperatura " + model.getHeat() + " (> 40) debe seguir apagada por histéresis");
                assertEquals(0.0, model.getSpeedMultiplier(), 0.001);
            }
        }

        // Una vez que baja a 40 o menos, la sobrecalentamiento se desactiva
        assertTrue(model.getHeat() <= 40.0);
        assertFalse(model.isOverheated(), "Al descender a <= 40 debe reanudarse automáticamente");
        assertEquals(1.0, model.getSpeedMultiplier(), 0.001, "Al reanudarse a 40 °C opera a velocidad completa");
    }

    @Test
    @DisplayName("Enfriamiento hasta 0 en reposo")
    void testIdleCoolingToZero() {
        HeatModel model = new HeatModel();
        model.setHeat(35.0);

        for (int i = 0; i < 50; i++) {
            model.tick(false, 0.0, 1.0, 2.0);
        }

        assertEquals(0.0, model.getHeat(), 0.001, "La máquina en reposo debe enfriarse completamente a 0");
    }

    @Test
    @DisplayName("Acotamiento: el calor nunca desciende de 0 ni supera 100")
    void testClamping() {
        HeatModel model = new HeatModel();
        model.tick(false, 0.0, 1.0, 10.0);
        assertEquals(0.0, model.getHeat(), 0.001);

        model.setHeat(150.0);
        assertEquals(100.0, model.getHeat(), 0.001);

        model.setHeat(-50.0);
        assertEquals(0.0, model.getHeat(), 0.001);
    }
}
