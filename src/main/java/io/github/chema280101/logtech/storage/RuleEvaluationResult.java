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
        Set<Integer> activeRedstoneChannels
) {
    public RuleEvaluationResult(Set<K> activeColdItems, List<RuleNotification<K>> notifications, boolean redstoneSignalActive) {
        this(activeColdItems, notifications, redstoneSignalActive, redstoneSignalActive ? Set.of(1) : Set.of());
    }

    public boolean isColdActive(K item) {
        return item != null && activeColdItems.contains(item);
    }

    public boolean isChannelActive(int channel) {
        return activeRedstoneChannels != null && activeRedstoneChannels.contains(channel);
    }
}
