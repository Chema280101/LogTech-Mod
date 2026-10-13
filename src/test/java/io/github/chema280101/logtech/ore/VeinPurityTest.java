package io.github.chema280101.logtech.ore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VeinPurityTest {

    @Test
    @DisplayName("Generación determinista: todas las vetas tienen pureza entre 1 y 5")
    void testVeinPurityRange() {
        long worldSeed = 123456789L;
        Set<Integer> observedPurities = new HashSet<>();

        for (int rx = -10; rx <= 10; rx++) {
            for (int rz = -10; rz <= 10; rz++) {
                List<DepositVein> veins = VeinGenerator.generateVeins(worldSeed, rx, rz);
                for (DepositVein vein : veins) {
                    int purity = vein.getPurity();
                    assertTrue(purity >= 1 && purity <= 5, "La pureza de la veta debe estar entre 1 y 5, valor observado: " + purity);
                    observedPurities.add(purity);
                }
            }
        }

        // Con 441 regiones analizadas, todas las purezas de 1 a 5 deben haberse generado al menos una vez
        for (int p = 1; p <= 5; p++) {
            assertTrue(observedPurities.contains(p), "Debe existir al menos una veta con pureza " + p);
        }
    }

    @Test
    @DisplayName("Determinismo estricto de pureza con la misma semilla y coordenadas de región")
    void testVeinPurityDeterminism() {
        long worldSeed = 987654321L;

        for (int rx = -5; rx <= 5; rx++) {
            for (int rz = -5; rz <= 5; rz++) {
                List<DepositVein> firstCall = VeinGenerator.generateVeins(worldSeed, rx, rz);
                List<DepositVein> secondCall = VeinGenerator.generateVeins(worldSeed, rx, rz);

                assertEquals(firstCall.size(), secondCall.size());
                for (int i = 0; i < firstCall.size(); i++) {
                    assertEquals(firstCall.get(i).getPurity(), secondCall.get(i).getPurity(),
                            "La pureza generada debe ser exactamente la misma para la misma semilla y región");
                }
            }
        }
    }
}
