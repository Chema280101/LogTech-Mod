package io.github.chema280101.logtech.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrashBinTest {

    private TrashBin<String> trashBin;

    @BeforeEach
    void setUp() {
        trashBin = new TrashBin<>();
    }

    @Test
    @DisplayName("Añadir ítems los agrupa por tipo de ítem y calcula el conteo total")
    void testAddAndGroupItems() {
        trashBin.add("iron", 64, 1000L, 4096);
        trashBin.add("copper", 32, 1050L, 4096);
        trashBin.add("iron", 36, 1100L, 4096);

        assertEquals(100L, trashBin.getAmount("iron"));
        assertEquals(32L, trashBin.getAmount("copper"));
        assertEquals(132L, trashBin.getTotalCount());
        assertEquals(2, trashBin.size());
        assertEquals(1100L, trashBin.getEntry("iron").gameTime());
    }

    @Test
    @DisplayName("Restaurar ítems devuelve la cantidad solicitada y reduce la papelera")
    void testRestoreItems() {
        trashBin.add("gold", 64, 500L, 4096);

        long restored1 = trashBin.restore("gold", 20);
        assertEquals(20L, restored1);
        assertEquals(44L, trashBin.getAmount("gold"));

        long restored2 = trashBin.restore("gold", 50);
        assertEquals(44L, restored2);
        assertEquals(0L, trashBin.getAmount("gold"));
        assertTrue(trashBin.isEmpty());
    }

    @Test
    @DisplayName("Capacidad máxima: cuando se llena, elimina los ítems más antiguos (menor gameTime)")
    void testCapacityEnforcementDropsOldest() {
        long capacity = 4096;

        // Añadir 3000 de hierro en tick 100
        trashBin.add("iron", 3000, 100L, capacity);
        // Añadir 1000 de oro en tick 200
        trashBin.add("gold", 1000, 200L, capacity);
        assertEquals(4000L, trashBin.getTotalCount());

        // Añadir 200 de diamante en tick 300 (total pasaría a 4200, excede por 104)
        trashBin.add("diamond", 200, 300L, capacity);

        assertEquals(capacity, trashBin.getTotalCount());
        // El hierro es el más antiguo (tick 100): debe haber perdido 104 ítems -> 2896
        assertEquals(2896L, trashBin.getAmount("iron"));
        assertEquals(1000L, trashBin.getAmount("gold"));
        assertEquals(200L, trashBin.getAmount("diamond"));
    }

    @Test
    @DisplayName("Capacidad máxima: si el exceso supera la entrada más antigua, la elimina y descuenta de la siguiente")
    void testCapacityEnforcementRemovesMultipleOldEntries() {
        long capacity = 100;

        trashBin.add("itemA", 40, 10L, capacity);
        trashBin.add("itemB", 50, 20L, capacity);
        trashBin.add("itemC", 10, 30L, capacity);
        assertEquals(100L, trashBin.getTotalCount());

        // Añadir 60 de itemD en tick 40 (exceso de 60)
        // itemA (40) debe desaparecer completamente (quedan 20 de exceso)
        // itemB (50) debe reducirse en 20 -> queda en 30
        trashBin.add("itemD", 60, 40L, capacity);

        assertEquals(100L, trashBin.getTotalCount());
        assertEquals(0L, trashBin.getAmount("itemA"));
        assertNull(trashBin.getEntry("itemA"));
        assertEquals(30L, trashBin.getAmount("itemB"));
        assertEquals(10L, trashBin.getAmount("itemC"));
        assertEquals(60L, trashBin.getAmount("itemD"));
    }

    @Test
    @DisplayName("Caducidad: pruneExpired elimina las entradas que superen el tiempo de vida en ticks")
    void testPruneExpiredEntries() {
        long lifetime = 36000L;

        trashBin.add("apple", 10, 1000L, 4096);
        trashBin.add("bread", 20, 10000L, 4096);
        trashBin.add("cake", 5, 30000L, 4096);

        // A tick 35000: antigüedad de apple es 34000 < 36000. Ninguno expira.
        int pruned1 = trashBin.pruneExpired(35000L, lifetime);
        assertEquals(0, pruned1);
        assertEquals(3, trashBin.size());

        // A tick 37001: antigüedad de apple es 36001 >= 36000 -> expira.
        // bread (antigüedad 27001) y cake (antigüedad 7001) sobreviven.
        int pruned2 = trashBin.pruneExpired(37001L, lifetime);
        assertEquals(1, pruned2);
        assertEquals(0L, trashBin.getAmount("apple"));
        assertEquals(20L, trashBin.getAmount("bread"));
        assertEquals(5L, trashBin.getAmount("cake"));

        // A tick 70000: tanto bread como cake han superado los 36000 ticks.
        int pruned3 = trashBin.pruneExpired(70000L, lifetime);
        assertEquals(2, pruned3);
        assertTrue(trashBin.isEmpty());
    }

    @Test
    @DisplayName("Restaurar con parámetros inválidos o vacíos retorna 0 de forma segura")
    void testRestoreEdgeCases() {
        assertEquals(0L, trashBin.restore(null, 10));
        assertEquals(0L, trashBin.restore("unknown", 10));
        assertEquals(0L, trashBin.restore("unknown", 0));
        assertEquals(0L, trashBin.restore("unknown", -5));
    }
}
