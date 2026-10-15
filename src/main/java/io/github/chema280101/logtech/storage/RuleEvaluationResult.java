package io.github.chema280101.logtech.storage;

import java.util.List;
import java.util.Set;

/**
 * Resultado inmutable de un ciclo de evaluación del motor de reglas.
 * Contiene los ítems cuya acción de solo frío está activa, los avisos disparados y la señal de redstone.
 */
public record RuleEvaluationResult<K>(
        Set<K> activeColdItems,
        List<RuleNotification<K>> notifications,
        boolean redstoneSignalActive,
        Set<Integer> activeRedstoneChannels,
        java.util.Map<K, Long> discardExcessItems,
        java.util.Map<K, Long> compactExcessItems
) {
    public RuleEvaluationResult(Set<K> activeColdItems, List<RuleNotification<K>> notifications, boolean redstoneSignalActive) {
        this(activeColdItems, notifications, redstoneSignalActive, redstoneSignalActive ? Set.of(1) : Set.of(), java.util.Map.of(), java.util.Map.of());
    }

    public RuleEvaluationResult(Set<K> activeColdItems, List<RuleNotification<K>> notifications, boolean redstoneSignalActive, Set<Integer> activeRedstoneChannels) {
        this(activeColdItems, notifications, redstoneSignalActive, activeRedstoneChannels, java.util.Map.of(), java.util.Map.of());
    }

    public RuleEvaluationResult(Set<K> activeColdItems, List<RuleNotification<K>> notifications, boolean redstoneSignalActive, Set<Integer> activeRedstoneChannels, java.util.Map<K, Long> discardExcessItems) {
        this(activeColdItems, notifications, redstoneSignalActive, activeRedstoneChannels, discardExcessItems, java.util.Map.of());
    }

    public boolean isColdActive(K item) {
        return item != null && activeColdItems.contains(item);
    }

    public boolean isChannelActive(int channel) {
        return activeRedstoneChannels != null && activeRedstoneChannels.contains(channel);
    }

    public long getExcessToDiscard(K item) {
        return item != null && discardExcessItems != null ? discardExcessItems.getOrDefault(item, 0L) : 0L;
    }

    public long getExcessToCompact(K item) {
        return item != null && compactExcessItems != null ? compactExcessItems.getOrDefault(item, 0L) : 0L;
    }
}
