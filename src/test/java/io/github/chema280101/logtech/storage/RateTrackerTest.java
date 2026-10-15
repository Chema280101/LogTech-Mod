package io.github.chema280101.logtech.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas unitarias para RateTracker")
class RateTrackerTest {

    @Test
    @DisplayName("Estimaciones de llenado con una Perforadora ingresando mineral")
    void testDrillFillingStorage() {
        RateTracker<String> tracker = new RateTracker<>();

        // Perforadora produce 120 de mineral en 1 minuto (a nivel caliente)
        tracker.recordInsert("iron_ore", 120, RateTracker.Tier.HOT);
        tracker.advanceMinute();

        assertEquals(120, tracker.getItemRate("iron_ore"));
        assertEquals(120, tracker.getHotNetRate());
        assertEquals(0, tracker.getColdNetRate());
        assertEquals(120, tracker.getTotalNetRate());

        // Capacidad restante: 16 080 items
        // 16 080 / 120 = 134 minutos = 2 h 14 min
        long remaining = 16_080L;
        long fillMinutes = RateTracker.calculateFillTimeMinutes(remaining, tracker.getTotalNetRate());
        assertEquals(134L, fillMinutes);
        assertEquals("2 h 14 min", RateTracker.formatDuration(fillMinutes));
        assertEquals("se llena en 2 h 14 min", RateTracker.formatFillEstimate(remaining, tracker.getTotalNetRate()));
    }

    @Test
    @DisplayName("Estimación de agotamiento al consumir un ítem")
    void testItemDepletionEstimate() {
        RateTracker<String> tracker = new RateTracker<>();

        // Se consumen 45 lingotes en 1 minuto
        tracker.recordExtract("iron_ingot", 45, RateTracker.Tier.HOT);
        tracker.advanceMinute();

        assertEquals(-45, tracker.getItemRate("iron_ingot"));
        assertEquals(-45, tracker.getHotNetRate());

        // Quedan 270 lingotes en la red: 270 / 45 = 6 minutos
        long currentAmount = 270L;
        long depletionMins = RateTracker.calculateDepletionTimeMinutes(currentAmount, tracker.getItemRate("iron_ingot"));
        assertEquals(6L, depletionMins);
        assertEquals("6 min", RateTracker.formatDuration(depletionMins));
    }

    @Test
    @DisplayName("Ritmo cero o negativo muestra condición estable")
    void testStableCondition() {
        RateTracker<String> tracker = new RateTracker<>();

        // Minuto sin cambios
        tracker.advanceMinute();
        assertEquals(0, tracker.getTotalNetRate());
        assertEquals(-1L, RateTracker.calculateFillTimeMinutes(1000, tracker.getTotalNetRate()));
        assertEquals("estable", RateTracker.formatFillEstimate(1000, tracker.getTotalNetRate()));

        // Minuto con extracción neta (disminuye en lugar de llenarse)
        tracker.recordExtract("copper_ingot", 100, RateTracker.Tier.HOT);
        tracker.advanceMinute();
        assertTrue(tracker.getTotalNetRate() < 0);
        assertEquals("estable", RateTracker.formatFillEstimate(1000, tracker.getTotalNetRate()));
    }

    @Test
    @DisplayName("Formateo de tiempos de duración")
    void testFormatDuration() {
        assertEquals("< 1 min", RateTracker.formatDuration(0));
        assertEquals("1 min", RateTracker.formatDuration(1));
        assertEquals("6 min", RateTracker.formatDuration(6));
        assertEquals("59 min", RateTracker.formatDuration(59));
        assertEquals("1 h", RateTracker.formatDuration(60));
        assertEquals("1 h 1 min", RateTracker.formatDuration(61));
        assertEquals("2 h 14 min", RateTracker.formatDuration(134));
        assertEquals("1 d", RateTracker.formatDuration(1440));
        assertEquals("1 d 2 h", RateTracker.formatDuration(1560));
    }

