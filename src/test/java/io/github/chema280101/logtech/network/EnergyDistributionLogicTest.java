package io.github.chema280101.logtech.network;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnergyDistributionLogicTest {

    @Test
    @DisplayName("Sin peticiones o con energía disponible cero devuelve mapa vacío")
    void testEmptyOrZeroAvailable() {
        Map<String, Integer> empty = EnergyDistributionLogic.distributeProportional(Collections.emptyMap(), 1000);
        assertTrue(empty.isEmpty());

        Map<String, Integer> reqs = Map.of("crusher", 100);
        Map<String, Integer> zeroEnergy = EnergyDistributionLogic.distributeProportional(reqs, 0);
        assertTrue(zeroEnergy.isEmpty());

        Map<String, Integer> negativeEnergy = EnergyDistributionLogic.distributeProportional(reqs, -50);
        assertTrue(negativeEnergy.isEmpty());
    }

    @Test
    @DisplayName("Cuando hay suficiente energía, cada miembro recibe el 100% de lo pedido")
    void testFullEnergyDistribution() {
        Map<String, Integer> reqs = new LinkedHashMap<>();
        reqs.put("crusher", 2000);
        reqs.put("washer", 1500);
        reqs.put("centrifuge", 5000);

        Map<String, Integer> result = EnergyDistributionLogic.distributeProportional(reqs, 10000);

        assertEquals(2000, result.get("crusher"));
        assertEquals(1500, result.get("washer"));
        assertEquals(5000, result.get("centrifuge"));

        long sum = result.values().stream().mapToLong(Integer::longValue).sum();
        assertEquals(8500, sum);
    }

    @Test
    @DisplayName("Cuando la energía es insuficiente, el reparto es proporcional sin exceder disponible")
    void testProportionalDistributionUnderDeficit() {
        Map<String, Integer> reqs = new LinkedHashMap<>();
        reqs.put("crusher", 1000);     // 25% de 4000
        reqs.put("washer", 1000);      // 25% de 4000
        reqs.put("centrifuge", 2000);  // 50% de 4000

        int available = 2000;
        Map<String, Integer> result = EnergyDistributionLogic.distributeProportional(reqs, available);

        assertEquals(500, result.get("crusher"));
        assertEquals(500, result.get("washer"));
        assertEquals(1000, result.get("centrifuge"));

        long sum = result.values().stream().mapToLong(Integer::longValue).sum();
        assertTrue(sum <= available);
        assertEquals(2000, sum);
    }

    @Test
    @DisplayName("Conservación de energía con divisiones inexactas y redondeos truncados")
    void testProportionalConservationWithOddNumbers() {
        Map<String, Integer> reqs = new LinkedHashMap<>();
        reqs.put("m1", 333);
        reqs.put("m2", 333);
        reqs.put("m3", 334);

        int available = 500;
        Map<String, Integer> result = EnergyDistributionLogic.distributeProportional(reqs, available);

        long sum = result.values().stream().mapToLong(Integer::longValue).sum();
        assertTrue(sum <= available);
        assertTrue(sum > 0);

        for (Map.Entry<String, Integer> entry : result.entrySet()) {
            assertTrue(entry.getValue() <= reqs.get(entry.getKey()));
            assertTrue(entry.getValue() >= 0);
        }
    }

    @Test
    @DisplayName("Miembros con demanda cero no reciben nada ni distorsionan el reparto")
    void testZeroDemandMembersIgnored() {
        Map<String, Integer> reqs = new LinkedHashMap<>();
        reqs.put("generator", 0);
        reqs.put("crusher", 1000);

        Map<String, Integer> result = EnergyDistributionLogic.distributeProportional(reqs, 500);

        assertFalse(result.containsKey("generator"));
        assertEquals(500, result.get("crusher"));
    }
}
