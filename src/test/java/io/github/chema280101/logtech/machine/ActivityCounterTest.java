package io.github.chema280101.logtech.machine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActivityCounterTest {

    @Test
    @DisplayName("Cambio de minuto: al alcanzar 1200 ticks avanza al siguiente cubo y preserva el anterior")
    void testMinuteChange() {
        ActivityCounter counter = new ActivityCounter();

        counter.recordItemIn(10);
        counter.recordItemOut(5);
        counter.advanceTicks(800, true);
        counter.advanceTicks(400, false);

        // Al alcanzar exactamente 1200 ticks, debió ocurrir el cambio de minuto
        assertEquals(0, counter.getCurrentTickInMinute(), "El contador de ticks del minuto actual debe reiniciarse a 0");
        assertEquals(1, counter.getMinutesRecorded(), "Debe haberse registrado 1 minuto completado");

        // El cubo del minuto recién completado debe contener los datos registrados
        ActivityCounter.MinuteBucket completedBucket = counter.getBucket(0);
        assertEquals(10, completedBucket.getItemsIn());
        assertEquals(5, completedBucket.getItemsOut());
        assertEquals(800, completedBucket.getActiveTicks());
        assertEquals(400, completedBucket.getInactiveTicks());

        // El resumen del último minuto también debe coincidir
        ActivityCounter.ActivitySummary summary1 = counter.getSummary(1);
        assertEquals(10, summary1.itemsIn());
        assertEquals(5, summary1.itemsOut());
        assertEquals(800, summary1.activeTicks());
        assertEquals(400, summary1.inactiveTicks());

        // Ahora iniciamos actividad en el nuevo minuto
        counter.recordItemIn(2);
        counter.recordItemOut(3);
        counter.advanceTicks(100, true);

        // El cubo actual en curso tiene 2 items in, 3 out y 100 ticks activos
        ActivityCounter.MinuteBucket currentBucket = counter.getCurrentBucket();
        assertEquals(2, currentBucket.getItemsIn());
        assertEquals(3, currentBucket.getItemsOut());
        assertEquals(100, currentBucket.getActiveTicks());

        // El cubo histórico anterior (getBucket(1)) sigue teniendo los datos del minuto 0
        ActivityCounter.MinuteBucket previousBucket = counter.getBucket(1);
        assertEquals(10, previousBucket.getItemsIn());
        assertEquals(5, previousBucket.getItemsOut());
        assertEquals(800, previousBucket.getActiveTicks());

        // La suma de los últimos 2 minutos acumula el minuto en curso y el anterior
        ActivityCounter.ActivitySummary summary2 = counter.getSummary(2);
        assertEquals(12, summary2.itemsIn(), "2 + 10 = 12 items in");
        assertEquals(8, summary2.itemsOut(), "3 + 5 = 8 items out");
        assertEquals(900, summary2.activeTicks(), "100 + 800 = 900 active ticks");
    }

    @Test
    @DisplayName("Vuelta del búfer tras 60 minutos: los datos más antiguos se sobrescriben en el búfer circular")
    void testBufferWrapAroundAfter60Minutes() {
        ActivityCounter counter = new ActivityCounter();

        // Minuto 0: registramos valores distintivos
        counter.recordItemIn(99);
        counter.recordItemOut(49);
        counter.advanceTicks(1200, true); // completa minuto 0

        // Avanzamos 60 minutos más (cada uno con 1 item in, 1 item out)
        for (int m = 0; m < 60; m++) {
            counter.recordItemIn(1);
            counter.recordItemOut(1);
            if (m < 59) {
                counter.advanceTicks(1200, true);
            } else {
                counter.advanceTicks(1200, true);
            }
        }

        // Se han completado 61 minutos en total; los minutos registrados alcanzan el tope de 60
        assertEquals(60, counter.getMinutesRecorded(), "Los minutos registrados no deben superar la capacidad del búfer (60)");

        // El minuto 0 (con 99 items in) debió haber sido sobrescrito por la vuelta del búfer
        ActivityCounter.ActivitySummary summary59 = counter.getSummary(59);
        assertEquals(59, summary59.itemsIn(), "Los 59 minutos activos deben contener 1 item cada uno");
        assertEquals(59, summary59.itemsOut());

        ActivityCounter.ActivitySummary summary60 = counter.getSummary(60);
        // Debe confirmar que los 99 items ya no existen en el búfer circular
        assertFalse(summary60.itemsIn() >= 99, "Los 99 items iniciales deben haber sido expulsados del búfer circular");
    }

    @Test
    @DisplayName("Suma de los últimos N minutos: acumula correctamente múltiples cubos del búfer")
    void testSumLastNMinutes() {
        ActivityCounter counter = new ActivityCounter();

        // Minuto 0
        counter.recordItemIn(10);
        counter.recordItemOut(5);
        counter.advanceTicks(600, true);
        counter.advanceTicks(600, false);

        // Minuto 1
        counter.recordItemIn(20);
        counter.recordItemOut(15);
        counter.advanceTicks(1200, true);

        // Minuto 2
        counter.recordItemIn(30);
        counter.recordItemOut(25);
        counter.advanceTicks(900, true);
        counter.advanceTicks(300, false);

        // Consulta del último 1 minuto (minuto 2)
        ActivityCounter.ActivitySummary sum1 = counter.getSummary(1);
        assertEquals(30, sum1.itemsIn());
        assertEquals(25, sum1.itemsOut());
        assertEquals(900, sum1.activeTicks());
        assertEquals(300, sum1.inactiveTicks());

        // Consulta de los últimos 2 minutos (minuto 2 + minuto 1)
        ActivityCounter.ActivitySummary sum2 = counter.getSummary(2);
        assertEquals(50, sum2.itemsIn(), "30 + 20 = 50 items in");
        assertEquals(40, sum2.itemsOut(), "25 + 15 = 40 items out");
        assertEquals(2100, sum2.activeTicks(), "900 + 1200 = 2100 active ticks");
        assertEquals(300, sum2.inactiveTicks(), "300 + 0 = 300 inactive ticks");

        // Consulta de los últimos 3 minutos (minuto 2 + minuto 1 + minuto 0)
        ActivityCounter.ActivitySummary sum3 = counter.getSummary(3);
        assertEquals(60, sum3.itemsIn(), "30 + 20 + 10 = 60 items in");
        assertEquals(45, sum3.itemsOut(), "25 + 15 + 5 = 45 items out");
        assertEquals(2700, sum3.activeTicks(), "900 + 1200 + 600 = 2700 active ticks");
        assertEquals(900, sum3.inactiveTicks(), "300 + 0 + 600 = 900 inactive ticks");

        // Métodos de conveniencia individuales
        assertEquals(60, counter.getItemsIn(3));
        assertEquals(45, counter.getItemsOut(3));
        assertEquals(2700, counter.getActiveTicks(3));
        assertEquals(900, counter.getInactiveTicks(3));

        // Consulta con N mayor a los minutos disponibles
        assertEquals(60, counter.getItemsIn(10));

        // Consulta con N <= 0
        assertEquals(0, counter.getItemsIn(0));
        assertEquals(ActivityCounter.ActivitySummary.EMPTY, counter.getSummary(0));
    }

    @Test
    @DisplayName("Un minuto sin actividad: 0 items, 0 ticks activos y 1200 ticks inactivos")
    void testMinuteWithoutActivity() {
        ActivityCounter counter = new ActivityCounter();

        // 1200 ticks inactivos (equivalente a 1 minuto sin hacer nada)
        for (int i = 0; i < 1200; i++) {
            counter.tick(false);
        }

        assertEquals(1, counter.getMinutesRecorded());

        // El minuto transcurrido debe tener exactamente 0 actividad
        ActivityCounter.ActivitySummary pastMinute = counter.getSummary(1);
        assertEquals(0, pastMinute.itemsIn());
        assertEquals(0, pastMinute.itemsOut());
        assertEquals(0, pastMinute.activeTicks());
        assertEquals(1200, pastMinute.inactiveTicks());
        assertEquals(1200, pastMinute.totalTicks());
        assertEquals(0.0, pastMinute.getActivePercentage(), 0.0001);
    }

    @Test
    @DisplayName("Actualización por tiempo de juego (update con game ticks)")
    void testGameTimeUpdate() {
        ActivityCounter counter = new ActivityCounter();

        // Simula actualización de tiempo de juego
        counter.update(100L, true);   // Inicia observación en tick 100
        counter.update(700L, true);   // 600 ticks activos (100 a 700)
        counter.update(1300L, false); // 600 ticks inactivos (700 a 1300)

        // Se completó exactamente 1 minuto (1200 ticks)
        ActivityCounter.MinuteBucket bucket = counter.getBucket(0);
        assertEquals(600, bucket.getActiveTicks());
        assertEquals(600, bucket.getInactiveTicks());
        assertEquals(0, bucket.getItemsIn());
        assertEquals(0, bucket.getItemsOut());
    }

    @Test
    @DisplayName("Telemetría del Núcleo: inserciones por minuto, media de 5 minutos y arreglo de 60 minutos para gráfica")
    void testCoreTelemetryStats() {
        ActivityCounter counter = new ActivityCounter();

        // Minuto 0: 50 items insertados
        counter.recordItemIn(50);
        counter.advanceTicks(1200, true);

        // Minuto 1: 30 items
        counter.recordItemIn(30);
        counter.advanceTicks(1200, true);

        // Minuto 2: 20 items
        counter.recordItemIn(20);
        counter.advanceTicks(1200, true);

        // Minuto 3: 40 items
        counter.recordItemIn(40);
        counter.advanceTicks(1200, true);

        // Minuto 4: 60 items
        counter.recordItemIn(60);
        counter.advanceTicks(1200, true);

        // Último minuto completado (minuto 4)
        assertEquals(60, counter.getItemsIn(1));

        // Media de 5 minutos: (50 + 30 + 20 + 40 + 60) / 5 = 200 / 5 = 40.0
        float avg5 = counter.getItemsIn(5) / 5.0f;
        assertEquals(40.0f, avg5, 0.001f);

        // Arreglo de 60 minutos para la gráfica (desde 59 minutos atrás hasta el más reciente)
        int[] hourlyHistory = new int[60];
        for (int i = 0; i < 60; i++) {
            hourlyHistory[i] = counter.getBucket(59 - i).getItemsIn();
        }

        // El cubo más reciente (índice 59) debe tener 60 items
        assertEquals(60, hourlyHistory[59]);
        // El anterior (índice 58) 40 items
        assertEquals(40, hourlyHistory[58]);
        // Índice 57 -> 20 items
        assertEquals(20, hourlyHistory[57]);
        // Índice 56 -> 30 items
        assertEquals(30, hourlyHistory[56]);
        // Índice 55 -> 50 items
        assertEquals(50, hourlyHistory[55]);
        // Minutos anteriores sin actividad -> 0
        assertEquals(0, hourlyHistory[0]);
    }
}
