package io.github.chema280101.logtech.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas unitarias de capacidades y multiplicadores de módulos de datos (T32)")
class DataModuleCapacityTest {

    private static final long HOT_BASE = 4096L;
    private static final long COLD_BASE = 16384L;

    @Test
    @DisplayName("Módulo básico (v0.1) aporta exactamente x1 de la capacidad base (4.096 caliente, 16.384 frío)")
    void testBasicModuleCapacityV01Compatibility() {
        assertEquals(4096L, DataModuleLogic.calculateModuleCapacity(HOT_BASE, DataModuleLogic.ModuleTier.BASIC.getDefaultMultiplier()));
        assertEquals(16384L, DataModuleLogic.calculateModuleCapacity(COLD_BASE, DataModuleLogic.ModuleTier.BASIC.getDefaultMultiplier()));
    }

    @Test
    @DisplayName("Módulo avanzado aporta exactamente x4 de la capacidad base (16.384 caliente, 65.536 frío)")
    void testAdvancedModuleCapacity() {
        int mult = DataModuleLogic.ModuleTier.ADVANCED.getDefaultMultiplier();
        assertEquals(4, mult);
        assertEquals(16384L, DataModuleLogic.calculateModuleCapacity(HOT_BASE, mult));
        assertEquals(65536L, DataModuleLogic.calculateModuleCapacity(COLD_BASE, mult));
    }

    @Test
    @DisplayName("Módulo maestro aporta exactamente x16 de la capacidad base (65.536 caliente, 262.144 frío)")
    void testMasterModuleCapacity() {
        int mult = DataModuleLogic.ModuleTier.MASTER.getDefaultMultiplier();
        assertEquals(16, mult);
        assertEquals(65536L, DataModuleLogic.calculateModuleCapacity(HOT_BASE, mult));
        assertEquals(262144L, DataModuleLogic.calculateModuleCapacity(COLD_BASE, mult));
    }

    @Test
    @DisplayName("Rack caliente con módulos mezclados (1 Básico + 1 Avanzado + 1 Maestro + 1 Vacío) suma 86.016")
    void testHotRackMixedModules() {
        // Ranura 0: Básico (4096)
        // Ranura 1: Avanzado (16384)
        // Ranura 2: Maestro (65536)
        // Ranura 3: Vacío (0)
        List<Integer> modules = Arrays.asList(1, 4, 16, 0);
        long rackCapacity = DataModuleLogic.calculateRackCapacity(HOT_BASE, modules);

        long expected = 4096L + 16384L + 65536L + 0L;
        assertEquals(86016L, expected);
        assertEquals(expected, rackCapacity);
    }

    @Test
    @DisplayName("Rack frío con módulos mezclados (1 Básico + 1 Avanzado + 1 Maestro + 1 Vacío) suma 344.064")
    void testColdRackMixedModules() {
        List<Integer> modules = Arrays.asList(1, 4, 16, 0);
        long rackCapacity = DataModuleLogic.calculateRackCapacity(COLD_BASE, modules);

        long expected = 16384L + 65536L + 262144L + 0L;
        assertEquals(344064L, expected);
        assertEquals(expected, rackCapacity);
    }

    @Test
    @DisplayName("Red con múltiples racks mezclados suma correctamente el almacenamiento global")
    void testNetworkMultipleRacksSum() {
        // Rack 1 (caliente): 4 básicos (v0.1) = 4 * 4096 = 16.384
        long rack1 = DataModuleLogic.calculateRackCapacity(HOT_BASE, Arrays.asList(1, 1, 1, 1));
        assertEquals(16384L, rack1);

        // Rack 2 (caliente): 2 avanzados + 2 maestros = 2*16384 + 2*65536 = 163.840
        long rack2 = DataModuleLogic.calculateRackCapacity(HOT_BASE, Arrays.asList(4, 4, 16, 16));
        assertEquals(163840L, rack2);

        // Rack 3 (frío): 1 básico + 2 avanzados = 16384 + 2*65536 = 147.456
        long rack3 = DataModuleLogic.calculateRackCapacity(COLD_BASE, Arrays.asList(1, 4, 4, 0));
        assertEquals(147456L, rack3);

        // Rack 4 (frío): 4 maestros = 4 * 262144 = 1.048.576
        long rack4 = DataModuleLogic.calculateRackCapacity(COLD_BASE, Arrays.asList(16, 16, 16, 16));
        assertEquals(1048576L, rack4);

        // Suma total en red
        long totalNetwork = DataModuleLogic.calculateNetworkCapacity(Arrays.asList(rack1, rack2, rack3, rack4));
        long expectedTotal = 16384L + 163840L + 147456L + 1048576L;
        assertEquals(1376256L, expectedTotal);
        assertEquals(expectedTotal, totalNetwork);
    }

    @Test
    @DisplayName("Comportamiento idéntico a v0.1 cuando solo hay módulos básicos presentes")
    void testStrictV01Fidelity() {
        // En v0.1, cada módulo básico aporta 4096 en rack caliente
        for (int count = 1; count <= 4; count++) {
            List<Integer> modules = Collections.nCopies(count, 1);
            long expected = count * 4096L;
            assertEquals(expected, DataModuleLogic.calculateRackCapacity(HOT_BASE, modules));
        }
    }

    @Test
    @DisplayName("Multiplicadores configurables personalizados respetan el escalado proporcional")
    void testCustomMultipliers() {
        int customAdv = 8;    // x8 en vez de x4
        int customMaster = 32; // x32 en vez de x16

        long moduleAdv = DataModuleLogic.calculateModuleCapacity(HOT_BASE, customAdv);
        long moduleMaster = DataModuleLogic.calculateModuleCapacity(HOT_BASE, customMaster);

        assertEquals(32768L, moduleAdv);
        assertEquals(131072L, moduleMaster);

        List<Integer> customRack = Arrays.asList(1, customAdv, customMaster, 0);
        long total = DataModuleLogic.calculateRackCapacity(HOT_BASE, customRack);
        assertEquals(4096L + 32768L + 131072L, total);
    }

    @Test
    @DisplayName("Manejo robusto de casos límite (nulos, negativos y colecciones vacías)")
    void testEdgeCases() {
        assertEquals(0L, DataModuleLogic.calculateModuleCapacity(0L, 4));
        assertEquals(0L, DataModuleLogic.calculateModuleCapacity(HOT_BASE, 0));
        assertEquals(0L, DataModuleLogic.calculateModuleCapacity(HOT_BASE, -5));
        assertEquals(0L, DataModuleLogic.calculateModuleCapacity(-1000L, 4));

        assertEquals(0L, DataModuleLogic.calculateRackCapacity(HOT_BASE, null));
        assertEquals(0L, DataModuleLogic.calculateRackCapacity(HOT_BASE, Collections.emptyList()));
        assertEquals(0L, DataModuleLogic.calculateRackCapacity(HOT_BASE, Arrays.asList(0, -1, null)));

        assertEquals(0L, DataModuleLogic.calculateNetworkCapacity(null));
        assertEquals(0L, DataModuleLogic.calculateNetworkCapacity(Collections.emptyList()));
        assertEquals(0L, DataModuleLogic.calculateNetworkCapacity(Arrays.asList(0L, -50L, null)));
    }
}
