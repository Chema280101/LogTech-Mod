package io.github.chema280101.logtech.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TierPolicyTest {

    private SimpleTier<String> hotTier;
    private SimpleTier<String> coldTier;
    private TierPolicyLogic<String> logic;

    @BeforeEach
    void setUp() {
        hotTier = new SimpleTier<>("hot", 1000L);
        coldTier = new SimpleTier<>("cold", 4000L);
        logic = new TierPolicyLogic<>();
    }

    @Test
    @DisplayName("Conservación atómica estricta: suma antes == suma después para cada ítem")
    void testAtomicConservationOfItems() {
        String keyA = "minecraft:iron_ingot";
        String keyB = "minecraft:copper_ingot";
        String keyC = "minecraft:gold_ingot";

        // 950 items en caliente (95% de uso, superior al 90%)
        hotTier.insert(keyA, 500L, false);
        hotTier.insert(keyB, 450L, false);

        // 200 items en frío con extracciones frecuentes
        coldTier.insert(keyC, 200L, false);
        logic.recordExtraction(keyC);
        logic.recordExtraction(keyC);
        logic.recordExtraction(keyC);

        long totalA_before = hotTier.getAmount(keyA) + coldTier.getAmount(keyA);
        long totalB_before = hotTier.getAmount(keyB) + coldTier.getAmount(keyB);
        long totalC_before = hotTier.getAmount(keyC) + coldTier.getAmount(keyC);
        long grandTotalBefore = hotTier.getTotalStored() + coldTier.getTotalStored();

        TierPolicyLogic.PolicyConfig config = TierPolicyLogic.PolicyConfig.DEFAULT;
        TierPolicyLogic.CycleResult result = logic.runCycle(hotTier, coldTier, config);

        assertTrue(result.totalItemsMoved() > 0, "Debe haberse movido al menos algún ítem");

        long totalA_after = hotTier.getAmount(keyA) + coldTier.getAmount(keyA);
        long totalB_after = hotTier.getAmount(keyB) + coldTier.getAmount(keyB);
        long totalC_after = hotTier.getAmount(keyC) + coldTier.getAmount(keyC);
        long grandTotalAfter = hotTier.getTotalStored() + coldTier.getTotalStored();

        assertEquals(totalA_before, totalA_after, "El total de keyA debe ser exactamente idéntico");
        assertEquals(totalB_before, totalB_after, "El total de keyB debe ser exactamente idéntico");
        assertEquals(totalC_before, totalC_after, "El total de keyC debe ser exactamente idéntico");
        assertEquals(grandTotalBefore, grandTotalAfter, "La suma total global debe conservarse estrictamente");
    }

    @Test
    @DisplayName("Promoción: ítems en frío con puntuación alta ascienden a caliente mientras haya espacio")
    void testPromotionFromColdToHot() {
        String popularItem = "logtech:neoferrite_ingot";
        String untouchedItem = "minecraft:dirt";

        // Caliente vacío, frío con dos tipos de ítems
        coldTier.insert(popularItem, 100L, false);
        coldTier.insert(untouchedItem, 100L, false);

        // Simulamos extracciones para el popular
        logic.recordExtraction(popularItem);
        logic.recordExtraction(popularItem);

        TierPolicyLogic.PolicyConfig config = TierPolicyLogic.PolicyConfig.DEFAULT;
        logic.runCycle(hotTier, coldTier, config);

        // El popular debe haber ascendido a caliente
        assertEquals(100L, hotTier.getAmount(popularItem), "El ítem popular debe subir a caliente");
        assertEquals(0L, coldTier.getAmount(popularItem), "El ítem popular no debe quedar en frío si cupo entero");

        // El intocado debe permanecer en frío
        assertEquals(0L, hotTier.getAmount(untouchedItem), "El ítem sin uso no debe subir");
        assertEquals(100L, coldTier.getAmount(untouchedItem), "El ítem sin uso debe seguir en frío");
    }

    @Test
    @DisplayName("Democión: cuando caliente supera el 90%, ítems de menor puntuación bajan hasta estar bajo 80%")
    void testDemotionWhenHotOver90Percent() {
        hotTier.setCapacity(1000L);
        String unusedItem = "minecraft:cobblestone";
        String usedItem = "minecraft:diamond";

        // 950 items en caliente (95% > 90% umbral)
        hotTier.insert(usedItem, 500L, false);
        hotTier.insert(unusedItem, 450L, false);

        // usedItem tiene uso
        logic.recordExtraction(usedItem);
        logic.recordExtraction(usedItem);

        TierPolicyLogic.PolicyConfig config = TierPolicyLogic.PolicyConfig.DEFAULT;
        logic.runCycle(hotTier, coldTier, config);

        // El caliente debe haber bajado al menos por debajo del 80% (<= 800 items)
        assertTrue(hotTier.getTotalStored() <= 800L,
                "El almacenamiento caliente debe quedar en o bajo el 80% (800 items), actual: " + hotTier.getTotalStored());

        // El ítem usado debe seguir en caliente
        assertEquals(500L, hotTier.getAmount(usedItem), "El ítem usado no debe haberse bajado");

        // El ítem no usado debe haber bajado parcialmente al frío
        assertTrue(coldTier.getAmount(unusedItem) >= 150L, "Al menos 150 items sin uso deben haber bajado al frío");
    }

    @Test
    @DisplayName("Fijado FIJAR_CALIENTE nunca baja al frío y tiene máxima prioridad para subir")
    void testPinnedHotNeverDemotesAndPromotesFirst() {
        hotTier.setCapacity(1000L);
        String pinnedHot = "minecraft:netherite_ingot";
        String regularHot = "minecraft:stone";

        logic.setPinMode(pinnedHot, ItemPinMode.FIJAR_CALIENTE);

        // Ponemos 950 en caliente (puntuación 0 en ambos)
        hotTier.insert(pinnedHot, 500L, false);
        hotTier.insert(regularHot, 450L, false);

        TierPolicyLogic.PolicyConfig config = TierPolicyLogic.PolicyConfig.DEFAULT;
        logic.runCycle(hotTier, coldTier, config);

        // pinnedHot NUNCA debe bajar
        assertEquals(500L, hotTier.getAmount(pinnedHot), "Ítem fijado en caliente nunca debe bajar");
        assertEquals(0L, coldTier.getAmount(pinnedHot));

        // El regular debe haber absorbido la bajada
        assertTrue(coldTier.getAmount(regularHot) > 0L, "El ítem no fijado debe ser el que baje");

        // Si además pinnedHot está en frío por inserción directa previa, debe subir incluso con puntuación 0
        String pinnedInCold = "minecraft:beacon";
        logic.setPinMode(pinnedInCold, ItemPinMode.FIJAR_CALIENTE);
        coldTier.insert(pinnedInCold, 50L, false);

        logic.runCycle(hotTier, coldTier, config);
        assertEquals(50L, hotTier.getAmount(pinnedInCold), "Ítem fijado en caliente en el frío debe subir a caliente");
    }

    @Test
    @DisplayName("Fijado SOLO_FRIO nunca sube a caliente y desciende inmediatamente si está en caliente")
    void testPinnedColdNeverPromotesAndDemotesImmediately() {
        String pinnedCold = "minecraft:gravel";
        logic.setPinMode(pinnedCold, ItemPinMode.SOLO_FRIO);

        // Simulamos muchas extracciones
        for (int i = 0; i < 10; i++) {
            logic.recordExtraction(pinnedCold);
        }

        coldTier.insert(pinnedCold, 100L, false);

        TierPolicyLogic.PolicyConfig config = TierPolicyLogic.PolicyConfig.DEFAULT;
        logic.runCycle(hotTier, coldTier, config);

        // A pesar de puntuación alta, NO debe subir
        assertEquals(0L, hotTier.getAmount(pinnedCold), "SOLO_FRIO no debe subir a caliente");
        assertEquals(100L, coldTier.getAmount(pinnedCold));

        // Si entra en caliente, en el ciclo debe descender a frío
        hotTier.insert(pinnedCold, 50L, false);
        assertEquals(50L, hotTier.getAmount(pinnedCold));

        logic.runCycle(hotTier, coldTier, config);
        assertEquals(0L, hotTier.getAmount(pinnedCold), "SOLO_FRIO que estaba en caliente debe descender a frío");
        assertEquals(150L, coldTier.getAmount(pinnedCold));
    }

    @Test
    @DisplayName("Decaimiento de puntuaciones: se multiplica por 0.9 cada ciclo")
    void testScoreDecay() {
        String key = "minecraft:redstone";
        logic.setScore(key, 10.0);

        TierPolicyLogic.PolicyConfig config = new TierPolicyLogic.PolicyConfig(200, 0.9, 0.9, 0.8, 50, false);
        logic.runCycle(hotTier, coldTier, config);

        assertEquals(9.0, logic.getScore(key), 0.0001, "La puntuación debe ser 10.0 * 0.9 = 9.0");

        logic.runCycle(hotTier, coldTier, config);
        assertEquals(8.1, logic.getScore(key), 0.0001, "La puntuación debe ser 9.0 * 0.9 = 8.1");
    }

    @Test
    @DisplayName("Rendimiento con 5 000 tipos de ítems distintos procesa como máximo N ítems sin lag")
    void testPerformanceWith5000ItemsAndMaxItemsLimit() {
        int maxItemsPerCycle = 50;
        TierPolicyLogic.PolicyConfig config = new TierPolicyLogic.PolicyConfig(200, 0.9, 0.9, 0.8, maxItemsPerCycle, false);

        hotTier.setCapacity(100_000L);
        coldTier.setCapacity(500_000L);

        // Creamos 5 000 items y los metemos en caliente superando el 90%
        List<String> keys = new ArrayList<>(5000);
        for (int i = 0; i < 5000; i++) {
            String k = "mod:item_" + i;
            keys.add(k);
            hotTier.insert(k, 19L, false); // 5000 * 19 = 95 000 (95%)
            if (i < 100) {
                logic.recordExtraction(k);
            }
        }

        assertEquals(95_000L, hotTier.getTotalStored());

        long startTime = System.currentTimeMillis();
        TierPolicyLogic.CycleResult result = logic.runCycle(hotTier, coldTier, config);
        long durationMs = System.currentTimeMillis() - startTime;

        // Debe haber procesado como máximo N items
        assertTrue(result.demotedCount() <= maxItemsPerCycle,
                "No debe procesar más de N items distintos en un ciclo: " + result.demotedCount());
        assertTrue(durationMs < 200, "Un ciclo con 5 000 ítems debe completarse en menos de 200 ms, tardó: " + durationMs + " ms");
    }
}
