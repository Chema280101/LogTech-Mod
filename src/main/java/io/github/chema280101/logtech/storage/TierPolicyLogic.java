package io.github.chema280101.logtech.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Lógica matemática y algorítmica desacoplada para la política de niveles caliente y frío.
 * Controla puntuaciones, decaimiento y ciclos de rebalanceo atómicos.
 *
 * @param <K> tipo de clave del ítem
 */
public class TierPolicyLogic<K> {

    public record PolicyConfig(
            int cycleInterval,
            double decayRate,
            double hotHighWatermark,
            double hotLowWatermark,
            int maxItemsPerCycle,
            boolean debugLog
    ) {
        public static final PolicyConfig DEFAULT = new PolicyConfig(200, 0.9, 0.90, 0.80, 50, false);
    }

    public record CycleResult(int promotedCount, int demotedCount, long totalItemsMoved) {
    }

    private final Map<K, Double> usageScores = new HashMap<>();
    private final Map<K, ItemPinMode> pinnedPolicies = new HashMap<>();
    private Consumer<String> debugLogger = null;

    public void setDebugLogger(Consumer<String> logger) {
        this.debugLogger = logger;
    }

    public void recordExtraction(K key) {
        if (key != null) {
            usageScores.merge(key, 1.0, Double::sum);
        }
    }

    public double getScore(K key) {
        return usageScores.getOrDefault(key, 0.0);
    }

    public void setScore(K key, double score) {
        if (score <= 0.0) {
            usageScores.remove(key);
        } else {
            usageScores.put(key, score);
        }
    }

    public ItemPinMode getPinMode(K key) {
        return pinnedPolicies.getOrDefault(key, ItemPinMode.AUTO);
    }

    public void setPinMode(K key, ItemPinMode mode) {
        if (key == null) return;
        if (mode == null || mode == ItemPinMode.AUTO) {
            pinnedPolicies.remove(key);
        } else {
            pinnedPolicies.put(key, mode);
        }
    }

    public ItemPinMode cyclePinMode(K key) {
        ItemPinMode current = getPinMode(key);
        ItemPinMode next = current.next();
        setPinMode(key, next);
        return next;
    }

    public Map<K, ItemPinMode> getPinnedPolicies() {
        return Collections.unmodifiableMap(pinnedPolicies);
    }

    public Map<K, Double> getUsageScores() {
        return Collections.unmodifiableMap(usageScores);
    }

    public void clear() {
        usageScores.clear();
        pinnedPolicies.clear();
    }

