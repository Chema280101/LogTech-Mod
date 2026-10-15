package io.github.chema280101.logtech.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CompactionDetectorTest {

    private static final String IRON_INGOT = "minecraft:iron_ingot";
    private static final String IRON_BLOCK = "minecraft:iron_block";
    private static final String IRON_NUGGET = "minecraft:iron_nugget";
    private static final String SAND = "minecraft:sand";
    private static final String SANDSTONE = "minecraft:sandstone";
    private static final String CLAY_BALL = "minecraft:clay_ball";
    private static final String CLAY_BLOCK = "minecraft:clay";
    private static final String QUARTZ = "minecraft:quartz";
    private static final String QUARTZ_BLOCK = "minecraft:quartz_block";
    private static final String SMOOTH_QUARTZ = "minecraft:smooth_quartz";
    private static final String HONEY_BOTTLE = "minecraft:honey_bottle";
    private static final String HONEY_BLOCK = "minecraft:honey_block";

    @Test
    @DisplayName("Detecta par reversible 3x3 exacto (Lingote de hierro ↔ Bloque de hierro)")
    void testDetectsValid3x3Pair() {
        List<CompactionCandidate<String>> recipes = new ArrayList<>();

        // 9 lingotes -> 1 bloque de hierro (3x3 con cuadrícula)
        List<String> nineIngots = Collections.nCopies(9, IRON_INGOT);
        recipes.add(CompactionCandidate.shaped("iron_block", 3, 3, nineIngots, false, IRON_BLOCK, 1));

        // 1 bloque de hierro -> 9 lingotes
        recipes.add(CompactionCandidate.shapeless("iron_ingot_from_block", List.of(IRON_BLOCK), false, IRON_INGOT, 9));

        Map<String, CompactionPair<String>> pairs = CompactionDetector.detectPairs(recipes, Collections.emptySet());

        assertTrue(pairs.containsKey(IRON_INGOT));
        CompactionPair<String> pair = pairs.get(IRON_INGOT);
        assertEquals(IRON_INGOT, pair.baseItem());
        assertEquals(IRON_BLOCK, pair.compactItem());
        assertEquals(9, pair.ratio());
    }

    @Test
    @DisplayName("Detecta par reversible 2x2 exacto")
    void testDetectsValid2x2Pair() {
        String base = "mod:copper_pebble";
        String compact = "mod:copper_cluster";

        List<CompactionCandidate<String>> recipes = new ArrayList<>();
        recipes.add(CompactionCandidate.shaped("cluster", 2, 2, Collections.nCopies(4, base), false, compact, 1));
        recipes.add(CompactionCandidate.shapeless("pebble", List.of(compact), false, base, 4));

        Map<String, CompactionPair<String>> pairs = CompactionDetector.detectPairs(recipes, Collections.emptySet());

        assertTrue(pairs.containsKey(base));
        assertEquals(compact, pairs.get(base).compactItem());
        assertEquals(4, pairs.get(base).ratio());
    }

    @Test
    @DisplayName("Detecta múltiples niveles (Pepita ↔ Lingote y Lingote ↔ Bloque)")
    void testDetectsMultiTierPairs() {
        List<CompactionCandidate<String>> recipes = new ArrayList<>();

        // Pepita -> Lingote (9 a 1) y Lingote -> Pepita (1 a 9)
        recipes.add(CompactionCandidate.shaped("iron_ingot_from_nuggets", 3, 3, Collections.nCopies(9, IRON_NUGGET), false, IRON_INGOT, 1));
        recipes.add(CompactionCandidate.shapeless("iron_nuggets_from_ingot", List.of(IRON_INGOT), false, IRON_NUGGET, 9));

        // Lingote -> Bloque (9 a 1) y Bloque -> Lingote (1 a 9)
        recipes.add(CompactionCandidate.shaped("iron_block", 3, 3, Collections.nCopies(9, IRON_INGOT), false, IRON_BLOCK, 1));
        recipes.add(CompactionCandidate.shapeless("iron_ingots_from_block", List.of(IRON_BLOCK), false, IRON_INGOT, 9));

        Map<String, CompactionPair<String>> pairs = CompactionDetector.detectPairs(recipes, Collections.emptySet());

        assertEquals(2, pairs.size());
        assertEquals(new CompactionPair<>(IRON_NUGGET, IRON_INGOT, 9), pairs.get(IRON_NUGGET));
        assertEquals(new CompactionPair<>(IRON_INGOT, IRON_BLOCK, 9), pairs.get(IRON_INGOT));
    }

    @Test
    @DisplayName("Receta no reversible: Arenisca (4 arena -> 1 arenisca, sin descompactación) se descarta")
    void testSandstoneRejectedDueToMissingDecompaction() {
        List<CompactionCandidate<String>> recipes = new ArrayList<>();
        // 4 arena -> 1 arenisca
        recipes.add(CompactionCandidate.shaped("sandstone", 2, 2, Collections.nCopies(4, SAND), false, SANDSTONE, 1));
        // No existe receta de arenisca a 4 arena en la mesa de crafteo

        Map<String, CompactionPair<String>> pairs = CompactionDetector.detectPairs(recipes, Collections.emptySet());
        assertFalse(pairs.containsKey(SAND), "La arena no debe compactarse porque la arenisca no es reversible a arena");
    }

    @Test
    @DisplayName("Receta no reversible: Arcilla (4 bolas -> 1 bloque, sin descompactación) se descarta")
    void testClayRejectedDueToMissingDecompaction() {
        List<CompactionCandidate<String>> recipes = new ArrayList<>();
        recipes.add(CompactionCandidate.shaped("clay", 2, 2, Collections.nCopies(4, CLAY_BALL), false, CLAY_BLOCK, 1));

        Map<String, CompactionPair<String>> pairs = CompactionDetector.detectPairs(recipes, Collections.emptySet());
        assertFalse(pairs.containsKey(CLAY_BALL), "La arcilla no debe compactarse");
    }

    @Test
    @DisplayName("Receta engañosa: Bloque de cuarzo descompacta a cuarzo liso, no a cuarzo normal")
    void testQuartzRejectedDueToAsymmetricalOutput() {
        List<CompactionCandidate<String>> recipes = new ArrayList<>();
        recipes.add(CompactionCandidate.shaped("quartz_block", 2, 2, Collections.nCopies(4, QUARTZ), false, QUARTZ_BLOCK, 1));
        // Descompacta a smooth_quartz en vez de quartz
        recipes.add(CompactionCandidate.shapeless("smooth_quartz", List.of(QUARTZ_BLOCK), false, SMOOTH_QUARTZ, 4));

        Map<String, CompactionPair<String>> pairs = CompactionDetector.detectPairs(recipes, Collections.emptySet());
        assertFalse(pairs.containsKey(QUARTZ), "El cuarzo debe descartarse por asimetría en el producto de retorno");
    }

    @Test
    @DisplayName("Receta con restos de crafteo: Bloque de miel (devuelve botellas de cristal) se descarta")
    void testHoneyBlockRejectedDueToRemainder() {
        List<CompactionCandidate<String>> recipes = new ArrayList<>();
        // 4 botellas de miel -> 1 bloque de miel, pero hasRemainder = true (deja 4 botellas de vidrio vacías)
        recipes.add(CompactionCandidate.shaped("honey_block", 2, 2, Collections.nCopies(4, HONEY_BOTTLE), true, HONEY_BLOCK, 1));
        recipes.add(CompactionCandidate.shapeless("honey_bottle_from_block", List.of(HONEY_BLOCK), false, HONEY_BOTTLE, 4));

        Map<String, CompactionPair<String>> pairs = CompactionDetector.detectPairs(recipes, Collections.emptySet());
        assertFalse(pairs.containsKey(HONEY_BOTTLE), "Las botellas de miel deben descartarse porque dejan restos de crafteo");
    }

    @Test
    @DisplayName("Receta con ratio asimétrico: 9 lingotes -> 1 bloque pero 1 bloque -> 10 pepitas o 8 lingotes")
    void testAsymmetricalRatioRejected() {
        List<CompactionCandidate<String>> recipes = new ArrayList<>();
        recipes.add(CompactionCandidate.shaped("iron_block", 3, 3, Collections.nCopies(9, IRON_INGOT), false, IRON_BLOCK, 1));
        // Devuelve 8 lingotes en vez de 9
        recipes.add(CompactionCandidate.shapeless("iron_ingot_from_block", List.of(IRON_BLOCK), false, IRON_INGOT, 8));

        Map<String, CompactionPair<String>> pairs = CompactionDetector.detectPairs(recipes, Collections.emptySet());
        assertFalse(pairs.containsKey(IRON_INGOT), "Debe descartarse si el retorno no coincide exactamente con el ratio de entrada");
    }

    @Test
    @DisplayName("Receta ambigua: múltiples salidas compactas conflictivas para el mismo ítem base")
    void testAmbiguousCompactionOutputsRejected() {
        String altBlock = "mod:alternative_iron_block";
        List<CompactionCandidate<String>> recipes = new ArrayList<>();

        recipes.add(CompactionCandidate.shaped("iron_block_1", 3, 3, Collections.nCopies(9, IRON_INGOT), false, IRON_BLOCK, 1));
        recipes.add(CompactionCandidate.shaped("iron_block_2", 3, 3, Collections.nCopies(9, IRON_INGOT), false, altBlock, 1));
        recipes.add(CompactionCandidate.shapeless("iron_decomp_1", List.of(IRON_BLOCK), false, IRON_INGOT, 9));
        recipes.add(CompactionCandidate.shapeless("iron_decomp_2", List.of(altBlock), false, IRON_INGOT, 9));

        Map<String, CompactionPair<String>> pairs = CompactionDetector.detectPairs(recipes, Collections.emptySet());
        assertFalse(pairs.containsKey(IRON_INGOT), "Si un ítem puede compactarse en 2 bloques distintos, es ambiguo y se descarta");
    }

    @Test
    @DisplayName("Cuadrícula incompleta o huecos (ej. cofre o horno con 8 ítems en 3x3) se descarta")
    void testIncompleteGridRejected() {
        List<CompactionCandidate<String>> recipes = new ArrayList<>();
        // 8 tablones en 3x3 para cofre
        recipes.add(CompactionCandidate.shaped("chest", 3, 3, Collections.nCopies(8, "minecraft:oak_planks"), false, "minecraft:chest", 1));

        Map<String, CompactionPair<String>> pairs = CompactionDetector.detectPairs(recipes, Collections.emptySet());
        assertFalse(pairs.containsKey("minecraft:oak_planks"), "8 ítems en 3x3 no es una compactación completa");
    }

    @Test
    @DisplayName("Lista negra de configuración excluye el ítem aunque sea simétrico")
    void testBlacklistRejectsItem() {
        List<CompactionCandidate<String>> recipes = new ArrayList<>();
        recipes.add(CompactionCandidate.shaped("iron_block", 3, 3, Collections.nCopies(9, IRON_INGOT), false, IRON_BLOCK, 1));
        recipes.add(CompactionCandidate.shapeless("iron_ingot_from_block", List.of(IRON_BLOCK), false, IRON_INGOT, 9));

        Set<String> blacklist = Set.of(IRON_INGOT);
        Map<String, CompactionPair<String>> pairs = CompactionDetector.detectPairs(recipes, blacklist);

        assertFalse(pairs.containsKey(IRON_INGOT), "El ítem en la lista negra debe ser ignorado");
    }
}