    @Test
    @DisplayName("Ventana deslizante de 5 minutos y cálculo de medias")
    void testSlidingWindowFiveMinutes() {
        RateTracker<String> tracker = new RateTracker<>();

        // Minuto 1: +100
        tracker.recordInsert("gold", 100, RateTracker.Tier.HOT);
        tracker.advanceMinute();
        assertEquals(100, tracker.getItemRate("gold"));

        // Minuto 2: +200 (media: (100+200)/2 = 150)
        tracker.recordInsert("gold", 200, RateTracker.Tier.HOT);
        tracker.advanceMinute();
        assertEquals(150, tracker.getItemRate("gold"));

        // Minutos 3, 4, 5: +100 cada uno
        tracker.recordInsert("gold", 100, RateTracker.Tier.HOT);
        tracker.advanceMinute();
        tracker.recordInsert("gold", 100, RateTracker.Tier.HOT);
        tracker.advanceMinute();
        tracker.recordInsert("gold", 100, RateTracker.Tier.HOT);
        tracker.advanceMinute();

        // Total en 5 min: 100 + 200 + 100 + 100 + 100 = 600. Media: 600 / 5 = 120.
        assertEquals(120, tracker.getItemRate("gold"));
        assertEquals(5, tracker.getCompletedMinutes());

        // Minuto 6: 0 inserciones de gold. El Minuto 1 (+100) sale de la ventana.
        // Ventana restante: 200 + 100 + 100 + 100 + 0 = 500. Media: 500 / 5 = 100.
        tracker.advanceMinute();
        assertEquals(100, tracker.getItemRate("gold"));
    }

    @Test
    @DisplayName("Podado automático de ítems inactivos tras 5 minutos sin cambios")
    void testPruningInactiveItems() {
        RateTracker<String> tracker = new RateTracker<>();

        // Minuto 1: se inserta 'rare_gem'
        tracker.recordInsert("rare_gem", 50, RateTracker.Tier.HOT);
        tracker.advanceMinute();
        assertEquals(50, tracker.getItemRate("rare_gem"));
        assertEquals(1, tracker.getTrackedItemCount());

        // Minutos 2 a 5: sin cambios para 'rare_gem'
        for (int i = 0; i < 4; i++) {
            tracker.advanceMinute();
        }
        // Aún en ventana (5 minutos completados), ritmo reducido pero presente
        assertTrue(tracker.getTrackedItemCount() >= 1);

        // Minuto 6: 'rare_gem' sale completamente de la ventana de 5 minutos
        tracker.advanceMinute();
        assertEquals(0, tracker.getItemRate("rare_gem"));
        assertEquals(0, tracker.getTrackedItemCount());
        assertFalse(tracker.getAggregatedRates().containsKey("rare_gem"));
    }

    @Test
    @DisplayName("Cancelación inmediata de ítems con cambio neto cero dentro del minuto")
    void testZeroDeltaWithinMinute() {
        RateTracker<String> tracker = new RateTracker<>();

        tracker.recordInsert("sand", 64, RateTracker.Tier.HOT);
        tracker.recordExtract("sand", 64, RateTracker.Tier.HOT);

        tracker.advanceMinute();
        assertEquals(0, tracker.getItemRate("sand"));
        assertEquals(0, tracker.getTrackedItemCount());
    }

    @Test
    @DisplayName("Segregación por niveles caliente y frío")
    void testTierSegregation() {
        RateTracker<String> tracker = new RateTracker<>();

        tracker.recordInsert("coal", 80, RateTracker.Tier.HOT);
        tracker.recordInsert("stone", 200, RateTracker.Tier.COLD);
        tracker.advanceMinute();

        assertEquals(80, tracker.getHotNetRate());
        assertEquals(200, tracker.getColdNetRate());
        assertEquals(280, tracker.getTotalNetRate());
    }

    @Test
    @DisplayName("Protección contra crecimiento de memoria con miles de ítems")
    void testMemorySafetyLimit() {
        RateTracker<String> tracker = new RateTracker<>();
        tracker.setMaxTrackedItems(500);

        // Intentar registrar 2 000 ítems distintos en un solo minuto
        for (int i = 0; i < 2_000; i++) {
            tracker.recordInsert("item_" + i, 10, RateTracker.Tier.HOT);
        }
        tracker.advanceMinute();

        // La cantidad de ítems rastreados no debe superar el límite fijado
        assertTrue(tracker.getTrackedItemCount() <= 500);
    }
}
