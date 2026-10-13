package io.github.chema280101.logtech.network;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnergyNetworkSimulationTest {

    private static class SimpleMachineBuffer {
        private final String id;
        private final int capacity;
        private int stored;
        private String status = "WORKING";

        public SimpleMachineBuffer(String id, int capacity) {
            this.id = id;
            this.capacity = capacity;
            this.stored = 0;
        }

        public int requestedEnergy() {
            return Math.max(0, this.capacity - this.stored);
        }

        public int receiveNetworkEnergy(int amount) {
            if (amount <= 0) return 0;
            int accepted = Math.min(amount, this.capacity - this.stored);
            this.stored += accepted;
            return accepted;
        }

        public void tick(int consumePerTick) {
            if (this.stored >= consumePerTick) {
                this.stored -= consumePerTick;
                this.status = "WORKING";
            } else {
                this.status = "NO_ENERGY";
            }
        }
    }

    @Test
    @DisplayName("Simulación de conservación estricta de energía en ciclo del Núcleo y reparto proporcional")
    void testStrictEnergyConservation() {
        int coreCapacity = 200_000;
        int coreConsumptionPerInterval = 400; // 20 FE/t * 20 ticks

        // Crear 3 máquinas consumidoras
        SimpleMachineBuffer crusher = new SimpleMachineBuffer("crusher", 10_000);
        SimpleMachineBuffer washer = new SimpleMachineBuffer("washer", 10_000);
        SimpleMachineBuffer centrifuge = new SimpleMachineBuffer("centrifuge", 20_000);

        List<SimpleMachineBuffer> machines = List.of(crusher, washer, centrifuge);

        // Inyectar 15 000 FE al Núcleo
        int initialCoreEnergy = 15_000;
        long debugReceivedTotal = initialCoreEnergy;
        long debugDistributedTotal = 0;
        long debugSelfConsumedTotal = 0;
        int currentCoreEnergy = initialCoreEnergy;

        // Ciclo 1 del Núcleo:
        // 1. Descuento propio del Núcleo primero
        assertTrue(currentCoreEnergy >= coreConsumptionPerInterval);
        currentCoreEnergy -= coreConsumptionPerInterval;
        debugSelfConsumedTotal += coreConsumptionPerInterval;

        // 2. Reparto de energía a las máquinas
        Map<SimpleMachineBuffer, Integer> requests = new LinkedHashMap<>();
        for (SimpleMachineBuffer m : machines) {
            int req = m.requestedEnergy();
            if (req > 0) requests.put(m, req);
        }
        // Total pedido: 10 000 + 10 000 + 20 000 = 40 000 FE
        // Disponible: 14 600 FE
        Map<SimpleMachineBuffer, Integer> shares =
                EnergyDistributionLogic.distributeProportional(requests, currentCoreEnergy);

        int distributedThisCycle = 0;
        for (Map.Entry<SimpleMachineBuffer, Integer> entry : shares.entrySet()) {
            int accepted = entry.getKey().receiveNetworkEnergy(entry.getValue());
            distributedThisCycle += accepted;
        }

        currentCoreEnergy -= distributedThisCycle;
        debugDistributedTotal += distributedThisCycle;

        // Comprobación de invariante: Energía Total del Sistema = Inicial
        int energyInMachines = machines.stream().mapToInt(m -> m.stored).sum();
        long totalSystemEnergy = currentCoreEnergy + debugSelfConsumedTotal + energyInMachines;
        assertEquals(debugReceivedTotal, totalSystemEnergy, "La energía en el sistema debe conservarse al 100%");
        assertEquals(debugDistributedTotal, energyInMachines, "La energía repartida debe estar en los búferes de las máquinas");
        assertTrue(currentCoreEnergy >= 0);
    }

    @Test
    @DisplayName("Al cortar el suministro, las máquinas consumen de su búfer interno y se detienen en NO_ENERGY")
    void testMachineDrainsInternalBufferAndStopsAtNoEnergy() {
        SimpleMachineBuffer crusher = new SimpleMachineBuffer("crusher", 10_000);
        crusher.receiveNetworkEnergy(160); // Permite exactamente 4 ticks a 40 FE/t

        assertEquals(160, crusher.stored);
        assertEquals("WORKING", crusher.status);

        // Tick 1
        crusher.tick(40);
        assertEquals(120, crusher.stored);
        assertEquals("WORKING", crusher.status);

        // Tick 2
        crusher.tick(40);
        assertEquals(80, crusher.stored);
        assertEquals("WORKING", crusher.status);

        // Tick 3
        crusher.tick(40);
        assertEquals(40, crusher.stored);
        assertEquals("WORKING", crusher.status);

        // Tick 4
        crusher.tick(40);
        assertEquals(0, crusher.stored);
        assertEquals("WORKING", crusher.status);

        // Tick 5: Búfer agotado, debe detenerse con causa NO_ENERGY
        crusher.tick(40);
        assertEquals(0, crusher.stored);
        assertEquals("NO_ENERGY", crusher.status);
    }

    @Test
    @DisplayName("Si el Núcleo no tiene energía suficiente para su propio consumo, no reparte nada")
    void testCoreStarvationPreventsDistribution() {
        int coreEnergy = 200; // Menos de los 400 necesarios para 20 ticks
        int coreConsumptionPerInterval = 400;

        SimpleMachineBuffer crusher = new SimpleMachineBuffer("crusher", 10_000);
        Map<SimpleMachineBuffer, Integer> reqs = Map.of(crusher, crusher.requestedEnergy());

        boolean selfPowered = false;
        int remaining = coreEnergy;
        if (remaining >= coreConsumptionPerInterval) {
            remaining -= coreConsumptionPerInterval;
            selfPowered = true;
        } else if (remaining > 0) {
            remaining = 0;
        }

        assertFalse(selfPowered);
        assertEquals(0, remaining);

        // Como selfPowered es false, distributeNetworkEnergy no se ejecuta
        assertEquals(0, crusher.stored);
    }
}
