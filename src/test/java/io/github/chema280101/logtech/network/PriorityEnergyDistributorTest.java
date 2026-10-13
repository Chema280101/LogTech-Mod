package io.github.chema280101.logtech.network;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PriorityEnergyDistributorTest {

    @Test
    @DisplayName("Criterio T34: Tres máquinas con prioridades distintas y un generador insuficiente: ALTA trabaja y BAJA se detiene. Al añadir otro generador, BAJA vuelve.")
    void testThreeMachinesDifferentPrioritiesWithInsufficientEnergy() {
        PriorityEnergyDistributor<String> distributor = new PriorityEnergyDistributor<>(3);

        String machineHigh = "Crusher_Alta";
        String machineNormal = "Centrifuge_Normal";
        String machineLow = "Washer_Baja";

        // Ciclo 1: 1 generador insuficiente (800 FE disponibles tras el Núcleo)
        // Cada máquina pide 800 FE (demanda total 2400 FE)
        Map<String, PriorityEnergyDistributor.Request<String>> reqs = new LinkedHashMap<>();
        reqs.put(machineHigh, new PriorityEnergyDistributor.Request<>(machineHigh, 800, EnergyPriority.HIGH));
        reqs.put(machineNormal, new PriorityEnergyDistributor.Request<>(machineNormal, 800, EnergyPriority.NORMAL));
        reqs.put(machineLow, new PriorityEnergyDistributor.Request<>(machineLow, 800, EnergyPriority.LOW));

        PriorityEnergyDistributor.DistributionResult<String> res1 = distributor.distribute(reqs, 800);

        // ALTA trabaja recibiendo 800 FE completos
        assertEquals(800, res1.getAllocated(machineHigh));
        assertFalse(res1.isShed(machineHigh));

        // NORMAL y BAJA se quedan sin energía por prioridad y pasan a SHED
        assertEquals(0, res1.getAllocated(machineNormal));
        assertTrue(res1.isShed(machineNormal));

        assertEquals(0, res1.getAllocated(machineLow));
        assertTrue(res1.isShed(machineLow));
        assertEquals(800, res1.totalDistributed());

        // Ciclos 2 y 3: Se mantiene la asignación estable durante los ciclos de hold
        for (int c = 2; c <= 3; c++) {
            PriorityEnergyDistributor.DistributionResult<String> resHold = distributor.distribute(reqs, 800);
            assertEquals(800, resHold.getAllocated(machineHigh));
            assertFalse(resHold.isShed(machineHigh));
            assertEquals(0, resHold.getAllocated(machineLow));
            assertTrue(resHold.isShed(machineLow));
        }

        // Ciclo 4: Se añade otro generador (ahora 2400 FE disponibles, cubre toda la demanda)
        PriorityEnergyDistributor.DistributionResult<String> resNewGen = distributor.distribute(reqs, 2400);

        // ALTA sigue trabajando
        assertEquals(800, resNewGen.getAllocated(machineHigh));
        assertFalse(resNewGen.isShed(machineHigh));

        // NORMAL vuelve
        assertEquals(800, resNewGen.getAllocated(machineNormal));
        assertFalse(resNewGen.isShed(machineNormal));

        // BAJA vuelve a recibir energía y ya no está en SHED
        assertEquals(800, resNewGen.getAllocated(machineLow));
        assertFalse(resNewGen.isShed(machineLow));
        assertEquals(2400, resNewGen.totalDistributed());
    }

    @Test
    @DisplayName("Reparto proporcional dentro de un mismo nivel cuando la energía no alcanza para todo el nivel")
    void testProportionalDistributionWithinSamePriorityLevel() {
        PriorityEnergyDistributor<String> distributor = new PriorityEnergyDistributor<>(1);

        String machineA = "MachineA"; // NORMAL pide 600 FE
        String machineB = "MachineB"; // NORMAL pide 400 FE
        String machineLow = "MachineLow"; // LOW pide 500 FE

        Map<String, PriorityEnergyDistributor.Request<String>> reqs = new LinkedHashMap<>();
        reqs.put(machineA, new PriorityEnergyDistributor.Request<>(machineA, 600, EnergyPriority.NORMAL));
        reqs.put(machineB, new PriorityEnergyDistributor.Request<>(machineB, 400, EnergyPriority.NORMAL));
        reqs.put(machineLow, new PriorityEnergyDistributor.Request<>(machineLow, 500, EnergyPriority.LOW));

        // Presupuesto: 500 FE. No alcanza para NORMAL (requiere 1000 FE total)
        PriorityEnergyDistributor.DistributionResult<String> res = distributor.distribute(reqs, 500);

        // Proporcional: A recibe floor(500 * 600 / 1000) = 300 FE
        assertEquals(300, res.getAllocated(machineA));
        assertFalse(res.isShed(machineA));

        // Proporcional: B recibe floor(500 * 400 / 1000) = 200 FE
        assertEquals(200, res.getAllocated(machineB));
        assertFalse(res.isShed(machineB));

        // LOW recibe 0 FE y es SHED
        assertEquals(0, res.getAllocated(machineLow));
        assertTrue(res.isShed(machineLow));

        assertEquals(500, res.totalDistributed());
    }

    @Test
    @DisplayName("Estabilidad en el límite: una máquina no parpadea ON/OFF en cada ciclo ante fluctuaciones temporales")
    void testStabilityAtBoundaryPreventsFlickering() {
        int holdCycles = 3;
        PriorityEnergyDistributor<String> distributor = new PriorityEnergyDistributor<>(holdCycles);

        String machineHigh = "Alta";
        String machineNormal = "Normal";

        Map<String, PriorityEnergyDistributor.Request<String>> reqs = new LinkedHashMap<>();
        reqs.put(machineHigh, new PriorityEnergyDistributor.Request<>(machineHigh, 800, EnergyPriority.HIGH));
        reqs.put(machineNormal, new PriorityEnergyDistributor.Request<>(machineNormal, 400, EnergyPriority.NORMAL));

        // Ciclo 1: Presupuesto 1000 FE -> HIGH recibe 800, NORMAL recibe 200 (SERVED)
        PriorityEnergyDistributor.DistributionResult<String> r1 = distributor.distribute(reqs, 1000);
        assertEquals(800, r1.getAllocated(machineHigh));
        assertEquals(200, r1.getAllocated(machineNormal));
        assertFalse(r1.isShed(machineNormal));

        // Ciclo 2: Caída momentánea a 750 FE (menos de lo que pide HIGH).
        // Sin hold, NORMAL pasaría a SHED inmediatamente.
        // Con hold de 3 ciclos, NORMAL mantiene su estado SERVED (no pasa a SHED)
        PriorityEnergyDistributor.DistributionResult<String> r2 = distributor.distribute(reqs, 750);
        assertFalse(r2.isShed(machineNormal), "NORMAL no debe alternar a SHED ante una caída momentánea");
        assertTrue(r2.totalDistributed() <= 750, "Conservación de energía garantizada");

        // Ciclo 3: Vuelve a 1000 FE
        PriorityEnergyDistributor.DistributionResult<String> r3 = distributor.distribute(reqs, 1000);
        assertFalse(r3.isShed(machineNormal));

        // Ciclo 4: Otra caída momentánea
        PriorityEnergyDistributor.DistributionResult<String> r4 = distributor.distribute(reqs, 750);
        assertFalse(r4.isShed(machineNormal), "NORMAL sigue sin parpadear");
        assertTrue(r4.totalDistributed() <= 750);
    }

    @Test
    @DisplayName("Cambio de prioridad por el jugador reinicia el hold inmediatamente")
    void testPriorityChangeResetsHoldImmediately() {
        PriorityEnergyDistributor<String> distributor = new PriorityEnergyDistributor<>(5);

        String machineA = "MachineA";
        String machineB = "MachineB";

        // Ciclo 1: MachineA está en LOW, MachineB en HIGH con 800 FE
        Map<String, PriorityEnergyDistributor.Request<String>> reqs = new LinkedHashMap<>();
        reqs.put(machineA, new PriorityEnergyDistributor.Request<>(machineA, 800, EnergyPriority.LOW));
        reqs.put(machineB, new PriorityEnergyDistributor.Request<>(machineB, 800, EnergyPriority.HIGH));

        PriorityEnergyDistributor.DistributionResult<String> r1 = distributor.distribute(reqs, 800);
        assertTrue(r1.isShed(machineA), "MachineA debe estar SHED");
        assertEquals(800, r1.getAllocated(machineB));

        // Ciclo 2: El jugador cambia la prioridad de MachineA a HIGH mediante el botón
        reqs.put(machineA, new PriorityEnergyDistributor.Request<>(machineA, 800, EnergyPriority.HIGH));
        reqs.put(machineB, new PriorityEnergyDistributor.Request<>(machineB, 800, EnergyPriority.NORMAL));

        PriorityEnergyDistributor.DistributionResult<String> r2 = distributor.distribute(reqs, 800);
        assertFalse(r2.isShed(machineA), "El cambio de prioridad a HIGH debe tener efecto inmediato");
        assertEquals(800, r2.getAllocated(machineA));
        assertTrue(r2.isShed(machineB), "MachineB degradada a NORMAL queda SHED");
    }

    @Test
    @DisplayName("Invariantes de conservación de energía bajo todas las condiciones")
    void testEnergyConservationInvariant() {
        PriorityEnergyDistributor<String> distributor = new PriorityEnergyDistributor<>(3);

        List<PriorityEnergyDistributor.Request<String>> reqs = new ArrayList<>();
        reqs.add(new PriorityEnergyDistributor.Request<>("M1", 500, EnergyPriority.HIGH));
        reqs.add(new PriorityEnergyDistributor.Request<>("M2", 300, EnergyPriority.NORMAL));
        reqs.add(new PriorityEnergyDistributor.Request<>("M3", 700, EnergyPriority.NORMAL));
        reqs.add(new PriorityEnergyDistributor.Request<>("M4", 1000, EnergyPriority.LOW));

        int[] availableCases = {0, -50, 100, 500, 800, 1500, 2500, 5000};
        for (int avail : availableCases) {
            PriorityEnergyDistributor.DistributionResult<String> result = distributor.distribute(reqs, avail);
            assertTrue(result.totalDistributed() <= Math.max(0, avail),
                    "Total distribuido (" + result.totalDistributed() + ") no debe superar disponible (" + avail + ")");

            for (PriorityEnergyDistributor.Request<String> req : reqs) {
                int alloc = result.getAllocated(req.member());
                assertTrue(alloc >= 0, "Asignación no puede ser negativa");
                assertTrue(alloc <= req.requestedEnergy(), "Asignación no puede superar lo pedido");
            }
        }
    }
}