    public CycleResult runCycle(SimpleTier<K> hotTier, SimpleTier<K> coldTier, PolicyConfig config) {
        if (hotTier == null || coldTier == null || config == null) {
            return new CycleResult(0, 0, 0L);
        }

        // 1. Decaimiento de puntuaciones (se multiplican por el factor, por defecto 0.9)
        decayScores(config.decayRate(), hotTier, coldTier);

        int itemsProcessed = 0;
        int promotedCount = 0;
        int demotedCount = 0;
        long totalItemsMoved = 0L;

        long hotCapacity = hotTier.getCapacity();
        long coldCapacity = coldTier.getCapacity();

        // 2. Ascenso de frío a caliente (Promotion):
        // Los items del frío con puntuación alta (o fijados en FIJAR_CALIENTE) suben a caliente mientras haya espacio.
        if (hotCapacity > 0 && hotTier.getRemainingSpace() > 0 && !coldTier.isEmpty()) {
            List<K> promotionCandidates = findPromotionCandidates(coldTier);
            for (K key : promotionCandidates) {
                if (itemsProcessed >= config.maxItemsPerCycle() || hotTier.getRemainingSpace() <= 0) {
                    break;
                }
                long coldAmount = coldTier.getAmount(key);
                if (coldAmount <= 0) {
                    continue;
                }
                long moved = coldTier.moveTo(hotTier, key, coldAmount);
                if (moved > 0) {
                    itemsProcessed++;
                    promotedCount++;
                    totalItemsMoved += moved;
                }
            }
        }

        // 3. Descenso de caliente a frío (Demotion):
        // A) Primero: items fijados en SOLO_FRIO que estén en caliente descienden si hay espacio en frío.
        if (coldCapacity > 0 && coldTier.getRemainingSpace() > 0 && !hotTier.isEmpty()) {
            for (Map.Entry<K, ItemPinMode> entry : new HashMap<>(pinnedPolicies).entrySet()) {
                if (itemsProcessed >= config.maxItemsPerCycle() || coldTier.getRemainingSpace() <= 0) {
                    break;
                }
                if (entry.getValue() == ItemPinMode.SOLO_FRIO) {
                    K key = entry.getKey();
                    long hotAmt = hotTier.getAmount(key);
                    if (hotAmt > 0) {
                        long moved = hotTier.moveTo(coldTier, key, hotAmt);
                        if (moved > 0) {
                            itemsProcessed++;
                            demotedCount++;
                            totalItemsMoved += moved;
                        }
                    }
                }
            }
        }

        // B) Si el caliente supera el umbral alto (90% por defecto), bajan al frío los de menor puntuación
        // hasta quedar por debajo del umbral bajo (80% por defecto).
        if (hotCapacity > 0 && coldCapacity > 0 && coldTier.getRemainingSpace() > 0 && !hotTier.isEmpty()) {
            double usage = (double) hotTier.getTotalStored() / (double) hotCapacity;
            if (usage > config.hotHighWatermark()) {
                long targetMaxHot = (long) Math.floor(hotCapacity * config.hotLowWatermark());
                List<K> demotionCandidates = findDemotionCandidates(hotTier);

                for (K key : demotionCandidates) {
                    if (itemsProcessed >= config.maxItemsPerCycle() || coldTier.getRemainingSpace() <= 0) {
                        break;
                    }
                    if (hotTier.getTotalStored() <= targetMaxHot) {
                        break;
                    }

                    long excessInHot = hotTier.getTotalStored() - targetMaxHot;
                    long hotAmt = hotTier.getAmount(key);
                    long toMove = Math.min(hotAmt, Math.min(excessInHot, coldTier.getRemainingSpace()));
                    if (toMove <= 0) {
                        continue;
                    }

                    long moved = hotTier.moveTo(coldTier, key, toMove);
                    if (moved > 0) {
                        itemsProcessed++;
                        demotedCount++;
                        totalItemsMoved += moved;
                    }
                }
            }
        }

        if (config.debugLog() && debugLogger != null && (promotedCount > 0 || demotedCount > 0)) {
            debugLogger.accept(String.format("[LogTech TierPolicy] Cycle completed: %d promotions, %d demotions, %d items moved. Hot usage: %d/%d (%.1f%%)",
                    promotedCount, demotedCount, totalItemsMoved,
                    hotTier.getTotalStored(), hotCapacity,
                    hotCapacity > 0 ? (hotTier.getTotalStored() * 100.0 / hotCapacity) : 0.0));
        }

        return new CycleResult(promotedCount, demotedCount, totalItemsMoved);
    }

    public List<K> findPromotionCandidates(SimpleTier<K> coldTier) {
        List<K> candidates = new ArrayList<>();
        for (K key : coldTier.getAll().keySet()) {
            ItemPinMode mode = getPinMode(key);
            if (mode == ItemPinMode.SOLO_FRIO) {
                continue;
            }
            if (mode == ItemPinMode.FIJAR_CALIENTE || getScore(key) > 0.0) {
                candidates.add(key);
            }
        }

        candidates.sort((a, b) -> {
            ItemPinMode modeA = getPinMode(a);
            ItemPinMode modeB = getPinMode(b);
            if (modeA == ItemPinMode.FIJAR_CALIENTE && modeB != ItemPinMode.FIJAR_CALIENTE) return -1;
            if (modeB == ItemPinMode.FIJAR_CALIENTE && modeA != ItemPinMode.FIJAR_CALIENTE) return 1;
            return Double.compare(getScore(b), getScore(a)); // Puntuación descendente
        });

        return candidates;
    }

    public List<K> findDemotionCandidates(SimpleTier<K> hotTier) {
        List<K> candidates = new ArrayList<>();
        for (K key : hotTier.getAll().keySet()) {
            ItemPinMode mode = getPinMode(key);
            if (mode == ItemPinMode.AUTO) {
                candidates.add(key);
            }
        }

        candidates.sort(Comparator.comparingDouble(this::getScore)); // Puntuación ascendente (menor primero)
        return candidates;
    }

    private void decayScores(double decayRate, SimpleTier<K> hotTier, SimpleTier<K> coldTier) {
        usageScores.entrySet().removeIf(entry -> {
            double newScore = entry.getValue() * decayRate;
            entry.setValue(newScore);
            return newScore < 0.001 && hotTier.getAmount(entry.getKey()) == 0 && coldTier.getAmount(entry.getKey()) == 0;
        });
    }
}
