package io.github.chema280101.logtech.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class QuarantineBufferTest {

    private QuarantineBuffer<String> buffer;

    @BeforeEach
    void setUp() {
        buffer = new QuarantineBuffer<>();
    }

    @Test
    @DisplayName("Inserción básica respeta cantidades y acumula por tipo de ítem")
    void testBasicInsertion() {
        long capacity = 1024;
        long insertedIron = buffer.insert("iron", 64, false, capacity);
        long insertedCopper = buffer.insert("copper", 32, false, capacity);
        long insertedMoreIron = buffer.insert("iron", 36, false, capacity);

        assertEquals(64L, insertedIron);
        assertEquals(32L, insertedCopper);
        assertEquals(36L, insertedMoreIron);
        assertEquals(100L, buffer.getAmount("iron"));
        assertEquals(32L, buffer.getAmount("copper"));
        assertEquals(132L, buffer.getTotalCount());
        assertEquals(2, buffer.size());
    }

    @Test
    @DisplayName("Simulate=true respeta el espacio disponible pero no modifica el búfer de cuarentena")
    void testSimulateDoesNotMutateBuffer() {
        long capacity = 1024;
        long simulated = buffer.insert("iron", 100, true, capacity);

        assertEquals(100L, simulated, "Debe simular que acepta los 100 ítems");
        assertEquals(0L, buffer.getTotalCount(), "El total almacenado debe permanecer en 0 tras simulate");
        assertEquals(0L, buffer.getAmount("iron"), "La cantidad del ítem debe permanecer en 0 tras simulate");
        assertTrue(buffer.isEmpty(), "El búfer debe seguir vacío tras simulate");
    }

    @Test
    @DisplayName("Límite de capacidad: cuando se llena a 1024 ítems, rechaza inserciones adicionales")
    void testCapacityEnforcementAndRejection() {
        long capacity = 1024;

        // Llenar casi por completo (1000 ítems)
        long ins1 = buffer.insert("iron", 1000, false, capacity);
        assertEquals(1000L, ins1);
        assertEquals(1000L, buffer.getTotalCount());

        // Intentar insertar 50 ítems: solo deben caber 24
        long insSim = buffer.insert("gold", 50, true, capacity);
        assertEquals(24L, insSim, "Simulate debe reportar exactamente el espacio restante (24)");
        assertEquals(1000L, buffer.getTotalCount(), "Simulate no debe haber modificado el búfer");

        long ins2 = buffer.insert("gold", 50, false, capacity);
        assertEquals(24L, ins2, "Solo deben insertarse 24 ítems para alcanzar 1024");
        assertEquals(1024L, buffer.getTotalCount(), "Búfer completamente lleno");

        // Búfer lleno: cualquier intento adicional debe devolver 0 (rechazado)
        long rejectedSim = buffer.insert("diamond", 10, true, capacity);
        long rejectedReal = buffer.insert("diamond", 10, false, capacity);
        assertEquals(0L, rejectedSim, "Simulate en búfer lleno debe rechazar (0)");
        assertEquals(0L, rejectedReal, "Inserción en búfer lleno debe rechazar (0)");
        assertEquals(1024L, buffer.getTotalCount());
    }

    @Test
    @DisplayName("Vaciado con límite por ciclo hacia el destino conserva ítems exactamente y sin duplicación")
    void testDrainToSinkWithCycleLimit() {
        long capacity = 1024;
        buffer.insert("iron", 100, false, capacity);
        buffer.insert("copper", 50, false, capacity);

        AtomicLong sinkStored = new AtomicLong(0);

        // Ciclo 1: límite de 64 ítems por ciclo
        long drained1 = buffer.drainTo((item, amount) -> {
            sinkStored.addAndGet(amount);
            return amount;
        }, 64);

        assertEquals(64L, drained1, "Debe drenar exactamente 64 ítems en el primer ciclo");
        assertEquals(36L, buffer.getAmount("iron"), "Deben quedar 36 de hierro en cuarentena");
        assertEquals(50L, buffer.getAmount("copper"), "Deben quedar 50 de cobre en cuarentena");
        assertEquals(86L, buffer.getTotalCount());
        assertEquals(64L, sinkStored.get());
        assertEquals(150L, buffer.getTotalCount() + sinkStored.get(), "Conservación matemática estricta");

        // Ciclo 2: límite de 64 ítems. Quedan 36 de hierro y luego pasa a cobre
        long drained2 = buffer.drainTo((item, amount) -> {
            sinkStored.addAndGet(amount);
            return amount;
        }, 64);

        assertEquals(64L, drained2, "Debe drenar 36 de hierro + 28 de cobre = 64 ítems");
        assertEquals(0L, buffer.getAmount("iron"), "Hierro completamente vaciado");
        assertEquals(22L, buffer.getAmount("copper"), "Deben quedar 22 de cobre (50 - 28)");
        assertEquals(22L, buffer.getTotalCount());
        assertEquals(128L, sinkStored.get());
        assertEquals(150L, buffer.getTotalCount() + sinkStored.get());

        // Ciclo 3: se vacían los 22 ítems restantes
        long drained3 = buffer.drainTo((item, amount) -> {
            sinkStored.addAndGet(amount);
            return amount;
        }, 64);

        assertEquals(22L, drained3);
        assertTrue(buffer.isEmpty(), "Cuarentena debe quedar completamente vacía");
        assertEquals(150L, sinkStored.get(), "El destino debe contener exactamente los 150 ítems originales");
    }

    @Test
    @DisplayName("Vaciado parcial cuando el destino se llena: cuarentena retiene el sobrante sin pérdidas")
    void testDrainWhenSinkBecomesFull() {
        long capacity = 1024;
        buffer.insert("raw_neoferrite", 100, false, capacity);

        // El destino solo tiene espacio para 40 ítems
        AtomicLong networkStorage = new AtomicLong(0);
        long maxNetworkSpace = 40;

        long drained = buffer.drainTo((item, amount) -> {
            long current = networkStorage.get();
            long space = Math.max(0, maxNetworkSpace - current);
            long accepted = Math.min(amount, space);
            networkStorage.addAndGet(accepted);
            return accepted;
        }, 64);

        assertEquals(40L, drained, "El vaciado solo debe transferir lo que la red realmente pudo aceptar (40)");
        assertEquals(60L, buffer.getAmount("raw_neoferrite"), "Cuarentena debe retener los 60 ítems no aceptados");
        assertEquals(40L, networkStorage.get(), "La red debe haber recibido exactamente 40 ítems");
        assertEquals(100L, buffer.getTotalCount() + networkStorage.get(), "Cero ítems perdidos ni duplicados");
    }

    @Test
    @DisplayName("Extracción manual respeta simulación y cantidades existentes")
    void testExtractManual() {
        long capacity = 1024;
        buffer.insert("iron", 50, false, capacity);

        long simExtract = buffer.extract("iron", 20, true);
        assertEquals(20L, simExtract);
        assertEquals(50L, buffer.getAmount("iron"), "Simulate no debe descontar del búfer");

        long realExtract = buffer.extract("iron", 20, false);
        assertEquals(20L, realExtract);
        assertEquals(30L, buffer.getAmount("iron"));

        long overExtract = buffer.extract("iron", 100, false);
        assertEquals(30L, overExtract, "No puede extraer más de lo que hay");
        assertEquals(0L, buffer.getAmount("iron"));
        assertTrue(buffer.isEmpty());
    }
}
