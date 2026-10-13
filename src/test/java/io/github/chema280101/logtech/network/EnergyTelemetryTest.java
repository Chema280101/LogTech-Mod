package io.github.chema280101.logtech.network;

import io.github.chema280101.logtech.machine.ActivityCounter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnergyTelemetryTest {

    record MachineData(String name, int energyConsumed, EnergyPriority priority, boolean isCore) {}

    @Test
    @DisplayName("Coherencia de cifras: la suma del consumo por máquina coincide exactamente con el total de la red")
    void testEnergySumCoherence() {
        ActivityCounter coreCounter = new ActivityCounter();
        ActivityCounter coreReceivedCounter = new ActivityCounter();

        ActivityCounter crusherCounter = new ActivityCounter();
        ActivityCounter washerCounter = new ActivityCounter();
        ActivityCounter centrifugeCounter = new ActivityCounter();
        ActivityCounter drillCounter = new ActivityCounter();

        // Simular 1 minuto de operación (1 200 ticks)
        // Núcleo consume 20 FE/t = 24 000 FE/min
        coreCounter.recordEnergyConsumed(24_000);
        coreCounter.tick(true);

        // Generador entrega 60 000 FE/min al Núcleo
        coreReceivedCounter.recordEnergyReceived(60_000);
        coreReceivedCounter.tick(true);

        // Triturador: 40 FE/t trabajando 600 ticks = 24 000 FE/min
        crusherCounter.recordEnergyConsumed(24_000);
        crusherCounter.tick(true);

        // Lavadora: 60 FE/t trabajando 200 ticks = 12 000 FE/min
        washerCounter.recordEnergyConsumed(12_000);
        washerCounter.tick(true);

        // Centrífuga: 80 FE/t trabajando 100 ticks = 8 000 FE/min
        centrifugeCounter.recordEnergyConsumed(8_000);
        centrifugeCounter.tick(true);

        // Perforadora: 80 FE/t trabajando 400 ticks = 32 000 FE/min
        drillCounter.recordEnergyConsumed(32_000);
        drillCounter.tick(true);

        // Cálculo de telemetría (como en TerminalMenu)
        int coreConsumed = coreCounter.getEnergyConsumed(1);
        long storageBranch = coreConsumed; // Núcleo + Terminal (0)
        long mineralsBranch = 0;

        List<MachineData> entries = new ArrayList<>();
        entries.add(new MachineData("Núcleo", coreConsumed, EnergyPriority.HIGH, true));

        // Máquinas de la rama Minerales
        int mCrusher = crusherCounter.getEnergyConsumed(1);
        mineralsBranch += mCrusher;
        entries.add(new MachineData("Crusher", mCrusher, EnergyPriority.HIGH, false));

        int mWasher = washerCounter.getEnergyConsumed(1);
        mineralsBranch += mWasher;
        entries.add(new MachineData("Washer", mWasher, EnergyPriority.NORMAL, false));

        int mCentrifuge = centrifugeCounter.getEnergyConsumed(1);
        mineralsBranch += mCentrifuge;
        entries.add(new MachineData("Centrifuge", mCentrifuge, EnergyPriority.NORMAL, false));

        int mDrill = drillCounter.getEnergyConsumed(1);
        mineralsBranch += mDrill;
        entries.add(new MachineData("Drill", mDrill, EnergyPriority.LOW, false));

        int totalNetworkConsumed = coreConsumed + (mCrusher + mWasher + mCentrifuge + mDrill);

        // Comprobación 1: la suma de las entradas por máquina coincide con el total de la red
        int sumOfMachineEntries = entries.stream().mapToInt(MachineData::energyConsumed).sum();
        assertEquals(totalNetworkConsumed, sumOfMachineEntries,
                "La suma por máquina (Núcleo + máquinas) debe coincidir con el total de red");
        assertEquals(100_000, totalNetworkConsumed, "24k + 24k + 12k + 8k + 32k = 100 000 FE/min");

        // Comprobación 2: la suma de las ramas coincide con el total
        assertEquals(totalNetworkConsumed, storageBranch + mineralsBranch,
                "Almacenamiento (24k) + Minerales (76k) debe coincidir con el total (100k)");

        // Comprobación 3: energía recibida
        assertEquals(60_000, coreReceivedCounter.getEnergyReceived(1));
    }

    @Test
    @DisplayName("Desconexión del generador: la entrada de energía cae a 0 en el contador de minutos")
    void testGeneratorDisconnectDropsInput() {
        ActivityCounter coreReceivedCounter = new ActivityCounter();

        // Minuto 1 con generador conectado: 50 000 FE recibidas
        coreReceivedCounter.recordEnergyReceived(50_000);
        coreReceivedCounter.advanceTicks(1200, true);

        assertEquals(50_000, coreReceivedCounter.getEnergyReceived(1),
                "Con generador conectado la entrada debe ser 50 000 FE/min");

        // Minuto 2 con generador desconectado: 0 FE recibidas
        coreReceivedCounter.advanceTicks(1200, true);

        assertEquals(0, coreReceivedCounter.getEnergyReceived(1),
                "Al desconectar el generador la entrada debe caer a 0 FE/min");
    }

    @Test
    @DisplayName("Ordenamiento de máquinas por consumo de mayor a menor con su prioridad")
    void testMachineSortingAndPriority() {
        MachineData lowConsumer = new MachineData("Low Machine", 5_000, EnergyPriority.LOW, false);
        MachineData highConsumer = new MachineData("High Machine", 40_000, EnergyPriority.HIGH, false);
        MachineData midConsumer = new MachineData("Mid Machine", 15_000, EnergyPriority.NORMAL, false);

        List<MachineData> entries = new ArrayList<>(List.of(lowConsumer, highConsumer, midConsumer));

        // Ordenar de mayor a menor consumo
        entries.sort((a, b) -> {
            int cmp = Integer.compare(b.energyConsumed(), a.energyConsumed());
            if (cmp != 0) return cmp;
            return a.name().compareToIgnoreCase(b.name());
        });

        assertEquals("High Machine", entries.get(0).name());
        assertEquals(40_000, entries.get(0).energyConsumed());
        assertEquals(EnergyPriority.HIGH, entries.get(0).priority());

        assertEquals("Mid Machine", entries.get(1).name());
        assertEquals(15_000, entries.get(1).energyConsumed());
        assertEquals(EnergyPriority.NORMAL, entries.get(1).priority());

        assertEquals("Low Machine", entries.get(2).name());
        assertEquals(5_000, entries.get(2).energyConsumed());
        assertEquals(EnergyPriority.LOW, entries.get(2).priority());
    }

    @Test
    @DisplayName("Cambio de prioridad cíclico: NORMAL -> HIGH -> LOW -> NORMAL")
    void testPriorityCycling() {
        EnergyPriority p = EnergyPriority.NORMAL;
        p = p.next();
        assertEquals(EnergyPriority.HIGH, p);
        p = p.next();
        assertEquals(EnergyPriority.LOW, p);
        p = p.next();
        assertEquals(EnergyPriority.NORMAL, p);
    }
}
