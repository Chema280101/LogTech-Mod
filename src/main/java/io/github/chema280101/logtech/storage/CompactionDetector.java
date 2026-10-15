package io.github.chema280101.logtech.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Algoritmo puro en Java para detectar pares reversibles exactos a partir de recetas de crafteo.
 *
 * Criterios estrictos de simetría y descarte:
 * - Receta de compactación: exactamente 9 (3x3) o 4 (2x2) ingredientes idénticos sin huecos,
 *   sin restos de crafteo, produciendo exactamente 1 resultado distinto del ingrediente base.
 * - Receta inversa (descompactación): exactamente 1 ingrediente que es el resultado compacto,
 *   sin restos de crafteo, produciendo exactamente ese mismo número (9 o 4) del ítem base.
 * - Simetría exacta: lo que entra en una debe salir en la otra y en la misma proporción.
 * - Cualquier ambigüedad (múltiples salidas compactas conflictivas o múltiples descompactaciones incompatibles)
 *   o ítem en la lista negra descarta el par de inmediato.
 */
public final class CompactionDetector {

    private CompactionDetector() {}

    public static <K> Map<K, CompactionPair<K>> detectPairs(
            Collection<CompactionCandidate<K>> candidates,
            Set<K> blacklist
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<K> blocked = blacklist != null ? blacklist : Collections.emptySet();

        // 1. Filtrar candidatos válidos de compactación y descompactación
        Map<K, List<CompactionCandidate<K>>> compactionsByBase = new HashMap<>();
        Map<K, List<CompactionCandidate<K>>> decompactionsByCompact = new HashMap<>();

        for (CompactionCandidate<K> cand : candidates) {
            if (cand == null || cand.hasRemainder() || cand.resultItem() == null) {
                continue;
            }
            if (blocked.contains(cand.resultItem())) {
                continue;
            }

            // Candidato a compactación: da 1 resultado a partir de 4 o 9 del mismo ítem base
            if (cand.resultCount() == 1) {
                int count = cand.ingredients().size();
                if (count == 4 || count == 9) {
                    if (cand.isGrid()) {
                        if (count == 4 && (cand.gridWidth() != 2 || cand.gridHeight() != 2)) {
                            continue;
                        }
                        if (count == 9 && (cand.gridWidth() != 3 || cand.gridHeight() != 3)) {
                            continue;
                        }
                    }
                    K first = cand.ingredients().getFirst();
                    if (first != null && !first.equals(cand.resultItem()) && !blocked.contains(first)) {
                        boolean allSame = true;
                        for (K ing : cand.ingredients()) {
                            if (!Objects.equals(first, ing)) {
                                allSame = false;
                                break;
                            }
                        }
                        if (allSame) {
                            compactionsByBase.computeIfAbsent(first, k -> new ArrayList<>()).add(cand);
                        }
                    }
                }
            }

            // Candidato a descompactación: 1 ingrediente da 4 o 9 ítems
            if (cand.ingredients().size() == 1 && (cand.resultCount() == 4 || cand.resultCount() == 9)) {
                if (cand.isGrid() && (cand.gridWidth() != 1 || cand.gridHeight() != 1)) {
                    continue;
                }
                K compact = cand.ingredients().getFirst();
                if (compact != null && !compact.equals(cand.resultItem()) && !blocked.contains(compact)) {
                    decompactionsByCompact.computeIfAbsent(compact, k -> new ArrayList<>()).add(cand);
                }
            }
        }

        // 2. Descartar ambigüedades en descompactaciones
        // Si un compactItem tiene recetas de descompactación hacia distintos ítems o con distintos ratios, es dudoso
        Map<K, CompactionCandidate<K>> unambiguousDecompact = new HashMap<>();
        for (Map.Entry<K, List<CompactionCandidate<K>>> entry : decompactionsByCompact.entrySet()) {
            K compactItem = entry.getKey();
            List<CompactionCandidate<K>> list = entry.getValue();
            K targetBase = list.getFirst().resultItem();
            int targetRatio = list.getFirst().resultCount();
            boolean consistent = true;
            for (CompactionCandidate<K> d : list) {
                if (!Objects.equals(targetBase, d.resultItem()) || targetRatio != d.resultCount()) {
                    consistent = false;
                    break;
                }
            }
            if (consistent) {
                unambiguousDecompact.put(compactItem, list.getFirst());
            }
        }

        // 3. Emparejar compactación con descompactación exigiendo simetría exacta
        Map<K, CompactionPair<K>> result = new HashMap<>();
        for (Map.Entry<K, List<CompactionCandidate<K>>> entry : compactionsByBase.entrySet()) {
            K baseItem = entry.getKey();
            List<CompactionCandidate<K>> compList = entry.getValue();

            // Comprobar consistencia de la salida compacta (todas las recetas deben dar el mismo compactItem y ratio)
            K compactItem = compList.getFirst().resultItem();
            int ratio = compList.getFirst().ingredients().size();
            boolean consistent = true;
            for (CompactionCandidate<K> c : compList) {
                if (!Objects.equals(compactItem, c.resultItem()) || ratio != c.ingredients().size()) {
                    consistent = false;
                    break;
                }
            }
            if (!consistent) {
                continue; // Ambigüedad en la compactación
            }

            // Buscar la receta de descompactación correspondiente
            CompactionCandidate<K> decomp = unambiguousDecompact.get(compactItem);
            if (decomp == null) {
                continue; // No existe descompactación para este compactItem (ej. arenisca, arcilla, cuarzo, etc.)
            }

            // Exigir simetría exacta: decomp.resultItem == baseItem && decomp.resultCount == ratio
            if (Objects.equals(baseItem, decomp.resultItem()) && decomp.resultCount() == ratio) {
                result.put(baseItem, new CompactionPair<>(baseItem, compactItem, ratio));
            }
        }

        return Collections.unmodifiableMap(result);
    }
}
